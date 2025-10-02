/*
 * SPDX-FileCopyrightText: 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { Component, EventEmitter, OnInit, Output } from "@angular/core";

@Component({
  selector: "zac-parameter-edit-process-definition-select",
  templateUrl: "./parameter-edit-process-definition-select.component.html",
  styleUrls: ["./parameter-edit-process-definition-select.component.less"],
})
export class ParameterEditProcessDefinitionSelectComponent implements OnInit {
  @Output() switchTo = new EventEmitter<"CMMN" | "BPMN">();

  constructor() {}

  ngOnInit() {
    console.log(
      "ParameterEditProcessDefinitionSelectComponent ngOnInit called",
    );
  }
}
