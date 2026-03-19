/*
 * SPDX-FileCopyrightText: 2022 Atos, 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { NgIf } from "@angular/common";
import { Component, Input } from "@angular/core";
import { MatButtonModule } from "@angular/material/button";
import { MatIconModule } from "@angular/material/icon";
import { TranslateModule } from "@ngx-translate/core";
import { UtilService } from "../../../core/service/util.service";
import { MaterialFormBuilderModule } from "../../material-form-builder/material-form-builder.module";
import { InputFormField } from "../../material-form-builder/form-components/input/input-form-field";
import { MaterialFormBuilderService } from "../../material-form-builder/material-form-builder.service";
import { EmptyPipe } from "../../pipes/empty.pipe";
import { OutsideClickDirective } from "../../directives/outside-click.directive";
import { EditComponent } from "../edit.component";

@Component({
  selector: "zac-edit-input",
  templateUrl: "./edit-input.component.html",
  styleUrls: [
    "../../static-text/static-text.component.less",
    "../edit.component.less",
  ],
  standalone: true,
  imports: [
    NgIf,
    MatIconModule,
    MatButtonModule,
    TranslateModule,
    EmptyPipe,
    OutsideClickDirective,
    MaterialFormBuilderModule,
  ],
})
export class EditInputComponent extends EditComponent {
  @Input({ required: true }) formField!: InputFormField;
  @Input() reasonField?: InputFormField;

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
