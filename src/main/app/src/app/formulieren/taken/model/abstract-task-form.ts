/*
 * SPDX-FileCopyrightText: 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { inject, Injectable, OnDestroy } from "@angular/core";
import { FormBuilder, FormGroup } from "@angular/forms";
import { TranslateService } from "@ngx-translate/core";
import { Subject } from "rxjs";
import { FormField } from "../../../shared/form/composed-form/form-field.types";
import { GeneratedType } from "../../../shared/utils/generated-types";

@Injectable({
  providedIn: "root",
})
export abstract class AbstractTaskForm implements OnDestroy {
  protected readonly formBuilder = inject(FormBuilder);
  protected readonly translateService = inject(TranslateService);
  protected readonly destroy$ = new Subject<void>();

  abstract requestForm(
    zaak: GeneratedType<"RestZaak">,
    planItem?: GeneratedType<"RESTPlanItem">,
  ): Promise<FormField[]>;
  abstract handleForm(
    taak: GeneratedType<"RestTask">,
    zaak?: GeneratedType<"RestZaak">,
  ): Promise<FormField[]>;

  /**
   * Called once the taak is completed. Forms whose fields depend on what completing did -
   * signing a document, for instance - update them here. Does nothing by default.
   */
  onTaskCompleted(
    _taak: GeneratedType<"RestTask">,
    _form: FormGroup,
    _formFields: FormField[],
  ): Promise<void> | void {}

  ngOnDestroy() {
    this.destroy$.next();
    this.destroy$.complete();
  }
}
