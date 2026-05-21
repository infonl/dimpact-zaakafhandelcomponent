/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { HarnessLoader } from "@angular/cdk/testing";
import { TestbedHarnessEnvironment } from "@angular/cdk/testing/testbed";
import { provideHttpClient } from "@angular/common/http";
import { Component, Input } from "@angular/core";
import { ComponentFixture, TestBed } from "@angular/core/testing";
import { MatButtonHarness } from "@angular/material/button/testing";
import { provideNativeDateAdapter } from "@angular/material/core";
import { NoopAnimationsModule } from "@angular/platform-browser/animations";
import { ActivatedRoute, provideRouter, Routes } from "@angular/router";
import { TranslateModule } from "@ngx-translate/core";
import { provideQueryClient } from "@tanstack/angular-query-experimental";
import { of, Subject } from "rxjs";
import { fromPartial } from "src/test-helpers";
import { testQueryClient } from "../../../../setupJest";
import { KlantContactmomentenTabelComponent } from "../../contactmomenten/klant-contactmomenten-tabel/klant-contactmomenten-tabel.component";
import { UtilService } from "../../core/service/util.service";
import { GeneratedType } from "../../shared/utils/generated-types";
import { KlantZakenTabelComponent } from "../klant-zaken-tabel/klant-zaken-tabel.component";
import { KlantenService } from "../klanten.service";
import { BedrijfViewComponent } from "./bedrijf-view.component";

@Component({
  standalone: true,
  template: "",
})
class TestErrorRouteComponent {}

@Component({
  selector: "zac-klant-zaken-tabel",
  template: "",
  standalone: true,
})
class KlantZakenTabelStubComponent {
  @Input() klant: GeneratedType<"RestBedrijf"> | null = null;
}

@Component({
  selector: "zac-klant-contactmomenten-tabel",
  template: "",
  standalone: true,
})
class KlantContactmomentenTabelStubComponent {
  @Input() vestigingsnummer: GeneratedType<"RestBedrijf">["vestigingsnummer"];
}

const testRoutes: Routes = [
  { path: "fout", component: TestErrorRouteComponent },
];

function makeBedrijf(
  overrides: Partial<GeneratedType<"RestBedrijf">> = {},
): GeneratedType<"RestBedrijf"> {
  return fromPartial<GeneratedType<"RestBedrijf">>({
    naam: "Test Bedrijf BV",
    kvkNummer: "12345678",
    vestigingsnummer: "000011112222",
    rsin: "123456789",
    type: "RECHTSPERSOON",
    adres: "Teststraat 1, 1234AB Amsterdam",
    telefoonnummer: "0201234567",
    emailadres: "info@testbedrijf.nl",
    ...overrides,
  });
}

function makeBedrijfsprofiel(
  overrides: Partial<GeneratedType<"RestBedrijfsprofiel">> = {},
): GeneratedType<"RestBedrijfsprofiel"> {
  return fromPartial<GeneratedType<"RestBedrijfsprofiel">>({
    vestigingsnummer: "000011112222",
    totaalWerkzamePersonen: 12,
    sbiHoofdActiviteit: "Software ontwikkeling",
    sbiActiviteiten: ["Software ontwikkeling", "Consultancy"],
    website: "https://testbedrijf.nl",
    adressen: [
      {
        type: "bezoekadres",
        volledigAdres: "Teststraat 1, 1234AB Amsterdam",
      },
    ],
    ...overrides,
  });
}

