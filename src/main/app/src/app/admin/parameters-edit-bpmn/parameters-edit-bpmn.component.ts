/*
 * SPDX-FileCopyrightText: 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import {Component, EventEmitter, Input, OnDestroy, Output} from "@angular/core";
import { takeUntilDestroyed } from "@angular/core/rxjs-interop";
import {
  FormBuilder,
  FormControl,
  FormGroup,
  Validators,
} from "@angular/forms";
import { MatDialog } from "@angular/material/dialog";
import { ActivatedRoute } from "@angular/router";
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
import {forkJoin, Subject, takeUntil} from "rxjs";
import {ConfiguratieService} from "../../configuratie/configuratie.service";
import {ReferentieTabelService} from "../referentie-tabel.service";

@Component({
  selector: "zac-parameters-edit-bpmn",
  templateUrl: "./parameters-edit-bpmn.component.html",
  styleUrls: ["./parameters-edit-bpmn.component.less"],
})
export class ParametersEditBpmnComponent implements OnDestroy {
  @Input({ required: false }) selectedIndexStart: number = 0;
  @Output() switchProcessDefinition = new EventEmitter<ZaakProcessDefinition>();

  private readonly destroy$ = new Subject<void>();

  protected isLoading: boolean = false;
  protected isSavedZaakafhandelParameters: boolean = false;

  protected bpmnProcessDefinitions: GeneratedType<"RestBpmnProcessDefinition">[] =
    [];
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
    betrokkeneKoppelingen: {
      brpKoppelen: false,
      kvkKoppelen: false,
    },
    brpDoelbindingen: {
      zoekWaarde: "",
      raadpleegWaarde: "",
      verwerkingregisterWaarde: "",
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
  protected brpProtocoleringFormGroup = new FormGroup({
    zoekWaarde: new FormControl(""),
    raadpleegWaarde: new FormControl(""),
    verwerkingregisterWaarde: new FormControl(""),
  });

  protected smartDocumentsEnabledForm = new FormGroup({
    enabledForZaaktype: new FormControl<boolean | undefined>(false),
  });

  protected betrokkeneKoppelingen = new FormGroup({
    brpKoppelen: new FormControl(false),
    kvkKoppelen: new FormControl(false),
  });

  protected showDoelbindingen = false;
  protected brpConsultingValues: string[] = [];
  protected brpSearchValues: string[] = [];
  protected brpProcessingValues: string[] = [];
  protected brpProtocollering: string = "";

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
    private readonly configuratieService: ConfiguratieService,
    private readonly referentieTabelService: ReferentieTabelService,
  ) {
    this.route.data.pipe(takeUntilDestroyed()).subscribe((data) => {
      if (!data?.parameters?.zaakafhandelParameters) {
        return;
      }

      this.bpmnZaakafhandelParameters =
        data.parameters.bpmnZaakafhandelParameters;
      this.isSavedZaakafhandelParameters =
        data.parameters.isSavedZaakafhandelParameters;
      this.bpmnProcessDefinitions =
        data.parameters.bpmnProcessDefinitions || [];

      forkJoin(
          referentieTabelService.listBrpSearchValues(),
          referentieTabelService.listBrpViewValues(),
          referentieTabelService.listBrpProcessingValues(),
          configuratieService.readBrpProtocollering(),
      ).subscribe(async([
                                brpSearchValues,
                                brpViewValues,
                                brpProcessingValues,
                                brpProtocollering,
                              ]) => {
        this.brpSearchValues = brpSearchValues;
        this.brpConsultingValues = brpViewValues;
        this.brpProcessingValues = brpProcessingValues;
        this.brpProtocollering = brpProtocollering;
        await this.createForm();
      })
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
      this.bpmnProcessDefinitions?.find(
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

    this.createBetrokkeneKoppelingenForm();

    this.showDoelbindingen = this.getProtocolering(this.brpProtocollering);
    if (this.showDoelbindingen) {
      this.createBrpDoelbindingForm();
    }
  }

  private createBetrokkeneKoppelingenForm() {
    this.betrokkeneKoppelingen = this.formBuilder.group({
      kvkKoppelen: [
        this.bpmnZaakafhandelParameters.betrokkeneKoppelingen?.kvkKoppelen ?? false,
      ],
      brpKoppelen: [
        this.bpmnZaakafhandelParameters.betrokkeneKoppelingen?.brpKoppelen ?? false,
      ],
    });

    this.betrokkeneKoppelingen.controls.brpKoppelen.valueChanges
        .pipe(takeUntil(this.destroy$))
        .subscribe((value) => {
          this.brpProtocoleringFormGroup.controls.raadpleegWaarde.setValidators(
              value ? [Validators.required] : [],
          );
          this.brpProtocoleringFormGroup.controls.zoekWaarde.setValidators(
              value ? [Validators.required] : [],
          );
          this.brpProtocoleringFormGroup.controls.verwerkingregisterWaarde.setValidators(
              value ? [Validators.required] : [],
          );

          this.brpProtocoleringFormGroup.updateValueAndValidity({
            emitEvent: false,
          });
          if (value) return;

          this.brpProtocoleringFormGroup.reset();
        });
  }

  private getProtocolering(protocolering: string) {
    return protocolering?.trim() === "iConnect";
  }

  private createBrpDoelbindingForm() {
    this.brpProtocoleringFormGroup = this.formBuilder.group({
      raadpleegWaarde: [
        this.bpmnZaakafhandelParameters.brpDoelbindingen.raadpleegWaarde ?? "",
        this.betrokkeneKoppelingen.controls.brpKoppelen.value
            ? [Validators.required]
            : [],
      ],
      zoekWaarde: [
        this.bpmnZaakafhandelParameters.brpDoelbindingen.zoekWaarde ?? "",
        this.betrokkeneKoppelingen.controls.brpKoppelen.value
            ? [Validators.required]
            : [],
      ],
      verwerkingregisterWaarde: [
        this.bpmnZaakafhandelParameters.brpDoelbindingen.verwerkingregisterWaarde ?? "",
        this.betrokkeneKoppelingen.controls.brpKoppelen.value
            ? [Validators.required]
            : [],
      ],
    });
  }

  protected opslaan() {
    const bpmnProcessDefinitionKey =
      this.algemeenFormGroup.value.bpmnDefinition?.key;

    if (!bpmnProcessDefinitionKey) {
      return;
    }

    this.bpmnZaakafhandelParameters.betrokkeneKoppelingen = {
      kvkKoppelen: Boolean(
          this.betrokkeneKoppelingen.controls.kvkKoppelen.value,
      ),
      brpKoppelen: Boolean(
          this.betrokkeneKoppelingen.controls.brpKoppelen.value,
      ),
    };
    this.bpmnZaakafhandelParameters.brpDoelbindingen = this.brpProtocoleringFormGroup.value;


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
        betrokkeneKoppelingen:
          this.bpmnZaakafhandelParameters.betrokkeneKoppelingen,
        brpDoelbindingen: this.bpmnZaakafhandelParameters.brpDoelbindingen,
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

  ngOnDestroy() {
    this.destroy$.next();
    this.destroy$.complete();
  }
}
