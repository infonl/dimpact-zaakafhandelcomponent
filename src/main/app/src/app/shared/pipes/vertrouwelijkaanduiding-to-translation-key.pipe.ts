/*
 * SPDX-FileCopyrightText: 2024 INFO.nl
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
  public static readonly expectedKeys = [
    "OPENBAAR",
    "BEPERKT_OPENBAAR",
    "INTERN",
    "ZAAKVERTROUWELIJK",
    "VERTROUWELIJK",
    "CONFIDENTIEEL",
    "GEHEIM",
    "ZEER_GEHEIM",
  ] as const;

  private static _transform(
    value?: GeneratedType<"RestVertrouwelijkheidaanduiding"> | null | undefined,
  ) {
    if (
      value &&
      !VertrouwelijkaanduidingToTranslationKeyPipe.expectedKeys.includes(value)
    ) {
      throw new Error(`Unexpected vertrouwelijkheidaanduiding: ${value}`);
    }

    return `vertrouwelijkheidaanduiding.${value || "-geen-"}` as const;
  }

  public static readonly selectList =
    VertrouwelijkaanduidingToTranslationKeyPipe.expectedKeys.map(
      (value) =>
        ({
          value,
          label: VertrouwelijkaanduidingToTranslationKeyPipe._transform(value),
        }) as const,
    );

  transform = VertrouwelijkaanduidingToTranslationKeyPipe._transform;
}
