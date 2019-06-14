import React from "react";
import {requireNativeComponent, View, ViewPropTypes} from 'react-native';
import PropTypes from 'prop-types';

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


    render() {
        let nativeProps=this.props
        let props = {
            ...nativeProps,
            clickHeader: this._clickHeader
        }
        return <MyCoordinatorView {...props}  />;
    }

}

//
const propTypes = {
    clickHeader: PropTypes.func
}
//
CoordinatorView.propTypes = propTypes

let MyCoordinatorView = requireNativeComponent(`CAIRMap`);
export default CoordinatorView