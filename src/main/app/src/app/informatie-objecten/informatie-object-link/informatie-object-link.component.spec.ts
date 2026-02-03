/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { HarnessLoader } from "@angular/cdk/testing";
import { TestbedHarnessEnvironment } from "@angular/cdk/testing/testbed";
import { provideHttpClient } from "@angular/common/http";
import { provideHttpClientTesting } from "@angular/common/http/testing";
import { ComponentRef } from "@angular/core";
import { ComponentFixture, TestBed } from "@angular/core/testing";
import { FormsModule, ReactiveFormsModule } from "@angular/forms";
import { MatButtonHarness } from "@angular/material/button/testing";
import { MatInputHarness } from "@angular/material/input/testing";
import { MatDrawer } from "@angular/material/sidenav";
import { MatTableHarness } from "@angular/material/table/testing";
import { NoopAnimationsModule } from "@angular/platform-browser/animations";
import { TranslateModule, TranslateService } from "@ngx-translate/core";
import { fromPartial } from "@total-typescript/shoehorn";
import { of, throwError } from "rxjs";
import { Response } from "../../shared/http/http-client";
import { UtilService } from "../../core/service/util.service";
import { MaterialFormBuilderModule } from "../../shared/material-form-builder/material-form-builder.module";
import { MaterialModule } from "../../shared/material/material.module";
import { PipesModule } from "../../shared/pipes/pipes.module";
import { GeneratedType } from "../../shared/utils/generated-types";
import {
  LINKABLE_ZAKEN_PAGINATION_SIZE,
  ZoekenService,
} from "../../zoeken/zoeken.service";
import { InformatieObjectenService } from "../informatie-objecten.service";
import { InformatieObjectLinkComponent } from "./informatie-object-link.component";

