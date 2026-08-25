/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import {
  Component,
  inject,
  OnDestroy,
  OnInit,
  signal,
  ViewChild,
} from "@angular/core";
import { MatSidenav, MatSidenavContainer } from "@angular/material/sidenav";
import { ActivatedRoute, Router } from "@angular/router";
import { FormioAppConfig, FormioForm, FormioModule } from "@formio/angular";
import { ConfiguratieService } from "../../configuratie/configuratie.service";
import { UtilService } from "../../core/service/util.service";
import { SharedModule } from "../../shared/shared.module";
import { AdminComponent } from "../admin/admin.component";
import { BpmnService } from "../bpmn.service";
import { FormioBuilderStylesService } from "./formio-builder-styles.service";
import { findTaskFormIssues, TaskFormIssue } from "./task-form-issues";
import { createTaskFormTemplate } from "./task-form-template";
import { ZAC_BUILDER_PALETTE } from "./zac-builder-palette";

type FormBuilderChangeEvent = { form?: FormioForm };

@Component({
  standalone: true,
  templateUrl: "./formulier-builder.component.html",
  styleUrls: ["./formulier-builder.component.less"],
  imports: [SharedModule, FormioModule],
  providers: [
    {
      provide: FormioAppConfig,
      useValue: {
        appUrl: window.location.origin,
        apiUrl: window.location.origin,
      },
    },
  ],
})
export class FormulierBuilderComponent
  extends AdminComponent
  implements OnInit, OnDestroy
{
  @ViewChild("sideNavContainer") sideNavContainer!: MatSidenavContainer;
  @ViewChild("menuSidenav") menuSidenav!: MatSidenav;

  private readonly formioStyles = inject(FormioBuilderStylesService);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly bpmnService = inject(BpmnService);

  protected readonly processDefinitionKey =
    this.route.snapshot.paramMap.get("processDefinitionKey") ?? "";

  /** Taken from the route rather than typed: it has to equal the form key on the BPMN user task. */
  protected readonly formKey =
    this.route.snapshot.paramMap.get("formKey") ?? "";

  protected readonly isEditingExistingForm =
    this.route.snapshot.queryParamMap.get("bewerken") === "true";

  /**
   * Held by reference: the builder mutates this object in place and rebuilds itself whenever the
   * binding changes identity, which would throw away the form on every edit.
   */
  protected readonly form: FormioForm = createTaskFormTemplate(
    this.formKey,
    this.formKey,
  );

  protected readonly builderOptions = {
    icons: "bi",
    builder: ZAC_BUILDER_PALETTE,
  };

  protected readonly formJson = signal(this.toJson(this.form));
  protected readonly issues = signal<TaskFormIssue[]>([]);
  protected readonly isStylesheetLinked = signal(false);
  protected readonly isFormLoaded = signal(false);
  protected readonly isSaving = signal(false);

  constructor() {
    super(inject(UtilService), inject(ConfiguratieService));
  }

  ngOnInit() {
    this.setupMenu(
      this.isEditingExistingForm
        ? "title.taakformulier.bewerken"
        : "title.taakformulier.aanmaken",
      { formulier: this.formKey },
    );
    void this.formioStyles.link().then(() => this.isStylesheetLinked.set(true));

    if (!this.isEditingExistingForm) {
      this.isFormLoaded.set(true);
      this.issues.set(findTaskFormIssues(this.form));
      return;
    }
    this.subscriptions$.push(
      this.bpmnService
        .readProcessDefinitionForm(this.processDefinitionKey, this.formKey)
        .subscribe(({ content }) => {
          Object.assign(this.form, JSON.parse(content) as FormioForm, {
            name: this.formKey,
          });
          this.formJson.set(this.toJson(this.form));
          // the issues are advisory, so they are produced only once the form itself is usable
          this.isFormLoaded.set(true);
          this.issues.set(findTaskFormIssues(this.form));
        }),
    );
  }

  override ngOnDestroy() {
    this.formioStyles.unlink();
    super.ngOnDestroy();
  }

  protected onBuilderChange(event: FormBuilderChangeEvent) {
    const form = event.form ?? this.form;
    this.formJson.set(this.toJson(form));
    this.issues.set(findTaskFormIssues(form));
  }

  protected opslaan() {
    this.isSaving.set(true);
    this.bpmnService
      .uploadProcessDefinitionForm(this.processDefinitionKey, {
        filename: `${this.formKey}.json`,
        content: this.toJson({ ...this.form, name: this.formKey }),
      })
      .subscribe({
        next: () => {
          this.utilService.openSnackbar("msg.bpmn.task-forms.upload.success", {
            namen: this.formKey,
          });
          void this.returnToProcessDefinition();
        },
        error: () => this.isSaving.set(false),
      });
  }

  protected annuleren() {
    void this.returnToProcessDefinition();
  }

  protected downloadFormJson() {
    const url = URL.createObjectURL(
      new Blob([this.formJson()], { type: "application/json" }),
    );
    const link = document.createElement("a");
    link.href = url;
    link.download = `${this.formKey}.json`;
    link.click();
    URL.revokeObjectURL(url);
  }

  private returnToProcessDefinition() {
    return this.router.navigate(["/admin/bpmn-procesdefinities"], {
      queryParams: { key: this.processDefinitionKey },
    });
  }

  private toJson(form: FormioForm) {
    return JSON.stringify(form, null, 2);
  }
}
