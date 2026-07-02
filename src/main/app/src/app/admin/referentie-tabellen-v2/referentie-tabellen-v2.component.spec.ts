/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { ComponentFixture, TestBed } from "@angular/core/testing";
import { NoopAnimationsModule } from "@angular/platform-browser/animations";
import { provideRouter } from "@angular/router";
import { TranslateModule } from "@ngx-translate/core";
import { of } from "rxjs";
import { ConfiguratieService } from "../../configuratie/configuratie.service";
import { UtilService } from "../../core/service/util.service";
import { GeneratedType } from "../../shared/utils/generated-types";
import { ReferentieTabelService } from "../referentie-tabel.service";
import { ReferentieTabellenV2Component } from "./referentie-tabellen-v2.component";

const tabellen: GeneratedType<"RestReferenceTable">[] = [
  { id: 1, code: "TABEL_A", naam: "Tabel A", systeem: false, aantalWaarden: 2 },
  { id: 2, code: "TABEL_B", naam: "Tabel B", systeem: true, aantalWaarden: 1 },
];

const geladenTabelA: GeneratedType<"RestReferenceTable"> = {
  id: 1,
  code: "TABEL_A",
  naam: "Tabel A",
  systeem: false,
  aantalWaarden: 2,
  waarden: [
    { id: 10, naam: "Waarde A1" },
    { id: 11, naam: "Waarde A2" },
  ],
};

describe(ReferentieTabellenV2Component.name, () => {
  let fixture: ComponentFixture<ReferentieTabellenV2Component>;
  let component: ReferentieTabellenV2Component;
  let utilServiceMock: Pick<UtilService, "setTitle" | "openSnackbar">;
  let referentieTabelServiceMock: Pick<
    ReferentieTabelService,
    "listReferentieTabellen" | "readReferentieTabel"
  >;

  beforeEach(async () => {
    utilServiceMock = { setTitle: jest.fn(), openSnackbar: jest.fn() };
    referentieTabelServiceMock = {
      listReferentieTabellen: jest.fn().mockReturnValue(of(tabellen)),
      readReferentieTabel: jest.fn().mockReturnValue(of(geladenTabelA)),
    };

    await TestBed.configureTestingModule({
      imports: [
        ReferentieTabellenV2Component,
        TranslateModule.forRoot(),
        NoopAnimationsModule,
      ],
      providers: [
        provideRouter([]),
        { provide: UtilService, useValue: utilServiceMock },
        {
          provide: ConfiguratieService,
          useValue: {} satisfies Partial<ConfiguratieService>,
        },
        {
          provide: ReferentieTabelService,
          useValue: referentieTabelServiceMock,
        },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(ReferentieTabellenV2Component);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it("should set the title and load the tabellen on init", () => {
    expect(utilServiceMock.setTitle).toHaveBeenCalledWith(
      "title.referentietabellen.v2",
      undefined,
    );
    expect(
      referentieTabelServiceMock.listReferentieTabellen,
    ).toHaveBeenCalled();
  });

  it("should render an expansion panel per tabel", () => {
    const panels =
      fixture.nativeElement.querySelectorAll("mat-expansion-panel");
    expect(panels).toHaveLength(2);
    expect(fixture.nativeElement.textContent).toContain("TABEL_A");
    expect(fixture.nativeElement.textContent).toContain("TABEL_B");
  });

  it("should lazily load the waarden when a panel is opened", () => {
    component["onPanelOpened"](tabellen[0]);

    expect(
      referentieTabelServiceMock.readReferentieTabel,
    ).toHaveBeenCalledWith(1);
    expect(component["getWaarden"](tabellen[0])).toEqual(
      geladenTabelA.waarden,
    );
  });

  it("should not reload the waarden for an already loaded tabel", () => {
    component["onPanelOpened"](tabellen[0]);
    component["onPanelOpened"](tabellen[0]);

    expect(
      referentieTabelServiceMock.readReferentieTabel,
    ).toHaveBeenCalledTimes(1);
  });

  it("should render the loaded waarden inside the panel", () => {
    component["onPanelOpened"](tabellen[0]);
    fixture.detectChanges();

    expect(fixture.nativeElement.textContent).toContain("Waarde A1");
    expect(fixture.nativeElement.textContent).toContain("Waarde A2");
  });
});
