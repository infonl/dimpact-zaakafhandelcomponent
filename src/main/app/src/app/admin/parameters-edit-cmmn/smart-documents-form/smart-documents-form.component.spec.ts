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
    jest.spyOn(smartDocumentsService, "getTemplateMappings").mockReturnValue([]);

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
});
