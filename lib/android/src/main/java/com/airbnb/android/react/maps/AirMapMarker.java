package com.airbnb.android.react.maps;

import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.graphics.drawable.Animatable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.text.StaticLayout;
import android.text.TextPaint;
import android.util.Log;
import android.util.Pair;
import android.view.View;
import android.widget.LinearLayout;
import android.animation.ObjectAnimator;
import android.util.Property;
import android.animation.TypeEvaluator;

import com.facebook.common.references.CloseableReference;
import com.facebook.datasource.DataSource;
import com.facebook.drawee.backends.pipeline.Fresco;
import com.facebook.drawee.controller.BaseControllerListener;
import com.facebook.drawee.controller.ControllerListener;
import com.facebook.drawee.drawable.ScalingUtils;
import com.facebook.drawee.generic.GenericDraweeHierarchy;
import com.facebook.drawee.generic.GenericDraweeHierarchyBuilder;
import com.facebook.drawee.interfaces.DraweeController;
import com.facebook.drawee.view.DraweeHolder;
import com.facebook.imagepipeline.core.ImagePipeline;
import com.facebook.imagepipeline.image.CloseableImage;
import com.facebook.imagepipeline.image.CloseableStaticBitmap;
import com.facebook.imagepipeline.image.ImageInfo;
import com.facebook.imagepipeline.request.ImageRequest;
import com.facebook.imagepipeline.request.ImageRequestBuilder;
import com.facebook.react.bridge.ReadableMap;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.*;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;

import javax.annotation.Nullable;

import static com.airbnb.android.react.maps.AirMapView.PIN_SCALE_FACTOR;
import static com.airbnb.android.react.maps.PolylineUtils.decodePoly;
import static java.lang.String.format;
import static java.util.Locale.ENGLISH;

public class AirMapMarker extends AirMapFeature {

  static PolylineOptions pathOptions;
  static Polyline path;

  private static final List<PatternItem> PATH_PATTERN = Arrays.asList(new Dot(), new Gap(20));

  private MarkerOptions markerOptions;
  private AirMapView mapView;

  private Marker marker;
  private int width;
  private int height;
  private String identifier;

  private boolean filteredOut;
  private boolean selected;

  private LatLng position;
  private String title;
  private String snippet;

  private boolean anchorIsSet;
  private float anchorX;
  private float anchorY;

  private AirMapCallout calloutView;
  private View wrappedCalloutView;
  private final Context context;

  private float markerHue = 0.0f; // should be between 0 and 360
  private BitmapDescriptor iconBitmapDescriptor;
  private Bitmap iconBitmap;
  private BitmapDescriptor originalBitmapDescriptor;
  private Bitmap originalIconBitmap;
  private BitmapDescriptor scaledDownBitmapDescriptor;
  private Bitmap scaledDownBitmap;

  private float rotation = 0.0f;
  private boolean flat = false;
  private boolean draggable = false;
  private int zIndex = 0;
  private float opacity = 1.0f;

  private float calloutAnchorX;
  private float calloutAnchorY;
  private boolean calloutAnchorIsSet;

  private boolean tracksViewChanges = true;
  private boolean tracksViewChangesActive = false;
  private boolean hasViewChanges = true;

  private boolean hasCustomMarkerView = false;
  private final AirMapMarkerManager markerManager;
  private String imageUri;

