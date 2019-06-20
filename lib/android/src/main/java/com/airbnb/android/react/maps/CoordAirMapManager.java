package com.airbnb.android.react.maps;

import android.support.design.widget.AppBarLayout;
import android.view.View;
import android.view.ViewGroup;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReadableArray;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.common.MapBuilder;
import com.facebook.react.uimanager.ThemedReactContext;
import com.facebook.react.uimanager.ViewGroupManager;
import com.facebook.react.uimanager.annotations.ReactProp;
import com.facebook.react.uimanager.events.RCTEventEmitter;
import com.hardsoftstudio.widget.AnchorSheetBehavior;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Map;

public class CoordAirMapManager extends ViewGroupManager<CoordAirMapView> {

    private static final String REACT_CLASS = "CAIRMap";
    private final ReactApplicationContext appContext;

    private final int CHANGE_STATUS_BOTTONSHEET = 1;
    private final String EXPAND = "EXPAND";
    private final String COLLAPSED = "COLLAPSED";
    private final String HIDE = "HIDE";
    private final String ANCHOR = "ANCHOR";

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
        return new CoordAirMapView(reactContext,this);

    }

    @Override
    @Nullable
    public Map getExportedCustomDirectEventTypeConstants() {
        Map<String, Map<String, String>> map = MapBuilder.of(
                "clickHeader",MapBuilder.of("registrationName", "clickHeader")
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


    @Override
    public void receiveCommand(@Nonnull CoordAirMapView root, int commandId, @Nullable ReadableArray args) {
        switch (commandId) {
            case CHANGE_STATUS_BOTTONSHEET:
                if (args != null && args.size() > 0) {
                    switch (args.getString(0)) {
                        case EXPAND:
                            root.setStatusBottomSheet(AnchorSheetBehavior.STATE_EXPANDED);
                            break;
                        case HIDE:
                            root.setStatusBottomSheet(AnchorSheetBehavior.STATE_HIDDEN);
                            break;
                        case COLLAPSED:
                            root.setStatusBottomSheet(AnchorSheetBehavior.STATE_COLLAPSED);
                            break;
                        case ANCHOR:
                            root.setStatusBottomSheet(AnchorSheetBehavior.STATE_ANCHOR);
                            break;
                    }
                }
                break;
        }
    }

    @Nullable
    @Override
    public Map<String, Integer> getCommandsMap() {
        return MapBuilder.of("setStatusExpandable", CHANGE_STATUS_BOTTONSHEET);
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
                if (child instanceof AirMapView)
                    parent.setAirMapView((AirMapView) child);
                ViewGroup view = parent.findViewById(R.id.replaceMap);
                view.addView(child);
            }
            break;
            case 1: {
//                if (child instanceof ViewGroup)
//                    parent.setPeekHeightFirstView(((ViewGroup) child).getChildAt(0));
//                else
//                    parent.setPeekHeightFirstView(child);
                ViewGroup view = parent.findViewById(R.id.replaceSheet);
                view.addView(child);
            }
            break;
            case 2: {
                AppBarLayout view = parent.findViewById(R.id.mainAppbar);
                view.setExpanded(false);
                view.setLiftable(false);
                view.addView(child, 0);
                view.setVisibility(View.VISIBLE);
            }
            break;
        }

    }
}
