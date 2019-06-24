import React from "react";
import {findNodeHandle, NativeModules, Platform, requireNativeComponent} from 'react-native';
import PropTypes from 'prop-types';

export const BOTTOM_SHEET_TYPES = {
    EXPAND: 'EXPAND',
    HIDE: 'HIDE',
    COLLAPSED: 'COLLAPSED',
    ANCHOR: 'ANCHOR',

};

class CoordinatorView extends React.Component {


    constructor(props) {
        super(props);

        this._clickHeader = this._clickHeader.bind(this)
    }

    _clickHeader(event) {
        if (this.props.clickHeader) {
            this.props.clickHeader(event.nativeEvent);
        }
    }

    setStatusExpandable(value) {
        this._runCommand("setStatusExpandable", [value])
    }

    _uiManagerCommand(name) {
        const UIManager = NativeModules.UIManager;
        const componentName = "CAIRMap"

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
        let nativeProps=this.props
        let props = {
            ...nativeProps,
            clickHeader: this._clickHeader
        }
        return <MyCoordinatorView
            ref={ref => {
                this.coordinator = ref;
            }}
            {...props}  />;
    }

}

//
const propTypes = {

    clickHeader: PropTypes.func,

    peekHeight: PropTypes.number,

    anchorPoint: PropTypes.number,

    statusBootonSheet: PropTypes.string
}
//
CoordinatorView.propTypes = propTypes

let MyCoordinatorView = requireNativeComponent(`CAIRMap`);
export default CoordinatorView