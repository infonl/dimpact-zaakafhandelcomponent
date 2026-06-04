/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { inject, Injectable } from "@angular/core";
import { lastValueFrom } from "rxjs";
import { InformatieObjectenService } from "../../informatie-objecten/informatie-objecten.service";

type EvalContext = Record<string, unknown>;

// Each factory receives the taakdata and the specific parameter names found in the form,
// pre-fetches async data, and returns a synchronous closure for form.io's {{ }} evaluator.
type FormioFunctionFactory = (
  taakdata: Record<string, unknown>,
  parameters: string[],
) => Promise<(uuids: unknown) => string>;

@Injectable({ providedIn: "root" })
export class FormioCustomFunctions {
  private readonly informatieObjectenService = inject(InformatieObjectenService);

  private readonly functionRegistry: Record<string, FormioFunctionFactory> = {
    getDocumentTitles: async (taakdata, parameters) => {
      const documentUuids = parameters.flatMap((parameter) => {
        const fieldValue = taakdata[parameter];
        return Array.isArray(fieldValue) ? (fieldValue as string[]) : [];
      });
      const titlesById = await this.fetchTitlesByUuid(documentUuids);
      return (uuids: unknown) =>
        this.formatValues(
          (Array.isArray(uuids) ? (uuids as string[]) : []).map(
            (uuid) => titlesById.get(uuid) ?? uuid,
          ),
        );
    },
  };

  hasFunctionCalls(form: unknown): boolean {
    return this.extractFormFunctions(form).size > 0;
  }

  async prepareFormContext(
    form: unknown,
    taakdata: Record<string, unknown>,
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

    const context: EvalContext = { ...taakdata };
    for (const [funcName, factory] of Object.entries(this.functionRegistry)) {
      if (foundFunctions.has(funcName)) {
        context[funcName] = await factory(taakdata, foundFunctions.get(funcName) ?? []);
      }
    }
    return context;
  }

  // ── Private helpers ──────────────────────────────────────────────────────────

  private extractFormFunctions(form: unknown): Map<string, string[]> {
    const result = new Map<string, string[]>();
    for (const [, funcName, parameter] of JSON.stringify(form ?? "").matchAll(/\{\{\s*(\w+)\((\w+)\)/g)) {
      const params = result.get(funcName) ?? [];
      if (!params.includes(parameter)) params.push(parameter);
      result.set(funcName, params);
    }
    return result;
  }

  private async fetchTitlesByUuid(
    uuids: string[],
  ): Promise<Map<string, string>> {
    const titlesById = new Map<string, string>();
    await Promise.all(
      uuids.map(async (uuid) => {
        try {
          const document = await lastValueFrom(
            this.informatieObjectenService.readEnkelvoudigInformatieobject(uuid),
          );
          if (document?.titel) titlesById.set(uuid, document.titel);
        } catch {

        }
      }),
    );
    return titlesById;
  }

  private formatValues(values: string[]): string {
    if (values.length === 0) return "";
    if (values.length === 1) return values[0];
    return values.slice(0, -1).join(", ") + " en " + values[values.length - 1];
  }
}
