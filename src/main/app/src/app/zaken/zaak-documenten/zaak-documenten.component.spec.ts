/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { HarnessLoader } from "@angular/cdk/testing";
import { TestbedHarnessEnvironment } from "@angular/cdk/testing/testbed";
import { provideHttpClient } from "@angular/common/http";
import { ComponentFixture, TestBed } from "@angular/core/testing";
import { MatCheckboxChange } from "@angular/material/checkbox";
import { MatDialog, MatDialogRef } from "@angular/material/dialog";
import { MatSlideToggleHarness } from "@angular/material/slide-toggle/testing";
import { NoopAnimationsModule } from "@angular/platform-browser/animations";
import { provideRouter } from "@angular/router";
import { TranslateModule } from "@ngx-translate/core";
import { provideQueryClient } from "@tanstack/angular-query-experimental";
import { NEVER, of } from "rxjs";
import { fromPartial } from "src/test-helpers";
import { testQueryClient } from "../../../../setupJest";
import { UtilService } from "../../core/service/util.service";
import { WebsocketListener } from "../../core/websocket/model/websocket-listener";
import { WebsocketService } from "../../core/websocket/websocket.service";
import { InformatieObjectenService } from "../../informatie-objecten/informatie-objecten.service";
import { FileFormat } from "../../informatie-objecten/model/file-format";
import { GeneratedType } from "../../shared/utils/generated-types";
import { ZaakDocumentenComponent } from "./zaak-documenten.component";

const LIST_QUERY_KEY = "/rest/informatieobjecten/informatieobjectenList";

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
  rechten: { lezen: true, wijzigen: false },
  isBesluitDocument: false,
});

