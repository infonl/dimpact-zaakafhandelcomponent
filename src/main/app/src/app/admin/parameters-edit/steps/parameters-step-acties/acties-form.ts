/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { FormArray, FormControl, FormGroup } from "@angular/forms";
import { GeneratedType } from "../../../../shared/utils/generated-types";

export type ActieForm = FormGroup<{
  id: FormControl<string>;
  naam: FormControl<string>;
  toelichting: FormControl<string | null>;
}>;

export type ActiesForm = FormArray<ActieForm>;

export function createActiesForm(
  userEventListenerParameters?:
    | GeneratedType<"RESTUserEventListenerParameter">[]
    | null,
): ActiesForm {
  return new FormArray(
    (userEventListenerParameters ?? []).map(
      ({ id, naam, toelichting }) =>
        new FormGroup({
          id: new FormControl(id ?? "", { nonNullable: true }),
          naam: new FormControl(naam ?? "", { nonNullable: true }),
          toelichting: new FormControl(toelichting ?? null),
        }),
    ),
  );
}

/**
 * The user event listener parameters for a case definition that was just chosen: one per user event listener of
 * the case definition, without toelichting.
 */
export function createActiesFormForProcessDefinition(
  processDefinition: GeneratedType<"RestProcessDefinition"> | null,
): ActiesForm {
  return createActiesForm(
    (processDefinition?.userEventListenerDefinitions ?? []).map(
      ({ id, naam }) => ({
        id,
        naam,
      }),
    ),
  );
}

export function toUserEventListenerParameters(
  form: ActiesForm,
): GeneratedType<"RESTUserEventListenerParameter">[] {
  return form.controls.map((actieForm) => {
    const { id, naam, toelichting } = actieForm.getRawValue();
    return { id, naam, toelichting: toelichting ?? undefined };
  });
}
