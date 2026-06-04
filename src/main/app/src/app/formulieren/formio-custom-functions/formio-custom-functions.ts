/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { inject, Injectable } from "@angular/core";
import { lastValueFrom } from "rxjs";
import { InformatieObjectenService } from "../../informatie-objecten/informatie-objecten.service";

export enum KNOWN_FORMIO_FUNCTIONS {
  GET_DOCUMENT_TITLE = "getDocumentTitles",
}

type EvalContext = Record<string, unknown>;
type FormNode = string | unknown[] | Record<string, unknown> | null | undefined;

@Injectable({ providedIn: "root" })
export class FormioCustomFunctions {
  private readonly informatieObjectenService = inject(InformatieObjectenService);

  hasFunctionCalls(form: unknown): boolean {
    return this.extractFormFunctions(form as FormNode).size > 0;
  }

  async buildEvalContext(
    form: unknown,
    taakdata: Record<string, unknown>,
  ): Promise<EvalContext> {
    const functionsContext: EvalContext = {};
    const functionCallsByName = this.extractFormFunctions(form as FormNode);

    for (const functionName of Object.values(KNOWN_FORMIO_FUNCTIONS)) {
      const zaakdataFields = functionCallsByName.get(functionName);
      if (!zaakdataFields?.length) continue;

      switch (functionName) {
        case KNOWN_FORMIO_FUNCTIONS.GET_DOCUMENT_TITLE: {
          const allUuids = zaakdataFields.flatMap((field) => {
            const fieldValue = taakdata[field];
            return Array.isArray(fieldValue) ? (fieldValue as string[]) : [];
          });
          const titlesById = await this.fetchTitlesById(allUuids);
          functionsContext[functionName] = (uuids: unknown) =>
            this.formatValues(
              (Array.isArray(uuids) ? (uuids as string[]) : []).map(
                (uuid) => titlesById.get(uuid) ?? uuid,
              ),
            );
          break;
        }
      }
    }

    return { ...taakdata, ...functionsContext };
  }

  private extractFormFunctions(
    formNode: FormNode,
    functionCallsByName = new Map<string, string[]>(),
  ): Map<string, string[]> {
    if (typeof formNode === "string") {
      const functionCallPattern = /\{\{\s*(\w+)\((\w+)\)/g;
      let regexMatch: RegExpExecArray | null;
      while ((regexMatch = functionCallPattern.exec(formNode)) !== null) {
        const [, funcName, paramName] = regexMatch;
        const existing = functionCallsByName.get(funcName) ?? [];
        if (!existing.includes(paramName)) existing.push(paramName);
        functionCallsByName.set(funcName, existing);
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

  private async fetchTitlesById(uuids: string[]): Promise<Map<string, string>> {
    const titlesById = new Map<string, string>();
    await Promise.all(
      uuids.map(async (uuid) => {
        try {
          const document = await lastValueFrom(
            this.informatieObjectenService.readEnkelvoudigInformatieobject(uuid),
          );
          if (document?.titel) titlesById.set(uuid, document.titel);
        } catch {
          return uuid
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
