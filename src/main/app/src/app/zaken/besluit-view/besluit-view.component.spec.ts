/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { provideHttpClient } from "@angular/common/http";
import { provideHttpClientTesting } from "@angular/common/http/testing";
import { provideMomentDateAdapter } from "@angular/material-moment-adapter";
import { MatDialog, MatDialogRef } from "@angular/material/dialog";
import { NoopAnimationsModule } from "@angular/platform-browser/animations";
import { TranslateModule } from "@ngx-translate/core";
import { provideQueryClient } from "@tanstack/angular-query-experimental";
import { render, screen } from "@testing-library/angular";
import userEvent from "@testing-library/user-event";
import { of } from "rxjs";
import { fromPartial } from "src/test-helpers";
import { testQueryClient } from "../../../../setupJest";
import { GeneratedType } from "../../shared/utils/generated-types";
import { ZakenService } from "../zaken.service";
import { BesluitIntrekkenDialogComponent } from "./besluit-intrekken-dialog/besluit-intrekken-dialog.component";
import { BesluitViewComponent } from "./besluit-view.component";

const makeBesluit = (fields: Partial<GeneratedType<"RestBesluit">> = {}) =>
  fromPartial<GeneratedType<"RestBesluit">>({
    uuid: "besluit-uuid-1",
    identificatie: "BESLUIT-001",
    besluittype: fromPartial<GeneratedType<"RestBesluitType">>({
      naam: "Besluittype 1",
      publication: { enabled: false },
    }),
    ingangsdatum: "2026-01-01",
    vervaldatum: "2026-12-31",
    toelichting: "Een toelichting",
    isIngetrokken: false,
    informatieobjecten: [],
    ...fields,
  });

