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
import { MatDrawer } from "@angular/material/sidenav";
import { NoopAnimationsModule } from "@angular/platform-browser/animations";
import { TranslateModule, TranslateService } from "@ngx-translate/core";
import { fromPartial } from "@total-typescript/shoehorn";
import { of } from "rxjs";
import { UtilService } from "../../core/service/util.service";
import { Response } from "../../shared/http/http-client";
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

  const mockInfoObjectRestInboxDocument = fromPartial<
    GeneratedType<"RestInboxDocument">
  >({
    enkelvoudiginformatieobjectUUID: "inbox-uuid-789",
    enkelvoudiginformatieobjectID: "inbox-doc-789",
    titel: "Inbox Document",
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
        NoopAnimationsModule,
        TranslateModule.forRoot(),
        MaterialModule,
        MaterialFormBuilderModule,
        PipesModule,
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

    zoekenService = TestBed.inject(ZoekenService);
    informatieObjectenService = TestBed.inject(InformatieObjectenService);
    utilService = TestBed.inject(UtilService);
    translateService = TestBed.inject(TranslateService);

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
      .mockImplementation(
        (key: string | string[], params?: Record<string, unknown>) => {
          if (typeof key !== "string") return key[0];
          if (params && params["documentID"]) {
            return `${key} ${params["documentID"]}`;
          }
          return key;
        },
      );

    fixture = TestBed.createComponent(InformatieObjectLinkComponent);
    component = fixture.componentInstance;
    componentRef = fixture.componentRef;
    loader = TestbedHarnessEnvironment.loader(fixture);

    componentRef.setInput("sideNav", mockSideNav);
    componentRef.setInput("source", "SOURCE-ZAAK");
    componentRef.setInput("actionLabel", "actie.document.koppelen");

    fixture.detectChanges();
  });

  it("should validate form requires minimum 2 characters", () => {
    const control = component["form"].get("caseSearch");
    control?.setValue("a");
    expect(control?.hasError("minlength")).toBe(true);
  });

  it("should disable submit button when form invalid", async () => {
    componentRef.setInput("infoObject", mockInfoObjectRESTOntkoppeldDocument);
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

  it("should search cases with correct parameters", () => {
    const listSpy = jest.spyOn(zoekenService, "listDocumentKoppelbareZaken");
    componentRef.setInput("infoObject", mockInfoObjectRESTOntkoppeldDocument);
    fixture.detectChanges();
    component["form"].patchValue({ caseSearch: "ZAAK-001" });

    component["searchCases"]();

    expect(listSpy).toHaveBeenCalledWith({
      zaakIdentificator: "ZAAK-001",
      informationObjectTypeUuid: "info-type-uuid-456",
      page: 0,
      rows: LINKABLE_ZAKEN_PAGINATION_SIZE,
    });
  });

  it("should populate results after search", () => {
    componentRef.setInput("infoObject", mockInfoObjectRESTOntkoppeldDocument);
    fixture.detectChanges();
    component["form"].patchValue({ caseSearch: "ZAAK-001" });

    component["searchCases"]();

    expect(component["cases"].data).toEqual(
      mockCaseLinkSearchResult.resultaten,
    );
  });

  it("should link document with correct UUID", () => {
    const linkSpy = jest.spyOn(informatieObjectenService, "linkDocumentToCase");
    componentRef.setInput("infoObject", mockInfoObjectRESTOntkoppeldDocument);
    fixture.detectChanges();
    const selectableCase = mockCaseLinkSearchResult.resultaten![0];

    component["selectCase"](selectableCase);

    expect(linkSpy).toHaveBeenCalledWith({
      documentUUID: "doc-uuid-123",
      bron: "SOURCE-ZAAK",
      nieuweZaakID: "ZAAK-001",
    });
  });

  it("should show snackbar after successful link", () => {
    componentRef.setInput("infoObject", mockInfoObjectRESTOntkoppeldDocument);
    fixture.detectChanges();
    const selectableCase = mockCaseLinkSearchResult.resultaten![0];

    component["selectCase"](selectableCase);

    expect(utilService.openSnackbar).toHaveBeenCalledWith(
      "msg.document.koppelen.uitgevoerd",
      { document: "Test Document", case: "ZAAK-001" },
    );
  });

  it("should emit event after successful link", () => {
    const emitSpy = jest.spyOn(component.informationObjectLinked, "emit");
    componentRef.setInput("infoObject", mockInfoObjectRESTOntkoppeldDocument);
    fixture.detectChanges();
    const selectableCase = mockCaseLinkSearchResult.resultaten![0];

    component["selectCase"](selectableCase);

    expect(emitSpy).toHaveBeenCalled();
  });

  it("should disable row when case is not koppelbaar", () => {
    expect(
      component["rowDisabled"](
        fromPartial<GeneratedType<"RestZaakKoppelenZoekObject">>({
          identificatie: "ZAAK-1",
          isKoppelbaar: false,
        }),
      ),
    ).toBe(true);
  });

  it("should disable row when case matches source", () => {
    expect(
      component["rowDisabled"](
        fromPartial<GeneratedType<"RestZaakKoppelenZoekObject">>({
          identificatie: "SOURCE-ZAAK",
          isKoppelbaar: true,
        }),
      ),
    ).toBe(true);
  });

  it("should reset form on reset", () => {
    component["form"].patchValue({ caseSearch: "test-value" });

    component["reset"]();

    expect(component["form"].value.caseSearch).toBeNull();
  });

  it("should clear cases data on reset", () => {
    component["cases"].data = mockCaseLinkSearchResult.resultaten || [];

    component["reset"]();

    expect(component["cases"].data).toEqual([]);
  });

  it("should close side nav on close", () => {
    component["close"]();

    expect(mockSideNav.close).toHaveBeenCalled();
  });

  it("should extract UUID from RESTOntkoppeldDocument", () => {
    componentRef.setInput("infoObject", mockInfoObjectRESTOntkoppeldDocument);
    fixture.detectChanges();

    expect(component["getDocumentUUID"]()).toBe("doc-uuid-123");
  });

  it("should extract UUID from RestInboxDocument", () => {
    componentRef.setInput("infoObject", mockInfoObjectRestInboxDocument);
    fixture.detectChanges();

    expect(component["getDocumentUUID"]()).toBe("inbox-uuid-789");
  });
});