describe(InformatieObjectLinkComponent.name, () => {
  let component: InformatieObjectLinkComponent;
  let componentRef: ComponentRef<InformatieObjectLinkComponent>;
  let fixture: ComponentFixture<typeof component>;
  let loader: HarnessLoader;
  let zoekenService: ZoekenService;
  let informatieObjectenService: InformatieObjectenService;
  let utilService: UtilService;
  let translateService: TranslateService;

  const mockSideNav = fromPartial<MatDrawer>({
    close: jest.fn().mockReturnValue(Promise.resolve()),
  });

  const mockInfoObjectRESTOntkoppeldDocument = fromPartial<
    GeneratedType<"RESTOntkoppeldDocument">
  >({
    documentUUID: "doc-uuid-123",
    documentID: "DOC-001",
    titel: "Test Document",
    informatieobjectTypeUUID: "info-type-uuid-456",
  });

  const mockInfoObjectRESTInboxDocument = fromPartial<
    GeneratedType<"RESTInboxDocument">
  >({
    enkelvoudiginformatieobjectUUID: "inbox-uuid-789",
    enkelvoudiginformatieobjectID: "inbox-doc-789",
    titel: "Inbox Document",
    informatieobjectTypeUUID: "info-type-uuid-456",
  });

  const mockInfoObjectRestEnkelvoudigInformatieobject = fromPartial<
    GeneratedType<"RestEnkelvoudigInformatieobject">
  >({
    uuid: "enkelvoudig-uuid-999",
    titel: "Enkelvoudig Document",
    informatieobjectTypeUUID: "info-type-uuid-456",
  });

  const mockCaseLinkSearchResult: {
    resultaten: GeneratedType<"RestZaakKoppelenZoekObject">[];
    totaal: number;
    filters: Record<string, unknown>;
  } = {
    resultaten: [
      fromPartial<GeneratedType<"RestZaakKoppelenZoekObject">>({
        identificatie: "ZAAK-001",
        zaaktypeOmschrijving: "Type A",
        statustypeOmschrijving: "In behandeling",
        omschrijving: "Zaak omschrijving 1",
        isKoppelbaar: true,
      }),
      fromPartial<GeneratedType<"RestZaakKoppelenZoekObject">>({
        identificatie: "ZAAK-002",
        zaaktypeOmschrijving: "Type B",
        statustypeOmschrijving: "Afgerond",
        omschrijving: "Zaak omschrijving 2",
        isKoppelbaar: false,
      }),
      fromPartial<GeneratedType<"RestZaakKoppelenZoekObject">>({
        identificatie: "SOURCE-ZAAK",
        zaaktypeOmschrijving: "Type C",
        statustypeOmschrijving: "Open",
        omschrijving: "Source zaak (same as source)",
        isKoppelbaar: true,
      }),
    ],
    totaal: 3,
    filters: {},
  };

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      declarations: [InformatieObjectLinkComponent],
      imports: [
        FormsModule,
        ReactiveFormsModule,
        MaterialModule,
        TranslateModule.forRoot(),
        MaterialFormBuilderModule,
        PipesModule,
        NoopAnimationsModule,
      ],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        {
          provide: MatDrawer,
          useValue: mockSideNav,
        },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(InformatieObjectLinkComponent);
    component = fixture.componentInstance;
    componentRef = fixture.componentRef;
    loader = TestbedHarnessEnvironment.loader(fixture);

    zoekenService = TestBed.inject(ZoekenService);
    informatieObjectenService = TestBed.inject(InformatieObjectenService);
    utilService = TestBed.inject(UtilService);
    translateService = TestBed.inject(TranslateService);

    // Mock service methods
    jest
      .spyOn(zoekenService, "listDocumentKoppelbareZaken")
      .mockReturnValue(
        of(mockCaseLinkSearchResult as Response<"/rest/zoeken/zaken", "put">),
      );

    jest
      .spyOn(informatieObjectenService, "linkDocumentToCase")
      .mockReturnValue(
        of(undefined) as ReturnType<
          typeof informatieObjectenService.linkDocumentToCase
        >,
      );

    jest.spyOn(utilService, "setLoading").mockImplementation();
    jest.spyOn(utilService, "openSnackbar").mockImplementation();

    jest
      .spyOn(translateService, "instant")
      .mockImplementation((key: string | string[]) =>
        typeof key === "string" ? key : key[0],
      );

    // Set required inputs
    componentRef.setInput("sideNav", mockSideNav);
    componentRef.setInput("source", "SOURCE-ZAAK");
    componentRef.setInput("actionLabel", "actie.document.koppelen");

    fixture.detectChanges();
  });

  describe("Component initialization", () => {
    it("should set actionIcon to 'link' when actionLabel is 'actie.document.koppelen'", () => {
      componentRef.setInput("actionLabel", "actie.document.koppelen");
      component.ngOnInit();
      expect(component["actionIcon"]).toBe("link");
    });

    it("should set actionIcon to 'move_item' when actionLabel is 'actie.document.verplaatsen'", () => {
      componentRef.setInput("actionLabel", "actie.document.verplaatsen");
      component.ngOnInit();
      expect(component["actionIcon"]).toBe("move_item");
    });

    it("should initialize form with caseSearch control", () => {
      expect(component["form"].get("caseSearch")).toBeDefined();
    });
  });

  describe("Form validation", () => {
    it("should require caseSearch field", () => {
      const caseSearchControl = component["form"].get("caseSearch");
      expect(caseSearchControl?.hasError("required")).toBe(true);
    });

    it("should validate minimum length of 2 characters", () => {
      const caseSearchControl = component["form"].get("caseSearch");
      caseSearchControl?.setValue("a");
      expect(caseSearchControl?.hasError("minlength")).toBe(true);

      caseSearchControl?.setValue("ab");
      expect(caseSearchControl?.hasError("minlength")).toBe(false);
    });

    it("should have valid form with correct input", () => {
      component["form"].patchValue({ caseSearch: "ZAAK-001" });
      expect(component["form"].valid).toBe(true);
    });

    it("should disable submit button when form is invalid", async () => {
      componentRef.setInput("infoObject", mockInfoObjectRESTOntkoppeldDocument);
      fixture.detectChanges();

      component["form"].patchValue({ caseSearch: null });
      fixture.detectChanges();

      const submitButton = await loader.getHarness(
        MatButtonHarness.with({ text: /actie\.zoeken/ }),
      );
      expect(await submitButton.isDisabled()).toBe(true);
    });

    it("should disable submit button when loading", async () => {
      componentRef.setInput("infoObject", mockInfoObjectRESTOntkoppeldDocument);
      component["form"].patchValue({ caseSearch: "ZAAK-001" });
      component["loading"] = true;
      fixture.detectChanges();

      const submitButton = await loader.getHarness(
        MatButtonHarness.with({ text: /actie\.zoeken/ }),
      );
      expect(await submitButton.isDisabled()).toBe(true);
    });
  });

  describe("searchCases", () => {
    beforeEach(() => {
      componentRef.setInput("infoObject", mockInfoObjectRESTOntkoppeldDocument);
      fixture.detectChanges();
    });

    it("should call zoekenService.listDocumentKoppelbareZaken with correct parameters", () => {
      const listSpy = jest.spyOn(zoekenService, "listDocumentKoppelbareZaken");
      component["form"].patchValue({ caseSearch: "ZAAK-001" });

      component["searchCases"]();

      expect(listSpy).toHaveBeenCalledWith({
        zaakIdentificator: "ZAAK-001",
        informationObjectTypeUuid: "info-type-uuid-456",
        page: 0,
        rows: LINKABLE_ZAKEN_PAGINATION_SIZE,
      });
    });

    it("should set loading state during search", () => {
      component["form"].patchValue({ caseSearch: "ZAAK-001" });
      component["searchCases"]();

      expect(utilService.setLoading).toHaveBeenCalledWith(true);
    });

    it("should populate cases table with search results", () => {
      component["form"].patchValue({ caseSearch: "ZAAK-001" });

      component["searchCases"]();

      expect(component["cases"].data).toEqual(
        mockCaseLinkSearchResult.resultaten,
      );
      expect(component["totalCases"]).toBe(3);
      expect(component["loading"]).toBe(false);
    });

    it("should stop loading state after successful search", () => {
      component["form"].patchValue({ caseSearch: "ZAAK-001" });

      component["searchCases"]();

      expect(component["loading"]).toBe(false);
      expect(utilService.setLoading).toHaveBeenCalledWith(false);
    });

    it("should handle search errors", () => {
      jest
        .spyOn(zoekenService, "listDocumentKoppelbareZaken")
        .mockReturnValue(throwError(() => new Error("Search error")));

      component["form"].patchValue({ caseSearch: "ZAAK-001" });
      component["searchCases"]();

      expect(component["loading"]).toBe(false);
      expect(utilService.setLoading).toHaveBeenCalledWith(false);
    });

    it("should return early if infoObject has no informatieobjectTypeUUID", () => {
      const listSpy = jest.spyOn(zoekenService, "listDocumentKoppelbareZaken");
      componentRef.setInput("infoObject", {
        ...mockInfoObjectRESTOntkoppeldDocument,
        informatieobjectTypeUUID: undefined,
      });
      fixture.detectChanges();

      component["form"].patchValue({ caseSearch: "ZAAK-001" });
      component["searchCases"]();

      expect(listSpy).not.toHaveBeenCalled();
    });

    it("should display results in table", async () => {
      component["form"].patchValue({ caseSearch: "ZAAK-001" });
      component["searchCases"]();
      fixture.detectChanges();

      const table = await loader.getHarness(MatTableHarness);
      const rows = await table.getRows();

      expect(rows.length).toBe(3);
    });
  });

  describe("selectCase", () => {
    const selectableCase = mockCaseLinkSearchResult.resultaten![0];

    beforeEach(() => {
      componentRef.setInput("infoObject", mockInfoObjectRESTOntkoppeldDocument);
      fixture.detectChanges();
    });

    it("should call informatieObjectenService.linkDocumentToCase with correct parameters for RESTOntkoppeldDocument", () => {
      const linkSpy = jest.spyOn(
        informatieObjectenService,
        "linkDocumentToCase",
      );

      component["selectCase"](selectableCase);

      expect(linkSpy).toHaveBeenCalledWith({
        documentUUID: "doc-uuid-123",
        bron: "SOURCE-ZAAK",
        nieuweZaakID: "ZAAK-001",
      });
    });

    it("should call informatieObjectenService.linkDocumentToCase with correct parameters for RESTInboxDocument", () => {
      componentRef.setInput("infoObject", mockInfoObjectRESTInboxDocument);
      fixture.detectChanges();

      const linkSpy = jest.spyOn(
        informatieObjectenService,
        "linkDocumentToCase",
      );

      component["selectCase"](selectableCase);

      expect(linkSpy).toHaveBeenCalledWith({
        documentUUID: "inbox-uuid-789",
        bron: "SOURCE-ZAAK",
        nieuweZaakID: "ZAAK-001",
      });
    });

    it("should call informatieObjectenService.linkDocumentToCase with correct parameters for RestEnkelvoudigInformatieobject", () => {
      componentRef.setInput(
        "infoObject",
        mockInfoObjectRestEnkelvoudigInformatieobject,
      );
      fixture.detectChanges();

      const linkSpy = jest.spyOn(
        informatieObjectenService,
        "linkDocumentToCase",
      );

      component["selectCase"](selectableCase);

      expect(linkSpy).toHaveBeenCalledWith({
        documentUUID: "enkelvoudig-uuid-999",
        bron: "SOURCE-ZAAK",
        nieuweZaakID: "ZAAK-001",
      });
    });

    it("should show success snackbar with 'koppelen' message when actionLabel is actie.document.koppelen", () => {
      componentRef.setInput("actionLabel", "actie.document.koppelen");
      fixture.detectChanges();

      component["selectCase"](selectableCase);

      expect(utilService.openSnackbar).toHaveBeenCalledWith(
        "msg.document.koppelen.uitgevoerd",
        {
          document: "Test Document",
          case: "ZAAK-001",
        },
      );
    });

    it("should show success snackbar with 'verplaatsen' message when actionLabel is actie.document.verplaatsen", () => {
      componentRef.setInput("actionLabel", "actie.document.verplaatsen");
      fixture.detectChanges();

      component["selectCase"](selectableCase);

      expect(utilService.openSnackbar).toHaveBeenCalledWith(
        "msg.document.verplaatsen.uitgevoerd",
        {
          document: "Test Document",
          case: "ZAAK-001",
        },
      );
    });

    it("should close side nav after successful link", () => {
      component["selectCase"](selectableCase);

      expect(mockSideNav.close).toHaveBeenCalled();
    });

    it("should emit informationObjectLinked event after successful link", () => {
      const emitSpy = jest.spyOn(component.informationObjectLinked, "emit");

      component["selectCase"](selectableCase);

      expect(emitSpy).toHaveBeenCalled();
    });

    it("should reset form and cases after successful link", () => {
      component["form"].patchValue({ caseSearch: "test" });
      component["cases"].data = mockCaseLinkSearchResult.resultaten || [];

      component["selectCase"](selectableCase);

      expect(component["form"].value.caseSearch).toBeNull();
      expect(component["cases"].data).toEqual([]);
    });

    it("should handle link errors", () => {
      jest
        .spyOn(informatieObjectenService, "linkDocumentToCase")
        .mockReturnValue(throwError(() => new Error("Link error")));

      component["selectCase"](selectableCase);

      expect(component["loading"]).toBe(false);
      expect(utilService.setLoading).toHaveBeenCalledWith(false);
    });
  });

  describe("rowDisabled", () => {
    it("should return true when case is not koppelbaar", () => {
      const notKoppelbaarCase = fromPartial<
        GeneratedType<"RestZaakKoppelenZoekObject">
      >({
        identificatie: "ZAAK-NOT-KOPPELBAAR",
        isKoppelbaar: false,
      });

      expect(component["rowDisabled"](notKoppelbaarCase)).toBe(true);
    });

    it("should return true when case identificatie matches source", () => {
      const sourceCase = fromPartial<
        GeneratedType<"RestZaakKoppelenZoekObject">
      >({
        identificatie: "SOURCE-ZAAK",
        isKoppelbaar: true,
      });

      expect(component["rowDisabled"](sourceCase)).toBe(true);
    });

    it("should return false when case is koppelbaar and not source", () => {
      const validCase = fromPartial<
        GeneratedType<"RestZaakKoppelenZoekObject">
      >({
        identificatie: "ZAAK-VALID",
        isKoppelbaar: true,
      });

      expect(component["rowDisabled"](validCase)).toBe(false);
    });
  });

  describe("reset", () => {
    it("should reset form controls", () => {
      component["form"].patchValue({ caseSearch: "test-value" });

      component["reset"]();

      expect(component["form"].value.caseSearch).toBeNull();
    });

    it("should clear cases data", () => {
      component["cases"].data = mockCaseLinkSearchResult.resultaten || [];
      component["totalCases"] = 10;

      component["reset"]();

      expect(component["cases"].data).toEqual([]);
      expect(component["totalCases"]).toBe(0);
    });

    it("should reset loading state", () => {
      component["loading"] = true;

      component["reset"]();

      expect(component["loading"]).toBe(false);
    });

    it("should be called when reset button is clicked", async () => {
      componentRef.setInput("infoObject", mockInfoObjectRESTOntkoppeldDocument);
      component["form"].patchValue({ caseSearch: "test" });
      fixture.detectChanges();

      const resetSpy = jest.spyOn(component, "reset" as any);
      const resetButton = await loader.getHarness(
        MatButtonHarness.with({ text: /actie\.wissen/ }),
      );

      await resetButton.click();

      expect(resetSpy).toHaveBeenCalled();
    });
  });

  describe("close", () => {
    it("should close side nav", () => {
      component["close"]();

      expect(mockSideNav.close).toHaveBeenCalled();
    });

    it("should reset component state", () => {
      component["form"].patchValue({ caseSearch: "test" });
      component["cases"].data = mockCaseLinkSearchResult.resultaten || [];

      component["close"]();

      expect(component["form"].value.caseSearch).toBeNull();
      expect(component["cases"].data).toEqual([]);
    });
  });

  describe("UI interactions", () => {
    beforeEach(() => {
      componentRef.setInput("infoObject", mockInfoObjectRESTOntkoppeldDocument);
      fixture.detectChanges();
    });

    it("should display search input field", async () => {
      const searchInput = await loader.getHarness(
        MatInputHarness.with({ ancestor: "fieldset" }),
      );
      expect(searchInput).toBeDefined();
    });

    it("should display search and reset buttons", async () => {
      const buttons = await loader.getAllHarnesses(MatButtonHarness);
      const buttonTexts = await Promise.all(buttons.map((b) => b.getText()));

      expect(buttonTexts).toContain("actie.zoeken");
      expect(buttonTexts).toContain("actie.wissen");
    });

    it("should display cancel button", async () => {
      const cancelButton = await loader.getHarness(
        MatButtonHarness.with({ text: /actie\.annuleren/ }),
      );
      expect(cancelButton).toBeDefined();
    });

    it("should submit form when search button is clicked", async () => {
      const searchSpy = jest.spyOn(component, "searchCases" as any);
      component["form"].patchValue({ caseSearch: "ZAAK-001" });
      fixture.detectChanges();

      const searchButton = await loader.getHarness(
        MatButtonHarness.with({ text: /actie\.zoeken/ }),
      );

      await searchButton.click();

      expect(searchSpy).toHaveBeenCalled();
    });

    it("should show loading message when loading", async () => {
      component["loading"] = true;
      fixture.detectChanges();

      const compiled = fixture.nativeElement;
      expect(compiled.textContent).toContain("msg.loading");
    });

    it("should show no data message when no results and not loading", async () => {
      component["loading"] = false;
      component["cases"].data = [];
      fixture.detectChanges();

      const compiled = fixture.nativeElement;
      expect(compiled.textContent).toContain("msg.geen.gegevens.gevonden");
    });

    it("should show pagination warning when more than 10 results", async () => {
      component["loading"] = false;
      component["cases"].data = mockCaseLinkSearchResult.resultaten || [];
      component["totalCases"] = 15;
      fixture.detectChanges();

      const compiled = fixture.nativeElement;
      expect(compiled.textContent).toContain("Alleen de eerste 10 resultaten");
    });

    it("should not show pagination warning when 10 or fewer results", async () => {
      component["loading"] = false;
      component["cases"].data = mockCaseLinkSearchResult.resultaten || [];
      component["totalCases"] = 3;
      fixture.detectChanges();

      const compiled = fixture.nativeElement;
      expect(compiled.textContent).not.toContain(
        "Alleen de eerste 10 resultaten",
      );
    });
  });

  describe("getDocumentUUID", () => {
    it("should return uuid when infoObject has uuid property", () => {
      componentRef.setInput("infoObject", { uuid: "test-uuid-123" });
      fixture.detectChanges();

      expect(component["getDocumentUUID"]()).toBe("test-uuid-123");
    });

    it("should return documentUUID when infoObject has documentUUID property", () => {
      componentRef.setInput("infoObject", mockInfoObjectRESTOntkoppeldDocument);
      fixture.detectChanges();

      expect(component["getDocumentUUID"]()).toBe("doc-uuid-123");
    });

    it("should return enkelvoudiginformatieobjectUUID when infoObject is RESTInboxDocument", () => {
      componentRef.setInput("infoObject", mockInfoObjectRESTInboxDocument);
      fixture.detectChanges();

      expect(component["getDocumentUUID"]()).toBe("inbox-uuid-789");
    });

    it("should return uuid when infoObject is RestEnkelvoudigInformatieobject", () => {
      componentRef.setInput(
        "infoObject",
        mockInfoObjectRestEnkelvoudigInformatieobject,
      );
      fixture.detectChanges();

      expect(component["getDocumentUUID"]()).toBe("enkelvoudig-uuid-999");
    });

    it("should return empty string when infoObject is null", () => {
      componentRef.setInput("infoObject", null);
      fixture.detectChanges();

      expect(component["getDocumentUUID"]()).toBe("");
    });
  });
});
