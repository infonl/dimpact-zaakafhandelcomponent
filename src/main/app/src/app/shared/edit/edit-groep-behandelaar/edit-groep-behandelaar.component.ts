/*
 * SPDX-FileCopyrightText: 2021 - 2023 Atos, 2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { Component, Input, OnDestroy, OnInit } from "@angular/core";
import { FormControl } from "@angular/forms";
import { UtilService } from "../../../core/service/util.service";
import { IdentityService } from "../../../identity/identity.service";
import { InputFormField } from "../../material-form-builder/form-components/input/input-form-field";
import { MedewerkerGroepFormField } from "../../material-form-builder/form-components/medewerker-groep/medewerker-groep-form-field";
import { MaterialFormBuilderService } from "../../material-form-builder/material-form-builder.service";
import { EditComponent } from "../edit.component";
import {GeneratedType} from "../../utils/generated-types";

@Component({
  selector: "zac-edit-groep-behandelaar",
  templateUrl: "./edit-groep-behandelaar.component.html",
  styleUrls: [
    "../../static-text/static-text.component.less",
    "../edit.component.less",
    "./edit-groep-behandelaar.component.less",
  ],
})
export class EditGroepBehandelaarComponent
  extends EditComponent
  implements OnInit, OnDestroy
{
  @Input() formField: MedewerkerGroepFormField;
  @Input() reasonField: InputFormField;
  private loggedInUser: GeneratedType<"RestLoggedInUser">;

  constructor(
    mfbService: MaterialFormBuilderService,
    utilService: UtilService,
    private identityService: IdentityService,
  ) {
    super(mfbService, utilService);
    this.identityService.readLoggedInUser().subscribe((user) => {
      this.loggedInUser = user;
    });
  }

  ngOnInit(): void {
    super.ngOnInit();
  }

  ngOnDestroy(): void {
    super.ngOnDestroy();
  }

  edit(): void {
    super.edit();

    if (this.reasonField) {
      this.formFields.setControl("reden", this.reasonField.formControl);
    }
  }

  release(): void {
    this.formField.medewerkerValue(null);
  }

  assignToMe(): void {
    this.formField.medewerkerValue(this.loggedInUser);
  }

  getFormControl(control: string): FormControl {
    return this.formField.formControl.get(control) as FormControl;
  }

  get showAssignToMe(): boolean {
    return (
      this.loggedInUser.id !== this.formField.medewerker.value?.id &&
      this.loggedInUser.groupIds.indexOf(this.formField.groep.value?.id) >= 0
    );
  }

  get showRelease(): boolean {
    return this.formField.medewerker.value !== null;
  }
}