  private final DraweeHolder<?> logoHolder;
  private DataSource<CloseableReference<CloseableImage>> dataSource;
  private final ControllerListener<ImageInfo> mLogoControllerListener =
      new BaseControllerListener<ImageInfo>() {
        @Override
        public void onFinalImageSet(
            String id,
            @Nullable final ImageInfo imageInfo,
            @Nullable Animatable animatable) {
          CloseableReference<CloseableImage> imageReference = null;
          try {
            imageReference = dataSource.getResult();
            if (imageReference != null) {
              CloseableImage image = imageReference.get();
              if (image != null && image instanceof CloseableStaticBitmap) {
                CloseableStaticBitmap closeableStaticBitmap = (CloseableStaticBitmap) image;
                Bitmap bitmap = closeableStaticBitmap.getUnderlyingBitmap();
                if (bitmap != null) {
                  bitmap = bitmap.copy(Bitmap.Config.ARGB_8888, true);
                  scaledDownBitmap = getScaledDownBitmap(bitmap);
                  scaledDownBitmapDescriptor = BitmapDescriptorFactory.fromBitmap(scaledDownBitmap);
                  originalIconBitmap = bitmap;
                  originalBitmapDescriptor = BitmapDescriptorFactory.fromBitmap(originalIconBitmap);
                  iconBitmap = selected ? originalIconBitmap : scaledDownBitmap;
                  iconBitmapDescriptor = selected ? originalBitmapDescriptor : scaledDownBitmapDescriptor;
                }
              }
            }
          } finally {
            dataSource.close();
            if (imageReference != null) {
              CloseableReference.closeSafely(imageReference);
            }
          }
          if (AirMapMarker.this.markerManager != null && AirMapMarker.this.imageUri != null) {
            AirMapMarker.this.markerManager.getSharedIcon(AirMapMarker.this.imageUri)
                .updateIcon(scaledDownBitmapDescriptor, scaledDownBitmap, originalBitmapDescriptor, originalIconBitmap);
          }
          update(true);
        }
      };

  public AirMapMarker(Context context, AirMapMarkerManager markerManager) {
    super(context);
    this.context = context;
    this.markerManager = markerManager;
    logoHolder = DraweeHolder.create(createDraweeHierarchy(), context);
    logoHolder.onAttach();
  }

  public AirMapMarker(Context context, MarkerOptions options, AirMapMarkerManager markerManager) {
    super(context);
    this.context = context;
    this.markerManager = markerManager;
    logoHolder = DraweeHolder.create(createDraweeHierarchy(), context);
    logoHolder.onAttach();

    position = options.getPosition();
    setAnchor(options.getAnchorU(), options.getAnchorV());
    setCalloutAnchor(options.getInfoWindowAnchorU(), options.getInfoWindowAnchorV());
    setTitle(options.getTitle());
    setSnippet(options.getSnippet());
    setRotation(options.getRotation());
    setFlat(options.isFlat());
    setDraggable(options.isDraggable());
    setZIndex(Math.round(options.getZIndex()));
    setAlpha(options.getAlpha());
    iconBitmapDescriptor = options.getIcon();
  }

  private GenericDraweeHierarchy createDraweeHierarchy() {
    return new GenericDraweeHierarchyBuilder(getResources())
        .setActualImageScaleType(ScalingUtils.ScaleType.FIT_CENTER)
        .setFadeDuration(0)
        .build();
  }

  public void setCoordinate(ReadableMap coordinate) {
    position = new LatLng(coordinate.getDouble("latitude"), coordinate.getDouble("longitude"));
    if (marker != null) {
      marker.setPosition(position);
    }
    update(false);
  }

  public void setIdentifier(String identifier) {
    this.identifier = identifier;
    update(false);
  }

  public String getIdentifier() {
    return this.identifier;
  }

  public void setTitle(String title) {
    this.title = title;
    if (marker != null) {
      marker.setTitle(title);
    }
    update(false);
  }

  public void setSnippet(String snippet) {
    this.snippet = snippet;
    if (marker != null) {
      marker.setSnippet(snippet);
    }
    update(false);
  }

  public void setRotation(float rotation) {
    this.rotation = rotation;
    if (marker != null) {
      marker.setRotation(rotation);
    }
    update(false);
  }

  public void setFlat(boolean flat) {
    this.flat = flat;
    if (marker != null) {
      marker.setFlat(flat);
    }
    update(false);
  }

