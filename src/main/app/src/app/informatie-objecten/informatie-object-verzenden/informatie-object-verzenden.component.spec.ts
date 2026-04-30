/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { provideHttpClient } from "@angular/common/http";
import { provideHttpClientTesting } from "@angular/common/http/testing";
import { ComponentRef } from "@angular/core";
import { ComponentFixture, TestBed } from "@angular/core/testing";
import { FormControl, FormGroup } from "@angular/forms";
import { MatDrawer } from "@angular/material/sidenav";
import { NoopAnimationsModule } from "@angular/platform-browser/animations";
import { provideRouter } from "@angular/router";
import { TranslateModule, TranslateService } from "@ngx-translate/core";
import { of } from "rxjs";
import { fromPartial } from "src/test-helpers";
import { MaterialFormBuilderModule } from "../../shared/material-form-builder/material-form-builder.module";
import { MaterialModule } from "../../shared/material/material.module";
import { GeneratedType } from "../../shared/utils/generated-types";
import { InformatieObjectenService } from "../informatie-objecten.service";
import { InformatieObjectVerzendenComponent } from "./informatie-object-verzenden.component";

describe(InformatieObjectVerzendenComponent.name, () => {
  let component: InformatieObjectVerzendenComponent;
  let componentRef: ComponentRef<InformatieObjectVerzendenComponent>;
  let fixture: ComponentFixture<InformatieObjectVerzendenComponent>;
  let informatieObjectenService: InformatieObjectenService;
  let translateService: TranslateService;

  const mockSideNav = fromPartial<MatDrawer>({
    close: jest.fn().mockReturnValue(Promise.resolve()),
  });

  const makeZaak = (
    fields: Partial<GeneratedType<"RestZaak">> = {},
  ): GeneratedType<"RestZaak"> =>
    fromPartial<GeneratedType<"RestZaak">>({
      uuid: "zaak-uuid-001",
      zaaktype: fromPartial<GeneratedType<"RestZaaktype">>({
        uuid: "zaaktype-uuid-001",
      }),
      ...fields,
    });

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [
        InformatieObjectVerzendenComponent,
        MaterialModule,
        MaterialFormBuilderModule,
        TranslateModule.forRoot(),
        NoopAnimationsModule,
      ],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        provideRouter([]),
      ],
    }).compileComponents();

    informatieObjectenService = TestBed.inject(InformatieObjectenService);
    translateService = TestBed.inject(TranslateService);

    jest
      .spyOn(informatieObjectenService, "listInformatieobjectenVoorVerzenden")
      .mockReturnValue(of([]));

    jest
      .spyOn(translateService, "instant")
      .mockImplementation((key: string | string[]) =>
        typeof key === "string" ? key : key[0],
      );

    fixture = TestBed.createComponent(InformatieObjectVerzendenComponent);
    component = fixture.componentInstance;
    componentRef = fixture.componentRef;

    componentRef.setInput("sideNav", mockSideNav);
    componentRef.setInput("zaak", makeZaak());

    fixture.detectChanges();
  });

  describe("toolbar", () => {
    it("should render the toolbar title", () => {
      const toolbar = fixture.nativeElement.querySelector("mat-toolbar span");
      expect(toolbar.textContent.trim()).toBe("actie.document.verzenden");
    });
  });

  describe("ngOnInit", () => {
    it("should build a fields array with 4 rows", () => {
      expect(component["fields"].length).toBe(4);
    });

    it("should set formConfig with save and cancel labels", () => {
      const config = component["formConfig"];
      expect(config).not.toBeNull();
      expect(config!.saveButtonText).toBe("actie.verzenden");
      expect(config!.cancelButtonText).toBe("actie.annuleren");
    });

    it("should call listInformatieobjectenVoorVerzenden with zaak uuid", () => {
      expect(
        informatieObjectenService.listInformatieobjectenVoorVerzenden,
      ).toHaveBeenCalledWith("zaak-uuid-001");
    });
  });

  describe("onFormSubmit", () => {
    it("should close sideNav when called with null/falsy formGroup", () => {
      component["onFormSubmit"](null as unknown as FormGroup);
      expect(mockSideNav.close).toHaveBeenCalled();
    });

    it("should call verzenden with correct data when formGroup is provided", () => {
      jest
        .spyOn(informatieObjectenService, "verzenden")
        .mockReturnValue(of(undefined) as never);

      const formGroup = new FormGroup({
        documenten: new FormControl(["doc-uuid-1"]),
        verzenddatum: new FormControl("2026-01-15"),
        toelichting: new FormControl("test toelichting"),
      });

      component["onFormSubmit"](formGroup);

      expect(informatieObjectenService.verzenden).toHaveBeenCalledWith(
        expect.objectContaining({
          zaakUuid: "zaak-uuid-001",
          verzenddatum: "2026-01-15",
          toelichting: "test toelichting",
        }),
      );
    });

    it("should emit documentSent after successful verzenden", () => {
      jest
        .spyOn(informatieObjectenService, "verzenden")
        .mockReturnValue(of(undefined) as never);

      const emitSpy = jest.spyOn(component.documentSent, "emit");

      const formGroup = new FormGroup({
        documenten: new FormControl(["doc-uuid-1"]),
        verzenddatum: new FormControl("2026-01-15"),
        toelichting: new FormControl("test toelichting"),
      });

      component["onFormSubmit"](formGroup);

      expect(emitSpy).toHaveBeenCalled();
    });
  });

  describe("ngOnChanges", () => {
    it("should call updateDocumenten when zaak changes and previousValue exists", () => {
      jest
        .spyOn(informatieObjectenService, "listInformatieobjectenVoorVerzenden")
        .mockReturnValue(of([]));

      const updateSpy = jest.spyOn(
        component["documentSelectFormField"],
        "updateDocumenten",
      );

      const newZaak = makeZaak({ uuid: "zaak-uuid-002" });
      componentRef.setInput("zaak", newZaak);
      component.ngOnChanges({
        zaak: {
          currentValue: newZaak,
          previousValue: makeZaak(),
          firstChange: false,
          isFirstChange: () => false,
        },
      });

      expect(updateSpy).toHaveBeenCalled();
    });
  });
});
