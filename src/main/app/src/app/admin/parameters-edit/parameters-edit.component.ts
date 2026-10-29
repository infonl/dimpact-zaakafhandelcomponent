/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import {
  Component,
  computed,
  effect,
  inject,
  OnInit,
  signal,
  untracked,
  viewChild,
  ChangeDetectionStrategy,
} from "@angular/core";
import { toSignal } from "@angular/core/rxjs-interop";
import { MatProgressSpinnerModule } from "@angular/material/progress-spinner";
import {
  MatSidenav,
  MatSidenavContainer,
  MatSidenavModule,
} from "@angular/material/sidenav";
import { MatStepperModule } from "@angular/material/stepper";
import { ActivatedRoute } from "@angular/router";
import { TranslatePipe } from "@ngx-translate/core";
import { injectQuery } from "@tanstack/angular-query-experimental";
import { ConfiguratieService } from "../../configuratie/configuratie.service";
import { injectMutation } from "../../shared/http/inject-mutation";
import { SideNavComponent } from "../../shared/side-nav/side-nav.component";
import { GeneratedType } from "../../shared/utils/generated-types";
import { AdminComponent } from "../admin/admin.component";
import { SmartDocumentsFormComponent } from "../parameters-components/smart-documents-form/smart-documents-form.component";
import { ReferentieTabelService } from "../referentie-tabel.service";
import { ZaakafhandelParametersService } from "../zaakafhandel-parameters.service";
import { ParametersStepButtonsComponent } from "./parameters-step-buttons.component";
import { createActiesFormForProcessDefinition } from "./steps/parameters-step-acties/acties-form";
import { ParametersStepActiesComponent } from "./steps/parameters-step-acties/parameters-step-acties.component";
import { ParametersStepAlgemeenComponent } from "./steps/parameters-step-algemeen/parameters-step-algemeen.component";
import { ParametersStepBeeindigingComponent } from "./steps/parameters-step-beeindiging/parameters-step-beeindiging.component";
import {
  createHumanTasksFormForProcessDefinition,
  syncReferentieTabellen,
} from "./steps/parameters-step-humantasks/human-tasks-form";
import { ParametersStepHumantasksComponent } from "./steps/parameters-step-humantasks/parameters-step-humantasks.component";
import { ParametersStepKoppelingenComponent } from "./steps/parameters-step-koppelingen/parameters-step-koppelingen.component";
import { ParametersStepMailComponent } from "./steps/parameters-step-mail/parameters-step-mail.component";
import { ParametersStepProcesModelComponent } from "./steps/parameters-step-proces-model/parameters-step-proces-model.component";
import {
  ProcessDefinition,
  toProcessDefinition,
} from "./steps/parameters-step-proces-model/process-model-form";
import {
  applyProcessDefinition,
  toForm,
  toRestConfiguration,
  ZaaktypeConfigurationForm,
} from "./zaaktype-configuration-form";

@Component({
  templateUrl: "./parameters-edit.component.html",
  standalone: true,
  changeDetection: ChangeDetectionStrategy.Eager,
  imports: [
    MatProgressSpinnerModule,
    MatSidenavModule,
    MatStepperModule,
    TranslatePipe,
    SideNavComponent,
    SmartDocumentsFormComponent,
    ParametersStepButtonsComponent,
    ParametersStepActiesComponent,
    ParametersStepAlgemeenComponent,
    ParametersStepBeeindigingComponent,
    ParametersStepHumantasksComponent,
    ParametersStepKoppelingenComponent,
    ParametersStepMailComponent,
    ParametersStepProcesModelComponent,
  ],
})
export class ParametersEditComponent extends AdminComponent implements OnInit {
  readonly sideNavContainer =
    viewChild.required<MatSidenavContainer>("sideNavContainer");
  readonly menuSidenav = viewChild.required<MatSidenav>("menuSidenav");
  private readonly smartDocumentsForm = viewChild(SmartDocumentsFormComponent);

  private readonly zaakafhandelParametersService = inject(
    ZaakafhandelParametersService,
  );

  private readonly referentieTabelService = inject(ReferentieTabelService);
  private readonly formulierDefinitiesQuery = injectQuery(() =>
    this.zaakafhandelParametersService.listFormulierDefinities(),
  );
  private readonly referentieTabellenQuery = injectQuery(() =>
    this.referentieTabelService.listReferentieTabellenQuery(),
  );

