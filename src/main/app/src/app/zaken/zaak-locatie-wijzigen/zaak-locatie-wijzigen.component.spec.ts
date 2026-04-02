// SPDX-FileCopyrightText: 2026 INFO.nl
// SPDX-License-Identifier: EUPL-1.2+

import { ComponentFixture, TestBed } from "@angular/core/testing";
import { MatDrawer } from "@angular/material/sidenav";
import { TranslateModule } from "@ngx-translate/core";
import { fromPartial } from "@total-typescript/shoehorn";
import { LocationService } from "../../shared/location/location.service";
import { GeneratedType } from "../../shared/utils/generated-types";
import { ZakenService } from "../zaken.service";
import { CaseLocationEditComponent } from "./zaak-locatie-wijzigen.component";

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

describe(CaseLocationEditComponent.name, () => {
  let fixture: ComponentFixture<CaseLocationEditComponent>;
  let component: CaseLocationEditComponent;

  const mockLocationService = {
    coordinateToAddress: jest.fn(),
    addressSuggest: jest.fn(),
    addressLookup: jest.fn(),
  };

  const mockZakenService = {
    updateZaakLocatie: jest.fn(),
  };

  const mockSideNav = fromPartial<MatDrawer>({
    close: jest.fn(),
  });

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [CaseLocationEditComponent, TranslateModule.forRoot()],
      providers: [
        { provide: LocationService, useValue: mockLocationService },
        { provide: ZakenService, useValue: mockZakenService },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(CaseLocationEditComponent);
    component = fixture.componentInstance;

    component.zaak = fromPartial<GeneratedType<"RestZaak">>({
      uuid: "zaak-123",
      rechten: { wijzigenLocatie: true },
    });
    component.sideNav = mockSideNav;

    fixture.detectChanges();
  });

  it("renders the map container", () => {
    expect(
      fixture.nativeElement.querySelector(".open-layers-map"),
    ).not.toBeNull();
  });

  it("enables the reason control on init when zaak has no geometry", () => {
    expect(component["reasonControl"].enabled).toBe(true);
  });
});
