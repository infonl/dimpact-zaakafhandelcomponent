/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { inject, Injectable } from "@angular/core";
import { lastValueFrom } from "rxjs";
import { InformatieObjectenService } from "../../informatie-objecten/informatie-objecten.service";

type EvalContext = Record<string, unknown>;
type FormNode = string | unknown[] | Record<string, unknown> | null | undefined;

// Each factory pre-fetches async data and returns a synchronous closure for form.io's {{ }} evaluator.
type FormioFunctionFactory = (
  taakdata: Record<string, unknown>,
) => Promise<(uuids: unknown) => string>;

@Injectable({ providedIn: "root" })
export class FormioCustomFunctions {
  private readonly informatieObjectenService = inject(InformatieObjectenService);

  private readonly functionRegistry: Record<string, FormioFunctionFactory> = {
    getDocumentTitles: async (taakdata) => {
      const allUuids = Object.values(taakdata)
        .filter(Array.isArray)
        .flat() as string[];
      const titlesById = await this.fetchTitlesByUuid(allUuids);
      return (uuids: unknown) =>
        this.formatValues(
          (Array.isArray(uuids) ? (uuids as string[]) : []).map(
            (uuid) => titlesById.get(uuid) ?? uuid,
          ),
        );
    },
  };

  hasFunctionCalls(form: unknown): boolean {
    return this.extractFormFunctions(form as FormNode).size > 0;
  }

  async prepareFormContext(
    form: unknown,
    taakdata: Record<string, unknown>,
  ): Promise<EvalContext> {
    const foundFunctions = this.extractFormFunctions(form as FormNode);

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
        context[funcName] = await factory(taakdata);
      }
    }
    return context;
  }

  // ── Private helpers ──────────────────────────────────────────────────────────

  private extractFormFunctions(
    formNode: FormNode,
    functionCallsByName = new Map<string, string[]>(),
  ): Map<string, string[]> {
    if (typeof formNode === "string") {
        // Add ZAC_ to regex
      const functionCallPattern = /\{\{\s*(\w+)\((\w+)\)/g;
      let regexMatch: RegExpExecArray | null;
      while ((regexMatch = functionCallPattern.exec(formNode)) !== null) {
        const [, funcName, parameter] = regexMatch;
        const existingParams = functionCallsByName.get(funcName) ?? [];
        if (!existingParams.includes(parameter)) existingParams.push(parameter);
        functionCallsByName.set(funcName, existingParams);
      }
    } else if (Array.isArray(formNode)) {
      for (const arrayElement of formNode)
        this.extractFormFunctions(arrayElement as FormNode, functionCallsByName);
    } else if (formNode !== null && typeof formNode === "object") {
      for (const nestedValue of Object.values(formNode))
        this.extractFormFunctions(nestedValue as FormNode, functionCallsByName);
    }
    return functionCallsByName;
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
