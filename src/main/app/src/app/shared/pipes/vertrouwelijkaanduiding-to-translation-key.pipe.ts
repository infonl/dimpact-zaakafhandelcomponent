/*
 * SPDX-FileCopyrightText: 2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { Pipe, PipeTransform } from "@angular/core";
import { Vertrouwelijkheidaanduiding } from "../../informatie-objecten/model/vertrouwelijkheidaanduiding.enum";

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

  transform(
    value: string | (typeof this.expectedKeys)[0],
  ): `vertrouwelijkheidaanduiding.${Vertrouwelijkheidaanduiding}` {
    if (!this.expectedKeys.includes(value as any)) {
      throw new Error(`Unexpected vertrouwelijkheidaanduiding: ${value}`);
    }
    const expectedKey = value as (typeof this.expectedKeys)[number];
    return `vertrouwelijkheidaanduiding.${expectedKey}`;
  }

  toUpperCase<T extends string>(v: T): Uppercase<T> {
    return v.toUpperCase() as Uppercase<T>;
  }
}
