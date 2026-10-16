/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import {
  FormArray,
  FormControl,
  FormGroup,
  FormRecord,
  Validators,
} from "@angular/forms";
import { GeneratedType } from "../../../../shared/utils/generated-types";

export type HumanTaskReferentieTabelForm = FormGroup<{
  id: FormControl<number | null>;
  tabel: FormControl<GeneratedType<"RestReferenceTable"> | null>;
}>;

export type HumanTaskForm = FormGroup<{
  id: FormControl<number | null>;
  planItemDefinition: FormControl<GeneratedType<"RESTPlanItemDefinition">>;
  formulierDefinitieId: FormControl<string | null>;
  defaultGroepId: FormControl<string | null>;
  doorlooptijd: FormControl<number | null>;
  actief: FormControl<boolean>;
  referentieTabellen: FormRecord<HumanTaskReferentieTabelForm>;
}>;

export type HumanTasksForm = FormArray<HumanTaskForm>;

export function createHumanTaskReferentieTabelForm({
  id,
  tabel,
}: GeneratedType<"RestHumanTaskReferenceTable">): HumanTaskReferentieTabelForm {
  return new FormGroup({
    id: new FormControl(id ?? null),
    tabel: new FormControl(tabel ?? null, [Validators.required]),
  });
}

function createHumanTaskForm(
  humanTaskParameters: GeneratedType<"RESTHumanTaskParameters">,
): HumanTaskForm {
  const referentieTabellen = new FormRecord<HumanTaskReferentieTabelForm>({});
  for (const referentieTabel of humanTaskParameters.referentieTabellen ?? []) {
    referentieTabellen.addControl(
      referentieTabel.veld ?? "",
      createHumanTaskReferentieTabelForm(referentieTabel),
    );
  }

  const form: HumanTaskForm = new FormGroup({
    id: new FormControl(humanTaskParameters.id ?? null),
    planItemDefinition: new FormControl(
      humanTaskParameters.planItemDefinition ?? {},
      { nonNullable: true },
    ),
    formulierDefinitieId: new FormControl(
      humanTaskParameters.formulierDefinitieId ?? null,
      [Validators.required],
    ),
    defaultGroepId: new FormControl(humanTaskParameters.defaultGroepId ?? null),
    doorlooptijd: new FormControl(humanTaskParameters.doorlooptijd ?? null, [
      Validators.min(0),
    ]),
    actief: new FormControl(humanTaskParameters.actief ?? true, {
      nonNullable: true,
    }),
    referentieTabellen,
  });

  form.controls.doorlooptijd.valueChanges.subscribe((doorlooptijd) => {
    if (doorlooptijd === null || Number.isInteger(doorlooptijd)) return;
    form.controls.doorlooptijd.setValue(Math.trunc(doorlooptijd), {
      emitEvent: false,
    });
  });

  return form;
}

export function createHumanTasksForm(
  humanTaskParameters?: GeneratedType<"RESTHumanTaskParameters">[] | null,
): HumanTasksForm {
  return new FormArray(
    [...(humanTaskParameters ?? [])]
      .sort((a, b) =>
        (a.planItemDefinition?.naam ?? "").localeCompare(
          b.planItemDefinition?.naam ?? "",
        ),
      )
      .map(createHumanTaskForm),
  );
}

/**
 * The human task parameters for a case definition that was just chosen: one per human task of the case definition,
 * active, with the default formulier of the human task and the default groep of the zaaktype.
 */
export function createHumanTasksFormForProcessDefinition(
  processDefinition: GeneratedType<"RestProcessDefinition"> | null,
  defaultGroepId: string | null,
): HumanTasksForm {
  return createHumanTasksForm(
    (processDefinition?.humanTaskDefinitions ?? []).map(
      (planItemDefinition) => ({
        planItemDefinition,
        defaultGroepId: defaultGroepId ?? undefined,
        formulierDefinitieId: planItemDefinition.defaultFormulierDefinitie,
        referentieTabellen: [],
        actief: true,
      }),
    ),
  );
}

export function toHumanTaskParameters(
  form: HumanTasksForm,
): GeneratedType<"RESTHumanTaskParameters">[] {
  return form.controls.map((humanTaskForm) => {
    const {
      id,
      planItemDefinition,
      formulierDefinitieId,
      defaultGroepId,
      doorlooptijd,
      actief,
      referentieTabellen,
    } = humanTaskForm.getRawValue();
    return {
      id: id ?? undefined,
      planItemDefinition,
      formulierDefinitieId: formulierDefinitieId ?? undefined,
      defaultGroepId: defaultGroepId || undefined,
      doorlooptijd: doorlooptijd ?? undefined,
      actief,
      referentieTabellen: Object.entries(referentieTabellen).map(
        ([veld, { id: referentieTabelId, tabel }]) => ({
          id: referentieTabelId ?? undefined,
          veld,
          tabel: tabel ?? undefined,
        }),
      ),
    };
  });
}
