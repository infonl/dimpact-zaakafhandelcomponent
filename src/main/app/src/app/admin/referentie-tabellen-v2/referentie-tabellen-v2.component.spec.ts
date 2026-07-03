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
    | "listReferentieTabellen"
    | "readReferentieTabel"
    | "createReferentieTabel"
  >;

  beforeEach(async () => {
    utilServiceMock = { setTitle: jest.fn(), openSnackbar: jest.fn() };
    referentieTabelServiceMock = {
      listReferentieTabellen: jest.fn().mockReturnValue(of(tabellen)),
      readReferentieTabel: jest.fn().mockReturnValue(of(geladenTabelA)),
      createReferentieTabel: jest.fn().mockReturnValue(of(geladenTabelA)),
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

  function findButton(text: string): HTMLButtonElement | undefined {
    return Array.from(
      fixture.nativeElement.querySelectorAll<HTMLButtonElement>("button"),
    ).find((button) => button.textContent?.includes(text));
  }

  it("should set the title and load the tabellen on init", () => {
    expect(utilServiceMock.setTitle).toHaveBeenCalledWith(
      "title.referentietabellen.v2",
      undefined,
    );
    expect(
      referentieTabelServiceMock.listReferentieTabellen,
    ).toHaveBeenCalled();
  });

  it("should render a row per tabel", () => {
    const rows = fixture.nativeElement.querySelectorAll(".tabel-row");
    expect(rows).toHaveLength(2);
    expect(fixture.nativeElement.textContent).toContain("TABEL_A");
    expect(fixture.nativeElement.textContent).toContain("TABEL_B");
  });

  it("should lazily load the waarden when a row is expanded", () => {
    component["toggle"](tabellen[0]);

    expect(
      referentieTabelServiceMock.readReferentieTabel,
    ).toHaveBeenCalledWith(1);
    expect(component["expandedId"]).toBe(1);
    expect(component["getLoadedTabel"](tabellen[0])).toEqual(geladenTabelA);
  });

  it("should collapse when the expanded row is toggled again", () => {
    component["toggle"](tabellen[0]);
    component["toggle"](tabellen[0]);

    expect(component["expandedId"]).toBeNull();
  });

  it("should open the create form and disable the header button when clicked", () => {
    expect(component["showCreateForm"]).toBe(false);
    const headerButton = findButton("referentietabel.toevoegen");
    expect(headerButton?.disabled).toBe(false);

    headerButton?.click();
    fixture.detectChanges();

    expect(component["showCreateForm"]).toBe(true);
    expect(findButton("referentietabel.toevoegen")?.disabled).toBe(true);
  });

  it("should disable the submit button until both code and naam are filled", () => {
    component["openCreateForm"]();
    fixture.detectChanges();

    const button = fixture.nativeElement.querySelector(
      "button[type='submit']",
    ) as HTMLButtonElement;
    expect(button.disabled).toBe(true);

    component["form"].setValue({ code: "NEW_CODE", naam: "New name" });
    fixture.detectChanges();

    expect(button.disabled).toBe(false);
  });

  it("should close and reset the form when cancel is clicked", () => {
    component["openCreateForm"]();
    component["form"].setValue({ code: "NEW_CODE", naam: "New name" });
    fixture.detectChanges();

    findButton("actie.annuleren")?.click();
    fixture.detectChanges();

    expect(component["showCreateForm"]).toBe(false);
    expect(component["form"].value).toEqual({ code: "", naam: "" });
    expect(
      referentieTabelServiceMock.createReferentieTabel,
    ).not.toHaveBeenCalled();
  });

  it("should create the tabel, close the form and reload when submitted", () => {
    component["openCreateForm"]();
    component["form"].setValue({ code: "NEW_CODE", naam: "New name" });

    component["addReferentieTabel"]();

    expect(
      referentieTabelServiceMock.createReferentieTabel,
    ).toHaveBeenCalledWith({
      code: "NEW_CODE",
      naam: "New name",
      systeem: false,
      waarden: [],
    });
    expect(utilServiceMock.openSnackbar).toHaveBeenCalledWith(
      "msg.referentietabel.toegevoegd",
      { tabel: "NEW_CODE" },
    );
    expect(component["showCreateForm"]).toBe(false);
    // once on init, once after creating
    expect(
      referentieTabelServiceMock.listReferentieTabellen,
    ).toHaveBeenCalledTimes(2);
    expect(component["form"].value).toEqual({ code: "", naam: "" });
  });

  it("should not create a tabel when the form is invalid", () => {
    component["addReferentieTabel"]();

    expect(
      referentieTabelServiceMock.createReferentieTabel,
    ).not.toHaveBeenCalled();
  });

  it("should not reload the waarden for an already loaded tabel", () => {
    component["toggle"](tabellen[0]);
    component["toggle"](tabellen[0]);
    component["toggle"](tabellen[0]);

    expect(
      referentieTabelServiceMock.readReferentieTabel,
    ).toHaveBeenCalledTimes(1);
  });

  it("should render the loaded waarden below the expanded row", () => {
    component["toggle"](tabellen[0]);
    fixture.detectChanges();

    expect(fixture.nativeElement.textContent).toContain("Waarde A1");
    expect(fixture.nativeElement.textContent).toContain("Waarde A2");
  });
});