describe(BesluitViewComponent.name, () => {
  const user = userEvent.setup();

  let dialogOpen: jest.SpyInstance;

  const setup = async (
    besluiten: GeneratedType<"RestBesluit">[] = [makeBesluit()],
    readonly = false,
  ) => {
    const listBesluitHistorie = jest
      .spyOn(ZakenService.prototype, "listBesluitHistorie")
      .mockReturnValue(of([]));
    dialogOpen = jest.spyOn(MatDialog.prototype, "open").mockReturnValue(
      fromPartial<MatDialogRef<unknown>>({
        afterClosed: () => of(undefined),
      }),
    );
    const besluitWijzigen = jest.fn();

    const { fixture } = await render(BesluitViewComponent, {
      inputs: { besluiten, readonly },
      on: { besluitWijzigen },
      imports: [TranslateModule.forRoot(), NoopAnimationsModule],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        provideQueryClient(testQueryClient),
        provideMomentDateAdapter(),
      ],
    });

    // the documents table creates its row views in one pass and binds the cells in the next
    fixture.detectChanges();

    return { fixture, listBesluitHistorie, besluitWijzigen };
  };

  const panelHeader = (identificatie: string) =>
    screen.getByRole("button", { name: new RegExp(identificatie) });

  it("shows the besluit fields as read-only text", async () => {
    await setup();

    expect(screen.getByText("Besluittype 1")).toBeVisible();
    expect(screen.getByText("Een toelichting")).toBeVisible();
  });

  it("lists the linked documents without offering to change the selection", async () => {
    await setup([
      makeBesluit({
        informatieobjecten: [
          fromPartial<GeneratedType<"RestEnkelvoudigInformatieobject">>({
            uuid: "doc-1",
            titel: "Document 1",
            bestandsnaam: "document-1.pdf",
          }),
        ],
      }),
    ]);

    expect(screen.getByRole("row", { name: /Document 1/ })).toBeVisible();
    expect(screen.queryByRole("checkbox")).toBeNull();
  });

  it("offers to edit and to withdraw an open besluit", async () => {
    await setup();

    expect(
      screen.getByRole("button", { name: "actie.besluit.wijzigen" }),
    ).toBeVisible();
    expect(
      screen.getByRole("button", { name: "actie.besluit.intrekken" }),
    ).toBeVisible();
  });

  it("hides the edit and withdraw actions when the view is read-only", async () => {
    await setup([makeBesluit()], true);

    expect(
      screen.queryByRole("button", { name: "actie.besluit.wijzigen" }),
    ).toBeNull();
    expect(
      screen.queryByRole("button", { name: "actie.besluit.intrekken" }),
    ).toBeNull();
  });

  it("hides the edit and withdraw actions for a besluit that is already withdrawn", async () => {
    await setup([makeBesluit({ isIngetrokken: true })]);

    expect(
      screen.queryByRole("button", { name: "actie.besluit.wijzigen" }),
    ).toBeNull();
    expect(
      screen.queryByRole("button", { name: "actie.besluit.intrekken" }),
    ).toBeNull();
  });

  it("opens the intrekken dialog for the besluit of the panel it was clicked on", async () => {
    const besluit = makeBesluit();
    await setup([besluit]);

    await user.click(
      screen.getByRole("button", { name: "actie.besluit.intrekken" }),
    );

    expect(dialogOpen).toHaveBeenCalledWith(BesluitIntrekkenDialogComponent, {
      data: besluit,
    });
  });

  it("shows a panel for every besluit with only the first one opened", async () => {
    await setup([
      makeBesluit({ uuid: "besluit-uuid-1", identificatie: "BESLUIT-001" }),
      makeBesluit({ uuid: "besluit-uuid-2", identificatie: "BESLUIT-002" }),
    ]);

    expect(panelHeader("BESLUIT-001")).toHaveAttribute("aria-expanded", "true");
    expect(panelHeader("BESLUIT-002")).toHaveAttribute(
      "aria-expanded",
      "false",
    );
  });

  it("shows the besluiten of a changed list", async () => {
    const { fixture } = await setup([
      makeBesluit({ uuid: "besluit-uuid-1", identificatie: "BESLUIT-001" }),
    ]);

    fixture.componentRef.setInput("besluiten", [
      makeBesluit({ uuid: "besluit-uuid-1", identificatie: "BESLUIT-001" }),
      makeBesluit({ uuid: "besluit-uuid-2", identificatie: "BESLUIT-002" }),
    ]);
    fixture.detectChanges();

    expect(panelHeader("BESLUIT-002")).toBeVisible();
  });

  it("loads the history of the first besluit", async () => {
    const { listBesluitHistorie } = await setup([
      makeBesluit({ uuid: "besluit-uuid-1" }),
      makeBesluit({ uuid: "besluit-uuid-2", identificatie: "BESLUIT-002" }),
    ]);

    expect(listBesluitHistorie).toHaveBeenCalledWith("besluit-uuid-1");
    expect(listBesluitHistorie).not.toHaveBeenCalledWith("besluit-uuid-2");
  });

  it("reloads the history of an opened besluit when the besluiten change", async () => {
    const { fixture, listBesluitHistorie } = await setup();
    listBesluitHistorie.mockReturnValue(
      of([
        fromPartial<GeneratedType<"HistoryLine">>({
          attribuutLabel: "fakeAttribuutLabel",
          door: "fakeGebruiker",
        }),
      ]),
    );

    fixture.componentRef.setInput("besluiten", [
      makeBesluit({ toelichting: "Een gewijzigde toelichting" }),
    ]);
    fixture.detectChanges();
    await user.click(screen.getByRole("tab", { name: /historie/ }));
    fixture.detectChanges();
    fixture.detectChanges();

    expect(listBesluitHistorie).toHaveBeenCalledTimes(2);
    expect(
      screen.getByRole("row", { name: /fakeAttribuutLabel/ }),
    ).toBeVisible();
  });

  it("hides the edit and withdraw actions once the view becomes read-only", async () => {
    const { fixture } = await setup([makeBesluit()], false);

    fixture.componentRef.setInput("readonly", true);
    fixture.detectChanges();

    expect(
      screen.queryByRole("button", { name: "actie.besluit.wijzigen" }),
    ).toBeNull();
    expect(
      screen.queryByRole("button", { name: "actie.besluit.intrekken" }),
    ).toBeNull();
  });

  it("offers the edit and withdraw actions once the view is no longer read-only", async () => {
    const { fixture } = await setup([makeBesluit()], true);

    fixture.componentRef.setInput("readonly", false);
    fixture.detectChanges();

    expect(
      screen.getByRole("button", { name: "actie.besluit.wijzigen" }),
    ).toBeVisible();
    expect(
      screen.getByRole("button", { name: "actie.besluit.intrekken" }),
    ).toBeVisible();
  });

  it("asks to edit the besluit of the panel it was clicked on", async () => {
    const besluit = makeBesluit();
    const { besluitWijzigen } = await setup([besluit]);

    await user.click(
      screen.getByRole("button", { name: "actie.besluit.wijzigen" }),
    );

    expect(besluitWijzigen).toHaveBeenCalledWith(besluit);
  });
});
