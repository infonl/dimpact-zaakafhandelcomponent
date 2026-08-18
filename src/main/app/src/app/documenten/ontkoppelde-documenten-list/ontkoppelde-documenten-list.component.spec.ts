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
import { OntkoppeldeDocumentenListComponent } from "./ontkoppelde-documenten-list.component";

const SEARCH_PARAMETERS_KEY = "ONTKOPPELDE_DOCUMENTEN_ZOEKPARAMETERS";

const detachedDocument = fromPartial<GeneratedType<"RestDetachedDocument">>({
  id: 42,
  titel: "Aanvraag formulier",
  documentUUID: "fakeDocumentUuid",
  creatiedatum: "2026-01-01",
  zaakID: "ZAAK-001",
  ontkoppeldDoor: { id: "fakeUserId1", naam: "fakeUserName1" },
  ontkoppeldOp: "2026-01-02",
  reden: "Onterecht gekoppeld",
  isVergrendeld: false,
});

describe(OntkoppeldeDocumentenListComponent.name, () => {
  let fixture: ComponentFixture<OntkoppeldeDocumentenListComponent>;
  let httpTestingController: HttpTestingController;

  const user = userEvent.setup({ delay: null });

  jest.setTimeout(20_000);

  async function setup(
    werklijstRechten: GeneratedType<"RestWerklijstRechten"> = fromPartial({
      ontkoppeldeDocumentenVerwijderen: true,
    }),
  ) {
    const { fixture: renderedFixture } = await render(
      OntkoppeldeDocumentenListComponent,
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
    return httpTestingController.match("/rest/ontkoppeldedocumenten");
  }

  async function showDocuments(
    documents: GeneratedType<"RestDetachedDocument">[],
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

  function rememberedParameters() {
    return JSON.parse(sessionStorage.getItem(SEARCH_PARAMETERS_KEY)!);
  }

  function documentRow() {
    return screen.getByRole("row", { name: /Aanvraag formulier/ });
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
    await showDocuments([detachedDocument]);

    const row = documentRow();
    expect(within(row).getByText("ZAAK-001")).toBeVisible();
    expect(within(row).getByText("fakeUserName1")).toBeVisible();
    expect(within(row).getByText("Onterecht gekoppeld")).toBeVisible();
  });

  it("reports that nothing was found when the list is empty", async () => {
    await setup();
    await showDocuments([]);

    expect(screen.getByText("msg.geen.gegevens.gevonden")).toBeVisible();
  });

  it("asks for the first page sorted by ontkoppeldOp and remembers that", async () => {
    await setup();

    const defaultParameters = {
      sort: "ontkoppeldOp",
      order: "desc",
      filtersType: "DetachedDocumentListParameters",
      page: 0,
      maxResults: 10,
    };
    expect(await lastListRequestBody()).toEqual(defaultParameters);
    expect(rememberedParameters()).toEqual(defaultParameters);
  });

  it("reuses the filters remembered from a previous visit", async () => {
    sessionStorage.setItem(
      SEARCH_PARAMETERS_KEY,
      JSON.stringify({
        titel: "Aanvraag",
        filtersType: "DetachedDocumentListParameters",
      }),
    );

    await setup();

    expect(await lastListRequestBody()).toMatchObject({ titel: "Aanvraag" });
  });

  it("preselects the person remembered as the ontkoppeld door filter", async () => {
    sessionStorage.setItem(
      SEARCH_PARAMETERS_KEY,
      JSON.stringify({
        ontkoppeldDoor: { id: "fakeUserId1", naam: "fakeUserName1" },
        filtersType: "DetachedDocumentListParameters",
      }),
    );
    await setup();
    await showDocuments([detachedDocument]);
    await sleep();
    fixture.detectChanges();

    expect(
      screen.getByRole("combobox", { name: "fakeUserName1" }),
    ).toBeVisible();
  });

  it("goes back to the first page when a filter changes", async () => {
    await setup();
    await showDocuments([detachedDocument], 30);

    await user.click(
      screen.getByRole("button", { name: "actie.pagina.volgende" }),
    );
    await sleep();
    fixture.detectChanges();
    expect(screen.getByText(/11 - 20/)).toBeVisible();

    const [titelFilter] = screen.getAllByPlaceholderText("...");
    await user.type(titelFilter, "Aanvraag{Enter}");
    await sleep();
    fixture.detectChanges();

    expect(screen.getByText(/1 - 10/)).toBeVisible();
  });

  it("remembers the sorting column that was clicked, back on the first page", async () => {
    await setup();
    await showDocuments([detachedDocument], 30);

    await user.click(
      screen.getByRole("button", { name: "actie.pagina.volgende" }),
    );
    await lastListRequestBody();

    await user.click(screen.getByRole("columnheader", { name: "titel" }));

    expect(rememberedParameters()).toMatchObject({
      sort: "titel",
      order: "asc",
      page: 0,
    });
  });

  it("remembers the first page for the next visit when it is destroyed", async () => {
    await setup();
    await showDocuments([detachedDocument], 30);

    await user.click(
      screen.getByRole("button", { name: "actie.pagina.volgende" }),
    );
    await lastListRequestBody();
    fixture.destroy();

    expect(rememberedParameters()).toMatchObject({ page: 0 });
  });

  it("offers the search opdrachten stored for the ontkoppelde documenten werklijst", async () => {
    await setup();
    await showDocuments([detachedDocument]);

    expect(
      httpTestingController.expectOne(
        "/rest/gebruikersvoorkeuren/zoekopdracht/ONTKOPPELDE_DOCUMENTEN",
      ).request.method,
    ).toBe("GET");
  });

  it("links to the download of the document on the row", async () => {
    await setup();
    await showDocuments([detachedDocument]);

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
      fromPartial<GeneratedType<"RestDetachedDocument">>({
        ...detachedDocument,
        documentUUID: undefined,
      }),
    ]);

    expect(
      within(documentRow()).queryByRole("link", { name: "actie.downloaden" }),
    ).toBeNull();
  });

  it("offers no delete without the right to remove ontkoppelde documenten", async () => {
    await setup(fromPartial({ ontkoppeldeDocumentenVerwijderen: false }));
    await showDocuments([detachedDocument]);

    expect(
      within(documentRow()).queryByRole("button", {
        name: "actie.verwijderen",
      }),
    ).toBeNull();
  });

  it("marks a locked document as such instead of offering to delete it", async () => {
    await setup();
    await showDocuments([
      fromPartial<GeneratedType<"RestDetachedDocument">>({
        ...detachedDocument,
        isVergrendeld: true,
      }),
    ]);

    const row = documentRow();
    expect(
      within(row).queryByRole("button", { name: "actie.verwijderen" }),
    ).toBeNull();
    expect(within(row).getByTitle("indicatie.VERGRENDELD")).toBeVisible();
  });

  it("asks for confirmation before deleting the document of the row", async () => {
    await setup();
    await showDocuments([detachedDocument]);

    await user.click(
      within(documentRow()).getByRole("button", { name: "actie.verwijderen" }),
    );

    expect(
      screen.getByText("msg.document.verwijderen.bevestigen"),
    ).toBeVisible();
    httpTestingController.expectNone("/rest/ontkoppeldedocumenten/42");
  });

  it("deletes the document of the row once confirmed", async () => {
    await setup();
    await showDocuments([detachedDocument]);

    await user.click(
      within(documentRow()).getByRole("button", { name: "actie.verwijderen" }),
    );
    await user.click(screen.getByRole("button", { name: "actie.ja" }));
    await sleep();

    expect(
      httpTestingController.expectOne("/rest/ontkoppeldedocumenten/42").request
        .method,
    ).toBe("DELETE");
  });

  it("keeps the document when the confirmation is cancelled", async () => {
    await setup();
    await showDocuments([detachedDocument]);

    await user.click(
      within(documentRow()).getByRole("button", { name: "actie.verwijderen" }),
    );
    await user.click(screen.getByRole("button", { name: "actie.nee" }));
    await sleep();

    httpTestingController.expectNone("/rest/ontkoppeldedocumenten/42");
  });

  it("opens the koppelen drawer for the document of the row", async () => {
    await setup();
    await showDocuments([detachedDocument]);

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
