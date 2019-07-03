import React from 'react';
import {
  findNodeHandle,
  NativeModules,
  Platform,
  requireNativeComponent,
} from 'react-native';
import PropTypes from 'prop-types';

export const BOTTOM_SHEET_TYPES = {
  EXPAND: 'EXPAND',
  HIDE: 'HIDE',
  COLLAPSED: 'COLLAPSED',
  ANCHOR: 'ANCHOR',
  DRAGGING: 'DRAGGING',
};

class CoordinatorView extends React.Component {
  constructor(props) {
    super(props);

    this._newStatusValue = this._newStatusValue.bind(this);
  }

  _newStatusValue(event) {
    if (this.props.newStatusValue) {
      this.props.newStatusValue(event.nativeEvent);
    }
  }

  setStatus(value) {
    this._runCommand('setStatus', [value]);
  }

  setShowHeader(value) {
    this._runCommand('showHeader', [value]);
  }

  _uiManagerCommand(name) {
    const UIManager = NativeModules.UIManager;
    const componentName = 'CAIRMap';

    if (!UIManager.getViewManagerConfig) {
      // RN < 0.58
      return UIManager[componentName].Commands[name];
    }

    // RN >= 0.58
    return UIManager.getViewManagerConfig(componentName).Commands[name];
  }

  _getHandle() {
    return findNodeHandle(this.coordinator);
  }

  _runCommand(name, args) {
    switch (Platform.OS) {
      case 'android':
        return NativeModules.UIManager.dispatchViewManagerCommand(
          this._getHandle(),
          this._uiManagerCommand(name),
          args
        );

      case 'ios':
        return this._mapManagerCommand(name)(this._getHandle(), ...args);

      default:
        return Promise.reject(`Invalid platform was passed: ${Platform.OS}`);
    }
  }

  render() {
    const nativeProps = this.props;
    const props = {
      ...nativeProps,
        newStatusValue: this._newStatusValue,
    };
    return (
      <MyCoordinatorView
        ref={ref => {
          this.coordinator = ref;
        }}
        {...props}
      />
    );
  }
}

//
const propTypes = {
  newStatusValue: PropTypes.func,
  peekHeight: PropTypes.number,
  anchorPoint: PropTypes.number,
  bottomSheetStatus: PropTypes.string,
};
//
CoordinatorView.propTypes = propTypes;

const MyCoordinatorView = requireNativeComponent('CAIRMap');
export default CoordinatorView;
