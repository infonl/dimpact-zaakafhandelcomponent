// SPDX-FileCopyrightText: 2026 INFO.nl
// SPDX-License-Identifier: EUPL-1.2+

import { ComponentFixture, TestBed } from "@angular/core/testing";
import { TranslateModule } from "@ngx-translate/core";
import { fromPartial } from "src/test-helpers";
import { LocationService } from "../../shared/location/location.service";
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
    getView: jest.fn(() => ({ fit: jest.fn() })),
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

describe(LocatieTonenComponent.name, () => {
  let fixture: ComponentFixture<LocatieTonenComponent>;
  let component: LocatieTonenComponent;

  const mockLocationService = {
    coordinateToAddress: jest.fn(),
    addressSuggest: jest.fn(),
    addressLookup: jest.fn(),
  };

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [TranslateModule.forRoot(), LocatieTonenComponent],
      providers: [{ provide: LocationService, useValue: mockLocationService }],
    }).compileComponents();

    fixture = TestBed.createComponent(LocatieTonenComponent);
    component = fixture.componentInstance;

    component.currentLocation = fromPartial<GeneratedType<"RestGeometry">>({
      type: "Point",
    });

    fixture.detectChanges();
  });

  it("renders the map container", () => {
    expect(
      fixture.nativeElement.querySelector(".open-layers-map"),
    ).not.toBeNull();
  });
});
