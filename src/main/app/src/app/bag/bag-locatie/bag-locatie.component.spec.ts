// SPDX-FileCopyrightText: 2026 INFO.nl
// SPDX-License-Identifier: EUPL-1.2+

import { ComponentFixture, TestBed } from "@angular/core/testing";
import { TranslateModule } from "@ngx-translate/core";

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
  Map: jest.fn(() => {
    const view = { fit: jest.fn() };
    return {
      setTarget: jest.fn(),
      getView: jest.fn(() => view),
      getSize: jest.fn(() => [400, 300]),
    };
  }),
  View: jest.fn(),
  Feature: jest.fn(() => ({ setStyle: jest.fn() })),
}));
jest.mock("ol/interaction", () => ({
  defaults: jest.fn(() => []),
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
}));
jest.mock("ol/proj/proj4.js", () => ({ register: jest.fn() }));
jest.mock("ol/source.js", () => ({
  WMTS: jest.fn(),
  Vector: jest.fn(() => ({
    addFeature: jest.fn(),
    clear: jest.fn(),
    getExtent: jest.fn(() => [10, 20, 30, 40]),
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

import * as geom from "ol/geom.js";
import * as ol from "ol/index.js";
import * as source from "ol/source.js";
import { fromPartial } from "src/test-helpers";
import { sleep } from "../../../../setupJest";
import { GeneratedType } from "../../shared/utils/generated-types";
import { BagLocatieComponent } from "./bag-locatie.component";

type Geometry = GeneratedType<"RestGeometry">;

const point = (longitude: number, latitude: number) =>
  fromPartial<Geometry>({ type: "POINT", point: { longitude, latitude } });

const polygon = (...coordinates: [number, number][]) =>
  fromPartial<Geometry>({
    type: "POLYGON",
    polygon: [
      coordinates.map(([longitude, latitude]) => ({ longitude, latitude })),
    ] as unknown as Geometry["polygon"],
  });

describe(BagLocatieComponent.name, () => {
  let fixture: ComponentFixture<BagLocatieComponent>;

  const map = () => jest.mocked(ol.Map).mock.results.at(-1)!.value;
  const geometrieSource = () =>
    jest.mocked(source.Vector).mock.results.at(-1)!.value;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [BagLocatieComponent, TranslateModule.forRoot()],
    }).compileComponents();

    fixture = TestBed.createComponent(BagLocatieComponent);
  });

  function render(bagGeometrie?: Geometry) {
    if (bagGeometrie) {
      fixture.componentRef.setInput("bagGeometrie", bagGeometrie);
    }
    fixture.detectChanges();
  }

  function changeGeometrie(bagGeometrie?: Geometry) {
    fixture.componentRef.setInput("bagGeometrie", bagGeometrie);
    fixture.detectChanges();
  }

  it("attaches the map to its container once the view has been rendered", async () => {
    render(point(5.1, 52.1));
    await sleep();

    expect(map().setTarget).toHaveBeenCalledWith(
      expect.objectContaining({ id: "open-layers-map" }),
    );
  });

  it("draws nothing and does not zoom without a geometry", () => {
    render();

    expect(geometrieSource().addFeature).not.toHaveBeenCalled();
    expect(map().getView().fit).not.toHaveBeenCalled();
  });

  it("draws a point geometry once and zooms the map to it", () => {
    render(point(5.1, 52.1));

    expect(geom.Point).toHaveBeenCalledWith([5.1, 52.1]);
    expect(geometrieSource().addFeature).toHaveBeenCalledTimes(1);
    expect(map().getView().fit).toHaveBeenCalledWith(
      [10, 20, 30, 40],
      expect.objectContaining({ maxZoom: 14 }),
    );
  });

  it("draws a polygon geometry", () => {
    render(polygon([4.9, 52.3], [4.91, 52.3], [4.9, 52.31]));

    expect(geom.Polygon).toHaveBeenCalledWith(
      expect.arrayContaining([
        [
          [4.9, 52.3],
          [4.91, 52.3],
          [4.9, 52.31],
        ],
      ]),
    );
    expect(geometrieSource().addFeature).toHaveBeenCalledTimes(1);
  });

  it("draws every geometry of a geometry collection", () => {
    render(
      fromPartial<Geometry>({
        type: "GEOMETRY_COLLECTION",
        geometrycollection: [
          point(5.1, 52.1),
          polygon([4.9, 52.3], [4.91, 52.3], [4.9, 52.31]),
        ],
      }),
    );

    expect(geom.Point).toHaveBeenCalledWith([5.1, 52.1]);
    expect(geom.Polygon).toHaveBeenCalledTimes(1);
    expect(geometrieSource().addFeature).toHaveBeenCalledTimes(2);
    expect(map().getView().fit).toHaveBeenCalledTimes(1);
  });

  it("replaces the drawn geometry and zooms again when the geometry changes", () => {
    render(point(5.1, 52.1));

    changeGeometrie(point(4.9, 52.3));

    expect(geometrieSource().clear).toHaveBeenCalledTimes(1);
    expect(geom.Point).toHaveBeenLastCalledWith([4.9, 52.3]);
    expect(geometrieSource().addFeature).toHaveBeenCalledTimes(2);
    expect(map().getView().fit).toHaveBeenCalledTimes(2);
  });

  it("keeps the drawn geometry when the geometry is removed", () => {
    render(point(5.1, 52.1));

    changeGeometrie(undefined);

    expect(geometrieSource().clear).not.toHaveBeenCalled();
    expect(geometrieSource().addFeature).toHaveBeenCalledTimes(1);
    expect(map().getView().fit).toHaveBeenCalledTimes(1);
  });
});
