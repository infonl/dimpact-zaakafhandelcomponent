/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { Component, input } from "@angular/core";
import { ComponentFixture, TestBed } from "@angular/core/testing";
import { NoopAnimationsModule } from "@angular/platform-browser/animations";
import { ActivatedRoute, provideRouter } from "@angular/router";
import { TranslateModule } from "@ngx-translate/core";
import { screen } from "@testing-library/angular";
import { of } from "rxjs";
import { KlantContactmomentenTabelComponent } from "../../contactmomenten/klant-contactmomenten-tabel/klant-contactmomenten-tabel.component";
import { UtilService } from "../../core/service/util.service";
import { DatumPipe } from "../../shared/pipes/datum.pipe";
import { StaticTextComponent } from "../../shared/static-text/static-text.component";
import { GeneratedType } from "../../shared/utils/generated-types";
import { KlantZakenTabelComponent } from "../klant-zaken-tabel/klant-zaken-tabel.component";
import { PersoonViewComponent } from "./persoon-view.component";

@Component({
  selector: "zac-klant-zaken-tabel",
  template: "<p>zaken of klant {{ klant().naam }} ({{ klant().bsn }})</p>",
  standalone: true,
})
class KlantZakenTabelStubComponent {
  readonly klant = input.required<GeneratedType<"RestPersoon">>();
}

@Component({
  selector: "zac-klant-contactmomenten-tabel",
  template: "<p>contactmomenten of bsn {{ bsn() }}</p>",
  standalone: true,
})
class KlantContactmomentenTabelStubComponent {
  readonly bsn = input.required<GeneratedType<"RestPersoon">["bsn"]>();
}

const makePersoon = (
  overrides: Partial<GeneratedType<"RestPersoon">> = {},
): GeneratedType<"RestPersoon"> => ({
  naam: "Jan de Vries",
  bsn: "123456789",
  geboortedatum: "1990-01-15",
  verblijfplaats: "Amsterdam",
  telefoonnummer: "0612345678",
  emailadres: "jan.devries@example.com",
  indicaties: [],
  ...overrides,
});

const configureTestBed = (
  persoon: GeneratedType<"RestPersoon"> | null = makePersoon(),
) =>
  TestBed.configureTestingModule({
    imports: [
      NoopAnimationsModule,
      TranslateModule.forRoot(),
      PersoonViewComponent,
      KlantZakenTabelStubComponent,
      KlantContactmomentenTabelStubComponent,
      StaticTextComponent,
      DatumPipe,
    ],
    providers: [
      provideRouter([]),
      {
        provide: ActivatedRoute,
        useValue: { data: of({ persoon }) },
      },
      UtilService,
    ],
  })
    .overrideComponent(PersoonViewComponent, {
      remove: {
        imports: [KlantZakenTabelComponent, KlantContactmomentenTabelComponent],
      },
      add: {
        imports: [
          KlantZakenTabelStubComponent,
          KlantContactmomentenTabelStubComponent,
        ],
      },
    })
    .compileComponents();

describe(PersoonViewComponent.name, () => {
  describe("with a full persoon", () => {
    let component: PersoonViewComponent;
    let fixture: ComponentFixture<PersoonViewComponent>;
    let utilService: UtilService;
    let setTitleSpy: jest.SpiedFunction<UtilService["setTitle"]>;

    beforeEach(async () => {
      await configureTestBed();
      utilService = TestBed.inject(UtilService);
      setTitleSpy = jest.spyOn(utilService, "setTitle");
      fixture = TestBed.createComponent(PersoonViewComponent);
      component = fixture.componentInstance;
      fixture.detectChanges();
    });

    describe("initialisation", () => {
      it("sets the page title to 'persoonsgegevens'", () => {
        expect(setTitleSpy).toHaveBeenCalledWith("persoonsgegevens");
      });

      it("loads persoon from route resolver data", () => {
        expect(component["persoon"]).toEqual(makePersoon());
      });
    });

    describe("persoonsgegevens card", () => {
      it("renders all required persoon fields", () => {
        const labels = [
          "naam",
          "burgerservicenummer",
          "geboortedatum",
          "verblijfplaats",
          "telefoonnummer",
          "emailadres",
        ];
        for (const label of labels) {
          expect(screen.getByText(label)).toBeInTheDocument();
        }
      });

      it("passes naam value to static-text", () => {
        expect(screen.getByText("naam")).toBeInTheDocument();
        expect(screen.getByText("Jan de Vries")).toBeInTheDocument();
      });

      it("passes bsn value to burgerservicenummer static-text", () => {
        expect(screen.getByText("burgerservicenummer")).toBeInTheDocument();
        expect(screen.getByText("123456789")).toBeInTheDocument();
      });
    });

    describe("zaken tabel", () => {
      it("renders zac-klant-zaken-tabel", () => {
        expect(screen.getByText(/^zaken of klant/)).toBeInTheDocument();
      });

      it("passes persoon as klant input to zaken tabel", () => {
        expect(
          screen.getByText("zaken of klant Jan de Vries (123456789)"),
        ).toBeInTheDocument();
      });
    });

    describe("contactmomenten tabel", () => {
      it("renders zac-klant-contactmomenten-tabel when persoon has bsn", () => {
        expect(screen.getByText(/^contactmomenten of bsn/)).toBeInTheDocument();
      });

      it("passes bsn to contactmomenten tabel", () => {
        expect(
          screen.getByText("contactmomenten of bsn 123456789"),
        ).toBeInTheDocument();
      });
    });
  });

  describe("when persoon is null", () => {
    let fixture: ComponentFixture<PersoonViewComponent>;

    beforeEach(async () => {
      await configureTestBed(null);
      fixture = TestBed.createComponent(PersoonViewComponent);
      fixture.detectChanges();
    });

    it("does not render zac-klant-zaken-tabel", () => {
      expect(screen.queryByText(/^zaken of klant/)).not.toBeInTheDocument();
    });

    it("does not render zac-klant-contactmomenten-tabel", () => {
      expect(
        screen.queryByText(/^contactmomenten of bsn/),
      ).not.toBeInTheDocument();
    });
  });

  describe("when persoon has no bsn", () => {
    let fixture: ComponentFixture<PersoonViewComponent>;

    beforeEach(async () => {
      await configureTestBed(makePersoon({ bsn: undefined }));
      fixture = TestBed.createComponent(PersoonViewComponent);
      fixture.detectChanges();
    });

    it("does not render zac-klant-contactmomenten-tabel", () => {
      expect(
        screen.queryByText(/^contactmomenten of bsn/),
      ).not.toBeInTheDocument();
    });

    it("still renders zac-klant-zaken-tabel", () => {
      expect(screen.getByText(/^zaken of klant/)).toBeInTheDocument();
    });
  });
});
