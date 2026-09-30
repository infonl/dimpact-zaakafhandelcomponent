/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { provideHttpClient } from "@angular/common/http";
import { Component, input } from "@angular/core";
import { ComponentFixture, TestBed } from "@angular/core/testing";
import { provideNativeDateAdapter } from "@angular/material/core";
import { NoopAnimationsModule } from "@angular/platform-browser/animations";
import { ActivatedRoute, provideRouter, Routes } from "@angular/router";
import { TranslateModule } from "@ngx-translate/core";
import { provideQueryClient } from "@tanstack/angular-query-experimental";
import { screen } from "@testing-library/angular";
import userEvent from "@testing-library/user-event";
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
  template: "<p>zaken of klant {{ klant().naam }}</p>",
  standalone: true,
})
class KlantZakenTabelStubComponent {
  readonly klant = input.required<GeneratedType<"RestBedrijf">>();
}

@Component({
  selector: "zac-klant-contactmomenten-tabel",
  template: "<p>contactmomenten of vestiging {{ vestigingsnummer() }}</p>",
  standalone: true,
})
class KlantContactmomentenTabelStubComponent {
  readonly vestigingsnummer =
    input.required<GeneratedType<"RestBedrijf">["vestigingsnummer"]>();
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
    adres: {
      type: "bezoekadres",
      afgeschermd: false,
      volledigAdres: "Teststraat 1, 1234AB Amsterdam",
    },
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

function expectStaticText(label: string, value: string) {
  expect(screen.getByText(label)).toBeInTheDocument();
  expect(screen.getByText(value)).toBeInTheDocument();
}

function profielOphalenButton() {
  return screen.getByRole("button", { name: "bedrijf.profiel.ophalen" });
}

describe(BedrijfViewComponent.name, () => {
  const user = userEvent.setup();

  let component: BedrijfViewComponent;
  let fixture: ComponentFixture<BedrijfViewComponent>;
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
        bedrijf: makeBedrijf({
          vestigingsnummer: undefined,
          kvkNummer: undefined,
        }),
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
      expectStaticText("bedrijfsnaam", "Test Bedrijf BV");
    });

    it("renders kvknummer when kvkNummer is present", () => {
      expectStaticText("kvknummer", "12345678");
    });

    it("renders vestigingsnummer when present", () => {
      expectStaticText("vestigingsnummer", "000011112222");
    });

    it("renders type field", () => {
      expectStaticText("type", "RECHTSPERSOON");
    });

    it("renders adres when no profiel is loaded", () => {
      expectStaticText("bezoekadres", "Teststraat 1, 1234AB Amsterdam");
    });

    it("renders telefoonnummer field", () => {
      expectStaticText("telefoonnummer", "0201234567");
    });