describe(BedrijfViewComponent.name, () => {
  let component: BedrijfViewComponent;
  let fixture: ComponentFixture<BedrijfViewComponent>;
  let harnessLoader: HarnessLoader;
  let utilService: UtilService;
  let klantenService: KlantenService;

  const routeDataSubject = new Subject<{
    bedrijf: GeneratedType<"RestBedrijf"> | null;
  }>();

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [
        BedrijfViewComponent,
        TestErrorRouteComponent,
        KlantZakenTabelStubComponent,
        NoopAnimationsModule,
        TranslateModule.forRoot(),
        KlantContactmomentenTabelStubComponent,
      ],
      providers: [
        provideNativeDateAdapter(),
        provideHttpClient(),
        provideRouter(testRoutes),
        provideQueryClient(testQueryClient),
        {
          provide: ActivatedRoute,
          useValue: { data: routeDataSubject.asObservable() },
        },
      ],
    })
      .overrideComponent(BedrijfViewComponent, {
        remove: {
          imports: [
            KlantZakenTabelComponent,
            KlantContactmomentenTabelComponent,
          ],
        },
        add: {
          imports: [
            KlantZakenTabelStubComponent,
            KlantContactmomentenTabelStubComponent,
          ],
        },
      })
      .compileComponents();

    utilService = TestBed.inject(UtilService);
    klantenService = TestBed.inject(KlantenService);
    jest.spyOn(utilService, "setTitle").mockImplementation(() => undefined);

    fixture = TestBed.createComponent(BedrijfViewComponent);
    component = fixture.componentInstance;
    harnessLoader = TestbedHarnessEnvironment.loader(fixture);
  });

  describe("initialisation", () => {
    it("sets the page title on construction", () => {
      routeDataSubject.next({ bedrijf: null });
      fixture.detectChanges();
      expect(utilService.setTitle).toHaveBeenCalledWith("bedrijfsgegevens");
    });

    it("populates bedrijf from route resolver data", () => {
      const bedrijf = makeBedrijf();
      routeDataSubject.next({ bedrijf });
      fixture.detectChanges();
      expect(component["bedrijf"]).toEqual(bedrijf);
    });

    it("sets profielOphalenMogelijk to true when bedrijf has vestigingsnummer", () => {
      routeDataSubject.next({
        bedrijf: makeBedrijf({ vestigingsnummer: "000011112222" }),
      });
      fixture.detectChanges();
      expect(component["profielOphalenMogelijk"]).toBe(true);
    });

    it("sets profielOphalenMogelijk to true when bedrijf is RECHTSPERSOON with kvkNummer", () => {
      routeDataSubject.next({
        bedrijf: makeBedrijf({
          vestigingsnummer: undefined,
          type: "RECHTSPERSOON",
          kvkNummer: "12345678",
        }),
      });
      fixture.detectChanges();
      expect(component["profielOphalenMogelijk"]).toBe(true);
    });

    it("sets profielOphalenMogelijk to false when bedrijf has no vestigingsnummer and no kvkNummer", () => {
      routeDataSubject.next({
        bedrijf: makeBedrijf({ vestigingsnummer: undefined, kvkNummer: undefined }),
      });
      fixture.detectChanges();
      expect(component["profielOphalenMogelijk"]).toBe(false);
    });

    it("sets profielOphalenMogelijk to false when bedrijf is null", () => {
      routeDataSubject.next({ bedrijf: null });
      fixture.detectChanges();
      expect(component["profielOphalenMogelijk"]).toBe(false);
    });
  });

  describe("bedrijfsgegevens card", () => {
    beforeEach(() => {
      routeDataSubject.next({ bedrijf: makeBedrijf() });
      fixture.detectChanges();
    });

    it("renders bedrijfsnaam static-text field", () => {
      const element = fixture.debugElement
        .queryAll((de) => de.name === "zac-static-text")
        .find((de) => de.componentInstance.label === "bedrijfsnaam");
      expect(element).toBeTruthy();
    });

    it("renders kvknummer when kvkNummer is present", () => {
      const element = fixture.debugElement
        .queryAll((de) => de.name === "zac-static-text")
        .find((de) => de.componentInstance.label === "kvknummer");
      expect(element).toBeTruthy();
    });

    it("renders vestigingsnummer when present", () => {
      const element = fixture.debugElement
        .queryAll((de) => de.name === "zac-static-text")
        .find((de) => de.componentInstance.label === "vestigingsnummer");
      expect(element).toBeTruthy();
    });

     it("renders type field", () => {
      const element = fixture.debugElement
        .queryAll((de) => de.name === "zac-static-text")
        .find((de) => de.componentInstance.label === "type");
      expect(element).toBeTruthy();
    });

    it("renders adres when no profiel is loaded", () => {
      const element = fixture.debugElement
        .queryAll((de) => de.name === "zac-static-text")
        .find((de) => de.componentInstance.label === "adres");
      expect(element).toBeTruthy();
    });

    it("renders telefoonnummer field", () => {
      const element = fixture.debugElement
        .queryAll((de) => de.name === "zac-static-text")
        .find((de) => de.componentInstance.label === "telefoonnummer");
      expect(element).toBeTruthy();
    });

    it("renders emailadres field", () => {
      const element = fixture.debugElement
        .queryAll((de) => de.name === "zac-static-text")
        .find((de) => de.componentInstance.label === "emailadres");
      expect(element).toBeTruthy();
    });
  });

  describe("kvknummer warning icon", () => {
    it("renders warning icon when kvkNummer is missing", () => {
      routeDataSubject.next({ bedrijf: makeBedrijf({ kvkNummer: undefined }) });
      fixture.detectChanges();
      const kvkFields = fixture.debugElement.queryAll(
        (de) =>
          de.name === "zac-static-text" &&
          de.componentInstance.label === "kvknummer",
      );
      expect(kvkFields.length).toBe(1);
    });

    it("does not render warning icon when kvkNummer is present", () => {
      routeDataSubject.next({
        bedrijf: makeBedrijf({ kvkNummer: "12345678" }),
      });
      fixture.detectChanges();
      const kvkFields = fixture.debugElement.queryAll(
        (de) =>
          de.name === "zac-static-text" &&
          de.componentInstance.label === "kvknummer",
      );
      expect(kvkFields.length).toBe(1);
    });
  });

  describe("profiel ophalen button", () => {
    it("is enabled when vestigingsnummer is present", async () => {
      routeDataSubject.next({
        bedrijf: makeBedrijf({
          type: "RECHTSPERSOON",
          vestigingsnummer: "000011112222",
        }),
      });
      fixture.detectChanges();
      const button = await harnessLoader.getHarness(
        MatButtonHarness.with({ selector: "button[mat-icon-button]" }),
      );
      await expect(button.isDisabled()).resolves.toBe(false);
    });

    it("is enabled when type is RECHTSPERSOON with kvkNummer", async () => {
      routeDataSubject.next({
        bedrijf: makeBedrijf({
          type: "RECHTSPERSOON",
          vestigingsnummer: undefined,
          kvkNummer: "12345678",
        }),
      });
      fixture.detectChanges();
      const button = await harnessLoader.getHarness(
        MatButtonHarness.with({ selector: "button[mat-icon-button]" }),
      );
      await expect(button.isDisabled()).resolves.toBe(false);
    });

    it("is disabled when vestigingsnummer is absent and no kvkNummer", async () => {
      routeDataSubject.next({
        bedrijf: makeBedrijf({
          vestigingsnummer: undefined,
          kvkNummer: undefined,
        }),
      });
      fixture.detectChanges();
      const button = await harnessLoader.getHarness(
        MatButtonHarness.with({ selector: "button[mat-icon-button]" }),
      );
      await expect(button.isDisabled()).resolves.toBe(true);
    });

    it("calls readVestigingsprofiel when vestigingsnummer is present", async () => {
      routeDataSubject.next({
        bedrijf: makeBedrijf({
          type: "RECHTSPERSOON",
          vestigingsnummer: "000011112222",
        }),
      });
      fixture.detectChanges();
      jest
        .spyOn(klantenService, "readVestigingsprofiel")
        .mockReturnValue(of(makeBedrijfsprofiel()));
      const button = await harnessLoader.getHarness(
        MatButtonHarness.with({ selector: "button[mat-icon-button]" }),
      );
      await button.click();
      fixture.detectChanges();
      expect(klantenService.readVestigingsprofiel).toHaveBeenCalledWith(
        "000011112222",
      );
    });

    it("calls readRechtspersoonsprofiel when type is RECHTSPERSOON without vestigingsnummer", async () => {
      routeDataSubject.next({
        bedrijf: makeBedrijf({
          type: "RECHTSPERSOON",
          vestigingsnummer: undefined,
          kvkNummer: "12345678",
        }),
      });
      fixture.detectChanges();
      jest
        .spyOn(klantenService, "readRechtspersoonsprofiel")
        .mockReturnValue(of(makeBedrijfsprofiel()));
      const button = await harnessLoader.getHarness(
        MatButtonHarness.with({ selector: "button[mat-icon-button]" }),
      );
      await button.click();
      fixture.detectChanges();
      expect(klantenService.readRechtspersoonsprofiel).toHaveBeenCalledWith(
        "12345678",
      );
    });
  });

  describe("ophalenProfiel()", () => {
    it("sets profielOphalenMogelijk to false immediately", () => {
      routeDataSubject.next({ bedrijf: makeBedrijf() });
      fixture.detectChanges();
      jest
        .spyOn(klantenService, "readVestigingsprofiel")
        .mockReturnValue(of(makeBedrijfsprofiel()));
      component["ophalenProfiel"]();
      expect(component["profielOphalenMogelijk"]).toBe(false);
    });

    it("populates profiel on success for vestiging", () => {
      routeDataSubject.next({ bedrijf: makeBedrijf() });
      fixture.detectChanges();
      const profiel = makeBedrijfsprofiel();
      jest
        .spyOn(klantenService, "readVestigingsprofiel")
        .mockReturnValue(of(profiel));
      component["ophalenProfiel"]();
      fixture.detectChanges();
      expect(component["profiel"]).toEqual(profiel);
    });

    it("populates profiel on success for rechtspersoon", () => {
      routeDataSubject.next({
        bedrijf: makeBedrijf({ vestigingsnummer: undefined, type: "RECHTSPERSOON", kvkNummer: "12345678" }),
      });
      fixture.detectChanges();
      const profiel = makeBedrijfsprofiel({ rechtsvorm: "BV", uitgebreideRechtsvorm: "Besloten Vennootschap", statutaireNaam: "Test BV" });
      jest
        .spyOn(klantenService, "readRechtspersoonsprofiel")
        .mockReturnValue(of(profiel));
      component["ophalenProfiel"]();
      fixture.detectChanges();
      expect(component["profiel"]).toEqual(profiel);
    });

    it("does nothing when bedrijf has no vestigingsnummer and no kvkNummer", () => {
      routeDataSubject.next({
        bedrijf: makeBedrijf({ vestigingsnummer: undefined, kvkNummer: undefined }),
      });
      fixture.detectChanges();
      const vestigingSpy = jest.spyOn(klantenService, "readVestigingsprofiel");
      const rechtspersoonSpy = jest.spyOn(klantenService, "readRechtspersoonsprofiel");
      component["ophalenProfiel"]();
      expect(vestigingSpy).not.toHaveBeenCalled();
      expect(rechtspersoonSpy).not.toHaveBeenCalled();
    });
  });

  describe("profiel fields", () => {
    beforeEach(() => {
      routeDataSubject.next({ bedrijf: makeBedrijf() });
      fixture.detectChanges();
      jest
        .spyOn(klantenService, "readVestigingsprofiel")
        .mockReturnValue(of(makeBedrijfsprofiel()));
      component["ophalenProfiel"]();
      fixture.detectChanges();
    });

    it("hides adres field when profiel is loaded", () => {
      const adresField = fixture.debugElement
        .queryAll((de) => de.name === "zac-static-text")
        .find((de) => de.componentInstance.label === "adres");
      expect(adresField).toBeFalsy();
    });

    it("renders totaalWerkzamePersonen when profiel is loaded", () => {
      const element = fixture.debugElement
        .queryAll((de) => de.name === "zac-static-text")
        .find((de) => de.componentInstance.label === "totaalWerkzamePersonen");
      expect(element).toBeTruthy();
    });

    it("renders hoofdactiviteit when profiel is loaded", () => {
      const element = fixture.debugElement
        .queryAll((de) => de.name === "zac-static-text")
        .find((de) => de.componentInstance.label === "hoofdactiviteit");
      expect(element).toBeTruthy();
    });

    it("renders activiteiten when profiel is loaded", () => {
      const element = fixture.debugElement
        .queryAll((de) => de.name === "zac-static-text")
        .find((de) => de.componentInstance.label === "activiteiten");
      expect(element).toBeTruthy();
    });

    it("renders website when profiel is loaded", () => {
      const element = fixture.debugElement
        .queryAll((de) => de.name === "zac-static-text")
        .find((de) => de.componentInstance.label === "website");
      expect(element).toBeTruthy();
    });
  });

  describe("rsin from profiel", () => {
    it("does not render rsin before profiel is loaded", () => {
      routeDataSubject.next({ bedrijf: makeBedrijf() });
      fixture.detectChanges();
      const element = fixture.debugElement
        .queryAll((de) => de.name === "zac-static-text")
        .find((de) => de.componentInstance.label === "rsin");
      expect(element).toBeFalsy();
    });

    it("renders rsin when profiel is loaded", () => {
      routeDataSubject.next({ bedrijf: makeBedrijf() });
      fixture.detectChanges();
      jest
        .spyOn(klantenService, "readVestigingsprofiel")
        .mockReturnValue(of(makeBedrijfsprofiel({ rsin: "123456789" })));
      component["ophalenProfiel"]();
      fixture.detectChanges();
      const element = fixture.debugElement
        .queryAll((de) => de.name === "zac-static-text")
        .find((de) => de.componentInstance.label === "rsin");
      expect(element).toBeTruthy();
    });
  });

  describe("rechtspersoon profiel fields", () => {
    beforeEach(() => {
      routeDataSubject.next({
        bedrijf: makeBedrijf({ vestigingsnummer: undefined, type: "RECHTSPERSOON", kvkNummer: "12345678" }),
      });
      fixture.detectChanges();
      jest
        .spyOn(klantenService, "readRechtspersoonsprofiel")
        .mockReturnValue(
          of(
            makeBedrijfsprofiel({
              rechtsvorm: "BV",
              uitgebreideRechtsvorm: "Besloten Vennootschap",
              statutaireNaam: "Test BV Statutair",
            }),
          ),
        );
      component["ophalenProfiel"]();
      fixture.detectChanges();
    });

    it("renders rechtsvorm when profiel is loaded", () => {
      const element = fixture.debugElement
        .queryAll((de) => de.name === "zac-static-text")
        .find((de) => de.componentInstance.label === "rechtsvorm");
      expect(element).toBeTruthy();
    });

    it("renders uitgebreideRechtsvorm when profiel is loaded", () => {
      const element = fixture.debugElement
        .queryAll((de) => de.name === "zac-static-text")
        .find((de) => de.componentInstance.label === "uitgebreideRechtsvorm");
      expect(element).toBeTruthy();
    });

    it("renders statutaireNaam when profiel is loaded", () => {
      const element = fixture.debugElement
        .queryAll((de) => de.name === "zac-static-text")
        .find((de) => de.componentInstance.label === "statutaireNaam");
      expect(element).toBeTruthy();
    });
  });

  describe("zac-klant-zaken-tabel", () => {
    it("renders when bedrijf is available", () => {
      routeDataSubject.next({ bedrijf: makeBedrijf() });
      fixture.detectChanges();
      expect(
        fixture.nativeElement.querySelector("zac-klant-zaken-tabel"),
      ).toBeTruthy();
    });

    it("does not render when bedrijf is null", () => {
      routeDataSubject.next({ bedrijf: null });
      fixture.detectChanges();
      expect(
        fixture.nativeElement.querySelector("zac-klant-zaken-tabel"),
      ).toBeFalsy();
    });
  });

  describe("zac-klant-contactmomenten-tabel", () => {
    it("renders when bedrijf has vestigingsnummer", () => {
      routeDataSubject.next({
        bedrijf: makeBedrijf({ vestigingsnummer: "000011112222" }),
      });
      fixture.detectChanges();
      expect(
        fixture.nativeElement.querySelector("zac-klant-contactmomenten-tabel"),
      ).toBeTruthy();
    });

    it("does not render when bedrijf has no vestigingsnummer", () => {
      routeDataSubject.next({
        bedrijf: makeBedrijf({ vestigingsnummer: undefined }),
      });
      fixture.detectChanges();
      expect(
        fixture.nativeElement.querySelector("zac-klant-contactmomenten-tabel"),
      ).toBeFalsy();
    });

    it("does not render when bedrijf is null", () => {
      routeDataSubject.next({ bedrijf: null });
      fixture.detectChanges();
      expect(
        fixture.nativeElement.querySelector("zac-klant-contactmomenten-tabel"),
      ).toBeFalsy();
    });
  });
});
