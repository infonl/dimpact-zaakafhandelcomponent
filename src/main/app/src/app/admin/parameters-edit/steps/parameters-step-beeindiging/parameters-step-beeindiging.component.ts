/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { Component, computed, inject, input } from "@angular/core";
import { FormControl, ReactiveFormsModule } from "@angular/forms";
import { MatCardModule } from "@angular/material/card";
import { MatCheckboxModule } from "@angular/material/checkbox";
import { MatSelectModule } from "@angular/material/select";
import { MatTableModule } from "@angular/material/table";
import { TranslateModule } from "@ngx-translate/core";
import { injectQuery } from "@tanstack/angular-query-experimental";
import { UtilService } from "../../../../core/service/util.service";
import { GeneratedType } from "../../../../shared/utils/generated-types";
import { ZaakafhandelParametersService } from "../../../zaakafhandel-parameters.service";
import {
  createZaakbeeindigRedenForm,
  ZaakbeeindigingForm,
  ZaakbeeindigRedenForm,
} from "./zaakbeeindiging-form";

type ZaakbeeindigingRow = {
  /** `null` for the row of the zaak niet ontvankelijk, which is always chosen */
  naam: string | null;
  selected: FormControl<boolean> | null;
  resultaattype: FormControl<GeneratedType<"RestResultaattype"> | null>;
};

@Component({
  selector: "zac-parameters-step-beeindiging",
  templateUrl: "./parameters-step-beeindiging.component.html",
  standalone: true,
  imports: [
    ReactiveFormsModule,
    MatCardModule,
    MatCheckboxModule,
    MatSelectModule,
    MatTableModule,
    TranslateModule,
  ],
})
export class ParametersStepBeeindigingComponent {
  readonly form = input.required<ZaakbeeindigingForm>();
  readonly zaaktypeUuid = input.required<string>();

  private readonly zaakafhandelParametersService = inject(
    ZaakafhandelParametersService,
  );
  private readonly utilService = inject(UtilService);

  private readonly zaakbeeindigRedenenQuery = injectQuery(() =>
    this.zaakafhandelParametersService.listZaakbeeindigRedenen(),
  );
  protected readonly resultaattypesQuery = injectQuery(() =>
    this.zaakafhandelParametersService.listResultaattypes(this.zaaktypeUuid()),
  );

  protected readonly rows = computed<ZaakbeeindigingRow[]>(() => [
    {
      naam: null,
      selected: null,
      resultaattype: this.form().controls.zaakNietOntvankelijkResultaattype,
    },
    ...(this.zaakbeeindigRedenenQuery.data() ?? []).map((zaakbeeindigReden) => {
      const { controls } = this.redenForm(zaakbeeindigReden);
      return {
        naam: zaakbeeindigReden.naam ?? "",
        selected: controls.selected,
        resultaattype: controls.resultaattype,
      };
    }),
  ]);

  protected readonly compareObject = (a: unknown, b: unknown) =>
    this.utilService.compare(a, b);

  private redenForm(
    zaakbeeindigReden: GeneratedType<"RestZaakbeeindigReden">,
  ): ZaakbeeindigRedenForm {
    const redenen = this.form().controls.redenen;
    const key = zaakbeeindigReden.id ?? "";
    const existing = redenen.controls[key];
    if (existing) return existing;

    const redenForm = createZaakbeeindigRedenForm({
      zaakbeeindigReden,
      resultaattype: null,
      selected: false,
    });
    redenen.addControl(key, redenForm, { emitEvent: false });
    return redenForm;
  }
}
