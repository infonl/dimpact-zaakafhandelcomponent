/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { MAT_DIALOG_DATA, MatDialogRef } from "@angular/material/dialog";
import { NoopAnimationsModule } from "@angular/platform-browser/animations";
import { provideTranslateService } from "@ngx-translate/core";
import { render, screen } from "@testing-library/angular";
import userEvent from "@testing-library/user-event";
import { fromPartial } from "../../../test-helpers";
import {
  OntkoppeldeDocumentenBulkFoutenDialogComponent,
  OntkoppeldeDocumentenBulkFoutenDialogData,
} from "./ontkoppelde-documenten-bulk-fouten-dialog.component";

describe(OntkoppeldeDocumentenBulkFoutenDialogComponent.name, () => {
  const dialogRef = fromPartial<
    MatDialogRef<OntkoppeldeDocumentenBulkFoutenDialogComponent>
  >({ close: jest.fn() });

  async function setup(
    data: Partial<OntkoppeldeDocumentenBulkFoutenDialogData> = {},
  ) {
    await render(OntkoppeldeDocumentenBulkFoutenDialogComponent, {
      imports: [NoopAnimationsModule],
      providers: [
        provideTranslateService(),
        {
          provide: MAT_DIALOG_DATA,
          useValue: {
            actie: "koppelen",
            nietsVerwerkt: false,
            mislukt: [{ id: 1, titel: "fakeTitel1", foutcode: "VERGRENDELD" }],
            ...data,
          } satisfies OntkoppeldeDocumentenBulkFoutenDialogData,
        },
        { provide: MatDialogRef, useValue: dialogRef },
      ],
    });
  }

  beforeEach(() => jest.mocked(dialogRef.close).mockClear());

  it("titles the dialog after the action that partly failed", async () => {
    await setup();

    expect(screen.getByRole("heading")).toHaveTextContent(
      "ontkoppelde-documenten.bulk.fouten.koppelen.titel",
    );
  });

  it("says that nothing was processed when every document failed", async () => {
    await setup({ nietsVerwerkt: true });

    expect(screen.getByRole("heading")).toHaveTextContent(
      "ontkoppelde-documenten.bulk.fouten.koppelen.titel.niets-verwerkt",
    );
  });

  it("tells that Open Zaak is unavailable instead of blaming the document type", async () => {
    await setup({
      mislukt: [
        { id: 1, titel: "fakeTitel1", foutcode: "OPEN_ZAAK_NIET_BESCHIKBAAR" },
      ],
    });

    expect(screen.getByRole("listitem")).toHaveTextContent(
      "fakeTitel1: ontkoppelde-documenten.bulk.foutcode.open-zaak-niet-beschikbaar",
    );
  });

  it("tells that a document no longer exists in Open Zaak instead of saying that it left the list", async () => {
    await setup({
      mislukt: [
        { id: 1, titel: "fakeTitel1", foutcode: "ONTBREEKT_IN_OPEN_ZAAK" },
      ],
    });

    expect(screen.getByRole("listitem")).toHaveTextContent(
      "fakeTitel1: ontkoppelde-documenten.bulk.foutcode.ontbreekt-in-open-zaak",
    );
  });

  it("lists each failed document with its reason", async () => {
    await setup();

    expect(screen.getByRole("listitem")).toHaveTextContent(
      "fakeTitel1: ontkoppelde-documenten.bulk.foutcode.vergrendeld",
    );
  });

  it("closes from the cross in the title bar, like the other dialogs", async () => {
    await setup();

    const [crossInTitleBar] = screen.getAllByRole("button", {
      name: "actie.sluiten",
    });

    await userEvent.setup().click(crossInTitleBar);

    expect(dialogRef.close).toHaveBeenCalled();
  });
});
