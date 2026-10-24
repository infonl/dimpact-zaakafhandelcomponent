/*
 * SPDX-FileCopyrightText: 2021 Atos
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { Component, Input } from "@angular/core";
import { UtilService } from "../../../core/service/util.service";
import { InputFormField } from "../../material-form-builder/form-components/input/input-form-field";
import { MaterialFormBuilderService } from "../../material-form-builder/material-form-builder.service";
import { EditAutocompleteComponent } from "../edit-autocomplete/edit-autocomplete.component";

@Component({
  selector: "zac-edit-groep",
  templateUrl: "./edit-groep.component.html",
  styleUrls: [
    "../../static-text/static-text.component.less",
    "../edit.component.less",
  ],
})
export class EditGroepComponent extends EditAutocompleteComponent {
  @Input() reasonField: InputFormField;

  constructor(
    mfbService: MaterialFormBuilderService,
    utilService: UtilService,
  ) {
    super(mfbService, utilService);
  }

  edit(): void {
    super.edit();
    if (this.reasonField) {
      this.formFields.setControl("reden", this.reasonField.formControl);
    }
  }
}
