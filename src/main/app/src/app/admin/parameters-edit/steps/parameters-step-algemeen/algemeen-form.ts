/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { FormControl, FormGroup, Validators } from "@angular/forms";

export type AlgemeenSettings = {
  defaultGroepId: string | null;
  defaultBehandelaarId: string | null;
  einddatumGeplandWaarschuwing: number | null;
  uiterlijkeEinddatumAfdoeningWaarschuwing: number | null;
  productaanvraagtype: string | null;
};

export type AlgemeenForm = FormGroup<{
  [Key in keyof AlgemeenSettings]: FormControl<AlgemeenSettings[Key]>;
}>;

export function createAlgemeenForm(
  settings: Partial<{
    [Key in keyof AlgemeenSettings]: AlgemeenSettings[Key] | undefined;
  }> = {},
): AlgemeenForm {
  return new FormGroup({
    defaultGroepId: new FormControl(settings.defaultGroepId ?? null, [
      Validators.required,
    ]),
    defaultBehandelaarId: new FormControl(
      settings.defaultBehandelaarId ?? null,
    ),
    einddatumGeplandWaarschuwing: new FormControl(
      settings.einddatumGeplandWaarschuwing ?? null,
      [Validators.min(0), Validators.max(31)],
    ),
    uiterlijkeEinddatumAfdoeningWaarschuwing: new FormControl(
      settings.uiterlijkeEinddatumAfdoeningWaarschuwing ?? null,
      [Validators.min(0)],
    ),
    productaanvraagtype: new FormControl(settings.productaanvraagtype ?? null),
  });
}

/**
 * The input of a warning window gives a string, because `zac-input` binds its `type`, and Angular only picks the
 * number value accessor for a static `type="number"`.
 */
function toNumber(value: number | string | null) {
  return value === null || value === "" ? null : Number(value);
}

export function toAlgemeenSettings(form: AlgemeenForm): AlgemeenSettings {
  const settings = form.getRawValue();
  return {
    ...settings,
    einddatumGeplandWaarschuwing: toNumber(
      settings.einddatumGeplandWaarschuwing,
    ),
    uiterlijkeEinddatumAfdoeningWaarschuwing: toNumber(
      settings.uiterlijkeEinddatumAfdoeningWaarschuwing,
    ),
  };
}
