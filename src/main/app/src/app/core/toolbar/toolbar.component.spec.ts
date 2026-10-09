/*
 * SPDX-FileCopyrightText: 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 *
 */

import { HarnessLoader } from "@angular/cdk/testing";
import { TestbedHarnessEnvironment } from "@angular/cdk/testing/testbed";
import {
  provideHttpClient,
  withInterceptorsFromDi,
} from "@angular/common/http";
import {
  HttpTestingController,
  provideHttpClientTesting,
} from "@angular/common/http/testing";
import { Injector, runInInjectionContext } from "@angular/core";
import { ComponentFixture, TestBed } from "@angular/core/testing";
import { MatButtonHarness } from "@angular/material/button/testing";
import { MatFormFieldHarness } from "@angular/material/form-field/testing";
import { MatIconHarness } from "@angular/material/icon/testing";
import { MatSidenav } from "@angular/material/sidenav";
import { NoopAnimationsModule } from "@angular/platform-browser/animations";
import { provideRouter, Router } from "@angular/router";
import { TranslateModule } from "@ngx-translate/core";
import {
  injectMutation,
  provideTanStackQuery,
} from "@tanstack/angular-query-experimental";
import { screen } from "@testing-library/angular";
import userEvent from "@testing-library/user-event";
import { of } from "rxjs";
import { fromPartial } from "src/test-helpers";
import { mockMutationFn, sleep, testQueryClient } from "../../../../setupJest";
import { IdentityService } from "../../identity/identity.service";
import { PolicyService } from "../../policy/policy.service";
import { NavigationService } from "../../shared/navigation/navigation.service";
import { GeneratedType } from "../../shared/utils/generated-types";
import { ZakenService } from "../../zaken/zaken.service";
import { ZoekenService } from "../../zoeken/zoeken.service";
import { ObjectType } from "../websocket/model/object-type";
import { Opcode } from "../websocket/model/opcode";
import { WebsocketListener } from "../websocket/model/websocket-listener";
import { WebsocketService } from "../websocket/websocket.service";
import { ToolbarComponent } from "./toolbar.component";

