/*
 * SPDX-FileCopyrightText: 2023 Atos, 2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { SelectionModel } from "@angular/cdk/collections";
import { Component, EventEmitter, Input, OnInit, Output } from "@angular/core";
import { FormBuilder, FormControl, FormGroup } from "@angular/forms";
import moment from "moment";
import { FormulierDefinitie } from "../../admin/model/formulieren/formulier-definitie";
import { FormulierVeldDefinitie } from "../../admin/model/formulieren/formulier-veld-definitie";
import { FormulierVeldtype } from "../../admin/model/formulieren/formulier-veld-type.enum";
import { IdentityService } from "../../identity/identity.service";
import { GeneratedType } from "../../shared/utils/generated-types";
import { Zaak } from "../../zaken/model/zaak";

@Component({
  selector: "zac-formulier",
  templateUrl: "./formulier.component.html",
  styleUrls: ["./formulier.component.less"],
})
export class FormulierComponent implements OnInit {
  @Input() definitie: FormulierDefinitie;
  @Input() readonly: boolean;
  @Input() zaak: Zaak;
  @Input() waarden: {};
  @Output() formPartial = new EventEmitter<{}>();
  @Output() formSubmit = new EventEmitter<{}>();

  formGroup: FormGroup;
  FormulierVeldtype = FormulierVeldtype;

  checked: Map<string, SelectionModel<string>> = new Map<
    string,
    SelectionModel<string>
  >();
  referentietabellen: Map<string, string[]> = new Map<string, string[]>();

  medewerkers: GeneratedType<"RestUser">[];
  groepen: GeneratedType<"RestGroup">[];

  bezigMetOpslaan = false;

  constructor(
    public formBuilder: FormBuilder,
    public identityService: IdentityService,
  ) {}

  ngOnInit(): void {
    this.identityService.listUsers().subscribe((u) => {
      this.medewerkers = u;
    });
    this.identityService.listGroups().subscribe((g) => {
      this.groepen = g;
    });
    this.createForm();
    if (this.readonly) {
      this.formGroup.disable();
    }
  }

  createForm() {
    this.formGroup = this.formBuilder.group({});
    this.definitie.veldDefinities.forEach((veldDefinitie) => {
      if (veldDefinitie.veldtype === FormulierVeldtype.CHECKBOXES) {
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
        this.formGroup.addControl(veldDefinitie.systeemnaam, control);
      } else if (FormulierVeldDefinitie.isHervatten(veldDefinitie)) {
        const control = FormulierVeldDefinitie.asControl(veldDefinitie);
        if (!this.isHervatenMogelijk()) {
          control.setValue(false);
          control.disable();
        }
        this.formGroup.addControl(veldDefinitie.systeemnaam, control);
      } else {
        this.formGroup.addControl(
          veldDefinitie.systeemnaam,
          FormulierVeldDefinitie.asControl(veldDefinitie),
        );
      }
    });
    this.formGroup.setValue(this.waarden);
  }

  toggleCheckboxes(systeemnaam: string, optie: string) {
    this.checked.get(systeemnaam).toggle(optie);
    this.formGroup
      .get(systeemnaam)
      .setValue(this.checked.get(systeemnaam).selected.join(";"));
  }

  getControl(systeemnaam: string): FormControl<string> {
    return this.formGroup.get(systeemnaam) as FormControl<string>;
  }

  days(systeemnaam) {
    const datum = this.formGroup.get(systeemnaam).value;
    if (datum) {
      return moment(datum).diff(moment().startOf("day"), "days");
    }
  }

  hasValue(systeemnaam: string) {
    return (
      this.formGroup.get(systeemnaam).value !== null &&
      this.formGroup.get(systeemnaam).value !== ""
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
    return !this.zaak.isOpgeschort && this.zaak.isOpen && !this.zaak.isHeropend;
  }

  isHervatenMogelijk() {
    return this.zaak.isOpgeschort;
  }

  toonVeld(veldDefinitie: FormulierVeldDefinitie): boolean {
    if (FormulierVeldDefinitie.isOpschorten(veldDefinitie)) {
      return this.isOpgeschortenMogelijk();
    }
    if (FormulierVeldDefinitie.isHervatten(veldDefinitie)) {
      return this.isHervatenMogelijk();
    }
    return true;
  }
}