    it("renders emailadres field", () => {
      expectStaticText("emailadres", "info@testbedrijf.nl");
    });
  });

  describe("kvknummer warning icon", () => {
    it("renders warning icon when kvkNummer is missing", () => {
      routeDataSubject.next({ bedrijf: makeBedrijf({ kvkNummer: undefined }) });
      fixture.detectChanges();

      expectStaticText("kvknummer", "msg.error.kvk.unknown");
      expect(screen.getByText("warning")).toBeInTheDocument();
    });

    it("does not render warning icon when kvkNummer is present", () => {
      routeDataSubject.next({
        bedrijf: makeBedrijf({ kvkNummer: "12345678" }),
      });
      fixture.detectChanges();

      expectStaticText("kvknummer", "12345678");
      expect(screen.queryByText("warning")).not.toBeInTheDocument();
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
      expect(profielOphalenButton()).toBeEnabled();
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
      expect(profielOphalenButton()).toBeEnabled();
    });

    it("is disabled when vestigingsnummer is absent and no kvkNummer", async () => {
      routeDataSubject.next({
        bedrijf: makeBedrijf({
          vestigingsnummer: undefined,
          kvkNummer: undefined,
        }),
      });
      fixture.detectChanges();
      expect(profielOphalenButton()).toBeDisabled();
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
      await user.click(profielOphalenButton());
      fixture.detectChanges();
      expect(klantenService.readVestigingsprofiel).toHaveBeenCalledWith(
        "000011112222",
      );
    });

    it("calls readBasisprofiel when type is RECHTSPERSOON without vestigingsnummer", async () => {
      routeDataSubject.next({
        bedrijf: makeBedrijf({
          type: "RECHTSPERSOON",
          vestigingsnummer: undefined,
          kvkNummer: "12345678",
        }),
      });
      fixture.detectChanges();
      jest
        .spyOn(klantenService, "readBasisprofiel")
        .mockReturnValue(of(makeBedrijfsprofiel()));
      await user.click(profielOphalenButton());
      fixture.detectChanges();
      expect(klantenService.readBasisprofiel).toHaveBeenCalledWith("12345678");
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
        bedrijf: makeBedrijf({
          vestigingsnummer: undefined,
          type: "RECHTSPERSOON",
          kvkNummer: "12345678",
        }),
      });
      fixture.detectChanges();
      const profiel = makeBedrijfsprofiel({
        rechtsvorm: "BV",
        uitgebreideRechtsvorm: "Besloten Vennootschap",
        statutaireNaam: "Test BV",
      });
      jest
        .spyOn(klantenService, "readBasisprofiel")
        .mockReturnValue(of(profiel));
      component["ophalenProfiel"]();
      fixture.detectChanges();
      expect(component["profiel"]).toEqual(profiel);
    });

    it("does nothing when bedrijf has no vestigingsnummer and no kvkNummer", () => {
      routeDataSubject.next({
        bedrijf: makeBedrijf({
          vestigingsnummer: undefined,
          kvkNummer: undefined,
        }),
      });
      fixture.detectChanges();
      const vestigingSpy = jest.spyOn(klantenService, "readVestigingsprofiel");
      const rechtspersoonSpy = jest.spyOn(klantenService, "readBasisprofiel");
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
      expect(screen.getAllByText("bezoekadres")).toHaveLength(1);
      expect(
        screen.getAllByText("Teststraat 1, 1234AB Amsterdam"),
      ).toHaveLength(1);
    });

    it("renders totaalWerkzamePersonen when profiel is loaded", () => {
      expectStaticText("totaalWerkzamePersonen", "12");
    });

    it("renders hoofdactiviteit when profiel is loaded", () => {
      expectStaticText("hoofdactiviteit", "Software ontwikkeling");
    });

    it("renders activiteiten when profiel is loaded", () => {
      expectStaticText("activiteiten", "Software ontwikkeling, Consultancy");
    });

    it("renders website when profiel is loaded", () => {
      expectStaticText("website", "https://testbedrijf.nl");
    });
  });

  describe("rsin from profiel", () => {
    it("does not render rsin before profiel is loaded", () => {
      routeDataSubject.next({ bedrijf: makeBedrijf() });
      fixture.detectChanges();

      expect(screen.queryByText("rsin")).not.toBeInTheDocument();
    });

    it("renders rsin when profiel is loaded", () => {
      routeDataSubject.next({ bedrijf: makeBedrijf() });
      fixture.detectChanges();
      jest
        .spyOn(klantenService, "readVestigingsprofiel")
        .mockReturnValue(of(makeBedrijfsprofiel({ rsin: "123456789" })));
      component["ophalenProfiel"]();
      fixture.detectChanges();

      expectStaticText("rsin", "123456789");
    });
  });

  describe("rechtspersoon profiel fields", () => {
    beforeEach(() => {
      routeDataSubject.next({
        bedrijf: makeBedrijf({
          vestigingsnummer: undefined,
          type: "RECHTSPERSOON",
          kvkNummer: "12345678",
        }),
      });
      fixture.detectChanges();
      jest.spyOn(klantenService, "readBasisprofiel").mockReturnValue(
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
      expectStaticText("rechtsvorm", "BV");
    });

    it("renders uitgebreideRechtsvorm when profiel is loaded", () => {
      expectStaticText("uitgebreideRechtsvorm", "Besloten Vennootschap");
    });

    it("renders statutaireNaam when profiel is loaded", () => {
      expectStaticText("statutaireNaam", "Test BV Statutair");
    });
  });

  describe("zac-klant-zaken-tabel", () => {
    it("renders the zaken of the bedrijf when bedrijf is available", () => {
      routeDataSubject.next({ bedrijf: makeBedrijf() });
      fixture.detectChanges();

      expect(
        screen.getByText("zaken of klant Test Bedrijf BV"),
      ).toBeInTheDocument();
    });

    it("does not render when bedrijf is null", () => {
      routeDataSubject.next({ bedrijf: null });
      fixture.detectChanges();

      expect(screen.queryByText(/^zaken of klant/)).not.toBeInTheDocument();
    });
  });

  describe("zac-klant-contactmomenten-tabel", () => {
    it("renders the contactmomenten of the vestiging when bedrijf has vestigingsnummer", () => {
      routeDataSubject.next({
        bedrijf: makeBedrijf({ vestigingsnummer: "000011112222" }),
      });
      fixture.detectChanges();

      expect(
        screen.getByText("contactmomenten of vestiging 000011112222"),
      ).toBeInTheDocument();
    });

    it("does not render when bedrijf has no vestigingsnummer", () => {
      routeDataSubject.next({
        bedrijf: makeBedrijf({ vestigingsnummer: undefined }),
      });
      fixture.detectChanges();

      expect(
        screen.queryByText(/^contactmomenten of vestiging/),
      ).not.toBeInTheDocument();
    });

    it("does not render when bedrijf is null", () => {
      routeDataSubject.next({ bedrijf: null });
      fixture.detectChanges();

      expect(
        screen.queryByText(/^contactmomenten of vestiging/),
      ).not.toBeInTheDocument();
    });
  });
});
