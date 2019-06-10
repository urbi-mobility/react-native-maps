package com.airbnb.android.react.maps;

import android.content.Context;
import android.view.View;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.bridge.WritableNativeMap;
import com.facebook.react.uimanager.ThemedReactContext;
//import com.facebook.react.uimanager.ThemedReactContext;

public class CoordAirMapView extends android.support.design.widget.CoordinatorLayout {
//    public AirMapView airMapView;
public CoordAirMapManager manager;

    public CoordAirMapView(Context context, CoordAirMapManager coordAirMapManager) {
        super(context);
        this.manager = coordAirMapManager;
        init();
    }


    private void init() {
        View view = View.inflate(getContext(), R.layout.coordinator, this);
        final CoordAirMapView coordAirMapView = this;

        view.findViewById(R.id.headerClick).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                WritableMap event = new WritableNativeMap();
                event.putString("message", "Cliccato Header");
                manager.pushEvent((ThemedReactContext) getContext(), coordAirMapView, "clickHeader", event);

            }
        });
    }
}
