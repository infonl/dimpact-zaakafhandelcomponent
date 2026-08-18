/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import {
  provideHttpClient,
  withInterceptorsFromDi,
} from "@angular/common/http";
import {
  HttpTestingController,
  provideHttpClientTesting,
} from "@angular/common/http/testing";
import { ComponentFixture, TestBed } from "@angular/core/testing";
import { provideNativeDateAdapter } from "@angular/material/core";
import { NoopAnimationsModule } from "@angular/platform-browser/animations";
import { ActivatedRoute, Router, provideRouter } from "@angular/router";
import { TranslateModule } from "@ngx-translate/core";
import { provideQueryClient } from "@tanstack/angular-query-experimental";
import { render, screen, within } from "@testing-library/angular";
import userEvent from "@testing-library/user-event";
import { of } from "rxjs";
import { UtilService } from "src/app/core/service/util.service";
import { GeneratedType } from "src/app/shared/utils/generated-types";
import { fromPartial } from "src/test-helpers";
import { sleep, testQueryClient } from "../../../../setupJest";
import { InboxProductaanvragenListComponent } from "./inbox-productaanvragen-list.component";

const SEARCH_PARAMETERS_KEY = "INBOX_PRODUCTAANVRAGEN_ZOEKPARAMETERS";

const inboxProductaanvraag = fromPartial<
  GeneratedType<"RestInboxProductaanvraag">
>({
  id: 42,
  type: "type-A",
  ontvangstdatum: "2026-01-01",
  initiatorID: "fakeInitiatorId1",
  aantalBijlagen: 2,
  aanvraagdocumentUUID: "fakeDocumentUuid",
});

