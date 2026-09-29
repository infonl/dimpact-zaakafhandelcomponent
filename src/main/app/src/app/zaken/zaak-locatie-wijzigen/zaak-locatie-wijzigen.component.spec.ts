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
import { TestBed } from "@angular/core/testing";
import { MatDrawer } from "@angular/material/sidenav";
import { NoopAnimationsModule } from "@angular/platform-browser/animations";
import { TranslateModule } from "@ngx-translate/core";
import { provideTanStackQuery } from "@tanstack/angular-query-experimental";
import { render, screen, within } from "@testing-library/angular";
import userEvent from "@testing-library/user-event";
import { EMPTY, of } from "rxjs";
import { fromPartial } from "src/test-helpers";
import { sleep, testQueryClient } from "../../../../setupJest";
import { FoutAfhandelingService } from "../../fout-afhandeling/fout-afhandeling.service";
import {
  AddressResult,
  LocationService,
  SuggestResult,
} from "../../shared/location/location.service";
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
const mockMapListeners: {
  type: string;
  listener: (event: { coordinate: number[] }) => void;
}[] = [];
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
    on: jest.fn(
      (type: string, listener: (event: { coordinate: number[] }) => void) =>
        mockMapListeners.push({ type, listener }),
    ),
  })),
  View: jest.fn(() => ({
    setZoom: jest.fn(),
    setCenter: jest.fn(),
  })),
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

const addressName = "fakeStraat 1, fakeWoonplaats";

const point = fromPartial<GeneratedType<"RestGeometry">>({
  type: "POINT",
  point: { latitude: 52, longitude: 5 },
});

