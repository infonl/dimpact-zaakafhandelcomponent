/*
 * SPDX-FileCopyrightText: 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { Component, EventEmitter, Input, Output } from "@angular/core";
import { takeUntilDestroyed } from "@angular/core/rxjs-interop";
import { FormBuilder, Validators } from "@angular/forms";
import { MatDialog } from "@angular/material/dialog";
import { ActivatedRoute } from "@angular/router";
import { Subject } from "rxjs";
import { UtilService } from "src/app/core/service/util.service";
import { IdentityService } from "src/app/identity/identity.service";
import {
  ConfirmDialogComponent,
  ConfirmDialogData,
} from "src/app/shared/confirm-dialog/confirm-dialog.component";
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
export class ParametersEditBpmnComponent {
  @Input({ required: false }) selectedIndexStart: number = 0;
  @Output() switchProcessDefinition = new EventEmitter<ZaakProcessDefinition>();

  private readonly destroy$ = new Subject<void>();

  protected isLoading: boolean = false;
  protected isSavedZaakafhandelParameters: boolean = false;

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

  protected cmmnBpmnFormGroup = this.formBuilder.group({
    options: this.formBuilder.control<{
      value: ZaakProcessSelect;
      label: string;
    }>({ label: "BPMN", value: "BPMN" }, []),
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
    public readonly dialog: MatDialog,
  ) {
    this.route.data.pipe(takeUntilDestroyed()).subscribe((data) => {
      if (!data?.parameters?.zaakafhandelParameters) {
        return;
      }

      this.bpmnZaakafhandelParameters =
        data.parameters.bpmnZaakafhandelParameters;
      this.isSavedZaakafhandelParameters =
        data.parameters.isSavedZaakafhandelParameters;
      this.bpmnDefinitions = data.parameters.bpmnProcessDefinitionsList || [];

      this.createForm();
    });

    this.cmmnBpmnFormGroup.controls.options.valueChanges.subscribe((value) => {
      if (value?.value === "CMMN" && this.isDirty()) {
        this.confirmProcessDefinitionSwitch();
        return;
      }
      this.switchProcessDefinition.emit({
        type: value?.value || "SELECT-PROCESS-DEFINITION",
      });
    });
  }

  async createForm() {
    if (this.isSavedZaakafhandelParameters) {
      this.cmmnBpmnFormGroup.disable({ emitEvent: false });
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
          this.bpmnZaakafhandelParameters.id = data.id; // needed for next save
          this.cmmnBpmnFormGroup.disable({ emitEvent: false }); // disable form to prevent modifications until explicitly enabled again

          this.utilService.openSnackbar(
            "msg.zaakafhandelparameters.opgeslagen",
          );
        },
        error: () => {
          this.isLoading = false;
        },
      });
  }

  confirmProcessDefinitionSwitch() {
    this.dialog
      .open(ConfirmDialogComponent, {
        data: new ConfirmDialogData({
          key: "zaps.step.proces-definitie.bevestig-switch.msg",
          args: { process: "BPMN" },
        }),
      })
      .afterClosed()
      .subscribe((result) => {
        if (result) {
          this.switchProcessDefinition.emit({
            type: "CMMN",
          });
        } else {
          this.cmmnBpmnFormGroup.controls.options.patchValue(
            { value: "BPMN", label: "BPMN" },
            {
              emitEvent: false,
            },
          );
        }
      });
  }

  protected isValid(): boolean {
    return (
      (this.cmmnBpmnFormGroup.disabled || this.cmmnBpmnFormGroup.valid) &&
      this.algemeenFormGroup.valid
    );
  }

  private isDirty(): boolean {
    return this.algemeenFormGroup.dirty;
  }
}
