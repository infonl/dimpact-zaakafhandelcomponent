/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import {
  provideHttpClient,
  withInterceptorsFromDi,
  withXhr,
} from "@angular/common/http";
import {
  HttpTestingController,
  provideHttpClientTesting,
} from "@angular/common/http/testing";
import { ComponentFixture, TestBed } from "@angular/core/testing";
import { provideMomentDateAdapter } from "@angular/material-moment-adapter";
import { MatDialog } from "@angular/material/dialog";
import { MatPaginatorIntl } from "@angular/material/paginator";
import { NoopAnimationsModule } from "@angular/platform-browser/animations";
import { ActivatedRoute, provideRouter } from "@angular/router";
import { TranslateService, provideTranslateService } from "@ngx-translate/core";
import { provideQueryClient } from "@tanstack/angular-query-experimental";
import { render, screen, within } from "@testing-library/angular";
import userEvent from "@testing-library/user-event";
import { of } from "rxjs";
import { UtilService } from "src/app/core/service/util.service";
import { PaginatorTranslator } from "src/app/shared/paginator/paginator-translator";
import { GeneratedType } from "src/app/shared/utils/generated-types";
import { fromPartial } from "src/test-helpers";
import { sleep, testQueryClient } from "../../../../setupJest";
import { OntkoppeldeDocumentenService } from "../ontkoppelde-documenten.service";
import { OntkoppeldeDocumentenListComponent } from "./ontkoppelde-documenten-list.component";

const SEARCH_PARAMETERS_KEY = "ONTKOPPELDE_DOCUMENTEN_ZOEKPARAMETERS";

const UNFILTERED_REQUEST_BODY = {
  sort: "ontkoppeldOp",
  order: "desc",
  filtersType: "DetachedDocumentListParameters",
  page: 0,
  maxResults: 10,
};

