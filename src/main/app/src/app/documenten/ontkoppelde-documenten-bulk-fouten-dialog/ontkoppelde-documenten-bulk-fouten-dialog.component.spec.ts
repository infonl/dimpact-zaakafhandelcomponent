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

  async function setup() {
    await render(OntkoppeldeDocumentenBulkFoutenDialogComponent, {
      imports: [NoopAnimationsModule],
      providers: [
        provideTranslateService(),
        {
          provide: MAT_DIALOG_DATA,
          useValue: {
            actie: "koppelen",
            mislukt: [{ id: 1, titel: "fakeTitel1", foutcode: "VERGRENDELD" }],
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
