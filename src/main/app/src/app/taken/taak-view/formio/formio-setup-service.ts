/*
 * SPDX-FileCopyrightText: 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { inject, Injectable } from "@angular/core";
import { ExtendedComponentSchema, FormioForm } from "@formio/angular";
import { QueryClient } from "@tanstack/angular-query-experimental";
import { lastValueFrom } from "rxjs";
import { ReferentieTabelService } from "../../../admin/referentie-tabel.service";
import { ZaakafhandelParametersService } from "../../../admin/zaakafhandel-parameters.service";
import { UtilService } from "../../../core/service/util.service";
import { FormioCustomEvent } from "../../../formulieren/formio-wrapper/formio-wrapper.component";
import { InformatieObjectenService } from "../../../informatie-objecten/informatie-objecten.service";
import { ZacQueryClient } from "../../../shared/http/zac-query-client";
import { OrderUtil } from "../../../shared/order/order-util";
import { GeneratedType } from "../../../shared/utils/generated-types";
import { ZakenService } from "../../../zaken/zaken.service";

export const ZAC_FIELD_ATTRIBUTE = "ZAC_TYPE";
export enum KNOWN_ZAC_FIELDS {
  GROEP = "ZAC_groep",
  MEDEWERKER = "ZAC_medewerker",
  SMART_DOCUMENTS_TEMPLATE = "ZAC_smart_documents_template",
  REFERENTIE_TABEL = "ZAC_referentie_tabel",
  DOCUMENTEN = "ZAC_documenten",
  RESULTAAT = "ZAC_resultaat",
  STATUS = "ZAC_status",
  PROCESS_DATA = "ZAC_process_data",
}

@Injectable({
  providedIn: "root",
})
export class FormioSetupService {
  private readonly queryClient = inject(QueryClient);
  private readonly zacQueryClient = inject(ZacQueryClient);

  private taak?: GeneratedType<"RestTask">;
  private formioChangeData?: Record<string, string>;

  constructor(
    public utilService: UtilService,
    private zaakafhandelParametersService: ZaakafhandelParametersService,
    private zakenService: ZakenService,
    private referenceTableService: ReferentieTabelService,
    private informatieObjectenService: InformatieObjectenService,
  ) {}

  createFormioForm(
    formioFormulier: FormioForm,
    taak: GeneratedType<"RestTask">,
  ) {
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
  ) {
    components?.forEach((component) => {
      this.safeInit(
        component.attributes?.[ZAC_FIELD_ATTRIBUTE] ??
          component.key ??
          component.type,
        () => {
          switch (
            component.attributes?.[ZAC_FIELD_ATTRIBUTE] ??
            component.type
          ) {
            case KNOWN_ZAC_FIELDS.GROEP:
              this.initializeGroepField(component);
              break;
            case KNOWN_ZAC_FIELDS.MEDEWERKER:
              this.initializeMedewerkerField(component);
              break;
            case KNOWN_ZAC_FIELDS.PROCESS_DATA:
              this.initializeProcessDataField(component);
              break;
            case "smartDocumentsFieldset":
              this.initializeSmartDocumentsFieldsetComponent(component);
              break;
            case KNOWN_ZAC_FIELDS.REFERENTIE_TABEL:
              this.initializeReferenceTableField(component);
              break;
            case "documentsFieldset":
              this.initializeAvailableDocumentsFieldsetComponent(component);
              break;
            case KNOWN_ZAC_FIELDS.RESULTAAT:
              this.initializeZaakResultField(component);
              break;
            case KNOWN_ZAC_FIELDS.STATUS:
              this.initializeZaakStatusField(component);
              break;
          }
          this.initializeSpecializedFormioComponents(
            this.getChildComponents(component),
          );
        },
      );
    });
  }

  private safeInit(context: string, fn: () => void) {
    try {
      fn();
    } catch (err) {
      const errorMessage =
        err instanceof Error ? err.message : "Unknown initialization error";
      this.utilService.handleFormIOInitError(context, errorMessage);
    }
  }

  private getChildComponents(fieldsetComponent: ExtendedComponentSchema) {
    return "components" in fieldsetComponent &&
      Array.isArray(fieldsetComponent.components)
      ? Array.from(fieldsetComponent.components)
      : [];
  }

  private initializeMedewerkerField(component: ExtendedComponentSchema) {
    component.valueProperty = "id";
    component.template = "{{ item.naam }}";
    component.data = {
      custom: () =>
        this.formioChangeData?.[component.refreshOn]
          ? this.queryClient.ensureQueryData(
              this.zacQueryClient.GET("/rest/identity/groups/{groupId}/users", {
                path: { groupId: this.formioChangeData?.[component.refreshOn] },
              }),
            )
          : Promise.resolve([]),
    };
  }

  private initializeProcessDataField(component: ExtendedComponentSchema) {
    component.type = "input";
  }

  private initializeGroepField(component: ExtendedComponentSchema) {
    component.valueProperty = "id";
    component.template = "{{ item.naam }}";
    component.data = {
      custom: async () => {
        const data = await this.queryClient.ensureQueryData(
          this.zacQueryClient.GET(
            "/rest/identity/groups/zaaktype/{zaaktypeUuid}",
            {
              path: { zaaktypeUuid: this.taak!.zaaktypeUUID! },
            },
          ),
        );
        return data.sort(OrderUtil.orderBy("naam"));
      },
    };
  }

  private initializeSmartDocumentsFieldsetComponent(
    fieldsetComponent: ExtendedComponentSchema,
  ) {
    fieldsetComponent.type = "fieldset";
    const smartDocumentsPath = this.findSmartDocumentsPath(fieldsetComponent);
    const smartDocumentsPathKey = smartDocumentsPath.path.join("/");
    const smartDocumentsTemplateComponent = fieldsetComponent.components?.find(
      (component: ExtendedComponentSchema) =>
        component.key === fieldsetComponent.key + "_Template",
    );

    smartDocumentsTemplateComponent.valueProperty = "id";
    smartDocumentsTemplateComponent.template = "{{ item.naam }}";

    smartDocumentsTemplateComponent.data = {
      custom: async () => {
        const data = await this.queryClient.ensureQueryData({
          queryKey: [
            "smartDocumentsGroupTemplateNamesQuery",
            smartDocumentsPathKey,
          ],
          queryFn: () =>
            lastValueFrom(
              this.zaakafhandelParametersService.listSmartDocumentsGroupTemplateNames(
                smartDocumentsPath,
              ),
            ),
        });
        return data.sort();
      },
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
    return component?.properties["SmartDocuments_Group"]?.split("/") ?? [];
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
  extractFieldsetName(component: ExtendedComponentSchema) {
    return component.key.split("_").slice(0, -1).join("_");
  }

  extractSmartDocumentsTemplateName(event: FormioCustomEvent) {
    return event.data[this.extractFieldsetName(event.component) + "_Template"];
  }

  normalizeSmartDocumentsTemplateName(smartDocumentsTemplateName: string) {
    return smartDocumentsTemplateName?.replace(/ /g, "_").trim();
  }

  getInformatieobjecttypeUuid(
    event: FormioCustomEvent,
    normalizedTemplateName: string,
  ) {
    return (
      event.component.properties[
        `SmartDocuments_${normalizedTemplateName}_InformatieobjecttypeUuid`
      ] || event.component.properties["SmartDocuments_InformatieobjecttypeUuid"]
    );
  }

  private initializeReferenceTableField(component: ExtendedComponentSchema) {
    const referenceTableCode = component.properties["ReferenceTable_Code"];
    component.valueProperty = "id";
    component.template = "{{ item.naam }}";
    component.data = {
      custom: async () => {
        const data = await this.queryClient.ensureQueryData(
          this.referenceTableService.readReferentieTabelByCode(
            referenceTableCode,
          ),
        );
        return data.waarden;
      },
    };
  }

  private initializeAvailableDocumentsFieldsetComponent(
    fieldsetComponent: ExtendedComponentSchema,
  ): void {
    const documentViewComponent = fieldsetComponent.components?.find(
      (component: { type: string }) => component.type === "select",
    );

    if (!documentViewComponent) return;

    fieldsetComponent.type = "fieldset";

    documentViewComponent.valueProperty = "uuid";
    documentViewComponent.template = "{{ item.titel }}";
    documentViewComponent.data = {
      custom: async () =>
        this.queryClient.ensureQueryData({
          queryKey: ["availableDocumentsQuery", this.taak!.zaakUuid],
          queryFn: () =>
            lastValueFrom(
              this.informatieObjectenService.listEnkelvoudigInformatieobjecten({
                zaakUUID: this.taak!.zaakUuid,
              }),
            ),
        }),
    };
  }

  private initializeZaakResultField(component: ExtendedComponentSchema) {
    component.valueProperty = "naam";
    component.template = "{{ item.naam }}";
    component.data = {
      custom: () =>
        this.queryClient.ensureQueryData(
          this.zacQueryClient.GET("/rest/zaken/resultaattypes/{zaaktypeUUID}", {
            path: { zaaktypeUUID: this.taak!.zaaktypeUUID! },
          }),
        ),
    };
  }

  private initializeZaakStatusField(component: ExtendedComponentSchema) {
    component.valueProperty = "naam";
    component.template = "{{ item.naam }}";
    component.data = {
      custom: () =>
        this.queryClient.ensureQueryData(
          this.zakenService.listStatustypes(this.taak!.zaaktypeUUID!),
        ),
    };
  }
}
