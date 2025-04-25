/*
 * SPDX-FileCopyrightText: 2023 Atos
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { Component, Input, OnInit } from "@angular/core";
import {
  AbstractControl,
  FormArray,
  FormBuilder,
  FormControl,
  FormGroup,
} from "@angular/forms";
import { MatDrawer } from "@angular/material/sidenav";
import { Zaak } from "../model/zaak";
import { ZakenService } from "../zaken.service";

@Component({
  selector: "zac-zaakdata",
  templateUrl: "./zaakdata.component.html",
  styleUrls: ["./zaakdata.component.less"],
})
export class ZaakdataComponent implements OnInit {
  @Input({ required: true }) zaak!: Zaak;
  @Input({ required: true }) sideNav!: MatDrawer;
  @Input() readonly = false;
  bezigMetOpslaan = false;
  form?: FormGroup;
  procesVariabelen: string[] = [];

  constructor(
    private formBuilder: FormBuilder,
    private zakenService: ZakenService,
  ) {}

  ngOnInit(): void {
    this.zakenService.listProcesVariabelen().subscribe((data) => {
      this.procesVariabelen = data;
      this.form = this.buildForm(
        this.zaak.zaakdata,
        this.formBuilder.group({}),
      );
      if (this.readonly) {
        this.form.disable();
      }
    });
  }

  buildForm(data: Record<string, unknown>, formData: FormGroup): FormGroup {
    for (const [k, v] of Object.entries(data)) {
      formData.addControl(k, this.getControl(v, this.isProcesVariabele(k)));
    }
    return formData;
  }

  buildArray(values: unknown[], proces: boolean): FormArray {
    if (!values?.length) {
      return this.formBuilder.array([[]]);
    }
    return this.formBuilder.array(
      values.map((value) => this.getControl(value, proces)),
    );
  }

  getControl(value: unknown, proces: boolean): AbstractControl {
    if (this.isValue(value)) {
      return new FormControl({ value: value, disabled: proces });
    } else if (this.isFile(value)) {
      return new FormControl({
        value: (value as File)["originalName"],
        disabled: true,
      });
    } else if (Array.isArray(value)) {
      return this.buildArray(value, proces);
    } else if (this.isObject(value)) {
      return this.buildForm(
        value as Record<string, unknown>,
        this.formBuilder.group({}),
      );
    }

    return new FormControl({ value: value, disabled: proces });
  }

  isProcesVariabele(key: string): boolean {
    return this.procesVariabelen.includes(key);
  }

  isFile(data?: unknown) {
    if (!data) {
      return false;
    }

    if (typeof data !== "object") {
      return false;
    }

    return "originalName" in data;
  }

  isObject(data: unknown) {
    return typeof data === "object" && !Array.isArray(data) && data !== null;
  }

  isValue(data: unknown) {
    return !this.isObject(data) && !Array.isArray(data);
  }

  opslaan(): void {
    this.mergeDeep(this.zaak.zaakdata, this.form?.value);
    this.bezigMetOpslaan = true;
    this.zakenService.updateZaakdata(this.zaak).subscribe(() => {
      this.bezigMetOpslaan = false;
      this.sideNav.close();
    });
  }

  mergeDeep(dest: Record<string, unknown>, src: Record<string, unknown>): void {
    Object.keys(src).forEach((key) => {
      if (key === "__proto__" || key === "constructor") return;
      const destVal = dest[key];
      const srcVal = src[key];
      if (Array.isArray(destVal) && Array.isArray(srcVal)) {
        dest[key] = destVal.concat(...srcVal);
      } else if (
        key in dest &&
        this.isObject(destVal) &&
        this.isObject(srcVal)
      ) {
        this.mergeDeep(
          destVal as Record<string, unknown>,
          srcVal as Record<string, unknown>,
        );
      } else {
        dest[key] = srcVal;
      }
    });
  }
}
