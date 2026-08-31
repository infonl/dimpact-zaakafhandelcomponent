/*
 * SPDX-FileCopyrightText: 2025, 2026 INFO.nl
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
import { ActivatedRoute, provideRouter } from "@angular/router";
import { TranslateModule } from "@ngx-translate/core";
import { provideQueryClient } from "@tanstack/angular-query-experimental";
import { render, screen, within } from "@testing-library/angular";
import userEvent from "@testing-library/user-event";
import { of } from "rxjs";
import { UtilService } from "src/app/core/service/util.service";
import { GeneratedType } from "src/app/shared/utils/generated-types";
import { fromPartial } from "src/test-helpers";
import { sleep, testQueryClient } from "../../../../setupJest";
import { InboxDocumentenListComponent } from "./inbox-documenten-list.component";

const SEARCH_PARAMETERS_KEY = "INBOX_DOCUMENTEN_ZOEKPARAMETERS";

const inboxDocument = fromPartial<GeneratedType<"RestInboxDocument">>({
  id: 42,
  titel: "Aanvraag formulier",
  enkelvoudiginformatieobjectUUID: "fakeDocumentUuid",
  enkelvoudiginformatieobjectID: "DOCUMENT-001",
  creatiedatum: "2026-01-01",
});

describe(InboxDocumentenListComponent.name, () => {
  let fixture: ComponentFixture<InboxDocumentenListComponent>;
  let httpTestingController: HttpTestingController;

  const user = userEvent.setup({ delay: null });

  jest.setTimeout(20_000);

  async function setup() {
    const { fixture: renderedFixture } = await render(
      InboxDocumentenListComponent,
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
    return httpTestingController.match("/rest/inboxdocumenten");
  }

  async function showDocuments(
    documents: GeneratedType<"RestInboxDocument">[],
    totaal = documents.length,
  ) {
    await sleep();
    listRequests().forEach((request) =>
      request.flush({ totaal, resultaten: documents }),
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
    requests.forEach((request) => request.flush({ totaal: 0, resultaten: [] }));
    await sleep();
    return body;
  }

  function documentRow() {
    return screen.getByRole("row", { name: /DOCUMENT-001/ });
  }

  beforeEach(() => {
    sessionStorage.clear();
  });

  afterEach(() => {
    httpTestingController
      ?.match(() => true)
      .forEach((request) => request.flush([]));
  });

  it("shows a row for every document in the list response", async () => {
    await setup();
    await showDocuments([inboxDocument]);

    expect(within(documentRow()).getByText("Aanvraag formulier")).toBeVisible();
  });

  it("reports that nothing was found when the list is empty", async () => {
    await setup();
    await showDocuments([]);

    expect(screen.getByText("msg.geen.gegevens.gevonden")).toBeVisible();
  });

  it("asks for the first page sorted by creation date and remembers that", async () => {
    await setup();

    const defaultParameters = {
      sort: "creatiedatum",
      order: "desc",
      filtersType: "InboxDocumentListParameters",
      page: 0,
      maxResults: 10,
    };
    expect(await lastListRequestBody()).toEqual(defaultParameters);
    expect(JSON.parse(sessionStorage.getItem(SEARCH_PARAMETERS_KEY)!)).toEqual(
      defaultParameters,
    );
  });

  it("reuses the filters remembered from a previous visit", async () => {
    sessionStorage.setItem(
      SEARCH_PARAMETERS_KEY,
      JSON.stringify({
        titel: "Aanvraag",
        filtersType: "InboxDocumentListParameters",
      }),
    );

    await setup();

    expect(await lastListRequestBody()).toMatchObject({ titel: "Aanvraag" });
  });

  it("searches on the title that was typed in the title filter", async () => {
    await setup();
    await showDocuments([inboxDocument]);

    const [, titelFilter] = screen.getAllByPlaceholderText("...");
    await user.type(titelFilter, "Aanvraag{Enter}");

    expect(await lastListRequestBody()).toMatchObject({
      titel: "Aanvraag",
      page: 0,
    });
  });

  it("goes back to the first page when the sorting column changes", async () => {
    await setup();
    await showDocuments([inboxDocument], 30);

    await user.click(
      screen.getByRole("button", { name: "actie.pagina.volgende" }),
    );
    expect(await lastListRequestBody()).toMatchObject({ page: 1 });

    await user.click(screen.getByRole("columnheader", { name: "titel" }));

    expect(await lastListRequestBody()).toMatchObject({
      sort: "titel",
      order: "asc",
      page: 0,
    });
  });

  it("remembers the first page for the next visit when it is destroyed", async () => {
    await setup();
    await showDocuments([inboxDocument], 30);

    await user.click(
      screen.getByRole("button", { name: "actie.pagina.volgende" }),
    );
    await lastListRequestBody();
    fixture.destroy();

    expect(
      JSON.parse(sessionStorage.getItem(SEARCH_PARAMETERS_KEY)!),
    ).toMatchObject({ page: 0 });
  });

  it("offers the search opdrachten stored for the inbox documenten werklijst", async () => {
    await setup();
    await showDocuments([inboxDocument]);

    expect(
      httpTestingController.expectOne(
        "/rest/gebruikersvoorkeuren/zoekopdracht/INBOX_DOCUMENTEN",
      ).request.method,
    ).toBe("GET");
  });

  it("links to the download of the document on the row", async () => {
    await setup();
    await showDocuments([inboxDocument]);

    expect(
      within(documentRow()).getByRole("link", { name: "actie.downloaden" }),
    ).toHaveAttribute(
      "href",
      "/rest/informatieobjecten/informatieobject/fakeDocumentUuid/download",
    );
  });

  it("offers no download for a document without an informatieobject", async () => {
    await setup();
    await showDocuments([
      fromPartial<GeneratedType<"RestInboxDocument">>({
        ...inboxDocument,
        enkelvoudiginformatieobjectUUID: undefined,
      }),
    ]);

    expect(
      within(documentRow()).queryByRole("link", { name: "actie.downloaden" }),
    ).toBeNull();
  });

  it("asks for confirmation before deleting the document of the row", async () => {
    await setup();
    await showDocuments([inboxDocument]);

    await user.click(
      within(documentRow()).getByRole("button", { name: "actie.verwijderen" }),
    );

    expect(
      screen.getByText("msg.document.verwijderen.bevestigen"),
    ).toBeVisible();
    httpTestingController.expectNone("/rest/inboxdocumenten/42");
  });

  it("deletes the document of the row once confirmed", async () => {
    await setup();
    await showDocuments([inboxDocument]);

    await user.click(
      within(documentRow()).getByRole("button", { name: "actie.verwijderen" }),
    );
    await user.click(screen.getByRole("button", { name: "actie.ja" }));
    await sleep();

    expect(
      httpTestingController.expectOne("/rest/inboxdocumenten/42").request
        .method,
    ).toBe("DELETE");
  });

  it("keeps the document when the confirmation is cancelled", async () => {
    await setup();
    await showDocuments([inboxDocument]);

    await user.click(
      within(documentRow()).getByRole("button", { name: "actie.verwijderen" }),
    );
    await user.click(screen.getByRole("button", { name: "actie.nee" }));
    await sleep();

    httpTestingController.expectNone("/rest/inboxdocumenten/42");
  });

  it("opens the koppelen drawer for the document of the row", async () => {
    await setup();
    await showDocuments([inboxDocument]);

    await user.click(
      within(documentRow()).getByRole("button", {
        name: "actie.document.koppelen",
      }),
    );
    await sleep();
    fixture.detectChanges();

    expect(screen.getByText("informatieobject.koppelen.uitleg")).toBeVisible();
  });
});
