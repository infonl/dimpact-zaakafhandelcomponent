/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { provideHttpClient } from "@angular/common/http";
import { provideHttpClientTesting } from "@angular/common/http/testing";
import {
  ComponentRef,
  provideExperimentalZonelessChangeDetection,
} from "@angular/core";
import { ComponentFixture, TestBed } from "@angular/core/testing";
import { ReactiveFormsModule } from "@angular/forms";
import { MatDialog } from "@angular/material/dialog";
import { MatDrawer } from "@angular/material/sidenav";
import { provideRouter } from "@angular/router";
import { By } from "@angular/platform-browser";
import { NoopAnimationsModule } from "@angular/platform-browser/animations";
import { TranslateModule } from "@ngx-translate/core";
import {
  provideQueryClient,
  provideTanStackQuery,
} from "@tanstack/angular-query-experimental";
import { of } from "rxjs";
import { SmartDocumentsService } from "src/app/admin/smart-documents.service";
import { fromPartial } from "src/test-helpers";
import { testQueryClient } from "../../../../setupJest";
import { IdentityService } from "../../identity/identity.service";
import { ZacAutoComplete } from "../../shared/form/auto-complete/auto-complete";
import { ZacDate } from "../../shared/form/date/date";
import { ZacInput } from "../../shared/form/input/input";
import { MaterialFormBuilderModule } from "../../shared/material-form-builder/material-form-builder.module";
import { MaterialModule } from "../../shared/material/material.module";
import { VertrouwelijkaanduidingToTranslationKeyPipe } from "../../shared/pipes/vertrouwelijkaanduiding-to-translation-key.pipe";
import { GeneratedType } from "../../shared/utils/generated-types";
import { InformatieObjectenService } from "../informatie-objecten.service";
import { InformatieObjectCreateAttendedComponent } from "./informatie-object-create-attended.component";

