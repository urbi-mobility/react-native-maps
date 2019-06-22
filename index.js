import MapView, {Animated, MAP_TYPES, ProviderPropType} from './lib/components/MapView';
import Marker from './lib/components/MapMarker.js';
import Overlay from './lib/components/MapOverlay.js';
import {BOTTOM_SHEET_TYPES} from "./lib/components/CoordinatorView";

export { default as Polyline } from './lib/components/MapPolyline.js';
export { default as Polygon } from './lib/components/MapPolygon.js';
export { default as Circle } from './lib/components/MapCircle.js';
export { default as UrlTile } from './lib/components/MapUrlTile.js';
export { default as WMSTile } from './lib/components/MapWMSTile.js';
export { default as LocalTile } from './lib/components/MapLocalTile.js';
export { default as Callout } from './lib/components/MapCallout.js';
export { default as CalloutSubview } from './lib/components/MapCalloutSubview.js';
export { default as AnimatedRegion } from './lib/components/AnimatedRegion.js';
export {default as CoordinatorView} from "./lib/components/CoordinatorView";


export { Marker, Overlay };
export { Animated, MAP_TYPES, ProviderPropType };

export const PROVIDER_GOOGLE = MapView.PROVIDER_GOOGLE;
export const PROVIDER_DEFAULT = MapView.PROVIDER_DEFAULT;

export const MarkerAnimated = Marker.Animated;
export const OverlayAnimated = Overlay.Animated;
export {BOTTOM_SHEET_TYPES}

export default MapView;

