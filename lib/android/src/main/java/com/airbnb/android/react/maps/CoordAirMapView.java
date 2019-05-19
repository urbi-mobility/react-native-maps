package com.airbnb.android.react.maps;

import android.view.View;
import com.facebook.react.uimanager.ThemedReactContext;

public class CoordAirMapView extends android.support.design.widget.CoordinatorLayout {
    public AirMapView airMapView;

    public CoordAirMapView(ThemedReactContext reactContext,
                           AirMapManager manager) {
        super(reactContext);
        init();
        airMapView.setExtra(manager);
    }

    private void init() {
        View view = View.inflate(getContext(), R.layout.coordinator, this);
        airMapView = view.findViewById(R.id.airMap);
    }
}
