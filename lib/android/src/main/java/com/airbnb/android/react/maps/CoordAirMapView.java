package com.airbnb.android.react.maps;

import android.content.Context;
import android.support.design.widget.BottomSheetBehavior;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.FrameLayout;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.bridge.WritableNativeMap;
import com.facebook.react.uimanager.ThemedReactContext;

public class CoordAirMapView extends android.support.design.widget.CoordinatorLayout {
    public CoordAirMapManager manager;

    private BottomSheetBehavior<FrameLayout> mainBottomSheetBehavior;
    public CoordAirMapView(Context context, CoordAirMapManager coordAirMapManager) {
        super(context);
        this.manager = coordAirMapManager;
        init();
    }


    private void init() {
        View view = View.inflate(getContext(), R.layout.coordinator, this);
        FrameLayout replaceSheet = view.findViewById(R.id.replaceSheet);
        mainBottomSheetBehavior = BottomSheetBehavior.from(replaceSheet);
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



}
