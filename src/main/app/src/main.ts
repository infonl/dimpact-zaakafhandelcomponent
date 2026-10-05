/*
 * SPDX-FileCopyrightText: 2021 Atos, 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { registerLocaleData } from "@angular/common";
import localeNl from "@angular/common/locales/nl";
import { bootstrapApplication } from "@angular/platform-browser";

import { AppComponent } from "./app/app.component";
import { appConfig } from "./app/app.config";
import { alterMoment } from "./functions";

alterMoment();
registerLocaleData(localeNl, "nl-NL");

bootstrapApplication(AppComponent, appConfig).catch((err) =>
  console.error(err),
);
