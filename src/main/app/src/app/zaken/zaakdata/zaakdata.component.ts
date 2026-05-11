/*
 * SPDX-FileCopyrightText: 2023 Atos, 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { KeyValuePipe, NgIf, NgTemplateOutlet } from "@angular/common";
import { Component, effect, inject, input, output } from "@angular/core";
import {
  AbstractControl,
  FormArray,
  FormBuilder,
  FormControl,
  FormGroup,
  ReactiveFormsModule,
} from "@angular/forms";
import { MatButton } from "@angular/material/button";
import { MatDivider } from "@angular/material/divider";
import { MatExpansionModule } from "@angular/material/expansion";
import { MatFormField, MatLabel } from "@angular/material/form-field";
import { MatIcon } from "@angular/material/icon";
import { MatInput } from "@angular/material/input";
import { MatDrawer } from "@angular/material/sidenav";
import { MatToolbar } from "@angular/material/toolbar";
import {
  injectMutation,
  injectQuery,
} from "@tanstack/angular-query-experimental";
import { TranslatePipe } from "@ngx-translate/core";
import { ZacInput } from "../../shared/form/input/input";
import { GeneratedType } from "../../shared/utils/generated-types";
import { ZakenService } from "../zaken.service";

@Component({
  selector: "zac-zaakdata",
  templateUrl: "./zaakdata.component.html",
  standalone: true,
  imports: [
    NgIf,
    NgTemplateOutlet,
    KeyValuePipe,
    ReactiveFormsModule,
    MatToolbar,
    MatIcon,
    MatDivider,
    MatButton,
    MatExpansionModule,
    MatFormField,
    MatLabel,
    MatInput,
    ZacInput,
    TranslatePipe,
  ],
})
export class ZaakdataComponent {
  private readonly formBuilder = inject(FormBuilder);
  private readonly zakenService = inject(ZakenService);

  protected readonly zaak = input.required<GeneratedType<"RestZaak">>();
  protected readonly sideNav = input.required<MatDrawer>();
  protected readonly readonly = input<boolean>(false);

  protected readonly dataChanged = output();

  protected readonly form = this.formBuilder.group({});

  protected readonly procesVariabeleQuery = injectQuery(() =>
    this.zakenService.listProcesVariabelen(),
  );

  protected readonly updateZaakDataMutation = injectMutation(() => ({
    ...this.zakenService.updateZaakdata(),
    onSuccess: async () => {
      this.dataChanged.emit();
      void this.sideNav().close();
    },
  }));

  constructor() {
    effect(() => {
      const zaakData = this.zaak().zaakdata;
      const procesVariabele = this.procesVariabeleQuery.data();
      if (!zaakData || !procesVariabele) return;

      this.buildForm(zaakData, this.form);
      if (this.readonly()) this.form.disable();
    });
  }

  protected formSubmit() {
    this.updateZaakDataMutation.mutate({
      uuid: this.zaak().uuid,
      zaakdata: this.form.value,
    });
  }

  private buildForm(data: Record<string, unknown>, formGroup: FormGroup) {
    for (const [key, value] of Object.entries(data)) {
      const control = this.getControl(value);

      if (this.isProcesVariabele(key)) control.disable();
      formGroup.addControl(key, control);
    }
    return formGroup;
  }

  private isProcesVariabele(data?: unknown) {
    const procesVariabeleList = this.procesVariabeleQuery.data();
    if (!procesVariabeleList) return false;
    if (typeof data !== "string") return false;
    return procesVariabeleList.includes(data);
  }

  private isFile(value?: unknown) {
    if (!value) return false;

    if (typeof value !== "object") return false;

    return "originalName" in value;
  }

  private isObject(value: unknown) {
    return typeof value === "object" && !Array.isArray(value) && value !== null;
  }

  private isValue(value: unknown) {
    return !this.isObject(value) && !Array.isArray(value);
  }

  private getControl(value: unknown): FormControl | FormGroup | FormArray {
    if (this.isValue(value)) {
      return this.formBuilder.control(value);
    }

    if (this.isFile(value)) {
      const control = this.formBuilder.control(
        (value as { originalName: string }).originalName,
      );
      control.disable();
      return control;
    }

    if (Array.isArray(value)) {
      const formArray = this.formBuilder.array(
        value.map((item) => this.getControl(item)),
      );
      formArray.disable();
      return formArray;
    }

    if (this.isObject(value)) {
      const formGroup = this.buildForm(
        value as Record<string, unknown>,
        this.formBuilder.group({}),
      );
      formGroup.disable();
      return formGroup;
    }

    return this.formBuilder.control(value);
  }

  protected isFormControl(control: AbstractControl): control is FormControl {
    return control instanceof FormControl;
  }
  protected isFormGroup(control: AbstractControl): control is FormGroup {
    return control instanceof FormGroup;
  }
  protected isFormArray(control: AbstractControl): control is FormArray {
    return control instanceof FormArray;
  }

  protected getFormArrayControls(formArray: FormArray): AbstractControl[] {
    return formArray.controls;
  }
}
