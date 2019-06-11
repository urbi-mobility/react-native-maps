import React from "react";
import {requireNativeComponent, View, ViewPropTypes} from 'react-native';
import PropTypes from 'prop-types';

class CoordinatorView extends React.Component {


    constructor(props) {
        super(props);
        console.log("CoordinatorView CON")

        this._clickHeader = this._clickHeader.bind(this)
    }

    _clickHeader(event) {
        if (this.props.clickHeader) {
            this.props.clickHeader(event.nativeEvent);
        }
    }


    render() {
        let props = {
            style: this.props.style,
            clickHeader: this._clickHeader
        }
        return <MyCoordinatorView {...props}  />;
    }

}

// if ViewPropTypes is not defined fall back to View.propType (to support RN < 0.44)
const viewPropTypes = ViewPropTypes || View.propTypes;

const propTypes = {
    ...viewPropTypes,

    style: viewPropTypes.style,

    clickHeader: PropTypes.func
}

CoordinatorView.propTypes = propTypes

var MyCoordinatorView = requireNativeComponent(`CAIRMap`);
export default CoordinatorView