import React from 'react';
import {
  Image,
  StyleSheet,
  Text,
  ToastAndroid,
  TouchableHighlight,
  View,
} from 'react-native';

import MapView, {
  Marker,
  ProviderPropType,
  Polygon,
} from 'react-native-maps';
import berlinVehicleList from './assets/four-vehicles.json';
import hamburgVehicleList from './assets/vehicles-hamburg.json';
import cityList from './assets/cities.json';
import pins, { cityIcons } from './UrbiImages';

const LATITUDE = 52.520873;
const LONGITUDE = 13.409419;

const vehicleLists = {
  berlin: berlinVehicleList,
  hamburg: hamburgVehicleList,
};

const SWITCH_TO_PINS_LAT_LON_DELTA = 0.04;
export const DEFAULT_ZOOMED_IN_LAT_LON_DELTA = 0.0075;
export const SHOW_BICYCLES_LAT_LON_DELTA = 0.025;

const cityPins = cityList.cities.map(c => ({
  id: c.id,
  pos: { latitude: c.center.lat, longitude: c.center.lon },
  image: Image.resolveAssetSource(cityIcons[c.id]).uri,
  bounds: { topLeft: c.topLeft, bottomRight: c.bottomRight },
}));

const offsets = {
  ANCHOR: 200,
  COLLAPSED: 100,
  EXPAND: 200,
};

class Urbi extends React.Component {
  generateMarker = v => (
    <Marker
      key={v.id}
      centerOffset={{ x: 0, y: -19.5 }}
      coordinate={{ latitude: v.location.lat, longitude: v.location.lon }}
      image={v.booked ? pins.flagPink : pins[`ic_pin_${v.provider}`]}
      onPress={this.onMarkerPress(`${v.provider} - ${v.id}`)}
      tracksViewChanges={false}
      off={v.off}
      selected={v.selected}
    />
  );

  constructor(props) {
    super(props);

    this.state = {
      region: {
        latitude: LATITUDE,
        longitude: LONGITUDE,
        latitudeDelta: DEFAULT_ZOOMED_IN_LAT_LON_DELTA,
        longitudeDelta: DEFAULT_ZOOMED_IN_LAT_LON_DELTA,
      },
      markers: [],
      selected: null,
      city: 'berlin',
      showHeader: true,
      bottomOffset: offsets.ANCHOR,
      hideArea: false,
    };
    this.coordinator = React.createRef();
    this.map = React.createRef();

    this.onMapPress = this.onMapPress.bind(this);
    this.onMapReady = this.onMapReady.bind(this);
    this.onMarkerPress = this.onMarkerPress.bind(this);
    this.generateMarker = this.generateMarker.bind(this);
    this.onCityChange = this.onCityChange.bind(this);
    this.onCenterPress = this.onCenterPress.bind(this);
    this.onFilterPress = this.onFilterPress.bind(this);
    this.onStatusChange = this.onStatusChange.bind(this);
    this.onTest = this.onTest.bind(this);
    this.renderPolygons = this.renderPolygons.bind(this);
  }

  onMapReady() {
    setTimeout(() => {
      const bookMe = this.state.markers[0];
      bookMe.selected = true;
      this.setState({ markers: [...this.state.markers], selected: bookMe.id });
    }, 1500);
  }

  onMapPress() {
    this.setState({ selected: null });
  }

  onStatusChange(e) {
    ToastAndroid.show(
      `new status: ${e.nativeEvent.status}`,
      ToastAndroid.SHORT
    );
    this.setState({ bottomOffset: offsets[e.nativeEvent.status] });
  }

  onMarkerPress(key) {
    return () => {
      const { selected } = this.state;
      if (selected) {
        const [oldProvider, oldId] = selected.split(' - ');
        const oldSelected = this.state.markers.find(m => m.provider === oldProvider && m.id === oldId);
        if (oldSelected) oldSelected.selected = false;
      }
      const [provider, id] = key.split(' - ');
      const selectedMarker = this.state.markers.find(m => m.provider === provider && m.id === id);
      selectedMarker.selected = true;
      this.setState({ selected: key, markers: [...this.state.markers] });
    };
  }

  onCityPress(e) {
    ToastAndroid.show(`pressed ${e.nativeEvent.id}`, ToastAndroid.SHORT);
  }

  onCityChange(e) {
    const city = e.nativeEvent.city;
    ToastAndroid.show(`changed city to ${city}`, ToastAndroid.SHORT);
    if (city !== 'unset') {
      this.setState({ markers: vehicleLists[city].vehicles, city });
    }
  }

  onCenterPress() {
    this.map.current.centerToUserLocation();
  }

  onFilterPress() {
    this.setState({ hideArea: !this.state.hideArea });
  }

  onTest(value) {
    this.coordinator.current.setStatus(value);
  }

