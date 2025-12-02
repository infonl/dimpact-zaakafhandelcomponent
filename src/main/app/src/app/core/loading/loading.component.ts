/*
 * SPDX-FileCopyrightText: 2021 Atos, 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { Component, inject } from "@angular/core";
import { QueryClient } from "@tanstack/angular-query-experimental";
import { UtilService } from "../service/util.service";

@Component({
  selector: "zac-loading",
  templateUrl: "./loading.component.html",
  styleUrls: ["./loading.component.less"],
})
export class LoadingComponent {
  protected readonly utilService = inject(UtilService);
  protected readonly queryClient = inject(QueryClient);
}
