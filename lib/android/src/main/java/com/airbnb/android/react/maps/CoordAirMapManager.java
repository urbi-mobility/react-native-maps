package com.airbnb.android.react.maps;

import android.content.Context;
import android.graphics.Point;
import android.view.Display;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;

import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReadableArray;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.common.MapBuilder;
import com.facebook.react.uimanager.ThemedReactContext;
import com.facebook.react.uimanager.ViewGroupManager;
import com.facebook.react.uimanager.annotations.ReactProp;
import com.facebook.react.uimanager.events.RCTEventEmitter;
import com.hardsoftstudio.widget.AnchorSheetBehavior;

import java.util.HashMap;
import java.util.Map;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class CoordAirMapManager extends ViewGroupManager<CoordAirMapView> {

  private static final String REACT_CLASS = "CAIRMap";
  private final ReactApplicationContext appContext;

  private final int CHANGE_STATUS_BOTTOM_SHEET = 1;
  private final static String EXPAND = "EXPAND";
  private final static String COLLAPSED = "COLLAPSED";
  private final static String HIDE = "HIDE";
  private final static String ANCHOR = "ANCHOR";
  private final static String DRAGGING = "DRAGGING";

  public final static Map<Integer, String> stateConvert = new HashMap<Integer, String>() {{
    put(AnchorSheetBehavior.STATE_ANCHOR, ANCHOR);
    put(AnchorSheetBehavior.STATE_COLLAPSED, COLLAPSED);
    put(AnchorSheetBehavior.STATE_DRAGGING, DRAGGING);
    put(AnchorSheetBehavior.STATE_EXPANDED, EXPAND);
  }};

  private final static Map<String, Integer> inveseStateConvert = new HashMap<String, Integer>() {{
    put(ANCHOR, AnchorSheetBehavior.STATE_ANCHOR);
    put(COLLAPSED, AnchorSheetBehavior.STATE_COLLAPSED);
    put(DRAGGING, AnchorSheetBehavior.STATE_DRAGGING);
    put(EXPAND, AnchorSheetBehavior.STATE_EXPANDED);
  }};


  public CoordAirMapManager(ReactApplicationContext context) {
    this.appContext = context;
  }

  @Nonnull
  @Override
  public String getName() {
    return REACT_CLASS;
  }

  @Nonnull
  @Override
  protected CoordAirMapView createViewInstance(@Nonnull ThemedReactContext reactContext) {
    return new CoordAirMapView(reactContext, this);
  }

  @Override
  @Nullable
  public Map getExportedCustomDirectEventTypeConstants() {
    Map<String, Map<String, String>> map = MapBuilder.of(
        "newStatusValue", MapBuilder.of("registrationName", "newStatusValue")
    );
    return map;
  }

  void pushEvent(ThemedReactContext context, View view, String name, WritableMap data) {
    context.getJSModule(RCTEventEmitter.class)
        .receiveEvent(view.getId(), name, data);
  }

  @ReactProp(name = "peekHeight")
  public void setPeekHeight(CoordAirMapView view, int peekHeight) {
    view.setPeekHeightFirstView(peekHeight);
  }

  @ReactProp(name = "anchorPoint")
  public void setAnchorPoint(CoordAirMapView view, float anchorSize) {
    WindowManager wm = (WindowManager) appContext.getSystemService(Context.WINDOW_SERVICE);
    Display display = wm.getDefaultDisplay();
    Point size = new Point();
    display.getSize(size);
    float anchor = convertPxToDp(appContext, anchorSize);
    float height = convertPxToDp(appContext, size.y);
    view.setAnchorPoint(1 - (anchor / height));
  }

  @ReactProp(name = "bottomSheetStatus")
  public void setBottomSheetStatus(CoordAirMapView view, String status) {
    if (inveseStateConvert.containsKey(status))
      view.setBottomSheetStatus(inveseStateConvert.get(status));
  }

  @Override
  public void receiveCommand(@Nonnull CoordAirMapView root, int commandId, @Nullable ReadableArray args) {
    switch (commandId) {
      case CHANGE_STATUS_BOTTOM_SHEET:
        if (args != null && args.size() > 0) {
          String key = args.getString(0);
          root.setBottomSheetStatus(inveseStateConvert.get(key));
        }
        break;
    }
  }

  @Nullable
  @Override
  public Map<String, Integer> getCommandsMap() {
    return MapBuilder.of("setStatus", CHANGE_STATUS_BOTTOM_SHEET);
  }

  /***
   *
   * @param parent
   * @param child
   * @param index
   */
  @Override
  public void addView(CoordAirMapView parent, View child, int index) {

    switch (index) {
      case 0: {
        findAirMapView(parent, child);
        ViewGroup view = parent.findViewById(R.id.replaceMap);
        view.addView(child);
      }
      break;
      case 1: {
        ViewGroup view = parent.findViewById(R.id.replaceSheet);
        view.addView(child);
      }
      break;
      case 2: {
        ViewGroup view = parent.findViewById(R.id.replaceHeader);
        view.addView(child);
      }
      break;
    }

  }

  private void findAirMapView(CoordAirMapView parent, View child) {
    if (child instanceof AirMapView) {
      parent.setAirMapView((AirMapView) child);
    } else if (child instanceof ViewGroup) {
      for (int i = 0; i < ((ViewGroup) child).getChildCount(); i++) {
        findAirMapView(parent, ((ViewGroup) child).getChildAt(i));
      }
    }
  }


  public float convertPxToDp(Context context, float px) {
    return (px / context.getResources().getDisplayMetrics().density);
  }
}
