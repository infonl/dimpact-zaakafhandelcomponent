/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { provideHttpClient } from "@angular/common/http";
import { provideHttpClientTesting } from "@angular/common/http/testing";
import { ComponentFixture, TestBed } from "@angular/core/testing";
import { NoopAnimationsModule } from "@angular/platform-browser/animations";
import { provideRouter } from "@angular/router";
import { TranslateModule } from "@ngx-translate/core";
import {
  provideQueryClient,
  queryOptions,
} from "@tanstack/angular-query-experimental";
import { notifyManager } from "@tanstack/query-core";
import { within } from "@testing-library/angular";
import { fromPartial } from "src/test-helpers";
import { testQueryClient } from "../../../../../setupJest";
import { KlantenService } from "../../../klanten/klanten.service";
import { GeneratedType } from "../../../shared/utils/generated-types";
import { ZaakInitiatorPanelComponent } from "./zaak-initiator-panel.component";

describe(ZaakInitiatorPanelComponent.name, () => {
  let fixture: ComponentFixture<ZaakInitiatorPanelComponent>;

  const screen = () => within(fixture.nativeElement as HTMLElement);

  const koppelingen = fromPartial<GeneratedType<"RestBetrokkeneKoppelingen">>({
    brpKoppelen: true,
    kvkKoppelen: true,
  });

  const zaakWith = (
    overrides: Partial<GeneratedType<"RestZaak">> = {},
    betrokkeneKoppelingen = koppelingen,
  ) =>
    fromPartial<GeneratedType<"RestZaak">>({
      uuid: "1234",
      indicaties: [],
      rechten: { behandelen: true },
      groep: {},
      vertrouwelijkheidaanduiding: "OPENBAAR",
      gerelateerdeZaken: [],
      zaaktype: fromPartial<GeneratedType<"RestZaaktype">>({
        omschrijving: "fakeZaaktypeOmschrijving",
        zaakafhandelparameters: fromPartial<
          GeneratedType<"RestZaaktypeConfiguration">
        >({ betrokkeneKoppelingen }),
      }),
      ...overrides,
    });

  const renderPanel = (
    zaak: GeneratedType<"RestZaak">,
    hasBrpSearchRight = true,
  ) => {
    fixture.componentRef.setInput("zaak", zaak);
    fixture.componentRef.setInput("hasBrpSearchRight", hasBrpSearchRight);
    fixture.detectChanges();
  };

  beforeEach(() => {
    notifyManager.setScheduler((fn) => fn());
  });

  afterEach(() => {
    notifyManager.setScheduler(queueMicrotask);
  });

  beforeEach(async () => {
    jest.spyOn(KlantenService.prototype, "readPersoon").mockReturnValue(
      queryOptions({
        queryKey: ["fakePersoon"],
        queryFn: async () =>
          fromPartial<GeneratedType<"RestPersoon">>({
            naam: "fakePersoonNaam",
            indicaties: [],
          }),
      }) as ReturnType<KlantenService["readPersoon"]>,
    );
    jest.spyOn(KlantenService.prototype, "readBedrijf").mockReturnValue(
      queryOptions({
        queryKey: ["fakeBedrijf"],
        queryFn: async () =>
          fromPartial<GeneratedType<"RestBedrijf">>({
            naam: "fakeBedrijfNaam",
            identificatieType: "VN",
            vestigingsnummer: "fakeVestigingsnummer",
            kvkNummer: "fakeKvkNummer",
          }),
      }) as ReturnType<KlantenService["readBedrijf"]>,
    );

    await TestBed.configureTestingModule({
      imports: [
        ZaakInitiatorPanelComponent,
        NoopAnimationsModule,
        TranslateModule.forRoot(),
      ],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        provideRouter([]),
        provideQueryClient(testQueryClient),
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(ZaakInitiatorPanelComponent);
  });

  describe("which initiator view is shown", () => {
    it("offers to add an initiator when no type matches and there are no contact details", () => {
      renderPanel(
        zaakWith({
          initiatorIdentificatie: null,
          zaakSpecificContactDetails: null,
        }),
      );

      expect(
        screen().getByRole("button", { name: /msg.zaak.geen.initiator/ }),
      ).toBeInTheDocument();
    });

    it("shows the persoonsgegevens when the initiator is identified by BSN", async () => {
      renderPanel(
        zaakWith({
          initiatorIdentificatie: fromPartial({
            type: "BSN",
            temporaryPersonId: "fakeTemporaryPersonId",
          }),
          zaakSpecificContactDetails: null,
        }),
      );

      await fixture.whenStable();
      fixture.detectChanges();
      await new Promise((resolve) => setTimeout(resolve, 0));
      fixture.detectChanges();

      expect(
        screen().getByRole("button", { name: /fakePersoonNaam/ }),
      ).toBeInTheDocument();
    });

    it("shows the bedrijfsgegevens when the initiator is identified by vestigingsnummer", async () => {
      renderPanel(
        zaakWith({
          initiatorIdentificatie: fromPartial({
            type: "VN",
            vestigingsnummer: "fakeVestigingsnummer",
            kvkNummer: "fakeKvkNummer",
          }),
          zaakSpecificContactDetails: null,
        }),
      );

      await fixture.whenStable();
      fixture.detectChanges();
      await new Promise((resolve) => setTimeout(resolve, 0));
      fixture.detectChanges();

      expect(
        screen().getByRole("button", { name: /fakeBedrijfNaam/ }),
      ).toBeInTheDocument();
    });

    it("shows the zaakspecifieke contactgegevens when the zaak has them", () => {
      renderPanel(
        zaakWith({
          initiatorIdentificatie: null,
          zaakSpecificContactDetails: fromPartial<
            GeneratedType<"ContactDetails">
          >({
            telephoneNumber: "0612345678",
            emailAddress: "fake@example.com",
          }),
        }),
      );

      expect(
        screen().getByRole("button", {
          name: /initiator.aanvraagspecifieke-contactgegevens/,
        }),
      ).toBeInTheDocument();
    });

    it("falls back to adding an initiator when the contactgegevens are all empty", () => {
      renderPanel(
        zaakWith({
          initiatorIdentificatie: null,
          zaakSpecificContactDetails: fromPartial<
            GeneratedType<"ContactDetails">
          >({ telephoneNumber: null, emailAddress: null }),
        }),
      );

      expect(
        screen().queryByRole("button", {
          name: /initiator.aanvraagspecifieke-contactgegevens/,
        }),
      ).toBeNull();
      expect(
        screen().getByRole("button", { name: /msg.zaak.geen.initiator/ }),
      ).toBeInTheDocument();
    });

    it("renders nothing when no koppelingen are configured and the contactgegevens are all empty", () => {
      renderPanel(
        zaakWith(
          {
            initiatorIdentificatie: null,
            zaakSpecificContactDetails: fromPartial<
              GeneratedType<"ContactDetails">
            >({ telephoneNumber: null, emailAddress: null }),
          },
          fromPartial<GeneratedType<"RestBetrokkeneKoppelingen">>({
            brpKoppelen: false,
            kvkKoppelen: false,
          }),
        ),
      );

      expect(
        screen().queryByRole("button", {
          name: /initiator.aanvraagspecifieke-contactgegevens/,
        }),
      ).toBeNull();
      expect(
        screen().queryByRole("button", { name: /msg.zaak.geen.initiator/ }),
      ).toBeNull();
    });
  });

  describe("what the panel reports to its parent", () => {
    const zaakWithoutInitiator = () =>
      zaakWith({
        initiatorIdentificatie: null,
        zaakSpecificContactDetails: null,
        rechten: fromPartial<GeneratedType<"RestZaakRechten">>({
          toevoegenInitiatorPersoon: true,
          toevoegenInitiatorBedrijf: true,
        }),
      });

    const koppelButton = () =>
      screen().queryByRole("button", { name: "actie.initiator.koppelen" });

    it("asks the parent to add an initiator when the koppel button is pressed", () => {
      const addOrEdit = jest.fn();
      renderPanel(zaakWithoutInitiator());
      fixture.componentInstance.addOrEdit.subscribe(addOrEdit);

      koppelButton()!.click();

      expect(addOrEdit).toHaveBeenCalled();
    });

    it("offers no koppel button without the right to search the BRP", () => {
      renderPanel(
        zaakWith({
          initiatorIdentificatie: null,
          zaakSpecificContactDetails: null,
          rechten: fromPartial<GeneratedType<"RestZaakRechten">>({
            toevoegenInitiatorPersoon: true,
            toevoegenInitiatorBedrijf: false,
          }),
        }),
        false,
      );

      expect(koppelButton()).toBeNull();
    });
  });
});
