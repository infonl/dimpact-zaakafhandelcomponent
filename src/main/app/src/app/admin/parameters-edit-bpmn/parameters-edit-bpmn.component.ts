/*
 * SPDX-FileCopyrightText: 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { Component, EventEmitter, Input, Output } from "@angular/core";
import { FormBuilder, Validators } from "@angular/forms";
import { ActivatedRoute } from "@angular/router";
import { Subject } from "rxjs";
import { UtilService } from "src/app/core/service/util.service";
import { IdentityService } from "src/app/identity/identity.service";
import { GeneratedType } from "src/app/shared/utils/generated-types";
import {
  ZaakProcessDefinition,
  ZaakProcessSelect,
} from "../model/parameters/zaak-process-definition-type";
import { ZaakafhandelParametersService } from "../zaakafhandel-parameters.service";

@Component({
  selector: "zac-parameters-edit-bpmn",
  templateUrl: "./parameters-edit-bpmn.component.html",
  styleUrls: ["./parameters-edit-bpmn.component.less"],
})
export class ParameterEditBpmnComponent {
  @Input({ required: false }) selectedIndexStart: number = 0;
  @Output() switchProcessDefinition = new EventEmitter<ZaakProcessDefinition>();

  private readonly destroy$ = new Subject<void>();

  protected isLoading: boolean = false;
  protected isSavedZaakafhandelParameters: boolean = false;
  protected featureFlagBpmnSupport: boolean = false;

  protected bpmnDefinitions: GeneratedType<"RestBpmnProcessDefinition">[] = [];
  protected groepen = this.identityService.listGroups();

  protected bpmnZaakafhandelParameters: GeneratedType<"RestZaaktypeBpmnConfiguration"> & {
    zaaktype: GeneratedType<"RestZaaktype">;
  } = {
    zaaktypeUuid: "",
    zaaktypeOmschrijving: "",
    bpmnProcessDefinitionKey: "",
    productaanvraagtype: null,
    groepNaam: "",
    zaaktype: {
      uuid: "",
      identificatie: "",
      doel: "",
      omschrijving: "",
    },
  };

  protected readonly zaakProcessDefinitionOptions: Array<{
    label: string;
    value: ZaakProcessSelect;
  }> = [
    { label: "CMMN", value: "CMMN" },
    { label: "BPMN", value: "BPMN" },
  ];

  cmmnBpmnFormGroup = this.formBuilder.group({
    options: this.formBuilder.control<{
      value: ZaakProcessSelect;
      label: string;
    }>({ label: "BPMN", value: "BPMN" }, [Validators.required]),
  });

  algemeenFormGroup = this.formBuilder.group({
    bpmnDefinition:
      this.formBuilder.control<GeneratedType<"RestBpmnProcessDefinition"> | null>(
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
    protected readonly utilService: UtilService,
  ) {
    this.route.data.subscribe(async (data) => {
      if (!data?.parameters?.zaakafhandelParameters) {
        return;
      }

      this.bpmnZaakafhandelParameters =
        data.parameters.bpmnZaakafhandelParameters;
      this.isSavedZaakafhandelParameters =
        data.parameters.isSavedZaakafhandelParameters;
      this.bpmnDefinitions = data.parameters.bpmnProcessDefinitionsList || [];
      this.featureFlagBpmnSupport = data.parameters.featureFlagBpmnSupport;

      await this.createForm();
    });
  }

  async createForm() {
    if (this.isSavedZaakafhandelParameters) {
      this.cmmnBpmnFormGroup.disable();
    }

    this.algemeenFormGroup.patchValue(this.bpmnZaakafhandelParameters);

    this.algemeenFormGroup.controls.bpmnDefinition.setValue(
      this.bpmnDefinitions?.find(
        (bpmnDef) =>
          bpmnDef.key ===
          this.bpmnZaakafhandelParameters.bpmnProcessDefinitionKey,
      ) || null,
    );

    const { groepNaam: defaultGroepId } = this.bpmnZaakafhandelParameters;
    if (defaultGroepId) {
      const groups = await this.groepen.toPromise();

      const defaultGroup = groups?.find(({ id }) => id === defaultGroepId);
      this.algemeenFormGroup.controls.defaultGroep.setValue(
        defaultGroup ?? groups?.at(0) ?? null,
      );
    }

    if (!this.featureFlagBpmnSupport) {
      // if no bpmn support, disable the form for better UX
      this.algemeenFormGroup.disable();
    }
  }

  protected opslaan() {
    const bpmnProcessDefinitionKey =
      this.algemeenFormGroup.value.bpmnDefinition?.key;

    if (!bpmnProcessDefinitionKey) {
      return;
    }

    this.isLoading = true;
    this.zaakafhandelParametersService
      .updateBpmnZaakafhandelparameters(bpmnProcessDefinitionKey, {
        id: this.bpmnZaakafhandelParameters?.id || null,
        zaaktypeUuid: this.bpmnZaakafhandelParameters.zaaktype.uuid,
        zaaktypeOmschrijving:
          this.bpmnZaakafhandelParameters.zaaktype.omschrijving || "",
        bpmnProcessDefinitionKey,
        productaanvraagtype:
          this.algemeenFormGroup.value.productaanvraagtype || null,
        groepNaam: this.algemeenFormGroup.value.defaultGroep!.id || "",
      })
      .subscribe({
        next: (data) => {
          this.isLoading = false;
          this.bpmnZaakafhandelParameters.id = data.id; // needed when save on this case type is repeated
          this.utilService.openSnackbar(
            "msg.zaakafhandelparameters.opgeslagen",
          );
        },
        error: () => {
          this.isLoading = false;
        },
      });
  }

  protected isValid(): boolean {
    return (
      (this.cmmnBpmnFormGroup.disabled || this.cmmnBpmnFormGroup.valid) &&
      this.algemeenFormGroup.valid
    );
  }
}
