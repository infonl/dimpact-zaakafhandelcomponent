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
  OnDestroy,
  output,
  viewChild,
} from "@angular/core";
import { takeUntilDestroyed } from "@angular/core/rxjs-interop";
import { FormBuilder, ReactiveFormsModule, Validators } from "@angular/forms";
import { MatButtonModule } from "@angular/material/button";
import { MatCardModule } from "@angular/material/card";
import { MatDialog, MatDialogModule } from "@angular/material/dialog";
import { MatFormFieldModule } from "@angular/material/form-field";
import { MatIconModule } from "@angular/material/icon";
import { MatStepperModule } from "@angular/material/stepper";
import { ActivatedRoute } from "@angular/router";
import { TranslateModule } from "@ngx-translate/core";
import { forkJoin, Subject, takeUntil } from "rxjs";
import { IdentityService } from "src/app/identity/identity.service";
import {
  ConfirmDialogComponent,
  ConfirmDialogData,
} from "src/app/shared/confirm-dialog/confirm-dialog.component";
import { ZacInput } from "src/app/shared/form/input/input";
import { ZacSelect } from "src/app/shared/form/select/select";
import { injectMutation } from "src/app/shared/http/inject-mutation";
import { StaticTextComponent } from "src/app/shared/static-text/static-text.component";
import { GeneratedType } from "src/app/shared/utils/generated-types";
import { ConfiguratieService } from "../../configuratie/configuratie.service";
import { ProcessModelMethodSelection } from "../model/parameters/process-model-method";
import { SmartDocumentsFormComponent } from "../parameters-components/smart-documents-form/smart-documents-form.component";
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
    ZacInput,
    StaticTextComponent,
    SmartDocumentsFormComponent,
    ParametersStepBeeindigingComponent,
    ParametersStepKoppelingenComponent,
    ParametersStepProcesModelComponent,
  ],
})
export class ParametersEditBpmnComponent implements AfterViewInit, OnDestroy {
  readonly selectedIndexStart = input<number>(0);
  readonly switchModellingMethod = output<ProcessModelMethodSelection>();

  readonly smartDocumentsFormComponent = viewChild<SmartDocumentsFormComponent>(
    "smartDocumentsFormRef",
  );

  private readonly destroy$ = new Subject<void>();
  private readonly dialog = inject(MatDialog);

  protected readonly createOrUpdateBpmnZaakafhandelparametersMutation =
    injectMutation(() =>
      this.zaakafhandelParametersService.createOrUpdateBpmnZaakafhandelparameters(),
    );
  protected isSavedZaakafhandelParameters: boolean = false;

  protected bpmnProcessDefinitions: GeneratedType<"RestBpmnProcessDefinition">[] =
    [];
  protected groepen: GeneratedType<"RestGroup">[] = [];
  protected medewerkers: GeneratedType<"RestLoggedInUser">[] = [];

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

  protected algemeenFormGroup = this.formBuilder.group({
    bpmnDefinition:
      this.formBuilder.control<GeneratedType<"RestBpmnProcessDefinition"> | null>(
        null,
        [Validators.required],
      ),
    defaultGroep: this.formBuilder.control<GeneratedType<"RestGroup"> | null>(
      null,
      [Validators.required],
    ),
    defaultBehandelaar:
      this.formBuilder.control<GeneratedType<"RestUser"> | null>(null),
    productaanvraagtype: this.formBuilder.control<string | null>(null),
  });

  constructor(
    private readonly route: ActivatedRoute,
    private readonly formBuilder: FormBuilder,
    private readonly zaakafhandelParametersService: ZaakafhandelParametersService,
    private readonly cdr: ChangeDetectorRef,
    private readonly identityService: IdentityService,
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

      forkJoin([
        configuratieService.readBrpDoelbindingSetupEnabled(),
        this.identityService.listBehandelaarGroupsForZaaktype(
          this.bpmnZaakafhandelParameters.zaaktype.omschrijving!,
        ),
      ]).subscribe(async ([brpDoelbindingSetupEnabled, groepen]) => {
        this.brpDoelbindingSetupEnabled = brpDoelbindingSetupEnabled;
        this.groepen = groepen;
        await this.createForm();
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

    const { groepNaam: defaultGroepId, defaultBehandelaarId } =
      this.bpmnZaakafhandelParameters;

    this.algemeenFormGroup.controls.defaultGroep.valueChanges
      .pipe(takeUntil(this.destroy$))
      .subscribe((group) => {
        if (!group) return;

        this.identityService.listUsersInGroup(group.id).subscribe((users) => {
          this.medewerkers = users;
          const pickedUserId =
            this.algemeenFormGroup.controls.defaultBehandelaar.value?.id;
          const defaultUser = users.find(
            ({ id }) => id === (pickedUserId ?? defaultBehandelaarId),
          );
          this.algemeenFormGroup.controls.defaultBehandelaar.setValue(
            defaultUser ?? null,
          );
        });
      });

    if (defaultGroepId) {
      const defaultGroup = this.groepen?.find(
        ({ id }) => id === defaultGroepId,
      );
      this.algemeenFormGroup.controls.defaultGroep.setValue(
        defaultGroup ?? null,
      );
    }

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
        productaanvraagtype:
          this.algemeenFormGroup.value.productaanvraagtype || null,
        groepNaam: this.algemeenFormGroup.value.defaultGroep!.id || "",
        defaultBehandelaarId:
          this.algemeenFormGroup.value.defaultBehandelaar?.id || null,
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

  ngOnDestroy() {
    this.destroy$.next();
    this.destroy$.complete();
  }
}
