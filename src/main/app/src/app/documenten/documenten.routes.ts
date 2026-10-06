/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { Routes } from "@angular/router";
import { TabelGegevensResolver } from "../shared/dynamic-table/datasource/tabel-gegevens-resolver.service";
import { GeneratedType } from "../shared/utils/generated-types";
import { InboxDocumentenListComponent } from "./inbox-documenten-list/inbox-documenten-list.component";
import { OntkoppeldeDocumentenListComponent } from "./ontkoppelde-documenten-list/ontkoppelde-documenten-list.component";

export const DOCUMENTEN_ROUTES: Routes = [
  {
    path: "ontkoppelde",
    component: OntkoppeldeDocumentenListComponent,
    resolve: { tabelGegevens: TabelGegevensResolver },
    data: {
      werklijst: "ONTKOPPELDE_DOCUMENTEN" satisfies GeneratedType<"Werklijst">,
    },
  },
  {
    path: "inbox",
    component: InboxDocumentenListComponent,
    resolve: { tabelGegevens: TabelGegevensResolver },
    data: {
      werklijst: "INBOX_DOCUMENTEN" satisfies GeneratedType<"Werklijst">,
    },
  },
];