describe(ToolbarComponent.name, () => {
  let fixture: ComponentFixture<ToolbarComponent>;
  let loader: HarnessLoader;
  let identityService: IdentityService;
  let policyService: PolicyService;
  let zakenService: ZakenService;
  let injector: Injector;
  let createZaakMutation: ReturnType<typeof injectMutation>;
  let zoekenSideNav: MatSidenav;

  const user = userEvent.setup();

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [
        ToolbarComponent,
        NoopAnimationsModule,
        TranslateModule.forRoot(),
      ],
      providers: [
        provideHttpClient(withInterceptorsFromDi()),
        provideHttpClientTesting(),
        provideRouter([]),
        provideTanStackQuery(testQueryClient),
      ],
    }).compileComponents();

    identityService = TestBed.inject(IdentityService);
    policyService = TestBed.inject(PolicyService);
    zakenService = TestBed.inject(ZakenService);
    injector = TestBed.inject(Injector);

    testQueryClient.setQueryData(
      identityService.readLoggedInUser().queryKey,
      fromPartial<GeneratedType<"RestUser">>({
        id: "user-id",
        naam: "Jan Jansen",
      }),
    );

    testQueryClient.setQueryData(policyService.readOverigeRechten().queryKey, {
      canStartenZaak: true,
      canBeheren: false,
      canZoeken: false,
    });
    jest.spyOn(policyService, "readWerklijstRechten").mockReturnValue(
      of(
        fromPartial<GeneratedType<"RestWerklijstRechten">>({
          canZakenTaken: false,
          canInbox: false,
        }),
      ),
    );

    createZaakMutation = runInInjectionContext(injector, () =>
      injectMutation(() => ({
        mutationKey: zakenService.createZaak().mutationKey,
        mutationFn: () => mockMutationFn(),
      })),
    );
  });

  function createComponent() {
    zoekenSideNav = fromPartial<MatSidenav>({ open: jest.fn() });
    fixture = TestBed.createComponent(ToolbarComponent);
    fixture.componentRef.setInput("zoekenSideNav", zoekenSideNav);
    loader = TestbedHarnessEnvironment.loader(fixture);
    fixture.detectChanges();
  }

  describe("Zaak create button", () => {
    it("is enabled when no mutation is in progress", async () => {
      createComponent();

      const button = await loader.getHarness(
        MatButtonHarness.with({ text: "create_new_folder" }),
      );
      expect(await button.isDisabled()).toBe(false);
    });

    it("is disabled when createZaak mutation is in progress", async () => {
      createComponent();
      createZaakMutation.mutate({});
      fixture.detectChanges();

      const button = await loader.getHarness(
        MatButtonHarness.with({ text: "create_new_folder" }),
      );
      expect(await button.isDisabled()).toBe(true);
    });

    it("is not rendered when overigeRechten.startenZaak is false", async () => {
      testQueryClient.setQueryData(
        policyService.readOverigeRechten().queryKey,
        { canStartenZaak: false, canBeheren: false, canZoeken: false },
      );
      createComponent();

      const buttons = await loader.getAllHarnesses(
        MatButtonHarness.with({ text: "create_new_folder" }),
      );
      expect(buttons).toHaveLength(0);
    });
  });

  describe("Navigation menus", () => {
    it("renders zaken and taken menu buttons when werklijstRechten.zakenTaken is true", async () => {
      jest.spyOn(policyService, "readWerklijstRechten").mockReturnValue(
        of(
          fromPartial<GeneratedType<"RestWerklijstRechten">>({
            canZakenTaken: true,
            canInbox: false,
          }),
        ),
      );
      createComponent();

      const zakenButton = await loader.getAllHarnesses(
        MatButtonHarness.with({ selector: '[aria-label="Menu voor zaken"]' }),
      );
      const takenButton = await loader.getAllHarnesses(
        MatButtonHarness.with({ selector: '[aria-label="Menu voor taken"]' }),
      );
      expect(zakenButton).toHaveLength(1);
      expect(takenButton).toHaveLength(1);
    });

    it("hides zaken and taken menu buttons when werklijstRechten.zakenTaken is false", async () => {
      createComponent();

      const zakenButton = await loader.getAllHarnesses(
        MatButtonHarness.with({ selector: '[aria-label="Menu voor zaken"]' }),
      );
      const takenButton = await loader.getAllHarnesses(
        MatButtonHarness.with({ selector: '[aria-label="Menu voor taken"]' }),
      );
      expect(zakenButton).toHaveLength(0);
      expect(takenButton).toHaveLength(0);
    });

    it("renders inbox menu button when werklijstRechten.inbox is true", async () => {
      jest.spyOn(policyService, "readWerklijstRechten").mockReturnValue(
        of(
          fromPartial<GeneratedType<"RestWerklijstRechten">>({
            canZakenTaken: false,
            canInbox: true,
          }),
        ),
      );
      createComponent();

      const inboxButtons = await loader.getAllHarnesses(
        MatButtonHarness.with({ selector: '[aria-label="Menu voor inboxen"]' }),
      );
      expect(inboxButtons).toHaveLength(1);
    });

    it("hides inbox menu button when werklijstRechten.inbox is false", async () => {
      createComponent();

      const inboxButtons = await loader.getAllHarnesses(
        MatButtonHarness.with({ selector: '[aria-label="Menu voor inboxen"]' }),
      );
      expect(inboxButtons).toHaveLength(0);
    });
  });

  describe("Search field", () => {
    it("is rendered when overigeRechten.zoeken is true", async () => {
      testQueryClient.setQueryData(
        policyService.readOverigeRechten().queryKey,
        { canStartenZaak: false, canBeheren: false, canZoeken: true },
      );
      createComponent();

      const formFields = await loader.getAllHarnesses(
        MatFormFieldHarness.with({ floatingLabelText: "actie.zoeken" }),
      );
      expect(formFields).toHaveLength(1);
    });

    it("is not rendered when overigeRechten.zoeken is false", async () => {
      createComponent();

      const formFields = await loader.getAllHarnesses(
        MatFormFieldHarness.with({ floatingLabelText: "actie.zoeken" }),
      );
      expect(formFields).toHaveLength(0);
    });

    it("shows the clear button when hasSearched is true", async () => {
      testQueryClient.setQueryData(
        policyService.readOverigeRechten().queryKey,
        { canStartenZaak: false, canBeheren: false, canZoeken: true },
      );
      createComponent();
      TestBed.inject(ZoekenService).hasSearched.set(true);
      fixture.detectChanges();

      const icons = await loader.getAllHarnesses(
        MatIconHarness.with({ ancestor: ".search-field" }),
      );
      const iconNames = await Promise.all(icons.map((icon) => icon.getName()));
      expect(iconNames).toContain("close");
      expect(iconNames).not.toContain("search");
    });

    it("shows the search icon when hasSearched is false", async () => {
      testQueryClient.setQueryData(
        policyService.readOverigeRechten().queryKey,
        { canStartenZaak: false, canBeheren: false, canZoeken: true },
      );
      createComponent();

      const icons = await loader.getAllHarnesses(
        MatIconHarness.with({ ancestor: ".search-field" }),
      );
      const iconNames = await Promise.all(icons.map((icon) => icon.getName()));
      expect(iconNames).toContain("search");
      expect(iconNames).not.toContain("close");
    });

    it("opens the search side nav when enter is pressed in the search field", async () => {
      testQueryClient.setQueryData(
        policyService.readOverigeRechten().queryKey,
        { canStartenZaak: false, canBeheren: false, canZoeken: true },
      );
      createComponent();

      await user.type(
        screen.getByRole("textbox", { name: "actie.zoeken" }),
        "fakeTrefwoord{Enter}",
      );

      expect(zoekenSideNav.open).toHaveBeenCalledTimes(1);
    });

    it("opens the search side nav from each search button", async () => {
      testQueryClient.setQueryData(
        policyService.readOverigeRechten().queryKey,
        { canStartenZaak: false, canBeheren: false, canZoeken: true },
      );
      createComponent();

      const searchButtons = screen.getAllByRole("button", {
        name: "actie.zoeken",
      });
      for (const searchButton of searchButtons) {
        await user.click(searchButton);
      }

      expect(searchButtons).toHaveLength(2);
      expect(zoekenSideNav.open).toHaveBeenCalledTimes(2);
    });
  });

  describe("Admin button", () => {
    it("is rendered when overigeRechten.beheren is true", async () => {
      testQueryClient.setQueryData(
        policyService.readOverigeRechten().queryKey,
        { canStartenZaak: false, canBeheren: true, canZoeken: false },
      );
      createComponent();

      const buttons = await loader.getAllHarnesses(
        MatButtonHarness.with({ text: "settings" }),
      );
      expect(buttons).toHaveLength(1);
    });

    it("is not rendered when overigeRechten.beheren is false", async () => {
      createComponent();

      const buttons = await loader.getAllHarnesses(
        MatButtonHarness.with({ text: "settings" }),
      );
      expect(buttons).toHaveLength(0);
    });
  });

  describe("Signaleringen badge", () => {
    const LATEST_SIGNALERING_URL = "/rest/signaleringen/latest";

    let httpTestingController: HttpTestingController;
    let signaleringenChanged: () => void;

    beforeEach(() => {
      httpTestingController = TestBed.inject(HttpTestingController);
      const websocketService = TestBed.inject(WebsocketService);
      jest.spyOn(websocketService, "removeListener").mockImplementation();
      jest
        .spyOn(websocketService, "addListener")
        .mockImplementation((_opcode, _objectType, _objectId, callback) => {
          signaleringenChanged = callback as () => void;
          return fromPartial<WebsocketListener>({});
        });
    });

    afterEach(() => sessionStorage.removeItem("dashboardOpened"));

    async function respondWithLatestSignalering(
      latestSignalering: string | null,
    ) {
      httpTestingController
        .expectOne(LATEST_SIGNALERING_URL)
        .flush(latestSignalering);
      await sleep();
      await sleep();
      fixture.detectChanges();
    }

    async function isBadgeHidden() {
      const dashboardButton = await loader.getHarness(
        MatButtonHarness.with({ selector: '[aria-label="Dashboard"]' }),
      );
      return (await dashboardButton.host()).hasClass("mat-badge-hidden");
    }

    it("is hidden while the latest signalering is still being read", async () => {
      createComponent();

      expect(await isBadgeHidden()).toBe(true);

      await respondWithLatestSignalering("2026-01-02T00:00:00Z");
    });

    it("is hidden when there is no signalering", async () => {
      createComponent();

      await respondWithLatestSignalering(null);

      expect(await isBadgeHidden()).toBe(true);
    });

    it("is visible when there is a signalering and the dashboard has not been opened", async () => {
      createComponent();

      await respondWithLatestSignalering("2026-01-02T00:00:00Z");

      expect(await isBadgeHidden()).toBe(false);
    });

    it("is visible when the latest signalering is newer than the last time the dashboard was opened", async () => {
      sessionStorage.setItem(
        "dashboardOpened",
        JSON.stringify("2026-01-01T00:00:00Z"),
      );
      createComponent();

      await respondWithLatestSignalering("2026-01-02T00:00:00Z");

      expect(await isBadgeHidden()).toBe(false);
    });

    it("is hidden when the dashboard was opened after the latest signalering", async () => {
      sessionStorage.setItem(
        "dashboardOpened",
        JSON.stringify("2026-01-03T00:00:00Z"),
      );
      createComponent();

      await respondWithLatestSignalering("2026-01-02T00:00:00Z");

      expect(await isBadgeHidden()).toBe(true);
    });

    it("listens for changes to the signaleringen of the logged-in user", async () => {
      createComponent();
      await respondWithLatestSignalering(null);

      expect(TestBed.inject(WebsocketService).addListener).toHaveBeenCalledWith(
        Opcode.UPDATED,
        ObjectType.SIGNALERINGEN,
        "user-id",
        expect.any(Function),
      );
    });

    it("appears when a new signalering arrives", async () => {
      sessionStorage.setItem(
        "dashboardOpened",
        JSON.stringify("2026-01-03T00:00:00Z"),
      );
      createComponent();
      await respondWithLatestSignalering("2026-01-02T00:00:00Z");

      signaleringenChanged();
      await respondWithLatestSignalering("2026-01-04T00:00:00Z");

      expect(await isBadgeHidden()).toBe(false);
    });

    it("disappears when the dashboard is opened, even though the latest signalering stays the same", async () => {
      createComponent();
      await respondWithLatestSignalering("2026-01-02T00:00:00Z");

      sessionStorage.setItem(
        "dashboardOpened",
        JSON.stringify("2026-01-03T00:00:00Z"),
      );
      void testQueryClient.invalidateQueries({
        queryKey: [LATEST_SIGNALERING_URL],
      });
      await respondWithLatestSignalering("2026-01-02T00:00:00Z");

      expect(await isBadgeHidden()).toBe(true);
    });
  });

  describe("User initials", () => {
    it("computes initials from each word's first letter in the user name", () => {
      createComponent();

      expect(fixture.componentInstance["medewerkerNaamToolbar"]()).toBe("JJ");
    });
  });

  describe("Back button", () => {
    it("is disabled when navigation back is unavailable", async () => {
      const navigationService = TestBed.inject(NavigationService);
      navigationService.backDisabled$ = of(true);
      createComponent();
      await fixture.whenStable();
      fixture.detectChanges();

      const backButton = await loader.getHarness(
        MatButtonHarness.with({ selector: '[aria-label="Terug"]' }),
      );
      expect(await backButton.isDisabled()).toBe(true);
    });

    it("is enabled when navigation back is available", async () => {
      const navigationService = TestBed.inject(NavigationService);
      navigationService.backDisabled$ = of(false);
      createComponent();
      await fixture.whenStable();
      fixture.detectChanges();

      const backButton = await loader.getHarness(
        MatButtonHarness.with({ selector: '[aria-label="Terug"]' }),
      );
      expect(await backButton.isDisabled()).toBe(false);
    });
  });

  describe("Route active detection", () => {
    it("isCaseRouteActive returns true when on a case detail page", () => {
      createComponent();
      jest
        .spyOn(TestBed.inject(Router), "url", "get")
        .mockReturnValue("/zaken/ZAAK-2024-001");

      expect(fixture.componentInstance["isCaseRouteActive"]()).toBe(true);
    });

    it("isCaseRouteActive returns false for the create-zaak route", () => {
      createComponent();
      jest
        .spyOn(TestBed.inject(Router), "url", "get")
        .mockReturnValue("/zaken/create");

      expect(fixture.componentInstance["isCaseRouteActive"]()).toBe(false);
    });

    it("isTaskRouteActive returns true when on a task detail page", () => {
      createComponent();
      jest
        .spyOn(TestBed.inject(Router), "url", "get")
        .mockReturnValue("/taken/task-id-123");

      expect(fixture.componentInstance["isTaskRouteActive"]()).toBe(true);
    });

    it("isTaskRouteActive returns false for non-task routes", () => {
      createComponent();
      jest
        .spyOn(TestBed.inject(Router), "url", "get")
        .mockReturnValue("/zaken/werkvoorraad");

      expect(fixture.componentInstance["isTaskRouteActive"]()).toBe(false);
    });
  });
});
