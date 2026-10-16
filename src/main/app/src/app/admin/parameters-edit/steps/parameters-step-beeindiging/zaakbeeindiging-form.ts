/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { FormControl, FormGroup, FormRecord, Validators } from "@angular/forms";
import { GeneratedType } from "../../../../shared/utils/generated-types";

export type ZaakbeeindigRedenForm = FormGroup<{
  id: FormControl<number | null>;
  selected: FormControl<boolean>;
  zaakbeeindigReden: FormControl<GeneratedType<"RestZaakbeeindigReden">>;
  resultaattype: FormControl<GeneratedType<"RestResultaattype"> | null>;
}>;

export type ZaakbeeindigingForm = FormGroup<{
  zaakNietOntvankelijkResultaattype: FormControl<GeneratedType<"RestResultaattype"> | null>;
  redenen: FormRecord<ZaakbeeindigRedenForm>;
}>;

export function createZaakbeeindigRedenForm({
  id,
  zaakbeeindigReden,
  resultaattype,
  selected,
}: {
  id?: number | null;
  zaakbeeindigReden: GeneratedType<"RestZaakbeeindigReden">;
  resultaattype: GeneratedType<"RestResultaattype"> | null;
  selected: boolean;
}): ZaakbeeindigRedenForm {
  const form: ZaakbeeindigRedenForm = new FormGroup({
    id: new FormControl(id ?? null),
    selected: new FormControl(selected, { nonNullable: true }),
    zaakbeeindigReden: new FormControl(zaakbeeindigReden, {
      nonNullable: true,
    }),
    resultaattype: new FormControl(resultaattype, {
      validators: selected ? [Validators.required] : [],
    }),
  });

  form.controls.selected.valueChanges.subscribe((isSelected) => {
    form.controls.resultaattype.setValidators(
      isSelected ? [Validators.required] : [],
    );
    form.controls.resultaattype.updateValueAndValidity();
  });

  return form;
}

export function createZaakbeeindigingForm({
  zaakNietOntvankelijkResultaattype,
  zaakbeeindigParameters,
}: {
  zaakNietOntvankelijkResultaattype?: GeneratedType<"RestResultaattype"> | null;
  zaakbeeindigParameters?: GeneratedType<"RestZaakbeeindigParameter">[] | null;
}): ZaakbeeindigingForm {
  const redenen = new FormRecord<ZaakbeeindigRedenForm>({});
  for (const parameter of zaakbeeindigParameters ?? []) {
    redenen.addControl(
      parameter.zaakbeeindigReden.id ?? "",
      createZaakbeeindigRedenForm({ ...parameter, selected: true }),
    );
  }

  return new FormGroup({
    zaakNietOntvankelijkResultaattype: new FormControl(
      zaakNietOntvankelijkResultaattype ?? null,
      [Validators.required],
    ),
    redenen,
  });
}

export function toZaakbeeindigParameters(
  form: ZaakbeeindigingForm,
): GeneratedType<"RestZaakbeeindigParameter">[] {
  return Object.values(form.controls.redenen.controls)
    .map((redenForm) => redenForm.getRawValue())
    .filter(({ selected, resultaattype }) => selected && resultaattype)
    .map(({ id, zaakbeeindigReden, resultaattype }) => ({
      id,
      zaakbeeindigReden,
      resultaattype: resultaattype!,
    }));
}
