/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { Component, EventEmitter, Output } from "@angular/core";
import { ComponentFixture, TestBed } from "@angular/core/testing";
import { MatDialogRef } from "@angular/material/dialog";
import { NoopAnimationsModule } from "@angular/platform-browser/animations";
import { TranslateModule } from "@ngx-translate/core";
import { KlantZoekComponent } from "../../../../klanten/zoek/klanten/klant-zoek.component";
import { GeneratedType } from "../../../../shared/utils/generated-types";
import { KlantZoekDialog } from "./klant-zoek-dialog.component";

@Component({ selector: "zac-klant-zoek", template: "", standalone: true })
class KlantZoekStub {
  @Output() klant = new EventEmitter<
    GeneratedType<"RestBedrijf" | "RestPersoon">
  >();
}

describe(KlantZoekDialog.name, () => {
  let fixture: ComponentFixture<KlantZoekDialog>;
  let dialogRef: { close: jest.Mock };

  beforeEach(async () => {
    dialogRef = { close: jest.fn() };

    await TestBed.configureTestingModule({
      imports: [
        KlantZoekDialog,
        NoopAnimationsModule,
        TranslateModule.forRoot(),
      ],
      providers: [{ provide: MatDialogRef, useValue: dialogRef }],
    })
      .overrideComponent(KlantZoekDialog, {
        remove: { imports: [KlantZoekComponent] },
        add: { imports: [KlantZoekStub] },
      })
      .compileComponents();

    fixture = TestBed.createComponent(KlantZoekDialog);
    fixture.detectChanges();
    await fixture.whenStable();
  });

  it("renders the klant-zoek component inside dialog content", () => {
    const klantZoek = fixture.nativeElement.querySelector("zac-klant-zoek");
    expect(klantZoek).toBeTruthy();
  });

  it("closes the dialog with the selected klant when klant event fires", () => {
    const klantZoekStub = fixture.debugElement.query(
      (el) => el.componentInstance instanceof KlantZoekStub,
    ).componentInstance as KlantZoekStub;

    const mockKlant = { bsn: "123456789" } as GeneratedType<"RestPersoon">;
    klantZoekStub.klant.emit(mockKlant);

    expect(dialogRef.close).toHaveBeenCalledWith(mockKlant);
  });
});