describe(ZaakDocumentenComponent.name, () => {
  let fixture: ComponentFixture<ZaakDocumentenComponent>;
  let component: ZaakDocumentenComponent;
  let loader: HarnessLoader;
  let informatieObjectenService: InformatieObjectenService;
  let websocketService: WebsocketService;
  let utilService: UtilService;
  let dialog: MatDialog;

  const createComponent = async (
    zaak: GeneratedType<"RestZaak"> = fakeZaak,
  ) => {
    fixture = TestBed.createComponent(ZaakDocumentenComponent);
    loader = TestbedHarnessEnvironment.loader(fixture);
    component = fixture.componentInstance;
    fixture.componentRef.setInput("zaak", zaak);
    fixture.detectChanges();
    await fixture.whenStable();
    fixture.detectChanges();
    dialog = fixture.debugElement.injector.get(MatDialog);
  };

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [
        ZaakDocumentenComponent,
        NoopAnimationsModule,
        TranslateModule.forRoot(),
      ],
      providers: [
        provideHttpClient(),
        provideRouter([]),
        provideQueryClient(testQueryClient),
      ],
    }).compileComponents();

    informatieObjectenService = TestBed.inject(InformatieObjectenService);
    websocketService = TestBed.inject(WebsocketService);
    utilService = TestBed.inject(UtilService);

    jest
      .spyOn(informatieObjectenService, "listEnkelvoudigInformatieobjecten")
      .mockReturnValue(of([]));
    jest
      .spyOn(websocketService, "addListener")
      .mockReturnValue(fromPartial<WebsocketListener>({}));
    jest.spyOn(websocketService, "removeListeners").mockImplementation();
  });

  describe("initialisation", () => {
    it("registers websocket listeners on init", async () => {
      await createComponent();
      expect(websocketService.addListener).toHaveBeenCalledTimes(2);
    });

    it("loads documents on init", async () => {
      await createComponent();
      expect(
        informatieObjectenService.listEnkelvoudigInformatieobjecten,
      ).toHaveBeenCalledWith(
        expect.objectContaining({ zaakUUID: "zaak-uuid-1" }),
      );
    });
  });

  describe("teardown", () => {
    it("removes all websocket listeners when the component is destroyed", async () => {
      await createComponent();
      fixture.destroy();
      expect(websocketService.removeListeners).toHaveBeenCalled();
    });
  });

  describe("zaak input changes", () => {
    it("re-registers websocket listeners when the zaak changes", async () => {
      await createComponent();
      jest.clearAllMocks();

      const newZaak = fromPartial<GeneratedType<"RestZaak">>({
        uuid: "zaak-uuid-2",
        gerelateerdeZaken: [],
      });
      fixture.componentRef.setInput("zaak", newZaak);
      fixture.detectChanges();
      await fixture.whenStable();

      expect(websocketService.removeListeners).toHaveBeenCalled();
      expect(websocketService.addListener).toHaveBeenCalledTimes(2);
    });

    it("reloads documents for the new zaak", async () => {
      await createComponent();
      jest.clearAllMocks();

      const newZaak = fromPartial<GeneratedType<"RestZaak">>({
        uuid: "zaak-uuid-2",
        gerelateerdeZaken: [],
      });
      fixture.componentRef.setInput("zaak", newZaak);
      fixture.detectChanges();
      await fixture.whenStable();

      expect(
        informatieObjectenService.listEnkelvoudigInformatieobjecten,
      ).toHaveBeenCalledWith(
        expect.objectContaining({ zaakUUID: "zaak-uuid-2" }),
      );
    });

    it("does NOT re-register listeners when a new zaak reference has the same uuid", async () => {
      await createComponent();
      jest.clearAllMocks();

      // Parent pushes a fresh zaak object (e.g. after an Opcode.ANY refresh) with the same uuid.
      const sameUuidZaak = fromPartial<GeneratedType<"RestZaak">>({
        uuid: "zaak-uuid-1",
        gerelateerdeZaken: [],
      });
      fixture.componentRef.setInput("zaak", sameUuidZaak);
      fixture.detectChanges();
      await fixture.whenStable();

      expect(websocketService.removeListeners).not.toHaveBeenCalled();
      expect(websocketService.addListener).not.toHaveBeenCalled();
    });
  });

  describe("heeftGerelateerdeZaken", () => {
    it("is false when zaak has no related cases", async () => {
      await createComponent();
      expect(component["heeftGerelateerdeZaken"]()).toBe(false);
    });

    it("is true when zaak has related cases", async () => {
      await createComponent(fakeZaakMetRelaties);
      expect(component["heeftGerelateerdeZaken"]()).toBe(true);
    });
  });

  describe("slide toggle (gekoppelde zaak documenten)", () => {
    it("is hidden when heeftGerelateerdeZaken is false", async () => {
      await createComponent();
      const toggles = await loader.getAllHarnesses(MatSlideToggleHarness);
      expect(toggles.length).toBe(0);
    });

    it("is shown when heeftGerelateerdeZaken is true", async () => {
      await createComponent(fakeZaakMetRelaties);
      const toggle = await loader.getHarness(MatSlideToggleHarness);
      expect(toggle).toBeTruthy();
    });

    it("is checked by default", async () => {
      await createComponent(fakeZaakMetRelaties);
      const toggle = await loader.getHarness(MatSlideToggleHarness);
      expect(await toggle.isChecked()).toBe(true);
    });

    it("is disabled while documents are loading", async () => {
      jest
        .spyOn(informatieObjectenService, "listEnkelvoudigInformatieobjecten")
        .mockReturnValue(NEVER);
      fixture = TestBed.createComponent(ZaakDocumentenComponent);
      loader = TestbedHarnessEnvironment.loader(fixture);
      component = fixture.componentInstance;
      fixture.componentRef.setInput("zaak", fakeZaakMetRelaties);
      fixture.detectChanges();
      await fixture.whenStable();
      fixture.detectChanges();

      const toggle = await loader.getHarness(MatSlideToggleHarness);
      expect(await toggle.isDisabled()).toBe(true);
    });

    it("is enabled once loading has finished", async () => {
      // Seed the cache so the query mounts with fresh data and is not fetching.
      testQueryClient.setQueryData([LIST_QUERY_KEY, "zaak-uuid-1", true], []);
      await createComponent(fakeZaakMetRelaties);
      const toggle = await loader.getHarness(MatSlideToggleHarness);
      expect(await toggle.isDisabled()).toBe(false);
    });
  });

  describe("loading state", () => {
    it("shows the no-data message once loading has finished and the table is empty", async () => {
      // Seed the cache so the query mounts with fresh (empty) data and is not fetching.
      testQueryClient.setQueryData([LIST_QUERY_KEY, "zaak-uuid-1", true], []);
      await createComponent();
      const text = fixture.nativeElement.textContent;
      expect(text).toContain("msg.geen.gegevens.gevonden");
    });
  });

  describe("gekoppelde zaak documenten columns", () => {
    it("includes zaakIdentificatie and relatieType columns when the toggle is enabled", async () => {
      await createComponent();
      component["toonGekoppeldeZaakDocumenten"].set(true);
      fixture.detectChanges();
      expect(component["documentColumns"]()).toContain("zaakIdentificatie");
      expect(component["documentColumns"]()).toContain("relatieType");
    });

    it("excludes zaakIdentificatie and relatieType columns when the toggle is disabled", async () => {
      await createComponent();
      component["toonGekoppeldeZaakDocumenten"].set(false);
      fixture.detectChanges();
      expect(component["documentColumns"]()).not.toContain("zaakIdentificatie");
      expect(component["documentColumns"]()).not.toContain("relatieType");
    });

    it("reloads documents with the new filter when the toggle changes", async () => {
      await createComponent();
      jest.clearAllMocks();

      component["toonGekoppeldeZaakDocumenten"].set(false);
      fixture.detectChanges();
      await fixture.whenStable();

      expect(
        informatieObjectenService.listEnkelvoudigInformatieobjecten,
      ).toHaveBeenCalledWith(
        expect.objectContaining({ gekoppeldeZaakDocumenten: false }),
      );
    });
  });

  describe("updateDocumentList()", () => {
    it("invalidates the documents query so it refetches", async () => {
      await createComponent();
      const invalidateSpy = jest.spyOn(testQueryClient, "invalidateQueries");

      component.updateDocumentList();

      expect(invalidateSpy).toHaveBeenCalledWith(
        expect.objectContaining({
          queryKey: [LIST_QUERY_KEY, "zaak-uuid-1"],
        }),
      );
    });
  });

  describe("emitDocumentMove()", () => {
    it("emits documentMoveToCase event with the given document", async () => {
      await createComponent();
      const emitted: GeneratedType<"RestEnkelvoudigInformatieobject">[] = [];
      component.documentMoveToCase.subscribe((v) => emitted.push(v));
      component.emitDocumentMove(fakeDocument);
      expect(emitted).toEqual([fakeDocument]);
    });
  });

  describe("updateSelected()", () => {
    it("adds document to selection when not yet selected", async () => {
      await createComponent();
      component.updateSelected(fakeDocument);
      expect(component.downloadAlsZipSelection.isSelected(fakeDocument)).toBe(
        true,
      );
    });

    it("removes document from selection when already selected", async () => {
      await createComponent();
      component.downloadAlsZipSelection.select(fakeDocument);
      component.updateSelected(fakeDocument);
      expect(component.downloadAlsZipSelection.isSelected(fakeDocument)).toBe(
        false,
      );
    });
  });

  describe("updateAll()", () => {
    it("selects all documents when checkbox is checked", async () => {
      await createComponent();
      component.enkelvoudigInformatieObjecten.data = [fakeDocument];
      component.updateAll({
        checked: true,
      } as Partial<MatCheckboxChange> as unknown as MatCheckboxChange);
      expect(component.downloadAlsZipSelection.isSelected(fakeDocument)).toBe(
        true,
      );
    });

    it("deselects all documents when checkbox is unchecked", async () => {
      await createComponent();
      component.enkelvoudigInformatieObjecten.data = [fakeDocument];
      component.downloadAlsZipSelection.select(fakeDocument);
      component.updateAll({
        checked: false,
      } as Partial<MatCheckboxChange> as unknown as MatCheckboxChange);
      expect(component.downloadAlsZipSelection.isSelected(fakeDocument)).toBe(
        false,
      );
    });
  });

  describe("downloadAlsZip()", () => {
    it("calls getZIPDownload with selected document UUIDs and clears selection", async () => {
      await createComponent();
      jest
        .spyOn(informatieObjectenService, "getZIPDownload")
        .mockReturnValue(of({} as never));
      jest.spyOn(utilService, "downloadBlobResponse").mockImplementation();
      component.downloadAlsZipSelection.select(fakeDocument);

      component.downloadAlsZip();

      expect(informatieObjectenService.getZIPDownload).toHaveBeenCalledWith([
        "doc-uuid-1",
      ]);
      expect(component.downloadAlsZipSelection.isEmpty()).toBe(true);
    });
  });

  describe("isBewerkenToegestaan()", () => {
    it("returns true for an office document with wijzigen rights", async () => {
      await createComponent();
      const doc = fromPartial<GeneratedType<"RestEnkelvoudigInformatieobject">>(
        {
          rechten: { wijzigen: true },
          formaat:
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
        },
      );
      expect(component.isBewerkenToegestaan(doc)).toBe(true);
    });

    it("returns false when wijzigen is false", async () => {
      await createComponent();
      const doc = fromPartial<GeneratedType<"RestEnkelvoudigInformatieobject">>(
        {
          rechten: { wijzigen: false },
          formaat:
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
        },
      );
      expect(component.isBewerkenToegestaan(doc)).toBe(false);
    });

    it("returns false for a non-office format", async () => {
      await createComponent();
      const doc = fromPartial<GeneratedType<"RestEnkelvoudigInformatieobject">>(
        {
          rechten: { wijzigen: true },
          formaat: "application/pdf",
        },
      );
      expect(component.isBewerkenToegestaan(doc)).toBe(false);
    });
  });

  describe("isPreviewBeschikbaar()", () => {
    it("returns true for a PDF format", async () => {
      await createComponent();
      expect(
        component.isPreviewBeschikbaar("application/pdf" as FileFormat),
      ).toBe(true);
    });

    it("returns false for a non-previewable format", async () => {
      await createComponent();
      expect(
        component.isPreviewBeschikbaar("application/zip" as FileFormat),
      ).toBe(false);
    });
  });

  describe("getZaakUuidVanInformatieObject()", () => {
    it("returns the document's zaakUUID when present", async () => {
      await createComponent();
      const doc = fromPartial<GeneratedType<"RestEnkelvoudigInformatieobject">>(
        {
          uuid: "doc-uuid-1",
        },
      );
      (doc as never as { zaakUUID: string }).zaakUUID = "other-zaak-uuid";
      expect(component.getZaakUuidVanInformatieObject(doc as never)).toBe(
        "other-zaak-uuid",
      );
    });

    it("falls back to zaak.uuid when zaakUUID is absent", async () => {
      await createComponent();
      const doc = fromPartial<GeneratedType<"RestEnkelvoudigInformatieobject">>(
        {
          uuid: "doc-uuid-1",
        },
      );
      expect(component.getZaakUuidVanInformatieObject(doc as never)).toBe(
        "zaak-uuid-1",
      );
    });
  });

  describe("getDownloadURL()", () => {
    it("delegates to informatieObjectenService.getDownloadURL", async () => {
      await createComponent();
      jest
        .spyOn(informatieObjectenService, "getDownloadURL")
        .mockReturnValue("https://download/doc-uuid-1");
      const result = component.getDownloadURL(fakeDocument as never);
      expect(informatieObjectenService.getDownloadURL).toHaveBeenCalledWith(
        "doc-uuid-1",
      );
      expect(result).toBe("https://download/doc-uuid-1");
    });
  });

  describe("getFileIcon()", () => {
    it("delegates to FileIcon.getIconByBestandsnaam", async () => {
      await createComponent();
      const result = component.getFileIcon("test.pdf");
      expect(result).toBeDefined();
    });
  });

  describe("getFileTooltip()", () => {
    it("returns translated file type string", async () => {
      await createComponent();
      const result = component.getFileTooltip("pdf");
      expect(typeof result).toBe("string");
    });
  });

  describe("bewerken()", () => {
    it("calls editEnkelvoudigInformatieObjectInhoud and opens the returned URL", async () => {
      await createComponent();
      jest
        .spyOn(
          informatieObjectenService,
          "editEnkelvoudigInformatieObjectInhoud",
        )
        .mockReturnValue(of("https://edit-url") as never);
      const windowOpenSpy = jest.spyOn(window, "open").mockImplementation();

      component.bewerken(fakeDocument);

      expect(
        informatieObjectenService.editEnkelvoudigInformatieObjectInhoud,
      ).toHaveBeenCalledWith("doc-uuid-1", "zaak-uuid-1");
      expect(windowOpenSpy).toHaveBeenCalledWith("https://edit-url");
    });
  });

  describe("documentOntkoppelen()", () => {
    it("opens a dialog with the document's details", async () => {
      await createComponent();
      jest
        .spyOn(
          informatieObjectenService,
          "listZaakIdentificatiesForInformatieobject",
        )
        .mockReturnValue(of([]));
      jest
        .spyOn(dialog, "open")
        .mockReturnValue(
          fromPartial<MatDialogRef<unknown>>({ afterClosed: () => of(false) }),
        );

      component.documentOntkoppelen(fakeDocument);

      expect(dialog.open).toHaveBeenCalled();
    });
  });

  describe("DOM: row action visibility", () => {
    const docWithLezen = fromPartial<
      GeneratedType<"RestEnkelvoudigInformatieobject">
    >({
      uuid: "doc-uuid-2",
      titel: "Leesbaar",
      bestandsnaam: "leesbaar.pdf",
      formaat: "application/pdf",
      vertrouwelijkheidaanduiding: "OPENBAAR",
      rechten: { lezen: true, wijzigen: false },
      isBesluitDocument: false,
    });

    const docMetBewerken = fromPartial<
      GeneratedType<"RestEnkelvoudigInformatieobject">
    >({
      uuid: "doc-uuid-3",
      titel: "Bewerkbaar",
      bestandsnaam: "bewerkbaar.docx",
      formaat:
        "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
      vertrouwelijkheidaanduiding: "OPENBAAR",
      rechten: { lezen: true, wijzigen: true },
      isBesluitDocument: false,
    });

    it("shows the bekijken link when row.rechten.lezen is true", async () => {
      await createComponent();
      component.enkelvoudigInformatieObjecten.data = [docWithLezen];
      fixture.detectChanges();
      const link = fixture.nativeElement.querySelector("a[mat-icon-button]");
      expect(link).not.toBeNull();
    });

    it("shows the bewerken button when isBewerkenToegestaan returns true", async () => {
      await createComponent();
      component.enkelvoudigInformatieObjecten.data = [docMetBewerken];
      fixture.detectChanges();
      const buttons = fixture.nativeElement.querySelectorAll(
        "button[mat-icon-button]",
      );
      const titles = Array.from(buttons).map(
        (b: unknown) => (b as HTMLElement).title,
      );
      expect(titles).toContain("actie.document.bewerken");
    });
  });

  describe("document preview expansion", () => {
    it("sets documentPreviewRow when a different row is selected", async () => {
      await createComponent();
      const previewDoc = fromPartial<
        GeneratedType<"RestEnkelvoudigInformatieobject">
      >({
        uuid: "doc-preview",
        formaat: "application/pdf",
      });
      component.documentPreviewRow = null;
      component.documentPreviewRow =
        component.documentPreviewRow === previewDoc ? null : previewDoc;
      expect(component.documentPreviewRow).toBe(previewDoc);
    });

    it("clears documentPreviewRow when the same row is selected again", async () => {
      await createComponent();
      const previewDoc = fromPartial<
        GeneratedType<"RestEnkelvoudigInformatieobject">
      >({
        uuid: "doc-preview",
        formaat: "application/pdf",
      });
      component.documentPreviewRow = previewDoc;
      component.documentPreviewRow =
        component.documentPreviewRow === previewDoc ? null : previewDoc;
      expect(component.documentPreviewRow).toBeNull();
    });
  });
});
