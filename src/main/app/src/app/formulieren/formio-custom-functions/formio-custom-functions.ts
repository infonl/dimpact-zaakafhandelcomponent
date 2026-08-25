/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { inject, Injectable, LOCALE_ID } from "@angular/core";
import { TranslateService } from "@ngx-translate/core";
import { lastValueFrom } from "rxjs";
import { InformatieObjectenService } from "../../informatie-objecten/informatie-objecten.service";
import { DatumPipe } from "../../shared/pipes/datum.pipe";
import { EmptyPipe } from "../../shared/pipes/empty.pipe";

type EvalContext = Record<string, unknown>;

// Pre-fetches its async data, then returns a synchronous closure for form.io's {{ }} evaluator.
type FormioFunctionFactory = (
  taakdata: Record<string, unknown>,
  parameters: string[],
) => Promise<(uuids: unknown) => string>;

// Removing a tag can splice a new one together, as in `<scr<x>ipt>`, so repeat until nothing changes.
function stripTags(value: string) {
  let stripped = value;
  for (let previous = ""; stripped !== previous; ) {
    previous = stripped;
    stripped = stripped.replace(/<[^>]*>/g, "");
  }
  return stripped.replace(/[<>]/g, "");
}

/** Removed rather than escaped: the same values are seeded into inputs, where `&amp;` would show. */
function stripTagsDeep<T>(value: T): T {
  if (typeof value === "string") return stripTags(value) as T;
  if (Array.isArray(value)) return value.map(stripTagsDeep) as T;
  if (value !== null && typeof value === "object") {
    return Object.fromEntries(
      Object.entries(value).map(([key, entry]) => [key, stripTagsDeep(entry)]),
    ) as T;
  }
  return value;
}

/** Carried on the value so `sleutels` can spell its keys out in full. */
const PATH = Symbol("path");

const PRIMITIVE_HOOKS: (string | symbol)[] = [
  Symbol.toPrimitive,
  "toString",
  "valueOf",
];

/** `then` and symbols stay absent, or a value would look thenable and `for...of` would loop. */
function standsInFor(property: string | symbol): property is string {
  return typeof property === "string" && property !== "then";
}

const ABSENT: Record<string | symbol, unknown> = new Proxy(
  {},
  {
    get(_target, property) {
      if (PRIMITIVE_HOOKS.includes(property)) return () => "";
      return standsInFor(property) ? ABSENT : undefined;
    },
  },
);

/** Form.io re-renders a form many times over, so each unknown property is reported once. */
const reportedPaths = new Set<string>();

function pathOf(value: unknown) {
  return (value as Record<symbol, string> | null)?.[PATH];
}

/** Absent-but-present is ordinary data (an unassigned zaak has no behandelaar), so it stays quiet. */
function reportUnknownProperty(target: object, property: string, path: string) {
  if (Array.isArray(target) && /^\d+$/.test(property)) return;
  const reported = `${path}.${property}`;
  if (reportedPaths.has(reported)) return;
  reportedPaths.add(reported);
  console.warn(
    `[FormioCustomFunctions] "${reported}" is not a property of the ` +
      `${path.split(".")[0]}. It renders as empty.`,
  );
}

function readableThrough<T>(value: T, path: string): T {
  if (value === null || value === undefined) return ABSENT as T;
  if (typeof value !== "object") return value;
  return new Proxy(value as object, {
    get(target, property, receiver) {
      if (property === PATH) return path;

      const read = Reflect.get(target, property, receiver);
      if (read === null || read === undefined) {
        if (!standsInFor(property)) return read;
        if (!(property in target)) {
          reportUnknownProperty(target, property, path);
        }
        return ABSENT as unknown;
      }
      return typeof read === "object"
        ? readableThrough(read, `${path}.${String(property)}`)
        : read;
    },
  }) as T;
}

@Injectable({ providedIn: "root" })
export class FormioCustomFunctions {
  private readonly informatieObjectenService = inject(
    InformatieObjectenService,
  );
  private readonly translateService = inject(TranslateService);

  // Reused, not reimplemented, so a form renders a value exactly as the rest of ZAC does.
  private readonly datumPipe = new DatumPipe(inject(LOCALE_ID));
  private readonly emptyPipe = new EmptyPipe();

