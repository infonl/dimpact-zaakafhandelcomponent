/*
 * SPDX-FileCopyrightText: 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { Component, EventEmitter, OnInit, Output } from "@angular/core";

@Component({
  selector: "zac-parameter-edit-process-definition",
  templateUrl: "./parameter-edit-process-definition.component.html",
  styleUrls: ["./parameter-edit-process-definition.component.less"],
})
export class ParameterEditProcessDefinitionComponent implements OnInit {
  @Output() switchProcessDefinition = new EventEmitter<
    "CMMN" | "BPMN" | "PRISTINE"
  >();

  constructor() {}

  ngOnInit() {
    console.log("ParameterEditProcessDefinitionComponent ngOnInit called");
  }
}
