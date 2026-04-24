/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { Component, Input } from "@angular/core";
import { ComponentFixture, TestBed } from "@angular/core/testing";
import { NoopAnimationsModule } from "@angular/platform-browser/animations";
import { ActivatedRoute, provideRouter } from "@angular/router";
import { TranslateModule } from "@ngx-translate/core";
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
  template: "",
  standalone: true,
})
class KlantZakenTabelStubComponent {
  @Input() klant: unknown;
}

@Component({
  selector: "zac-klant-contactmomenten-tabel",
  template: "",
  standalone: true,
})
class KlantContactmomentenTabelStubComponent {
  @Input() bsn: unknown;
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
    let utilService: jest.Mocked<UtilService>;

    beforeEach(async () => {
      await configureTestBed();
      utilService = TestBed.inject(UtilService) as jest.Mocked<UtilService>;
      jest.spyOn(utilService, "setTitle");
      fixture = TestBed.createComponent(PersoonViewComponent);
      component = fixture.componentInstance;
      fixture.detectChanges();
    });

    describe("initialisation", () => {
      it("sets the page title to 'persoonsgegevens'", () => {
        expect(utilService.setTitle).toHaveBeenCalledWith("persoonsgegevens");
      });

      it("loads persoon from route resolver data", () => {
        expect(component["persoon"]).toEqual(makePersoon());
      });
    });

    describe("persoonsgegevens card", () => {
      const getStaticTexts = (
        fixtureRef: ComponentFixture<PersoonViewComponent>,
      ) =>
        fixtureRef.debugElement.queryAll((de) => de.name === "zac-static-text");

      it("renders all required persoon fields", () => {
        const labels = [
          "naam",
          "burgerservicenummer",
          "geboortedatum",
          "verblijfplaats",
          "telefoonnummer",
          "emailadres",
        ];
        const renderedLabels = getStaticTexts(fixture).map(
          (de) => de.componentInstance.label,
        );
        for (const label of labels) {
          expect(renderedLabels).toContain(label);
        }
      });

      it("passes naam value to static-text", () => {
        const element = getStaticTexts(fixture).find(
          (de) => de.componentInstance.label === "naam",
        );
        expect(element?.componentInstance.value).toBe("Jan de Vries");
      });

      it("passes bsn value to burgerservicenummer static-text", () => {
        const element = getStaticTexts(fixture).find(
          (de) => de.componentInstance.label === "burgerservicenummer",
        );
        expect(element?.componentInstance.value).toBe("123456789");
      });
    });

    describe("zaken tabel", () => {
      it("renders zac-klant-zaken-tabel", () => {
        expect(
          fixture.nativeElement.querySelector("zac-klant-zaken-tabel"),
        ).toBeTruthy();
      });

      it("passes persoon as klant input to zaken tabel", () => {
        const element = fixture.debugElement.query(
          (de) => de.name === "zac-klant-zaken-tabel",
        );
        expect(element?.componentInstance.klant).toEqual(makePersoon());
      });
    });

    describe("contactmomenten tabel", () => {
      it("renders zac-klant-contactmomenten-tabel when persoon has bsn", () => {
        expect(
          fixture.nativeElement.querySelector(
            "zac-klant-contactmomenten-tabel",
          ),
        ).toBeTruthy();
      });

      it("passes bsn to contactmomenten tabel", () => {
        const element = fixture.debugElement.query(
          (de) => de.name === "zac-klant-contactmomenten-tabel",
        );
        expect(element?.componentInstance.bsn).toBe("123456789");
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
      expect(
        fixture.nativeElement.querySelector("zac-klant-zaken-tabel"),
      ).toBeNull();
    });

    it("does not render zac-klant-contactmomenten-tabel", () => {
      expect(
        fixture.nativeElement.querySelector("zac-klant-contactmomenten-tabel"),
      ).toBeNull();
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
        fixture.nativeElement.querySelector("zac-klant-contactmomenten-tabel"),
      ).toBeNull();
    });

    it("still renders zac-klant-zaken-tabel", () => {
      expect(
        fixture.nativeElement.querySelector("zac-klant-zaken-tabel"),
      ).toBeTruthy();
    });
  });
});
