package com.airbnb.android.react.maps;

import com.facebook.react.bridge.ReadableArray;
import com.facebook.react.uimanager.ThemedReactContext;
import com.facebook.react.uimanager.annotations.ReactProp;
import com.google.android.gms.maps.model.LatLng;

import java.util.HashMap;
import java.util.Map;

public class AirMapUrbiMarkerManager extends AirMapMarkerManager {

  private Map<String, String> imageIds;

  @Override
  public String getName() {
    return "AIRMapUrbiMarker";
  }

  @Override
  public AirMapMarker createViewInstance(ThemedReactContext context) {
    return new AirMapUrbiMarker(context, this);
  }

  public void setImageIds(Map<String, ?> imageIds) {
    this.imageIds = new HashMap<>();
    for (Map.Entry<String, ?> entry : imageIds.entrySet()) {
      if (entry.getValue() instanceof String) {
        this.imageIds.put(entry.getKey(), (String) entry.getValue());
      }
    }
  }

  @ReactProp(name = "uId")
  public void setUId(AirMapUrbiMarker view, String uId) {
    view.setIdentifier(uId);
  }

  @ReactProp(name = "img")
  public void setImg(AirMapUrbiMarker view, String img) {
    String imageUrl = imageIds.get(img);
    if (imageUrl != null) {
      view.setImage(imageUrl);
    }
  }

  @ReactProp(name = "c")
  public void setCoordinates(AirMapUrbiMarker view, ReadableArray coords) {
    if (coords != null) {
      try {
        view.setCoordinate(new LatLng(coords.getDouble(0), coords.getDouble(1)));
      } catch (NullPointerException e) {
        // something went wrong while converting the array from the JS side to native, don't crash!
      }
    }
  }
}
