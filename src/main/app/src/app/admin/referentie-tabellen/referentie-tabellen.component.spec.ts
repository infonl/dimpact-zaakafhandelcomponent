/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { NgClass, NgIf } from "@angular/common";
import { Component } from "@angular/core";
import { ComponentFixture, TestBed } from "@angular/core/testing";
import { MatButtonModule } from "@angular/material/button";
import { MatCardModule } from "@angular/material/card";
import { MatDialog } from "@angular/material/dialog";
import { MatIconModule } from "@angular/material/icon";
import { MatSidenavModule } from "@angular/material/sidenav";
import { MatTableDataSource, MatTableModule } from "@angular/material/table";
import { NoopAnimationsModule } from "@angular/platform-browser/animations";
import { RouterModule } from "@angular/router";
import { TranslateModule } from "@ngx-translate/core";
import { of } from "rxjs";
import { ConfiguratieService } from "../../configuratie/configuratie.service";
import { UtilService } from "../../core/service/util.service";
import { SideNavComponent } from "../../shared/side-nav/side-nav.component";
import { GeneratedType } from "../../shared/utils/generated-types";
import { ReferentieTabelService } from "../referentie-tabel.service";
import { ReferentieTabellenComponent } from "./referentie-tabellen.component";

@Component({
  templateUrl: "./referentie-tabellen.component.html",
  standalone: true,
  imports: [
    NgClass,
    NgIf,
    MatSidenavModule,
    MatCardModule,
    MatTableModule,
    MatButtonModule,
    MatIconModule,
    RouterModule,
    SideNavComponent,
    TranslateModule,
  ],
})
class TestReferentieTabellenComponent extends ReferentieTabellenComponent {
  public override laadReferentieTabellen(): void {
    super.laadReferentieTabellen();
  }

  public override verwijderReferentieTabel(
    referentieTabel: GeneratedType<"RestReferenceTable">,
  ): void {
    super.verwijderReferentieTabel(referentieTabel);
  }

  get testIsLoadingResults(): boolean {
    return this.isLoadingResults;
  }

  get testDataSource(): MatTableDataSource<
    GeneratedType<"RestReferenceTable">
  > {
    return this.dataSource;
  }
}

describe(ReferentieTabellenComponent.name, () => {
  let fixture: ComponentFixture<TestReferentieTabellenComponent>;
  let component: TestReferentieTabellenComponent;
  let utilServiceMock: Pick<UtilService, "setTitle" | "openSnackbar">;
  let referentieTabelServiceMock: Pick<
    ReferentieTabelService,
    "listReferentieTabellen" | "deleteReferentieTabel"
  >;
  let dialogMock: Pick<MatDialog, "open">;

  beforeEach(async () => {
    utilServiceMock = { setTitle: jest.fn(), openSnackbar: jest.fn() };
    referentieTabelServiceMock = {
      listReferentieTabellen: jest.fn().mockReturnValue(of([])),
      deleteReferentieTabel: jest.fn().mockReturnValue(of(null)),
    };
    dialogMock = {
      open: jest.fn().mockReturnValue({ afterClosed: () => of(false) }),
    };

    await TestBed.configureTestingModule({
      imports: [
        TestReferentieTabellenComponent,
        TranslateModule.forRoot(),
        NoopAnimationsModule,
        RouterModule.forRoot([]),
      ],
      providers: [
        { provide: UtilService, useValue: utilServiceMock },
        {
          provide: ConfiguratieService,
          useValue: {} satisfies Partial<ConfiguratieService>,
        },
        {
          provide: ReferentieTabelService,
          useValue: referentieTabelServiceMock,
        },
        { provide: MatDialog, useValue: dialogMock },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(TestReferentieTabellenComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it("should call setTitle and load tabellen on init", () => {
    expect(utilServiceMock.setTitle).toHaveBeenCalledWith(
      "title.referentietabellen",
      undefined,
    );
    expect(
      referentieTabelServiceMock.listReferentieTabellen,
    ).toHaveBeenCalled();
  });

  it("should populate dataSource after loading tabellen", () => {
    const tabellen = [
      {
        id: 1,
        code: "TABEL_1",
        naam: "Tabel 1",
        systeem: false,
        aantalWaarden: 3,
      },
    ] as GeneratedType<"RestReferenceTable">[];
    (
      referentieTabelServiceMock.listReferentieTabellen as jest.Mock
    ).mockReturnValue(of(tabellen));

    component.laadReferentieTabellen();

    expect(component.testDataSource.data).toEqual(tabellen);
  });

  it("should set isLoadingResults false after loading tabellen", () => {
    (
      referentieTabelServiceMock.listReferentieTabellen as jest.Mock
    ).mockReturnValue(of([]));

    component.laadReferentieTabellen();

    expect(component.testIsLoadingResults).toBe(false);
  });

  it("should open confirm dialog when deleting a tabel", () => {
    const tabel = {
      id: 1,
      code: "TABEL_1",
    } as GeneratedType<"RestReferenceTable">;

    component.verwijderReferentieTabel(tabel);

    expect(dialogMock.open).toHaveBeenCalled();
  });

  it("should call openSnackbar and reload after confirmed delete", () => {
    dialogMock.open = jest
      .fn()
      .mockReturnValue({ afterClosed: () => of(true) });
    const tabel = {
      id: 1,
      code: "TABEL_1",
    } as GeneratedType<"RestReferenceTable">;

    component.verwijderReferentieTabel(tabel);

    expect(utilServiceMock.openSnackbar).toHaveBeenCalledWith(
      "msg.tabel.verwijderen.uitgevoerd",
      { tabel: tabel.code },
    );
    expect(
      referentieTabelServiceMock.listReferentieTabellen,
    ).toHaveBeenCalledTimes(2);
  });
});
