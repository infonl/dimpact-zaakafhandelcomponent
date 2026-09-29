// SPDX-FileCopyrightText: 2026 INFO.nl
// SPDX-License-Identifier: EUPL-1.2+

import { ComponentFixture, TestBed } from "@angular/core/testing";
import { TranslateModule } from "@ngx-translate/core";
import { screen } from "@testing-library/angular";
import { of } from "rxjs";
import { fromPartial } from "src/test-helpers";
import { sleep } from "../../../../setupJest";
import {
  AddressResult,
  LocationService,
} from "../../shared/location/location.service";
import { OpenLayersLocationMap } from "../../shared/location/open-layers-location-map";
import { GeneratedType } from "../../shared/utils/generated-types";
import { LocatieTonenComponent } from "./zaak-locatie-tonen.component";

jest.mock("ol/control.js", () => ({ defaults: jest.fn(() => []) }));
jest.mock("ol/coordinate.js", () => ({}));
jest.mock("ol/extent.js", () => ({
  getWidth: jest.fn(() => 0),
  getTopLeft: jest.fn(() => [0, 0]),
}));
jest.mock("ol/geom.js", () => ({
  Point: jest.fn(),
  Polygon: jest.fn(),
}));
jest.mock("ol/index.js", () => ({
  Map: jest.fn(() => ({
    setTarget: jest.fn(),
    getView: jest.fn(() => ({
      fit: jest.fn(),
      getZoom: jest.fn(() => 8),
      setCenter: jest.fn(),
    })),
    getSize: jest.fn(),
    addInteraction: jest.fn(),
    on: jest.fn(),
  })),
  View: jest.fn(),
  Feature: jest.fn(() => ({ setStyle: jest.fn() })),
}));
jest.mock("ol/interaction.js", () => ({
  defaults: jest.fn(() => []),
  Modify: jest.fn(),
}));
jest.mock("ol/layer.js", () => ({
  Tile: jest.fn(),
  Vector: jest.fn(),
}));
jest.mock("ol/proj.js", () => ({
  get: jest.fn(() => ({
    getExtent: jest.fn(() => [0, 0, 100, 100]),
    setExtent: jest.fn(),
  })),
  fromLonLat: jest.fn(() => [0, 0]),
  transform: jest.fn(() => [0, 0]),
}));
jest.mock("ol/proj/proj4.js", () => ({ register: jest.fn() }));
jest.mock("ol/source.js", () => ({
  WMTS: jest.fn(),
  Vector: jest.fn(() => ({
    addFeature: jest.fn(),
    clear: jest.fn(),
    getExtent: jest.fn(() => [0, 0, 100, 100]),
    getFeatures: jest.fn(() => []),
    removeFeature: jest.fn(),
    refresh: jest.fn(),
  })),
}));
jest.mock("ol/style.js", () => ({
  Style: jest.fn(),
  Fill: jest.fn(),
  Stroke: jest.fn(),
  Text: jest.fn(),
}));
jest.mock("ol/tilegrid/WMTS.js", () => jest.fn());
jest.mock("proj4", () => ({
  default: Object.assign(jest.fn(), { defs: jest.fn() }),
  defs: jest.fn(),
}));

const makePoint = (latitude: number, longitude: number) =>
  fromPartial<GeneratedType<"RestGeometry">>({
    type: "POINT",
    point: { latitude, longitude },
  });

const addressLookupResult = (weergavenaam: string) =>
  of({ response: { docs: [fromPartial<AddressResult>({ weergavenaam })] } });