  protected readonly configuration = signal<
    GeneratedType<"RestZaaktypeConfiguration">
  >(inject(ActivatedRoute).snapshot.data["parameters"]);

  protected readonly brpDoelbindingSetupEnabled = toSignal(
    inject(ConfiguratieService).readBrpDoelbindingSetupEnabled(),
  );

  protected readonly form = signal<ZaaktypeConfigurationForm | null>(null);
  protected readonly processDefinition = signal<ProcessDefinition | null>(
    this.configuration().processDefinition ?? null,
  );
  /**
   * The stepper is rendered anew for each process definition, because it cannot add or remove steps once it is
   * rendered, and the steps depend on what the process definition has use for.
   */
  protected readonly stepperProcessDefinitions = computed(() => [
    this.processDefinition(),
  ]);
  protected selectedIndex = this.processDefinition() ? 1 : 0;

  protected readonly updateZaakafhandelparametersMutation = injectMutation(() =>
    this.zaakafhandelParametersService.updateZaakafhandelparameters(
      this.configuration().zaaktype.uuid,
    ),
  );

  constructor() {
    super();

    effect(() => {
      const isBrpDoelbindingSetupEnabled = this.brpDoelbindingSetupEnabled();
      if (isBrpDoelbindingSetupEnabled === undefined) return;

      untracked(() =>
        this.setForm(
          toForm(this.configuration(), { isBrpDoelbindingSetupEnabled }),
        ),
      );
    });

    effect(() => {
      const form = this.form();
      if (!form) return;
      if (!this.formulierDefinitiesQuery.data()) return;
      if (!this.referentieTabellenQuery.data()) return;

      untracked(() => this.syncReferentieTabellen(form));
    });
  }

  ngOnInit() {
    this.setupMenu("title.parameters.wijzigen");
  }

  protected canSave(form: ZaaktypeConfigurationForm) {
    return (
      form.valid &&
      this.processDefinition() !== null &&
      !this.updateZaakafhandelparametersMutation.isPending()
    );
  }

  protected opslaan(form: ZaaktypeConfigurationForm) {
    const smartDocumentsForm = this.smartDocumentsForm();
    this.updateZaakafhandelparametersMutation.mutate(
      toRestConfiguration(form, this.configuration(), {
        isSmartDocumentsEnabledForZaaktype:
          smartDocumentsForm?.enabledForZaaktypeValue,
      }),
      {
        onSuccess: (savedConfiguration) => {
          if (smartDocumentsForm?.enabledForZaaktypeValue) {
            smartDocumentsForm.saveSmartDocumentsMapping();
          }
          this.configuration.set(savedConfiguration);
          this.setForm(
            toForm(savedConfiguration, {
              isBrpDoelbindingSetupEnabled:
                this.brpDoelbindingSetupEnabled() ?? false,
            }),
          );
        },
      },
    );
  }

  private setForm(form: ZaaktypeConfigurationForm) {
    form.controls.procesModel.valueChanges.subscribe(() => {
      const processDefinition = toProcessDefinition(form.controls.procesModel);
      form.setControl(
        "humanTasks",
        createHumanTasksFormForProcessDefinition(
          processDefinition,
          form.controls.algemeen.controls.defaultGroepId.value,
        ),
      );
      form.setControl(
        "acties",
        createActiesFormForProcessDefinition(processDefinition),
      );
      this.syncReferentieTabellen(form);
      applyProcessDefinition(form, processDefinition);
      this.processDefinition.set(processDefinition);
    });

    this.form.set(form);
  }

  /**
   * The Taken step renders only once it is opened, so the container gives the human tasks their default reference
   * tables; otherwise a configuration saved without opening that step stores none.
   */
  private syncReferentieTabellen(form: ZaaktypeConfigurationForm) {
    for (const humanTaskForm of form.controls.humanTasks.controls) {
      syncReferentieTabellen(humanTaskForm, {
        formulierDefinities: this.formulierDefinitiesQuery.data() ?? [],
        referentieTabellen: this.referentieTabellenQuery.data() ?? [],
      });
    }
  }
}
