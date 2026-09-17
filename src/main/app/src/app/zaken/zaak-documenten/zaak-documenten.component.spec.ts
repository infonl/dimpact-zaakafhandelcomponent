/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { provideHttpClient } from "@angular/common/http";
import {
  HttpTestingController,
  provideHttpClientTesting,
} from "@angular/common/http/testing";
import { provideZonelessChangeDetection } from "@angular/core";
import { ComponentFixture, TestBed } from "@angular/core/testing";
import { MatDialogRef } from "@angular/material/dialog";
import { NoopAnimationsModule } from "@angular/platform-browser/animations";
import { provideRouter } from "@angular/router";
import { TranslateModule } from "@ngx-translate/core";
import { provideQueryClient } from "@tanstack/angular-query-experimental";
import { render, RenderResult, screen, within } from "@testing-library/angular";
import userEvent from "@testing-library/user-event";
import { EMPTY, of } from "rxjs";
import { fromPartial } from "src/test-helpers";
import { sleep, testQueryClient } from "../../../../setupJest";
import { UtilService } from "../../core/service/util.service";
import { ObjectType } from "../../core/websocket/model/object-type";
import { Opcode } from "../../core/websocket/model/opcode";
import { ScreenEvent } from "../../core/websocket/model/screen-event";
import { ScreenEventId } from "../../core/websocket/model/screen-event-id";
import { WebsocketListener } from "../../core/websocket/model/websocket-listener";
import { WebsocketService } from "../../core/websocket/websocket.service";
import { DocumentDialogService } from "../../informatie-objecten/document-dialog.service";
import { InformatieObjectenService } from "../../informatie-objecten/informatie-objecten.service";
import { RedenDialogFormComponent } from "../../shared/dialog/reden-dialog-form/reden-dialog-form.component";
import { GeneratedType } from "../../shared/utils/generated-types";
import { ZaakDocumentenComponent } from "./zaak-documenten.component";

const LIST_URL = "/rest/informatieobjecten/informatieobjectenList";

const fakeZaak = fromPartial<GeneratedType<"RestZaak">>({
  uuid: "zaak-uuid-1",
  identificatie: "ZAAK-2024-001",
  gerelateerdeZaken: [],
});

const fakeZaakMetRelaties = fromPartial<GeneratedType<"RestZaak">>({
  uuid: "zaak-uuid-1",
  identificatie: "ZAAK-2024-001",
  gerelateerdeZaken: [fromPartial({})],
});

const fakeDocument = fromPartial<
  GeneratedType<"RestEnkelvoudigInformatieobject">
>({
  uuid: "doc-uuid-1",
  titel: "Test document",
  bestandsnaam: "test.pdf",
  formaat: "application/pdf",
  vertrouwelijkheidaanduiding: "OPENBAAR",
  rechten: { lezen: true, wijzigen: false },
  isBesluitDocument: false,
});

const fakeEditableDocument = fromPartial<
  GeneratedType<"RestEnkelvoudigInformatieobject">
>({
  uuid: "doc-uuid-2",
  titel: "Bewerkbaar document",
  bestandsnaam: "bewerkbaar.docx",
  formaat:
    "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
  vertrouwelijkheidaanduiding: "OPENBAAR",
  rechten: { lezen: true, wijzigen: true },
  isBesluitDocument: false,
});

