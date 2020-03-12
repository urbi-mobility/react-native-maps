package com.airbnb.android.react.maps;

import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;

import static java.lang.Math.PI;
import static java.lang.Math.cos;

public class LatLngBoundsUtils {
  public static final int METERS_IN_LAT_DEGREE = 111_100;
  public static final int LAT_TO_LON_FACTOR = 111320;

  public static boolean BoundsAreDifferent(LatLngBounds a, LatLngBounds b) {
    LatLng centerA = a.getCenter();
    double latA = centerA.latitude;
    double lngA = centerA.longitude;
    double latDeltaA = a.northeast.latitude - a.southwest.latitude;
    double lngDeltaA = a.northeast.longitude - a.southwest.longitude;

    LatLng centerB = b.getCenter();
    double latB = centerB.latitude;
    double lngB = centerB.longitude;
    double latDeltaB = b.northeast.latitude - b.southwest.latitude;
    double lngDeltaB = b.northeast.longitude - b.southwest.longitude;

    double latEps = LatitudeEpsilon(a, b);
    double lngEps = LongitudeEpsilon(a, b);

    return
        different(latA, latB, latEps) ||
            different(lngA, lngB, lngEps) ||
            different(latDeltaA, latDeltaB, latEps) ||
            different(lngDeltaA, lngDeltaB, lngEps);
  }

  private static boolean different(double a, double b, double epsilon) {
    return Math.abs(a - b) > epsilon;
  }

  private static double LatitudeEpsilon(LatLngBounds a, LatLngBounds b) {
    double sizeA = a.northeast.latitude - a.southwest.latitude; // something mod 180?
    double sizeB = b.northeast.latitude - b.southwest.latitude; // something mod 180?
    double size = Math.min(Math.abs(sizeA), Math.abs(sizeB));
    return size / 2560;
  }

  private static double LongitudeEpsilon(LatLngBounds a, LatLngBounds b) {
    double sizeA = a.northeast.longitude - a.southwest.longitude;
    double sizeB = b.northeast.longitude - b.southwest.longitude;
    double size = Math.min(Math.abs(sizeA), Math.abs(sizeB));
    return size / 2560;
  }

  public static double getMaxLatLng(LatLngBounds bounds) {
    return Math.max(bounds.northeast.latitude - bounds.southwest.latitude,
            bounds.northeast.longitude - bounds.southwest.longitude);
  }

  /**
   * Converts a distance in meters to a distance in latitude and longitude, with a raw
   * approximation.
   *
   * @param latitude         the latitude of the origin point
   * @param distanceInMeters the distance in meters
   * @return the distance as a latitude, longitude pair
   */
  public static LatLng toLatLonDistance(double latitude, double distanceInMeters) {

    double deltaLat = distanceInMeters / METERS_IN_LAT_DEGREE;

    double metersInLongitudeDegree = LAT_TO_LON_FACTOR * cos(latitude / 180.0 * PI);
    double deltaLon = distanceInMeters / metersInLongitudeDegree;

    return new LatLng(deltaLat, deltaLon);
  }
}
