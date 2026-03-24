/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { HarnessLoader } from "@angular/cdk/testing";
import { TestbedHarnessEnvironment } from "@angular/cdk/testing/testbed";
import { ComponentFixture, TestBed } from "@angular/core/testing";
import { MatCheckboxHarness } from "@angular/material/checkbox/testing";
import { MatSelectHarness } from "@angular/material/select/testing";
import { NoopAnimationsModule } from "@angular/platform-browser/animations";
import { TranslateModule } from "@ngx-translate/core";
import { fromPartial } from "@total-typescript/shoehorn";
import { GeneratedType } from "../../../../shared/utils/generated-types";
import { SmartDocumentsFormItemComponent } from "./smart-documents-form-item.component";

describe(SmartDocumentsFormItemComponent.name, () => {
  let fixture: ComponentFixture<SmartDocumentsFormItemComponent>;
  let loader: HarnessLoader;

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
    loader = TestbedHarnessEnvironment.loader(fixture);
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
      expect(fixture.nativeElement.textContent).toContain("Template A");
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

    it("should clear informatieObjectTypeUUID when checkbox is unchecked", async () => {
      const checkbox = await loader.getHarness(MatCheckboxHarness);
      await checkbox.uncheck();

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

    it("should register empty option plus one option per informationObjectType in mat-select", async () => {
      const select = await loader.getHarness(MatSelectHarness);
      await select.open();
      const options = await select.getOptions();
      // 1 empty option + 2 from *ngFor
      expect(options.length).toBe(informationObjectTypes.length + 1);
      await select.close();
    });

    it("should bind informationObjectType uuid as option value", async () => {
      const select = await loader.getHarness(MatSelectHarness);
      await select.open();
      const typeAOption = await select.getOptions({ text: "Type A" });
      await typeAOption[0].click();

      expect(fixture.componentInstance.node.informatieObjectTypeUUID).toBe(
        "uuid-1",
      );
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
