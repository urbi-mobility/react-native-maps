import React from 'react';
import {
  Image,
  PermissionsAndroid,
  StyleSheet,
  Text,
  ToastAndroid,
  TouchableHighlight,
  View,
} from 'react-native';

import MapView, { Marker, ProviderPropType, Polygon } from 'react-native-maps';
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
      image={pins[`ic_pin_${v.provider}${v.booked ? '_highlighted' : ''}`]}
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
    this.onDeselectPress = this.onDeselectPress.bind(this);
    this.onStatusChange = this.onStatusChange.bind(this);
    this.onTest = this.onTest.bind(this);
    this.renderPolygons = this.renderPolygons.bind(this);
    this.onToggleHighlight = this.onToggleHighlight.bind(this);
    this.onUserLocationUpdate = this.onUserLocationUpdate.bind(this)
  }

  UNSAFE_componentWillMount() {
    PermissionsAndroid.request(
      PermissionsAndroid.PERMISSIONS.ACCESS_FINE_LOCATION
    ).then(granted => {
      if (granted === PermissionsAndroid.RESULTS.GRANTED) {
        this.onMapReady();
        if(this.map.current){
          this.map.current.startLocationUpdates();
        }
      }
    });
  }

  onMapReady() {
    setTimeout(() => {
      const bookMe = this.state.markers[0];
      bookMe.selected = true;
      this.setState({ markers: [...this.state.markers], selected: bookMe.id });
    }, 1500);
  }

  onMapPress() {
    const { selected } = this.state;
    if (selected) {
      const [oldProvider, oldId] = selected.split(' - ');
      const oldSelected = this.state.markers.find(
        m => m.provider === oldProvider && m.id === oldId
      );
      if (oldSelected) {
        ToastAndroid.show(
          `${oldSelected.provider}-${oldSelected.id} deselected`,
          ToastAndroid.SHORT
        );
        oldSelected.selected = false;
      }
    }
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
        const oldSelected = this.state.markers.find(
          m => m.provider === oldProvider && m.id === oldId
        );
        if (oldSelected) {
          oldSelected.selected = false;
        }
      }
      const [provider, id] = key.split(' - ');
      const selectedMarker = this.state.markers.find(
        m => m.provider === provider && m.id === id
      );
      selectedMarker.selected = true;
      ToastAndroid.show(`new selection: ${key}`, ToastAndroid.SHORT);
      this.setState({ selected: key });
    };
  }

  onCityPress(e) {
    ToastAndroid.show(`pressed ${e.nativeEvent.id}`, ToastAndroid.SHORT);
  }

  onCityChange(e) {
    const city = e.nativeEvent.city;
    ToastAndroid.show(`changed city to ${city}`, ToastAndroid.SHORT);
    if (city !== 'unset') {
      this.setState({
        markers: vehicleLists[city].vehicles.map(v => ({
          ...v,
          selected: false,
        })),
        city,
      });
    }
  }

  onUserLocationUpdate(e) {
    const coordinate = e.nativeEvent.coordinate
    ToastAndroid.show(`onUserLocationUpdate ${JSON.stringify(coordinate)}`, ToastAndroid.SHORT);
  }

  onCenterPress() {
    this.map.current.centerToUserLocation();
  }

  onFilterPress() {
    this.setState({ hideArea: !this.state.hideArea });
  }

  onDeselectPress() {
    this.onMapPress();
    ToastAndroid.show('deselected', ToastAndroid.SHORT);
  }

  onToggleHighlight() {
    const [first, second, ...others] = this.state.markers;
    this.setState({
      markers: [
        first,
        { ...second, booked: !(second.booked || false) },
        ...others,
      ],
    });
  }

  onTest(value) {
    this.coordinator.current.setStatus(value);
  }

  renderPolygons() {
    return this.state.city === 'berlin' && !this.state.hideArea
      ? [
          <Polygon
            key="p1"
            fillColor="rgba(255, 0, 0, 0.2)"
            strokeColor="red"
            coordinates={[
              { latitude: 52.52198, longitude: 13.40728 },
              { latitude: 52.52198, longitude: 13.41299 },
              { latitude: 52.52008, longitude: 13.41299 },
              { latitude: 52.52008, longitude: 13.40728 },
            ]}
          />,
        ]
      : undefined;
  }

  render() {
    return (
      <View style={styles.container}>
        <MapView
          ref={this.map}
          provider={this.props.provider}
          backendURL="https://urbitunnel2.eu.ngrok.io"
          backendToken="fake"
          showPathIfCloserThan={1800}
          style={StyleSheet.absoluteFillObject}
          initialRegion={this.state.region}
          onPress={this.onMapPress}
          onMapReady={this.onMapReady}
          moveOnMarkerPress={false}
          switchToCityPinsDelta={SWITCH_TO_PINS_LAT_LON_DELTA}
          showsMyLocationButton={false}
          cityPins={cityPins}
          onCityPress={this.onCityPress}
          onCityChange={this.onCityChange}
          onUserLocationUpdate={this.onUserLocationUpdate}
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
        <View style={styles.buttonsFirstRow}>
          <View style={styles.childRowContainer}>
            <TouchableHighlight
              style={styles.button}
              onPress={this.onDeselectPress}
            >
              <Text style={styles.buttonLabel}>deselect</Text>
            </TouchableHighlight>
            <TouchableHighlight
              style={styles.button}
              onPress={this.onFilterPress}
            >
              <Text style={styles.buttonLabel}>
                {this.state.hideArea ? 'show' : 'hide'} area
              </Text>
            </TouchableHighlight>
            <TouchableHighlight
              style={styles.button}
              onPress={this.onToggleHighlight}
            >
              <Text style={styles.buttonLabel}>toggle booked</Text>
            </TouchableHighlight>
          </View>
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

const buttonRow = {
  position: 'absolute',
  right: 0,
  justifyContent: 'center',
  alignItems: 'flex-end',
  borderRadius: 10,
};

const styles = StyleSheet.create({
  container: {
    flex: 1,
    alignItems: 'center',
  },
  map: StyleSheet.absoluteFillObject,
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
  childRowContainer: {
    flexDirection: 'row',
    flex: 1,
    width: '100%',
  },
  buttonsFirstRow: {
    ...buttonRow,
    top: 10,
  },
  buttonsSecondRow: {
    ...buttonRow,
    top: 45,
  },
  buttonsThirdRow: {
    ...buttonRow,
    top: 80,
  },
  buttonLabel: {
    fontSize: 10,
    color: '#ffffff',
  },
  button: {
    backgroundColor: '#ec008b',
    paddingHorizontal: 10,
    paddingVertical: 5,
    borderRadius: 10,
    alignItems: 'center',
    margin: 5,
  },
  flatListItem: {
    padding: 10,
    fontSize: 18,
    height: 44,
    backgroundColor: 'blue',
  },
});

export default Urbi;
