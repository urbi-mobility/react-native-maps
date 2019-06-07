package com.airbnb.android.react.maps;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
//import com.facebook.react.uimanager.ThemedReactContext;

public class CoordAirMapView extends android.support.design.widget.CoordinatorLayout {
//    public AirMapView airMapView;


    public CoordAirMapView(Context context) {
        super(context);
        init();
    }

    public CoordAirMapView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public CoordAirMapView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }
//    public CoordAirMapView(ThemedReactContext reactContext,
//                           AirMapManager manager) {
//        super(reactContext);
//        init();
////        airMapView.setExtra(manager);
//    }


    private void init() {
        View view = View.inflate(getContext(), R.layout.coordinator, this);
//        airMapView = view.findViewById(R.id.airMap);
    }
}
