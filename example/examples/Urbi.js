import React from 'react';
import {
  FlatList,
  Image,
  StyleSheet,
  Text,
  ToastAndroid,
  TouchableHighlight,
  View,
} from 'react-native';
import NestedScrollView from 'react-native-nested-scroll-view';

import MapView, {
  BOTTOM_SHEET_TYPES,
  CoordinatorView,
  Marker,
  ProviderPropType,
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
    };
    this.coordinator = React.createRef();
    this.map = React.createRef();

    this.onMapPress = this.onMapPress.bind(this);
    this.onMapReady = this.onMapReady.bind(this);
    this.onMarkerPress = this.onMarkerPress.bind(this);
    this.generateMarker = this.generateMarker.bind(this);
    this.onCityChange = this.onCityChange.bind(this);
    this.onCenterPress = this.onCenterPress.bind(this);
    this.onToggleHeaderPress = this.onToggleHeaderPress.bind(this);
    this.onFilterPress = this.onFilterPress.bind(this);
    this.onStatusChange = this.onStatusChange.bind(this);
    this.onTest = this.onTest.bind(this);
  }

  onMapReady() {
    setTimeout(() => {
      const bookMe = this.state.markers[0];
      bookMe.selected = true;
      this.setState({ markers: [...this.state.markers], selected: bookMe.id });
      setTimeout(() => {
        bookMe.booked = true;
        this.setState({ markers: [...this.state.markers] });
      }, 1500);
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
        const [oldProvider, oldId] = selected.split(' - ') ;
        const oldSelected = this.state.markers.find(m => m.provider === oldProvider && m.id === oldId);
        if (oldSelected) oldSelected.selected = false;
      }
      const [provider, id] = key.split(' - ');
      const selectedMarker = this.state.markers.find(m => m.provider === provider && m.id === id);
      selectedMarker.selected = true;
      this.setState({ selected: key, markers: [...this.state.markers] });
    }
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

  onToggleHeaderPress() {
    this.setState({ showHeader: !this.state.showHeader });
    this.coordinator.current.setShowHeader(!this.state.showHeader)
  }

  onFilterPress() {
    this.state.markers.forEach(m => {
      m.off = Math.random() > 0.4;
    });
    this.setState({ markers: [...this.state.markers] });
  }

  onTest(value) {
    this.coordinator.current.setStatus(value);
  }

  render() {
    return (
      <View style={styles.container}>
        <CoordinatorView
          ref={this.coordinator}
          style={{ flex: 1, width: '100%' }}
          peekHeight={offsets.COLLAPSED}
          anchorPoint={offsets.ANCHOR}
          onStatusChange={this.onStatusChange}
        >
          <MapView
            ref={this.map}
            provider={this.props.provider}
            style={styles.map}
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
            showsUserLocation
          >
            {this.state.markers.map(this.generateMarker)}
          </MapView>

          <NestedScrollView>
            {this.state.selected ? (
              <View style={styles.bottomPanel}>
                <Text style={styles.text}>Selected: {this.state.selected}</Text>
              </View>
            ) : (
              <View>
                <Text
                  style={{
                    backgroundColor: 'red',
                    height: 100,
                    textAlign: 'center',
                  }}
                >
                  TESTO 1
                </Text>
                <Text
                  style={{
                    backgroundColor: 'red',
                    height: 100,
                    textAlign: 'center',
                  }}
                >
                  TESTO 1
                </Text>
                <FlatList
                  data={[
                    { key: 'Devin' },
                    { key: 'Jackson' },
                    { key: 'James' },
                    { key: 'Joel' },
                    { key: 'John' },
                    { key: 'Jillian' },
                    { key: 'Jimmy' },
                    { key: 'Julie' },
                    { key: 'A' },
                    { key: 'B' },
                    { key: 'C' },
                    { key: 'D' },
                    { key: 'F' },
                    { key: 'G' },
                    { key: 'H' },
                    { key: 'Dq' },
                    { key: 'Dvv' },
                  ]}
                  renderItem={({ item }) => (
                    <Text style={styles.item}>{item.key}</Text>
                  )}
                />
              </View>
            )}
          </NestedScrollView>
          <View>
              <Text
                style={{
                  backgroundColor: '#2e5263',
                  padding: 20,
                  textAlign: 'center',
                  color: 'white',
                }}
              >
                  header
              </Text>
          </View>
        </CoordinatorView>
        <View style={styles.toggleHeaderButton}>
          <TouchableHighlight
            style={styles.centerButton}
            onPress={this.onToggleHeaderPress}
          >
            <Text style={styles.locationButtonText}>toggle header</Text>
          </TouchableHighlight>
        </View>
        <View style={styles.filterButton}>
          <TouchableHighlight
            style={styles.centerButton}
            onPress={this.onFilterPress}
          >
            <Text style={styles.locationButtonText}>filter</Text>
          </TouchableHighlight>
        </View>
        <View style={styles.thirdButton}>
          <View style={styles.childRowContainer}>
            <TouchableHighlight
              style={styles.centerButtonMargin}
              onPress={() => {
                this.onTest(BOTTOM_SHEET_TYPES.EXPAND);
              }}
            >
              <Text style={styles.locationButtonText}>EXPAND</Text>
            </TouchableHighlight>

            <TouchableHighlight
              style={styles.centerButtonMargin}
              onPress={() => {
                this.onTest(BOTTOM_SHEET_TYPES.COLLAPSED);
              }}
            >
              <Text style={styles.locationButtonText}>COLLAPSE</Text>
            </TouchableHighlight>
            <TouchableHighlight
              style={styles.centerButtonMargin}
              onPress={() => {
                this.onTest(BOTTOM_SHEET_TYPES.ANCHOR);
              }}
            >
              <Text style={styles.locationButtonText}>ANCHOR</Text>
            </TouchableHighlight>
          </View>
        </View>
      </View>
    );
  }
}

Urbi.propTypes = {
  provider: ProviderPropType,
};

const styles = StyleSheet.create({
  container: {
    ...StyleSheet.absoluteFillObject,
    justifyContent: 'flex-end',
    alignItems: 'center',
  },
  map: {
    ...StyleSheet.absoluteFillObject,
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
    backgroundColor: '#ffffff',
    minHeight: 500,
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
