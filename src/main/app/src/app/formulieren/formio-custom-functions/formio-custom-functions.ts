/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { inject, Injectable, LOCALE_ID } from "@angular/core";
import { TranslateService } from "@ngx-translate/core";
import { lastValueFrom } from "rxjs";
import { InformatieObjectenService } from "../../informatie-objecten/informatie-objecten.service";
import { DatumPipe } from "../../shared/pipes/datum.pipe";

type EvalContext = Record<string, unknown>;

// Pre-fetches its async data, then returns a synchronous closure for form.io's {{ }} evaluator.
type FormioFunctionFactory = (
  taakdata: Record<string, unknown>,
  parameters: string[],
) => Promise<(uuids: unknown) => string>;

function stripTags(value: string) {
  let stripped = value;
  for (let previous = ""; stripped !== previous;) {
    previous = stripped;
    stripped = stripped.replace(/<[^>]*>/g, "");
  }
  return stripped.replace(/[<>]/g, "");
}

function isPlainObject(value: unknown): value is Record<string, unknown> {
  if (value === null || typeof value !== "object") return false;
  const prototype = Object.getPrototypeOf(value);
  return prototype === Object.prototype || prototype === null;
}

function stripTagsDeep<T>(value: T): T {
  if (typeof value === "string") return stripTags(value) as T;
  if (Array.isArray(value)) return value.map(stripTagsDeep) as T;
  if (isPlainObject(value)) {
    return Object.fromEntries(
      Object.entries(value).map(([key, entry]) => [key, stripTagsDeep(entry)]),
    ) as T;
  }
  return value;
}

const PRIMITIVE_HOOKS: (string | symbol)[] = [
  Symbol.toPrimitive,
  "toString",
  "valueOf",
];

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

function reportUnknownProperty(
  target: object,
  property: string,
  path: string,
  reportedPaths: Set<string>,
) {
  if (Array.isArray(target) && /^\d+$/.test(property)) return;
  const reported = `${path}.${property}`;
  if (reportedPaths.has(reported)) return;
  reportedPaths.add(reported);
  console.warn(
    `[FormioCustomFunctions] "${reported}" is not a property of the ` +
      `${path.split(".")[0]}. It renders as empty.`,
  );
}

function absentAsEmpty<T>(
  value: T,
  path: string,
  reportedPaths: Set<string>,
): T {
  if (value === null || value === undefined) return ABSENT as T;
  // A `Date` and the like carry internal state their methods read off `this`, which a proxy breaks.
  if (!isPlainObject(value) && !Array.isArray(value)) return value;
  return new Proxy(value as object, {
    get(target, property, receiver) {
      const read = Reflect.get(target, property, receiver);
      if (read === null || read === undefined) {
        if (!standsInFor(property)) return read;
        if (!(property in target)) {
          reportUnknownProperty(target, property, path, reportedPaths);
        }
        return ABSENT as unknown;
      }
      return typeof read === "object"
        ? absentAsEmpty(read, `${path}.${String(property)}`, reportedPaths)
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

  private readonly datumPipe = new DatumPipe(inject(LOCALE_ID));

  private readonly reportedPaths = new Set<string>();

  private readonly templateHelpers = {
    ZAC_opmaakDatum: (value: unknown) =>
      String(this.datumPipe.transform(value as string) ?? ""),

    ZAC_opmaakLegeWaarde: (value: unknown, whenEmpty?: string) => {
      const read = String(value ?? "");
      return read === ""
        ? this.translateService.instant(whenEmpty ?? "-")
        : read;
    },

    ZAC_opmaakBoolean: (
      value: unknown,
      whenTrue?: string,
      whenFalse?: string,
    ) => {
      // An absent property stands in as an object that reads as empty, so compare the rendered text.
      const read = String(value ?? "");
      if (read !== "true" && read !== "false") return read;
      return this.translateService.instant(
        read === "true" ? (whenTrue ?? "actie.ja") : (whenFalse ?? "actie.nee"),
      );
    },

    ZAC_opmaakLijst: (values: unknown, property?: string) =>
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

  asContextValue(value: object | undefined, path: string) {
    return absentAsEmpty(stripTagsDeep(value), path, this.reportedPaths);
  }

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
      zaak: this.asContextValue(zaak, "zaak"),
      taak: this.asContextValue(taak, "taak"),
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
