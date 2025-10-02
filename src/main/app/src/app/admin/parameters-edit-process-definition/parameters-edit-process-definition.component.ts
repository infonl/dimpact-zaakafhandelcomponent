/*
 * SPDX-FileCopyrightText: 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { Component, EventEmitter, OnInit, Output } from "@angular/core";
import { ProcessDefinitionType } from "../model/parameters/parameters-edit-process-definition-type";

@Component({
  selector: "zac-parameters-edit-process-definition",
  templateUrl: "./parameters-edit-process-definition.component.html",
  styleUrls: ["./parameters-edit-process-definition.component.less"],
})
export class ParameterEditProcessDefinitionComponent implements OnInit {
  @Output() switchProcessDefinition = new EventEmitter<ProcessDefinitionType>();

  constructor() {}

  ngOnInit() {
    console.log("ParameterEditProcessDefinitionComponent ngOnInit called");
  }
}
