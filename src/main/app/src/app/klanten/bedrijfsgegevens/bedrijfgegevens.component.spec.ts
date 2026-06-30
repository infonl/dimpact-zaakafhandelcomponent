/*
 * SPDX-FileCopyrightText: 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { HarnessLoader } from "@angular/cdk/testing";
import { TestbedHarnessEnvironment } from "@angular/cdk/testing/testbed";
import { provideHttpClient } from "@angular/common/http";
import {
  HttpTestingController,
  provideHttpClientTesting,
} from "@angular/common/http/testing";
import { ComponentRef } from "@angular/core";
import { ComponentFixture, TestBed } from "@angular/core/testing";
import { MatButtonHarness } from "@angular/material/button/testing";
import { MatExpansionPanelHarness } from "@angular/material/expansion/testing";
import { MatIconHarness } from "@angular/material/icon/testing";
import { MatProgressSpinnerHarness } from "@angular/material/progress-spinner/testing";
import { NoopAnimationsModule } from "@angular/platform-browser/animations";
import { provideRouter } from "@angular/router";
import { TranslateModule } from "@ngx-translate/core";
import {
  provideTanStackQuery,
  QueryClient,
} from "@tanstack/angular-query-experimental";
import { notifyManager } from "@tanstack/query-core";
import { of } from "rxjs";
import { fromPartial } from "src/test-helpers";
import { sleep } from "../../../../setupJest";
import { MaterialModule } from "../../shared/material/material.module";
import { PipesModule } from "../../shared/pipes/pipes.module";
import { GeneratedType } from "../../shared/utils/generated-types";
import { BetrokkeneIdentificatie } from "../../zaken/model/betrokkeneIdentificatie";
import { KlantenService } from "../klanten.service";
import { BedrijfsgegevensComponent } from "./bedrijfsgegevens.component";

function makeBedrijfsprofiel(
  overrides: Partial<GeneratedType<"RestBedrijfsprofiel">> = {},
): GeneratedType<"RestBedrijfsprofiel"> {
  return fromPartial<GeneratedType<"RestBedrijfsprofiel">>({
    totaalWerkzamePersonen: 10,
    sbiHoofdActiviteit: "Software ontwikkeling",
    sbiActiviteiten: ["Consultancy"],
    website: "https://testbedrijf.nl",
    adressen: [
      { type: "bezoekadres", volledigAdres: "Teststraat 1, 1234AB Amsterdam" },
    ],
    ...overrides,
  });
}

describe(BedrijfsgegevensComponent.name, () => {
  let fixture: ComponentFixture<BedrijfsgegevensComponent>;
  let component: BedrijfsgegevensComponent;
  let componentRef: ComponentRef<BedrijfsgegevensComponent>;
  let loader: HarnessLoader;
  let httpController: HttpTestingController;

  let klantenService: KlantenService;
  const queryClient = new QueryClient({
    defaultOptions: { queries: { retry: false } },
  });

  const betrokkeneIdentificatie = fromPartial<BetrokkeneIdentificatie>({
    type: "VN",
    kvkNummer: "12345678",
    vestigingsnummer: "12345678",
  });

  const vestigingUrl = `/rest/klanten/vestiging/${betrokkeneIdentificatie.vestigingsnummer}/${betrokkeneIdentificatie.kvkNummer}`;

  const testZaak = fromPartial<GeneratedType<"RestZaak">>({
    initiatorIdentificatie: betrokkeneIdentificatie,
    rechten: {
      toevoegenInitiatorBedrijf: false,
      verwijderenInitiator: false,
    },
  });

  const testBedrijf = fromPartial<GeneratedType<"RestBedrijf">>({
    naam: "Test BV",
    vestigingsnummer: betrokkeneIdentificatie.vestigingsnummer,
    identificatieType: betrokkeneIdentificatie.type,
  });

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [
        BedrijfsgegevensComponent,
        TranslateModule.forRoot(),
        NoopAnimationsModule,
        MaterialModule,
        PipesModule,
      ],
      providers: [
        KlantenService,
        provideHttpClient(),
        provideHttpClientTesting(),
        provideRouter([]),
        provideTanStackQuery(queryClient),
      ],
    });

    klantenService = TestBed.inject(KlantenService);
    jest.spyOn(klantenService, "readBedrijf").mockReturnValue({
      ...klantenService.readBedrijf(betrokkeneIdentificatie),
      retry: false,
    });

    fixture = TestBed.createComponent(BedrijfsgegevensComponent);
    component = fixture.componentInstance;

    componentRef = fixture.componentRef;
    componentRef.setInput("zaak", testZaak);

    loader = TestbedHarnessEnvironment.loader(fixture);
    httpController = TestBed.inject(HttpTestingController);

    fixture.detectChanges();
  });

  afterEach(() => {
    queryClient.clear();
  });

  describe("while loading", () => {
    it("should show a spinner", async () => {
      const spinner = await loader.getHarnessOrNull(MatProgressSpinnerHarness);
      expect(spinner).toBeTruthy();
    });
  });

  describe("rechten", () => {
    it("should show the edit button when toevoegenInitiatorBedrijf is true", async () => {
      componentRef.setInput(
        "zaak",
        fromPartial<GeneratedType<"RestZaak">>({
          ...testZaak,
          rechten: {
            toevoegenInitiatorBedrijf: true,
            verwijderenInitiator: false,
          },
        }),
      );
      fixture.detectChanges();

      const button = await loader.getHarnessOrNull(
        MatButtonHarness.with({
          selector: '[title="actie.initiator.wijzigen"]',
        }),
      );
      expect(button).toBeTruthy();
    });

    it("should not show the edit button when toevoegenInitiatorBedrijf is false", async () => {
      const button = await loader.getHarnessOrNull(
        MatButtonHarness.with({
          selector: '[title="actie.initiator.wijzigen"]',
        }),
      );
      expect(button).toBeNull();
    });

    it("should show the delete button when verwijderenInitiator is true", async () => {
      componentRef.setInput(
        "zaak",
        fromPartial<GeneratedType<"RestZaak">>({
          ...testZaak,
          rechten: {
            toevoegenInitiatorBedrijf: false,
            verwijderenInitiator: true,
          },
        }),
      );
      fixture.detectChanges();

      const button = await loader.getHarnessOrNull(
        MatButtonHarness.with({ selector: '[title="actie.ontkoppelen"]' }),
      );
      expect(button).toBeTruthy();
    });

    it("should not show the delete button when verwijderenInitiator is false", async () => {
      const button = await loader.getHarnessOrNull(
        MatButtonHarness.with({ selector: '[title="actie.ontkoppelen"]' }),
      );
      expect(button).toBeNull();
    });
  });

  describe("on successful load", () => {
    beforeEach(async () => {
      notifyManager.setScheduler((fn) => fn());
      const request = httpController.expectOne(vestigingUrl);
      request.flush(testBedrijf);
      await sleep();
      fixture.detectChanges();
    });

    afterEach(() => {
      notifyManager.setScheduler(queueMicrotask);
    });

    it("should expand the panel", async () => {
      const panel = await loader.getHarness(MatExpansionPanelHarness);
      expect(await panel.isExpanded()).toBe(true);
    });

    it("should show the visit link", async () => {
      const link = await loader.getHarnessOrNull(
        MatButtonHarness.with({
          selector: 'a[title="actie.bedrijf.bekijken"]',
        }),
      );
      expect(link).toBeTruthy();
    });
  });

  describe("type display", () => {
    beforeEach(async () => {
      notifyManager.setScheduler((fn) => fn());
      const request = httpController.expectOne(vestigingUrl);
      request.flush({ ...testBedrijf, type: "fakeType1" });
      await sleep();
      fixture.detectChanges();
    });

    afterEach(() => {
      notifyManager.setScheduler(queueMicrotask);
    });

    it("renders the bedrijf type from bedrijfQuery data, not the identificatieType", () => {
      const typeField: HTMLElement = fixture.nativeElement.querySelector(
        'zac-static-text[label="type"]',
      );
      expect(typeField?.textContent).toContain("fakeType1");
    });
  });

  describe("zaakSpecificContactDetails", () => {
    beforeEach(() => {
      notifyManager.setScheduler((fn) => fn());
    });

    afterEach(() => {
      notifyManager.setScheduler(queueMicrotask);
    });

    it("renders the zaak-specific telephoneNumber when set, overriding the bedrijf telefoonnummer", async () => {
      componentRef.setInput(
        "zaak",
        fromPartial<GeneratedType<"RestZaak">>({
          ...testZaak,
          zaakSpecificContactDetails: {
            telephoneNumber: "0612345678",
            emailAddress: null,
          },
        }),
      );

      const request = httpController.expectOne(vestigingUrl);
      request.flush({ ...testBedrijf, telefoonnummer: "0299123456" });
      await sleep();
      fixture.detectChanges();

      const telefoonField: HTMLElement = fixture.nativeElement.querySelector(
        'zac-static-text[label="telefoonnummer"]',
      );
      expect(telefoonField?.textContent).toContain("0612345678");
      expect(telefoonField?.textContent).not.toContain("0299123456");
    });

    it("renders the zaak-specific emailAddress when set, overriding the bedrijf emailadres", async () => {
      componentRef.setInput(
        "zaak",
        fromPartial<GeneratedType<"RestZaak">>({
          ...testZaak,
          zaakSpecificContactDetails: {
            telephoneNumber: null,
            emailAddress: "zaak@example.com",
          },
        }),
      );

      const request = httpController.expectOne(vestigingUrl);
      request.flush({ ...testBedrijf, emailadres: "bedrijf@example.com" });
      await sleep();
      fixture.detectChanges();

      const emailField: HTMLElement = fixture.nativeElement.querySelector(
        'zac-static-text[label="emailadres"]',
      );
      expect(emailField?.textContent).toContain("zaak@example.com");
      expect(emailField?.textContent).not.toContain("bedrijf@example.com");
    });

    it("falls back to the bedrijf telefoonnummer and emailadres when zaakSpecificContactDetails is absent", async () => {
      const request = httpController.expectOne(vestigingUrl);
      request.flush({
        ...testBedrijf,
        telefoonnummer: "0299123456",
        emailadres: "bedrijf@example.com",
      });
      await sleep();
      fixture.detectChanges();

      const telefoonField: HTMLElement = fixture.nativeElement.querySelector(
        'zac-static-text[label="telefoonnummer"]',
      );
      const emailField: HTMLElement = fixture.nativeElement.querySelector(
        'zac-static-text[label="emailadres"]',
      );
      expect(telefoonField?.textContent).toContain("0299123456");
      expect(emailField?.textContent).toContain("bedrijf@example.com");
    });

    it("falls back per-field to bedrijf data when only one zaak-specific contact value is set", async () => {
      componentRef.setInput(
        "zaak",
        fromPartial<GeneratedType<"RestZaak">>({
          ...testZaak,
          zaakSpecificContactDetails: {
            telephoneNumber: "0612345678",
            emailAddress: null,
          },
        }),
      );

      const request = httpController.expectOne(vestigingUrl);
      request.flush({
        ...testBedrijf,
        telefoonnummer: "0299123456",
        emailadres: "bedrijf@example.com",
      });
      await sleep();
      fixture.detectChanges();

      const telefoonField: HTMLElement = fixture.nativeElement.querySelector(
        'zac-static-text[label="telefoonnummer"]',
      );
      const emailField: HTMLElement = fixture.nativeElement.querySelector(
        'zac-static-text[label="emailadres"]',
      );
      expect(telefoonField?.textContent).toContain("0612345678");
      expect(emailField?.textContent).toContain("bedrijf@example.com");
    });

    it("shows the aanvraagspecifiek hint when telephoneNumber comes from contact details", async () => {
      componentRef.setInput(
        "zaak",
        fromPartial<GeneratedType<"RestZaak">>({
          ...testZaak,
          zaakSpecificContactDetails: {
            telephoneNumber: "0612345678",
            emailAddress: null,
          },
        }),
      );

      const request = httpController.expectOne(vestigingUrl);
      request.flush({ ...testBedrijf, telefoonnummer: "0299123456" });
      await sleep();
      fixture.detectChanges();

      const hints: HTMLElement[] = Array.from(
        fixture.nativeElement.querySelectorAll(".hint"),
      );
      expect(
        hints.some((h) =>
          h.textContent?.includes("initiator.aanvraagspecifiek-telefoonnummer"),
        ),
      ).toBe(true);
    });

    it("shows the aanvraagspecifiek hint when emailAddress comes from contact details", async () => {
      componentRef.setInput(
        "zaak",
        fromPartial<GeneratedType<"RestZaak">>({
          ...testZaak,
          zaakSpecificContactDetails: {
            telephoneNumber: null,
            emailAddress: "zaak@example.com",
          },
        }),
      );

      const request = httpController.expectOne(vestigingUrl);
      request.flush({ ...testBedrijf, emailadres: "bedrijf@example.com" });
      await sleep();
      fixture.detectChanges();

      const hints: HTMLElement[] = Array.from(
        fixture.nativeElement.querySelectorAll(".hint"),
      );
      expect(
        hints.some((h) =>
          h.textContent?.includes("initiator.aanvraagspecifiek-emailadres"),
        ),
      ).toBe(true);
    });

    it("does not show any aanvraagspecifiek hint when no zaak-specific contact details are set", async () => {
      const request = httpController.expectOne(vestigingUrl);
      request.flush({
        ...testBedrijf,
        telefoonnummer: "0299123456",
        emailadres: "bedrijf@example.com",
      });
      await sleep();
      fixture.detectChanges();

      expect(fixture.nativeElement.querySelector(".hint")).toBeNull();
    });
  });

  describe.each([
    {
      status: 404,
      iconName: "warning",
      statusText: "Not finding the vestiging",
    },
    {
      status: 500,
      iconName: "error",
      statusText: "Error fetching the vestiging",
    },
  ])(
    "Error handling fetching vestiging",
    ({ status, iconName, statusText }) => {
      beforeEach(() => {
        const request = httpController.expectOne(vestigingUrl);
        request.flush(null, { status, statusText });
      });

      it("should display the icon", async () => {
        await sleep();
        const icon = await loader.getHarnessOrNull(
          MatIconHarness.with({ name: iconName }),
        );
        expect(icon).toBeTruthy();
      });
    },
  );

  describe("ophalenProfiel()", () => {
    describe("for a vestiging", () => {
      beforeEach(async () => {
        notifyManager.setScheduler((fn) => fn());
        const request = httpController.expectOne(vestigingUrl);
        request.flush(
          fromPartial<GeneratedType<"RestBedrijf">>({
            ...testBedrijf,
            vestigingsnummer: "12345678",
            kvkNummer: "12345678",
          }),
        );
        await sleep();
        fixture.detectChanges();
      });

      afterEach(() => {
        notifyManager.setScheduler(queueMicrotask);
      });

      it("calls readVestigingsprofiel with the vestigingsnummer", () => {
        jest
          .spyOn(klantenService, "readVestigingsprofiel")
          .mockReturnValue(of(makeBedrijfsprofiel()));
        component["ophalenProfiel"]();
        expect(klantenService.readVestigingsprofiel).toHaveBeenCalledWith(
          "12345678",
        );
      });

      it("does not call readBasisprofiel", () => {
        jest
          .spyOn(klantenService, "readVestigingsprofiel")
          .mockReturnValue(of(makeBedrijfsprofiel()));
        const spy = jest.spyOn(klantenService, "readBasisprofiel");
        component["ophalenProfiel"]();
        expect(spy).not.toHaveBeenCalled();
      });

      it("populates profiel signal after service call", () => {
        const profiel = makeBedrijfsprofiel();
        jest
          .spyOn(klantenService, "readVestigingsprofiel")
          .mockReturnValue(of(profiel));
        component["ophalenProfiel"]();
        expect(component["profiel"]()).toEqual(profiel);
      });

      it("renders totaalWerkzamePersonen after profiel is loaded", () => {
        jest
          .spyOn(klantenService, "readVestigingsprofiel")
          .mockReturnValue(
            of(makeBedrijfsprofiel({ totaalWerkzamePersonen: 25 })),
          );
        component["ophalenProfiel"]();
        fixture.detectChanges();
        const element = fixture.debugElement
          .queryAll((de) => de.name === "zac-static-text")
          .find(
            (de) => de.componentInstance.label === "totaalWerkzamePersonen",
          );
        expect(element).toBeTruthy();
      });

      it("renders hoofdactiviteit after profiel is loaded", () => {
        jest
          .spyOn(klantenService, "readVestigingsprofiel")
          .mockReturnValue(
            of(makeBedrijfsprofiel({ sbiHoofdActiviteit: "Software" })),
          );
        component["ophalenProfiel"]();
        fixture.detectChanges();
        const element = fixture.debugElement
          .queryAll((de) => de.name === "zac-static-text")
          .find((de) => de.componentInstance.label === "hoofdactiviteit");
        expect(element).toBeTruthy();
      });

      it("renders website after profiel is loaded", () => {
        jest
          .spyOn(klantenService, "readVestigingsprofiel")
          .mockReturnValue(
            of(makeBedrijfsprofiel({ website: "https://test.nl" })),
          );
        component["ophalenProfiel"]();
        fixture.detectChanges();
        const element = fixture.debugElement
          .queryAll((de) => de.name === "zac-static-text")
          .find((de) => de.componentInstance.label === "website");
        expect(element).toBeTruthy();
      });
    });

    describe("for a rechtspersoon", () => {
      beforeEach(async () => {
        notifyManager.setScheduler((fn) => fn());
        const request = httpController.expectOne(vestigingUrl);
        request.flush(
          fromPartial<GeneratedType<"RestBedrijf">>({
            type: "RECHTSPERSOON",
            kvkNummer: "12345678",
            naam: "Test Rechtspersoon BV",
          }),
        );
        await sleep();
        fixture.detectChanges();
      });

      afterEach(() => {
        notifyManager.setScheduler(queueMicrotask);
      });

      it("calls readBasisprofiel with the kvkNummer", () => {
        jest
          .spyOn(klantenService, "readBasisprofiel")
          .mockReturnValue(of(makeBedrijfsprofiel()));
        component["ophalenProfiel"]();
        expect(klantenService.readBasisprofiel).toHaveBeenCalledWith(
          "12345678",
        );
      });

      it("does not call readVestigingsprofiel", () => {
        jest
          .spyOn(klantenService, "readBasisprofiel")
          .mockReturnValue(of(makeBedrijfsprofiel()));
        const spy = jest.spyOn(klantenService, "readVestigingsprofiel");
        component["ophalenProfiel"]();
        expect(spy).not.toHaveBeenCalled();
      });

      it("populates profiel signal after service call", () => {
        const profiel = makeBedrijfsprofiel({ rechtsvorm: "BV" });
        jest
          .spyOn(klantenService, "readBasisprofiel")
          .mockReturnValue(of(profiel));
        component["ophalenProfiel"]();
        expect(component["profiel"]()).toEqual(profiel);
      });

      it("renders rechtsvorm after profiel is loaded", () => {
        jest
          .spyOn(klantenService, "readBasisprofiel")
          .mockReturnValue(of(makeBedrijfsprofiel({ rechtsvorm: "BV" })));
        component["ophalenProfiel"]();
        fixture.detectChanges();
        const element = fixture.debugElement
          .queryAll((de) => de.name === "zac-static-text")
          .find((de) => de.componentInstance.label === "rechtsvorm");
        expect(element).toBeTruthy();
      });

      it("renders uitgebreideRechtsvorm after profiel is loaded", () => {
        jest.spyOn(klantenService, "readBasisprofiel").mockReturnValue(
          of(
            makeBedrijfsprofiel({
              uitgebreideRechtsvorm: "Besloten Vennootschap",
            }),
          ),
        );
        component["ophalenProfiel"]();
        fixture.detectChanges();
        const element = fixture.debugElement
          .queryAll((de) => de.name === "zac-static-text")
          .find((de) => de.componentInstance.label === "uitgebreideRechtsvorm");
        expect(element).toBeTruthy();
      });

      it("renders statutaireNaam after profiel is loaded", () => {
        jest
          .spyOn(klantenService, "readBasisprofiel")
          .mockReturnValue(
            of(makeBedrijfsprofiel({ statutaireNaam: "Test BV Statutair" })),
          );
        component["ophalenProfiel"]();
        fixture.detectChanges();
        const element = fixture.debugElement
          .queryAll((de) => de.name === "zac-static-text")
          .find((de) => de.componentInstance.label === "statutaireNaam");
        expect(element).toBeTruthy();
      });
    });
  });

  describe("when initiatorIdentificatie changes after profiel is loaded", () => {
    beforeEach(async () => {
      notifyManager.setScheduler((fn) => fn());
      const request = httpController.expectOne(vestigingUrl);
      request.flush(testBedrijf);
      await sleep();
      fixture.detectChanges();

      jest
        .spyOn(klantenService, "readVestigingsprofiel")
        .mockReturnValue(of(makeBedrijfsprofiel()));
      component["ophalenProfiel"]();
    });

    afterEach(() => {
      notifyManager.setScheduler(queueMicrotask);
    });

    it("clears profiel signal", () => {
      expect(component["profiel"]()).not.toBeNull();

      componentRef.setInput(
        "zaak",
        fromPartial<GeneratedType<"RestZaak">>({
          ...testZaak,
          initiatorIdentificatie: fromPartial<BetrokkeneIdentificatie>({
            type: "VN",
            kvkNummer: "99999999",
            vestigingsnummer: "99999999",
          }),
        }),
      );
      fixture.detectChanges();

      expect(component["profiel"]()).toBeNull();
    });
  });
});
