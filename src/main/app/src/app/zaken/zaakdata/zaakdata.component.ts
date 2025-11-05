/*
 * SPDX-FileCopyrightText: 2023 Atos
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { Component, effect, inject, input } from "@angular/core";
import { FormBuilder, Validators } from "@angular/forms";
import { MatDrawer } from "@angular/material/sidenav";
import {
  injectMutation,
  injectQuery,
  QueryClient,
} from "@tanstack/angular-query-experimental";
import { GeneratedType } from "../../shared/utils/generated-types";
import { ZakenService } from "../zaken.service";

@Component({
  selector: "zac-zaakdata",
  templateUrl: "./zaakdata.component.html",
})
export class ZaakdataComponent {
  private readonly formBuilder = inject(FormBuilder);
  private readonly zakenService = inject(ZakenService);
  private readonly queryClient = inject(QueryClient);

  protected readonly zaak = input.required<GeneratedType<"RestZaak">>();
  protected readonly sideNav = input.required<MatDrawer>();
  protected readonly readonly = input<boolean>(false);

  protected readonly form = this.formBuilder.group({});

  protected readonly procesVariabeleQuery = injectQuery(() =>
    this.zakenService.listProcesVariabelen(),
  );

  protected readonly updateZaakDataMutation = injectMutation(() => ({
    ...this.zakenService.updateZaakdata(),
    onSuccess: async () => {
      await this.queryClient.invalidateQueries({
        queryKey: this.zakenService.readZaak(this.zaak().uuid).queryKey,
      });
      void this.sideNav().close();
    },
  }));

  constructor() {
    effect(() => {
      const zaakData = this.zaak().zaakdata;
      const procesVariabele = this.procesVariabeleQuery.data();
      if (!zaakData || !procesVariabele) return;

      Array.from(Object.entries(zaakData)).forEach(([key, value]) => {
        const hasOriginalValue = value !== null && value !== undefined;
        const control = this.formBuilder.control(
          value,
          hasOriginalValue ? Validators.required : undefined,
        );
        if (this.readonly() || procesVariabele.includes(key)) control.disable();
        this.form.addControl(key, control);
      });
    });
  }

  formSubmit() {
    this.updateZaakDataMutation.mutate({
      uuid: this.zaak().uuid,
      zaakdata: this.form.value,
    });
  }
}
