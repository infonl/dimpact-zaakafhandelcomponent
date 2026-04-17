/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { provideHttpClient } from "@angular/common/http";
import { provideHttpClientTesting } from "@angular/common/http/testing";
import { ComponentFixture, TestBed } from "@angular/core/testing";
import { NoopAnimationsModule } from "@angular/platform-browser/animations";
import { TranslateModule } from "@ngx-translate/core";
import { provideQueryClient } from "@tanstack/angular-query-experimental";
import { of } from "rxjs";
import { InformatieObjectenService } from "src/app/informatie-objecten/informatie-objecten.service";
import { testQueryClient } from "../../../../../setupJest";
import { SmartDocumentsService } from "../../smart-documents.service";
import { SmartDocumentsFormComponent } from "./smart-documents-form.component";

describe(SmartDocumentsFormComponent.name, () => {
  let fixture: ComponentFixture<SmartDocumentsFormComponent>;
  let smartDocumentsService: SmartDocumentsService;
  let informatieObjectenService: InformatieObjectenService;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [
        SmartDocumentsFormComponent,
        TranslateModule.forRoot(),
        NoopAnimationsModule,
      ],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        provideQueryClient(testQueryClient),
      ],
    }).compileComponents();

    smartDocumentsService = TestBed.inject(SmartDocumentsService);
    jest
      .spyOn(smartDocumentsService, "getAllSmartDocumentsTemplateGroups")
      .mockReturnValue(of([]));
    jest
      .spyOn(smartDocumentsService, "getTemplatesMapping")
      .mockReturnValue(of([]));
    jest
      .spyOn(smartDocumentsService, "addParentIdsToTemplates")
      .mockReturnValue([]);
    jest
      .spyOn(smartDocumentsService, "addTemplateMappings")
      .mockReturnValue([]);
    jest.spyOn(smartDocumentsService, "flattenGroups").mockReturnValue([]);
    jest
      .spyOn(smartDocumentsService, "getTemplateMappings")
      .mockReturnValue([]);

    informatieObjectenService = TestBed.inject(InformatieObjectenService);
    jest
      .spyOn(informatieObjectenService, "listInformatieobjecttypes")
      .mockReturnValue(of([]));

    fixture = TestBed.createComponent(SmartDocumentsFormComponent);
    fixture.componentRef.setInput("zaakTypeUuid", "test-zaaktype-uuid");
    fixture.detectChanges();
    await fixture.whenStable();
    fixture.detectChanges();
  });

  it("should render", () => {
    expect(fixture.componentInstance).toBeTruthy();
  });

  it("should call getAllSmartDocumentsTemplateGroups on init", () => {
    expect(
      smartDocumentsService.getAllSmartDocumentsTemplateGroups,
    ).toHaveBeenCalled();
  });

  it("should call getTemplatesMapping with zaakTypeUuid on init", () => {
    expect(smartDocumentsService.getTemplatesMapping).toHaveBeenCalledWith(
      "test-zaaktype-uuid",
    );
  });

  it("should call listInformatieobjecttypes with zaakTypeUuid on init", () => {
    expect(
      informatieObjectenService.listInformatieobjecttypes,
    ).toHaveBeenCalledWith("test-zaaktype-uuid");
  });

  it("should not render the card when enabledGlobally is false", () => {
    fixture.componentRef.setInput("enabledGlobally", false);
    fixture.detectChanges();
    expect(fixture.nativeElement.querySelector("mat-card")).toBeNull();
  });

  describe("when enabledGlobally is true and enabledForZaaktype is false", () => {
    let localFixture: ComponentFixture<SmartDocumentsFormComponent>;

    beforeEach(async () => {
      localFixture = TestBed.createComponent(SmartDocumentsFormComponent);
      localFixture.componentRef.setInput("zaakTypeUuid", "test-zaaktype-uuid");
      localFixture.componentRef.setInput("enabledGlobally", true);
      localFixture.componentRef.setInput("enabledForZaaktype", false);
      localFixture.detectChanges();
      await localFixture.whenStable();
      localFixture.detectChanges();
    });

    it("should render the card", () => {
      expect(localFixture.nativeElement.querySelector("mat-card")).toBeTruthy();
    });

    it("should initialize enabledForZaaktypeForm with false", () => {
      expect(
        localFixture.componentInstance.enabledForZaaktypeForm.value
          .enabledForZaaktype,
      ).toBe(false);
    });

    it("enabledForZaaktypeValue should return false", () => {
      expect(localFixture.componentInstance.enabledForZaaktypeValue).toBe(
        false,
      );
    });

    it("should show disabled feedback", () => {
      expect(
        localFixture.nativeElement.querySelector(".form-disabled-feedback"),
      ).toBeTruthy();
    });

    it("should hide the tree form", () => {
      const treeForms =
        localFixture.nativeElement.querySelectorAll("mat-tree");
      expect(treeForms.length).toBe(0);
    });
  });

  describe("when enabledGlobally is true and enabledForZaaktype is true", () => {
    let localFixture: ComponentFixture<SmartDocumentsFormComponent>;

    beforeEach(async () => {
      localFixture = TestBed.createComponent(SmartDocumentsFormComponent);
      localFixture.componentRef.setInput("zaakTypeUuid", "test-zaaktype-uuid");
      localFixture.componentRef.setInput("enabledGlobally", true);
      localFixture.componentRef.setInput("enabledForZaaktype", true);
      localFixture.detectChanges();
      await localFixture.whenStable();
      localFixture.detectChanges();
    });

    it("should initialize enabledForZaaktypeForm with true", () => {
      expect(
        localFixture.componentInstance.enabledForZaaktypeForm.value
          .enabledForZaaktype,
      ).toBe(true);
    });

    it("enabledForZaaktypeValue should return true", () => {
      expect(localFixture.componentInstance.enabledForZaaktypeValue).toBe(
        true,
      );
    });

    it("should hide disabled feedback", () => {
      expect(
        localFixture.nativeElement.querySelector(".form-disabled-feedback"),
      ).toBeNull();
    });

    it("should show the tree form", () => {
      expect(
        localFixture.nativeElement.querySelector("mat-tree"),
      ).toBeTruthy();
    });
  });
});
