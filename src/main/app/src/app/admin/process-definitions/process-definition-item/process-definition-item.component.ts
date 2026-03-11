/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { Component, EventEmitter, Input, Output } from "@angular/core";
import { MatButtonModule } from "@angular/material/button";
import { MatChipsModule } from "@angular/material/chips";
import { MatIconModule } from "@angular/material/icon";
import { TranslateModule } from "@ngx-translate/core";
import { GeneratedType } from "../../../shared/utils/generated-types";

@Component({
  selector: "zac-process-definition-item",
  templateUrl: "./process-definition-item.component.html",
  styleUrls: ["./process-definition-item.component.less"],
  imports: [MatButtonModule, MatChipsModule, MatIconModule, TranslateModule],
})
export class ProcessDefinitionItemComponent {
  @Input({ required: true })
  processDefinition!: GeneratedType<"RestBpmnProcessDefinition">;
  @Output() uploadFormio = new EventEmitter<string>();

  onUploadFormio() {
    this.uploadFormio.emit(this.processDefinition.key);
  }
}
