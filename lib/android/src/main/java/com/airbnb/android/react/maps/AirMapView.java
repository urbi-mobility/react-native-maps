package com.airbnb.android.react.maps;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.IntentSender;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.PorterDuff;
import android.location.Location;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.util.Pair;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;

import androidx.annotation.NonNull;
import androidx.core.view.GestureDetectorCompat;
import androidx.core.view.MotionEventCompat;

import com.facebook.react.bridge.ActivityEventListener;
import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.BaseActivityEventListener;
import com.facebook.react.bridge.LifecycleEventListener;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReadableArray;
import com.facebook.react.bridge.ReadableMap;
import com.facebook.react.bridge.WritableArray;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.bridge.WritableNativeArray;
import com.facebook.react.bridge.WritableNativeMap;
import com.facebook.react.uimanager.ThemedReactContext;
import com.facebook.react.uimanager.UIManagerModule;
import com.facebook.react.uimanager.events.EventDispatcher;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.common.api.CommonStatusCodes;
import com.google.android.gms.common.api.ResolvableApiException;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResponse;
import com.google.android.gms.location.LocationSettingsStatusCodes;
import com.google.android.gms.location.SettingsClient;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.GoogleMapOptions;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.Projection;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.GroundOverlay;
import com.google.android.gms.maps.model.IndoorBuilding;
import com.google.android.gms.maps.model.IndoorLevel;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PointOfInterest;
import com.google.android.gms.maps.model.Polygon;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.TileOverlay;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.maps.android.data.kml.KmlContainer;
import com.google.maps.android.data.kml.KmlLayer;
import com.google.maps.android.data.kml.KmlPlacemark;
import com.google.maps.android.data.kml.KmlStyle;

import org.json.JSONObject;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.io.InputStream;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;

import static androidx.core.content.PermissionChecker.checkSelfPermission;
import static com.airbnb.android.react.maps.LatLngBoundsUtils.toLatLonDistance;
import static java.lang.String.format;
import static java.util.Arrays.asList;
import static java.util.Locale.ENGLISH;

