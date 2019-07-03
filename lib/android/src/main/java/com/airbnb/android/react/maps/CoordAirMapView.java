package com.airbnb.android.react.maps;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomSheetBehavior;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.LinearLayout;

import com.facebook.react.bridge.WritableMap;
import com.facebook.react.bridge.WritableNativeMap;
import com.facebook.react.uimanager.ThemedReactContext;
import com.hardsoftstudio.widget.AnchorSheetBehavior;

public class CoordAirMapView extends LinearLayout {
  public CoordAirMapManager manager;
  public View floatingActionButton;
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
    final CoordAirMapView coordAirMapView = this;
    mainBottomSheetBehavior.setAnchorSheetCallback(new AnchorSheetBehavior.AnchorSheetCallback() {
      @Override
      public void onStateChanged(@NonNull View bottomSheet, int newState) {

        if (BottomSheetBehavior.STATE_DRAGGING == newState) {
          floatingActionButton.animate().scaleX(1).scaleY(1).setDuration(300).start();
        } else if (BottomSheetBehavior.STATE_EXPANDED == newState) {
          floatingActionButton.animate().scaleX(0).scaleY(0).setDuration(300).start();
        }
        // send Event to JS, but don't send DRAGGING for now (we don't need it and the callback might slow us down)
        if (CoordAirMapManager.stateConvert.containsKey(newState) && newState != AnchorSheetBehavior.STATE_DRAGGING) {
          WritableMap event = new WritableNativeMap();
          event.putString("status", CoordAirMapManager.stateConvert.get(newState));
          manager.pushEvent((ThemedReactContext) getContext(), coordAirMapView, "onStatusChange", event);
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


  public void manuallyLayoutChildren(View child, int y) {
    child.measure(MeasureSpec.makeMeasureSpec(getMeasuredWidth(), MeasureSpec.EXACTLY),
        MeasureSpec.makeMeasureSpec(getMeasuredHeight(), MeasureSpec.EXACTLY));

    child.layout(0, y, child.getMeasuredWidth(), child.getMeasuredHeight());

  }

  public void setPeekHeightFirstView(final int peekHeight) {
    mainBottomSheetBehavior.setPeekHeight(manager.toPixels(peekHeight));
  }

  public void setAnchorPoint(final float anchorPoint) {
    mainBottomSheetBehavior.setAnchorOffset(anchorPoint);
    mainBottomSheetBehavior.setState(AnchorSheetBehavior.STATE_ANCHOR);
  }

  public void setBottomSheetStatus(int status) {
    mainBottomSheetBehavior.setState(status);
  }

  public void setAirMapView(AirMapView airMapView) {
    this.airMapView = airMapView;
  }

}