  public void setDraggable(boolean draggable) {
    this.draggable = draggable;
    if (marker != null) {
      marker.setDraggable(draggable);
    }
    update(false);
  }

  public void setZIndex(int zIndex) {
    this.zIndex = zIndex;
    if (marker != null) {
      marker.setZIndex(zIndex);
    }
    update(false);
  }

  public void setOpacity(float opacity) {
    this.opacity = opacity;
    if (marker != null) {
      marker.setAlpha(opacity);
    }
    update(false);
  }

  public void setMarkerHue(float markerHue) {
    this.markerHue = markerHue;
    update(false);
  }

  public void setAnchor(double x, double y) {
    anchorIsSet = true;
    anchorX = (float) x;
    anchorY = (float) y;
    if (marker != null) {
      marker.setAnchor(anchorX, anchorY);
    }
    update(false);
  }

  public void setCalloutAnchor(double x, double y) {
    calloutAnchorIsSet = true;
    calloutAnchorX = (float) x;
    calloutAnchorY = (float) y;
    if (marker != null) {
      marker.setInfoWindowAnchor(calloutAnchorX, calloutAnchorY);
    }
    update(false);
  }

  public void setTracksViewChanges(boolean tracksViewChanges) {
    this.tracksViewChanges = tracksViewChanges;
    updateTracksViewChanges();
  }

  private void updateTracksViewChanges() {
    boolean shouldTrack = tracksViewChanges && hasCustomMarkerView && marker != null;
    if (shouldTrack == tracksViewChangesActive) return;
    tracksViewChangesActive = shouldTrack;

    if (shouldTrack) {
      ViewChangesTracker.getInstance().addMarker(this);
    } else {
      ViewChangesTracker.getInstance().removeMarker(this);

      // Let it render one more time to avoid race conditions.
      // i.e. Image onLoad ->
      //      ViewChangesTracker may not get a chance to render ->
      //      setState({ tracksViewChanges: false }) ->
      //      image loaded but not rendered.
      updateMarkerIcon();
    }
  }

  public boolean updateCustomForTracking() {
    if (!tracksViewChangesActive)
      return false;

    updateMarkerIcon();

    return true;
  }

  public void updateMarkerIcon() {
    if (marker == null || marker.getTag() == null) return;

    if (!hasCustomMarkerView) {
      // No more updates for this, as it's a simple icon
      hasViewChanges = false;
    }
    if (marker != null && marker.getTag() != null) {
      marker.setIcon(getIcon());
    }
  }

  public LatLng interpolate(float fraction, LatLng a, LatLng b) {
    double lat = (b.latitude - a.latitude) * fraction + a.latitude;
    double lng = (b.longitude - a.longitude) * fraction + a.longitude;
    return new LatLng(lat, lng);
  }

  public void animateToCoodinate(LatLng finalPosition, Integer duration) {
    TypeEvaluator<LatLng> typeEvaluator = new TypeEvaluator<LatLng>() {
      @Override
      public LatLng evaluate(float fraction, LatLng startValue, LatLng endValue) {
        return interpolate(fraction, startValue, endValue);
      }
    };
    Property<Marker, LatLng> property = Property.of(Marker.class, LatLng.class, "position");
    ObjectAnimator animator = ObjectAnimator.ofObject(
      marker,
      property,
      typeEvaluator,
      finalPosition);
    animator.setDuration(duration);
    animator.start();
  }