public class AirMapView extends MapView implements GoogleMap.InfoWindowAdapter,
        GoogleMap.OnMarkerDragListener, OnMapReadyCallback, GoogleMap.OnPoiClickListener, GoogleMap.OnIndoorStateChangeListener {
  public GoogleMap map;
  private FusedLocationProviderClient fusedLocationClient;
  private KmlLayer kmlLayer;
  private ProgressBar mapLoadingProgressBar;
  private RelativeLayout mapLoadingLayout;
  private ImageView cacheImageView;
  private Boolean isMapLoaded = false;
  private Integer loadingBackgroundColor = null;
  private Integer loadingIndicatorColor = null;
  private final int baseMapPadding = 50;

  /**
   * urbi-specific fields
   */
  public static final double PIN_SCALE_FACTOR = 0.8;
  private static final int RADAR_CIRCLE_COLOR = Color.parseColor("#26EC008B");
  private static final int RADAR_CIRCLE_CENTER_COLOR = Color.parseColor("#AAEC008B");
  /**
   * Used as requestCode when enabling location services via startActivityForResult.
   */
  public static int LOCATION_SERVICES_ENABLE_REQUEST_CODE = 3100;
  private static final OkHttpClient HTTP_CLIENT = new OkHttpClient();
  private static final MediaType MIME_JSON = MediaType.parse("application/json");
  public GoogleMap mapToBeCleared;
  private float switchToCityPinsDelta = Float.MAX_VALUE;
  private final Set<AirMapMarker> allMarkers = new HashSet<>();
  private final Map<LatLng, AirMapCity> cities = new HashMap<>();
  private final Map<Marker, AirMapCity> cityPins = new HashMap<>();
  private AirMapCity lastCity;
  private AirMapCity lastCityWithMarkers;
  private boolean showingProviderMarkers;
  private String beURL = "";
  private String auth = "";
  private int showPathIfCloserThanSeconds;
  private Location lastLocation = null;
  private LocationCallback locationCallback;
  private LatLngBounds lastCameraBounds;
  private double lastLatLng;
  private boolean locationUpdatesStartCalled;
  private boolean locationServicesEnableInProgress;
  private boolean userDeniedLocationServicesEnable;
  private ActivityEventListener activityEventListener;
  private AirMapMarker selectedMarker;
  AirMapPaddingListener paddingListener;
  private Circle radarCircle;
  private Marker radarCircleCenter;
  private BitmapDescriptor radarCircleCenterImage;
  private LatLng radarCenter;
  private int radarRadius;
  /**
   * end of urbi-specific fields
   */

  private LatLngBounds boundsToMove;
  private CameraUpdate cameraToSet;
  private boolean showUserLocation = false;
  private boolean handlePanDrag = false;
  private boolean moveOnMarkerPress = true;
  private boolean cacheEnabled = false;
  private boolean initialRegionSet = false;
  private boolean initialCameraSet = false;
  private LatLngBounds cameraLastIdleBounds;
  private int cameraMoveReason = 0;

  private static final String[] PERMISSIONS = new String[]{
          "android.permission.ACCESS_FINE_LOCATION", "android.permission.ACCESS_COARSE_LOCATION"};

  private final List<AirMapFeature> features = new ArrayList<>();
  private final Map<Marker, AirMapMarker> markerMap = new HashMap<>();
  private final Map<Polyline, AirMapPolyline> polylineMap = new HashMap<>();
  private final Map<Polygon, AirMapPolygon> polygonMap = new HashMap<>();
  private final Map<GroundOverlay, AirMapOverlay> overlayMap = new HashMap<>();
  private final Map<Circle, AirMapCircle> circleMap = new HashMap<>();
  private final Map<TileOverlay, AirMapHeatmap> heatmapMap = new HashMap<>();
  private final Map<TileOverlay, AirMapGradientPolyline> gradientPolylineMap = new HashMap<>();
  private GestureDetectorCompat gestureDetector;
  private AirMapManager manager;
  private LifecycleEventListener lifecycleListener;
  private boolean paused = false;
  private boolean destroyed = false;
  private final ThemedReactContext context;
  private EventDispatcher eventDispatcher;

  private ViewAttacherGroup attacherGroup;

  private static boolean contextHasBug(Context context) {
    return context == null ||
            context.getResources() == null ||
            context.getResources().getConfiguration() == null;
  }

  // We do this to fix this bug:
  // https://github.com/react-native-community/react-native-maps/issues/271
  //
  // which conflicts with another bug regarding the passed in context:
  // https://github.com/react-native-community/react-native-maps/issues/1147
  //
  // Doing this allows us to avoid both bugs.
  private static Context getNonBuggyContext(ThemedReactContext reactContext,
                                            ReactApplicationContext appContext) {
    Context superContext = reactContext;
    if (!contextHasBug(appContext.getCurrentActivity())) {
      superContext = appContext.getCurrentActivity();
    } else if (contextHasBug(superContext)) {
      // we have the bug! let's try to find a better context to use
      if (!contextHasBug(reactContext.getCurrentActivity())) {
        superContext = reactContext.getCurrentActivity();
      } else if (!contextHasBug(reactContext.getApplicationContext())) {
        superContext = reactContext.getApplicationContext();
      } else {
        // ¯\_(ツ)_/¯
      }
    }
    return superContext;
  }

  public AirMapView(ThemedReactContext reactContext, ReactApplicationContext appContext,
                    AirMapManager manager,
                    GoogleMapOptions googleMapOptions) {
    super(getNonBuggyContext(reactContext, appContext), googleMapOptions);

    this.manager = manager;
    this.context = reactContext;

    if (activityEventListener != null) {
      appContext.removeActivityEventListener(activityEventListener);
    }

    activityEventListener = new BaseActivityEventListener() {
      @Override
      public void onActivityResult(Activity activity, int requestCode, int resultCode, Intent data) {
        onLocationServicesEnableResponse(resultCode);
      }
    };

    appContext.addActivityEventListener(activityEventListener);

    initView();

  }

  public void setExtra(AirMapManager manager) {
    this.manager = manager;
  }

  private void initView() {
    fusedLocationClient = LocationServices.getFusedLocationProviderClient(context);
    super.onCreate(null);
    // TODO(lmr): what about onStart????
    super.onResume();
    if (manager.singleInstance && mapToBeCleared != null) {
      mapToBeCleared.clear();
      mapToBeCleared = null;
      map = null;
    }
    super.getMapAsync(this);
    gestureDetector =
            new GestureDetectorCompat(context, new GestureDetector.SimpleOnGestureListener() {

              @Override
              public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX,
                                      float distanceY) {
                if (handlePanDrag) {
                  onPanDrag(e2);
                }
                return false;
              }

              @Override
              public boolean onDoubleTap(MotionEvent ev) {
                onDoublePress(ev);
                return false;
              }
            });
    this.addOnLayoutChangeListener(new OnLayoutChangeListener() {
      @Override
      public void onLayoutChange(View v, int left, int top, int right, int bottom,
                                 int oldLeft, int oldTop, int oldRight, int oldBottom) {
        if (!paused) {
          AirMapView.this.cacheView();
        }
      }
    });

    eventDispatcher = context.getNativeModule(UIManagerModule.class).getEventDispatcher();

    // Set up a parent view for triggering visibility in subviews that depend on it.
    // Mainly ReactImageView depends on Fresco which depends on onVisibilityChanged() event
    attacherGroup = new ViewAttacherGroup(context);
    LayoutParams attacherLayoutParams = new LayoutParams(0, 0);
    attacherLayoutParams.width = 0;
    attacherLayoutParams.height = 0;
    attacherLayoutParams.leftMargin = 99999999;
    attacherLayoutParams.topMargin = 99999999;
    attacherGroup.setLayoutParams(attacherLayoutParams);

    addView(attacherGroup);

  }

  private BitmapDescriptor getRadarCircleCenterImage() {
    if (radarCircleCenterImage == null) {
      try {
        Paint radarCenterPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        radarCenterPaint.setColor(Color.parseColor("#EC008B"));
        float dp = getResources().getDisplayMetrics().density;
        Bitmap img = Bitmap.createBitmap((int) (10 * dp), (int) (10 * dp), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(img);
        canvas.drawCircle(5 * dp, 5 * dp, 5 * dp, radarCenterPaint);

        radarCircleCenterImage = BitmapDescriptorFactory.fromBitmap(img);
      } catch (Exception e) {
        Log.e("urbi", "couldn't initialize radar circle center image");
      }
    }

    return radarCircleCenterImage;
  }

  /**
   * Listens for responses to our requests to enable location services.
   */
  public void onLocationServicesEnableResponse(int resultCode) {
    locationServicesEnableInProgress = false;
    userDeniedLocationServicesEnable = resultCode != Activity.RESULT_OK;
    if (!userDeniedLocationServicesEnable) {
      // we need to start again, we just enabled the location services
      locationUpdatesStartCalled = false;
      startLocationUpdates(false);
    }

    WritableMap event = new WritableNativeMap();
    event.putBoolean("allowed", !userDeniedLocationServicesEnable);
    manager.pushEvent(context, this, "onLocationServicesEnableResponse", event);
  }

  @SuppressLint("MissingPermission")
  private void startLocationUpdates(boolean forceLocationServicesDialog) {
    if (!locationUpdatesStartCalled && hasPermissions() && context.getCurrentActivity() != null && !locationServicesEnableInProgress) {
      locationUpdatesStartCalled = true;
      // reset lastLocation so that when centerToUserLocation() is called we'll ask the user to enable the location services again
      lastLocation = null;
      if (locationCallback == null) {
        locationCallback = createLocationCallBack();
      }
      fusedLocationClient.requestLocationUpdates(createLocationRequest(context.getCurrentActivity(), forceLocationServicesDialog), locationCallback, Looper.getMainLooper());
    }
  }

  private LocationRequest createLocationRequest(Activity currentActivity, final boolean forceDialog) {
    LocationRequest locationRequest = LocationRequest.create();
    locationRequest.setInterval(100000);
    locationRequest.setFastestInterval(20000);
    locationRequest.setMaxWaitTime(1500000);
    locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

    LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder()
            .addLocationRequest(locationRequest)
            .setAlwaysShow(true);

    SettingsClient client = LocationServices.getSettingsClient(currentActivity);

    Task<LocationSettingsResponse> task = client.checkLocationSettings(builder.build());

    task.addOnSuccessListener(context.getCurrentActivity(), new OnSuccessListener<LocationSettingsResponse>() {
      @Override
      public void onSuccess(LocationSettingsResponse locationSettingsResponse) {
        if (hasPermissions()) {
          locationUpdatesStartCalled = false;
          locationServicesEnableInProgress = false;
          map.setMyLocationEnabled(showUserLocation);
        }
      }
    });

    task.addOnFailureListener(context.getCurrentActivity(), new OnFailureListener() {
      @Override
      public void onFailure(@NonNull Exception e) {
        int statusCode = ((ApiException) e).getStatusCode();
        switch (statusCode) {
          case CommonStatusCodes.RESOLUTION_REQUIRED:
            // Location settings are not satisfied, but this can be fixed
            // by showing the user a dialog, unless they've already said no
            if (forceDialog || !userDeniedLocationServicesEnable) {
              try {
                // Show the dialog by calling startResolutionForResult(),
                // and check the result in onActivityResult().
                ResolvableApiException resolvable = (ResolvableApiException) e;
                locationServicesEnableInProgress = true;
                resolvable.startResolutionForResult(context.getCurrentActivity(), LOCATION_SERVICES_ENABLE_REQUEST_CODE);
              } catch (IntentSender.SendIntentException sendEx) {
                Log.e("createLocationRequest", Log.getStackTraceString(sendEx));
              }
              break;
            }
          case LocationSettingsStatusCodes.SETTINGS_CHANGE_UNAVAILABLE:
            Log.e("createLocationRequest", "SETTINGS CHANGE UNAVAILABLE");
            break;
        }
      }
    });

    return locationRequest;
  }

  private LocationCallback createLocationCallBack() {
    final AirMapView view = this;

    LocationCallback locationCallback = new LocationCallback() {
      @Override
      public void onLocationResult(LocationResult locationResult) {
        Location location = locationResult.getLastLocation();
        if (location != null) {
          view.lastLocation = location;
          WritableMap event = new WritableNativeMap();

          WritableMap coordinate = new WritableNativeMap();
          coordinate.putDouble("latitude", location.getLatitude());
          coordinate.putDouble("longitude", location.getLongitude());
          coordinate.putDouble("altitude", location.getAltitude());
          coordinate.putDouble("timestamp", location.getTime());
          coordinate.putDouble("accuracy", location.getAccuracy());
          coordinate.putDouble("speed", location.getSpeed());
          coordinate.putDouble("heading", location.getBearing());
          if (android.os.Build.VERSION.SDK_INT >= 18) {
            coordinate.putBoolean("isFromMockProvider", location.isFromMockProvider());
          }
          event.putMap("coordinate", coordinate);
          manager.pushEvent(context, view, "onUserLocationUpdate", event);
        }
      }

    };

    return locationCallback;
  }

  private void scaleDown(AirMapMarker airMapMarker) {

    // if there's no icon to rescale, skip everything
    Bitmap b = airMapMarker.getIconBitmap();
    if (b == null) return;

    Marker m = airMapMarker.getMarker();
    if (m != null && m.getTag() != null) {
      airMapMarker.getMarker().remove();
    }
    markerMap.remove(airMapMarker.getMarker());
    airMapMarker.setIconSelected(false);
    if (mustShowProviderMarkers()) {
      airMapMarker.addToMap(map, this);
      markerMap.put(airMapMarker.getMarker(), airMapMarker);
    }

  }

  private void setOriginalSize(AirMapMarker airMapMarker) {
    // if there's no icon to rescale, skip everything
    if (airMapMarker.getOriginalBitmapDescriptor() == null) return;

    Marker m = airMapMarker.getMarker();
    if (m != null && m.getTag() != null) {
      airMapMarker.getMarker().remove();
    }
    markerMap.remove(airMapMarker.getMarker());

    airMapMarker.setIconSelected(true);
    airMapMarker.addToMap(map, this);
    markerMap.put(airMapMarker.getMarker(), airMapMarker);
  }

  private void onCameraBoundsUpdated(LatLngBounds bounds) {

    double maxLatLng = LatLngBoundsUtils.getMaxLatLng(bounds);
    AirMapCity newCity = getCity(bounds);

    if (lastCameraBounds != null) {
      double lastMaxLatLng = LatLngBoundsUtils.getMaxLatLng(lastCameraBounds);

      if (maxLatLng > switchToCityPinsDelta && lastMaxLatLng < switchToCityPinsDelta) {
        // switch to city markers
        map.clear();
        showingProviderMarkers = false;
        cityPins.clear();
        if (lastCity != null) {
          lastCityWithMarkers = lastCity;
        }
        for (AirMapCity city : cities.values()) {
          cityPins.put(map.addMarker(city.getMarker().getMarkerOptions()), city);
        }
        addAllPolygons();
        addAllPolylines();
        addAllCircles();
        setRadarCircle(radarCenter, radarRadius);
      } else if (lastMaxLatLng > switchToCityPinsDelta && maxLatLng < switchToCityPinsDelta) {
        // switch to provider markers
        map.clear();
        // if we're still in the same city, add back all provider markers
        if (lastCityWithMarkers != null && lastCityWithMarkers.equals(newCity)) {
          readdProviderMarkers();
        } else if (newCity != null) {
          markerMap.clear();
          allMarkers.clear();
        }
      }
    }

    if (maxLatLng < switchToCityPinsDelta &&
            (lastCity == null && newCity != null || lastCity != null && !lastCity.equals(newCity))) {
      lastCity = newCity;
      WritableMap map = new WritableNativeMap();
      map.putString("city", newCity == null ? "unset" : newCity.getId());
      manager.pushEvent(context, this, "onCityChange", map);
      if (!showingProviderMarkers && maxLatLng < switchToCityPinsDelta && newCity != null && newCity.equals(lastCityWithMarkers)) {
        readdProviderMarkers();
      } else if (newCity != null) {
        allMarkers.clear();
      }
    }

    lastCameraBounds = bounds;
    lastLatLng = maxLatLng;
    cameraLastIdleBounds = bounds;

    eventDispatcher.dispatchEvent(new RegionChangeEvent(getId(), bounds, false));
  }

  @Override
  public void onMapReady(final GoogleMap map) {
    if (destroyed) {
      return;
    }
    if (manager.singleInstance) {
      mapToBeCleared = map;
    }
    this.map = map;
    map.setInfoWindowAdapter(this);
    map.setOnMarkerDragListener(this);
    map.setOnPoiClickListener(this);
    map.setOnIndoorStateChangeListener(this);

    manager.pushEvent(context, this, "onMapReady", new WritableNativeMap());

    final AirMapView view = this;

    map.setOnMyLocationChangeListener(new GoogleMap.OnMyLocationChangeListener() {
      @Override
      public void onMyLocationChange(Location location) {
        WritableMap event = new WritableNativeMap();

        WritableMap coordinate = new WritableNativeMap();
        coordinate.putDouble("latitude", location.getLatitude());
        coordinate.putDouble("longitude", location.getLongitude());
        coordinate.putDouble("altitude", location.getAltitude());
        coordinate.putDouble("timestamp", location.getTime());
        coordinate.putDouble("accuracy", location.getAccuracy());
        coordinate.putDouble("speed", location.getSpeed());
        coordinate.putDouble("heading", location.getBearing());
        if(android.os.Build.VERSION.SDK_INT >= 18){
          coordinate.putBoolean("isFromMockProvider", location.isFromMockProvider());
        }

        event.putMap("coordinate", coordinate);

        manager.pushEvent(context, view, "onUserLocationChange", event);
      }
    });

    if (manager.isLiteMode()) {
      map.setOnMarkerClickListener(new GoogleMap.OnMarkerClickListener() {
        @Override
        public boolean onMarkerClick(Marker marker) {
          return true;
        }
      });

      map.setOnMapClickListener(new GoogleMap.OnMapClickListener() {
        @Override
        public void onMapClick(LatLng point) {
          WritableMap event = makeClickEventData(point);
          event.putString("action", "press");
            manager.pushEvent(context, view, "onPress", event);
        }
      });
    } else {
      map.setOnMarkerClickListener(new GoogleMap.OnMarkerClickListener() {
        @Override
        public boolean onMarkerClick(Marker marker) {
          WritableMap event;
          AirMapCity city;

          if ((city = cityPins.get(marker)) != null) {
            event = makeClickEventData(marker.getPosition());
            event.putString("action", "marker-press");
            event.putString("id", city.getId());
            manager.pushEvent(context, view, "onCityPress", event);

            // TODO check if user's location is within the city, if so, zoom there instead
            LatLng centerTo = city.getPinPosition();
            map.animateCamera(CameraUpdateFactory.newLatLngZoom(centerTo, 16), 350, null);
            return true;
          }

          AirMapMarker airMapMarker = getMarkerMap(marker);
          if (airMapMarker == null) return false;

          event = makeClickEventData(marker.getPosition());
          event.putString("action", "marker-press");
          event.putString("id", airMapMarker.getIdentifier());
          manager.pushEvent(context, view, "onMarkerPress", event);

          event = makeClickEventData(marker.getPosition());
          event.putString("action", "marker-press");
          event.putString("id", airMapMarker.getIdentifier());
          manager.pushEvent(context, airMapMarker, "onPress", event);

          setSelectedMarker(airMapMarker);

          // Return false to open the callout info window and center on the marker
          // https://developers.google.com/android/reference/com/google/android/gms/maps/GoogleMap
          // .OnMarkerClickListener
          if (view.moveOnMarkerPress) {
            return false;
          } else {
            marker.showInfoWindow();
            centerCameraTo(marker.getPosition(), 150, null);
            return true;
          }
        }
      });

      map.setOnPolygonClickListener(new GoogleMap.OnPolygonClickListener() {
        @Override
        public void onPolygonClick(Polygon polygon) {
          WritableMap event = makeClickEventData(polygon.getPoints().get(0));
          event.putString("action", "polygon-press");
          manager.pushEvent(context, polygonMap.get(polygon), "onPress", event);
        }
      });

      map.setOnPolylineClickListener(new GoogleMap.OnPolylineClickListener() {
        @Override
        public void onPolylineClick(Polyline polyline) {
          WritableMap event = makeClickEventData(polyline.getPoints().get(0));
          event.putString("action", "polyline-press");
          manager.pushEvent(context, polylineMap.get(polyline), "onPress", event);
        }
      });

      map.setOnInfoWindowClickListener(new GoogleMap.OnInfoWindowClickListener() {
        @Override
        public void onInfoWindowClick(Marker marker) {
          WritableMap event;

          event = makeClickEventData(marker.getPosition());
          event.putString("action", "callout-press");
          manager.pushEvent(context, view, "onCalloutPress", event);

          event = makeClickEventData(marker.getPosition());
          event.putString("action", "callout-press");

          AirMapMarker markerView = getMarkerMap(marker);
          if (markerView != null) {
            manager.pushEvent(context, markerView, "onCalloutPress", event);
            event = makeClickEventData(marker.getPosition());
            event.putString("action", "callout-press");
            AirMapCallout infoWindow = markerView.getCalloutView();
            if (infoWindow != null) manager.pushEvent(context, infoWindow, "onPress", event);
          }

        }
      });

      map.setOnMapClickListener(new GoogleMap.OnMapClickListener() {
        @Override
        public void onMapClick(LatLng point) {
          WritableMap event = makeClickEventData(point);
          event.putString("action", "press");
          manager.pushEvent(context, view, "onPress", event);
          if (selectedMarker != null) {
            selectedMarker.setSelected(false);
            // setSelectedMarker(null);
          }
        }
      });

      map.setOnMapLongClickListener(new GoogleMap.OnMapLongClickListener() {
        @Override
        public void onMapLongClick(LatLng point) {
          WritableMap event = makeClickEventData(point);
          event.putString("action", "long-press");
          manager.pushEvent(context, view, "onLongPress", makeClickEventData(point));
        }
      });

      map.setOnGroundOverlayClickListener(new GoogleMap.OnGroundOverlayClickListener() {
        @Override
        public void onGroundOverlayClick(GroundOverlay groundOverlay) {
          WritableMap event = makeClickEventData(groundOverlay.getPosition());
          event.putString("action", "overlay-press");
          manager.pushEvent(context, overlayMap.get(groundOverlay), "onPress", event);
        }
      });
    }

    map.setOnCameraMoveStartedListener(new GoogleMap.OnCameraMoveStartedListener() {
      @Override
      public void onCameraMoveStarted(int reason) {
        cameraMoveReason = reason;
      }
    });

    map.setOnCameraMoveListener(new GoogleMap.OnCameraMoveListener() {
      @Override
      public void onCameraMove() {
        LatLngBounds bounds = map.getProjection().getVisibleRegion().latLngBounds;
        cameraLastIdleBounds = null;
        eventDispatcher.dispatchEvent(new RegionChangeEvent(getId(), bounds, true));
      }
    });

    map.setOnCameraIdleListener(new GoogleMap.OnCameraIdleListener() {
      @Override
      public void onCameraIdle() {
        LatLngBounds bounds = map.getProjection().getVisibleRegion().latLngBounds;
        if ((cameraMoveReason != 0) &&
                ((cameraLastIdleBounds == null) ||
                        LatLngBoundsUtils.BoundsAreDifferent(bounds, cameraLastIdleBounds))) {

          onCameraBoundsUpdated(bounds);
        }
      }
    });

    map.setOnMapLoadedCallback(new GoogleMap.OnMapLoadedCallback() {
      @Override
      public void onMapLoaded() {
        isMapLoaded = true;
        manager.pushEvent(context, view, "onMapLoaded", new WritableNativeMap());
        AirMapView.this.cacheView();
        LatLngBounds bounds = map.getProjection().getVisibleRegion().latLngBounds;
        double maxLatLng = LatLngBoundsUtils.getMaxLatLng(bounds);
        if (maxLatLng > switchToCityPinsDelta) {
          for (AirMapCity city : cities.values()) {
            cityPins.put(map.addMarker(city.getMarker().getMarkerOptions()), city);
          }
        }
      }
    });

    // pause location updates when in BG
    if (lifecycleListener != null) {
      context.removeLifecycleEventListener(lifecycleListener);
    }

    // We need to be sure to disable location-tracking when app enters background, in-case some
    // other module
    // has acquired a wake-lock and is controlling location-updates, otherwise, location-manager
    // will be left
    // updating location constantly, killing the battery, even though some other location-mgmt
    // module may
    // desire to shut-down location-services.
    lifecycleListener = new LifecycleEventListener() {
      @SuppressLint("MissingPermission")
      @Override
      public void onHostResume() {
        if (hasPermissions() && !manager.isLiteMode() && context.getCurrentActivity() != null && !locationServicesEnableInProgress) {
          startLocationUpdates(false);
        }
        synchronized (AirMapView.this) {
          if (!destroyed) {
            Log.i("urbi", "onResume()");
            AirMapView.this.onResume();
          }
          paused = false;
        }
      }

      @SuppressLint("MissingPermission")
      @Override
      public void onHostPause() {
        if (hasPermissions()) {
          map.setMyLocationEnabled(false);
          if (locationCallback != null) {
            fusedLocationClient.removeLocationUpdates(locationCallback);
            locationUpdatesStartCalled = false;
          }
        }
        synchronized (AirMapView.this) {
          if (!destroyed) {
            /*
             * detach all AirMapMarkers from the ViewGroup: only BaseSavedState.EMPTY_STATE are
             * saved for each marker, but they still add up to more than 500kB total when committing
             * the bundles when onSaveInstanceState() is called.
             *
             * We're adding all markers back anyway onHostResume()
             */
            if (attacherGroup != null) {
              attacherGroup.removeAllViews();
            }
            AirMapView.this.onPause();
          }
          paused = true;
        }
      }

      @Override
      public void onHostDestroy() {
        AirMapView.this.doDestroy();
      }
    };

    context.addLifecycleEventListener(lifecycleListener);
  }


  private void centerCameraTo(final LatLng position, final int delayMs, Integer newZoomLevel) {
    if (map != null) {
      if (newZoomLevel != null) {
        map.animateCamera(CameraUpdateFactory.newLatLngZoom(position, newZoomLevel));
      } else {
        map.animateCamera(
                CameraUpdateFactory.newLatLng(position),
                delayMs, null);
      }
    }
  }

  private void addAllPolygons() {
    Map<Polygon, AirMapPolygon> newMap = new HashMap<>();
    for (AirMapPolygon polygon : polygonMap.values()) {
      polygon.addToMap(map, this);
      newMap.put((Polygon)(polygon.getFeature()), polygon);
    }
    polygonMap.clear();
    polygonMap.putAll(newMap);
  }

  private void addAllPolylines() {
    Map<Polyline, AirMapPolyline> newMap = new HashMap<>();
    for (AirMapPolyline polyline : polylineMap.values()) {
      polyline.addToMap(map, this);
      newMap.put((Polyline)(polyline.getFeature()), polyline);
    }
    polylineMap.clear();
    polylineMap.putAll(newMap);
  }

  private void addAllCircles() {
    Map<Circle, AirMapCircle> newMap = new HashMap<>();
    for (AirMapCircle circle : circleMap.values()) {
      circle.addToMap(map, this);
      newMap.put((Circle)(circle.getFeature()), circle);
    }
    circleMap.clear();
    circleMap.putAll(newMap);
  }

  private void readdProviderMarkers() {
    markerMap.clear();
    for (AirMapMarker m : allMarkers) {
      m.readdToMapIfNotFilteredOut(map);
      Marker marker = m.getMarker();
      if (marker != null)
        markerMap.put(marker, m);
    }
    addAllPolygons();
    addAllPolylines();
    addAllCircles();
    setRadarCircle(radarCenter, radarRadius);
    showingProviderMarkers = true;
  }

  private AirMapCity getCity(LatLngBounds bounds) {

    for (AirMapCity city : cities.values()) {
      if (city.getBounds().contains(bounds.northeast) ||
              city.getBounds().contains(bounds.southwest)) {
        return city;
      }
    }

    return null;
  }

  @SuppressLint("WrongConstant")
  private boolean hasPermissions() {
    return checkSelfPermission(getContext(), PERMISSIONS[0]) == PackageManager.PERMISSION_GRANTED ||
            checkSelfPermission(getContext(), PERMISSIONS[1]) == PackageManager.PERMISSION_GRANTED;
  }


  /*
  onDestroy is final method so I can't override it.
   */
  public synchronized void doDestroy() {
    Log.i("URBI", "doDestroy(), destroyed: " + destroyed);
    if (destroyed) {
      return;
    }
    destroyed = true;
    if (map != null) {
      map.clear();
    }

    if (lifecycleListener != null && context != null) {
      context.removeLifecycleEventListener(lifecycleListener);
      lifecycleListener = null;
    }
    if (!paused) {
      onPause();
      paused = true;
    }
    onDestroy();
  }

  public void setInitialRegion(ReadableMap initialRegion) {
    if (!initialRegionSet && initialRegion != null) {
      setRegion(initialRegion);
      initialRegionSet = true;
    }
  }

  public void setInitialCamera(ReadableMap initialCamera) {
    if (!initialCameraSet && initialCamera != null) {
      setCamera(initialCamera);
      initialCameraSet = true;
    }
  }

  public void setRegion(ReadableMap region) {
    if (region == null) return;

    Double lng = region.getDouble("longitude");
    Double lat = region.getDouble("latitude");
    Double lngDelta = region.getDouble("longitudeDelta");
    Double latDelta = region.getDouble("latitudeDelta");
    LatLngBounds bounds = new LatLngBounds(
            new LatLng(lat - latDelta / 2, lng - lngDelta / 2), // southwest
            new LatLng(lat + latDelta / 2, lng + lngDelta / 2)  // northeast
    );
    if (super.getHeight() <= 0 || super.getWidth() <= 0) {
      // in this case, our map has not been laid out yet, so we save the bounds in a local
      // variable, and make a guess of zoomLevel 10. Not to worry, though: as soon as layout
      // occurs, we will move the camera to the saved bounds. Note that if we tried to move
      // to the bounds now, it would trigger an exception.
      map.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(lat, lng), 10));
      boundsToMove = bounds;
    } else {
      map.moveCamera(CameraUpdateFactory.newLatLngBounds(bounds, 0));
      boundsToMove = null;
    }
  }

  public void setCamera(ReadableMap camera) {
    if (camera == null) return;

    CameraPosition.Builder builder = new CameraPosition.Builder();

    ReadableMap center = camera.getMap("center");
    if (center != null) {
      Double lng = center.getDouble("longitude");
      Double lat = center.getDouble("latitude");
      builder.target(new LatLng(lat, lng));
    }

    builder.tilt((float) camera.getDouble("pitch"));
    builder.bearing((float) camera.getDouble("heading"));
    builder.zoom(camera.getInt("zoom"));

    CameraUpdate update = CameraUpdateFactory.newCameraPosition(builder.build());

    if (super.getHeight() <= 0 || super.getWidth() <= 0) {
      // in this case, our map has not been laid out yet, so we save the camera update in a
      // local variable. As soon as layout occurs, we will move the camera to the saved update.
      // Note that if we tried to move to the camera now, it would trigger an exception.
      cameraToSet = update;
    } else {
      map.moveCamera(update);
      cameraToSet = null;
    }
  }

  @SuppressLint("MissingPermission")
  public void centerToUserLocation(boolean fromButtonPress) {
    if (hasPermissions()) {
      if (lastLocation != null) {
        centerCameraTo(new LatLng(lastLocation.getLatitude(), lastLocation.getLongitude()), 600, 16);
      } else if (!locationServicesEnableInProgress) {
        // show the "enable location services" dialog if they're not enabled
        locationUpdatesStartCalled = false;
        // and if the user pressed a button, do it regardless of whether the user recently rejected it
        startLocationUpdates(fromButtonPress);
      }
    } else {
      WritableMap event = new WritableNativeMap();
      event.putString("permission", "location");
      manager.pushEvent(context, this, "onPermissionsNeeded", event);
    }
  }

  public void centerTo(LatLng coordinates) {
    centerCameraTo(coordinates, 600, 16);
  }

  public void setRadarCircle(LatLng center, int radius) {
    if (center != null) {
      hideRadarCircle(); // this should do nothing if all goes well, but...
      if (map != null) {
        radarCircle = map.addCircle(new CircleOptions().center(center).radius(radius).fillColor(RADAR_CIRCLE_COLOR).strokeWidth(0));
        BitmapDescriptor radarCircleCenterImage = getRadarCircleCenterImage();
        if (radarCircleCenterImage != null) {
          radarCircleCenter = map.addMarker(new MarkerOptions().icon(radarCircleCenterImage).anchor(0.5f, 0.5f).position(center));
        }
        radarCenter = center;
        radarRadius = radius;
      }
    }
  }

  public void hideRadarCircle() {
    if (radarCircle != null) {
      radarCircle.remove();
      radarCircleCenter.remove();
    }
    radarCircle = null;
    radarCircleCenter = null;
    radarCenter = null;
    radarRadius = 0;
  }

  @SuppressLint("MissingPermission")
  public void setShowsUserLocation(boolean showUserLocation) {
    this.showUserLocation = showUserLocation; // hold onto this for lifecycle handling
    if (hasPermissions()) {
      map.setMyLocationEnabled(showUserLocation);
    }
  }

  public void setShowsMyLocationButton(boolean showMyLocationButton) {
    if (hasPermissions() || !showMyLocationButton) {
      map.getUiSettings().setMyLocationButtonEnabled(showMyLocationButton);
    }
  }

  public void setToolbarEnabled(boolean toolbarEnabled) {
    if (hasPermissions() || !toolbarEnabled) {
      map.getUiSettings().setMapToolbarEnabled(toolbarEnabled);
    }
  }

  public void setCacheEnabled(boolean cacheEnabled) {
    this.cacheEnabled = cacheEnabled;
    this.cacheView();
  }

  public void enableMapLoading(boolean loadingEnabled) {
    if (loadingEnabled && !this.isMapLoaded) {
      this.getMapLoadingLayoutView().setVisibility(View.VISIBLE);
    }
  }

  public void setMoveOnMarkerPress(boolean moveOnPress) {
    this.moveOnMarkerPress = moveOnPress;
  }

  public void setLoadingBackgroundColor(Integer loadingBackgroundColor) {
    this.loadingBackgroundColor = loadingBackgroundColor;

    if (this.mapLoadingLayout != null) {
      if (loadingBackgroundColor == null) {
        this.mapLoadingLayout.setBackgroundColor(Color.WHITE);
      } else {
        this.mapLoadingLayout.setBackgroundColor(this.loadingBackgroundColor);
      }
    }
  }

  public void setLoadingIndicatorColor(Integer loadingIndicatorColor) {
    this.loadingIndicatorColor = loadingIndicatorColor;
    if (this.mapLoadingProgressBar != null) {
      Integer color = loadingIndicatorColor;
      if (color == null) {
        color = Color.parseColor("#606060");
      }

      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
        ColorStateList progressTintList = ColorStateList.valueOf(loadingIndicatorColor);
        ColorStateList secondaryProgressTintList = ColorStateList.valueOf(loadingIndicatorColor);
        ColorStateList indeterminateTintList = ColorStateList.valueOf(loadingIndicatorColor);

        this.mapLoadingProgressBar.setProgressTintList(progressTintList);
        this.mapLoadingProgressBar.setSecondaryProgressTintList(secondaryProgressTintList);
        this.mapLoadingProgressBar.setIndeterminateTintList(indeterminateTintList);
      } else {
        PorterDuff.Mode mode = PorterDuff.Mode.SRC_IN;
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.GINGERBREAD_MR1) {
          mode = PorterDuff.Mode.MULTIPLY;
        }
        if (this.mapLoadingProgressBar.getIndeterminateDrawable() != null)
          this.mapLoadingProgressBar.getIndeterminateDrawable().setColorFilter(color, mode);
        if (this.mapLoadingProgressBar.getProgressDrawable() != null)
          this.mapLoadingProgressBar.getProgressDrawable().setColorFilter(color, mode);
      }
    }
  }

  public void setHandlePanDrag(boolean handlePanDrag) {
    this.handlePanDrag = handlePanDrag;
  }

  public void addFeature(View child, int index) {
    // Our desired API is to pass up annotations/overlays as children to the mapview component.
    // This is where we intercept them and do the appropriate underlying mapview action.
    if (child instanceof AirMapUrbiMarker) {
      addMarker((AirMapUrbiMarker) child, index);
    } else if (child instanceof AirMapMarker) {
      addMarker((AirMapMarker) child, index);
    } else if (child instanceof AirMapPolyline) {
      AirMapPolyline polylineView = (AirMapPolyline) child;
      polylineView.addToMap(map, this);
      features.add(index, polylineView);
      Polyline polyline = (Polyline) polylineView.getFeature();
      polylineMap.put(polyline, polylineView);
    } else if (child instanceof AirMapGradientPolyline) {
      AirMapGradientPolyline polylineView = (AirMapGradientPolyline) child;
      polylineView.addToMap(map,this);
      features.add(index, polylineView);
      TileOverlay tileOverlay = (TileOverlay) polylineView.getFeature();
      gradientPolylineMap.put(tileOverlay, polylineView);
    } else if (child instanceof AirMapPolygon) {
      AirMapPolygon polygonView = (AirMapPolygon) child;
      polygonView.addToMap(map, this);
      features.add(index, polygonView);
      Polygon polygon = (Polygon) polygonView.getFeature();
      polygonMap.put(polygon, polygonView);
    } else if (child instanceof AirMapCircle) {
      AirMapCircle circleView = (AirMapCircle) child;
      circleView.addToMap(map, this);
      features.add(index, circleView);
      Circle circle = (Circle) circleView.getFeature();
      circleMap.put(circle, circleView);
    } else if (child instanceof AirMapUrlTile) {
      AirMapUrlTile urlTileView = (AirMapUrlTile) child;
      urlTileView.addToMap(map, this);
      features.add(index, urlTileView);
    } else if (child instanceof AirMapWMSTile) {
      AirMapWMSTile urlTileView = (AirMapWMSTile) child;
      urlTileView.addToMap(map, this);
      features.add(index, urlTileView);
    } else if (child instanceof AirMapLocalTile) {
      AirMapLocalTile localTileView = (AirMapLocalTile) child;
      localTileView.addToMap(map, this);
      features.add(index, localTileView);
    } else if (child instanceof AirMapOverlay) {
      AirMapOverlay overlayView = (AirMapOverlay) child;
      overlayView.addToMap(map, this);
      features.add(index, overlayView);
      GroundOverlay overlay = (GroundOverlay) overlayView.getFeature();
      overlayMap.put(overlay, overlayView);
    } else if (child instanceof AirMapHeatmap) {
      AirMapHeatmap heatmapView = (AirMapHeatmap) child;
      heatmapView.addToMap(map,this);
      features.add(index, heatmapView);
      TileOverlay heatmap = (TileOverlay)heatmapView.getFeature();
      heatmapMap.put(heatmap, heatmapView);
    } else if (child instanceof ViewGroup) {
      ViewGroup children = (ViewGroup) child;
      for (int i = 0; i < children.getChildCount(); i++) {
        addFeature(children.getChildAt(i), index);
      }
    } else {
      addView(child, index);
    }
  }

  public void addMarkerToMap(AirMapMarker marker) {
    if (mustShowProviderMarkers()) {
      marker.addToMap(map, this);
      markerMap.put(marker.getMarker(), marker);
    }
  }

  private void addMarker(AirMapMarker annotation, int index) {

    allMarkers.add(annotation);

    if (mustShowProviderMarkers()) {
      annotation.addToMap(map, this);
    }
    features.add(index, annotation);

    // Allow visibility event to be triggered later
    int visibility = annotation.getVisibility();
    annotation.setVisibility(INVISIBLE);

    // Remove from a view group if already present, prevent "specified child
    // already had a parent" error.
    ViewGroup annotationParent = (ViewGroup) annotation.getParent();
    if (annotationParent != null) {
      annotationParent.removeView(annotation);
    }

    // Add to the parent group
    attacherGroup.addView(annotation);

    // Trigger visibility event if necessary.
    // With some testing, seems like it is not always
    //   triggered just by being added to a parent view.
    annotation.setVisibility(visibility);

    Marker marker = (Marker) annotation.getFeature();
    if (marker != null) {
      markerMap.put(marker, annotation);
    }
  }

  private boolean mustShowProviderMarkers() {
    return lastLatLng < switchToCityPinsDelta;
  }

  public int getFeatureCount() {
    return features.size();
  }

  public View getFeatureAt(int index) {
    return features.get(index);
  }

  public void removeFeatureAt(int index) {
    AirMapFeature feature = features.remove(index);
    if (feature instanceof AirMapMarker) {
      markerMap.remove(feature.getFeature());
      allMarkers.remove(feature);
      if (this.selectedMarker != null && this.selectedMarker == feature) {
        setSelectedMarker(null);
      }
    } else if (feature instanceof AirMapHeatmap) {
      heatmapMap.remove(feature.getFeature());
    } else if (feature instanceof AirMapPolygon) {
      polygonMap.remove(feature.getFeature());
    } else if (feature instanceof AirMapCircle) {
      circleMap.remove(feature.getFeature());
    }
    feature.removeFromMap(map);
  }

  public WritableMap makeClickEventData(LatLng point) {
    WritableMap event = new WritableNativeMap();

    WritableMap coordinate = new WritableNativeMap();
    coordinate.putDouble("latitude", point.latitude);
    coordinate.putDouble("longitude", point.longitude);
    event.putMap("coordinate", coordinate);

    Projection projection = map.getProjection();
    Point screenPoint = projection.toScreenLocation(point);

    WritableMap position = new WritableNativeMap();
    position.putDouble("x", screenPoint.x);
    position.putDouble("y", screenPoint.y);
    event.putMap("position", position);

    return event;
  }

  public void updateExtraData(Object extraData) {
    // if boundsToMove is not null, we now have the MapView's width/height, so we can apply
    // a proper camera move
    try {
      if (boundsToMove != null) {
        HashMap<String, Float> data = (HashMap<String, Float>) extraData;
        int width = data.get("width") == null ? 0 : data.get("width").intValue();
        int height = data.get("height") == null ? 0 : data.get("height").intValue();

        //fix for https://github.com/react-native-community/react-native-maps/issues/245,
        //it's not guaranteed the passed-in height and width would be greater than 0.
        if (width <= 0 || height <= 0) {
          map.moveCamera(CameraUpdateFactory.newLatLngBounds(boundsToMove, 0));
        } else {
          map.moveCamera(CameraUpdateFactory.newLatLngBounds(boundsToMove, width, height, 0));
        }

        boundsToMove = null;
        cameraToSet = null;
      } else if (cameraToSet != null) {
        map.moveCamera(cameraToSet);
        cameraToSet = null;
      }
    } catch (Exception e) {
      Log.e("updateExtraData", Log.getStackTraceString(e));
    }
  }

  public void animateToCamera(ReadableMap camera, int duration) {
    if (map == null) return;
    CameraPosition.Builder builder = new CameraPosition.Builder(map.getCameraPosition());
    if (camera.hasKey("zoom")) {
      builder.zoom((float) camera.getDouble("zoom"));
    }
    if (camera.hasKey("heading")) {
      builder.bearing((float) camera.getDouble("heading"));
    }
    if (camera.hasKey("pitch")) {
      builder.tilt((float) camera.getDouble("pitch"));
    }
    if (camera.hasKey("center")) {
      ReadableMap center = camera.getMap("center");
      builder.target(new LatLng(center.getDouble("latitude"), center.getDouble("longitude")));
    }

    CameraUpdate update = CameraUpdateFactory.newCameraPosition(builder.build());

    if (duration <= 0) {
      map.moveCamera(update);
    } else {
      map.animateCamera(update, duration, null);
    }
  }

  public void animateToNavigation(LatLng location, float bearing, float angle, int duration) {
    if (map == null) return;
    CameraPosition cameraPosition = new CameraPosition.Builder(map.getCameraPosition())
            .bearing(bearing)
            .tilt(angle)
            .target(location)
            .build();
    map.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition), duration, null);
  }

  public void animateToRegion(LatLngBounds bounds, int duration) {
    if (map == null) return;
    map.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, 0), duration, null);
  }

  public void animateToViewingAngle(float angle, int duration) {
    if (map == null) return;

    CameraPosition cameraPosition = new CameraPosition.Builder(map.getCameraPosition())
            .tilt(angle)
            .build();
    map.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition), duration, null);
  }

  public void animateToBearing(float bearing, int duration) {
    if (map == null) return;
    CameraPosition cameraPosition = new CameraPosition.Builder(map.getCameraPosition())
            .bearing(bearing)
            .build();
    map.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition), duration, null);
  }

  public void animateToCoordinate(LatLng coordinate, int duration) {
    if (map == null) return;
    map.animateCamera(CameraUpdateFactory.newLatLng(coordinate), duration, null);
  }

  public void fitToElements(boolean animated) {
    if (map == null) return;

    LatLngBounds.Builder builder = new LatLngBounds.Builder();

    boolean addedPosition = false;

    for (AirMapFeature feature : features) {
      if (feature instanceof AirMapMarker) {
        Marker marker = (Marker) feature.getFeature();
        builder.include(marker.getPosition());
        addedPosition = true;
      }
      // TODO(lmr): may want to include shapes / etc.
    }
    if (addedPosition) {
      LatLngBounds bounds = builder.build();
      CameraUpdate cu = CameraUpdateFactory.newLatLngBounds(bounds, baseMapPadding);
      if (animated) {
        map.animateCamera(cu);
      } else {
        map.moveCamera(cu);
      }
    }
  }

  public void fitToSuppliedMarkers(ReadableArray markerIDsArray, ReadableMap edgePadding, boolean animated) {
    if (map == null) return;

    LatLngBounds.Builder builder = new LatLngBounds.Builder();

    String[] markerIDs = new String[markerIDsArray.size()];
    for (int i = 0; i < markerIDsArray.size(); i++) {
      markerIDs[i] = markerIDsArray.getString(i);
    }

    boolean addedPosition = false;

    List<String> markerIDList = asList(markerIDs);

    for (AirMapFeature feature : features) {
      if (feature instanceof AirMapMarker) {
        String identifier = ((AirMapMarker) feature).getIdentifier();
        Marker marker = (Marker) feature.getFeature();
        if (markerIDList.contains(identifier)) {
          builder.include(marker.getPosition());
          addedPosition = true;
        }
      }
    }

    if (addedPosition) {
      LatLngBounds bounds = builder.build();
      CameraUpdate cu = CameraUpdateFactory.newLatLngBounds(bounds, baseMapPadding);

      if (edgePadding != null) {
        map.setPadding(edgePadding.getInt("left"), edgePadding.getInt("top"),
                edgePadding.getInt("right"), edgePadding.getInt("bottom"));
      }

      if (animated) {
        map.animateCamera(cu);
      } else {
        map.moveCamera(cu);
      }
    }
  }

  public void fitToCoordinates(ReadableArray coordinatesArray, ReadableMap edgePadding,
                               boolean animated) {
    if (map == null) return;

    LatLngBounds.Builder builder = new LatLngBounds.Builder();

    for (int i = 0; i < coordinatesArray.size(); i++) {
      ReadableMap latLng = coordinatesArray.getMap(i);
      Double lat = latLng.getDouble("latitude");
      Double lng = latLng.getDouble("longitude");
      builder.include(new LatLng(lat, lng));
    }

    LatLngBounds bounds = builder.build();
    CameraUpdate cu = CameraUpdateFactory.newLatLngBounds(bounds, baseMapPadding);

    if (edgePadding != null) {
      map.setPadding(edgePadding.getInt("left"), edgePadding.getInt("top"),
              edgePadding.getInt("right"), edgePadding.getInt("bottom"));
    }

    if (animated) {
      map.animateCamera(cu);
    } else {
      map.moveCamera(cu);
    }
    map.setPadding(0, 0, 0,
            0); // Without this, the Google logo is moved up by the value of edgePadding.bottom
  }

  public double[][] getMapBoundaries() {
    LatLngBounds bounds = map.getProjection().getVisibleRegion().latLngBounds;
    LatLng northEast = bounds.northeast;
    LatLng southWest = bounds.southwest;

    return new double[][]{
            {northEast.longitude, northEast.latitude},
            {southWest.longitude, southWest.latitude}
    };
  }

  public void setMapBoundaries(ReadableMap northEast, ReadableMap southWest) {
    if (map == null) return;

    LatLngBounds.Builder builder = new LatLngBounds.Builder();

    Double latNE = northEast.getDouble("latitude");
    Double lngNE = northEast.getDouble("longitude");
    builder.include(new LatLng(latNE, lngNE));

    Double latSW = southWest.getDouble("latitude");
    Double lngSW = southWest.getDouble("longitude");
    builder.include(new LatLng(latSW, lngSW));

    LatLngBounds bounds = builder.build();

    map.setLatLngBoundsForCameraTarget(bounds);
  }

  // InfoWindowAdapter interface

  @Override
  public View getInfoWindow(Marker marker) {
    AirMapMarker markerView = getMarkerMap(marker);
    if (markerView == null) return null;
    return markerView.getCallout();
  }

  @Override
  public View getInfoContents(Marker marker) {
    AirMapMarker markerView = getMarkerMap(marker);
    if (markerView == null) return null;
    return markerView.getInfoContents();
  }

  @Override
  public boolean dispatchTouchEvent(MotionEvent ev) {
    gestureDetector.onTouchEvent(ev);

    int action = MotionEventCompat.getActionMasked(ev);

    switch (action) {
      case (MotionEvent.ACTION_DOWN):
        this.getParent().requestDisallowInterceptTouchEvent(
                map != null && map.getUiSettings().isScrollGesturesEnabled());
        break;
      case (MotionEvent.ACTION_UP):
        // Clear this regardless, since isScrollGesturesEnabled() may have been updated
        this.getParent().requestDisallowInterceptTouchEvent(false);
        break;
    }
    super.dispatchTouchEvent(ev);
    return true;
  }

  @Override
  public void onMarkerDragStart(Marker marker) {
    WritableMap event = makeClickEventData(marker.getPosition());
    manager.pushEvent(context, this, "onMarkerDragStart", event);

    AirMapMarker markerView = getMarkerMap(marker);
    if (markerView == null) return;
    event = makeClickEventData(marker.getPosition());
    manager.pushEvent(context, markerView, "onDragStart", event);
  }

  @Override
  public void onMarkerDrag(Marker marker) {
    WritableMap event = makeClickEventData(marker.getPosition());
    manager.pushEvent(context, this, "onMarkerDrag", event);

    AirMapMarker markerView = getMarkerMap(marker);
    if (markerView == null) return;
    event = makeClickEventData(marker.getPosition());
    manager.pushEvent(context, markerView, "onDrag", event);
  }

  @Override
  public void onMarkerDragEnd(Marker marker) {
    WritableMap event = makeClickEventData(marker.getPosition());
    manager.pushEvent(context, this, "onMarkerDragEnd", event);

    AirMapMarker markerView = getMarkerMap(marker);
    if (markerView == null) return;
    event = makeClickEventData(marker.getPosition());
    manager.pushEvent(context, markerView, "onDragEnd", event);
  }

  @Override
  public void onPoiClick(PointOfInterest poi) {
    WritableMap event = makeClickEventData(poi.latLng);

    event.putString("placeId", poi.placeId);
    event.putString("name", poi.name);

    manager.pushEvent(context, this, "onPoiClick", event);
  }

  private ProgressBar getMapLoadingProgressBar() {
    if (this.mapLoadingProgressBar == null) {
      this.mapLoadingProgressBar = new ProgressBar(getContext());
      this.mapLoadingProgressBar.setIndeterminate(true);
    }
    if (this.loadingIndicatorColor != null) {
      this.setLoadingIndicatorColor(this.loadingIndicatorColor);
    }
    return this.mapLoadingProgressBar;
  }

  private RelativeLayout getMapLoadingLayoutView() {
    if (this.mapLoadingLayout == null) {
      this.mapLoadingLayout = new RelativeLayout(getContext());
      this.mapLoadingLayout.setBackgroundColor(Color.LTGRAY);
      this.addView(this.mapLoadingLayout,
              new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                      ViewGroup.LayoutParams.MATCH_PARENT));

      RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(
              RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
      params.addRule(RelativeLayout.CENTER_IN_PARENT);
      this.mapLoadingLayout.addView(this.getMapLoadingProgressBar(), params);

      this.mapLoadingLayout.setVisibility(View.INVISIBLE);
    }
    this.setLoadingBackgroundColor(this.loadingBackgroundColor);
    return this.mapLoadingLayout;
  }

  private ImageView getCacheImageView() {
    if (this.cacheImageView == null) {
      this.cacheImageView = new ImageView(getContext());
      this.addView(this.cacheImageView,
              new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                      ViewGroup.LayoutParams.MATCH_PARENT));
      this.cacheImageView.setVisibility(View.INVISIBLE);
    }
    return this.cacheImageView;
  }

  private void removeCacheImageView() {
    if (this.cacheImageView != null) {
      ((ViewGroup) this.cacheImageView.getParent()).removeView(this.cacheImageView);
      this.cacheImageView = null;
    }
  }

  private void removeMapLoadingProgressBar() {
    if (this.mapLoadingProgressBar != null) {
      ((ViewGroup) this.mapLoadingProgressBar.getParent()).removeView(this.mapLoadingProgressBar);
      this.mapLoadingProgressBar = null;
    }
  }

  private void removeMapLoadingLayoutView() {
    this.removeMapLoadingProgressBar();
    if (this.mapLoadingLayout != null) {
      ((ViewGroup) this.mapLoadingLayout.getParent()).removeView(this.mapLoadingLayout);
      this.mapLoadingLayout = null;
    }
  }

  private void cacheView() {
    if (this.cacheEnabled) {
      final ImageView cacheImageView = this.getCacheImageView();
      final RelativeLayout mapLoadingLayout = this.getMapLoadingLayoutView();
      cacheImageView.setVisibility(View.INVISIBLE);
      mapLoadingLayout.setVisibility(View.VISIBLE);
      if (this.isMapLoaded) {
        this.map.snapshot(new GoogleMap.SnapshotReadyCallback() {
          @Override
          public void onSnapshotReady(Bitmap bitmap) {
            cacheImageView.setImageBitmap(bitmap);
            cacheImageView.setVisibility(View.VISIBLE);
            mapLoadingLayout.setVisibility(View.INVISIBLE);
          }
        });
      }
    } else {
      this.removeCacheImageView();
      if (this.isMapLoaded) {
        this.removeMapLoadingLayoutView();
      }
    }
  }

  public void onPanDrag(MotionEvent ev) {
    Point point = new Point((int) ev.getX(), (int) ev.getY());
    LatLng coords = this.map.getProjection().fromScreenLocation(point);
    WritableMap event = makeClickEventData(coords);
    manager.pushEvent(context, this, "onPanDrag", event);
  }

  public void setSwitchToCityPinsDelta(float switchDelta) {
    switchToCityPinsDelta = switchDelta;
  }

  public void setCityPins(ReadableArray pins) {
    if (pins != null) {
      for (int i = 0; i < pins.size(); i++) {
        ReadableMap encodedPin = pins.getMap(i);
        AirMapCity city = new AirMapCity(encodedPin);
        if (!cities.containsKey(city.getPinPosition())) {
          cities.put(city.getPinPosition(), city);
          city.setMarker(new AirMapMarker(context, null));
          city.getMarker().setImage(city.getIconSrc());
          city.getMarker().setCoordinate(encodedPin.getMap("pos"));
        }
      }
    }
  }

  public void setImageIds(ReadableMap imageIds) {
    this.manager.getUrbiMarkerManager().setImageIds(imageIds.toHashMap());
  }

  public void onDoublePress(MotionEvent ev) {
    Point point = new Point((int) ev.getX(), (int) ev.getY());
    LatLng coords = this.map.getProjection().fromScreenLocation(point);
    WritableMap event = makeClickEventData(coords);
    manager.pushEvent(context, this, "onDoublePress", event);
  }

  public void setKmlSrc(String kmlSrc) {
    try {
      InputStream kmlStream = new FileUtil(context).execute(kmlSrc).get();

      if (kmlStream == null) {
        return;
      }

      kmlLayer = new KmlLayer(map, kmlStream, context);
      kmlLayer.addLayerToMap();

      WritableMap pointers = new WritableNativeMap();
      WritableArray markers = new WritableNativeArray();

      if (kmlLayer.getContainers() == null) {
        manager.pushEvent(context, this, "onKmlReady", pointers);
        return;
      }

      //Retrieve a nested container within the first container
      KmlContainer container = kmlLayer.getContainers().iterator().next();
      if (container == null || container.getContainers() == null) {
        manager.pushEvent(context, this, "onKmlReady", pointers);
        return;
      }


      if (container.getContainers().iterator().hasNext()) {
        container = container.getContainers().iterator().next();
      }

      Integer index = 0;
      for (KmlPlacemark placemark : container.getPlacemarks()) {
        MarkerOptions options = new MarkerOptions();

        if (placemark.getInlineStyle() != null) {
          options = placemark.getMarkerOptions();
        } else {
          options.icon(BitmapDescriptorFactory.defaultMarker());
        }

        LatLng latLng = ((LatLng) placemark.getGeometry().getGeometryObject());
        String title = "";
        String snippet = "";

        if (placemark.hasProperty("name")) {
          title = placemark.getProperty("name");
        }

        if (placemark.hasProperty("description")) {
          snippet = placemark.getProperty("description");
        }

        options.position(latLng);
        options.title(title);
        options.snippet(snippet);

        AirMapMarker marker = new AirMapMarker(context, options, this.manager.getMarkerManager());

        if (placemark.getInlineStyle() != null
                && placemark.getInlineStyle().getIconUrl() != null) {
          marker.setImage(placemark.getInlineStyle().getIconUrl());
        } else if (container.getStyle(placemark.getStyleId()) != null) {
          KmlStyle style = container.getStyle(placemark.getStyleId());
          marker.setImage(style.getIconUrl());
        }

        String identifier = title + " - " + index;

        marker.setIdentifier(identifier);

        addFeature(marker, index++);

        WritableMap loadedMarker = makeClickEventData(latLng);
        loadedMarker.putString("id", identifier);
        loadedMarker.putString("title", title);
        loadedMarker.putString("description", snippet);

        markers.pushMap(loadedMarker);
      }

      pointers.putArray("markers", markers);

      manager.pushEvent(context, this, "onKmlReady", pointers);

    } catch (XmlPullParserException e) {
      e.printStackTrace();
    } catch (IOException e) {
      e.printStackTrace();
    } catch (InterruptedException e) {
      e.printStackTrace();
    } catch (ExecutionException e) {
      e.printStackTrace();
    }
  }

  @Override
  public void onIndoorBuildingFocused() {
    IndoorBuilding building = this.map.getFocusedBuilding();
    if (building != null) {
      List<IndoorLevel> levels = building.getLevels();
      int index = 0;
      WritableArray levelsArray = Arguments.createArray();
      for (IndoorLevel level : levels) {
        WritableMap levelMap = Arguments.createMap();
        levelMap.putInt("index", index);
        levelMap.putString("name", level.getName());
        levelMap.putString("shortName", level.getShortName());
        levelsArray.pushMap(levelMap);
        index++;
      }
      WritableMap event = Arguments.createMap();
      WritableMap indoorBuilding = Arguments.createMap();
      indoorBuilding.putArray("levels", levelsArray);
      indoorBuilding.putInt("activeLevelIndex", building.getActiveLevelIndex());
      indoorBuilding.putBoolean("underground", building.isUnderground());

      event.putMap("IndoorBuilding", indoorBuilding);

      manager.pushEvent(context, this, "onIndoorBuildingFocused", event);
    } else {
      WritableMap event = Arguments.createMap();
      WritableArray levelsArray = Arguments.createArray();
      WritableMap indoorBuilding = Arguments.createMap();
      indoorBuilding.putArray("levels", levelsArray);
      indoorBuilding.putInt("activeLevelIndex", 0);
      indoorBuilding.putBoolean("underground", false);

      event.putMap("IndoorBuilding", indoorBuilding);

      manager.pushEvent(context, this, "onIndoorBuildingFocused", event);
    }
  }

  @Override
  public void onIndoorLevelActivated(IndoorBuilding building) {
    if (building == null) {
      return;
    }
    int activeLevelIndex = building.getActiveLevelIndex();
    if (activeLevelIndex < 0 || activeLevelIndex >= building.getLevels().size()) {
      return;
    }
    IndoorLevel level = building.getLevels().get(activeLevelIndex);

    WritableMap event = Arguments.createMap();
    WritableMap indoorlevel = Arguments.createMap();

    indoorlevel.putInt("activeLevelIndex", activeLevelIndex);
    indoorlevel.putString("name", level.getName());
    indoorlevel.putString("shortName", level.getShortName());

    event.putMap("IndoorLevel", indoorlevel);

    manager.pushEvent(context, this, "onIndoorLevelActivated", event);
  }

  public void setIndoorActiveLevelIndex(int activeLevelIndex) {
    IndoorBuilding building = map.getFocusedBuilding();
    if (building != null) {
      if (activeLevelIndex >= 0 && activeLevelIndex < building.getLevels().size()) {
        IndoorLevel level = building.getLevels().get(activeLevelIndex);
        if (level != null) {
          level.activate();
        }
      }
    }
  }

  private AirMapMarker getMarkerMap(Marker marker) {
    AirMapMarker airMarker = markerMap.get(marker);

    if (airMarker != null) {
      return airMarker;
    }

    for (Map.Entry<Marker, AirMapMarker> entryMarker : markerMap.entrySet()) {
      Marker thisMarker = entryMarker.getKey();
      if (thisMarker.getPosition() == null || thisMarker.getTitle() == null)
        continue;

      if (thisMarker.getPosition().equals(marker.getPosition())
              && thisMarker.getTitle().equals(marker.getTitle())) {
        airMarker = entryMarker.getValue();
        break;
      }
    }

    return airMarker;
  }

  public void setSelectedMarker(final AirMapMarker selectedMarker) {

    if (this.selectedMarker == selectedMarker) return;

    if (this.selectedMarker != null) {
      this.selectedMarker.setSelectedFromClick(false);
      scaleDown(this.selectedMarker);
    }

    if (selectedMarker != null) {
      selectedMarker.setSelectedFromClick(true);
      setOriginalSize(selectedMarker);
    }

    this.selectedMarker = selectedMarker;
  }

  public void deselectIfSelected(final AirMapMarker selectedMarker) {
    if (this.selectedMarker == selectedMarker) {
      setSelectedMarker(null);
    }
  }

  public void setPaddingListener(AirMapPaddingListener listener) {
    this.paddingListener = listener;
  }

  public void manuallyLayoutChildren(View child) {
    child.measure(MeasureSpec.makeMeasureSpec(getMeasuredWidth(), MeasureSpec.EXACTLY),
            MeasureSpec.makeMeasureSpec(getMeasuredHeight(), MeasureSpec.EXACTLY));

    child.layout(0, 0, child.getMeasuredWidth(), child.getMeasuredHeight());
  }

  public interface DirectionsCallback {
    void accept(Integer timeEstimate, Integer distanceEstimate, String polyline);
  }

  @SuppressLint("MissingPermission")
  public void fetchDirectionsTo(final LatLng to, final com.airbnb.android.react.maps.AirMapView.DirectionsCallback callback) {
    if (hasPermissions()) {
      fusedLocationClient.getLastLocation().addOnSuccessListener(new OnSuccessListener<Location>() {
        @Override
        public void onSuccess(final Location from) {
          final Handler mainHandler = new Handler(Looper.getMainLooper());
          mainHandler.post(new Runnable() {
            @Override
            public void run() {
              try {
                List<Pair<String, String>> params = asList(
                        new Pair<>("origin", format(ENGLISH, "%.6f,%.6f", from.getLatitude(), from.getLongitude())),
                        new Pair<>("destination", format(ENGLISH, "%.6f,%.6f", to.latitude, to.longitude)),
                        new Pair<>("mode", "walking"),
                        new Pair<>("avoid", "tolls|highways|ferries"),
                        new Pair<>("language", "en")
                );

                StringBuilder builder = new StringBuilder();
                for (Pair<String, String> param : params) {
                  builder.append(param.first);
                  builder.append("=");
                  builder.append(URLEncoder.encode(param.second, "UTF-8"));
                  builder.append("&");
                }
                builder.deleteCharAt(builder.length() - 1);

                String url = format("%s/api/v3/legacy/maps/directions?%s", beURL, builder.toString());

                Request request = new Request.Builder().url(url)
                        .post(RequestBody.create(MIME_JSON, "{}"))
                        .addHeader("Authorization", auth)
                        .build();

                HTTP_CLIENT.newCall(request).enqueue(new Callback() {
                  @Override
                  public void onFailure(@NonNull Call call, @NonNull IOException e) {
                    e.printStackTrace();
                  }

                  @Override
                  public void onResponse(@NonNull Call call, @NonNull final Response response) throws IOException {
                    try {
                      ResponseBody body = response.body();
                      if (body != null) {
                        // we'll catch any error in the try/catch. Yes, Exception, #yolo
                        JSONObject route = new JSONObject(body.string())
                                .getJSONArray("routes")
                                .getJSONObject(0);

                        JSONObject leg = route
                                .getJSONArray("legs")
                                .getJSONObject(0);

                        final int seconds = leg.getJSONObject("duration").getInt("value");
                        final int meters = leg.getJSONObject("distance").getInt("value");
                        final String polyline = route.getJSONObject("overview_polyline").getString("points");

                        mainHandler.post(new Runnable() {
                          @Override
                          public void run() {
                            callback.accept(seconds, meters, polyline);
                          }
                        });
                      }
                    } catch (Exception e2) {
                      Log.e("urbi", "unexpected format", e2);
                    }
                  }
                });
              } catch (Exception encodingException) {
                encodingException.printStackTrace();
              }
            }
          });
        }
      });
    } else {
      Log.d("urbi", "user has given no location permissions");
    }
  }

  public AirMapView setBeURL(String beURL) {
    this.beURL = beURL;
    return this;
  }

  public AirMapView setAuth(String auth) {
    this.auth = auth;
    return this;
  }

  public int getShowPathIfCloserThanSeconds() {
    return showPathIfCloserThanSeconds;
  }

  public AirMapView setShowPathIfCloserThanSeconds(int showPathIfCloserThanSeconds) {
    this.showPathIfCloserThanSeconds = showPathIfCloserThanSeconds;
    return this;
  }


  interface AirMapPaddingListener {
    void forceLayout();

    int getTopHeight();
  }

  public Location getLastLocation() {
    return lastLocation;
  }

  public boolean hasUserDeniedLocationServicesEnable() {
    return userDeniedLocationServicesEnable;
  }
}

