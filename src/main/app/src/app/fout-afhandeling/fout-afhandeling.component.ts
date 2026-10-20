/*
 * SPDX-FileCopyrightText: 2021 Atos, 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { Component, inject, OnInit } from "@angular/core";
import { MatCardModule } from "@angular/material/card";
import { MatIconModule } from "@angular/material/icon";
import { TranslateModule } from "@ngx-translate/core";
import { injectQuery } from "@tanstack/angular-query-experimental";
import { ReferentieTabelService } from "../admin/referentie-tabel.service";
import { FoutAfhandelingService } from "./fout-afhandeling.service";

@Component({
  selector: "zac-fout-afhandeling",
  templateUrl: "./fout-afhandeling.component.html",
  styleUrls: ["./fout-afhandeling.component.less"],
  standalone: true,
  imports: [MatCardModule, MatIconModule, TranslateModule],
})
export class FoutAfhandelingComponent implements OnInit {
  private readonly foutAfhandelingService = inject(FoutAfhandelingService);
  private readonly referentieTabelService = inject(ReferentieTabelService);

  protected bericht: string | null = null;
  protected foutmelding: string | null = null;
  protected readonly serverErrorTextsQuery = injectQuery(() =>
    this.referentieTabelService.listServerErrorTextsQuery(),
  );

  ngOnInit() {
    this.foutmelding = this.foutAfhandelingService.foutmelding;
    this.bericht = this.foutAfhandelingService.bericht;
  }
}
