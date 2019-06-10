import React from "react";
import {requireNativeComponent} from 'react-native';
import PropTypes from 'prop-types';

class CoordinatorView extends React.Component {

    constructor(props) {
        super(props);
        console.log("CoordinatorView CON")

        this._clickHeader = this._clickHeader.bind(this)
    }

    _clickHeader(event) {
        console.log("_clickHeader")
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

CoordinatorView.propTypes = {
    clickHeader: PropTypes.func
}

var MyCoordinatorView = requireNativeComponent(`CAIRMap`);
export default CoordinatorView