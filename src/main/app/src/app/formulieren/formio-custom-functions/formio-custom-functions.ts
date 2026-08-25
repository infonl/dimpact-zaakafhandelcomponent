/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { inject, Injectable } from "@angular/core";
import { TranslateService } from "@ngx-translate/core";
import { lastValueFrom } from "rxjs";
import { InformatieObjectenService } from "../../informatie-objecten/informatie-objecten.service";

type EvalContext = Record<string, unknown>;

/**
 * Form.io interpolates `{{ }}` straight into the page and its `{{{ }}}` escape delimiter does not
 * work in this build, so a designer has no way to escape a value. Zaak and taak values are entered
 * by users, so they are escaped here instead - once, before the form can ever reach them.
 */
function escapeDeep<T>(value: T): T {
  if (typeof value === "string") {
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
    ) as T;
  }
  if (Array.isArray(value)) return value.map(escapeDeep) as T;
  if (value !== null && typeof value === "object") {
    return Object.fromEntries(
      Object.entries(value).map(([key, entry]) => [key, escapeDeep(entry)]),
    ) as T;
  }
  return value;
}

// Each factory receives the taakdata and the specific parameter names found in the form,
// pre-fetches async data, and returns a synchronous closure for form.io's {{ }} evaluator.
type FormioFunctionFactory = (
  taakdata: Record<string, unknown>,
  parameters: string[],
) => Promise<(uuids: unknown) => string>;

@Injectable({ providedIn: "root" })
export class FormioCustomFunctions {
  private readonly informatieObjectenService = inject(
    InformatieObjectenService,
  );
  private readonly translateService = inject(TranslateService);

  /**
   * Pure display helpers, always in the context: `{{ datum(zaak.startdatum) }}`. Unlike the
   * registry below they need no pre-fetch, and unlike it they take a path rather than a field key,
   * which the registry's parameter scan cannot express.
   */
  private readonly valueFormatters = {
    datum: (value: unknown) => {
      const parts = /^(\d{4})-(\d{2})-(\d{2})/.exec(String(value));
      return parts ? `${parts[3]}-${parts[2]}-${parts[1]}` : String(value);
    },
    jaNee: (value: unknown) =>
      this.translateService.instant(
        value === true || value === "true" ? "actie.ja" : "actie.nee",
      ),
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
      zaak: escapeDeep(zaak),
      taak: escapeDeep(taak),
      ...this.valueFormatters,
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
