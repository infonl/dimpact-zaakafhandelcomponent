/*
 * SPDX-FileCopyrightText: 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { Component, EventEmitter, OnInit, Output } from "@angular/core";
import { ActivatedRoute } from "@angular/router";
import { GeneratedType } from "src/app/shared/utils/generated-types";
import { ProcessDefinitionType } from "../model/parameters/parameters-edit-process-definition-type";

@Component({
  selector: "zac-parameters-edit-bpmn",
  templateUrl: "./parameters-edit-bpmn.component.html",
  styleUrls: ["./parameters-edit-bpmn.component.less"],
})
export class ParameterEditBpmnComponent implements OnInit {
  @Output() switchProcessDefinition = new EventEmitter<ProcessDefinitionType>();

  protected isSavedZaakafhandelparameters: boolean = false;

  parameters: GeneratedType<"RestZaaktypeBpmnProcessDefinition"> = {
    zaaktypeUuid: "",
    zaaktypeOmschrijving: "",
    bpmnProcessDefinitionKey: "",
    productaanvraagtype: null,
    groepNaam: "",
  };

  constructor(private readonly route: ActivatedRoute) {
    this.route.data.subscribe((data) => {
      this.parameters = data.parameters.zaakafhandelparameters;
      this.isSavedZaakafhandelparameters =
        data?.parameters.isSavedZaakafhandelparameters;
    });
  }

  ngOnInit() {
    console.log("ParameterEditBpmnComponent ngOnInit called");
  }
}
