/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { TestBed } from "@angular/core/testing";
import { MatDialog } from "@angular/material/dialog";
import { TranslateModule } from "@ngx-translate/core";
import { of } from "rxjs";
import { ConfirmDialogComponent } from "../shared/confirm-dialog/confirm-dialog.component";
import { RedenDialogComponent } from "../shared/dialog/reden-dialog/reden-dialog.component";
import { DocumentDialogService } from "./document-dialog.service";

const setup = () => {
  const open = jest.fn().mockReturnValue({ afterClosed: () => of(true) });

  TestBed.configureTestingModule({
    imports: [TranslateModule.forRoot()],
    providers: [
      DocumentDialogService,
      { provide: MatDialog, useValue: { open } },
    ],
  });

  return { service: TestBed.inject(DocumentDialogService), open };
};

describe(DocumentDialogService.name, () => {
  it("ontkoppelDocument opens a multiline reden dialog with the given melding", () => {
    const { service, open } = setup();
    const callback = jest.fn();

    service.openOntkoppelDocument("een melding", callback);

    expect(open).toHaveBeenCalledWith(RedenDialogComponent, expect.anything());
    const data = open.mock.calls.at(-1)![1].data;
    expect(data.titleKey).toBe("actie.document.ontkoppelen");
    expect(data.multiline).toBe(true);
    expect(data.melding).toBe("een melding");
    expect(data.callback).toBe(callback);
  });

  describe("verwijderDocument", () => {
    it("opens a reden dialog when the document belongs to a zaak", () => {
      const { service, open } = setup();
      const deleteFn = jest.fn().mockReturnValue(of(undefined));

      service.openVerwijderDocument({
        hasZaak: true,
        documentTitel: "doc.pdf",
        delete: deleteFn,
      });

      expect(open).toHaveBeenCalledWith(
        RedenDialogComponent,
        expect.anything(),
      );
      const data = open.mock.calls.at(-1)![1].data;
      expect(data.titleKey).toBe("actie.document.verwijderen");
      expect(data.maxlength).toBe(100);

      data.callback("mijn reden");
      expect(deleteFn).toHaveBeenCalledWith("mijn reden");
    });

    it("opens a plain confirm dialog (no reden) when there is no zaak", () => {
      const { service, open } = setup();
      const deleteObservable = of(undefined);
      const deleteFn = jest.fn().mockReturnValue(deleteObservable);

      service.openVerwijderDocument({
        hasZaak: false,
        documentTitel: "doc.pdf",
        delete: deleteFn,
      });

      expect(open).toHaveBeenCalledWith(
        ConfirmDialogComponent,
        expect.anything(),
      );
      // The delete observable is created eagerly and handed to the confirm dialog.
      expect(deleteFn).toHaveBeenCalledWith();
      expect(open.mock.calls.at(-1)![1].data.observable).toBe(deleteObservable);
    });
  });
});
