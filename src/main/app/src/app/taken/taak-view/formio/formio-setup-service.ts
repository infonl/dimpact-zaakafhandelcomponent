/*
 * SPDX-FileCopyrightText: 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { Injectable } from "@angular/core";
import { ExtendedComponentSchema, FormioForm } from "@formio/angular";
import { lastValueFrom } from "rxjs";
import { map, tap } from "rxjs/operators";
import { ReferentieTabelService } from "../../../admin/referentie-tabel.service";
import { ZaakafhandelParametersService } from "../../../admin/zaakafhandel-parameters.service";
import { UtilService } from "../../../core/service/util.service";
import { FormioCustomEvent } from "../../../formulieren/formio-wrapper/formio-wrapper.component";
import { IdentityService } from "../../../identity/identity.service";
import { InformatieObjectenService } from "../../../informatie-objecten/informatie-objecten.service";
import { InformatieobjectZoekParameters } from "../../../informatie-objecten/model/informatieobject-zoek-parameters";
import { OrderUtil } from "../../../shared/order/order-util";
import { GeneratedType } from "../../../shared/utils/generated-types";
import { Taak } from "../../model/taak";

@Injectable({
  providedIn: "root",
})
export class FormioSetupService {
  private taak?: Taak;
  private formioChangeData: Record<string, string> | undefined;

  constructor(
    public utilService: UtilService,
    private identityService: IdentityService,
    private zaakafhandelParametersService: ZaakafhandelParametersService,
    private referenceTableService: ReferentieTabelService,
    private informatieObjectenService: InformatieObjectenService,
  ) {}

  createFormioForm(formioFormulier: FormioForm, taak: Taak): void {
    this.taak = taak;

    this.initializeSpecializedFormioComponents(formioFormulier.components);
    this.utilService.setTitle("title.taak", {
      taak: formioFormulier.title,
    });
  }

  setFormioChangeData(data: Record<string, string>) {
    this.formioChangeData = data;
  }

  private initializeSpecializedFormioComponents(
    components: ExtendedComponentSchema[] | undefined,
  ): void {
    components?.forEach((component) => {
      switch (component.type) {
        case "groepMedewerkerFieldset":
          this.initializeGroepMedewerkerFieldsetComponent(component);
          break;
        case "smartDocumentsFieldset":
          this.initializeSmartDocumentsFieldsetComponent(component);
          break;
        case "referenceTableFieldset":
          this.initializeReferenceTableFieldsetComponent(component);
          break;
        case "documentsFieldset":
          this.initializeAvailableDocumentsFieldsetComponent(component);
          break;
      }
      if ("components" in component) {
        this.initializeSpecializedFormioComponents(component.components);
      }
    });
  }

  private initializeGroepMedewerkerFieldsetComponent(
    fieldsetComponent: ExtendedComponentSchema,
  ): void {
    fieldsetComponent.type = "fieldset";
    const groepComponent = fieldsetComponent.components[0];
    const medewerkerComponent = fieldsetComponent.components[1];
    this.initializeGroepMedewerkerFieldsetGroepComponent(groepComponent);
    this.initializeGroepMedewerkerFieldsetMedewerkerComponent(
      medewerkerComponent,
      groepComponent.key,
    );
  }

  private initializeGroepMedewerkerFieldsetGroepComponent(
    groepComponent: ExtendedComponentSchema,
  ): void {
    groepComponent.valueProperty = "id";
    groepComponent.template = "{{ item.naam }}";
    groepComponent.data = {
      custom: () =>
        lastValueFrom(
          this.identityService
            .listGroups(this.taak?.zaaktypeUUID)
            .pipe(tap((value) => value.sort(OrderUtil.orderBy("naam")))),
        ),
    };
  }

  private initializeGroepMedewerkerFieldsetMedewerkerComponent(
    medewerkerComponent: ExtendedComponentSchema,
    groepComponentKey: string,
  ): void {
    medewerkerComponent.valueProperty = "id";
    medewerkerComponent.template = "{{ item.naam }}";
    medewerkerComponent.data = {
      custom: () => {
        if (
          this.formioChangeData &&
          groepComponentKey in this.formioChangeData &&
          this.formioChangeData[groepComponentKey] != ""
        ) {
          return lastValueFrom(
            this.identityService
              .listUsersInGroup(this.formioChangeData[groepComponentKey])
              .pipe(tap((value) => value.sort(OrderUtil.orderBy("naam")))),
          );
        } else {
          return Promise.resolve([]);
        }
      },
    };
  }

  private initializeSmartDocumentsFieldsetComponent(
    fieldsetComponent: ExtendedComponentSchema,
  ): void {
    fieldsetComponent.type = "fieldset";
    const smartDocumentsPath = this.findSmartDocumentsPath(fieldsetComponent);
    const smartDocumentsTemplateComponent = fieldsetComponent.components?.find(
      (component: ExtendedComponentSchema) =>
        component.key === fieldsetComponent.key + "_Template",
    );
    smartDocumentsTemplateComponent.valueProperty = "id";
    smartDocumentsTemplateComponent.template = "{{ item.naam }}";
    smartDocumentsTemplateComponent.data = {
      custom: () =>
        lastValueFrom(
          this.zaakafhandelParametersService
            .listSmartDocumentsGroupTemplateNames(smartDocumentsPath)
            .pipe(tap((value) => value.sort())),
        ),
    };
  }

  private findSmartDocumentsPath(fieldsetComponent: ExtendedComponentSchema) {
    const componentWithProperties =
      this.getComponentWithProperties(fieldsetComponent);
    const smartDocumentsPath: GeneratedType<"RestSmartDocumentsPath"> = {
      path: this.getSmartDocumentsGroups(componentWithProperties),
    };
    return smartDocumentsPath;
  }

  /**
   * Find the first subcomponent that has properties
   *
   * @param component Parent component
   * @return sub-component with at least one property
   * @private
   */
  private getComponentWithProperties(
    component: ExtendedComponentSchema,
  ): ExtendedComponentSchema {
    return component.components?.find(
      (component: ExtendedComponentSchema) =>
        Object.keys(component.properties || []).length > 0,
    );
  }

  getSmartDocumentsGroups(component: ExtendedComponentSchema): string[] {
    return component?.properties["SmartDocuments_Group"].split("/");
  }

  /**
   * Returns the key name of the fieldset group using the key of the button. We assume that all components in the
   * fieldset have the same prefix as the key of the fieldset and that the separator is an underscore.
   *
   * If the name of the button is "AM_SmartDocuments_Create", the expected component base name is "AM_SmartDocuments".
   *
   * @example
   *     {
   *       "legend": "SmartDocuments",
   *       "type": "groepSmartDocumentsFieldset",
   *       "key": "AM_SmartDocuments",
   *       "components": [
   *         { "label": "Template", "type": "select", "key": "AM_SmartDocuments_Template", <.. more fields ..> },
   *         { "label": "Create", "key": "AM_SmartDocuments_Create", "type": "button", <.. more fields ..> }
   *       ]
   *     }
   */
  extractFieldsetName(component: ExtendedComponentSchema): string {
    return component.key.split("_").slice(0, -1).join("_");
  }

  extractSmartDocumentsTemplateName(event: FormioCustomEvent): string {
    return event.data[this.extractFieldsetName(event.component) + "_Template"];
  }

  normalizeSmartDocumentsTemplateName(
    smartDocumentsTemplateName: string,
  ): string {
    return smartDocumentsTemplateName?.replaceAll(" ", "_").trim();
  }

  getInformatieobjecttypeUuid(
    event: FormioCustomEvent,
    normalizedTemplateName: string,
  ): string {
    return (
      event.component.properties[
        `SmartDocuments_${normalizedTemplateName}_InformatieobjecttypeUuid`
      ] || event.component.properties["SmartDocuments_InformatieobjecttypeUuid"]
    );
  }

  private initializeReferenceTableFieldsetComponent(
    fieldsetComponent: ExtendedComponentSchema,
  ) {
    fieldsetComponent.type = "fieldset";
    const referenceTableSelector =
      this.getComponentWithProperties(fieldsetComponent);
    this.initializeReferenceTableSelectorComponent(referenceTableSelector);
  }

  private initializeReferenceTableSelectorComponent(
    referenceTableSelector: ExtendedComponentSchema,
  ) {
    const referenceTableCode =
      referenceTableSelector.properties["ReferenceTable_Code"];

    referenceTableSelector.valueProperty = "id";
    referenceTableSelector.template = "{{ item.naam }}";
    referenceTableSelector.data = {
      custom: () =>
        lastValueFrom(
          this.referenceTableService
            .readReferentieTabelByCode(referenceTableCode)
            .pipe(map((table) => table.waarden.map((value) => value.naam))),
        ),
    };
  }

  private initializeAvailableDocumentsFieldsetComponent(
    fieldsetComponent: ExtendedComponentSchema,
  ): void {

    const documentViewComponent = fieldsetComponent.components?.find(
      (component: { type: string }) => component.type === "select",
    );

    if (!documentViewComponent) {
      return;
    }

    fieldsetComponent.type = "fieldset";

    const zoekParameters = new InformatieobjectZoekParameters();
    zoekParameters.zaakUUID = this.taak!.zaakUuid;
    zoekParameters.gekoppeldeZaakDocumenten = false;

    documentViewComponent.valueProperty = "uuid";
    documentViewComponent.template = "{{ item.titel }}";
    documentViewComponent.data = {
      custom: () =>
        lastValueFrom(
          this.informatieObjectenService
            .listEnkelvoudigInformatieobjecten(zoekParameters)
            .pipe(
              map((docs) =>
                docs.map((doc) => ({
                  titel: doc.titel,
                  uuid: doc.uuid,
                })),
              ),
            ),
        ),
    };
  }
}