describe(LocatieTonenComponent.name, () => {
  let fixture: ComponentFixture<LocatieTonenComponent>;
  let setTarget: jest.SpyInstance;
  let addMarker: jest.SpyInstance;
  let clearMarkers: jest.SpyInstance;
  let zoomToMarker: jest.SpyInstance;

  const locationService = {
    coordinateToAddress: jest.fn(() =>
      addressLookupResult("fakeStraat 1, fakeWoonplaats"),
    ),
    addressSuggest: jest.fn(),
    addressLookup: jest.fn(),
  };

  const setup = async (currentLocation: GeneratedType<"RestGeometry">) => {
    await TestBed.configureTestingModule({
      imports: [TranslateModule.forRoot(), LocatieTonenComponent],
      providers: [{ provide: LocationService, useValue: locationService }],
    }).compileComponents();

    fixture = TestBed.createComponent(LocatieTonenComponent);
    fixture.componentRef.setInput("currentLocation", currentLocation);
    fixture.detectChanges();
  };

  const waitForTheMap = async () => {
    await sleep();
    fixture.detectChanges();
  };

  const changeLocation = (currentLocation: GeneratedType<"RestGeometry">) => {
    fixture.componentRef.setInput("currentLocation", currentLocation);
    fixture.detectChanges();
  };

  beforeEach(() => {
    setTarget = jest.spyOn(OpenLayersLocationMap.prototype, "setTarget");
    addMarker = jest.spyOn(OpenLayersLocationMap.prototype, "addMarker");
    clearMarkers = jest.spyOn(OpenLayersLocationMap.prototype, "clearMarkers");
    zoomToMarker = jest.spyOn(OpenLayersLocationMap.prototype, "zoomToMarker");
  });

  it("attaches the map to its container", async () => {
    await setup(makePoint(52, 5));
    await waitForTheMap();

    expect(setTarget).toHaveBeenCalledTimes(1);
    expect(setTarget.mock.calls[0][0]).toHaveClass("open-layers-map");
  });

  it("waits for the map to be attached before placing the marker", async () => {
    await setup(makePoint(52, 5));

    expect(addMarker).not.toHaveBeenCalled();
    expect(locationService.coordinateToAddress).not.toHaveBeenCalled();

    await waitForTheMap();

    expect(addMarker).toHaveBeenCalledTimes(1);
    expect(locationService.coordinateToAddress).toHaveBeenCalledTimes(1);
  });

  it("places a marker on the location and zooms in on it", async () => {
    await setup(makePoint(52, 5));
    await waitForTheMap();

    expect(addMarker).toHaveBeenCalledWith([5, 52]);
    expect(zoomToMarker).toHaveBeenCalledWith([5, 52]);
  });

  it("shows the address nearest to the location", async () => {
    await setup(makePoint(52, 5));
    await waitForTheMap();

    expect(locationService.coordinateToAddress).toHaveBeenCalledWith([5, 52]);
    expect(screen.getByText("fakeStraat 1, fakeWoonplaats")).toBeVisible();
  });

  it("places no marker for a location that is not a point", async () => {
    await setup(
      fromPartial<GeneratedType<"RestGeometry">>({ type: "POLYGON" }),
    );
    await waitForTheMap();

    expect(addMarker).not.toHaveBeenCalled();
    expect(locationService.coordinateToAddress).not.toHaveBeenCalled();
  });

  it("places the marker on the latest location when the location changes before the map is attached", async () => {
    await setup(makePoint(52, 5));

    changeLocation(makePoint(53, 6));
    await waitForTheMap();

    expect(addMarker).toHaveBeenCalledTimes(1);
    expect(addMarker).toHaveBeenCalledWith([6, 53]);
  });

  it("moves the marker to a changed location and shows its nearest address", async () => {
    await setup(makePoint(52, 5));
    await waitForTheMap();
    locationService.coordinateToAddress.mockReturnValueOnce(
      addressLookupResult("fakeStraat 2, fakeWoonplaats"),
    );

    changeLocation(makePoint(53, 6));

    expect(clearMarkers).toHaveBeenCalledTimes(2);
    expect(addMarker).toHaveBeenLastCalledWith([6, 53]);
    expect(zoomToMarker).toHaveBeenLastCalledWith([6, 53]);
    expect(locationService.coordinateToAddress).toHaveBeenLastCalledWith([
      6, 53,
    ]);
    expect(screen.getByText("fakeStraat 2, fakeWoonplaats")).toBeVisible();
  });

  it("removes the marker when the location changes to one that is not a point", async () => {
    await setup(makePoint(52, 5));
    await waitForTheMap();

    changeLocation(
      fromPartial<GeneratedType<"RestGeometry">>({ type: "POLYGON" }),
    );

    expect(clearMarkers).toHaveBeenCalledTimes(2);
    expect(addMarker).toHaveBeenCalledTimes(1);
  });
});
