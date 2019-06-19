package com.airbnb.android.react.maps;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomSheetBehavior;
import android.support.design.widget.FloatingActionButton;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.FrameLayout;

import com.facebook.react.bridge.WritableMap;
import com.facebook.react.bridge.WritableNativeMap;
import com.facebook.react.uimanager.ThemedReactContext;
import com.hardsoftstudio.widget.AnchorSheetBehavior;

public class CoordAirMapView extends android.support.design.widget.CoordinatorLayout {
    public CoordAirMapManager manager;
    public FloatingActionButton floatingActionButton;
    public AirMapView airMapView;

    private AnchorSheetBehavior<FrameLayout> mainBottomSheetBehavior;
    public CoordAirMapView(Context context, CoordAirMapManager coordAirMapManager) {
        super(context);
        this.manager = coordAirMapManager;
        init();
    }


    private void init() {
        View view = View.inflate(getContext(), R.layout.coordinator, this);
        FrameLayout replaceSheet = view.findViewById(R.id.replaceSheet);
        floatingActionButton = view.findViewById(R.id.floatingPoint);
        mainBottomSheetBehavior = AnchorSheetBehavior.from(replaceSheet);
        mainBottomSheetBehavior.setAnchorOffset(0.25f);
        mainBottomSheetBehavior.setAnchorSheetCallback(new AnchorSheetBehavior.AnchorSheetCallback() {
            @Override
            public void onStateChanged(@NonNull View bottomSheet, int newState) {

                // this part hides the button immediately and waits bottom sheet
                // to collapse to show
                if (BottomSheetBehavior.STATE_DRAGGING == newState) {
                    floatingActionButton.animate().scaleX(1).scaleY(1).setDuration(300).start();
                } else if (BottomSheetBehavior.STATE_EXPANDED == newState) {
                    floatingActionButton.animate().scaleX(0).scaleY(0).setDuration(300).start();
                }
            }

            @Override
            public void onSlide(@NonNull View bottomSheet, float slideOffset) {
            }
        });
        floatingActionButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                airMapView.centerToUserLocation();
            }
        });
    }

    public void setPeekHeightFirstView(final View view) {
        view.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                if (view.getHeight() != 0) {
                    view.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                    mainBottomSheetBehavior.setPeekHeight(view.getHeight());

                }
            }
        });
        final CoordAirMapView coordAirMapView = this;
        view.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                WritableMap event = new WritableNativeMap();
                event.putString("message", "Cliccato Header");
                manager.pushEvent((ThemedReactContext) getContext(), coordAirMapView, "clickHeader", event);

            }
        });
    }

    public void setStatusBottomSheet(int status) {
        mainBottomSheetBehavior.setState(status);
    }


    public void setAirMapView(AirMapView airMapView) {
        this.airMapView = airMapView;
    }


}
