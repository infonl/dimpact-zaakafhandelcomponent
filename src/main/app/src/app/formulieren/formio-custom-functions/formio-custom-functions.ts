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

// Each factory receives the taakdata and the specific parameter names found in the form,
// pre-fetches async data, and returns a synchronous closure for form.io's {{ }} evaluator.
type FormioFunctionFactory = (
  taakdata: Record<string, unknown>,
  parameters: string[],
) => Promise<(uuids: unknown) => string>;

/**
 * Form.io interpolates `{{ }}` straight into the page and its `{{{ }}}` escape delimiter does not
 * work in this build, so a designer has no way to make a value safe. Escaping is not an option
 * either: the same values are seeded into inputs by `customDefaultValue`, where `&amp;` would show
 * up literally. Markup is therefore removed rather than escaped, which leaves the `&` of
 * "Jansen & Zn" and the apostrophe of "'s-Hertogenbosch" intact.
 */
function stripTagsDeep<T>(value: T): T {
  if (typeof value === "string") return value.replace(/<[^>]*>/g, "") as T;
  if (Array.isArray(value)) return value.map(stripTagsDeep) as T;
  if (value !== null && typeof value === "object") {
    return Object.fromEntries(
      Object.entries(value).map(([key, entry]) => [key, stripTagsDeep(entry)]),
    ) as T;
  }
  return value;
}

/**
 * `{{ zaak.behandelaar.naam }}` is a correct expression on a zaak that has no behandelaar, but
 * Form.io evaluates it as JavaScript, so reading through the absent object throws and the whole
 * field fails to render. Every value handed to a form is therefore wrapped: an absent property
 * yields a stand-in that is safe to read on through and renders as nothing.
 */
/**
 * The path a value was reached by, carried on the value itself. A table of keys is only useful if
 * the reader can copy a row straight into an expression, and that needs the path the keys hang off,
 * which the helper receiving the value would otherwise have no way of knowing.
 */
const PATH = Symbol("path");

const PRIMITIVE_HOOKS: (string | symbol)[] = [
  Symbol.toPrimitive,
  "toString",
  "valueOf",
];

/**
 * Only a plain named property is stood in for. A symbol stays absent, or a `for...of` would loop on
 * the stand-in, and `then` stays absent, or every value would look like a promise and awaiting one
 * would hang.
 */
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

/** The path a wrapped value was reached by, or undefined for anything not wrapped. */
function pathOf(value: unknown) {
  return (value as Record<symbol, string> | null)?.[PATH];
}

/**
 * A property the object does not have at all is a typo, and nothing on screen says so any more. A
 * property that is present but empty is ordinary data - an unassigned zaak has no behandelaar - and
 * stays quiet.
 */
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

  // The Angular pipes are reused rather than reimplemented, so a value in a task form reads exactly
  // as the same value does elsewhere in ZAC - including the non-breaking hyphens in a date.
  private readonly datumPipe = new DatumPipe(inject(LOCALE_ID));
  private readonly emptyPipe = new EmptyPipe();

  /**
   * Pure helpers, always in the context: `{{ ZAC_formatter_datum(zaak.startdatum) }}`. Unlike the registry below
   * they need no pre-fetch, and unlike it they take a value rather than a field key, which the
   * registry's parameter scan cannot express.
   */
  private readonly templateHelpers = {
    ZAC_formatter_datum: (value: unknown) =>
      String(this.datumPipe.transform(value as string) ?? ""),

    /** `{{ ZAC_formatter_leeg(zaak.toelichting) }}` - a dash where the value is empty. */
    ZAC_formatter_leeg: (value: unknown) => this.emptyPipe.transform(value),

    ZAC_formatter_jaNee: (value: unknown) =>
      this.translateService.instant(
        value === true || value === "true" ? "actie.ja" : "actie.nee",
      ),

    /**
     * `customConditional: "show = bestaat(zaak.zaakdata.PD_x)"` - whether a property is really
     * there. A plain truthiness test cannot be used: an absent property yields a stand-in object so
     * that reading on through it is safe, and every object is truthy. The stand-in renders as the
     * empty string, which is what this leans on, so a present-but-empty value counts as absent too.
     */
    bestaat: (value: unknown) => String(value ?? "") !== "",

    /** `{{ ZAC_formatter_lijst(zaak.kenmerken, "kenmerk") }}` - one property of every element of a list. */
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

    /**
     * `{{ ZAC_formatter_sleutels(zaak.zaakdata, "where it comes from") }}` - every key of an object whose keys are
     * not known in advance, boxed with a caption. A nested object or list is counted rather than
     * expanded; address it with its own expression. The caption is optional and is the only place a
     * reader learns which system produced these keys, so it is worth filling in.
     */
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
