/*
 * SPDX-FileCopyrightText: 2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { Pipe, PipeTransform } from "@angular/core";
import { GeneratedType } from "../utils/generated-types";

@Pipe({
  name: "vertrouwelijkaanduidingToTranslationKey",
  standalone: true,
})
export class VertrouwelijkaanduidingToTranslationKeyPipe
  implements PipeTransform
{
  public expectedKeys = [
    "OPENBAAR",
    "BEPERKT_OPENBAAR",
    "INTERN",
    "ZAAKVERTROUWELIJK",
    "VERTROUWELIJK",
    "CONFIDENTIEEL",
    "GEHEIM",
    "ZEER_GEHEIM",
  ] as const;

  transform(value?: GeneratedType<"VertrouwelijkheidaanduidingEnum"> | null) {
    if (!value || !this.expectedKeys.includes(value)) {
      throw new Error(`Unexpected vertrouwelijkheidaanduiding: ${value}`);
    }

    return `vertrouwelijkheidaanduiding.${value}`;
  }

  toUpperCase<T extends string>(v: T): Uppercase<T> {
    return v.toUpperCase() as Uppercase<T>;
  }
}
