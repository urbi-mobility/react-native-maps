import React from 'react';
import {
  Platform,
  View,
  StyleSheet,
  TouchableOpacity,
  ScrollView,
  Text,
  Switch,
} from 'react-native';
import { PROVIDER_GOOGLE, PROVIDER_DEFAULT } from 'react-native-maps';
import Urbi from './examples/Urbi';

const IOS = Platform.OS === 'ios';
const ANDROID = Platform.OS === 'android';

function makeExampleMapper(useGoogleMaps) {
  if (useGoogleMaps) {
    return example => [
      example[0],
      [example[1], example[3]].filter(Boolean).join(' '),
    ];
  }
  return example => example;
}

export default class App extends React.Component {
  constructor(props) {
    super(props);

    this.state = {
      Component: null,
      useGoogleMaps: ANDROID,
    };
  }

  renderExample([Component, title]) {
    return (
      <TouchableOpacity
        key={title}
        style={styles.button}
        onPress={() => this.setState({ Component })}
      >
        <Text>{title}</Text>
      </TouchableOpacity>
    );
  }

  renderBackButton() {
    return (
      <TouchableOpacity
        style={styles.back}
        onPress={() => this.setState({ Component: null })}
      >
        <Text style={styles.backButton}>&larr;</Text>
      </TouchableOpacity>
    );
  }

  renderGoogleSwitch() {
    return (
      <View>
        <Text>Use GoogleMaps?</Text>
        <Switch
          onValueChange={value => this.setState({ useGoogleMaps: value })}
          style={{ marginBottom: 10 }}
          value={this.state.useGoogleMaps}
        />
      </View>
    );
  }

  renderExamples(examples) {
    const { Component, useGoogleMaps } = this.state;

    return (
      <View style={styles.container}>
        {Component && (
          <Component
            provider={useGoogleMaps ? PROVIDER_GOOGLE : PROVIDER_DEFAULT}
          />
        )}
        {Component && this.renderBackButton()}
        {!Component && (
          <ScrollView
            style={StyleSheet.absoluteFill}
            contentContainerStyle={styles.scrollview}
            showsVerticalScrollIndicator={false}
          >
            {IOS && this.renderGoogleSwitch()}
            {examples.map(example => this.renderExample(example))}
          </ScrollView>
        )}
      </View>
    );
  }

  render() {
    return (
      <Urbi
        provider={this.state.useGoogleMaps ? PROVIDER_GOOGLE : PROVIDER_DEFAULT}
      />
    );
    // return this.renderExamples([
    //   // [<component>, <component description>, <Google compatible>, <Google add'l description>]
    //   [StaticMap, 'StaticMap', true],
    //   [DisplayLatLng, 'Tracking Position', true, '(incomplete)'],
    //   [ViewsAsMarkers, 'Arbitrary Views as Markers', true],
    //   [EventListener, 'Events', true, '(incomplete)'],
    //   [MarkerTypes, 'Image Based Markers', true],
    //   [DraggableMarkers, 'Draggable Markers', true],
    //   [PolygonCreator, 'Polygon Creator', true],
    //   [PolylineCreator, 'Polyline Creator', true],
    //   [GradientPolylines, 'Gradient Polylines', true],
    //   [AnimatedViews, 'Animating with MapViews'],
    //   [AnimatedMarkers, 'Animated Marker Position'],
    //   [Callouts, 'Custom Callouts', true],
    //   [Overlays, 'Circles, Polygons, and Polylines', true],
    //   [DefaultMarkers, 'Default Markers', true],
    //   [CustomMarkers, 'Custom Markers', true],
    //   [TakeSnapshot, 'Take Snapshot', true, '(incomplete)'],
    //   [CachedMap, 'Cached Map'],
    //   [LoadingMap, 'Map with loading'],
    //   [MapBoundaries, 'Get visible map boundaries', true],
    //   [FitToSuppliedMarkers, 'Focus Map On Markers', true],
    //   [FitToCoordinates, 'Fit Map To Coordinates', true],
    //   [LiteMapView, 'Android Lite MapView'],
    //   [CustomTiles, 'Custom Tiles', true],
    //   [WMSTiles, 'WMS Tiles', true],
    //   [ZIndexMarkers, 'Position Markers with Z-index', true],
    //   [MapStyle, 'Customize the style of the map', true],
    //   [LegalLabel, 'Reposition the legal label', true],
    //   [SetNativePropsOverlays, 'Update native props', true],
    //   [CustomOverlay, 'Custom Overlay Component', true],
    //   [TestIdMarkers, 'Test ID for Automation', true],
    //   [MapKml, 'Load Map with KML', true],
    //   [BugMarkerWontUpdate, 'BUG: Marker Won\'t Update (Android)', true],
    //   [ImageOverlayWithAssets, 'Image Overlay Component with Assets', true],
    //   [ImageOverlayWithURL, 'Image Overlay Component with URL', true],
    //   [AnimatedNavigation, 'Animated Map Navigation', true],
    //   [OnPoiClick, 'On Poi Click', true],
    //   [IndoorMap, 'Indoor Map', true],
    //   [CameraControl, 'CameraControl', true],
    //   [MassiveCustomMarkers, 'MassiveCustomMarkers', true],
    // ]
    // // Filter out examples that are not yet supported for Google Maps on iOS.
    // .filter(example => ANDROID || (IOS && (example[2] || !this.state.useGoogleMaps)))
    // .map(makeExampleMapper(IOS && this.state.useGoogleMaps))
    // );
  }
}

const styles = StyleSheet.create({
  container: {
    ...StyleSheet.absoluteFillObject,
    justifyContent: 'flex-end',
    alignItems: 'center',
  },
  scrollview: {
    alignItems: 'center',
    paddingVertical: 40,
  },
  button: {
    flex: 1,
    marginTop: 10,
    backgroundColor: 'rgba(220,220,220,0.7)',
    paddingHorizontal: 18,
    paddingVertical: 12,
    borderRadius: 20,
  },
  back: {
    position: 'absolute',
    top: 20,
    left: 12,
    backgroundColor: 'rgba(255,255,255,0.4)',
    padding: 12,
    borderRadius: 20,
    width: 80,
    alignItems: 'center',
    justifyContent: 'center',
  },
  backButton: { fontWeight: 'bold', fontSize: 30 },
  googleSwitch: { marginBottom: 10 },
});
