package com.airbnb.android.react.maps;

import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.uimanager.ThemedReactContext;
import com.facebook.react.uimanager.ViewGroupManager;
import com.google.android.gms.maps.GoogleMapOptions;

import javax.annotation.Nonnull;

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

        return new CoordAirMapView(reactContext, new AirMapManager(this.appContext, new GoogleMapOptions().liteMode(true)));
    }
}
