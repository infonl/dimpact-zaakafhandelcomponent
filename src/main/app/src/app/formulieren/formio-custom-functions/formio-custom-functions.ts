/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { inject, Injectable } from "@angular/core";
import { lastValueFrom } from "rxjs";
import { InformatieObjectenService } from "../../informatie-objecten/informatie-objecten.service";
import { ZakenService } from "../../zaken/zaken.service";

/**
 * All custom function names that can appear in form.io ${...} expressions.
 * Add a new entry here when you add a new function below.
 */
export enum KNOWN_FORMIO_FUNCTIONS {
  GET_DOCUMENT_TITLE = "getDocumentTitles",
}

type EvalContext = Record<string, unknown>;

/**
 * Builds an evalContext object with custom functions for form.io ${...} expressions.
 * Form.io merges this into every component's eval context at render time.
 *
 * To add a new function:
 *   1. Add its name to KNOWN_FORMIO_FUNCTIONS.
 *   2. Add a case in the switch block below.
 *   3. Pre-fetch the needed data using zaakdata/zaakUuid and register a synchronous function.
 */
@Injectable({ providedIn: "root" })
export class FormioCustomFunctions {
  private readonly zakenService = inject(ZakenService);
  private readonly informatieObjectenService = inject(InformatieObjectenService);

  hasFunctionCalls(form: unknown): boolean {
    return this.getFormFunctions(form).size > 0;
  }

  async buildEvalContext(form: unknown, zaakUuid: string): Promise<EvalContext> {
      console.log("formio-custom-functions.ts: buildEvalContext");
    const context: EvalContext = {};
    const zaakDataKey = this.getFormFunctions(form);
    const zaakdata = await this.fetchZaakdata(zaakUuid);

    for (const functionName of Object.values(KNOWN_FORMIO_FUNCTIONS)) {
      const zaakdataField = zaakDataKey.get(functionName);

      switch (functionName) {
        case KNOWN_FORMIO_FUNCTIONS.GET_DOCUMENT_TITLE: {
          const documentUuids = (zaakdata[zaakdataField!] as string[]) ?? [];
          const titlesString = await this.getDocumentTitles(documentUuids, zaakUuid);
          context[functionName] = () => titlesString;
          break;
        }
      }
    }

    // Spread zaakdata as top-level keys so lodash template's with(__data__) scope
    // can resolve field names like ZAAK_Documenten_Ondertekenen_Selectie directly.
    return { ...zaakdata, ...context };
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

  private async fetchZaakdata(zaakUuid: string) {
    const zaak = await lastValueFrom(this.zakenService.readZaak(zaakUuid));
    return zaak.zaakdata as Record<string, unknown>;
  }

  private async getDocumentTitles(uuids: string[], zaakUuid?: string): Promise<string> {
    const titles = await Promise.all(
      uuids.map(async (uuid) => {
        try {
          const document = await lastValueFrom(
            this.informatieObjectenService.readEnkelvoudigInformatieobject(uuid, zaakUuid),
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
