package com.airbnb.android.react.maps;

import android.view.View;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.common.MapBuilder;
import com.facebook.react.uimanager.ThemedReactContext;
import com.facebook.react.uimanager.ViewGroupManager;
import com.facebook.react.uimanager.events.RCTEventEmitter;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Map;

public class CoordAirMapManager extends ViewGroupManager<CoordAirMapView> {

    private static final String REACT_CLASS = "CAIRMap";
    private final ReactApplicationContext appContext;

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

    @Override
    public void addView(CoordAirMapView parent, View child, int index) {
        super.addView(parent, child, index);
    }
}
