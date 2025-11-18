/*
 * SPDX-FileCopyrightText: 2021 - 2023 Atos, 2024 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { Component, Input, OnDestroy, OnInit } from "@angular/core";
import { FormControl } from "@angular/forms";
import { injectQuery } from "@tanstack/angular-query-experimental";
import { UtilService } from "../../../core/service/util.service";
import { IdentityService } from "../../../identity/identity.service";
import { InputFormField } from "../../material-form-builder/form-components/input/input-form-field";
import { MedewerkerGroepFormField } from "../../material-form-builder/form-components/medewerker-groep/medewerker-groep-form-field";
import { MaterialFormBuilderService } from "../../material-form-builder/material-form-builder.service";
import { GeneratedType } from "../../utils/generated-types";
import { EditComponent } from "../edit.component";

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
  @Input({ required: true }) formField!: MedewerkerGroepFormField;
  @Input({ required: true }) reasonField!: InputFormField;

  private readonly loggedInUserQuery = injectQuery(() =>
    this.identityService.readLoggedInUser(),
  );

  constructor(
    mfbService: MaterialFormBuilderService,
    utilService: UtilService,
    private identityService: IdentityService,
  ) {
    super(mfbService, utilService);
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
    this.formField.medewerkerValue(
      null as unknown as GeneratedType<"RestUser">,
    );
  }

  assignToMe(): void {
    const loggedInUser = this.loggedInUserQuery.data();
    if (!loggedInUser) return;
    this.formField.medewerkerValue(loggedInUser);
  }

  getFormControl(control: string): FormControl {
    return this.formField.formControl.get(control) as FormControl;
  }

  get showAssignToMe() {
    const loggedInUser = this.loggedInUserQuery.data();
    if (!loggedInUser) return false;
    if (loggedInUser.id === this.formField.medewerker.value?.id) return false;
    if (!this.formField.groep.value?.id) return false;
    return (
      loggedInUser.groupIds?.includes(this.formField.groep.value.id) ?? false
    );
  }

  get showRelease() {
    return this.formField.medewerker.value !== null;
  }
}
