/*
 * SPDX-FileCopyrightText: 2021 Atos, 2024 Dimpact, 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { Routes } from "@angular/router";
import { BpmnProcessDefinitionsComponent } from "./bpmn-process-definitions/bpmn-process-definitions.component";
import { GroepSignaleringenComponent } from "./groep-signaleringen/groep-signaleringen.component";
import { InrichtingscheckComponent } from "./inrichtingscheck/inrichtingscheck.component";
import { MailtemplateResolver } from "./mailtemplate-resolver.service";
import { MailtemplateComponent } from "./mailtemplate/mailtemplate.component";
import { MailtemplatesComponent } from "./mailtemplates/mailtemplates.component";
import { ParametersEditShellComponent } from "./parameters-edit-shell/parameters-edit-shell.component";
import { ParametersComponent } from "./parameters/parameters.component";
import { ReferentieTabelResolver } from "./referentie-tabel-resolver.service";
import { ReferentieTabelComponent } from "./referentie-tabel/referentie-tabel.component";
import { ReferentieTabellenComponent } from "./referentie-tabellen/referentie-tabellen.component";
import { ZaakafhandelParametersResolver } from "./zaakafhandel-parameters-resolver.service";

export const ADMIN_ROUTES: Routes = [
  { path: "", redirectTo: "check", pathMatch: "full" },
  { path: "groepen", component: GroepSignaleringenComponent },
  { path: "parameters", component: ParametersComponent },
  {
    path: "parameters/:uuid",
    component: ParametersEditShellComponent,
    resolve: { parameters: ZaakafhandelParametersResolver },
  },
  {
    path: "bpmn-procesdefinities",
    component: BpmnProcessDefinitionsComponent,
  },
  { path: "referentietabellen", component: ReferentieTabellenComponent },
  {
    path: "referentietabellen/:id",
    component: ReferentieTabelComponent,
    resolve: { tabel: ReferentieTabelResolver },
  },
  { path: "check", component: InrichtingscheckComponent },
  { path: "mailtemplates", component: MailtemplatesComponent },
  {
    path: "mailtemplate/:id",
    component: MailtemplateComponent,
    resolve: { template: MailtemplateResolver },
  },
  { path: "mailtemplate", component: MailtemplateComponent },
];
