/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { Routes } from "@angular/router";
import { ObservedValueOf } from "rxjs";
import { translatedTitle } from "../core/breadcrumbs/route-title";
import { MailtemplateResolver } from "./mailtemplate-resolver.service";
import { ZaakafhandelParametersResolver } from "./zaakafhandel-parameters-resolver.service";

type ResolvedParameters = ObservedValueOf<
  ReturnType<ZaakafhandelParametersResolver["resolve"]>
>;

type ResolvedMailtemplate = Awaited<
  ReturnType<MailtemplateResolver["resolve"]>
>;

export const ADMIN_ROUTES: Routes = [
  { path: "", redirectTo: "check", pathMatch: "full" },
  {
    path: "groepen",
    title: translatedTitle("title.signaleringen.settings.groep"),
    loadComponent: () =>
      import("./groep-signaleringen/groep-signaleringen.component").then(
        (module) => module.GroepSignaleringenComponent,
      ),
  },
  {
    path: "parameters",
    title: translatedTitle("title.parameters"),
    children: [
      {
        path: "",
        loadComponent: () =>
          import("./parameters/parameters.component").then(
            (module) => module.ParametersComponent,
          ),
      },
      {
        path: ":uuid",
        resolve: { parameters: ZaakafhandelParametersResolver },
        children: [
          {
            path: "",
            title: (route) =>
              (route.data["parameters"] as ResolvedParameters)
                .zaaktype.omschrijving ?? "",
            loadComponent: () =>
              import("./parameters-edit/parameters-edit.component").then(
                (module) => module.ParametersEditComponent,
              ),
          },
        ],
      },
    ],
  },
  {
    path: "bpmn-procesdefinities",
    title: translatedTitle("title.bpmn-procesdefinities"),
    loadComponent: () =>
      import(
        "./bpmn-process-definitions/bpmn-process-definitions.component"
      ).then((module) => module.BpmnProcessDefinitionsComponent),
  },
  {
    path: "referentietabellen",
    title: translatedTitle("title.referentietabellen"),
    loadComponent: () =>
      import("./referentie-tabellen/referentie-tabellen.component").then(
        (module) => module.ReferentieTabellenComponent,
      ),
  },
  {
    path: "check",
    title: translatedTitle("title.inrichtingscheck"),
    loadComponent: () =>
      import("./inrichtingscheck/inrichtingscheck.component").then(
        (module) => module.InrichtingscheckComponent,
      ),
  },
  {
    path: "mailtemplates",
    title: translatedTitle("title.mailtemplates"),
    children: [
      {
        path: "",
        loadComponent: () =>
          import("./mailtemplates/mailtemplates.component").then(
            (module) => module.MailtemplatesComponent,
          ),
      },
      {
        path: "toevoegen",
        title: translatedTitle("actie.toevoegen"),
        loadComponent: () =>
          import("./mailtemplate/mailtemplate.component").then(
            (module) => module.MailtemplateComponent,
          ),
      },
      {
        path: ":id",
        resolve: { template: MailtemplateResolver },
        children: [
          {
            path: "",
            title: (route) =>
              (route.data["template"] as ResolvedMailtemplate).mailTemplateNaam,
            loadComponent: () =>
              import("./mailtemplate/mailtemplate.component").then(
                (module) => module.MailtemplateComponent,
              ),
          },
        ],
      },
    ],
  },
  { path: "mailtemplate", redirectTo: "mailtemplates/toevoegen" },
  { path: "mailtemplate/:id", redirectTo: "mailtemplates/:id" },
];