describe(CaseLocationEditComponent.name, () => {
  let httpTestingController: HttpTestingController;
  let sideNav: MatDrawer;
  let locatieChanged: jest.Mock;
  let zakenService: ZakenService;

  const user = userEvent.setup();

  const locationService = {
    coordinateToAddress: jest.fn(() =>
      of({ response: { docs: [] as AddressResult[] } }),
    ),
    addressSuggest: jest.fn(() =>
      of({
        response: {
          docs: [
            fromPartial<SuggestResult>({
              id: "fakeAddressId",
              weergavenaam: addressName,
            }),
          ],
        },
      }),
    ),
    addressLookup: jest.fn(() =>
      of({
        response: {
          docs: [
            fromPartial<AddressResult>({
              id: "fakeAddressId",
              weergavenaam: addressName,
              centroide_ll: "POINT(5 52)",
            }),
          ],
        },
      }),
    ),
  };

  async function setup(zaak?: Partial<GeneratedType<"RestZaak">>) {
    mockMapListeners.length = 0;
    sideNav = fromPartial<MatDrawer>({ close: jest.fn() });
    locatieChanged = jest.fn();

    const rendered = await render(CaseLocationEditComponent, {
      inputs: {
        zaak: fromPartial<GeneratedType<"RestZaak">>({
          uuid: "zaak-123",
          rechten: { wijzigenLocatie: true },
          ...zaak,
        }),
        sideNav,
      },
      on: { locatie: locatieChanged },
      imports: [NoopAnimationsModule, TranslateModule.forRoot()],
      providers: [
        provideZonelessChangeDetection(),
        provideHttpClient(withInterceptorsFromDi()),
        provideHttpClientTesting(),
        provideTanStackQuery(testQueryClient),
        { provide: LocationService, useValue: locationService },
      ],
    });

    httpTestingController = TestBed.inject(HttpTestingController);
    jest
      .spyOn(TestBed.inject(FoutAfhandelingService), "foutAfhandelen")
      .mockReturnValue(EMPTY);
    zakenService = TestBed.inject(ZakenService);
    jest.spyOn(zakenService, "cacheZaak");

    await sleep();
    return rendered;
  }

  function submitButton() {
    return screen.getByRole("button", { name: "actie.opslaan" });
  }

  async function clickOnMap() {
    mockMapListeners
      .filter(({ type }) => type === "click")
      .forEach(({ listener }) => listener({ coordinate: [0, 0] }));
    await sleep();
  }

  async function pickAddressFromSearch() {
    await user.type(screen.getByLabelText("adres"), "fakeStraat");
    await user.click(
      await screen.findByRole("option", { name: new RegExp(addressName) }),
    );
  }

  async function fillInReason() {
    await user.type(
      screen.getByLabelText("reden"),
      "Verhuizing naar fakeAdres",
    );
  }

  it("shows the map", async () => {
    const { container } = await setup();

    expect(
      // eslint-disable-next-line no-restricted-syntax, testing-library/no-node-access -- OpenLayers renders into a plain div that carries no role, label or text
      container.querySelector(".open-layers-map"),
    ).not.toBeNull();
  });

  it("asks for a reason as soon as the zaak has no location yet", async () => {
    await setup();

    expect(screen.getByLabelText("reden")).toBeEnabled();
  });

  it("only asks for a reason once the location of the zaak changes", async () => {
    await setup({ zaakgeometrie: point });

    expect(screen.getByLabelText("reden")).toBeDisabled();

    await user.click(screen.getByRole("button", { name: "actie.ontkoppelen" }));

    expect(screen.getByLabelText("reden")).toBeEnabled();
  });

  it("saves the location picked from the address search together with the reason", async () => {
    await setup();

    await pickAddressFromSearch();
    await fillInReason();
    await user.click(submitButton());
    await sleep();

    const request = httpTestingController.expectOne(ZAAK_LOCATIE_URL);
    expect(request.request.method).toBe("PATCH");
    expect(request.request.body).toEqual({
      reden: "Verhuizing naar fakeAdres",
      geometrie: point,
    });
    request.flush(null);
  });

  it("announces the new location and closes the side nav after saving", async () => {
    await setup();
    const updatedZaak = fromPartial<GeneratedType<"RestZaak">>({
      uuid: "zaak-123",
      zaakgeometrie: fromPartial<GeneratedType<"RestGeometry">>({
        type: "POINT",
      }),
    });

    await pickAddressFromSearch();
    await fillInReason();
    await user.click(submitButton());
    await sleep();
    httpTestingController.expectOne(ZAAK_LOCATIE_URL).flush(updatedZaak);
    await sleep();

    expect(zakenService.cacheZaak).toHaveBeenCalledWith(updatedZaak);
    expect(locatieChanged).toHaveBeenCalled();
    expect(sideNav.close).toHaveBeenCalled();
  });

  it("blocks a second save while the first one is still running", async () => {
    await setup();

    await pickAddressFromSearch();
    await fillInReason();
    await user.click(submitButton());
    await sleep();

    expect(submitButton()).toBeDisabled();

    const requests = httpTestingController.match(ZAAK_LOCATIE_URL);
    expect(requests).toHaveLength(1);
    requests[0].flush(null);
  });

  it("blocks a second save while removing the location is still running", async () => {
    await setup({ zaakgeometrie: point });

    await user.click(screen.getByRole("button", { name: "actie.ontkoppelen" }));
    await fillInReason();
    await user.click(submitButton());
    await sleep();

    expect(submitButton()).toBeDisabled();

    const requests = httpTestingController.match(ZAAK_LOCATIE_URL);
    expect(requests).toHaveLength(1);
    expect(requests[0].request.body).toEqual({
      reden: "Verhuizing naar fakeAdres",
      geometrie: null,
    });
    requests[0].flush(null);
  });

  it("offers to link a location when the zaak has none yet", async () => {
    await setup();

    expect(
      screen.getByRole("heading", { name: /actie.zaak.locatie.koppelen/ }),
    ).toBeVisible();
  });

  it("offers to change the location when the zaak already has one", async () => {
    await setup({ zaakgeometrie: point });

    expect(
      screen.getByRole("heading", { name: /actie.zaak.locatie.wijzigen/ }),
    ).toBeVisible();
  });

  it("shows the coordinates and the nearest address of the current location of the zaak", async () => {
    locationService.coordinateToAddress.mockReturnValueOnce(
      of({
        response: {
          docs: [fromPartial<AddressResult>({ weergavenaam: addressName })],
        },
      }),
    );

    await setup({ zaakgeometrie: point });

    expect(locationService.coordinateToAddress).toHaveBeenCalledWith([5, 52]);
    expect(screen.getByText("52, 5")).toBeVisible();
    expect(screen.getByText(addressName)).toBeVisible();
  });

  it("moves the location to where the map is clicked", async () => {
    await setup({ zaakgeometrie: point });

    await clickOnMap();

    expect(screen.getByText("0, 0")).toBeVisible();
    expect(screen.getByLabelText("reden")).toBeEnabled();
  });

  it("only shows the location when the user may not change it", async () => {
    await setup({
      zaakgeometrie: point,
      rechten: fromPartial<GeneratedType<"RestZaakRechten">>({
        wijzigenLocatie: false,
      }),
    });

    expect(screen.getByText("52, 5")).toBeVisible();
    expect(screen.queryByLabelText("adres")).toBeNull();
    expect(screen.queryByLabelText("reden")).toBeNull();
    expect(screen.queryByRole("button", { name: "actie.opslaan" })).toBeNull();
  });

  it("does not move the location when the map is clicked and the user may not change it", async () => {
    await setup({
      zaakgeometrie: point,
      rechten: fromPartial<GeneratedType<"RestZaakRechten">>({
        wijzigenLocatie: false,
      }),
    });

    await clickOnMap();

    expect(screen.getByText("52, 5")).toBeVisible();
    expect(screen.queryByText("0, 0")).toBeNull();
  });

  it("closes the side nav when cancelled", async () => {
    await setup();

    await user.click(screen.getByRole("button", { name: "actie.annuleren" }));

    expect(sideNav.close).toHaveBeenCalled();
    httpTestingController.expectNone(ZAAK_LOCATIE_URL);
  });

  it("closes the side nav with the close button of the toolbar", async () => {
    await setup();

    await user.click(within(screen.getByRole("heading")).getByRole("button"));

    expect(sideNav.close).toHaveBeenCalled();
  });
});
