/*
 * SPDX-FileCopyrightText: 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { inject, Injectable } from "@angular/core";
import { ExtendedComponentSchema, FormioForm } from "@formio/angular";
import { TranslateService } from "@ngx-translate/core";
import { QueryClient } from "@tanstack/angular-query-experimental";
import { lastValueFrom } from "rxjs";
import { ReferentieTabelService } from "../../../admin/referentie-tabel.service";
import { SmartDocumentsService } from "../../../admin/smart-documents.service";
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
  SMART_DOCUMENTS_TEMPLATE_GROUPS = "ZAC_smart_documents_template_groups",
  SMART_DOCUMENTS_TEMPLATE_GROUP_TEMPLATES = "ZAC_smart_documents_template_group_templates",
  REFERENTIE_TABEL = "ZAC_referentie_tabel",
  DOCUMENTEN = "ZAC_documenten",
  DOCUMENTEN_NIET_ONDERTEKEND = "ZAC_documenten_niet_ondertekend",
  GEKOZEN_DOCUMENTEN_NIET_ONDERTEKEND = "ZAC_gekozen_documenten_niet_ondertekend",
  RESULTAAT = "ZAC_resultaat",
  STATUS = "ZAC_status",
  PROCESS_DATA = "ZAC_process_data",
}

/** Marker class picked up by `formio-wrapper.component.less` to hide the chrome of an empty field. */
const EMPTY_INPUT_FIELD_CLASS = "zac-empty-input-field";

