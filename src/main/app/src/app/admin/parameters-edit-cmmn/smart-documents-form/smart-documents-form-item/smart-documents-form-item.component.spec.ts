/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { ComponentFixture, TestBed } from "@angular/core/testing";
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
  });
});
