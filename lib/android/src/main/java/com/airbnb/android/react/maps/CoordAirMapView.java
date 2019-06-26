package com.airbnb.android.react.maps;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomSheetBehavior;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.LinearLayout;

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
    setStatusBottomSheet(AnchorSheetBehavior.STATE_ANCHOR);
  }

  public void setPeekHeightFirstView(final int peekHeight) {
    mainBottomSheetBehavior.setPeekHeight(peekHeight);
  }

  public void setAnchorPoint(final float anchorPoint) {
    mainBottomSheetBehavior.setAnchorOffset(anchorPoint);
  }

  public void setStatusBottomSheet(int status) {
    mainBottomSheetBehavior.setState(status);
  }

  public void setAirMapView(AirMapView airMapView) {
    this.airMapView = airMapView;
  }

}
