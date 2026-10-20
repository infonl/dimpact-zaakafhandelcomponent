/*
 * SPDX-FileCopyrightText: 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import {
  AfterViewInit,
  ChangeDetectorRef,
  Component,
  inject,
  input,
  output,
  viewChild,
} from "@angular/core";
import { takeUntilDestroyed } from "@angular/core/rxjs-interop";
import {
  FormControl,
  FormGroup,
  ReactiveFormsModule,
  Validators,
} from "@angular/forms";
import { MatButtonModule } from "@angular/material/button";
import { MatCardModule } from "@angular/material/card";
import { MatDialog, MatDialogModule } from "@angular/material/dialog";
import { MatFormFieldModule } from "@angular/material/form-field";
import { MatIconModule } from "@angular/material/icon";
import { MatStepperModule } from "@angular/material/stepper";
import { ActivatedRoute } from "@angular/router";
import { TranslateModule } from "@ngx-translate/core";
import {
  ConfirmDialogComponent,
  ConfirmDialogData,
} from "src/app/shared/confirm-dialog/confirm-dialog.component";
import { ZacSelect } from "src/app/shared/form/select/select";
import { injectMutation } from "src/app/shared/http/inject-mutation";
import { GeneratedType } from "src/app/shared/utils/generated-types";
import { ConfiguratieService } from "../../configuratie/configuratie.service";
import { ProcessModelMethodSelection } from "../model/parameters/process-model-method";
import { SmartDocumentsFormComponent } from "../parameters-components/smart-documents-form/smart-documents-form.component";
import {
  createAlgemeenForm,
  toAlgemeenSettings,
} from "../parameters-edit/steps/parameters-step-algemeen/algemeen-form";
import { ParametersStepAlgemeenComponent } from "../parameters-edit/steps/parameters-step-algemeen/parameters-step-algemeen.component";
import { ParametersStepBeeindigingComponent } from "../parameters-edit/steps/parameters-step-beeindiging/parameters-step-beeindiging.component";
import {
  createZaakbeeindigingForm,
  toZaakbeeindigParameters,
} from "../parameters-edit/steps/parameters-step-beeindiging/zaakbeeindiging-form";
import {
  createKoppelingenForm,
  toBetrokkeneKoppelingen,
  toBrpDoelbindingen,
} from "../parameters-edit/steps/parameters-step-koppelingen/koppelingen-form";
import { ParametersStepKoppelingenComponent } from "../parameters-edit/steps/parameters-step-koppelingen/parameters-step-koppelingen.component";
import { ParametersStepProcesModelComponent } from "../parameters-edit/steps/parameters-step-proces-model/parameters-step-proces-model.component";
import { createProcessModelForm } from "../parameters-edit/steps/parameters-step-proces-model/process-model-form";
import { ZaakafhandelParametersService } from "../zaakafhandel-parameters.service";

@Component({
  selector: "zac-parameters-edit-bpmn",
  templateUrl: "./parameters-edit-bpmn.component.html",
  styleUrls: ["./parameters-edit-bpmn.component.less"],
  standalone: true,
  imports: [
    ReactiveFormsModule,
    MatStepperModule,
    MatIconModule,
    MatButtonModule,
    MatCardModule,
    MatDialogModule,
    MatFormFieldModule,
    TranslateModule,
    ZacSelect,
    SmartDocumentsFormComponent,
    ParametersStepAlgemeenComponent,
    ParametersStepBeeindigingComponent,
    ParametersStepKoppelingenComponent,
    ParametersStepProcesModelComponent,
  ],
})
export class ParametersEditBpmnComponent implements AfterViewInit {
  readonly selectedIndexStart = input<number>(0);
  readonly switchModellingMethod = output<ProcessModelMethodSelection>();

  readonly smartDocumentsFormComponent = viewChild<SmartDocumentsFormComponent>(
    "smartDocumentsFormRef",
  );

  private readonly dialog = inject(MatDialog);

  protected readonly createOrUpdateBpmnZaakafhandelparametersMutation =
    injectMutation(() =>
      this.zaakafhandelParametersService.createOrUpdateBpmnZaakafhandelparameters(),
    );
  protected isSavedZaakafhandelParameters: boolean = false;

  protected bpmnProcessDefinitions: GeneratedType<"RestBpmnProcessDefinition">[] =
    [];

  protected bpmnZaakafhandelParameters: GeneratedType<"RestZaaktypeBpmnConfiguration"> & {
    zaaktype: GeneratedType<"RestZaaktype">;
    isZaakspecifiekAutoriseerbaar: boolean;
  } = {
    isZaakspecifiekAutoriseerbaar: false,
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
      isBrpKoppelenEnabled: false,
      isKvkKoppelenEnabled: false,
    },
    brpDoelbindingen: {
      zoekWaarde: "",
      raadpleegWaarde: "",
      verwerkingregisterWaarde: "",
    },
    zaakbeeindigParameters: [],
  };

  protected cmmnBpmnFormGroup = createProcessModelForm("BPMN");
  protected koppelingenForm = createKoppelingenForm(
    {},
    { isBrpDoelbindingSetupEnabled: false },
  );
  protected zaakbeeindigFormGroup = createZaakbeeindigingForm({});

  protected brpDoelbindingSetupEnabled = false;

  protected algemeenFormGroup = new FormGroup({
    bpmnDefinition:
      new FormControl<GeneratedType<"RestBpmnProcessDefinition"> | null>(null, [
        Validators.required,
      ]),
    algemeen: createAlgemeenForm(),
  });

  constructor(
    private readonly route: ActivatedRoute,
    private readonly zaakafhandelParametersService: ZaakafhandelParametersService,
    private readonly cdr: ChangeDetectorRef,
    private readonly configuratieService: ConfiguratieService,
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

      configuratieService
        .readBrpDoelbindingSetupEnabled()
        .subscribe((brpDoelbindingSetupEnabled) => {
          this.brpDoelbindingSetupEnabled = brpDoelbindingSetupEnabled;
          this.createForm();
        });
    });

    this.cmmnBpmnFormGroup.controls.options.valueChanges.subscribe((value) => {
      if (value?.value === "CMMN" && this.isDirty()) {
        this.confirmModellingMethodSwitch();
        return;
      }
      this.switchModellingMethod.emit({ type: value?.value ?? null });
    });
  }

  ngAfterViewInit(): void {
    this.cdr.detectChanges();
  }

  createForm() {
    if (this.isSavedZaakafhandelParameters) {
      this.cmmnBpmnFormGroup.disable({ emitEvent: false });
    }

    const { groepNaam, defaultBehandelaarId, productaanvraagtype } =
      this.bpmnZaakafhandelParameters;
    this.algemeenFormGroup.setControl(
      "algemeen",
      createAlgemeenForm({
        defaultGroepId: groepNaam,
        defaultBehandelaarId,
        productaanvraagtype,
      }),
    );

    this.algemeenFormGroup.controls.bpmnDefinition.setValue(
      this.bpmnProcessDefinitions?.find(
        (bpmnDef) =>
          bpmnDef.key ===
          this.bpmnZaakafhandelParameters.bpmnProcessDefinitionKey,
      ) || null,
    );

    this.koppelingenForm = createKoppelingenForm(
      this.bpmnZaakafhandelParameters,
      { isBrpDoelbindingSetupEnabled: this.brpDoelbindingSetupEnabled },
    );
    this.zaakbeeindigFormGroup = createZaakbeeindigingForm(
      this.bpmnZaakafhandelParameters,
    );
  }

  protected opslaan() {
    const bpmnProcessDefinitionKey =
      this.algemeenFormGroup.value.bpmnDefinition?.key;

    if (!bpmnProcessDefinitionKey) {
      return;
    }

    const algemeen = toAlgemeenSettings(
      this.algemeenFormGroup.controls.algemeen,
    );

    this.bpmnZaakafhandelParameters.betrokkeneKoppelingen =
      toBetrokkeneKoppelingen(this.koppelingenForm);
    this.bpmnZaakafhandelParameters.brpDoelbindingen = toBrpDoelbindingen(
      this.koppelingenForm,
    );

    this.bpmnZaakafhandelParameters.zaakNietOntvankelijkResultaattype =
      this.zaakbeeindigFormGroup.controls.zaakNietOntvankelijkResultaattype.value;
    this.bpmnZaakafhandelParameters.zaakbeeindigParameters =
      toZaakbeeindigParameters(this.zaakbeeindigFormGroup);

    this.createOrUpdateBpmnZaakafhandelparametersMutation.mutate(
      {
        id: this.bpmnZaakafhandelParameters?.id || null,
        zaaktypeUuid: this.bpmnZaakafhandelParameters.zaaktype.uuid,
        zaaktypeOmschrijving:
          this.bpmnZaakafhandelParameters.zaaktype.omschrijving || "",
        bpmnProcessDefinitionKey: bpmnProcessDefinitionKey,
        productaanvraagtype: algemeen.productaanvraagtype || null,
        groepNaam: algemeen.defaultGroepId || "",
        defaultBehandelaarId: algemeen.defaultBehandelaarId || null,
        betrokkeneKoppelingen:
          this.bpmnZaakafhandelParameters.betrokkeneKoppelingen,
        brpDoelbindingen: this.bpmnZaakafhandelParameters.brpDoelbindingen,
        zaakNietOntvankelijkResultaattype:
          this.bpmnZaakafhandelParameters.zaakNietOntvankelijkResultaattype,
        zaakbeeindigParameters:
          this.bpmnZaakafhandelParameters.zaakbeeindigParameters,
        smartDocuments: {
          isEnabledGlobally:
            this.bpmnZaakafhandelParameters.smartDocuments?.isEnabledGlobally ??
            false,
          isEnabledForZaaktype:
            this.smartDocumentsFormComponent()?.enabledForZaaktypeValue ??
            this.bpmnZaakafhandelParameters.smartDocuments
              ?.isEnabledForZaaktype ??
            false,
        },
      },
      {
        onSuccess: (data) => {
          this.bpmnZaakafhandelParameters.id = data.id; // needed for next save
          this.cmmnBpmnFormGroup.disable({ emitEvent: false }); // disable form to prevent modifications until explicitly enabled again

          const smartDocumentsFormComponent =
            this.smartDocumentsFormComponent();
          if (smartDocumentsFormComponent?.enabledForZaaktypeValue) {
            smartDocumentsFormComponent.saveSmartDocumentsMapping();
          }
        },
      },
    );
  }

  protected confirmModellingMethodSwitch() {
    this.dialog
      .open(ConfirmDialogComponent, {
        data: new ConfirmDialogData({
          key: "zaps.step.proces-model-methode.bevestig-switch.msg",
          args: { process: "BPMN" },
        }),
      })
      .afterClosed()
      .subscribe((result) => {
        if (result) {
          this.switchModellingMethod.emit({ type: "CMMN" });
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
      this.algemeenFormGroup.valid &&
      this.zaakbeeindigFormGroup.valid &&
      this.koppelingenForm.valid
    );
  }

  private isDirty(): boolean {
    return this.algemeenFormGroup.dirty;
  }
}
