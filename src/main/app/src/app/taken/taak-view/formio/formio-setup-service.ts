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
  REGEL_LINK = "ZAC_regel_link_tekstueel",
  REGEL_LINK_VIEW_ICON = "ZAC_regel_link_oog_icoon",
  RESULTAAT = "ZAC_resultaat",
  STATUS = "ZAC_status",
  PROCESS_DATA = "ZAC_process_data",
  ZAAK_GEGEVENS = "ZAC_zaak_gegevens",
  TAAK_GEGEVENS = "ZAC_taak_gegevens",
}

/** Names the property a gegevens field shows, as a dot path: `zaaktype.omschrijving`. */
const ZAC_PATH_PROPERTY = "ZAC_VELD";

/** Names an optional format function wrapped around the value: `ZAC_FORMAAT: "datum"`. */
const ZAC_FORMAT_PROPERTY = "ZAC_FORMAAT";

const ISO_DATE = /^(\d{4})-(\d{2})-(\d{2})/;

/** Shown when a property holds no value, so an empty field reads as empty rather than as broken. */
const NO_VALUE = "—";

/**
 * Wrappers a form may put around a zaak value. Without one the value is shown as the zaak holds it,
 * so a format is a deliberate choice by the form author rather than something guessed from the type.
 */
const VALUE_FORMATTERS: Record<
  string,
  (value: unknown, translate: TranslateService) => string
> = {
  datum: (value) => {
    const parts = ISO_DATE.exec(String(value));
    return parts ? `${parts[3]}-${parts[2]}-${parts[1]}` : String(value);
  },
  jaNee: (value, translate) =>
    translate.instant(value ? "actie.ja" : "actie.nee"),
};

/**
 * Renders every key of an object instead of a single value, so a form can show what a zaak or taak
 * actually carries without naming each key. Handled apart from [VALUE_FORMATTERS] because it emits
 * markup: a formatter returning a string would have its table escaped away.
 */
const TABLE_FORMAT = "tabel";

/**
 * Set to `true` to seed the value into the field the author declared, leaving it editable and part
 * of the submission, instead of rendering it as read-only text.
 */
const ZAC_INPUT_PROPERTY = "ZAC_INVOER";

/** Component types that parse their own value, so they need it raw rather than formatted. */
const VALUE_PARSING_TYPES = ["datetime", "date", "day", "time"];

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

const LIST_SEGMENT_SUFFIX = "[]";

const LIST_SEPARATOR = ", ";

/**
 * Walks a dot path into `source`, naming it `sourceLabel` in any error. A property that is absent
 * resolves to null rather than to an error: the API omits properties that hold no value, so absence
 * cannot be told apart from a typo.
 *
 * A segment may end in `[]` to read on through every element of a list, as in
 * `kenmerken[].kenmerk`, which resolves to the kenmerk of each row.
 */
function resolvePath(
  source: object,
  path: string,
  sourceLabel: string,
): unknown {
  if (!path) {
    throw new Error(`Missing ${ZAC_PATH_PROPERTY} property.`);
  }

  const value = walkSegments(source, path.split("."), [], sourceLabel);

  if (Array.isArray(value)) {
    const objectElement = value.find(
      (element) => typeof element === "object" && element !== null,
    );
    if (objectElement) {
      throw new Error(
        `${sourceLabel} property "${path}" holds a list of objects. Address a property of ` +
          `each element, as in "${path}${LIST_SEGMENT_SUFFIX}.` +
          `${Object.keys(objectElement).sort()[0]}".`,
      );
    }
    return value;
  }

  if (typeof value === "object" && value !== null) {
    throw new Error(
      `${sourceLabel} property "${path}" holds an object, not a single value.`,
    );
  }
  return value;
}

function resolveObjectPath(
  source: object,
  path: string,
  sourceLabel: string,
): Record<string, unknown> | null {
  if (!path) {
    throw new Error(`Missing ${ZAC_PATH_PROPERTY} property.`);
  }

  const value = walkSegments(source, path.split("."), [], sourceLabel);
  if (value === null || value === undefined) return null;
  if (typeof value !== "object") {
    throw new Error(
      `${sourceLabel} property "${path}" holds a single value, so it has no ` +
        `keys to tabulate. Drop ${ZAC_FORMAT_PROPERTY} "${TABLE_FORMAT}".`,
    );
  }
  return value as Record<string, unknown>;
}

