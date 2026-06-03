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

@Injectable({ providedIn: "root" })
export class FormioCustomFunctions {
  private readonly informatieObjectenService = inject(InformatieObjectenService);

  hasFunctionCalls(form: unknown): boolean {
    return this.getFormFunctions(form).size > 0;
  }

  async buildEvalContext(form: unknown, taakdata: Record<string, unknown>): Promise<EvalContext> {
    const context: EvalContext = {};
    const zaakDataKey = this.getFormFunctions(form);

    for (const functionName of Object.values(KNOWN_FORMIO_FUNCTIONS)) {
      const zaakdataField = zaakDataKey.get(functionName);

      switch (functionName) {
        case KNOWN_FORMIO_FUNCTIONS.GET_DOCUMENT_TITLE: {
          const documentUuids = (taakdata[zaakdataField!] as string[]) ?? [];
          const titlesString = await this.getDocumentTitles(documentUuids, );
          context[functionName] = () => titlesString;
          break;
        }
      }
    }

    return { ...taakdata, ...context };
  }

    private getFormFunctions(
        form: unknown,
        result = new Map<string, string>(),
    ): Map<string, string> {
        if (typeof form === "string") {
            const pattern = /\{\{\s*(\w+)\((\w+)\)/g;
            let match: RegExpExecArray | null;
            while ((match = pattern.exec(form)) !== null) {
                result.set(match[1], match[2]);
            }
        } else if (Array.isArray(form)) {
            for (const item of form) this.getFormFunctions(item, result);
        } else if (form !== null && typeof form === "object") {
            for (const v of Object.values(form)) this.getFormFunctions(v, result);
        }
        return result;
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
