/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { ComponentFixture, TestBed } from "@angular/core/testing";
import { MatSelect } from "@angular/material/select";
import { By } from "@angular/platform-browser";
import { NoopAnimationsModule } from "@angular/platform-browser/animations";
import { TranslateModule } from "@ngx-translate/core";
import { fromPartial } from "@total-typescript/shoehorn";
import { GeneratedType } from "../../../../shared/utils/generated-types";
import { SmartDocumentsFormItemComponent } from "./smart-documents-form-item.component";

describe(SmartDocumentsFormItemComponent.name, () => {
  let fixture: ComponentFixture<SmartDocumentsFormItemComponent>;

  const informationObjectTypes: GeneratedType<"RestInformatieobjecttype">[] = [
    fromPartial({
      uuid: "uuid-1",
      omschrijving: "Type A",
      vertrouwelijkheidaanduiding: "openbaar",
    }),
    fromPartial({
      uuid: "uuid-2",
      omschrijving: "Type B",
      vertrouwelijkheidaanduiding: "vertrouwelijk",
    }),
  ];

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [
        SmartDocumentsFormItemComponent,
        TranslateModule.forRoot(),
        NoopAnimationsModule,
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(SmartDocumentsFormItemComponent);
  });

  describe("when node has no informatieObjectTypeUUID", () => {
    beforeEach(() => {
      fixture.componentRef.setInput(
        "node",
        fromPartial<GeneratedType<"RestMappedSmartDocumentsTemplate">>({
          name: "Template A",
          informatieObjectTypeUUID: "",
        }),
      );
      fixture.componentRef.setInput(
        "informationObjectTypes",
        informationObjectTypes,
      );
      fixture.detectChanges();
    });

    it("should render the node name", () => {
      const nameEl = fixture.nativeElement.querySelector(
        ".flex-1",
      ) as HTMLElement;
      expect(nameEl?.textContent?.trim()).toBe("Template A");
    });

    it("should initialize checkbox as unchecked and disabled", () => {
      const component = fixture.componentInstance;
      expect(component["checkbox"].value).toBe(false);
      expect(component["checkbox"].disabled).toBe(true);
    });

    it("should initialize confidentiality as null", () => {
      expect(fixture.componentInstance["confidentiality"].value).toBeNull();
    });
  });

  describe("when node has an informatieObjectTypeUUID", () => {
    beforeEach(() => {
      fixture.componentRef.setInput(
        "node",
        fromPartial<GeneratedType<"RestMappedSmartDocumentsTemplate">>({
          name: "Template B",
          informatieObjectTypeUUID: "uuid-1",
        }),
      );
      fixture.componentRef.setInput(
        "informationObjectTypes",
        informationObjectTypes,
      );
      fixture.detectChanges();
    });

    it("should initialize checkbox as checked and enabled", () => {
      const component = fixture.componentInstance;
      expect(component["checkbox"].value).toBe(true);
      expect(component["checkbox"].disabled).toBe(false);
    });

    it("should set confidentiality from the matching information object type", () => {
      expect(fixture.componentInstance["confidentiality"].value).toBe(
        "vertrouwelijkheidaanduiding.openbaar",
      );
    });
  });

  describe("clearSelectedDocumentType", () => {
    beforeEach(() => {
      fixture.componentRef.setInput(
        "node",
        fromPartial<GeneratedType<"RestMappedSmartDocumentsTemplate">>({
          name: "Template C",
          informatieObjectTypeUUID: "uuid-1",
        }),
      );
      fixture.componentRef.setInput(
        "informationObjectTypes",
        informationObjectTypes,
      );
      fixture.detectChanges();
    });

    it("should clear informatieObjectTypeUUID", () => {
      fixture.componentInstance["clearSelectedDocumentType"]();
      expect(fixture.componentInstance.node.informatieObjectTypeUUID).toBe("");
    });

    it("should uncheck and disable the checkbox", () => {
      fixture.componentInstance["clearSelectedDocumentType"]();
      expect(fixture.componentInstance["checkbox"].value).toBe(false);
      expect(fixture.componentInstance["checkbox"].disabled).toBe(true);
    });

    it("should emit selectionChange with the cleared node", () => {
      const emitSpy = jest.spyOn(
        fixture.componentInstance.selectionChange,
        "emit",
      );
      fixture.componentInstance["clearSelectedDocumentType"]();
      expect(emitSpy).toHaveBeenCalledWith(
        expect.objectContaining({ informatieObjectTypeUUID: "" }),
      );
    });

    it("should clear informatieObjectTypeUUID via checkbox (change) template event", () => {
      const checkboxEl = fixture.debugElement.query(By.css("mat-checkbox"));
      checkboxEl.triggerEventHandler("change", { checked: false });

      expect(fixture.componentInstance.node.informatieObjectTypeUUID).toBe("");
    });
  });

  describe("mat-select options", () => {
    beforeEach(() => {
      fixture.componentRef.setInput(
        "node",
        fromPartial<GeneratedType<"RestMappedSmartDocumentsTemplate">>({
          name: "Template D",
          informatieObjectTypeUUID: "",
        }),
      );
      fixture.componentRef.setInput(
        "informationObjectTypes",
        informationObjectTypes,
      );
      fixture.detectChanges();
    });

    it("should register empty option plus one option per informationObjectType in mat-select", () => {
      const selectDE = fixture.debugElement.query(By.directive(MatSelect));
      const matSelect = selectDE.injector.get(MatSelect);
      // 1 empty option + 2 from *ngFor
      expect(matSelect.options.length).toBe(informationObjectTypes.length + 1);
    });

    it("should render informationObjectType omschrijving as option values", () => {
      const selectDE = fixture.debugElement.query(By.directive(MatSelect));
      const matSelect = selectDE.injector.get(MatSelect);
      const optionValues = matSelect.options.map((o) => o.value);
      expect(optionValues).toContain("uuid-1");
      expect(optionValues).toContain("uuid-2");
    });
  });

  describe("updateFormControls", () => {
    it("should emit selectionChange when UUID changes from previous value", () => {
      fixture.componentRef.setInput(
        "node",
        fromPartial<GeneratedType<"RestMappedSmartDocumentsTemplate">>({
          name: "Template E",
          informatieObjectTypeUUID: "",
        }),
      );
      fixture.componentRef.setInput(
        "informationObjectTypes",
        informationObjectTypes,
      );
      fixture.detectChanges(); // ngOnInit sets previousUUID = ""

      const emitSpy = jest.spyOn(
        fixture.componentInstance.selectionChange,
        "emit",
      );
      fixture.componentInstance.node.informatieObjectTypeUUID = "uuid-1";
      fixture.componentInstance["updateFormControls"]();

      expect(emitSpy).toHaveBeenCalledWith(
        expect.objectContaining({ informatieObjectTypeUUID: "uuid-1" }),
      );
    });

    it("should not emit selectionChange when UUID has not changed", () => {
      fixture.componentRef.setInput(
        "node",
        fromPartial<GeneratedType<"RestMappedSmartDocumentsTemplate">>({
          name: "Template F",
          informatieObjectTypeUUID: "uuid-1",
        }),
      );
      fixture.componentRef.setInput(
        "informationObjectTypes",
        informationObjectTypes,
      );
      fixture.detectChanges(); // ngOnInit sets previousUUID = "uuid-1"

      const emitSpy = jest.spyOn(
        fixture.componentInstance.selectionChange,
        "emit",
      );
      fixture.componentInstance["updateFormControls"]();

      expect(emitSpy).not.toHaveBeenCalled();
    });
  });
});
