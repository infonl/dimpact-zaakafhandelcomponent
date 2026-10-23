/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { Routes } from "@angular/router";
import { translatedTitle } from "../core/breadcrumbs/route-title";
import { SignaleringenSettingsComponent } from "./signaleringen-settings/signaleringen-settings.component";

export const SIGNALERINGEN_ROUTES: Routes = [
  {
    path: "settings",
    title: translatedTitle("title.signaleringen.settings"),
    component: SignaleringenSettingsComponent,
  },
];
