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

  buildForm(data: Record<string, any>, formData: FormGroup): FormGroup {
    for (const [k, v] of Object.entries(data)) {
      formData.addControl(k, this.getControl(v, this.isProcesVariabele(k)));
    }
    return formData;
  }

  buildArray(values: [], proces: boolean): FormArray {
    if (!values?.length) {
      return this.formBuilder.array([[]]);
    }
    return this.formBuilder.array(
      values.map((value) => this.getControl(value, proces)),
    );
  }

  getControl(value: any, proces: boolean): AbstractControl {
    if (this.isValue(value)) {
      return new FormControl({ value: value, disabled: proces });
    } else if (this.isFile(value)) {
      return new FormControl({ value: value["originalName"], disabled: true });
    } else if (this.isArray(value)) {
      return this.buildArray(value, proces);
    } else if (this.isObject(value)) {
      return this.buildForm(value, this.formBuilder.group({}));
    }

    return new FormControl({ value: value, disabled: proces });
  }

  isProcesVariabele(key: string): boolean {
    return this.procesVariabelen.includes(key);
  }

  isFile(data?: File): boolean {
    if (!data) {
      return false;
    }

    return "originalName" in data;
  }

  isArray(data: unknown): boolean {
    return Array.isArray(data);
  }

  isObject(data: unknown): boolean {
    return typeof data === "object" && !Array.isArray(data) && data !== null;
  }

  isValue(data: unknown): boolean {
    return !this.isObject(data) && !this.isArray(data);
  }

  opslaan(): void {
    this.mergeDeep(this.zaak.zaakdata, this.form?.value);
    this.bezigMetOpslaan = true;
    this.zakenService.updateZaakdata(this.zaak).subscribe(() => {
      this.bezigMetOpslaan = false;
      this.sideNav.close();
    });
  }

  mergeDeep(dest: Record<string, any>, src: Record<string, any>): void {
    Object.keys(src).forEach((key) => {
      if (key === "__proto__" || key === "constructor") return;
      const destVal = dest[key];
      const srcVal = src[key];
      if (this.isArray(destVal) && this.isArray(srcVal)) {
        dest[key] = destVal.concat(...srcVal);
      } else if (
        key in dest &&
        this.isObject(destVal) &&
        this.isObject(srcVal)
      ) {
        this.mergeDeep(destVal, srcVal);
      } else {
        dest[key] = srcVal;
      }
    });
  }
}
