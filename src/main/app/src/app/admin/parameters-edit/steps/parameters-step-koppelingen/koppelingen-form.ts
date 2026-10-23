/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { FormControl, FormGroup, Validators } from "@angular/forms";
import { GeneratedType } from "../../../../shared/utils/generated-types";

export type BrpDoelbindingenForm = FormGroup<{
  zoekWaarde: FormControl<string | null>;
  raadpleegWaarde: FormControl<string | null>;
  verwerkingregisterWaarde: FormControl<string | null>;
}>;

export type KoppelingenForm = FormGroup<{
  brpKoppelen: FormControl<boolean>;
  kvkKoppelen: FormControl<boolean>;
  brpDoelbindingen: BrpDoelbindingenForm;
}>;

export function createKoppelingenForm(
  {
    betrokkeneKoppelingen,
    brpDoelbindingen,
  }: {
    betrokkeneKoppelingen?: GeneratedType<"RestBetrokkeneKoppelingen"> | null;
    brpDoelbindingen?: GeneratedType<"RestBrpDoelbindingen"> | null;
  },
  { isBrpDoelbindingSetupEnabled }: { isBrpDoelbindingSetupEnabled: boolean },
): KoppelingenForm {
  const form: KoppelingenForm = new FormGroup({
    brpKoppelen: new FormControl(
      betrokkeneKoppelingen?.isBrpKoppelenEnabled ?? false,
      { nonNullable: true },
    ),
    kvkKoppelen: new FormControl(
      betrokkeneKoppelingen?.isKvkKoppelenEnabled ?? false,
      { nonNullable: true },
    ),
    brpDoelbindingen: new FormGroup({
      zoekWaarde: new FormControl(brpDoelbindingen?.zoekWaarde ?? ""),
      raadpleegWaarde: new FormControl(brpDoelbindingen?.raadpleegWaarde ?? ""),
      verwerkingregisterWaarde: new FormControl(
        brpDoelbindingen?.verwerkingregisterWaarde ?? "",
      ),
    }),
  });

  const doelbindingen = form.controls.brpDoelbindingen;
  const requireDoelbindingen = (brpKoppelen: boolean) => {
    for (const control of Object.values(doelbindingen.controls)) {
      control.setValidators(
        brpKoppelen && isBrpDoelbindingSetupEnabled
          ? [Validators.required]
          : [],
      );
      // revalidating the group alone leaves each field on its previous status
      control.updateValueAndValidity({ emitEvent: false });
    }
    doelbindingen.updateValueAndValidity({ emitEvent: false });
  };

  requireDoelbindingen(form.controls.brpKoppelen.value);
  form.controls.brpKoppelen.valueChanges.subscribe((brpKoppelen) => {
    requireDoelbindingen(brpKoppelen);
    if (!brpKoppelen) doelbindingen.reset();
  });

  return form;
}

export function toBetrokkeneKoppelingen(
  form: KoppelingenForm,
): GeneratedType<"RestBetrokkeneKoppelingen"> {
  return {
    isBrpKoppelenEnabled: form.controls.brpKoppelen.value,
    isKvkKoppelenEnabled: form.controls.kvkKoppelen.value,
  };
}

export function toBrpDoelbindingen(
  form: KoppelingenForm,
): GeneratedType<"RestBrpDoelbindingen"> {
  return form.controls.brpDoelbindingen.getRawValue();
}
