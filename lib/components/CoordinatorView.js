import React from "react";
import {requireNativeComponent} from 'react-native';

class CoordinatorView extends React.Component {

    constructor(props) {
        super(props);
    }

    render() {
        return <MyCoordinatorView {...this.props} />;
    }

}

var MyCoordinatorView = requireNativeComponent(`CAIRMap`);
export default CoordinatorView