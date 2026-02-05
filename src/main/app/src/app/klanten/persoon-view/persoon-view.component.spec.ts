/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { ComponentFixture, TestBed } from "@angular/core/testing";
import { ActivatedRoute } from "@angular/router";
import { TranslateModule } from "@ngx-translate/core";
import { of } from "rxjs";
import { UtilService } from "../../core/service/util.service";
import { DatumPipe } from "../../shared/pipes/datum.pipe";
import { GeneratedType } from "../../shared/utils/generated-types";
import { PersoonViewComponent } from "./persoon-view.component";

describe(PersoonViewComponent.name, () => {
  let component: PersoonViewComponent;
  let fixture: ComponentFixture<PersoonViewComponent>;

  const mockPersoon: GeneratedType<"RestPersoon"> = {
    naam: "Jan de Vries",
    bsn: "123456789",
    geboortedatum: "1990-01-15",
    verblijfplaats: "Amsterdam",
    telefoonnummer: "0612345678",
    emailadres: "jan.devries@example.com",
    indicaties: [],
  };

  const mockActivatedRoute = {
    data: of({ persoon: mockPersoon }),
  };

  const mockUtilService = {
    setTitle: jest.fn(),
  };

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      declarations: [PersoonViewComponent, DatumPipe],
      imports: [TranslateModule.forRoot()],
      providers: [
        {
          provide: ActivatedRoute,
          useValue: mockActivatedRoute,
        },
        {
          provide: UtilService,
          useValue: mockUtilService,
        },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(PersoonViewComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it("should receive persoon data from route resolver", () => {
    expect(component["persoon"]).toEqual(mockPersoon);
  });

  it("should render all person data fields", () => {
    const compiled = fixture.nativeElement as HTMLElement;

    const nameElement = compiled.querySelector('zac-static-text[label="naam"]');
    expect(nameElement).toBeTruthy();

    const bsnElement = compiled.querySelector(
      'zac-static-text[label="burgerservicenummer"]',
    );
    expect(bsnElement).toBeTruthy();

    const geboortedatumElement = compiled.querySelector(
      'zac-static-text[label="geboortedatum"]',
    );
    expect(geboortedatumElement).toBeTruthy();

    const verblijfplaatsElement = compiled.querySelector(
      'zac-static-text[label="verblijfplaats"]',
    );
    expect(verblijfplaatsElement).toBeTruthy();

    const telefoonnummerElement = compiled.querySelector(
      'zac-static-text[label="telefoonnummer"]',
    );
    expect(telefoonnummerElement).toBeTruthy();

    const emailadresElement = compiled.querySelector(
      'zac-static-text[label="emailadres"]',
    );
    expect(emailadresElement).toBeTruthy();
  });

  it("should render zaken table when persoon is available", () => {
    const compiled = fixture.nativeElement as HTMLElement;
    const zakenTabel = compiled.querySelector("zac-klant-zaken-tabel");
    expect(zakenTabel).toBeTruthy();
  });

  it("should render empty zaken table", () => {
    const compiled = fixture.nativeElement as HTMLElement;
    const zakenTabel = compiled.querySelector("zac-klant-zaken-tabel");
    const tableRows = zakenTabel?.querySelectorAll("mat-row");
    expect(tableRows?.length).toBe(0);
  });

  it("should render contactmomenten table when persoon has BSN", () => {
    const compiled = fixture.nativeElement as HTMLElement;
    const contactmomentenTabel = compiled.querySelector(
      "zac-klant-contactmomenten-tabel",
    );
    expect(contactmomentenTabel).toBeTruthy();
  });

  it("should render empty contactmomenten table", () => {
    const compiled = fixture.nativeElement as HTMLElement;
    const contactmomentenTabel = compiled.querySelector(
      "zac-klant-contactmomenten-tabel",
    );
    const tableRows = contactmomentenTabel?.querySelectorAll("mat-row");
    expect(tableRows?.length).toBe(0);
  });
});
