/*
 * SPDX-FileCopyrightText: 2021 Atos, 2024 Dimpact, 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { Routes } from "@angular/router";
import { MailtemplateResolver } from "./mailtemplate-resolver.service";
import { ZaakafhandelParametersResolver } from "./zaakafhandel-parameters-resolver.service";

export const ADMIN_ROUTES: Routes = [
  { path: "", redirectTo: "check", pathMatch: "full" },
  {
    path: "groepen",
    loadComponent: () =>
      import("./groep-signaleringen/groep-signaleringen.component").then(
        (module) => module.GroepSignaleringenComponent,
      ),
  },
  {
    path: "parameters",
    loadComponent: () =>
      import("./parameters/parameters.component").then(
        (module) => module.ParametersComponent,
      ),
  },
  {
    path: "parameters/:uuid",
    loadComponent: () =>
      import("./parameters-edit-shell/parameters-edit-shell.component").then(
        (module) => module.ParametersEditShellComponent,
      ),
    resolve: { parameters: ZaakafhandelParametersResolver },
  },
  {
    path: "bpmn-procesdefinities",
    loadComponent: () =>
      import("./bpmn-process-definitions/bpmn-process-definitions.component").then(
        (module) => module.BpmnProcessDefinitionsComponent,
      ),
  },
  {
    path: "referentietabellen",
    loadComponent: () =>
      import("./referentie-tabellen/referentie-tabellen.component").then(
        (module) => module.ReferentieTabellenComponent,
      ),
  },
  {
    path: "check",
    loadComponent: () =>
      import("./inrichtingscheck/inrichtingscheck.component").then(
        (module) => module.InrichtingscheckComponent,
      ),
  },
  {
    path: "mailtemplates",
    loadComponent: () =>
      import("./mailtemplates/mailtemplates.component").then(
        (module) => module.MailtemplatesComponent,
      ),
  },
  {
    path: "mailtemplate/:id",
    loadComponent: () =>
      import("./mailtemplate/mailtemplate.component").then(
        (module) => module.MailtemplateComponent,
      ),
    resolve: { template: MailtemplateResolver },
  },
  {
    path: "mailtemplate",
    loadComponent: () =>
      import("./mailtemplate/mailtemplate.component").then(
        (module) => module.MailtemplateComponent,
      ),
  },
];
