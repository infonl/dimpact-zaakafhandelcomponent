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
import { renderFieldError } from "./formio-component-utils";

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
  REGEL_LINK = "ZAC_regel_link_tekstueel",
  REGEL_LINK_VIEW_ICON = "ZAC_regel_link_oog_icoon",
  RESULTAAT = "ZAC_resultaat",
  STATUS = "ZAC_status",
  PROCESS_DATA = "ZAC_process_data",
}

/** Hides the field's chrome; styled in `formio-wrapper.component.less`. */
const EMPTY_INPUT_FIELD_CLASS = "zac-empty-input-field";

const NO_DOCUMENTS_TO_SIGN_MESSAGE = "msg.geen-documenten-te-ondertekenen";

const SELECT_A_DOCUMENT_MESSAGE = "msg.selecteer-minimaal-een-document";

type DocumentRow = {
  selected: boolean;
  titel?: string | null;
  uuid?: string | null;
};

type RowLink = {
  /** `{{ row.… }}` placeholders stay in: only Form.io can resolve those, per row. */
  href: (taak: GeneratedType<"RestTask">) => string;
  textKey: string;
};

/** The class is restyled in `formio-wrapper.component.less`: global rules miss the shadow DOM. */
const VIEW_ICON_CONTENT =
  '<span class="material-symbols-outlined">visibility</span>';

const DOCUMENT_ROW_LINK: RowLink = {
  href: () => `/informatie-objecten/{{ row.uuid }}`,
  textKey: "actie.document.openen-nieuw-tabblad",
};

/** Where a `ZAC_regel_link_tekstueel` column points, per `ZAC_TYPE` of the datagrid holding it. */
const ROW_LINKS: Record<string, RowLink> = {
  [KNOWN_ZAC_FIELDS.DOCUMENTEN_NIET_ONDERTEKEND]: DOCUMENT_ROW_LINK,
  [KNOWN_ZAC_FIELDS.GEKOZEN_DOCUMENTEN_NIET_ONDERTEKEND]: DOCUMENT_ROW_LINK,
};

@Injectable({
  providedIn: "root",
})
export class FormioSetupService {
  private readonly queryClient = inject(QueryClient);
  private readonly zacQueryClient = inject(ZacQueryClient);

  private formioChangeData?: Record<string, string>;

  constructor(
    public utilService: UtilService,
    private zakenService: ZakenService,
    private referenceTableService: ReferentieTabelService,
    private informatieObjectenService: InformatieObjectenService,
    private smartDocumentsService: SmartDocumentsService,
    private translateService: TranslateService,
  ) {}

