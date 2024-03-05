/*
 * SPDX-FileCopyrightText: 2023 Atos
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
import { Group } from "../../identity/model/group";
import { User } from "../../identity/model/user";
import { Zaak } from "../../zaken/model/zaak";

@Component({
  selector: "zac-formulier",
  templateUrl: "./formulier.component.html",
  styleUrls: ["./formulier.component.less"],
})
export class FormulierComponent implements OnInit {
  @Input() definitie: FormulierDefinitie;
  @Input() zaak: Zaak;
  @Output() submit = new EventEmitter<{}>();

  formGroup: FormGroup;
  FormulierVeldtype = FormulierVeldtype;

  checked: Map<string, SelectionModel<string>> = new Map<
    string,
    SelectionModel<string>
  >();
  referentietabellen: Map<string, string[]> = new Map<string, string[]>();

  medewerkers: User[];
  groepen: Group[];

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
  }

  createForm() {
    this.formGroup = this.formBuilder.group({});
    this.definitie.veldDefinities.forEach((vd) => {
      if (vd.veldtype === FormulierVeldtype.CHECKBOXES) {
        this.checked.set(vd.systeemnaam, new SelectionModel<string>(true));
      }
      this.formGroup.addControl(
        vd.systeemnaam,
        FormulierVeldDefinitie.asControl(vd),
      );
    });
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
    this.submit.emit(this.formGroup.value);
  }

  cancel() {
    this.bezigMetOpslaan = true;
    this.submit.emit(null);
  }
}