describe(InboxProductaanvragenListComponent.name, () => {
  let fixture: ComponentFixture<InboxProductaanvragenListComponent>;
  let httpTestingController: HttpTestingController;

  const user = userEvent.setup({ delay: null });

  jest.setTimeout(20_000);

  async function setup(
    werklijstRechten: GeneratedType<"RestWerklijstRechten"> = fromPartial({
      inboxProductaanvragenVerwijderen: true,
    }),
  ) {
    const { fixture: renderedFixture } = await render(
      InboxProductaanvragenListComponent,
      {
        imports: [NoopAnimationsModule, TranslateModule.forRoot()],
        providers: [
          provideRouter([]),
          {
            provide: ActivatedRoute,
            useValue: fromPartial<ActivatedRoute>({
              data: of({
                tabelGegevens: {
                  aantalPerPagina: 10,
                  pageSizeOptions: [10, 25, 50],
                  werklijstRechten,
                },
              }),
            }),
          },
          provideHttpClient(withInterceptorsFromDi()),
          provideHttpClientTesting(),
          provideNativeDateAdapter(),
          provideQueryClient(testQueryClient),
        ],
      },
    );

    fixture = renderedFixture;
    httpTestingController = TestBed.inject(HttpTestingController);
    jest
      .spyOn(TestBed.inject(UtilService), "openSnackbar")
      .mockImplementation(() => undefined);
  }

  function listRequests() {
    return httpTestingController.match("/rest/inbox-productaanvragen");
  }

  async function showProductaanvragen(
    productaanvragen: GeneratedType<"RestInboxProductaanvraag">[],
    totaal = productaanvragen.length,
    filterType = ["type-A", "type-B"],
  ) {
    await sleep();
    listRequests().forEach((request) =>
      request.flush({
        totaal,
        resultaten: productaanvragen,
        filterType,
      }),
    );
    await sleep();
    // the table creates the row views in one pass and binds their cells in the next
    fixture.detectChanges();
    fixture.detectChanges();
  }

  async function lastListRequestBody() {
    await sleep();
    const requests = listRequests();
    const body = requests[requests.length - 1].request.body;
    requests.forEach((request) =>
      request.flush({ totaal: 0, resultaten: [], filterType: [] }),
    );
    await sleep();
    return body;
  }

  function rememberedParameters() {
    return JSON.parse(sessionStorage.getItem(SEARCH_PARAMETERS_KEY)!);
  }

  function productaanvraagRow() {
    return screen.getByRole("row", { name: /fakeInitiatorId1/ });
  }

  beforeEach(() => {
    sessionStorage.clear();
  });

  afterEach(() => {
    httpTestingController
      ?.match(() => true)
      .forEach((request) => request.flush([]));
  });

  it("shows a row for every productaanvraag in the list response", async () => {
    await setup();
    await showProductaanvragen([inboxProductaanvraag]);

    const row = productaanvraagRow();
    expect(within(row).getByText("type-A")).toBeVisible();
    expect(within(row).getByText("2")).toBeVisible();
  });

  it("reports that nothing was found when the list is empty", async () => {
    await setup();
    await showProductaanvragen([]);

    expect(screen.getByText("msg.geen.gegevens.gevonden")).toBeVisible();
  });

  it("asks for the first page sorted by id and remembers that", async () => {
    await setup();

    const defaultParameters = {
      sort: "id",
      order: "desc",
      page: 0,
      maxResults: 10,
    };
    expect(await lastListRequestBody()).toEqual(defaultParameters);
    expect(rememberedParameters()).toEqual(defaultParameters);
  });

  it("reuses the filters remembered from a previous visit", async () => {
    sessionStorage.setItem(
      SEARCH_PARAMETERS_KEY,
      JSON.stringify({ sort: "id", order: "desc", type: "type-B" }),
    );

    await setup();

    expect(await lastListRequestBody()).toMatchObject({ type: "type-B" });
  });

  it("filters on the type that was chosen in the type filter", async () => {
    await setup();
    await showProductaanvragen([inboxProductaanvraag]);

    await user.click(screen.getByRole("combobox", { name: "filter.-alle-" }));
    await user.click(screen.getByRole("option", { name: "type-B" }));

    expect(await lastListRequestBody()).toMatchObject({
      type: "type-B",
      page: 0,
    });
  });

  it("sorts on the column header that was clicked", async () => {
    await setup();
    await showProductaanvragen([inboxProductaanvraag]);

    await user.click(screen.getByRole("columnheader", { name: "type" }));

    expect(await lastListRequestBody()).toMatchObject({
      sort: "type",
      order: "asc",
      page: 0,
    });
  });

  it("remembers the first page for the next visit when it is destroyed", async () => {
    await setup();
    await showProductaanvragen([inboxProductaanvraag], 30);

    await user.click(screen.getByRole("button", { name: "Next page" }));
    await lastListRequestBody();
    fixture.destroy();

    expect(rememberedParameters()).toMatchObject({ page: 0 });
  });

  it("offers the search opdrachten stored for the inbox productaanvragen werklijst", async () => {
    await setup();
    await showProductaanvragen([inboxProductaanvraag]);

    expect(
      httpTestingController.expectOne(
        "/rest/gebruikersvoorkeuren/zoekopdracht/INBOX_PRODUCTAANVRAGEN",
      ).request.method,
    ).toBe("GET");
  });

  it("links to the download of the aanvraagdocument on the row", async () => {
    await setup();
    await showProductaanvragen([inboxProductaanvraag]);

    expect(
      within(productaanvraagRow()).getByRole("link", {
        name: "actie.aanvraagdocument.downloaden",
      }),
    ).toHaveAttribute(
      "href",
      "/rest/informatieobjecten/informatieobject/fakeDocumentUuid/download",
    );
  });

  it("shows a preview of the aanvraagdocument when the row is expanded", async () => {
    await setup();
    await showProductaanvragen([inboxProductaanvraag]);

    await user.click(
      within(productaanvraagRow()).getByRole("button", { name: "expand row" }),
    );
    fixture.detectChanges();

    expect(screen.getByTitle("aanvraagdocument.pdf")).toHaveAttribute(
      "data",
      "/rest/inbox-productaanvragen/fakeDocumentUuid/pdfPreview",
    );
  });

  it("hides the preview again when the expanded row is collapsed", async () => {
    await setup();
    await showProductaanvragen([inboxProductaanvraag]);

    const expandButton = within(productaanvraagRow()).getByRole("button", {
      name: "expand row",
    });
    await user.click(expandButton);
    fixture.detectChanges();
    await user.click(expandButton);
    fixture.detectChanges();

    expect(screen.queryByTitle("aanvraagdocument.pdf")).toBeNull();
  });

  it("hands the productaanvraag of the row to the zaak that is created from it", async () => {
    await setup();
    const navigateByUrl = jest
      .spyOn(TestBed.inject(Router), "navigateByUrl")
      .mockResolvedValue(true);
    await showProductaanvragen([inboxProductaanvraag]);

    await user.click(
      within(productaanvraagRow()).getByRole("button", {
        name: "actie.zaak.aanmaken",
      }),
    );

    expect(navigateByUrl).toHaveBeenCalledWith("zaken/create", {
      state: { inboxProductaanvraag },
    });
  });

  it("offers no delete without the right to remove inbox productaanvragen", async () => {
    await setup(fromPartial({ inboxProductaanvragenVerwijderen: false }));
    await showProductaanvragen([inboxProductaanvraag]);

    expect(
      within(productaanvraagRow()).queryByRole("button", {
        name: "actie.verwijderen",
      }),
    ).toBeNull();
  });

  it("asks for confirmation before deleting the productaanvraag of the row", async () => {
    await setup();
    await showProductaanvragen([inboxProductaanvraag]);

    await user.click(
      within(productaanvraagRow()).getByRole("button", {
        name: "actie.verwijderen",
      }),
    );

    expect(
      screen.getByText("msg.inboxProductaanvraag.verwijderen.bevestigen"),
    ).toBeVisible();
    httpTestingController.expectNone("/rest/inbox-productaanvragen/42");
  });

  it("deletes the productaanvraag of the row once confirmed", async () => {
    await setup();
    await showProductaanvragen([inboxProductaanvraag]);

    await user.click(
      within(productaanvraagRow()).getByRole("button", {
        name: "actie.verwijderen",
      }),
    );
    await user.click(screen.getByRole("button", { name: "actie.ja" }));
    await sleep();

    expect(
      httpTestingController.expectOne("/rest/inbox-productaanvragen/42").request
        .method,
    ).toBe("DELETE");
  });

  it("keeps the productaanvraag when the confirmation is cancelled", async () => {
    await setup();
    await showProductaanvragen([inboxProductaanvraag]);

    await user.click(
      within(productaanvraagRow()).getByRole("button", {
        name: "actie.verwijderen",
      }),
    );
    await user.click(screen.getByRole("button", { name: "actie.nee" }));
    await sleep();

    httpTestingController.expectNone("/rest/inbox-productaanvragen/42");
  });
});
