package com.airbnb.android.react.maps;

import com.facebook.react.bridge.ReactApplicationContext;

public class CAirMapLiteManager extends CoordAirMapManager {

    private static final String REACT_CLASS = "CAIRMapLite";

    public CAirMapLiteManager(ReactApplicationContext context) {
        super(context);
    }

    @Override
    public String getName() {
        return REACT_CLASS;
    }

}
