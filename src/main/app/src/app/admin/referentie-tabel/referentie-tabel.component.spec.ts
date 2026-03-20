/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import type { CdkDragDrop } from "@angular/cdk/drag-drop";
import { DragDropModule } from "@angular/cdk/drag-drop";
import { NgIf } from "@angular/common";
import { provideHttpClient } from "@angular/common/http";
import { provideHttpClientTesting } from "@angular/common/http/testing";
import { Component } from "@angular/core";
import { ComponentFixture, TestBed } from "@angular/core/testing";
import { MatButtonModule } from "@angular/material/button";
import { MatCardModule } from "@angular/material/card";
import { MatIconModule } from "@angular/material/icon";
import { MatSidenavModule } from "@angular/material/sidenav";
import { MatTableModule } from "@angular/material/table";
import { By } from "@angular/platform-browser";
import { NoopAnimationsModule } from "@angular/platform-browser/animations";
import { ActivatedRoute, provideRouter } from "@angular/router";
import { TranslateModule } from "@ngx-translate/core";
import { of } from "rxjs";
import { ConfiguratieService } from "../../configuratie/configuratie.service";
import { UtilService } from "../../core/service/util.service";
import { FoutAfhandelingService } from "../../fout-afhandeling/fout-afhandeling.service";
import { EditInputComponent } from "../../shared/edit/edit-input/edit-input.component";
import { SideNavComponent } from "../../shared/side-nav/side-nav.component";
import { GeneratedType } from "../../shared/utils/generated-types";
import { ReferentieTabelService } from "../referentie-tabel.service";
import { ReferentieTabelComponent } from "./referentie-tabel.component";

const mockTabel: GeneratedType<"RestReferenceTable"> = {
  id: 1,
  code: "TEST_CODE",
  naam: "Test tabel",
  systeem: false,
  waarden: [
    { id: 1, naam: "Waarde A", systemValue: false },
    { id: 2, naam: "Waarde B", systemValue: true },
  ],
  aantalWaarden: 2,
};

@Component({
  templateUrl: "./referentie-tabel.component.html",
  standalone: true,
  imports: [
    NgIf,
    DragDropModule,
    MatSidenavModule,
    MatCardModule,
    MatTableModule,
    MatIconModule,
    MatButtonModule,
    TranslateModule,
    SideNavComponent,
    EditInputComponent,
  ],
})
class TestReferentieTabelComponent extends ReferentieTabelComponent {}

describe(ReferentieTabelComponent.name, () => {
  let fixture: ComponentFixture<TestReferentieTabelComponent>;
  let referentieTabelService: ReferentieTabelService;
  let utilServiceMock: Pick<
    UtilService,
    "setTitle" | "openSnackbar" | "hasEditOverlay"
  >;

  beforeEach(async () => {
    utilServiceMock = {
      setTitle: jest.fn(),
      openSnackbar: jest.fn(),
      hasEditOverlay: jest.fn().mockReturnValue(false),
    };

    await TestBed.configureTestingModule({
      imports: [
        TestReferentieTabelComponent,
        NoopAnimationsModule,
        TranslateModule.forRoot(),
      ],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        provideRouter([]),
        { provide: UtilService, useValue: utilServiceMock },
        {
          provide: ConfiguratieService,
          useValue: {} satisfies Partial<ConfiguratieService>,
        },
        {
          provide: FoutAfhandelingService,
          useValue: {
            openFoutDialog: jest.fn(),
          } satisfies Pick<FoutAfhandelingService, "openFoutDialog">,
        },
        {
          provide: ActivatedRoute,
          useValue: { data: of({ tabel: mockTabel }) },
        },
      ],
    }).compileComponents();

    referentieTabelService = TestBed.inject(ReferentieTabelService);
    jest
      .spyOn(referentieTabelService, "updateReferentieTabel")
      .mockReturnValue(
        of(mockTabel) as ReturnType<
          typeof referentieTabelService.updateReferentieTabel
        >,
      );
    jest
      .spyOn(referentieTabelService, "createReferentieTabel")
      .mockReturnValue(
        of(mockTabel) as ReturnType<
          typeof referentieTabelService.createReferentieTabel
        >,
      );

    fixture = TestBed.createComponent(TestReferentieTabelComponent);
    fixture.detectChanges();
  });

  it("should call setTitle on init", () => {
    expect(utilServiceMock.setTitle).toHaveBeenCalledWith(
      "title.referentietabel",
      { tabel: "TEST_CODE" },
    );
  });

  it("should render table rows for each tabel waarde", () => {
    const rows = fixture.nativeElement.querySelectorAll("tr[mat-row]");
    expect(rows.length).toBe(2);
  });

  it("should set code field as readonly when tabel is systeem", () => {
    TestBed.inject(ActivatedRoute).data = of({
      tabel: { ...mockTabel, systeem: true },
    });
    const sistemFixture = TestBed.createComponent(TestReferentieTabelComponent);
    sistemFixture.detectChanges();
    const editInputs = sistemFixture.debugElement.queryAll(
      By.directive(EditInputComponent),
    );
    expect(editInputs[0].componentInstance.readonly).toBe(true);
    expect(editInputs[1].componentInstance.readonly).toBeFalsy();
  });

  it("should show delete button only for non-system values", () => {
    const deleteButtons =
      fixture.nativeElement.querySelectorAll("button#verwijderen");
    expect(deleteButtons.length).toBe(1);
  });

  it("should call updateReferentieTabel when adding a new waarde", () => {
    const addButton = fixture.nativeElement.querySelector(
      "button#toevoegen",
    ) as HTMLButtonElement;
    addButton.click();
    expect(referentieTabelService.updateReferentieTabel).toHaveBeenCalled();
  });

  it("should call updateReferentieTabel when deleting a waarde", () => {
    const deleteButton = fixture.nativeElement.querySelector(
      "button#verwijderen",
    ) as HTMLButtonElement;
    deleteButton.click();
    expect(referentieTabelService.updateReferentieTabel).toHaveBeenCalled();
  });

  it("should set row edit-input as readonly for system values", () => {
    const editInputs = fixture.debugElement.queryAll(
      By.directive(EditInputComponent),
    );
    const readonlyStates = editInputs.map(
      (el) => !!el.componentInstance.readonly,
    );
    // Exactly one edit-input should be readonly: the row with systemValue=true
    expect(readonlyStates.filter(Boolean)).toHaveLength(1);
  });

  it("should call updateReferentieTabel when reordering waarden", () => {
    fixture.componentInstance["moveTabelWaarde"]({
      previousIndex: 0,
      currentIndex: 1,
      container: { data: [...mockTabel.waarden!] },
    } as unknown as CdkDragDrop<GeneratedType<"RestReferenceTableValue">[]>);
    expect(referentieTabelService.updateReferentieTabel).toHaveBeenCalled();
  });
});