const NO_DOCUMENTS_TO_SIGN_MESSAGE = "msg.geen-documenten-te-ondertekenen";

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
    private zakenService: ZakenService,
    private referenceTableService: ReferentieTabelService,
    private informatieObjectenService: InformatieObjectenService,
    private smartDocumentsService: SmartDocumentsService,
    private translateService: TranslateService,
  ) {}

  async createFormioForm(
    formioFormulier: FormioForm,
    taak: GeneratedType<"RestTask">,
  ) {
    this.taak = taak;

    await this.initializeSpecializedFormioComponents(
      formioFormulier.components,
    );
    this.utilService.setTitle("title.taak", {
      taak: formioFormulier.title,
    });
  }

  setFormioChangeData(data: Record<string, string>) {
    this.formioChangeData = data;
  }

  private async initializeSpecializedFormioComponents(
    components: ExtendedComponentSchema[] | undefined,
  ) {
    for (const component of components ?? []) {
      await this.safeInit(
        component.attributes?.[ZAC_FIELD_ATTRIBUTE] ??
          component.key ??
          component.type,
        async () => {
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
            case KNOWN_ZAC_FIELDS.SMART_DOCUMENTS_TEMPLATE_GROUPS:
              this.initializeSmartDocumentsTemplateGroupsField(component);
              break;
            case KNOWN_ZAC_FIELDS.SMART_DOCUMENTS_TEMPLATE_GROUP_TEMPLATES:
              this.initializeSmartDocumentsTemplateGroupTemplatesField(
                component,
              );
              break;
            case KNOWN_ZAC_FIELDS.REFERENTIE_TABEL:
              this.initializeReferenceTableField(component);
              break;
            case KNOWN_ZAC_FIELDS.DOCUMENTEN:
              this.initializeDocumentsField(component);
              break;
            case KNOWN_ZAC_FIELDS.DOCUMENTEN_NIET_ONDERTEKEND:
              await this.initializeUnsignedDocumentsDatagrid(component);
              break;
            case KNOWN_ZAC_FIELDS.GEKOZEN_DOCUMENTEN_NIET_ONDERTEKEND:
              await this.initializeSelectedUnsignedDocumentsDatagrid(component);
              break;
            case KNOWN_ZAC_FIELDS.RESULTAAT:
              this.initializeZaakResultField(component);
              break;
            case KNOWN_ZAC_FIELDS.STATUS:
              this.initializeZaakStatusField(component);
              break;
          }
          await this.initializeSpecializedFormioComponents(
            this.getChildComponents(component),
          );
        },
      );
    }
  }

  private async safeInit(context: string, fn: () => Promise<void>) {
    try {
      await fn();
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
            "/rest/identity/zaaktype/{zaaktypeDescription}/behandelaar-groups",
            {
              path: { zaaktypeDescription: this.taak!.zaaktypeOmschrijving! },
            },
          ),
        );
        return data.sort(OrderUtil.orderBy("naam"));
      },
    };
  }

  private getSmartDocumentTemplates(zaaktypeUuid: string) {
    return this.queryClient.ensureQueryData({
      queryKey: ["smartDocumentsTemplatesMapping", zaaktypeUuid],
      queryFn: () =>
        lastValueFrom(
          this.smartDocumentsService.getTemplatesMapping(zaaktypeUuid),
        ),
    });
  }

  private initializeSmartDocumentsTemplateGroupsField(
    component: ExtendedComponentSchema,
  ) {
    component.valueProperty = "id";
    component.template = "{{ item.naam }}";
    component.data = {
      custom: async () => {
        const data = await this.getSmartDocumentTemplates(
          this.taak!.zaaktypeUUID!,
        );
        return data
          .map((templateGroup) => ({
            id: templateGroup.id,
            naam: templateGroup.name,
            active: false,
          }))
          .sort(OrderUtil.orderBy("naam"));
      },
    };
  }

  private initializeSmartDocumentsTemplateGroupTemplatesField(
    component: ExtendedComponentSchema,
  ) {
    component.valueProperty = "id";
    component.template = "{{ item.naam }}";
    component.data = {
      custom: async () => {
        const data = await this.getSmartDocumentTemplates(
          this.taak!.zaaktypeUUID!,
        );
        const templateGroupId = this.formioChangeData?.[component.refreshOn];
        const templateGroup = data.find(
          (group) => group.id === templateGroupId,
        );
        return (
          templateGroup?.templates
            .map((template) => ({
              id: template.id,
              naam: template.name,
              active: false,
            }))
            .sort(OrderUtil.orderBy("naam")) ?? []
        );
      },
    };
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

  extractSmartDocumentsGroupId(event: FormioCustomEvent): string | undefined {
    return event.data[this.extractFieldsetName(event.component) + "_Group"];
  }

  extractSmartDocumentsTemplateId(
    event: FormioCustomEvent,
  ): string | undefined {
    return event.data[this.extractFieldsetName(event.component) + "_Template"];
  }

  private initializeReferenceTableField(component: ExtendedComponentSchema) {
    const referenceTableCode = component.properties["ReferenceTable_Code"];
    component.valueProperty = "id";
    component.template = "{{ item.name }}";
    component.data = {
      custom: async () => {
        const data = await this.queryClient.ensureQueryData(
          this.referenceTableService.readReferentieTabelByCode(
            referenceTableCode,
          ),
        );
        return data.values;
      },
    };
  }

  private initializeDocumentsField(component: ExtendedComponentSchema) {
    component.valueProperty = "uuid";
    component.template = "{{ item.titel }}";
    component.data = {
      custom: async () => this.fetchZaakDocuments(),
    };
  }

  private async initializeUnsignedDocumentsDatagrid(
    component: ExtendedComponentSchema,
  ): Promise<void> {
    const documents = await this.fetchZaakDocuments();
    const alreadySelectedUuids = new Set(
      this.getSelectedRows(component.key)
        .map((row) => row.uuid)
        .filter((uuid): uuid is string => Boolean(uuid)),
    );

    this.setDatagridRows(
      component,
      documents
        .filter((document) => !document.ondertekening)
        .map((document) =>
          this.toDocumentRow(
            document,
            !!document.uuid && alreadySelectedUuids.has(document.uuid),
          ),
        ),
    );
  }

  /**
   * Form.io gives the submission data precedence over a component's `defaultValue`, and the task
   * data holds a value for every form field, so rows have to be written into the submission itself
   * or the grid renders empty.
   */
  private setDatagridRows(
    component: ExtendedComponentSchema,
    rows: ReturnType<typeof this.toDocumentRow>[],
  ) {
    // without this a datagrid falls back to a single blank row, which would render an empty
    // checkbox and title and end up in the task data as a row without a document
    component.initEmpty = true;
    component.defaultValue = rows;
    if (this.taak?.taakdata) {
      this.taak.taakdata[component.key] = rows;
    }
    this.applyEmptyState(
      component,
      rows.length === 0,
      NO_DOCUMENTS_TO_SIGN_MESSAGE,
    );
  }

  /**
   * Form.io keeps rendering a field's chrome when it has nothing to show — an empty datagrid still
   * draws its column headers. Mark the field so the stylesheet can drop that chrome, and state why
   * it is empty instead. Values set by the form author are kept, and restored once the field fills
   * up again: this runs on every initialization, including reopening the task.
   */
  private applyEmptyState(
    component: ExtendedComponentSchema,
    isEmpty: boolean,
    emptyMessageKey: string,
  ) {
    const emptyMessage: string =
      this.translateService.instant(emptyMessageKey) ?? "";
    const authorClasses = String(component.customClass ?? "")
      .split(" ")
      .filter((cssClass) => cssClass && cssClass !== EMPTY_INPUT_FIELD_CLASS);
    const authorDescription =
      component.description === emptyMessage
        ? ""
        : (component.description ?? "");

    component.customClass = (
      isEmpty ? [...authorClasses, EMPTY_INPUT_FIELD_CLASS] : authorClasses
    ).join(" ");
    component.description = isEmpty ? emptyMessage : authorDescription;
  }

  /**
   * A task can stay open for days, so the rows stored by the preceding selection task are only
   * trusted for their uuids: titles and signing state are re-read here, and documents signed in
   * the meantime are dropped so they cannot be offered for signing twice.
   */
  private async initializeSelectedUnsignedDocumentsDatagrid(
    component: ExtendedComponentSchema,
  ): Promise<void> {
    const selectedUuids = component.refreshOn
      ? this.getSelectedRows(component.refreshOn)
          .map((row) => row.uuid)
          .filter((uuid): uuid is string => Boolean(uuid))
      : [];

    if (!selectedUuids.length) {
      component.defaultValue = [];
      this.applyEmptyState(component, true, NO_DOCUMENTS_TO_SIGN_MESSAGE);
      return;
    }

    const documents = await this.fetchZaakDocuments(selectedUuids);
    const rows = documents
      .filter((document) => !document.ondertekening)
      .map((document) => this.toDocumentRow(document, true));

    component.defaultValue = rows;
    this.applyEmptyState(
      component,
      rows.length === 0,
      NO_DOCUMENTS_TO_SIGN_MESSAGE,
    );
  }

  private toDocumentRow(
    document: GeneratedType<"RestEnkelvoudigInformatieobject">,
    selected: boolean,
  ) {
    return {
      selected,
      titel: document.titel,
      uuid: document.uuid,
    };
  }

  private getSelectedRows(refreshOnKey: string) {
    const rows = this.taak?.taakdata?.[refreshOnKey];
    return Array.isArray(rows)
      ? (rows as { selected: boolean; uuid?: string }[]).filter(
          (row) => row.selected,
        )
      : [];
  }

  private fetchZaakDocuments(informatieobjectUUIDs?: string[]) {
    // the uuids discriminate the result, so they belong in the query key or a filtered fetch
    // collides on the cache with the unfiltered list of the same zaak
    return this.queryClient.ensureQueryData({
      queryKey: [
        "availableDocumentsQuery",
        this.taak!.zaakUuid,
        informatieobjectUUIDs && [...informatieobjectUUIDs].sort(),
      ],
      queryFn: () =>
        lastValueFrom(
          this.informatieObjectenService.listEnkelvoudigInformatieobjecten({
            zaakUUID: this.taak!.zaakUuid,
            informatieobjectUUIDs,
          }),
        ),
    });
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
