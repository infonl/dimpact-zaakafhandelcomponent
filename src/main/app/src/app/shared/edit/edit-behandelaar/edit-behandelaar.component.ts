/*
 * SPDX-FileCopyrightText: 2021 - 2022 Atos, 2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { Component, Input } from "@angular/core";
import { UtilService } from "../../../core/service/util.service";
import { IdentityService } from "../../../identity/identity.service";
import { InputFormField } from "../../material-form-builder/form-components/input/input-form-field";
import { MaterialFormBuilderService } from "../../material-form-builder/material-form-builder.service";
import { EditAutocompleteComponent } from "../edit-autocomplete/edit-autocomplete.component";
import {GeneratedType} from "../../utils/generated-types";

@Component({
  selector: "zac-edit-behandelaar",
  templateUrl: "./edit-behandelaar.component.html",
  styleUrls: [
    "../../static-text/static-text.component.less",
    "../edit.component.less",
    "./edit-behandelaar.component.less",
  ],
})
export class EditBehandelaarComponent extends EditAutocompleteComponent {
  @Input() reasonField: InputFormField;
  showAssignToMe = false;
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

  edit(): void {
    super.edit();

    this.showAssignToMe =
      this.loggedInUser.id !== this.formField.formControl.defaultValue?.id;
    if (this.reasonField) {
      this.formFields.setControl("reden", this.reasonField.formControl);
    }
  }

  release(): void {
    this.formField.value(null);
  }

  assignToMe(): void {
    this.formField.value(this.loggedInUser);
  }
}