  /**
   * `taak` is passed down rather than stored: this service is a singleton, setups overlap, and the
   * data sources below are called back long after this returns — a field would go stale under them.
   */
  async createFormioForm(
    formioFormulier: FormioForm,
    taak: GeneratedType<"RestTask">,
    zaak: GeneratedType<"RestZaak">,
  ) {
    await this.initializeSpecializedFormioComponents(
      formioFormulier.components,
      taak,
      zaak,
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
    taak: GeneratedType<"RestTask">,
    zaak: GeneratedType<"RestZaak">,
    parentZacType?: string,
  ) {
    for (const component of components ?? []) {
      const zacType: string =
        component.attributes?.[ZAC_FIELD_ATTRIBUTE] ?? component.type;

      await this.safeInit(
        component.attributes?.[ZAC_FIELD_ATTRIBUTE] ??
          component.key ??
          component.type,
        async () => {
          switch (zacType) {
            case KNOWN_ZAC_FIELDS.GROEP:
              this.initializeGroepField(component, taak);
              break;
            case KNOWN_ZAC_FIELDS.MEDEWERKER:
              this.initializeMedewerkerField(component);
              break;
            case KNOWN_ZAC_FIELDS.PROCESS_DATA:
              this.initializeProcessDataField(component);
              break;
            case KNOWN_ZAC_FIELDS.SMART_DOCUMENTS_TEMPLATE_GROUPS:
              this.initializeSmartDocumentsTemplateGroupsField(component, taak);
              break;
            case KNOWN_ZAC_FIELDS.SMART_DOCUMENTS_TEMPLATE_GROUP_TEMPLATES:
              this.initializeSmartDocumentsTemplateGroupTemplatesField(
                component,
                taak,
              );
              break;
            case KNOWN_ZAC_FIELDS.REFERENTIE_TABEL:
              this.initializeReferenceTableField(component);
              break;
            case KNOWN_ZAC_FIELDS.DOCUMENTEN:
              this.initializeDocumentsField(component, taak);
              break;
            case KNOWN_ZAC_FIELDS.DOCUMENTEN_NIET_ONDERTEKEND:
              await this.initializeUnsignedDocumentsDatagrid(component, taak);
              break;
            case KNOWN_ZAC_FIELDS.GEKOZEN_DOCUMENTEN_NIET_ONDERTEKEND:
              await this.initializeSelectedUnsignedDocumentsDatagrid(
                component,
                taak,
              );
              break;
            case KNOWN_ZAC_FIELDS.REGEL_LINK:
              this.initializeRowLinkColumn(component, taak, parentZacType);
              break;
            case KNOWN_ZAC_FIELDS.REGEL_LINK_VIEW_ICON:
              this.initializeRowLinkColumn(component, taak, parentZacType, {
                asIcon: true,
              });
              break;
            case KNOWN_ZAC_FIELDS.RESULTAAT:
              this.initializeZaakResultField(component, taak);
              break;
            case KNOWN_ZAC_FIELDS.STATUS:
              this.initializeZaakStatusField(component, taak);
              break;
            default:
              this.markUnknownZacType(component);
          }
          await this.initializeSpecializedFormioComponents(
            this.getChildComponents(component),
            taak,
            zaak,
            zacType,
          );
        },
      );
    }
  }

  // Form.io flags an unknown `type` itself, but a misspelled `ZAC_TYPE` renders a silently empty field.
  private markUnknownZacType(component: ExtendedComponentSchema) {
    const zacType = component.attributes?.[ZAC_FIELD_ATTRIBUTE];
    if (!zacType) return;

    renderFieldError(component, `Undefined ZAC_TYPE: '${zacType}'`);
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

  /** A table nests its children per cell in `rows`, and a columns component in `columns`. */
  private getChildComponents(
    component: ExtendedComponentSchema,
  ): ExtendedComponentSchema[] {
    const children = Array.isArray(component.components)
      ? Array.from(component.components)
      : [];

    const cells: ExtendedComponentSchema[] = [
      ...(Array.isArray(component.rows) ? component.rows.flat() : []),
      ...(Array.isArray(component.columns) ? component.columns : []),
    ];

    return [
      ...children,
      ...cells.flatMap((cell) =>
        Array.isArray(cell?.components) ? cell.components : [],
      ),
    ];
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

  private initializeGroepField(
    component: ExtendedComponentSchema,
    taak: GeneratedType<"RestTask">,
  ) {
    component.valueProperty = "id";
    component.template = "{{ item.naam }}";
    component.data = {
      custom: async () => {
        const data = await this.queryClient.ensureQueryData(
          this.zacQueryClient.GET(
            "/rest/identity/zaaktype/{zaaktypeDescription}/behandelaar-groups",
            {
              path: { zaaktypeDescription: taak.zaaktypeOmschrijving! },
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
    taak: GeneratedType<"RestTask">,
  ) {
    component.valueProperty = "id";
    component.template = "{{ item.naam }}";
    component.data = {
      custom: async () => {
        const data = await this.getSmartDocumentTemplates(taak.zaaktypeUUID!);
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
    taak: GeneratedType<"RestTask">,
  ) {
    component.valueProperty = "id";
    component.template = "{{ item.naam }}";
    component.data = {
      custom: async () => {
        const data = await this.getSmartDocumentTemplates(taak.zaaktypeUUID!);
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

  private initializeDocumentsField(
    component: ExtendedComponentSchema,
    taak: GeneratedType<"RestTask">,
  ) {
    component.valueProperty = "uuid";
    component.template = "{{ item.titel }}";
    component.data = {
      custom: async () => this.fetchZaakDocuments(taak),
    };
  }

  /**
   * `validate.required` on a datagrid only checks that it has rows, which the rows filled in here
   * always satisfy — so ticking a checkbox needs a custom rule. Form.io evaluates it with the
   * component's value as `input` and reads back `valid`; a string becomes the error message.
   */
  private requireASelectedRow(component: ExtendedComponentSchema) {
    if (component.validate?.custom) return;

    const message: string = this.translateService.instant(
      SELECT_A_DOCUMENT_MESSAGE,
    );
    component.validate = {
      ...component.validate,
      custom:
        `valid = (input || []).some(function (row) { return row.selected; })` +
        ` ? true : ${JSON.stringify(message)}`,
    };
  }

  private isFinished(taak: GeneratedType<"RestTask">) {
    return taak.status === "AFGEROND";
  }

  /**
   * A finished task is a record of what was submitted, so its grids show the stored rows as they are.
   * Re-reading the zaak documents would drop every document the task got signed and lose the ticks
   * that say which ones were chosen. The grid is already disabled by the view's `isReadonly()`.
   */
  private renderStoredDocumentRows(
    component: ExtendedComponentSchema,
    taak: GeneratedType<"RestTask">,
  ) {
    const rows = this.getStoredRows(taak, component.key);
    component.initEmpty = true;
    component.defaultValue = rows;
    this.applyEmptyState(
      component,
      rows.length === 0,
      NO_DOCUMENTS_TO_SIGN_MESSAGE,
    );
  }

  // Ticked only when this task submitted the document and it carries a signature now, so a failed
  // signing reads as failed.
  private async renderSigningResult(
    component: ExtendedComponentSchema,
    taak: GeneratedType<"RestTask">,
  ) {
    const storedRows = this.getStoredRows(taak, component.key);
    const uuids = storedRows
      .map((row) => row.uuid)
      .filter((uuid): uuid is string => Boolean(uuid));
    const documents = uuids.length
      ? await this.fetchZaakDocuments(taak, uuids)
      : [];

    const rows = storedRows.map((row) => {
      const document = documents.find(({ uuid }) => uuid === row.uuid);
      return {
        selected: Boolean(row.selected && document?.ondertekening),
        titel: document?.titel ?? row.titel,
        uuid: row.uuid,
      };
    });

    this.setDatagridRows(component, taak, rows);
  }

  private async initializeUnsignedDocumentsDatagrid(
    component: ExtendedComponentSchema,
    taak: GeneratedType<"RestTask">,
  ): Promise<void> {
    if (this.isFinished(taak)) {
      this.renderStoredDocumentRows(component, taak);
      return;
    }

    this.requireASelectedRow(component);

    const documents = await this.fetchZaakDocuments(taak);
    const alreadySelectedUuids = new Set(
      this.getSelectedRows(taak, component.key)
        .map((row) => row.uuid)
        .filter((uuid): uuid is string => Boolean(uuid)),
    );

    this.setDatagridRows(
      component,
      taak,
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
   * Form.io prefers the submission data over `defaultValue`, and the task data holds a value for
   * every field, so the rows have to go into the submission too or the grid renders empty.
   */
  private setDatagridRows(
    component: ExtendedComponentSchema,
    taak: GeneratedType<"RestTask">,
    rows: DocumentRow[],
  ) {
    // without this a datagrid falls back to one blank row, which lands in the task data as a row
    // without a document
    component.initEmpty = true;
    component.defaultValue = rows;
    if (taak.taakdata) {
      taak.taakdata[component.key] = rows;
    }
    this.applyEmptyState(
      component,
      rows.length === 0,
      NO_DOCUMENTS_TO_SIGN_MESSAGE,
    );
  }

  /** Anything the form author set wins, per property, so a form can deviate on one and keep the rest. */
  private initializeRowLinkColumn(
    component: ExtendedComponentSchema,
    taak: GeneratedType<"RestTask">,
    parentZacType?: string,
    { asIcon = false }: { asIcon?: boolean } = {},
  ) {
    const zacType = asIcon
      ? KNOWN_ZAC_FIELDS.REGEL_LINK_VIEW_ICON
      : KNOWN_ZAC_FIELDS.REGEL_LINK;
    const rowLink = parentZacType ? ROW_LINKS[parentZacType] : undefined;
    if (!rowLink) {
      throw new Error(
        `No row link registered for parent "${parentZacType}". A ${zacType} ` +
          `column takes its route from the datagrid holding it.`,
      );
    }

    const linkText: string = this.translateService.instant(rowLink.textKey);

    component.tag ||= "a";
    component.content ||= asIcon ? VIEW_ICON_CONTENT : linkText;

    if (Array.isArray(component.attrs) && component.attrs.length) return;

    component.attrs = [
      { attr: "href", value: rowLink.href(taak) },
      { attr: "target", value: "_blank" },
      { attr: "rel", value: "noopener noreferrer" },
      ...(asIcon
        ? [
            { attr: "aria-label", value: linkText },
            { attr: "title", value: linkText },
          ]
        : []),
    ];
  }

  /**
   * An empty datagrid still draws its column headers, so mark it for the stylesheet to hide and say
   * why instead. The form author's class and description are restored once the field fills up again.
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
   * The rows stored by the selection task are trusted for their uuids only: titles and signing state
   * are re-read, dropping anything signed in the meantime so it cannot be offered for signing twice.
   */
  private async initializeSelectedUnsignedDocumentsDatagrid(
    component: ExtendedComponentSchema,
    taak: GeneratedType<"RestTask">,
  ): Promise<void> {
    if (this.isFinished(taak)) {
      await this.renderSigningResult(component, taak);
      return;
    }

    const selectedUuids = component.refreshOn
      ? this.getSelectedRows(taak, component.refreshOn)
          .map((row) => row.uuid)
          .filter((uuid): uuid is string => Boolean(uuid))
      : [];

    if (!selectedUuids.length) {
      component.defaultValue = [];
      this.applyEmptyState(component, true, NO_DOCUMENTS_TO_SIGN_MESSAGE);
      return;
    }

    const documents = await this.fetchZaakDocuments(taak, selectedUuids);
    const rows = documents
      .filter((document) => !document.ondertekening)
      .map((document) => this.toDocumentRow(document, false));

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
  ): DocumentRow {
    return {
      selected,
      titel: document.titel,
      uuid: document.uuid,
    };
  }

  private getStoredRows(taak: GeneratedType<"RestTask">, key: string) {
    const rows = taak.taakdata?.[key];
    return Array.isArray(rows) ? (rows as DocumentRow[]) : [];
  }

  private getSelectedRows(
    taak: GeneratedType<"RestTask">,
    refreshOnKey: string,
  ) {
    return this.getStoredRows(taak, refreshOnKey).filter((row) => row.selected);
  }

  private fetchZaakDocuments(
    taak: GeneratedType<"RestTask">,
    informatieobjectUUIDs?: string[],
  ) {
    // The uuids belong in the key, or a filtered fetch collides with the full list of the same zaak.
    return this.queryClient.fetchQuery({
      queryKey: [
        "availableDocumentsQuery",
        taak.zaakUuid,
        informatieobjectUUIDs && [...informatieobjectUUIDs].sort(),
      ],
      queryFn: () =>
        lastValueFrom(
          this.informatieObjectenService.listEnkelvoudigInformatieobjecten({
            zaakUUID: taak.zaakUuid,
            informatieobjectUUIDs,
          }),
        ),
      staleTime: 0,
    });
  }

  private initializeZaakResultField(
    component: ExtendedComponentSchema,
    taak: GeneratedType<"RestTask">,
  ) {
    component.valueProperty = "naam";
    component.template = "{{ item.naam }}";
    component.data = {
      custom: () =>
        this.queryClient.ensureQueryData(
          this.zacQueryClient.GET("/rest/zaken/resultaattypes/{zaaktypeUUID}", {
            path: { zaaktypeUUID: taak.zaaktypeUUID! },
          }),
        ),
    };
  }

  private initializeZaakStatusField(
    component: ExtendedComponentSchema,
    taak: GeneratedType<"RestTask">,
  ) {
    component.valueProperty = "naam";
    component.template = "{{ item.naam }}";
    component.data = {
      custom: () =>
        this.queryClient.ensureQueryData(
          this.zakenService.listStatustypes(taak.zaaktypeUUID!),
        ),
    };
  }
}