describe(InformatieObjectCreateAttendedComponent.name, () => {
  let component: InformatieObjectCreateAttendedComponent;
  let componentRef: ComponentRef<InformatieObjectCreateAttendedComponent>;
  let fixture: ComponentFixture<InformatieObjectCreateAttendedComponent>;
  let informatieObjectenService: InformatieObjectenService;
  let smartDocumentsService: SmartDocumentsService;
  let identityService: IdentityService;

  const mockSideNav = fromPartial<MatDrawer>({
    close: jest.fn().mockReturnValue(Promise.resolve()),
  });

  const makeZaak = (
    fields: Partial<GeneratedType<"RestZaak">> = {},
  ): GeneratedType<"RestZaak"> =>
    ({
      uuid: "zaak-uuid-001",
      zaaktype: fromPartial<GeneratedType<"RestZaaktype">>({
        uuid: "zaaktype-uuid-001",
      }),
      ...fields,
    }) as Partial<GeneratedType<"RestZaak">> as unknown as GeneratedType<"RestZaak">;

  const mockTemplateGroups: GeneratedType<"RestMappedSmartDocumentsTemplateGroup">[] =
    [
      {
        id: "group-1",
        name: "Group One",
        templates: [
          {
            id: "tpl-1",
            name: "Template One",
            informatieObjectTypeUUID: "info-type-uuid",
          },
        ],
        groups: null,
      },
    ];

  const mockLoggedInUser = fromPartial<GeneratedType<"RestUser">>({
    id: "user-1",
    naam: "Test User",
  });

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      declarations: [InformatieObjectCreateAttendedComponent],
      imports: [
        ReactiveFormsModule,
        MaterialModule,
        MaterialFormBuilderModule,
        TranslateModule.forRoot(),
        NoopAnimationsModule,
        ZacAutoComplete,
        ZacInput,
        ZacDate,
      ],
      providers: [
        provideExperimentalZonelessChangeDetection(),
        provideHttpClient(),
        provideHttpClientTesting(),
        provideTanStackQuery(testQueryClient),
        provideQueryClient(testQueryClient),
        provideRouter([]),
        VertrouwelijkaanduidingToTranslationKeyPipe,
      ],
    }).compileComponents();

    informatieObjectenService = TestBed.inject(InformatieObjectenService);
    smartDocumentsService = TestBed.inject(SmartDocumentsService);
    identityService = TestBed.inject(IdentityService);

    jest
      .spyOn(informatieObjectenService, "listInformatieobjecttypes")
      .mockReturnValue(of([]));

    jest
      .spyOn(smartDocumentsService, "getTemplatesMapping")
      .mockReturnValue(of(mockTemplateGroups) as ReturnType<typeof smartDocumentsService.getTemplatesMapping>);

    testQueryClient.setQueryData(
      identityService.readLoggedInUser().queryKey,
      mockLoggedInUser,
    );

    fixture = TestBed.createComponent(InformatieObjectCreateAttendedComponent);
    component = fixture.componentInstance;
    componentRef = fixture.componentRef;

    componentRef.setInput("sideNav", mockSideNav);
    componentRef.setInput("zaak", makeZaak());

    fixture.detectChanges();
    await fixture.whenStable();
  });

  describe("toolbar", () => {
    it("should render the toolbar title", () => {
      const toolbar = fixture.debugElement.query(By.css("mat-toolbar span"));
      expect(toolbar.nativeElement.textContent.trim()).toBe(
        "actie.document.maken",
      );
    });
  });

  describe("ngOnInit — template loading", () => {
    it("should call getTemplatesMapping when no smartDocumentsGroupPath/TemplateName/Uuid are set", () => {
      expect(smartDocumentsService.getTemplatesMapping).toHaveBeenCalledWith(
        "zaaktype-uuid-001",
      );
    });

    it("should call getTemplateGroup when all three SmartDocuments inputs are set", async () => {
      const getTemplateGroupSpy = jest
        .spyOn(smartDocumentsService, "getTemplateGroup")
        .mockReturnValue(of(mockTemplateGroups) as ReturnType<typeof smartDocumentsService.getTemplateGroup>);

      // Need a fresh fixture with all inputs set before ngOnInit runs
      const freshFixture = TestBed.createComponent(InformatieObjectCreateAttendedComponent);
      const freshRef = freshFixture.componentRef;
      freshRef.setInput("sideNav", mockSideNav);
      freshRef.setInput("zaak", makeZaak());
      freshRef.setInput("smartDocumentsGroupPath", ["Group One"]);
      freshRef.setInput("smartDocumentsTemplateName", "Template One");
      freshRef.setInput("smartDocumentsInformatieobjecttypeUuid", "info-type-uuid");

      freshFixture.detectChanges();
      await freshFixture.whenStable();

      expect(getTemplateGroupSpy).toHaveBeenCalled();
    });

    it("should set author from logged-in user", async () => {
      await fixture.whenStable();
      expect(component["form"].controls.author.value).toBe("Test User");
    });
  });

  describe("form submit button", () => {
    it("should be disabled when form is invalid", () => {
      const button = fixture.debugElement.query(
        By.css("button[type='submit']"),
      );
      expect(button.nativeElement.disabled).toBe(true);
    });
  });

  describe("onFormSubmit", () => {
    it("should close sideNav when called without a formGroup (null)", () => {
      component["onFormSubmit"](undefined);
      expect(mockSideNav.close).toHaveBeenCalled();
    });

    it("should close sideNav when called with an invalid form", () => {
      component["onFormSubmit"](component["form"]);
      expect(mockSideNav.close).toHaveBeenCalled();
    });

    it("should call createDocumentAttended and emit document when form is valid and redirectURL is provided", () => {
      jest
        .spyOn(informatieObjectenService, "createDocumentAttended")
        .mockReturnValue(
          of({ redirectURL: "https://example.com/doc", message: null }) as ReturnType<
            typeof informatieObjectenService.createDocumentAttended
          >,
        );

      const emitSpy = jest.spyOn(component.document, "emit");

      // Fill the form to make it valid
      component["form"].controls.templateGroup.enable();
      component["form"].controls.templateGroup.setValue(
        mockTemplateGroups[0],
      );
      component["form"].controls.template.enable();
      component["form"].controls.template.setValue(
        mockTemplateGroups[0].templates![0],
      );
      component["form"].controls.title.setValue("Test Title");
      component["form"].controls.author.setValue("Test User");

      component["onFormSubmit"](component["form"]);

      expect(informatieObjectenService.createDocumentAttended).toHaveBeenCalled();
      expect(emitSpy).toHaveBeenCalled();
    });

    it("should open NotificationDialog when createDocumentAttended returns no redirectURL", () => {
      jest
        .spyOn(informatieObjectenService, "createDocumentAttended")
        .mockReturnValue(
          of({ redirectURL: null, message: "Document created without redirect" }) as ReturnType<
            typeof informatieObjectenService.createDocumentAttended
          >,
        );

      const dialog = fixture.debugElement.injector.get(MatDialog);
      const openSpy = jest.spyOn(dialog, "open");

      component["form"].controls.templateGroup.enable();
      component["form"].controls.templateGroup.setValue(mockTemplateGroups[0]);
      component["form"].controls.template.enable();
      component["form"].controls.template.setValue(
        mockTemplateGroups[0].templates![0],
      );
      component["form"].controls.title.setValue("Test Title");
      component["form"].controls.author.setValue("Test User");

      component["onFormSubmit"](component["form"]);

      expect(openSpy).toHaveBeenCalled();
    });
  });
});