function walkSegments(
  start: unknown,
  segments: string[],
  walked: string[],
  sourceLabel: string,
): unknown {
  let value = start;
  for (let index = 0; index < segments.length; index++) {
    if (value === null || value === undefined) return null;

    const segment = segments[index];
    const isList = segment.endsWith(LIST_SEGMENT_SUFFIX);
    const name = isList
      ? segment.slice(0, -LIST_SEGMENT_SUFFIX.length)
      : segment;

    if (typeof value !== "object") {
      throw new Error(
        `${sourceLabel} property "${walked.join(".")}" holds a single value, ` +
          `so "${name}" cannot be read from it.`,
      );
    }
    value = (value as Record<string, unknown>)[name];
    walked.push(name);

    if (!isList) continue;

    if (value === null || value === undefined) return null;
    if (!Array.isArray(value)) {
      throw new Error(
        `${sourceLabel} property "${walked.join(".")}" is not a list, ` +
          `so "${LIST_SEGMENT_SUFFIX}" cannot be used on it.`,
      );
    }
    const rest = segments.slice(index + 1);
    return value.map((element) =>
      rest.length
        ? walkSegments(element, rest, [...walked], sourceLabel)
        : element,
    );
  }
  return value;
}

/** A calendar widget can be put on other types too, so the widget is checked as well as the type. */
function parsesItsOwnValue(component: ExtendedComponentSchema) {
  return (
    VALUE_PARSING_TYPES.includes(String(component.type)) ||
    (component.widget as { type?: string } | undefined)?.type === "calendar"
  );
}

function isEmptyValue(value: unknown): boolean {
  return (
    value === null ||
    value === undefined ||
    value === "" ||
    (Array.isArray(value) &&
      !value.filter((element) => !isEmptyValue(element)).length)
  );
}

/** Form.io stores component properties as strings, so `"true"` has to count as true. */
function isTruthyProperty(value: unknown) {
  return value === true || value === "true";
}