  // Unlike the registry below these take a value rather than a field key, and need no pre-fetch.
  private readonly templateHelpers = {
    ZAC_formatter_datum: (value: unknown) =>
      String(this.datumPipe.transform(value as string) ?? ""),

    ZAC_formatter_leeg: (value: unknown) => this.emptyPipe.transform(value),

    ZAC_formatter_jaNee: (value: unknown) =>
      this.translateService.instant(
        value === true || value === "true" ? "actie.ja" : "actie.nee",
      ),

    ZAC_formatter_lijst: (values: unknown, property?: string) =>
      (Array.isArray(values) ? values : [])
        .map((element) =>
          property && element !== null && typeof element === "object"
            ? (element as Record<string, unknown>)[property]
            : element,
        )
        .filter(
          (element) =>
            element !== null && element !== undefined && element !== "",
        )
        .join(", "),

    /** Every key of an object whose keys are not known in advance; nested values are counted. */
    ZAC_formatter_sleutels: (source: unknown, caption?: string) => {
      const entries = Object.entries(
        source !== null && typeof source === "object" ? source : {},
      ).sort(([left], [right]) => left.localeCompare(right));
      const path = pathOf(source);

      const header = caption
        ? `<div class="card-header py-1 px-2 small text-body-secondary">${caption}</div>`
        : "";
      if (!entries.length) {
        return (
          `<div class="card mb-2">${header}<div class="card-body py-1 px-2 small ` +
          `text-body-secondary"><em>no keys</em></div></div>`
        );
      }

      const rows = entries
        .map(([key, value]) => {
          const rendered = Array.isArray(value)
            ? `[${value.length}]`
            : value !== null && typeof value === "object"
              ? `{${Object.keys(value).length}}`
              : String(value);
          return (
            `<tr><th scope="row" class="fw-normal text-nowrap">` +
            `<code>${path ? `${path}.${key}` : key}</code></th>` +
            `<td>${rendered}</td></tr>`
          );
        })
        .join("");
      return (
        `<div class="card mb-2">${header}` +
        `<table class="table table-sm mb-0"><tbody>${rows}</tbody></table></div>`
      );
    },
  };

  private readonly functionRegistry: Record<string, FormioFunctionFactory> = {
    ZAC_getDocumentTitles: async (taakdata, parameters) => {
      const documentUuids = parameters.flatMap((parameter) =>
        this.extractDocumentUuids(taakdata[parameter]),
      );
      const titleByUuid =
        await this.fetchInformatieObjectTitlesByUuid(documentUuids);

      const listFormat = new Intl.ListFormat("nl", {
        style: "long",
        type: "conjunction",
      });
      return (documents) =>
        listFormat.format(
          this.extractDocumentUuids(documents).map(
            (uuid) => titleByUuid.get(uuid) ?? uuid,
          ),
        );
    },
  };

  async prepareFormContext(
    form: unknown,
    taakdata: Record<string, unknown>,
    zaak?: object,
    taak?: object,
  ): Promise<EvalContext> {
    const foundFunctions = this.extractFormFunctions(form);

    for (const funcName of foundFunctions.keys()) {
      if (!(funcName in this.functionRegistry)) {
        console.warn(
          `[FormioCustomFunctions] Unknown function "{{ ${funcName}(...) }}" in form JSON. ` +
            `Known functions: ${Object.keys(this.functionRegistry).join(", ")}`,
        );
      }
    }

    const context: EvalContext = {
      ...taakdata,
      zaak: readableThrough(stripTagsDeep(zaak), "zaak"),
      taak: readableThrough(stripTagsDeep(taak), "taak"),
      ...this.templateHelpers,
    };
    for (const [funcName, factory] of Object.entries(this.functionRegistry)) {
      if (foundFunctions.has(funcName)) {
        context[funcName] = await factory(
          taakdata,
          foundFunctions.get(funcName) ?? [],
        );
      }
    }
    return context;
  }

  private extractFormFunctions(form: unknown): Map<string, string[]> {
    const result = new Map<string, string[]>();
    for (const [, funcName, parameter] of JSON.stringify(form ?? "").matchAll(
      /\{\{\s*(\w+)\((\w+)\)/g,
    )) {
      const params = result.get(funcName) ?? [];
      if (!params.includes(parameter)) params.push(parameter);
      result.set(funcName, params);
    }
    return result;
  }

  // Accepts a uuid, a list of uuids or datagrid rows, so both dropdown and datagrid fields work.
  private extractDocumentUuids(value: unknown): string[] {
    return (Array.isArray(value) ? value : [value]).flatMap((entry) => {
      if (typeof entry === "string") return [entry];
      if (!entry || typeof entry !== "object") return [];

      const { uuid, selected } = entry as {
        uuid?: unknown;
        selected?: unknown;
      };
      // An unticked row is not going to be signed, so it does not belong in the summary.
      return typeof uuid === "string" && selected !== false ? [uuid] : [];
    });
  }

  private async fetchInformatieObjectTitlesByUuid(
    uuids: string[],
  ): Promise<Map<string, string>> {
    const entries = await Promise.all(
      [...new Set(uuids)].map(async (uuid) => {
        try {
          const document = await lastValueFrom(
            this.informatieObjectenService.readEnkelvoudigInformatieobject(
              uuid,
            ),
          );
          return document?.titel ? ([uuid, document.titel] as const) : null;
        } catch {
          console.error(
            `[FormioCustomFunctions] Failed to fetch document with UUID ${uuid}`,
          );
          return null;
        }
      }),
    );
    return new Map(entries.filter((entry) => entry !== null));
  }
}
