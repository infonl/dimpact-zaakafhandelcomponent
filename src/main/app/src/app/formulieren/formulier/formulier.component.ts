/*
 * SPDX-FileCopyrightText: 2023 Atos, 2024 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { SelectionModel } from "@angular/cdk/collections";
import { Component, EventEmitter, Input, OnInit, Output } from "@angular/core";
import { FormBuilder, FormControl } from "@angular/forms";
import moment from "moment";
import { FormulierVeldDefinitie } from "../../admin/model/formulieren/formulier-veld-definitie";
import { FormulierVeldtype } from "../../admin/model/formulieren/formulier-veld-type.enum";
import { IdentityService } from "../../identity/identity.service";
import { GeneratedType } from "../../shared/utils/generated-types";

@Component({
  selector: "zac-formulier",
  templateUrl: "./formulier.component.html",
  styleUrls: ["./formulier.component.less"],
})
export class FormulierComponent implements OnInit {
  @Input({ required: true })
  definitie!: GeneratedType<"RESTFormulierDefinitie">;
  @Input() readonly = false;
  @Input({ required: true }) zaak!: GeneratedType<"RestZaak">;
  @Input() waarden: Record<string, unknown> = {};
  @Output() formPartial = new EventEmitter<Record<string, unknown>>();
  @Output() formSubmit = new EventEmitter<Record<string, unknown>>();

  formGroup = this.formBuilder.group({});
  FormulierVeldtype = FormulierVeldtype;

  checked = new Map<string, SelectionModel<string>>();
  referentietabellen = new Map<string, string[]>();

  medewerkers: GeneratedType<"RestUser">[] = [];
  groepen: GeneratedType<"RestGroup">[] = [];

  bezigMetOpslaan = false;

  constructor(
    private readonly formBuilder: FormBuilder,
    private readonly identityService: IdentityService,
  ) {}

  ngOnInit() {
    this.identityService.listUsers().subscribe((users) => {
      this.medewerkers = users;
    });
    this.identityService.listGroups().subscribe((groups) => {
      this.groepen = groups;
    });
    this.createForm();
    if (this.readonly) {
      this.formGroup.disable();
    }
  }

  createForm() {
    this.definitie.veldDefinities.forEach((veldDefinitie) => {
      if (
        veldDefinitie.veldtype === "CHECKBOXES" &&
        veldDefinitie.systeemnaam
      ) {
        this.checked.set(
          veldDefinitie.systeemnaam,
          new SelectionModel<string>(true),
        );
      }
      if (FormulierVeldDefinitie.isOpschorten(veldDefinitie)) {
        const control = FormulierVeldDefinitie.asControl(veldDefinitie);
        if (!this.isOpgeschortenMogelijk()) {
          control.setValue(false);
          control.disable();
        }
        this.formGroup.addControl(veldDefinitie.systeemnaam as never, control);
      } else if (
        FormulierVeldDefinitie.isHervatten(veldDefinitie) &&
        veldDefinitie.systeemnaam
      ) {
        const control = FormulierVeldDefinitie.asControl(veldDefinitie);
        if (!this.isHervatenMogelijk()) {
          control.setValue(false);
          control.disable();
        }
        this.formGroup.addControl(veldDefinitie.systeemnaam, control);
      } else if (veldDefinitie.systeemnaam) {
        this.formGroup.addControl(
          veldDefinitie.systeemnaam,
          FormulierVeldDefinitie.asControl(veldDefinitie),
        );
      }
    });
    this.formGroup.setValue(this.waarden);
  }

  toggleCheckboxes(systeemnaam: string, optie: string) {
    this.checked.get(systeemnaam)?.toggle(optie);
    this.formGroup
      .get(systeemnaam)
      ?.setValue(this.checked.get(systeemnaam)?.selected.join(";"));
  }

  getControl(systeemnaam?: string) {
    return this.formGroup.get(systeemnaam!) as FormControl<string>;
  }

  days(systeemnaam?: string) {
    if (!systeemnaam) return;
    const datum = this.formGroup.get(systeemnaam)?.value;
    if (!datum) return;
    return moment(datum).diff(moment().startOf("day"), "days");
  }

  hasValue(systeemnaam: string) {
    return (
      this.formGroup.get(systeemnaam)?.value !== null &&
      this.formGroup.get(systeemnaam)?.value !== ""
    );
  }

  opslaan() {
    this.bezigMetOpslaan = true;
    this.formPartial.emit(this.formGroup.value);
    this.bezigMetOpslaan = false;
  }

  opslaanEnAfronden() {
    this.bezigMetOpslaan = true;
    this.formSubmit.emit(this.formGroup.value);
    this.formGroup.disable();
  }

  isOpgeschortenMogelijk() {
    return Boolean(
      !this.zaak?.isOpgeschort && this.zaak?.isOpen && !this.zaak?.isHeropend,
    );
  }

  isHervatenMogelijk() {
    return Boolean(this.zaak?.isOpgeschort);
  }

  toonVeld(veldDefinitie: FormulierVeldDefinitie) {
    if (FormulierVeldDefinitie.isOpschorten(veldDefinitie)) {
      return this.isOpgeschortenMogelijk();
    }
    if (FormulierVeldDefinitie.isHervatten(veldDefinitie)) {
      return this.isHervatenMogelijk();
    }
    return true;
  }
}