function escapeHtml(value: string) {
  return value.replace(
    /[&<>"']/g,
    (character) =>
      ({
        "&": "&amp;",
        "<": "&lt;",
        ">": "&gt;",
        '"': "&quot;",
        "'": "&#39;",
      })[character]!,
  );
}

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
            case KNOWN_ZAC_FIELDS.ZAAK_GEGEVENS:
              this.initializeGegevensField(component, zaak, "Zaak", taak);
              break;
            case KNOWN_ZAC_FIELDS.TAAK_GEGEVENS:
              this.initializeGegevensField(component, taak, "Taak", taak);
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

    this.renderFieldError(component, `Undefined ZAC_TYPE: '${zacType}'`);
  }

  /**
   * Shown in place of the field rather than as a snackbar: a form authoring mistake belongs next to
   * the field that carries it, and a transient message would leave the author staring at a blank one.
   */
  private renderFieldError(
    component: ExtendedComponentSchema,
    message: string,
  ) {
    component.type = "content";
    component.label = "";
    component.input = false;
    component.html = `<div class="zac-unknown-zac-type">${escapeHtml(message)}</div>`;
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

  /**
   * A layout component holds its children under `components`, but a table nests them per cell in
   * `rows` and a columns component in `columns`, so those need walking too or their fields are
   * left uninitialized.
   */
  private getChildComponents(
    fieldsetComponent: ExtendedComponentSchema,
  ): ExtendedComponentSchema[] {
    const children = Array.isArray(fieldsetComponent.components)
      ? Array.from(fieldsetComponent.components)
      : [];

    const cells: ExtendedComponentSchema[] = [
      ...(Array.isArray(fieldsetComponent.rows)
        ? fieldsetComponent.rows.flat()
        : []),
      ...(Array.isArray(fieldsetComponent.columns)
        ? fieldsetComponent.columns
        : []),
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

  /**
   * Rendered as content rather than as an input: an input lands in the submission, and completing a
   * task writes every submitted key back as a process variable — which would overwrite the zaak
   * variables with whatever this read-only field happened to show.
   */
  private initializeGegevensField(
    component: ExtendedComponentSchema,
    source: object,
    sourceLabel: string,
    taak: GeneratedType<"RestTask">,
  ) {
    const path: string = component.properties?.[ZAC_PATH_PROPERTY] ?? "";
    const format: string | undefined =
      component.properties?.[ZAC_FORMAT_PROPERTY];
    const label = component.label ?? "";

    if (isTruthyProperty(component.properties?.[ZAC_INPUT_PROPERTY])) {
      this.seedInputField(component, source, sourceLabel, taak, path, format);
      return;
    }

    let body: string;
    try {
      body =
        format === TABLE_FORMAT
          ? this.renderKeyValueTable(
              resolveObjectPath(source, path, sourceLabel),
            )
          : `<span class="zac-gegevens-label">${label ? `${escapeHtml(label)}: ` : ""}</span>` +
            `<span class="zac-gegevens-value">${escapeHtml(
              this.formatValue(resolvePath(source, path, sourceLabel), format),
            )}</span>`;
    } catch (error) {
      this.renderFieldError(
        component,
        error instanceof Error ? error.message : String(error),
      );
      return;
    }

    const heading =
      format === TABLE_FORMAT && label
        ? `<h4 class="zac-gegevens-heading">${escapeHtml(label)}</h4>`
        : "";

    component.type = "content";
    component.input = false;
    component.html = `<div class="zac-gegevens">${heading}${body}</div>`;
    component.label = "";
  }

  /**
   * Leaves the component as the author declared it — a textfield stays an editable textfield — and
   * puts the resolved value in as its value. The value has to go into the task data as well, because
   * Form.io prefers submission data over `defaultValue`; a value already stored there is left alone,
   * so reopening a saved task shows what the user answered rather than re-seeding over it.
   */
  private seedInputField(
    component: ExtendedComponentSchema,
    source: object,
    sourceLabel: string,
    taak: GeneratedType<"RestTask">,
    path: string,
    format?: string,
  ) {
    try {
      if (format === TABLE_FORMAT) {
        throw new Error(
          `${ZAC_FORMAT_PROPERTY} "${TABLE_FORMAT}" renders a table, which cannot be put into ` +
            `an input. Drop ${ZAC_INPUT_PROPERTY} or drop the format.`,
        );
      }
      if (format && parsesItsOwnValue(component)) {
        throw new Error(
          `A "${component.type}" field parses its own value and formats it for display, so a ` +
            `formatted value cannot be read. Drop ${ZAC_FORMAT_PROPERTY} "${format}".`,
        );
      }
      const resolved = resolvePath(source, path, sourceLabel);
      // NO_VALUE is a marker for a reader; seeding it would hand a date picker the string "—"
      if (isEmptyValue(resolved)) return;

      const value = this.formatValue(resolved, format);
      component.defaultValue = value;

      const stored = taak.taakdata?.[component.key];
      if (
        taak.taakdata &&
        (stored === undefined || stored === null || stored === "")
      ) {
        taak.taakdata[component.key] = value;
      }
    } catch (error) {
      this.renderFieldError(
        component,
        error instanceof Error ? error.message : String(error),
      );
    }
  }

  /**
   * Keys are sorted so the same object always renders in the same order. A value that is itself an
   * object or a list is named rather than expanded: nesting a whole zaak inside a cell reads as
   * noise, and the form can address it with its own path.
   */
  private renderKeyValueTable(source: Record<string, unknown> | null) {
    const entries = Object.entries(source ?? {}).sort(([left], [right]) =>
      left.localeCompare(right),
    );
    if (!entries.length) {
      return `<span class="zac-gegevens-value">${NO_VALUE}</span>`;
    }

    const rows = entries
      .map(([key, value]) => {
        const rendered = Array.isArray(value)
          ? `[${value.length}]`
          : value !== null && typeof value === "object"
            ? `{${Object.keys(value).length}}`
            : this.formatValue(value);
        return (
          `<tr><th scope="row"><code>${escapeHtml(key)}</code></th>` +
          `<td>${escapeHtml(rendered)}</td></tr>`
        );
      })
      .join("");
    return `<table class="zac-gegevens-tabel"><tbody>${rows}</tbody></table>`;
  }

  private formatValue(value: unknown, format?: string): string {
    if (Array.isArray(value)) {
      const elements = value
        .filter(
          (element) =>
            element !== null && element !== undefined && element !== "",
        )
        .map((element) => this.formatValue(element, format));
      return elements.length ? elements.join(LIST_SEPARATOR) : NO_VALUE;
    }
    if (value === null || value === undefined || value === "") return NO_VALUE;
    if (!format) return String(value);

    const formatter = VALUE_FORMATTERS[format];
    if (!formatter) {
      throw new Error(
        `Unknown ${ZAC_FORMAT_PROPERTY} "${format}". Available: ` +
          Object.keys(VALUE_FORMATTERS).sort().join(", "),
      );
    }
    return formatter(value, this.translateService);
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