  renderPolygons() {
    return this.state.city === 'berlin' && !this.state.hideArea ? [
      <Polygon
        key="p1"
        fillColor="rgba(255, 0, 0, 0.2)"
        strokeColor="red"
        coordinates={[
          { latitude: 52.521980, longitude: 13.40728 },
          { latitude: 52.521980, longitude: 13.41299 },
          { latitude: 52.520080, longitude: 13.41299 },
          { latitude: 52.520080, longitude: 13.40728 },
        ]}
      />,
    ] : undefined;
  }

  render() {
    return (
      <View style={styles.container}>
        <MapView
          ref={this.map}
          provider={this.props.provider}
          style={StyleSheet.absoluteFillObject}
          initialRegion={this.state.region}
          onPress={this.onMapPress}
          onMapReady={this.onMapReady}
          moveOnMarkerPress={false}
          mapPadding={{ bottom: this.state.bottomOffset }}
          switchToCityPinsDelta={SWITCH_TO_PINS_LAT_LON_DELTA}
          showsMyLocationButton={false}
          cityPins={cityPins}
          onCityPress={this.onCityPress}
          onCityChange={this.onCityChange}
          mapPadding={{
            top: 0,
            right: 0,
            left: 0,
            bottom: this.state.selected ? 200 : 0,
          }}
          showsUserLocation
        >
          {this.state.markers.map(this.generateMarker)}
          {this.renderPolygons()}
        </MapView>
        <View style={styles.filterButton}>
          <TouchableHighlight
            style={styles.centerButton}
            onPress={this.onFilterPress}
          >
            <Text style={styles.locationButtonText}>{this.state.hideArea ? 'Show' : 'Hide'} area</Text>
          </TouchableHighlight>
        </View>
        <View style={styles.overlay}>
          <Text style={styles.text}>Current city: {this.state.city}</Text>
        </View>
        {this.state.selected && (
          <View style={styles.bottomPanel}>
            <Text style={styles.text}>Selected: {this.state.selected}</Text>
          </View>
        )}
      </View>
    );
  }
}

Urbi.propTypes = {
  provider: ProviderPropType,
};

const styles = StyleSheet.create({
  container: {
    flex: 1,
    alignItems: 'center',
  },
  childContainer: {
    flexDirection: 'column',
    flex: 1,
    width: '100%',
    backgroundColor: 'green',
  },
  childRowContainer: {
    flexDirection: 'row',
    flex: 1,
    width: '100%',
  },
  bottomPanel: {
    position: 'absolute',
    alignContent: 'center',
    bottom: 0,
    backgroundColor: '#ffffff',
    height: 200,
    left: 0,
    right: 0,
    width: '100%',
  },
  overlay: {
    position: 'absolute',
    bottom: 10,
    right: 0,
  },
  text: {
    textAlign: 'center',
  },
  item: {
    padding: 10,
    fontSize: 18,
    height: 44,
    backgroundColor: 'blue',
  },
  toggleHeaderButton: {
    position: 'absolute',
    top: 20,
    right: 20,
    height: 50,
    width: 120,
    justifyContent: 'center',
    alignItems: 'flex-end',
    borderRadius: 10,
  },
  filterButton: {
    position: 'absolute',
    top: 70,
    right: 20,
    height: 50,
    width: 50,
    justifyContent: 'center',
    alignItems: 'flex-end',
    borderRadius: 10,
  },
  thirdButton: {
    position: 'absolute',
    top: 120,
    right: 0,
    height: 50,
    justifyContent: 'center',
    alignItems: 'center',
    borderRadius: 10,
  },
  locationButtonText: {
    fontSize: 10,
    color: '#ffffff',
  },
  centerButton: {
    backgroundColor: '#ec008b',
    padding: 10,
    borderRadius: 10,
  },
  centerButtonMargin: {
    backgroundColor: '#ec008b',
    padding: 10,
    borderRadius: 10,
    margin: 5,
  },
  bubble: {
    backgroundColor: 'rgba(255,255,255,0.7)',
    paddingHorizontal: 18,
    paddingVertical: 12,
    borderRadius: 20,
  },
  latlng: {
    width: 200,
    alignItems: 'stretch',
  },
  button: {
    width: 80,
    paddingHorizontal: 12,
    alignItems: 'center',
    marginHorizontal: 10,
  },
  buttonContainer: {
    flexDirection: 'row',
    marginVertical: 20,
    backgroundColor: 'transparent',
  },
  spinner: {
    position: 'absolute',
    top: 50,
    backgroundColor: '#fff',
    borderRadius: 50,
    padding: 8,
  },
  topBar: {
    position: 'absolute',
    top: 0,
    left: 0,
    right: 0,
    height: 60,
    alignItems: 'center',
    justifyContent: 'flex-end',
    paddingBottom: 8,
    backgroundColor: '#ae016d',
  },
});

export default Urbi;
