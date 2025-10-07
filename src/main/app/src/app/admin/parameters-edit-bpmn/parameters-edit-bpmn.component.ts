/*
 * SPDX-FileCopyrightText: 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { Component, EventEmitter, Input, Output } from "@angular/core";
import { ActivatedRoute } from "@angular/router";
import { GeneratedType } from "src/app/shared/utils/generated-types";
import { ZaakProcessDefinition } from "../model/parameters/parameters-edit-process-definition-type";
import { FormBuilder, Validators } from "@angular/forms";
import { ZaakafhandelParametersService } from "../zaakafhandel-parameters.service";
import { IdentityService } from "src/app/identity/identity.service";
import { Subject } from "rxjs";

@Component({
  selector: "zac-parameters-edit-bpmn",
  templateUrl: "./parameters-edit-bpmn.component.html",
  styleUrls: ["./parameters-edit-bpmn.component.less"],
})
export class ParameterEditBpmnComponent {
  @Input() stepperStart: number = 0;
  @Output() switchProcessDefinition = new EventEmitter<ZaakProcessDefinition>();

  private readonly destroy$ = new Subject<void>();

  protected isLoading: boolean = false;
  protected isSavedZaakafhandelparameters: boolean = false;

  protected bpmnDefinitions: GeneratedType<"RestZaaktypeBpmnProcessDefinition">[] =
    [];
  protected groepen = this.identityService.listGroups();

  protected bpmnZaakafhandelParameters: GeneratedType<"RestZaaktypeBpmnProcessDefinition"> & {
    zaaktype: GeneratedType<"RestZaaktype">;
  } = {
    zaaktypeUuid: "",
    zaaktypeOmschrijving: "",
    bpmnProcessDefinitionKey: "",
    productaanvraagtype: null,
    groepNaam: "",

    // needs to be taken out if endpoint is fixe
    zaaktype: {
      uuid: "",
      identificatie: "",
      doel: "",
      omschrijving: "",
    },
  };

  cmmnBpmnFormGroup = this.formBuilder.group({
    options: this.formBuilder.control<{
      value: string;
      label: string;
    } | null>(null, [Validators.required]),
  });

  algemeenFormGroup = this.formBuilder.group({
    bpmnDefinition:
      this.formBuilder.control<GeneratedType<"RestZaaktypeBpmnProcessDefinition"> | null>(
        null,
        [Validators.required],
      ),
    defaultGroep: this.formBuilder.control<GeneratedType<"RestGroup"> | null>(
      null,
      [Validators.required],
    ),
    productaanvraagtype: this.formBuilder.control<string | null>(null),
  });

  constructor(
    private readonly route: ActivatedRoute,
    private readonly formBuilder: FormBuilder,
    private readonly zaakafhandelParametersService: ZaakafhandelParametersService,
    private readonly identityService: IdentityService,
  ) {
    this.route.data.subscribe((data) => {
      console.log("bpmn data", data);

      this.bpmnZaakafhandelParameters =
        data.parameters.bpmnZaakafhandelParameters;

      this.isSavedZaakafhandelparameters =
        data?.parameters.isSavedZaakafhandelparameters;

      this.bpmnDefinitions = data?.parameters.bpmnProcessDefinitionsList || [];
    });

    this.createForm();
  }

  async createForm() {
    this.algemeenFormGroup.patchValue(this.bpmnZaakafhandelParameters, {
      emitEvent: true,
    });

    const { groepNaam: defaultGroepId } = this.bpmnZaakafhandelParameters; // this name should be aligned in new backend

    if (defaultGroepId) {
      const groups = await this.groepen.toPromise();

      const defaultGroup = groups?.find(({ id }) => id === defaultGroepId);
      this.algemeenFormGroup.controls.defaultGroep.setValue(
        defaultGroup ?? null,
      );
    }
  }

  protected opslaan() {
    this.isLoading = true;

    this.bpmnZaakafhandelParameters = {
      ...this.bpmnZaakafhandelParameters,
      ...this.algemeenFormGroup.value,
      groepNaam: this.algemeenFormGroup.value.defaultGroep?.id || "",
    };

    console.log("bpmnZaakafhandelParameters", this.bpmnZaakafhandelParameters);

    this.zaakafhandelParametersService
      .updateBpmnZaakafhandelparameters(
        this.bpmnZaakafhandelParameters.bpmnProcessDefinitionKey,
        {
          zaaktypeUuid: this.bpmnZaakafhandelParameters.zaaktypeUuid,
          zaaktypeOmschrijving:
            this.bpmnZaakafhandelParameters.zaaktypeOmschrijving,
          bpmnProcessDefinitionKey:
            this.bpmnZaakafhandelParameters.bpmnProcessDefinitionKey,
          productaanvraagtype:
            this.algemeenFormGroup.value.productaanvraagtype || null,
          groepNaam: this.algemeenFormGroup.value.defaultGroep!.id || "",
        },
      )
      .subscribe({
        next: () => {
          this.isLoading = false;
        },
        error: () => {
          this.isLoading = false;
        },
      });
  }

  protected isValid(): boolean {
    return this.cmmnBpmnFormGroup.valid && this.algemeenFormGroup.valid;
  }
}
