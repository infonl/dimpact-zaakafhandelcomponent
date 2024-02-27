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
    "openbaar",
    "beperkt_openbaar",
    "intern",
    "zaakvertrouwelijk",
    "vertrouwelijk",
    "confidentieel",
    "geheim",
    "zeer_geheim",
  ] as const;

  transform(
    value: string | (typeof this.expectedKeys)[0],
  ): `vertrouwelijkheidaanduiding.${Vertrouwelijkheidaanduiding}` {
    if (!this.expectedKeys.includes(value as any)) {
      throw new Error(`Unexpected vertrouwelijkheidaanduiding: ${value}`);
    }
    const expectedKey = value as (typeof this.expectedKeys)[number];
    const newValue = this.toUpperCase(expectedKey);

    return `vertrouwelijkheidaanduiding.${newValue}`;
  }

  toUpperCase<T extends string>(v: T): Uppercase<T> {
    return v.toUpperCase() as Uppercase<T>;
  }
}
