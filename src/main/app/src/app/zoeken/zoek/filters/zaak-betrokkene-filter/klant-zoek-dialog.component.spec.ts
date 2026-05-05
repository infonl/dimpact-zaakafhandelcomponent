/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { provideHttpClient } from "@angular/common/http";
import { ComponentFixture, TestBed } from "@angular/core/testing";
import { MatDialogModule, MatDialogRef } from "@angular/material/dialog";
import { NoopAnimationsModule } from "@angular/platform-browser/animations";
import { provideRouter } from "@angular/router";
import { TranslateModule } from "@ngx-translate/core";
import { provideQueryClient } from "@tanstack/angular-query-experimental";
import { testQueryClient } from "../../../../../../setupJest";
import { fromPartial } from "src/test-helpers";
import { GeneratedType } from "../../../../shared/utils/generated-types";
import { KlantZoekComponent } from "../../../../klanten/zoek/klanten/klant-zoek.component";
import { KlantZoekDialog } from "./klant-zoek-dialog.component";

describe(KlantZoekDialog.name, () => {
  let fixture: ComponentFixture<KlantZoekDialog>;
  const closeDialog = jest.fn();

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [
        KlantZoekDialog,
        KlantZoekComponent,
        NoopAnimationsModule,
        MatDialogModule,
        TranslateModule.forRoot(),
      ],
      providers: [
        provideHttpClient(),
        provideRouter([]),
        provideQueryClient(testQueryClient),
        { provide: MatDialogRef, useValue: { close: closeDialog } },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(KlantZoekDialog);
    fixture.detectChanges();
  });

  it("renders the klant-zoek component", () => {
    const klantZoek = fixture.nativeElement.querySelector("zac-klant-zoek");
    expect(klantZoek).not.toBeNull();
  });

  it("closes the dialog with the klant when klant event is emitted", () => {
    const klantZoekDebugElement = fixture.debugElement.query(
      (debugElement) => debugElement.name === "zac-klant-zoek",
    );
    const fakeKlant = fromPartial<GeneratedType<"RestPersoon">>({
      bsn: "123456789",
    });
    klantZoekDebugElement.componentInstance.klant.emit(fakeKlant);
    expect(closeDialog).toHaveBeenCalledWith(fakeKlant);
  });
});
