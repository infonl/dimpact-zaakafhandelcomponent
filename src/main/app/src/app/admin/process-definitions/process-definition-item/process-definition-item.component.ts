/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { Component, EventEmitter, Input, Output } from "@angular/core";
import { SharedModule } from "../../../shared/shared.module";
import { GeneratedType } from "../../../shared/utils/generated-types";

@Component({
  selector: "zac-process-definition-item",
  templateUrl: "./process-definition-item.component.html",
  styleUrls: ["./process-definition-item.component.less"],
  imports: [SharedModule],
})
export class ProcessDefinitionItemComponent {
  @Input({ required: true })
  processDefinition!: GeneratedType<"RestBpmnProcessDefinition">;

  @Output() uploadBpmnFormFile = new EventEmitter<string>();
  @Output() delete = new EventEmitter<{ key: string; name: string }>();

  protected uploadBpmnForm() {
    this.uploadBpmnFormFile.emit(this.processDefinition.key);
  }

  protected deleteProcessDefinition() {
    this.delete.emit(this.processDefinition);
  }
}
