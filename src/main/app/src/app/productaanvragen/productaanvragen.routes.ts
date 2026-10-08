/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { Routes } from "@angular/router";
import { TabelGegevensResolver } from "../shared/dynamic-table/datasource/tabel-gegevens-resolver.service";
import { GeneratedType } from "../shared/utils/generated-types";
import { InboxProductaanvragenListComponent } from "./inbox-productaanvragen-list/inbox-productaanvragen-list.component";

export const PRODUCTAANVRAGEN_ROUTES: Routes = [
  {
    path: "inbox",
    component: InboxProductaanvragenListComponent,
    resolve: { tabelGegevens: TabelGegevensResolver },
    data: {
      werklijst: "INBOX_PRODUCTAANVRAGEN" satisfies GeneratedType<"Werklijst">,
    },
  },
];