  public void setImage(String uri) {
    hasViewChanges = true;

    boolean shouldLoadImage = true;

    if (this.markerManager != null) {
      // remove marker from previous shared icon if needed, to avoid future updates from it.
      // remove the shared icon completely if no markers on it as well.
      // this is to avoid memory leak due to orphan bitmaps.
      //
      // However in case where client want to update all markers from icon A to icon B
      // and after some time to update back from icon B to icon A
      // it may be better to keep it though. We assume that is rare.
      if (this.imageUri != null) {
        this.markerManager.getSharedIcon(this.imageUri).removeMarker(this);
        this.markerManager.removeSharedIconIfEmpty(this.imageUri);
      }
      if (uri != null) {
        // listening for marker bitmap descriptor update, as well as check whether to load the image.
        AirMapMarkerManager.AirMapMarkerSharedIcon sharedIcon = this.markerManager.getSharedIcon(uri);
        sharedIcon.addMarker(this);
        shouldLoadImage = sharedIcon.shouldLoadImage();
      }
    }

    this.imageUri = uri;
    if (!shouldLoadImage) {
      return;
    }

    if (uri == null) {
      iconBitmapDescriptor = null;
      update(true);
    } else if (uri.startsWith("http://") || uri.startsWith("https://") ||
        uri.startsWith("file://") || uri.startsWith("asset://") || uri.startsWith("data:")) {
      ImageRequest imageRequest = ImageRequestBuilder
          .newBuilderWithSource(Uri.parse(uri))
          .build();

      ImagePipeline imagePipeline = Fresco.getImagePipeline();
      dataSource = imagePipeline.fetchDecodedImage(imageRequest, this);
      DraweeController controller = Fresco.newDraweeControllerBuilder()
          .setImageRequest(imageRequest)
          .setControllerListener(mLogoControllerListener)
          .setOldController(logoHolder.getController())
          .build();
      logoHolder.setController(controller);
    } else {
      iconBitmapDescriptor = getBitmapDescriptorByName(uri);
      if (iconBitmapDescriptor != null) {
          int drawableId = getDrawableResourceByName(uri);
          iconBitmap = BitmapFactory.decodeResource(getResources(), drawableId);
          if (iconBitmap == null) { // VectorDrawable or similar
              Drawable drawable = getResources().getDrawable(drawableId);
              iconBitmap = Bitmap.createBitmap(drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
              drawable.setBounds(0, 0, drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight());
              Canvas canvas = new Canvas(iconBitmap);
              drawable.draw(canvas);
          } else {
            originalIconBitmap = iconBitmap;
            originalBitmapDescriptor = BitmapDescriptorFactory.fromBitmap(originalIconBitmap);
            scaledDownBitmap = getScaledDownBitmap(originalIconBitmap);
            scaledDownBitmapDescriptor = BitmapDescriptorFactory.fromBitmap(scaledDownBitmap);
            iconBitmap = selected ? originalIconBitmap : scaledDownBitmap;
            iconBitmapDescriptor = selected ? originalBitmapDescriptor : scaledDownBitmapDescriptor;
          }
      }
      if (this.markerManager != null && uri != null) {
        this.markerManager.getSharedIcon(uri).updateIcon(iconBitmapDescriptor, iconBitmap, originalBitmapDescriptor, originalIconBitmap);
      }
      update(true);
    }
  }

  public void setIconBitmapDescriptor(BitmapDescriptor bitmapDescriptor, Bitmap bitmap, BitmapDescriptor originalBitmapDescriptor, Bitmap originalBitmap) {
    this.scaledDownBitmapDescriptor = bitmapDescriptor;
    this.scaledDownBitmap = bitmap;
    this.originalBitmapDescriptor = originalBitmapDescriptor;
    this.originalIconBitmap = originalBitmap;
    this.iconBitmap = selected ? originalIconBitmap : scaledDownBitmap;
    this.iconBitmapDescriptor = selected ? originalBitmapDescriptor : scaledDownBitmapDescriptor;
    this.hasViewChanges = true;
    this.update(true);
  }

  private static String formatTime(int seconds) {
    if (seconds <= 60) return "< 1 min";
    if (seconds < 3600) return format(ENGLISH,"%d min", seconds / 60);
    if (seconds < 36000) return format(ENGLISH, "%dh%dm", seconds / 3600, (seconds % 3600) / 60);
    return format(ENGLISH, "%d h", seconds / 3600);
  }

  private static String formatDistance(int meters) {
    if (meters < 1000) return format(ENGLISH, "%dm", meters);
    if (meters < 10000) return format(ENGLISH, "%.1fkm", meters / 1000.0);
    return format(ENGLISH, "%dkm", meters / 1000);
  }

  @TargetApi(21)
  private void addEstimatesToIcon(int seconds, int meters) {
    if (selected) {

      String text = format(Locale.getDefault(), "%s - %s", formatTime(seconds), formatDistance(meters));

      Paint bgPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
      bgPaint.setColor(Color.parseColor("#152934"));

      float dp = getResources().getDisplayMetrics().density;

      TextPaint textPaint = new TextPaint(Paint.ANTI_ALIAS_FLAG | Paint.LINEAR_TEXT_FLAG);
      textPaint.setStyle(Paint.Style.FILL);
      textPaint.setColor(Color.WHITE);
      textPaint.setTypeface(Typeface.SANS_SERIF);
      textPaint.setFakeBoldText(true);
      textPaint.setTextSize(12 * dp);
      textPaint.setTextAlign(Paint.Align.LEFT);

      float baseline = -textPaint.ascent();
      int width = Math.max((int) (textPaint.measureText(text) + 0.5f + 8 * dp), originalIconBitmap.getWidth());
      int height = (int) (baseline + textPaint.descent() + 0.5f) ;
      float iconLeftPadding = (width - originalIconBitmap.getWidth()) / 2f + 0.5f;

      Bitmap image = Bitmap.createBitmap(width, (int) (height + 8 * dp + originalIconBitmap.getHeight() +0.5f), Bitmap.Config.ARGB_8888);
      Canvas canvas = new Canvas(image);
      canvas.drawRoundRect(0, 0, width, height + 4 * dp, 4 * dp, 4 * dp, bgPaint);
      canvas.drawText(text, 4 * dp, baseline + 2 * dp, textPaint);
      canvas.drawBitmap(originalIconBitmap, iconLeftPadding, height + 8 * dp, null);

      iconBitmap = image;
      iconBitmapDescriptor = BitmapDescriptorFactory.fromBitmap(iconBitmap);
      hasViewChanges = true;
      update(true);
    }
  }

  private void addPathToIcon(String encodedPoints) {
    List<LatLng> points = decodePoly(encodedPoints);

    pathOptions = new PolylineOptions()
        .addAll(points)
        .color(Color.parseColor("#152934"))
        .geodesic(true)
        .pattern(PATH_PATTERN)
        .geodesic(true);

    path = mapView.map.addPolyline(pathOptions);
  }

  public void setIconSelected(boolean isSelected) {
    if (isSelected) {
      mapView.fetchDirectionsTo(marker.getPosition(), new AirMapView.DirectionsCallback() {
        @Override
        public void accept(Integer timeEstimate, Integer distanceEstimate, String polyline) {
          addEstimatesToIcon(timeEstimate, distanceEstimate);
          addPathToIcon(polyline);
        }
      });
      iconBitmap = originalIconBitmap;
      iconBitmapDescriptor = originalBitmapDescriptor;
    } else {
      if (path != null) {
        path.remove();
        pathOptions = null;
      }
      iconBitmap = scaledDownBitmap;
      iconBitmapDescriptor = scaledDownBitmapDescriptor;
    }
    this.hasViewChanges = true;
    this.update(true);
  }

  public MarkerOptions getMarkerOptions() {
    if (markerOptions == null) {
      markerOptions = new MarkerOptions();
    }

    fillMarkerOptions(markerOptions);
    return markerOptions;
  }

  @Override
  public void addView(View child, int index) {
    super.addView(child, index);
    // if children are added, it means we are rendering a custom marker
    if (!(child instanceof AirMapCallout)) {
      hasCustomMarkerView = true;
      updateTracksViewChanges();
    }
    update(true);
  }

  @Override
  public void requestLayout() {
    super.requestLayout();

    if (getChildCount() == 0) {
      if (hasCustomMarkerView) {
        hasCustomMarkerView = false;
        clearDrawableCache();
        updateTracksViewChanges();
        update(true);
      }

    }
  }

  @Override
  public Object getFeature() {
    return marker;
  }

  @Override
  public void addToMap(GoogleMap map, AirMapView view) {
    this.mapView = view;
    if (!filteredOut) {
      marker = map.addMarker(getMarkerOptions());
      marker.setTag("");
      updateTracksViewChanges();
    }
  }

  public void readdToMapIfNotFilteredOut() {
    if (!filteredOut && mapView != null) {
      if (marker != null)
        mapView.addMarkerToMap(this);
      else
        addToMap(mapView.map, mapView);
    }
  }

  public void readdToMapIfNotFilteredOut(GoogleMap map) {
    if (filteredOut) {
      marker = null;
    } else {
      marker = map.addMarker(getMarkerOptions());
      marker.setTag("");
      updateMarkerIcon();
    }
  }

  @Override
  public void removeFromMap(GoogleMap map) {
    if (marker != null && marker.getTag() != null) {
      marker.remove();
    }
    marker = null;
    updateTracksViewChanges();
  }

  private BitmapDescriptor getIcon() {
    if (hasCustomMarkerView) {
      // creating a bitmap from an arbitrary view
      if (iconBitmapDescriptor != null) {
        Bitmap viewBitmap = createDrawable();
        int width = Math.max(iconBitmap.getWidth(), viewBitmap.getWidth());
        int height = Math.max(iconBitmap.getHeight(), viewBitmap.getHeight());
        Bitmap combinedBitmap = Bitmap.createBitmap(width, height, iconBitmap.getConfig());
        Canvas canvas = new Canvas(combinedBitmap);
        canvas.drawBitmap(iconBitmap, 0, 0, null);
        canvas.drawBitmap(viewBitmap, 0, 0, null);
        return BitmapDescriptorFactory.fromBitmap(combinedBitmap);
      } else {
        return BitmapDescriptorFactory.fromBitmap(createDrawable());
      }
    } else if (iconBitmapDescriptor != null) {
      // use local image as a marker
      return iconBitmapDescriptor;
    } else {
      // render the default marker pin
      return BitmapDescriptorFactory.defaultMarker(this.markerHue);
    }
  }

  public Bitmap getIconBitmap() {
    return iconBitmap;
  }

  public BitmapDescriptor getOriginalBitmapDescriptor() {
    return originalBitmapDescriptor;
  }

  public Bitmap getOriginalIconBitmap() {
    return originalIconBitmap;
  }

  public Marker getMarker() {
    return marker;
  }

  private MarkerOptions fillMarkerOptions(MarkerOptions options) {
    options.position(position);
    if (anchorIsSet) options.anchor(anchorX, anchorY);
    if (calloutAnchorIsSet) options.infoWindowAnchor(calloutAnchorX, calloutAnchorY);
    options.title(title);
    options.snippet(snippet);
    options.rotation(rotation);
    options.flat(flat);
    options.draggable(draggable);
    options.zIndex(zIndex);
    options.alpha(opacity);
    options.icon(getIcon());
    return options;
  }

  public void update(boolean updateIcon) {
    if (marker == null || marker.getTag() == null) {
      return;
    }

    if (updateIcon)
      updateMarkerIcon();

    if (anchorIsSet) {
      marker.setAnchor(anchorX, anchorY);
    } else {
      marker.setAnchor(0.5f, 1.0f);
    }

    if (calloutAnchorIsSet) {
      marker.setInfoWindowAnchor(calloutAnchorX, calloutAnchorY);
    } else {
      marker.setInfoWindowAnchor(0.5f, 0);
    }
  }

  public void update(int width, int height) {
    this.width = width;
    this.height = height;

    update(true);
  }

  private Bitmap mLastBitmapCreated = null;

  private void clearDrawableCache() {
    mLastBitmapCreated = null;
  }

  private Bitmap createDrawable() {
    int width = this.width <= 0 ? 100 : this.width;
    int height = this.height <= 0 ? 100 : this.height;
    this.buildDrawingCache();

    // Do not create the doublebuffer-bitmap each time. reuse it to save memory.
    Bitmap bitmap = mLastBitmapCreated;

    if (bitmap == null ||
            bitmap.isRecycled() ||
            bitmap.getWidth() != width ||
            bitmap.getHeight() != height) {
      bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
      mLastBitmapCreated = bitmap;
    } else {
      bitmap.eraseColor(Color.TRANSPARENT);
    }

    Canvas canvas = new Canvas(bitmap);
    this.draw(canvas);

    return bitmap;
  }

  public void setCalloutView(AirMapCallout view) {
    this.calloutView = view;
  }

  public AirMapCallout getCalloutView() {
    return this.calloutView;
  }

  public View getCallout() {
    if (this.calloutView == null) return null;

    if (this.wrappedCalloutView == null) {
      this.wrapCalloutView();
    }

    if (this.calloutView.getTooltip()) {
      return this.wrappedCalloutView;
    } else {
      return null;
    }
  }

  public View getInfoContents() {
    if (this.calloutView == null) return null;

    if (this.wrappedCalloutView == null) {
      this.wrapCalloutView();
    }

    if (this.calloutView.getTooltip()) {
      return null;
    } else {
      return this.wrappedCalloutView;
    }
  }

  private void wrapCalloutView() {
    // some hackery is needed to get the arbitrary infowindow view to render centered, and
    // with only the width/height that it needs.
    if (this.calloutView == null || this.calloutView.getChildCount() == 0) {
      return;
    }

    LinearLayout LL = new LinearLayout(context);
    LL.setOrientation(LinearLayout.VERTICAL);
    LL.setLayoutParams(new LinearLayout.LayoutParams(
        this.calloutView.width,
        this.calloutView.height,
        0f
    ));


    LinearLayout LL2 = new LinearLayout(context);
    LL2.setOrientation(LinearLayout.HORIZONTAL);
    LL2.setLayoutParams(new LinearLayout.LayoutParams(
        this.calloutView.width,
        this.calloutView.height,
        0f
    ));

    LL.addView(LL2);
    LL2.addView(this.calloutView);

    this.wrappedCalloutView = LL;
  }

  private int getDrawableResourceByName(String name) {
    return getResources().getIdentifier(
        name,
        "drawable",
        getContext().getPackageName());
  }

  private BitmapDescriptor getBitmapDescriptorByName(String name) {
    return BitmapDescriptorFactory.fromResource(getDrawableResourceByName(name));
  }

  public boolean isFilteredOut() {
    return filteredOut;
  }

  public void setOff(boolean isFilteredOut) {
    filteredOut = isFilteredOut;
    if (filteredOut && marker != null && marker.getTag() != null)
      marker.remove();
    else if (!filteredOut) {
      readdToMapIfNotFilteredOut();
    }
  }

  public void setSelected(boolean isSelected) {
    if (mapView != null) {
      if (this.selected && !isSelected) mapView.deselectIfSelected(this);
      else if (isSelected) mapView.setSelectedMarker(this);
    }
    this.selected = isSelected;
  }

  public void setSelectedFromClick(boolean isSelected) {
    this.selected = isSelected;
  }

  private Bitmap getScaledDownBitmap(Bitmap b) {
    return Bitmap.createScaledBitmap(b, (int) (b.getWidth() * PIN_SCALE_FACTOR), (int) (b.getHeight() * PIN_SCALE_FACTOR), true);
  }
}
