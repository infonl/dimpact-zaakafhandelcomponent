// SPDX-FileCopyrightText: 2026 INFO.nl
// SPDX-License-Identifier: EUPL-1.2+

import {
  provideHttpClient,
  withInterceptorsFromDi,
} from "@angular/common/http";
import {
  HttpTestingController,
  provideHttpClientTesting,
} from "@angular/common/http/testing";
import { provideZonelessChangeDetection } from "@angular/core";
import { ComponentFixture, TestBed } from "@angular/core/testing";
import { MatDrawer } from "@angular/material/sidenav";
import { NoopAnimationsModule } from "@angular/platform-browser/animations";
import { TranslateModule } from "@ngx-translate/core";
import { provideTanStackQuery } from "@tanstack/angular-query-experimental";
import { EMPTY, of } from "rxjs";
import { fromPartial } from "src/test-helpers";
import { sleep, testQueryClient } from "../../../../setupJest";
import { FoutAfhandelingService } from "../../fout-afhandeling/fout-afhandeling.service";
import { LocationService } from "../../shared/location/location.service";
import { GeneratedType } from "../../shared/utils/generated-types";
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

const ZAAK_LOCATIE_URL = "/rest/zaken/zaak-123/zaaklocatie";

const mockLocationService = {
  coordinateToAddress: jest.fn(() => of({ response: { docs: [] } })),
  addressSuggest: jest.fn(() => of({ response: { docs: [] } })),
  addressLookup: jest.fn(() => of({ response: { docs: [] } })),
};

const mockSideNav = fromPartial<MatDrawer>({
  close: jest.fn(),
});

const setup = (zaak?: Partial<GeneratedType<"RestZaak">>) => {
  TestBed.configureTestingModule({
    imports: [
      CaseLocationEditComponent,
      NoopAnimationsModule,
      TranslateModule.forRoot(),
    ],
    providers: [
      provideZonelessChangeDetection(),
      provideHttpClient(withInterceptorsFromDi()),
      provideHttpClientTesting(),
      provideTanStackQuery(testQueryClient),
      { provide: LocationService, useValue: mockLocationService },
    ],
  });

  const fixture: ComponentFixture<CaseLocationEditComponent> =
    TestBed.createComponent(CaseLocationEditComponent);
  const component = fixture.componentInstance;

  component.zaak = fromPartial<GeneratedType<"RestZaak">>({
    uuid: "zaak-123",
    rechten: { wijzigenLocatie: true },
    ...zaak,
  });
  component.sideNav = mockSideNav;

  fixture.detectChanges();

  const foutAfhandelingService = TestBed.inject(FoutAfhandelingService);
  jest.spyOn(foutAfhandelingService, "foutAfhandelen").mockReturnValue(EMPTY);

  return {
    fixture,
    component,
    httpTestingController: TestBed.inject(HttpTestingController),
    foutAfhandelingService,
  };
};

/**
 * Puts the form in a submittable state: a location change plus a reason, both of
 * which the reason control needs to be enabled, valid and dirty.
 */
const makeSubmittable = (
  component: CaseLocationEditComponent,
  geometrie: GeneratedType<"RestGeometry"> | null,
) => {
  component["markerLocatie$"].next(geometrie);
  component["reasonControl"].enable();
  component["reasonControl"].setValue("Verhuizing naar nieuw adres");
  component["reasonControl"].markAsDirty();
};

const point = fromPartial<GeneratedType<"RestGeometry">>({
  type: "Point",
  point: { latitude: 52, longitude: 5 },
});

describe(CaseLocationEditComponent.name, () => {
  afterEach(() => {
    testQueryClient.clear();
    jest.clearAllMocks();
  });

  it("renders the map container", () => {
    const { fixture } = setup();

    expect(
      fixture.nativeElement.querySelector(".open-layers-map"),
    ).not.toBeNull();
  });

  it("enables the reason control on init when zaak has no geometry", () => {
    const { component } = setup();

    expect(component["reasonControl"].enabled).toBe(true);
  });

  it("sends the reason and geometry in the patch body on save", async () => {
    const { component, httpTestingController } = setup();
    makeSubmittable(component, point);

    component.save();
    await sleep();

    const request = httpTestingController.expectOne(ZAAK_LOCATIE_URL);
    expect(request.request.method).toBe("PATCH");
    expect(request.request.body).toEqual({
      reden: "Verhuizing naar nieuw adres",
      geometrie: point,
    });
    request.flush(null);
  });

  it("emits and closes the side nav on a successful save", async () => {
    const { component, fixture, httpTestingController } = setup();
    jest.spyOn(component.locatie, "emit");
    makeSubmittable(component, point);

    component.save();
    await sleep();
    httpTestingController.expectOne(ZAAK_LOCATIE_URL).flush(null);
    await sleep();
    fixture.detectChanges();

    expect(component.locatie.emit).toHaveBeenCalled();
    expect(mockSideNav.close).toHaveBeenCalled();
  });

  it("sends only one request when the save button is clicked twice", async () => {
    const { component, fixture, httpTestingController } = setup();
    makeSubmittable(component, point);
    fixture.detectChanges();

    const submitButton: HTMLButtonElement = fixture.nativeElement.querySelector(
      'button[type="submit"]',
    );
    expect(submitButton.disabled).toBe(false);

    submitButton.click();
    await sleep();
    fixture.detectChanges();

    // While the first request is still in flight the button is disabled, so this
    // second click must not trigger another request.
    submitButton.click();
    await sleep();

    const requests = httpTestingController.match(ZAAK_LOCATIE_URL);
    expect(requests).toHaveLength(1);
    requests[0].flush(null);
  });

  it("sends only one request when deleting a location is submitted twice", async () => {
    // Deleting a location is the same save path with an empty geometry.
    const { component, fixture, httpTestingController } = setup({
      zaakgeometrie: point,
    });
    makeSubmittable(component, null);
    fixture.detectChanges();

    const submitButton: HTMLButtonElement = fixture.nativeElement.querySelector(
      'button[type="submit"]',
    );
    expect(submitButton.disabled).toBe(false);

    submitButton.click();
    await sleep();
    fixture.detectChanges();

    submitButton.click();
    await sleep();

    const requests = httpTestingController.match(ZAAK_LOCATIE_URL);
    expect(requests).toHaveLength(1);
    expect(requests[0].request.body).toEqual({
      reden: "Verhuizing naar nieuw adres",
      geometrie: null,
    });
    requests[0].flush(null);
  });
});
