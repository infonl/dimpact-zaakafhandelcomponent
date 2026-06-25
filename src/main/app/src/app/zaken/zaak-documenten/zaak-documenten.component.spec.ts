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
import { of } from "rxjs";
import { fromPartial } from "src/test-helpers";
import { UtilService } from "../../core/service/util.service";
import { WebsocketListener } from "../../core/websocket/model/websocket-listener";
import { WebsocketService } from "../../core/websocket/websocket.service";
import { InformatieObjectenService } from "../../informatie-objecten/informatie-objecten.service";
import { FileFormat } from "../../informatie-objecten/model/file-format";
import { GeneratedType } from "../../shared/utils/generated-types";
import { ZaakDocumentenComponent } from "./zaak-documenten.component";

const fakeZaak = fromPartial<GeneratedType<"RestZaak">>({
  uuid: "zaak-uuid-1",
  identificatie: "ZAAK-2024-001",
  gerelateerdeZaken: [],
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

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [
        ZaakDocumentenComponent,
        NoopAnimationsModule,
        TranslateModule.forRoot(),
      ],
      providers: [provideHttpClient(), provideRouter([])],
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

    fixture = TestBed.createComponent(ZaakDocumentenComponent);
    loader = TestbedHarnessEnvironment.loader(fixture);
    component = fixture.componentInstance;
    component.zaak = fakeZaak;
    fixture.detectChanges();

    dialog = fixture.debugElement.injector.get(MatDialog);
  });

  afterEach(() => {
    jest.clearAllMocks();
  });

  describe("initialisation", () => {
    it("registers websocket listeners on init", () => {
      expect(websocketService.addListener).toHaveBeenCalledTimes(2);
    });

    it("loads documents on init", () => {
      expect(
        informatieObjectenService.listEnkelvoudigInformatieobjecten,
      ).toHaveBeenCalledWith(
        expect.objectContaining({ zaakUUID: "zaak-uuid-1" }),
      );
    });
  });

  describe("ngOnDestroy", () => {
    it("removes all websocket listeners", () => {
      component.ngOnDestroy();
      expect(websocketService.removeListeners).toHaveBeenCalled();
    });
  });

  describe("ngOnChanges", () => {
    it("calls init with reload=true when zaak input changes (non-first change)", () => {
      const newZaak = fromPartial<GeneratedType<"RestZaak">>({
        uuid: "zaak-uuid-2",
        gerelateerdeZaken: [],
      });
      component.zaak = newZaak;
      const initSpy = jest.spyOn(component, "init");
      component.ngOnChanges({
        zaak: {
          currentValue: newZaak,
          previousValue: fakeZaak,
          firstChange: false,
          isFirstChange: () => false,
        },
      });
      expect(initSpy).toHaveBeenCalledWith(newZaak, true);
    });

    it("does NOT call init again on first change", () => {
      const initSpy = jest.spyOn(component, "init");
      component.ngOnChanges({
        zaak: {
          currentValue: fakeZaak,
          previousValue: undefined,
          firstChange: true,
          isFirstChange: () => true,
        },
      });
      expect(initSpy).not.toHaveBeenCalled();
    });
  });

  describe("heeftGerelateerdeZaken", () => {
    it("is false when zaak has no related cases", () => {
      expect(component.heeftGerelateerdeZaken).toBe(false);
    });

    it("is true when zaak has related cases", () => {
      const zaakMetRelaties = fromPartial<GeneratedType<"RestZaak">>({
        uuid: "zaak-uuid-1",
        identificatie: "ZAAK-2024-001",
        gerelateerdeZaken: [fromPartial({})],
      });
      component.zaak = zaakMetRelaties;
      component.init(zaakMetRelaties, false);
      expect(component.heeftGerelateerdeZaken).toBe(true);
    });
  });

  describe("slide toggle (gekoppelde zaak documenten)", () => {
    it("is hidden when heeftGerelateerdeZaken is false", async () => {
      fixture.detectChanges();
      const toggles = await loader.getAllHarnesses(MatSlideToggleHarness);
      expect(toggles.length).toBe(0);
    });

    it("is shown when heeftGerelateerdeZaken is true", async () => {
      component.heeftGerelateerdeZaken = true;
      fixture.detectChanges();
      const toggle = await loader.getHarness(MatSlideToggleHarness);
      expect(toggle).toBeTruthy();
    });

    it("is checked by default", async () => {
      component.heeftGerelateerdeZaken = true;
      fixture.detectChanges();
      const toggle = await loader.getHarness(MatSlideToggleHarness);
      expect(await toggle.isChecked()).toBe(true);
    });

    it("requests gekoppelde zaak documents on load because toggle defaults to true", () => {
      expect(
        informatieObjectenService.listEnkelvoudigInformatieobjecten,
      ).toHaveBeenCalledWith(
        expect.objectContaining({ gekoppeldeZaakDocumenten: true }),
      );
    });
  });

  describe("loading state", () => {
    it("shows loading message when isLoadingResults is true", () => {
      component.isLoadingResults = true;
      fixture.detectChanges();
      const text = fixture.nativeElement.textContent;
      expect(text).toContain("msg.loading");
    });

    it("shows no-data message when isLoadingResults is false and table is empty", () => {
      component.isLoadingResults = false;
      fixture.detectChanges();
      const text = fixture.nativeElement.textContent;
      expect(text).toContain("msg.geen.gegevens.gevonden");
    });
  });

  describe("toggleGekoppeldeZaakDocumenten()", () => {
    it("adds zaakIdentificatie and relatieType columns when toggle is true", () => {
      component.toonGekoppeldeZaakDocumenten.setValue(true);
      component.toggleGekoppeldeZaakDocumenten();
      expect(component.documentColumns).toContain("zaakIdentificatie");
      expect(component.documentColumns).toContain("relatieType");
    });

    it("removes zaakIdentificatie and relatieType columns when toggle is false", () => {
      component.toonGekoppeldeZaakDocumenten.setValue(false);
      component.toggleGekoppeldeZaakDocumenten();
      expect(component.documentColumns).not.toContain("zaakIdentificatie");
      expect(component.documentColumns).not.toContain("relatieType");
    });

    it("reloads documents after toggling", () => {
      jest.clearAllMocks();
      component.toonGekoppeldeZaakDocumenten.setValue(true);
      component.toggleGekoppeldeZaakDocumenten();
      expect(
        informatieObjectenService.listEnkelvoudigInformatieobjecten,
      ).toHaveBeenCalled();
    });
  });

  describe("emitDocumentMove()", () => {
    it("emits documentMoveToCase event with the given document", () => {
      const emitted: GeneratedType<"RestEnkelvoudigInformatieobject">[] = [];
      component.documentMoveToCase.subscribe((v) => emitted.push(v));
      component.emitDocumentMove(fakeDocument);
      expect(emitted).toEqual([fakeDocument]);
    });
  });

  describe("updateSelected()", () => {
    it("adds document to selection when not yet selected", () => {
      component.updateSelected(fakeDocument);
      expect(component.downloadAlsZipSelection.isSelected(fakeDocument)).toBe(
        true,
      );
    });

    it("removes document from selection when already selected", () => {
      component.downloadAlsZipSelection.select(fakeDocument);
      component.updateSelected(fakeDocument);
      expect(component.downloadAlsZipSelection.isSelected(fakeDocument)).toBe(
        false,
      );
    });
  });

  describe("updateAll()", () => {
    it("selects all documents when checkbox is checked", () => {
      component.enkelvoudigInformatieObjecten.data = [fakeDocument];
      component.updateAll({
        checked: true,
      } as Partial<MatCheckboxChange> as unknown as MatCheckboxChange);
      expect(component.downloadAlsZipSelection.isSelected(fakeDocument)).toBe(
        true,
      );
    });

    it("deselects all documents when checkbox is unchecked", () => {
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
    it("calls getZIPDownload with selected document UUIDs and clears selection", () => {
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
    it("returns true for an office document with wijzigen rights", () => {
      const doc = fromPartial<GeneratedType<"RestEnkelvoudigInformatieobject">>(
        {
          rechten: { wijzigen: true },
          formaat:
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
        },
      );
      expect(component.isBewerkenToegestaan(doc)).toBe(true);
    });

    it("returns false when wijzigen is false", () => {
      const doc = fromPartial<GeneratedType<"RestEnkelvoudigInformatieobject">>(
        {
          rechten: { wijzigen: false },
          formaat:
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
        },
      );
      expect(component.isBewerkenToegestaan(doc)).toBe(false);
    });

    it("returns false for a non-office format", () => {
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
    it("returns true for a PDF format", () => {
      expect(
        component.isPreviewBeschikbaar("application/pdf" as FileFormat),
      ).toBe(true);
    });

    it("returns false for a non-previewable format", () => {
      expect(
        component.isPreviewBeschikbaar("application/zip" as FileFormat),
      ).toBe(false);
    });
  });

  describe("getZaakUuidVanInformatieObject()", () => {
    it("returns the document's zaakUUID when present", () => {
      const doc = fromPartial<GeneratedType<"RestEnkelvoudigInformatieobject">>(
        {
          uuid: "doc-uuid-1",
        },
      );
      (doc as never as { zaakUUID: string }).zaakUUID = "other-zaak-uuid";
      expect(component["getZaakUuidVanInformatieObject"](doc as never)).toBe(
        "other-zaak-uuid",
      );
    });

    it("falls back to zaak.uuid when zaakUUID is absent", () => {
      const doc = fromPartial<GeneratedType<"RestEnkelvoudigInformatieobject">>(
        {
          uuid: "doc-uuid-1",
        },
      );
      expect(component["getZaakUuidVanInformatieObject"](doc as never)).toBe(
        "zaak-uuid-1",
      );
    });
  });

  describe("getDownloadURL()", () => {
    it("delegates to informatieObjectenService.getDownloadURL", () => {
      jest
        .spyOn(informatieObjectenService, "getDownloadURL")
        .mockReturnValue("http://download/doc-uuid-1");
      const result = component["getDownloadURL"](fakeDocument as never);
      expect(informatieObjectenService.getDownloadURL).toHaveBeenCalledWith(
        "doc-uuid-1",
      );
      expect(result).toBe("http://download/doc-uuid-1");
    });
  });

  describe("getFileIcon()", () => {
    it("delegates to FileIcon.getIconByBestandsnaam", () => {
      const result = component["getFileIcon"]("test.pdf");
      expect(result).toBeDefined();
    });
  });

  describe("getFileTooltip()", () => {
    it("returns translated file type string", () => {
      const result = component["getFileTooltip"]("pdf");
      expect(typeof result).toBe("string");
    });
  });

  describe("bewerken()", () => {
    it("calls editEnkelvoudigInformatieObjectInhoud and opens the returned URL", () => {
      jest
        .spyOn(
          informatieObjectenService,
          "editEnkelvoudigInformatieObjectInhoud",
        )
        .mockReturnValue(of("http://edit-url") as never);
      const windowOpenSpy = jest.spyOn(window, "open").mockImplementation();

      component.bewerken(fakeDocument);

      expect(
        informatieObjectenService.editEnkelvoudigInformatieObjectInhoud,
      ).toHaveBeenCalledWith("doc-uuid-1", "zaak-uuid-1");
      expect(windowOpenSpy).toHaveBeenCalledWith("http://edit-url");
    });
  });

  describe("documentOntkoppelen()", () => {
    it("opens a dialog with the document's details", () => {
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

    it("shows the bekijken link when row.rechten.lezen is true", () => {
      component.enkelvoudigInformatieObjecten.data = [docWithLezen];
      fixture.detectChanges();
      const link = fixture.nativeElement.querySelector("a[mat-icon-button]");
      expect(link).not.toBeNull();
    });

    it("shows the bewerken button when isBewerkenToegestaan returns true", () => {
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
    it("sets documentPreviewRow when a different row is selected", () => {
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

    it("clears documentPreviewRow when the same row is selected again", () => {
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