describe(ZaakDocumentenComponent.name, () => {
  const user = userEvent.setup();

  let rendered: RenderResult<ZaakDocumentenComponent>;
  let fixture: ComponentFixture<ZaakDocumentenComponent>;
  let httpTestingController: HttpTestingController;

  const settle = async () => {
    await sleep();
    fixture.detectChanges();
    await sleep();
    // the table creates the row views in one pass and binds their cells in the next
    fixture.detectChanges();
    fixture.detectChanges();
  };

  const setup = async (
    zaak = fakeZaak,
    documents: GeneratedType<"RestEnkelvoudigInformatieobject">[] | null = [
      fakeDocument,
    ],
  ) => {
    const documentMoveToCase = jest.fn();
    const addListener = jest
      .spyOn(WebsocketService.prototype, "addListener")
      .mockReturnValue(fromPartial<WebsocketListener>({}));
    const removeListeners = jest
      .spyOn(WebsocketService.prototype, "removeListeners")
      .mockImplementation();

    rendered = await render(ZaakDocumentenComponent, {
      inputs: { zaak },
      on: { documentMoveToCase },
      imports: [NoopAnimationsModule, TranslateModule.forRoot()],
      providers: [
        provideZonelessChangeDetection(),
        provideHttpClient(),
        provideHttpClientTesting(),
        provideRouter([]),
        provideQueryClient(testQueryClient),
      ],
    });

    fixture = rendered.fixture;
    httpTestingController = TestBed.inject(HttpTestingController);

    const utilService = TestBed.inject(UtilService);
    jest.spyOn(utilService, "downloadBlobResponse").mockImplementation();

    const listRequest = httpTestingController.expectOne(LIST_URL);
    if (documents) {
      listRequest.flush(documents);
      await settle();
    }

    return {
      documentMoveToCase,
      addListener,
      removeListeners,
      listRequest,
      utilService,
    };
  };

  const flushList = async (
    documents: GeneratedType<"RestEnkelvoudigInformatieobject">[],
  ) => {
    httpTestingController.expectOne(LIST_URL).flush(documents);
    await settle();
  };

  const rerenderWith = async (zaak: GeneratedType<"RestZaak">) => {
    await rendered.rerender({ inputs: { zaak } });
    await settle();
  };

  const documentRow = (titel: string) =>
    screen.getByRole("row", { name: new RegExp(titel) });

  const notifyListener = (
    addListener: jest.SpyInstance,
    objectType: ObjectType,
    event = fromPartial<ScreenEvent>({
      objectId: fromPartial<ScreenEventId>({}),
    }),
  ) => {
    const listener = addListener.mock.calls.find(
      ([, registeredType]) => registeredType === objectType,
    );
    listener?.[3](event);
  };

  const linkedDocumentsToggle = () =>
    screen.queryByRole("switch", { name: "toonGekoppeldeZaakDocumenten" });

  const openRowMenu = async (titel: string) => {
    await user.click(
      within(documentRow(titel)).getByRole("button", {
        name: "actie.menu.openen",
      }),
    );
  };

  it("shows the documents of the zaak", async () => {
    const { listRequest } = await setup();

    expect(listRequest.request.body).toEqual(
      expect.objectContaining({ zaakUUID: "zaak-uuid-1" }),
    );
    expect(documentRow("Test document")).toBeVisible();
  });

  it("listens for document and besluit updates of the zaak", async () => {
    const { addListener } = await setup();

    expect(addListener).toHaveBeenCalledWith(
      Opcode.UPDATED,
      ObjectType.ZAAK_INFORMATIEOBJECTEN,
      fakeZaak.uuid,
      expect.any(Function),
    );
    expect(addListener).toHaveBeenCalledWith(
      Opcode.UPDATED,
      ObjectType.ZAAK_BESLUITEN,
      fakeZaak.uuid,
      expect.any(Function),
    );
  });

  it("stops listening when it is destroyed", async () => {
    const { removeListeners } = await setup();

    fixture.destroy();

    expect(removeListeners).toHaveBeenCalled();
  });

  it("listens for updates of every document in the table", async () => {
    const documents = [fakeDocument, fakeEditableDocument];
    const { addListener } = await setup(fakeZaak, documents);

    for (const { uuid } of documents) {
      expect(addListener).toHaveBeenCalledWith(
        Opcode.UPDATED,
        ObjectType.ENKELVOUDIG_INFORMATIEOBJECT,
        uuid,
        expect.any(Function),
      );
    }
  });

  it("reloads the documents when one of them is updated", async () => {
    const { addListener } = await setup();
    const documentListener = addListener.mock.calls.find(
      ([, objectType]) =>
        objectType === ObjectType.ENKELVOUDIG_INFORMATIEOBJECT,
    );

    documentListener?.[3](fromPartial<ScreenEvent>({}));
    await settle();

    expect(httpTestingController.expectOne(LIST_URL).request.body).toEqual(
      expect.objectContaining({ zaakUUID: "zaak-uuid-1" }),
    );
  });

  it("shows the document that another user linked to the zaak", async () => {
    const { addListener } = await setup();

    notifyListener(addListener, ObjectType.ZAAK_INFORMATIEOBJECTEN);
    await settle();
    httpTestingController
      .expectOne(LIST_URL)
      .flush([
        fakeDocument,
        { ...fakeEditableDocument, titel: "Nieuw document" },
      ]);
    await settle();

    expect(documentRow("Nieuw document")).toBeVisible();
  });

  it("announces the document that another user linked, by name", async () => {
    const { addListener, utilService } = await setup();
    const openSnackbarAction = jest
      .spyOn(utilService, "openSnackbarAction")
      // an empty observable models the snackbar closing without its action being used
      .mockReturnValue(EMPTY);
    jest
      .spyOn(
        TestBed.inject(InformatieObjectenService),
        "readEnkelvoudigInformatieobjectByZaakInformatieobjectUUID",
      )
      .mockReturnValue(
        of(
          fromPartial<GeneratedType<"RestEnkelvoudigInformatieobject">>({
            titel: "Aangekoppeld document",
          }),
        ),
      );

    notifyListener(
      addListener,
      ObjectType.ZAAK_INFORMATIEOBJECTEN,
      fromPartial<ScreenEvent>({
        objectId: fromPartial<ScreenEventId>({
          detail: "fakeZaakInformatieobjectUuid",
        }),
      }),
    );
    await settle();
    await flushList([fakeDocument]);

    expect(openSnackbarAction).toHaveBeenCalledWith(
      "msg.document.toegevoegd.aan.zaak",
      "actie.document.bekijken",
      { document: "Aangekoppeld document" },
      7,
    );
  });

  it("shows the document that was added to the zaak along with a besluit", async () => {
    const { addListener } = await setup();

    notifyListener(addListener, ObjectType.ZAAK_BESLUITEN);
    await settle();
    httpTestingController
      .expectOne(LIST_URL)
      .flush([
        fakeDocument,
        { ...fakeEditableDocument, titel: "Besluitdocument" },
      ]);
    await settle();

    expect(documentRow("Besluitdocument")).toBeVisible();
  });

  it("asks for the documents of related cases when the zaak has any", async () => {
    const { listRequest } = await setup(fakeZaakMetRelaties);

    expect(listRequest.request.body).toEqual(
      expect.objectContaining({ gekoppeldeZaakDocumenten: true }),
    );
  });

  it("does not ask for the documents of related cases when the zaak has none", async () => {
    const { listRequest } = await setup();

    expect(listRequest.request.body).toEqual(
      expect.objectContaining({ gekoppeldeZaakDocumenten: false }),
    );
  });

  it("starts listening for the new zaak when another zaak is shown", async () => {
    const { addListener, removeListeners } = await setup();
    jest.clearAllMocks();

    await rerenderWith(
      fromPartial<GeneratedType<"RestZaak">>({
        uuid: "zaak-uuid-2",
        gerelateerdeZaken: [],
      }),
    );

    expect(removeListeners).toHaveBeenCalled();
    expect(addListener).toHaveBeenCalledTimes(2);
    expect(httpTestingController.expectOne(LIST_URL).request.body).toEqual(
      expect.objectContaining({ zaakUUID: "zaak-uuid-2" }),
    );
  });

  it("keeps its listeners when the same zaak is pushed again", async () => {
    const { addListener, removeListeners } = await setup();
    jest.clearAllMocks();

    await rerenderWith(
      fromPartial<GeneratedType<"RestZaak">>({
        uuid: "zaak-uuid-1",
        gerelateerdeZaken: [],
      }),
    );

    expect(removeListeners).not.toHaveBeenCalled();
    expect(addListener).not.toHaveBeenCalled();
  });

  it("reloads the documents when a refreshed zaak arrives", async () => {
    await setup();

    await rerenderWith(
      fromPartial<GeneratedType<"RestZaak">>({
        uuid: "zaak-uuid-1",
        gerelateerdeZaken: [fromPartial({})],
      }),
    );

    expect(httpTestingController.match(LIST_URL).length).toBeGreaterThan(0);
    httpTestingController
      .match(LIST_URL)
      .forEach((request) => request.flush([]));
  });

  it("offers no toggle for related documents when the zaak has no related cases", async () => {
    await setup();

    expect(linkedDocumentsToggle()).toBeNull();
  });

  it("offers the related documents toggle switched on when the zaak has related cases", async () => {
    await setup(fakeZaakMetRelaties);

    expect(linkedDocumentsToggle()).toBeChecked();
  });

  it("blocks the related documents toggle while the documents are loading", async () => {
    const { listRequest } = await setup(fakeZaakMetRelaties, null);

    expect(linkedDocumentsToggle()).toBeDisabled();

    listRequest.flush([]);
    await settle();
    expect(linkedDocumentsToggle()).toBeEnabled();
  });

  it("shows a message when the zaak has no documents", async () => {
    await setup(fakeZaak, []);

    expect(screen.getByText("msg.geen.gegevens.gevonden")).toBeVisible();
  });

  it("shows the related case columns while related documents are included", async () => {
    await setup(fakeZaakMetRelaties);

    expect(
      screen.getByRole("columnheader", { name: "zaakIdentificatie" }),
    ).toBeVisible();
    expect(
      screen.getByRole("columnheader", { name: "relatieType" }),
    ).toBeVisible();
  });

  it("drops the related case columns and reloads when the toggle is switched off", async () => {
    await setup(fakeZaakMetRelaties);

    await user.click(linkedDocumentsToggle()!);
    await flushList([fakeDocument]);

    expect(
      screen.queryByRole("columnheader", { name: "zaakIdentificatie" }),
    ).toBeNull();
    expect(
      screen.queryByRole("columnheader", { name: "relatieType" }),
    ).toBeNull();
  });

  it("has no related case columns for a zaak without related cases", async () => {
    await setup();

    expect(
      screen.queryByRole("columnheader", { name: "zaakIdentificatie" }),
    ).toBeNull();
  });

  it("reloads the documents on demand", async () => {
    await setup();

    fixture.componentInstance.updateDocumentList();
    await settle();

    httpTestingController.expectOne(LIST_URL).flush([fakeDocument]);
    await settle();
  });

  describe("selecting documents for a zip download", () => {
    const zipButton = () =>
      screen.getByRole("button", { name: "actie.downloaden.zip" });

    it("cannot be downloaded while nothing is selected", async () => {
      await setup();

      expect(zipButton()).toBeDisabled();
    });

    it("allows a zip download once a document is selected", async () => {
      await setup(fakeZaak, [fakeDocument, fakeEditableDocument]);

      await user.click(
        within(documentRow("Test document")).getByRole("checkbox"),
      );

      expect(zipButton()).toBeEnabled();
      expect(
        within(documentRow("Test document")).getByRole("checkbox"),
      ).toBeChecked();
      expect(
        within(documentRow("Bewerkbaar document")).getByRole("checkbox"),
      ).not.toBeChecked();
    });

    it("deselects a document that is selected again", async () => {
      await setup();

      const checkbox = within(documentRow("Test document")).getByRole(
        "checkbox",
      );
      await user.click(checkbox);
      await user.click(checkbox);

      expect(zipButton()).toBeDisabled();
    });

    it("selects and deselects every document at once", async () => {
      await setup(fakeZaak, [fakeDocument, fakeEditableDocument]);

      const selectAll = screen.getByRole("checkbox", {
        name: "actie.alles.selecteren",
      });
      await user.click(selectAll);

      expect(
        within(documentRow("Test document")).getByRole("checkbox"),
      ).toBeChecked();
      expect(
        within(documentRow("Bewerkbaar document")).getByRole("checkbox"),
      ).toBeChecked();

      await user.click(selectAll);

      expect(zipButton()).toBeDisabled();
    });

    it("downloads the selected documents as a zip and clears the selection", async () => {
      const { utilService } = await setup();
      const getZIPDownload = jest
        .spyOn(InformatieObjectenService.prototype, "getZIPDownload")
        .mockReturnValue(of({}) as never);

      await user.click(
        within(documentRow("Test document")).getByRole("checkbox"),
      );
      await user.click(zipButton());

      expect(getZIPDownload).toHaveBeenCalledWith(["doc-uuid-1"]);
      expect(utilService.downloadBlobResponse).toHaveBeenCalledWith(
        {},
        "ZAAK-2024-001",
      );
      expect(zipButton()).toBeDisabled();
    });
  });

  describe("the actions of a document row", () => {
    it("links to the document when it may be read", async () => {
      await setup();

      expect(
        within(documentRow("Test document")).getByRole("link", {
          name: "actie.document.bekijken",
        }),
      ).toBeVisible();
    });

    it("offers to edit an office document the user may change", async () => {
      await setup(fakeZaak, [fakeEditableDocument]);

      expect(
        within(documentRow("Bewerkbaar document")).getByRole("button", {
          name: "actie.document.bewerken",
        }),
      ).toBeVisible();
    });

    it("does not offer to edit a document the user may not change", async () => {
      await setup(fakeZaak, [
        fromPartial<GeneratedType<"RestEnkelvoudigInformatieobject">>({
          ...fakeEditableDocument,
          rechten: { lezen: true, wijzigen: false },
        }),
      ]);

      expect(
        within(documentRow("Bewerkbaar document")).queryByRole("button", {
          name: "actie.document.bewerken",
        }),
      ).toBeNull();
    });

    it("does not offer to edit a document that is not an office document", async () => {
      await setup(fakeZaak, [
        fromPartial<GeneratedType<"RestEnkelvoudigInformatieobject">>({
          ...fakeDocument,
          rechten: { lezen: true, wijzigen: true },
        }),
      ]);

      expect(
        within(documentRow("Test document")).queryByRole("button", {
          name: "actie.document.bewerken",
        }),
      ).toBeNull();
    });

    it("opens the office editor for the document", async () => {
      await setup(fakeZaak, [fakeEditableDocument]);
      const editInhoud = jest
        .spyOn(
          InformatieObjectenService.prototype,
          "editEnkelvoudigInformatieObjectInhoud",
        )
        .mockReturnValue(of("https://edit-url") as never);
      const windowOpen = jest.spyOn(window, "open").mockImplementation();

      await user.click(
        within(documentRow("Bewerkbaar document")).getByRole("button", {
          name: "actie.document.bewerken",
        }),
      );

      expect(editInhoud).toHaveBeenCalledWith("doc-uuid-2", "zaak-uuid-1");
      expect(windowOpen).toHaveBeenCalledWith("https://edit-url");
    });

    it("offers a download link for the document", async () => {
      await setup();

      await openRowMenu("Test document");

      expect(
        screen.getByRole("menuitem", { name: "actie.document.downloaden" }),
      ).toHaveAttribute(
        "href",
        "/rest/informatieobjecten/informatieobject/doc-uuid-1/download",
      );
    });

    it("asks the parent to move the document to another zaak", async () => {
      const { documentMoveToCase } = await setup(fakeZaak, [
        fakeEditableDocument,
      ]);

      await openRowMenu("Bewerkbaar document");
      await user.click(
        screen.getByRole("menuitem", { name: "actie.document.verplaatsen" }),
      );

      expect(documentMoveToCase).toHaveBeenCalledWith(fakeEditableDocument);
    });

    it("asks for confirmation before unlinking the document", async () => {
      await setup(fakeZaak, [fakeEditableDocument]);
      jest
        .spyOn(
          InformatieObjectenService.prototype,
          "listZaakIdentificatiesForInformatieobject",
        )
        .mockReturnValue(of([]));
      const openOntkoppelDocument = jest
        .spyOn(TestBed.inject(DocumentDialogService), "openOntkoppelDocument")
        .mockReturnValue(
          fromPartial<MatDialogRef<RedenDialogFormComponent>>({
            afterClosed: () => of(false),
          }),
        );

      await openRowMenu("Bewerkbaar document");
      await user.click(
        screen.getByRole("menuitem", { name: "actie.document.ontkoppelen" }),
      );

      expect(openOntkoppelDocument).toHaveBeenCalled();
    });
  });

  describe("previewing a document", () => {
    it("shows and hides the preview of a previewable document", async () => {
      await setup();

      await user.click(screen.getByText("Test document"));
      fixture.detectChanges();

      expect(screen.getByTitle("Test document")).toBeVisible();

      await user.click(screen.getByText("Test document"));
      fixture.detectChanges();

      expect(screen.queryByTitle("Test document")).toBeNull();
    });

    it("shows no preview for a format that cannot be previewed", async () => {
      await setup(fakeZaak, [
        fromPartial<GeneratedType<"RestEnkelvoudigInformatieobject">>({
          ...fakeDocument,
          titel: "Archief",
          bestandsnaam: "archief.zip",
          formaat: "application/zip",
        }),
      ]);

      await user.click(screen.getByText("Archief"));
      fixture.detectChanges();

      expect(screen.queryByTitle("Archief")).toBeNull();
    });
  });

  describe("helpers without a place in the template", () => {
    it("resolves an icon for a file name", async () => {
      await setup();

      expect(fixture.componentInstance.getFileIcon("test.pdf")).toBeDefined();
    });

    it("resolves a tooltip for a file type", async () => {
      await setup();

      expect(typeof fixture.componentInstance.getFileTooltip("pdf")).toBe(
        "string",
      );
    });
  });
});
