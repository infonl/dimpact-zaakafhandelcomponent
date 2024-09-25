/*
 * SPDX-FileCopyrightText: 2021 - 2024 Dimpact
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { NgModule } from "@angular/core";
import { RouterModule, Routes } from "@angular/router";
import { FormulierDefinitieEditComponent } from "./formulier-definitie-edit/formulier-definitie-edit.component";
import { FormulierDefinitieResolverService } from "./formulier-definitie-edit/formulier-definitie-resolver.service";
import { FormulierDefinitiesComponent } from "./formulier-definities/formulier-definities.component";
import { GroepSignaleringenComponent } from "./groep-signaleringen/groep-signaleringen.component";
import { InrichtingscheckComponent } from "./inrichtingscheck/inrichtingscheck.component";
import { MailtemplateResolver } from "./mailtemplate-resolver.service";
import { MailtemplateComponent } from "./mailtemplate/mailtemplate.component";
import { MailtemplatesComponent } from "./mailtemplates/mailtemplates.component";
import { ParameterEditComponent } from "./parameter-edit/parameter-edit.component";
import { ParametersComponent } from "./parameters/parameters.component";
import { ProcessDefinitionsComponent } from "./process-definitions/process-definitions.component";
import { ReferentieTabelResolver } from "./referentie-tabel-resolver.service";
import { ReferentieTabelComponent } from "./referentie-tabel/referentie-tabel.component";
import { ReferentieTabellenComponent } from "./referentie-tabellen/referentie-tabellen.component";
import { ZaakafhandelParametersResolver } from "./zaakafhandel-parameters-resolver.service";

const routes: Routes = [
  {
    path: "admin",
    children: [
      { path: "", redirectTo: "check", pathMatch: "full" },
      { path: "formulierdefinities", component: FormulierDefinitiesComponent },
      {
        path: "formulierdefinities/:id",
        component: FormulierDefinitieEditComponent,
        resolve: { definitie: FormulierDefinitieResolverService },
      },
      { path: "groepen", component: GroepSignaleringenComponent },
      { path: "parameters", component: ParametersComponent },
      {
        path: "parameters/:uuid",
        component: ParameterEditComponent,
        resolve: { parameters: ZaakafhandelParametersResolver },
      },
      { path: "processdefinitions", component: ProcessDefinitionsComponent },
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
    ],
  },
];

@NgModule({
  imports: [RouterModule.forChild(routes)],
  exports: [RouterModule],
})
export class AdminRoutingModule {}
