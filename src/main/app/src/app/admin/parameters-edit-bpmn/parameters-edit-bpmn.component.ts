/*
 * SPDX-FileCopyrightText: 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { Component, EventEmitter, OnInit, Output } from "@angular/core";
import { ActivatedRoute } from "@angular/router";
import { GeneratedType } from "src/app/shared/utils/generated-types";
import { ProcessDefinitionType } from "../model/parameters/parameters-edit-process-definition-type";
import {
  FormBuilder,
  FormControl,
  FormGroup,
  Validators,
} from "@angular/forms";

@Component({
  selector: "zac-parameters-edit-bpmn",
  templateUrl: "./parameters-edit-bpmn.component.html",
  styleUrls: ["./parameters-edit-bpmn.component.less"],
})
export class ParameterEditBpmnComponent implements OnInit {
  @Output() switchProcessDefinition = new EventEmitter<ProcessDefinitionType>();

  protected isLoading: boolean = false;
  protected isSavedZaakafhandelparameters: boolean = false;

  protected zaakType: GeneratedType<"RestZaaktype"> = {
    uuid: "",
    identificatie: "",
    doel: "",
    omschrijving: "",
  };

  protected bpmnProcessDefinition: GeneratedType<"RestZaaktypeBpmnProcessDefinition"> =
    {
      zaaktypeUuid: "",
      zaaktypeOmschrijving: "",
      bpmnProcessDefinitionKey: "",
      productaanvraagtype: null,
      groepNaam: "",
    };

  algemeenFormGroup = this.formBuilder.group({
    caseDefinition:
      this.formBuilder.control<GeneratedType<"RESTCaseDefinition"> | null>(
        null,
        [Validators.required],
      ),
    domein: this.formBuilder.control<string | null>(null),
    defaultGroep: this.formBuilder.control<GeneratedType<"RestGroup"> | null>(
      null,
      [Validators.required],
    ),
    defaultBehandelaar:
      this.formBuilder.control<GeneratedType<"RestUser"> | null>(null),
    einddatumGeplandWaarschuwing: this.formBuilder.control<number | null>(
      null,
      [Validators.min(0), Validators.max(31)],
    ),
    uiterlijkeEinddatumAfdoeningWaarschuwing: this.formBuilder.control<
      number | null
    >(null, [Validators.min(0)]),
    productaanvraagtype: this.formBuilder.control<string | null>(null),
  });

  constructor(
    private readonly route: ActivatedRoute,
    private readonly formBuilder: FormBuilder,
  ) {
    this.route.data.subscribe((data) => {
      this.bpmnProcessDefinition = data.parameters.bpmnProcessDefinition;
      this.zaakType = data.parameters.zaakafhandelparameters.zaaktype; // these zaaktype props should be provided by the bpmnProcessDefinition endpoint; for now taken from zaakafhandelparameters
      this.isSavedZaakafhandelparameters =
        data?.parameters.isSavedZaakafhandelparameters;
    });
  }

  ngOnInit() {
    console.log("ParameterEditBpmnComponent ngOnInit called");
  }

  protected isValid(): boolean {
    return false;
  }
}
