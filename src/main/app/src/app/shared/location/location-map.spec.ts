/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import * as ol from "ol/index.js";
import * as source from "ol/source.js";
import * as style from "ol/style.js";
import { fromPartial } from "src/test-helpers";
import { LocationMap } from "./location-map";

jest.mock("ol/control.js", () => ({ defaults: jest.fn(() => []) }));
jest.mock("ol/coordinate.js", () => ({}));
jest.mock("ol/extent.js", () => ({
  getWidth: jest.fn(() => 0),
  getTopLeft: jest.fn(() => [0, 0]),
}));
jest.mock("ol/geom.js", () => ({ Point: jest.fn() }));
jest.mock("ol/index.js", () => {
  const view = {
    fit: jest.fn(),
    getZoom: jest.fn(() => 8),
    setCenter: jest.fn(),
  };
  return {
    Map: jest.fn(() => ({
      setTarget: jest.fn(),
      getView: jest.fn(() => view),
      getSize: jest.fn(() => [100, 100]),
      addInteraction: jest.fn(),
      on: jest.fn(),
    })),
    View: jest.fn(() => ({ setZoom: jest.fn(), setCenter: jest.fn() })),
    Feature: jest.fn(() => ({ setStyle: jest.fn() })),
  };
});
jest.mock("ol/interaction.js", () => ({
  defaults: jest.fn(() => []),
  Modify: jest.fn(),
}));
jest.mock("ol/layer.js", () => ({ Tile: jest.fn(), Vector: jest.fn() }));
jest.mock("ol/proj.js", () => ({
  get: jest.fn(() => ({ getExtent: jest.fn(() => [0, 0, 100, 100]) })),
  fromLonLat: jest.fn(() => [0, 0]),
  transform: jest.fn(() => [0, 0]),
}));
jest.mock("ol/source.js", () => ({
  WMTS: jest.fn(),
  Vector: jest.fn(() => ({
    addFeature: jest.fn(),
    getFeatures: jest.fn(() => []),
    removeFeature: jest.fn(),
    refresh: jest.fn(),
    getExtent: jest.fn(() => [0, 0, 100, 100]),
  })),
}));
jest.mock("ol/style.js", () => ({
  Style: jest.fn(),
  Fill: jest.fn(),
  Stroke: jest.fn(),
  Text: jest.fn(),
}));
jest.mock("ol/tilegrid/WMTS.js", () => jest.fn());

describe(LocationMap.name, () => {
  const markerStyle = fromPartial<style.Style>({});
  let locationMap: LocationMap;

  beforeEach(() => {
    jest.clearAllMocks();
    locationMap = new LocationMap(markerStyle);
  });

  it("creates a map, view and marker source", () => {
    expect(ol.Map).toHaveBeenCalledTimes(1);
    expect(ol.View).toHaveBeenCalledTimes(1);
    expect(source.Vector).toHaveBeenCalledTimes(1);
    expect(locationMap.map).toBeDefined();
    expect(locationMap.view).toBeDefined();
    expect(locationMap.source).toBeDefined();
  });

  it("adds a modify interaction on construction", () => {
    expect(jest.mocked(locationMap.map.addInteraction)).toHaveBeenCalledTimes(1);
  });

  it("targets the map at the given element", () => {
    const element = document.createElement("div");

    locationMap.setTarget(element);

    expect(jest.mocked(locationMap.map.setTarget)).toHaveBeenCalledWith(element);
  });

  it("styles a feature with the marker style and adds it to the source", () => {
    locationMap.addMarker([1, 2]);

    const feature = jest.mocked(ol.Feature).mock.results[0].value;
    expect(jest.mocked(feature.setStyle)).toHaveBeenCalledWith(markerStyle);
    expect(jest.mocked(locationMap.source.addFeature)).toHaveBeenCalledWith(
      feature,
    );
  });

  it("clears existing markers and refreshes the source", () => {
    locationMap.clearMarkers();

    expect(jest.mocked(locationMap.source.getFeatures)).toHaveBeenCalledTimes(1);
    expect(jest.mocked(locationMap.source.refresh)).toHaveBeenCalledTimes(1);
  });

  it("centers and fits the view when zooming to a marker", () => {
    locationMap.zoomToMarker([1, 2]);

    const view = locationMap.map.getView();
    expect(jest.mocked(view.setCenter)).toHaveBeenCalledTimes(1);
    expect(jest.mocked(view.fit)).toHaveBeenCalledTimes(1);
  });

  it("resets the view to the default zoom and center", () => {
    locationMap.resetView();

    expect(jest.mocked(locationMap.view.setZoom)).toHaveBeenCalledWith(8);
    expect(jest.mocked(locationMap.view.setCenter)).toHaveBeenCalledTimes(1);
  });

  it("returns the current zoom of the view", () => {
    expect(locationMap.currentZoom()).toBe(8);
  });
});
