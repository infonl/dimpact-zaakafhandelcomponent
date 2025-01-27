/*
 * SPDX-FileCopyrightText: 2023 Atos
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { Component, Inject } from "@angular/core";
import { FormControl } from "@angular/forms";
import { MAT_DIALOG_DATA } from "@angular/material/dialog";
import { DialogData } from "../../../shared/dialog/dialog-data";

@Component({
  selector: "zac-tekstvlak-edit-dialog",
  templateUrl: "./tekstvlak-edit-dialog.component.html",
  styleUrls: ["./tekstvlak-edit-dialog.component.less"],
})
export class TekstvlakEditDialogComponent {
  formControl: FormControl<string>;

  constructor(@Inject(MAT_DIALOG_DATA) public data: DialogData<string>) {
    this.formControl = new FormControl(data.value);
  }

  updateData() {
    this.data.value = this.formControl.value;
  }
}
