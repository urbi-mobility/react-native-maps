package com.airbnb.android.react.maps;

import android.content.Context;

import com.facebook.react.views.view.ReactViewGroup;
import com.google.android.gms.maps.GoogleMap;

public abstract class AirMapFeature extends ReactViewGroup {
  public AirMapFeature(Context context) {
    super(context);
  }

  public abstract void addToMap(GoogleMap map, AirMapView view);

  public abstract void removeFromMap(GoogleMap map);

  public abstract Object getFeature();
}
