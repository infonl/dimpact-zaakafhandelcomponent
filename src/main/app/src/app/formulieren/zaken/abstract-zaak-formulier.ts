/*
 * SPDX-FileCopyrightText: 2022 Atos
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { FormGroup } from "@angular/forms";
import { TranslateService } from "@ngx-translate/core";
import { AbstractFormField } from "../../shared/material-form-builder/model/abstract-form-field";
import { Zaak } from "../../zaken/model/zaak";

export abstract class AbstractZaakFormulier {
  zaak: Zaak;
  dataElementen: Record<string, string | undefined>;
  form: Array<AbstractFormField[]>;

  constructor(protected translate: TranslateService) {}

  initForm(): void {
    this.form = [];
    this._initForm();
  }

  protected abstract _initForm(): void;

  protected getDataElement(key: string) {
    return this.dataElementen[key] ?? null;
  }

  getZaak(formGroup: FormGroup): Zaak {
    this.zaak.zaakdata = this.getDataElementen(formGroup);
    return this.zaak;
  }

  private getDataElementen(formGroup: FormGroup): Record<string, unknown> {
    if (!this.dataElementen) {
      this.dataElementen = {};
    }
    Object.keys(formGroup.controls).forEach((key) => {
      this.dataElementen[key] = formGroup.controls[key]?.value;
    });
    return this.dataElementen;
  }
}
