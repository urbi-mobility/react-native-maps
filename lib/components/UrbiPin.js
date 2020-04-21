import PropTypes from 'prop-types';
import React from 'react';
import {
  Animated,
  ViewPropTypes,
  View,
  requireNativeComponent,
} from 'react-native';

const viewConfig = {
  uiViewClassName: 'AIR<provider>MapUrbiMarker',
  validAttributes: {
    coordinate: true,
  },
};

// if ViewPropTypes is not defined fall back to View.propType (to support RN < 0.44)
const viewPropTypes = ViewPropTypes || View.propTypes;

const propTypes = {
  ...viewPropTypes,

  /**
   * The unique id for the pin
   */
  uId: PropTypes.string.isRequired,

  /**
   * The id of the image to be used as the pin's icon, as indexed by the parent
   * map's imageIds dict.
   */
  img: PropTypes.string.isRequired,

  /**
   * The coordinates for the pin ([lat, lon]).
   */
  c: PropTypes.array.isRequired,

  /**
   * Callback that is called when the user presses on the marker
   */
  onPress: PropTypes.func,

  /**
   * Whether this marker should not be displayed on the map.
   * Default: false
   */
  off: PropTypes.bool,

  /**
   * Whether this marker is currently selected.
   * Default: false
   */
  selected: PropTypes.bool,
};

const defaultProps = {
  selected: false,
  off: false,
};

const AIRMapUrbiMarker = requireNativeComponent('AIRMapUrbiMarker', UrbiPin);

class UrbiPin extends React.Component {
  setNativeProps(props) {
    this.marker.setNativeProps(props);
  }

  render() {
    return (
      <AIRMapUrbiMarker
        ref={ref => {
          this.marker = ref;
        }}
        {...this.props}
        onPress={event => {
          if (this.props.onPress) {
            this.props.onPress(event);
          }
        }}
      />
    );
  }
}

UrbiPin.propTypes = propTypes;
UrbiPin.defaultProps = defaultProps;
UrbiPin.viewConfig = viewConfig;

UrbiPin.Animated = Animated.createAnimatedComponent(UrbiPin);

export default UrbiPin;
