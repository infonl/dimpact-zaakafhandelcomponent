/*
 * SPDX-FileCopyrightText: 2021 Atos, 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { Component, inject } from "@angular/core";
import { MatCardModule } from "@angular/material/card";
import { MatIconModule } from "@angular/material/icon";
import { TranslatePipe } from "@ngx-translate/core";
import { injectQuery } from "@tanstack/angular-query-experimental";
import { ReferentieTabelService } from "../admin/referentie-tabel.service";
import { FoutAfhandelingService } from "./fout-afhandeling.service";

@Component({
  selector: "zac-fout-afhandeling",
  templateUrl: "./fout-afhandeling.component.html",
  styleUrls: ["./fout-afhandeling.component.less"],
  standalone: true,
  imports: [MatCardModule, MatIconModule, TranslatePipe],
})
export class FoutAfhandelingComponent {
  private readonly foutAfhandelingService = inject(FoutAfhandelingService);
  private readonly referentieTabelService = inject(ReferentieTabelService);

  protected readonly bericht = this.foutAfhandelingService.bericht;
  protected readonly foutmelding = this.foutAfhandelingService.foutmelding;
  protected readonly serverErrorTextsQuery = injectQuery(() => ({
    ...this.referentieTabelService.listServerErrorTexts(),
    // A failure would open the error dialog, which loads these texts again.
    meta: { reportErrors: false },
    retry: false,
  }));
}
