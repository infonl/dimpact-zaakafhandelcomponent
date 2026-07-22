/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { Coordinate } from "ol/coordinate.js";
import * as control from "ol/control.js";
import * as extent from "ol/extent.js";
import * as geom from "ol/geom.js";
import * as ol from "ol/index.js";
import * as interaction from "ol/interaction.js";
import * as layer from "ol/layer.js";
import * as proj from "ol/proj.js";
import * as source from "ol/source.js";
import * as style from "ol/style.js";
import WMTSTileGrid from "ol/tilegrid/WMTS.js";
import { environment } from "src/environments/environment";

/**
 * Shared OpenLayers setup for the zaak location map components (view and edit).
 * Owns the map, view and marker source so callers only deal with adding/clearing
 * markers and zooming; component-specific behaviour (event handlers, forms) stays
 * in the component via the exposed {@link map}.
 */
export class OpenLayersLocationMap {
  static readonly MAX_ZOOM = 14;

  private static readonly EPSG3857 = "EPSG:3857";
  private static readonly EXTENT_MATRIX = 20;
  private static readonly DEFAULT_ZOOM = 8;
  // Default center: middle of the Netherlands.
  private static readonly DEFAULT_CENTER = [631711.827985, 6856275.890632];

  private static readonly vectorStyle = new style.Style({
    fill: new style.Fill({ color: "rgba(255, 255, 255, 0.5)" }),
    stroke: new style.Stroke({ color: "#ff0000", width: 2 }),
  });

  readonly map: ol.Map;
  readonly view: ol.View;
  readonly source: source.Vector;

  constructor(private readonly markerStyle: style.Style) {
    const projection = proj.get(OpenLayersLocationMap.EPSG3857)!;
    const projectionExtent = projection.getExtent()!;
    const size = extent.getWidth(projectionExtent) / 256;
    const resolutions = new Array(OpenLayersLocationMap.EXTENT_MATRIX);
    const matrixIds = new Array(OpenLayersLocationMap.EXTENT_MATRIX);
    for (let z = 0; z < OpenLayersLocationMap.EXTENT_MATRIX; ++z) {
      resolutions[z] = size / Math.pow(2, z);
      matrixIds[z] = ("0" + z).slice(-2);
    }

    const backgroundLayer = new layer.Tile({
      source: new source.WMTS({
        projection: projection,
        layer: "standaard",
        format: "image/png",
        url: environment.BACKGROUND_MAP_API_URL,
        matrixSet: OpenLayersLocationMap.EPSG3857,
        style: "",
        tileGrid: new WMTSTileGrid({
          origin: extent.getTopLeft(projectionExtent),
          resolutions: resolutions,
          matrixIds: matrixIds,
        }),
        attributions: ["© OpenLayers en PDOK"],
      }),
    });

    this.source = new source.Vector();
    const locationLayer = new layer.Vector({
      source: this.source,
      style: OpenLayersLocationMap.vectorStyle,
    });

    this.view = new ol.View({
      projection: projection,
      center: OpenLayersLocationMap.DEFAULT_CENTER,
      constrainResolution: true,
      zoom: OpenLayersLocationMap.DEFAULT_ZOOM,
    });

    this.map = new ol.Map({
      interactions: interaction.defaults({ onFocusOnly: true }),
      controls: control.defaults({ zoom: false }),
      view: this.view,
      layers: [backgroundLayer, locationLayer],
    });

    this.map.addInteraction(new interaction.Modify({ source: this.source }));
  }

  setTarget(element: HTMLElement): void {
    this.map.setTarget(element);
  }

  addMarker(coordinate: Coordinate): void {
    const marker = new ol.Feature({
      geometry: new geom.Point(proj.fromLonLat(coordinate)),
    });
    marker.setStyle(this.markerStyle);
    this.source.addFeature(marker);
  }

  clearMarkers(): void {
    this.source
      .getFeatures()
      .forEach((feature) => this.source.removeFeature(feature));
    this.source.refresh();
  }

  zoomToMarker(coordinate: Coordinate): void {
    const mapCenter = proj.transform(coordinate, "EPSG:4326", "EPSG:3857");
    this.map.getView().setCenter(mapCenter);
    this.map.getView().fit(this.source.getExtent()!, {
      size: this.map.getSize(),
      maxZoom: OpenLayersLocationMap.MAX_ZOOM,
    });
  }

  resetView(): void {
    this.view.setZoom(OpenLayersLocationMap.DEFAULT_ZOOM);
    this.view.setCenter(OpenLayersLocationMap.DEFAULT_CENTER);
  }

  currentZoom(): number | undefined {
    return this.map.getView()?.getZoom();
  }
}
