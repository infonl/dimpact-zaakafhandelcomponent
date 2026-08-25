/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { inject, Injectable } from "@angular/core";
import { TranslateService } from "@ngx-translate/core";
import { lastValueFrom } from "rxjs";
import { InformatieObjectenService } from "../../informatie-objecten/informatie-objecten.service";

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
function standsInFor(property: string | symbol) {
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
    `[FormioCustomFunctions] "${reported}" is not a property of the ${path.split(".")[0]}. ` +
      `It renders as empty. Known here: ${Object.keys(target).sort().join(", ")}`,
  );
}

function readableThrough<T>(value: T, path: string): T {
  if (value === null || value === undefined) return ABSENT as T;
  if (typeof value !== "object") return value;
  return new Proxy(value as object, {
    get(target, property, receiver) {
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

  /**
   * Pure helpers, always in the context: `{{ datum(zaak.startdatum) }}`. Unlike the registry below
   * they need no pre-fetch, and unlike it they take a value rather than a field key, which the
   * registry's parameter scan cannot express.
   */
  private readonly templateHelpers = {
    datum: (value: unknown) => {
      const parts = /^(\d{4})-(\d{2})-(\d{2})/.exec(String(value));
      return parts ? `${parts[3]}-${parts[2]}-${parts[1]}` : String(value);
    },
    jaNee: (value: unknown) =>
      this.translateService.instant(
        value === true || value === "true" ? "actie.ja" : "actie.nee",
      ),

    /** `{{ lijst(zaak.kenmerken, "kenmerk") }}` - one property of every element of a list. */
    lijst: (values: unknown, property?: string) =>
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
     * `{{ sleutels(zaak.zaakdata) }}` - every key of an object whose keys are not known in advance.
     * A nested object or list is counted rather than expanded; address it with its own path.
     */
    sleutels: (source: unknown) => {
      const entries = Object.entries(
        source !== null && typeof source === "object" ? source : {},
      ).sort(([left], [right]) => left.localeCompare(right));
      if (!entries.length) return "";

      const rows = entries
        .map(([key, value]) => {
          const rendered = Array.isArray(value)
            ? `[${value.length}]`
            : value !== null && typeof value === "object"
              ? `{${Object.keys(value).length}}`
              : String(value);
          return (
            `<tr><th scope="row" class="fw-normal text-nowrap"><code>${key}</code></th>` +
            `<td>${rendered}</td></tr>`
          );
        })
        .join("");
      return `<table class="table table-sm table-bordered mb-0"><tbody>${rows}</tbody></table>`;
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
