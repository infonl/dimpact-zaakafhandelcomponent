/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { provideHttpClient } from "@angular/common/http";
import { provideZonelessChangeDetection } from "@angular/core";
import { ComponentFixture, TestBed } from "@angular/core/testing";
import { MAT_DIALOG_DATA, MatDialogRef } from "@angular/material/dialog";
import { NoopAnimationsModule } from "@angular/platform-browser/animations";
import { TranslateModule } from "@ngx-translate/core";
import { provideTanStackQuery } from "@tanstack/angular-query-experimental";
import { EMPTY, of } from "rxjs";
import { fromPartial } from "src/test-helpers";
import { sleep, testQueryClient } from "../../../../../setupJest";
import { InformatieObjectenService } from "../../../informatie-objecten/informatie-objecten.service";
import { GeneratedType } from "../../../shared/utils/generated-types";
import { ZakenService } from "../../zaken.service";
import {
  DocumentOntkoppelenDialogComponent,
  DocumentOntkoppelenDialogData,
} from "./document-ontkoppelen-dialog.component";

const fakeDocument = fromPartial<
  GeneratedType<"RestEnkelvoudigInformatieobject">
>({
  uuid: "doc-uuid-1",
  titel: "Test document",
});

const setup = (
  options: {
    data?: Partial<DocumentOntkoppelenDialogData>;
    andereZaken?: string[];
  } = {},
) => {
  const dialogRefMock = { close: jest.fn(), disableClose: false };

  TestBed.configureTestingModule({
    imports: [
      DocumentOntkoppelenDialogComponent,
      NoopAnimationsModule,
      TranslateModule.forRoot(),
    ],
    providers: [
      provideZonelessChangeDetection(),
      provideHttpClient(),
      provideTanStackQuery(testQueryClient),
      { provide: MatDialogRef, useValue: dialogRefMock },
      {
        provide: MAT_DIALOG_DATA,
        useValue: {
          zaakUuid: "zaak-uuid-1",
          zaakIdentificatie: "ZAAK-2024-001",
          document: fakeDocument,
          ...options.data,
        } satisfies DocumentOntkoppelenDialogData,
      },
    ],
  });

  const informatieObjectenService = TestBed.inject(InformatieObjectenService);
  const zakenService = TestBed.inject(ZakenService);
  jest
    .spyOn(
      informatieObjectenService,
      "listZaakIdentificatiesForInformatieobject",
    )
    .mockReturnValue(of(options.andereZaken ?? []));

  const fixture: ComponentFixture<DocumentOntkoppelenDialogComponent> =
    TestBed.createComponent(DocumentOntkoppelenDialogComponent);

  return { fixture, informatieObjectenService, zakenService };
};

describe(DocumentOntkoppelenDialogComponent.name, () => {
  afterEach(() => {
    testQueryClient.clear();
    jest.clearAllMocks();
  });

  it("renders the reden field", () => {
    const { fixture } = setup();
    fixture.detectChanges();

    expect(fixture.nativeElement.querySelector("textarea")).not.toBeNull();
  });

  it("shows the simple confirmation message when the document is only linked to this case", async () => {
    const { fixture } = setup({ andereZaken: ["ZAAK-2024-001"] });
    fixture.detectChanges();
    await fixture.whenStable();
    await sleep(100);
    fixture.detectChanges();

    expect(fixture.nativeElement.textContent).toContain(
      "msg.document.ontkoppelen.bevestigen",
    );
  });

  it("shows the multiple-cases warning when the document is linked to other cases", async () => {
    const { fixture } = setup({
      andereZaken: ["ZAAK-2024-001", "ZAAK-2024-999"],
    });
    fixture.detectChanges();
    await fixture.whenStable();
    await sleep(100);
    fixture.detectChanges();

    expect(fixture.nativeElement.textContent).toContain(
      "msg.document.ontkoppelen.meerdere.zaken.bevestigen",
    );
  });

  it("ontkoppelt the document with the entered reden", () => {
    const { fixture, zakenService } = setup();
    const ontkoppelSpy = jest
      .spyOn(zakenService, "ontkoppelInformatieObject")
      .mockReturnValue(EMPTY);
    fixture.detectChanges();

    const component = fixture.componentInstance;
    component["form"].controls.reden.setValue("Niet meer relevant");
    component["ontkoppel"]().subscribe();

    expect(ontkoppelSpy).toHaveBeenCalledWith({
      zaakUUID: "zaak-uuid-1",
      documentUUID: "doc-uuid-1",
      reden: "Niet meer relevant",
    });
  });
});
