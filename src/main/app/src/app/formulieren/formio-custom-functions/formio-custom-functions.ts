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

  async buildEvalContext(form: unknown, taakdata: Record<string, unknown>): Promise<EvalContext> {
    const functionsContext: EvalContext = {};
    const zaakDataKey = this.extractFormFunctions(form as FormNode);
    for (const functionName of Object.values(KNOWN_FORMIO_FUNCTIONS)) {
      const zaakdataField = zaakDataKey.get(functionName);
      if (!zaakdataField) continue;

      switch (functionName) {
        case KNOWN_FORMIO_FUNCTIONS.GET_DOCUMENT_TITLE: {
            console.log("called getDocumentTitles")
          const documentUuids = (taakdata[zaakdataField!] as string[]) ?? [];
          const titlesString = await this.getDocumentTitles(documentUuids, );
          functionsContext[functionName] = () => titlesString;
          break;
        }
      }
    }

    return { ...taakdata, ...functionsContext };
  }

    private extractFormFunctions(
        formNode: FormNode,
        functionCallsByName = new Map<string, string>(),
    ): Map<string, string> {
        if (typeof formNode === "string") {
            const functionCallPattern = /\{\{\s*(\w+)\((\w+)\)/g;
            let regexMatch: RegExpExecArray | null;
            while ((regexMatch = functionCallPattern.exec(formNode)) !== null) {
                functionCallsByName.set(regexMatch[1], regexMatch[2]);
            }
        } else if (Array.isArray(formNode)) {
            for (const arrayElement of formNode) this.extractFormFunctions(arrayElement as FormNode, functionCallsByName);
        } else if (formNode !== null && typeof formNode === "object") {
            for (const nestedValue of Object.values(formNode)) this.extractFormFunctions(nestedValue as FormNode, functionCallsByName);
        }
        return functionCallsByName;
    }

  private async getDocumentTitles(uuids: string[]): Promise<string> {
    const titles = await Promise.all(
      uuids.map(async (uuid) => {
        try {
          const document = await lastValueFrom(
            this.informatieObjectenService.readEnkelvoudigInformatieobject(uuid),
          );
          return document?.titel ?? uuid;
        } catch {
          return uuid;
        }
      }),
    );
    return this.formatValues(titles);
  }

  private formatValues(values: string[]): string {
    if (values.length === 0) return "";
    if (values.length === 1) return values[0];
    return values.slice(0, -1).join(", ") + " en " + values[values.length - 1];
  }
}