const detachedDocument = fromPartial<GeneratedType<"RestDetachedDocument">>({
  id: 42,
  titel: "Aanvraag formulier",
  documentUUID: "fakeDocumentUuid",
  creatiedatum: "2026-01-01",
  zaakID: "ZAAK-001",
  ontkoppeldDoor: { username: "fakeUserId1", naam: "fakeUserName1" },
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
      canOntkoppeldeDocumentenVerwijderen: true,
    }),
  ) {
    const { fixture: renderedFixture } = await render(
      OntkoppeldeDocumentenListComponent,
      {
        imports: [NoopAnimationsModule],
        providers: [
          provideTranslateService(),
          provideRouter([]),
          {
            provide: MatPaginatorIntl,
            deps: [TranslateService],
            useFactory: (translateService: TranslateService) =>
              new PaginatorTranslator(
                translateService,
              ).getTranslatedPaginator(),
          },
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
          provideHttpClient(withXhr(), withInterceptorsFromDi()),
          provideHttpClientTesting(),
          provideMomentDateAdapter(),
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

    expect(await lastListRequestBody()).toEqual(UNFILTERED_REQUEST_BODY);
    expect(rememberedParameters()).toEqual(UNFILTERED_REQUEST_BODY);
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
        ontkoppeldDoor: { username: "fakeUserId1", naam: "fakeUserName1" },
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

  it.each([
    { filter: "titel", position: 0 },
    { filter: "zaakID", position: 1 },
    { filter: "reden", position: 2 },
  ])(
    "searches on the text that was typed in the $filter filter",
    async ({ filter, position }) => {
      await setup();
      await showDocuments([detachedDocument]);

      await user.type(
        screen.getAllByPlaceholderText("...")[position],
        "Aanvraag{Enter}",
      );

      expect(await lastListRequestBody()).toEqual({
        ...UNFILTERED_REQUEST_BODY,
        [filter]: "Aanvraag",
      });
    },
  );

  it.each([
    { filter: "creatiedatum", position: 0 },
    { filter: "ontkoppeldOp", position: 1 },
  ])(
    "searches on the dates that were typed in the $filter filter",
    async ({ filter, position }) => {
      await setup();
      await showDocuments([detachedDocument]);

      await user.type(
        screen.getAllByPlaceholderText("zoeken.filter.van")[position],
        "01-01-2024",
      );
      await user.type(
        screen.getAllByPlaceholderText("zoeken.filter.tot-en-met")[position],
        "31-01-2024",
      );
      await user.tab();

      expect(await lastListRequestBody()).toEqual({
        ...UNFILTERED_REQUEST_BODY,
        [filter]: {
          van: "2024-01-01T00:00:00.000+01:00",
          tot: "2024-01-31T00:00:00.000+01:00",
        },
      });
    },
  );

  it("searches on the person that was chosen in the ontkoppeld door filter", async () => {
    await setup();
    await showDocuments([detachedDocument]);

    await user.click(screen.getByRole("combobox", { name: "filter.alle" }));
    await user.click(screen.getByRole("option", { name: "fakeUserName1" }));

    expect(await lastListRequestBody()).toEqual({
      ...UNFILTERED_REQUEST_BODY,
      ontkoppeldDoor: { username: "fakeUserId1", naam: "fakeUserName1" },
    });
  });

  it("asks for the list sorted on the column header that was clicked", async () => {
    await setup();
    await showDocuments([detachedDocument]);

    await user.click(screen.getByRole("columnheader", { name: "titel" }));

    expect(await lastListRequestBody()).toMatchObject({
      sort: "titel",
      order: "asc",
    });
  });

  it("moves to the last page that still has documents when the current page has become empty", async () => {
    await setup();
    await showDocuments([detachedDocument], 30);
    await user.click(
      screen.getByRole("button", { name: "actie.pagina.volgende" }),
    );

    await showDocuments([], 1);

    expect(rememberedParameters().page).toBe(0);
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
    await setup(fromPartial({ canOntkoppeldeDocumentenVerwijderen: false }));
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
    expect(within(row).getByTitle("indicatie.vergrendeld")).toBeVisible();
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

  describe("selecting documents", () => {
    const secondDetachedDocument = fromPartial<
      GeneratedType<"RestDetachedDocument">
    >({
      ...detachedDocument,
      id: 43,
      titel: "Tweede formulier",
      documentUUID: "fakeDocumentUuid2",
    });

    function queryBulkButton(name: string) {
      const rowButtons = screen
        .queryAllByRole("row")
        .flatMap((row) => within(row).queryAllByRole("button", { name }));
      return (
        screen
          .queryAllByRole("button", { name })
          .find((button) => !rowButtons.includes(button)) ?? null
      );
    }

    function bulkButton(name: string) {
      const button = queryBulkButton(name);
      if (!button) throw new Error(`No bulk button "${name}"`);
      return button;
    }

    function selectAllCheckbox() {
      return screen.getByRole("checkbox", { name: /actie.alles/ });
    }

    async function showTwoDocuments(totaal = 2) {
      await setup();
      await showDocuments([detachedDocument, secondDetachedDocument], totaal);
    }

    it("offers no selection without the right to delete ontkoppelde documenten", async () => {
      await setup(fromPartial({ canOntkoppeldeDocumentenVerwijderen: false }));
      await showDocuments([detachedDocument]);

      expect(screen.queryByRole("checkbox")).toBeNull();
    });

    it("selects every document on the page with the select all checkbox", async () => {
      await showTwoDocuments();

      await user.click(selectAllCheckbox());

      expect(selectAllCheckbox()).toBeChecked();
      expect(
        within(documentRow()).getByRole("checkbox", {
          name: "actie.deselecteren",
        }),
      ).toBeChecked();
    });

    it("shows the select all checkbox as indeterminate when only some rows are selected", async () => {
      await showTwoDocuments();

      await user.click(
        within(documentRow()).getByRole("checkbox", {
          name: "actie.selecteren",
        }),
      );

      expect(selectAllCheckbox()).toBePartiallyChecked();
    });

    it("offers no bulk actions while nothing is selected", async () => {
      await showTwoDocuments();

      expect(queryBulkButton("actie.verwijderen")).toBeNull();
      expect(queryBulkButton("actie.koppelen")).toBeNull();
    });

    it("shows the number of selected documents on the bulk actions", async () => {
      await showTwoDocuments();

      await user.click(selectAllCheckbox());

      const verwijderen = bulkButton("actie.verwijderen");
      expect(within(verwijderen).getByText("2")).toBeVisible();
      expect(within(bulkButton("actie.koppelen")).getByText("2")).toBeVisible();
    });

    it("offers koppelen before verwijderen", async () => {
      await showTwoDocuments();

      await user.click(selectAllCheckbox());

      expect(
        bulkButton("actie.koppelen").compareDocumentPosition(
          bulkButton("actie.verwijderen"),
        ),
      ).toBe(Node.DOCUMENT_POSITION_FOLLOWING);
    });

    it("clears the selection when the page changes", async () => {
      await showTwoDocuments(30);
      await user.click(selectAllCheckbox());

      await user.click(
        screen.getByRole("button", { name: "actie.pagina.volgende" }),
      );
      await showDocuments([detachedDocument, secondDetachedDocument], 30);

      expect(selectAllCheckbox()).not.toBeChecked();
      expect(queryBulkButton("actie.verwijderen")).toBeNull();
    });

    it("clears the selection when the sorting changes", async () => {
      await showTwoDocuments();
      await user.click(selectAllCheckbox());

      await user.click(screen.getByRole("columnheader", { name: "titel" }));
      await showDocuments([detachedDocument, secondDetachedDocument]);

      expect(selectAllCheckbox()).not.toBeChecked();
    });

    it("clears the selection when a filter changes", async () => {
      await showTwoDocuments();
      await user.click(selectAllCheckbox());

      const [titelFilter] = screen.getAllByPlaceholderText("...");
      await user.type(titelFilter, "Aanvraag{Enter}");
      await showDocuments([detachedDocument, secondDetachedDocument]);

      expect(selectAllCheckbox()).not.toBeChecked();
    });

    it("clears a selection made while the list was reloading", async () => {
      await showTwoDocuments(30);
      await user.click(
        screen.getByRole("button", { name: "actie.pagina.volgende" }),
      );
      await user.click(selectAllCheckbox());

      await showDocuments([detachedDocument, secondDetachedDocument], 30);

      expect(selectAllCheckbox()).not.toBeChecked();
      expect(queryBulkButton("actie.verwijderen")).toBeNull();
    });

    it("disables both bulk actions while a bulk link is still running", async () => {
      await showTwoDocuments();
      await user.click(selectAllCheckbox());

      void testQueryClient
        .getMutationCache()
        .build(
          testQueryClient,
          TestBed.inject(
            OntkoppeldeDocumentenService,
          ).linkOntkoppeldeDocumenten(),
        )
        .execute({ ids: [42, 43], nieuweZaakID: "fakeZaakId" });
      await sleep();
      fixture.detectChanges();

      expect(bulkButton("actie.koppelen")).toBeDisabled();
      expect(bulkButton("actie.verwijderen")).toBeDisabled();
    });

    describe("deleting the selection", () => {
      const BULK_DELETE_URL = "/rest/ontkoppeldedocumenten/lijst/verwijderen";
      let openDialog: jest.SpyInstance;

      async function selectBothAndPressVerwijderen() {
        await showTwoDocuments();
        await user.click(selectAllCheckbox());
        openDialog = jest.spyOn(TestBed.inject(MatDialog), "open");
        await user.click(bulkButton("actie.verwijderen"));
      }

      it("asks for confirmation with the number of documents before deleting", async () => {
        await selectBothAndPressVerwijderen();

        expect(
          screen.getByText(
            "msg.ontkoppelde-documenten.bulk.verwijderen.bevestigen",
          ),
        ).toBeVisible();
        expect(openDialog.mock.calls[0][1].data._melding.args).toEqual({
          aantal: 2,
        });
        httpTestingController.expectNone(BULK_DELETE_URL);
      });

      it("titles the confirmation with the number of documents to delete", async () => {
        await selectBothAndPressVerwijderen();

        expect(
          within(screen.getByRole("dialog")).getByRole("heading"),
        ).toHaveTextContent(
          "msg.ontkoppelde-documenten.bulk.verwijderen.titel",
        );
        expect(openDialog.mock.calls[0][1].data.opties.titel.args).toEqual({
          aantal: 2,
        });
      });

      it("uses the singular text when one document is selected", async () => {
        await showTwoDocuments();
        await user.click(
          within(documentRow()).getByRole("checkbox", {
            name: "actie.selecteren",
          }),
        );

        await user.click(bulkButton("actie.verwijderen"));

        expect(
          screen.getByText(
            "msg.ontkoppelde-documenten.bulk.verwijderen.bevestigen.enkelvoud",
          ),
        ).toBeVisible();
      });

      it("deletes nothing and keeps the selection when the confirmation is cancelled", async () => {
        await selectBothAndPressVerwijderen();

        await user.click(
          within(screen.getByRole("dialog")).getByRole("button", {
            name: "actie.annuleren",
          }),
        );
        await sleep();

        httpTestingController.expectNone(BULK_DELETE_URL);
        expect(selectAllCheckbox()).toBeChecked();
      });

      it("deletes the selected documents in one request once confirmed", async () => {
        await selectBothAndPressVerwijderen();

        await user.click(
          within(screen.getByRole("dialog")).getByRole("button", {
            name: "actie.verwijderen",
          }),
        );
        await sleep();

        const request = httpTestingController.expectOne(BULK_DELETE_URL);
        expect(request.request.method).toBe("PUT");
        expect(request.request.body).toEqual({ ids: [42, 43] });
        request.flush({ verwerkt: [42, 43], mislukt: [] });
      });

      it("reports the number of deleted documents and reloads the list", async () => {
        await selectBothAndPressVerwijderen();
        await user.click(
          within(screen.getByRole("dialog")).getByRole("button", {
            name: "actie.verwijderen",
          }),
        );
        await sleep();

        httpTestingController
          .expectOne(BULK_DELETE_URL)
          .flush({ verwerkt: [42, 43], mislukt: [] });
        await sleep();

        expect(TestBed.inject(UtilService).openSnackbar).toHaveBeenCalledWith(
          "msg.ontkoppelde-documenten.bulk.verwijderen.uitgevoerd",
          { aantal: 2 },
        );
        const reloads = listRequests();
        expect(reloads.length).toBeGreaterThan(0);
        reloads.forEach((request) =>
          request.flush({ totaal: 0, resultaten: [] }),
        );
      });

      it("tells which document failed and why, while the others are deleted", async () => {
        await selectBothAndPressVerwijderen();
        await user.click(
          within(screen.getByRole("dialog")).getByRole("button", {
            name: "actie.verwijderen",
          }),
        );
        await sleep();

        httpTestingController.expectOne(BULK_DELETE_URL).flush({
          verwerkt: [42],
          mislukt: [
            { id: 43, titel: "Tweede formulier", foutcode: "VERGRENDELD" },
          ],
        });
        await sleep();
        fixture.detectChanges();

        const dialog = screen.getByRole("dialog");
        expect(within(dialog).getByText("Tweede formulier")).toBeVisible();
        expect(
          within(dialog).getByText(
            /ontkoppelde-documenten.bulk.foutcode.vergrendeld/,
          ),
        ).toBeVisible();
      });

      it("shows the delete specific text when Open Zaak refuses a deletion", async () => {
        await selectBothAndPressVerwijderen();
        await user.click(
          within(screen.getByRole("dialog")).getByRole("button", {
            name: "actie.verwijderen",
          }),
        );
        await sleep();

        httpTestingController.expectOne(BULK_DELETE_URL).flush({
          verwerkt: [42],
          mislukt: [
            { id: 43, titel: "Tweede formulier", foutcode: "OPEN_ZAAK_FOUT" },
          ],
        });
        await sleep();
        fixture.detectChanges();

        expect(
          within(screen.getByRole("dialog")).getByText(
            /ontkoppelde-documenten.bulk.foutcode.verwijderen.open-zaak-fout/,
          ),
        ).toBeVisible();
      });

      async function confirmDeletion() {
        await user.click(
          within(screen.getByRole("dialog")).getByRole("button", {
            name: "actie.verwijderen",
          }),
        );
        await sleep();
      }

      async function closeDialogs() {
        TestBed.inject(MatDialog).closeAll();
        await sleep();
        fixture.detectChanges();
      }

      it("keeps only the documents that failed selected once the list has reloaded", async () => {
        await selectBothAndPressVerwijderen();
        await confirmDeletion();

        httpTestingController.expectOne(BULK_DELETE_URL).flush({
          verwerkt: [42],
          mislukt: [
            { id: 43, titel: "Tweede formulier", foutcode: "VERGRENDELD" },
          ],
        });
        await showDocuments([{ ...secondDetachedDocument }]);
        await closeDialogs();

        expect(
          within(
            screen.getByRole("row", { name: /Tweede formulier/ }),
          ).getByRole("checkbox", { name: "actie.deselecteren" }),
        ).toBeChecked();
        expect(
          within(bulkButton("actie.verwijderen")).getByText("1"),
        ).toBeVisible();
      });

      it("reloads the list and keeps the selection when the bulk request fails", async () => {
        await selectBothAndPressVerwijderen();
        await confirmDeletion();

        httpTestingController
          .expectOne(BULK_DELETE_URL)
          .flush(null, { status: 500, statusText: "Internal Server Error" });
        await showDocuments([
          { ...detachedDocument },
          { ...secondDetachedDocument },
        ]);
        await closeDialogs();

        expect(selectAllCheckbox()).toBeChecked();
      });

      it("clears the selection when every document was deleted", async () => {
        await selectBothAndPressVerwijderen();
        await confirmDeletion();

        httpTestingController
          .expectOne(BULK_DELETE_URL)
          .flush({ verwerkt: [42, 43], mislukt: [] });
        await showDocuments([{ ...detachedDocument }]);

        expect(queryBulkButton("actie.verwijderen")).toBeNull();
      });
    });

    describe("linking the selection", () => {
      it("opens the koppelen drawer for all selected documents", async () => {
        await showTwoDocuments();
        await user.click(selectAllCheckbox());

        await user.click(bulkButton("actie.koppelen"));
        await sleep();
        fixture.detectChanges();

        expect(
          screen.getByText("informatieobject.koppelen.bulk.uitleg"),
        ).toBeVisible();
      });
    });
  });
});
