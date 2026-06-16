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
import { provideHttpClientTesting } from "@angular/common/http/testing";
import { Injector, runInInjectionContext } from "@angular/core";
import { ComponentFixture, TestBed } from "@angular/core/testing";
import { MatButtonHarness } from "@angular/material/button/testing";
import { MatFormFieldHarness } from "@angular/material/form-field/testing";
import { MatIconHarness } from "@angular/material/icon/testing";
import { NoopAnimationsModule } from "@angular/platform-browser/animations";
import { provideRouter, Router } from "@angular/router";
import { TranslateModule } from "@ngx-translate/core";
import {
  injectMutation,
  provideTanStackQuery,
} from "@tanstack/angular-query-experimental";
import { of } from "rxjs";
import { fromPartial } from "src/test-helpers";
import { mockMutationFn, testQueryClient } from "../../../../setupJest";
import { IdentityService } from "../../identity/identity.service";
import { PolicyService } from "../../policy/policy.service";
import { NavigationService } from "../../shared/navigation/navigation.service";
import { GeneratedType } from "../../shared/utils/generated-types";
import { ZakenService } from "../../zaken/zaken.service";
import { ZoekenService } from "../../zoeken/zoeken.service";
import { ToolbarComponent } from "./toolbar.component";

describe(ToolbarComponent.name, () => {
  let fixture: ComponentFixture<ToolbarComponent>;
  let loader: HarnessLoader;
  let identityService: IdentityService;
  let policyService: PolicyService;
  let zakenService: ZakenService;
  let injector: Injector;
  let createZaakMutation: ReturnType<typeof injectMutation>;

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
      startenZaak: true,
      beheren: false,
      zoeken: false,
      brpZoeken: false,
    });
    jest.spyOn(policyService, "readWerklijstRechten").mockReturnValue(
      of(
        fromPartial<GeneratedType<"RestWerklijstRechten">>({
          zakenTaken: false,
          inbox: false,
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
    fixture = TestBed.createComponent(ToolbarComponent);
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
        { startenZaak: false, beheren: false, zoeken: false, brpZoeken: false },
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
            zakenTaken: true,
            inbox: false,
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
            zakenTaken: false,
            inbox: true,
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
        { startenZaak: false, beheren: false, zoeken: true, brpZoeken: false },
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
        { startenZaak: false, beheren: false, zoeken: true, brpZoeken: false },
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
        { startenZaak: false, beheren: false, zoeken: true, brpZoeken: false },
      );
      createComponent();

      const icons = await loader.getAllHarnesses(
        MatIconHarness.with({ ancestor: ".search-field" }),
      );
      const iconNames = await Promise.all(icons.map((icon) => icon.getName()));
      expect(iconNames).toContain("search");
      expect(iconNames).not.toContain("close");
    });
  });

  describe("Admin button", () => {
    it("is rendered when overigeRechten.beheren is true", async () => {
      testQueryClient.setQueryData(
        policyService.readOverigeRechten().queryKey,
        { startenZaak: false, beheren: true, zoeken: false, brpZoeken: false },
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
    it("is hidden when hasNewSignaleringen is false", async () => {
      createComponent();

      const dashboardButton = await loader.getHarness(
        MatButtonHarness.with({ selector: '[aria-label="Dashboard"]' }),
      );
      const host = await dashboardButton.host();
      expect(await host.hasClass("mat-badge-hidden")).toBe(true);
    });

    it("is visible when hasNewSignaleringen is true", async () => {
      createComponent();
      fixture.componentInstance["hasNewSignaleringen"] = true;
      fixture.detectChanges();

      const dashboardButton = await loader.getHarness(
        MatButtonHarness.with({ selector: '[aria-label="Dashboard"]' }),
      );
      const host = await dashboardButton.host();
      expect(await host.hasClass("mat-badge-hidden")).toBe(false);
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
