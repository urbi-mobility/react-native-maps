package com.airbnb.android.react.maps;

import android.content.Context;

import com.google.android.gms.maps.model.MarkerOptions;

public class AirMapUrbiMarker extends AirMapMarker {
  public AirMapUrbiMarker(Context context, AirMapMarkerManager markerManager) {
    super(context, markerManager);
  }

  public AirMapUrbiMarker(Context context, MarkerOptions options, AirMapMarkerManager markerManager) {
    super(context, options, markerManager);
  }
}
