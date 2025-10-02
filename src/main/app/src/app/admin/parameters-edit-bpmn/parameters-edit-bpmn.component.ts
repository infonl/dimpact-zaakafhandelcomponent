/*
 * SPDX-FileCopyrightText: 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { Component, EventEmitter, OnInit, Output } from "@angular/core";

@Component({
  selector: "zac-parameters-edit-bpmn",
  templateUrl: "./parameters-edit-bpmn.component.html",
  styleUrls: ["./parameters-edit-bpmn.component.less"],
})
export class ParameterEditBpmnComponent implements OnInit {
  @Output() switchProcessDefinition = new EventEmitter<
    "CMMN" | "BPMN" | "PRISTINE"
  >();

  constructor() {}

  ngOnInit() {
    console.log("ParameterEditBpmnComponent ngOnInit called");
  }
}
