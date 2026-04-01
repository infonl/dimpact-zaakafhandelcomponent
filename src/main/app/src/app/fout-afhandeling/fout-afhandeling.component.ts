/*
 * SPDX-FileCopyrightText: 2021 Atos, 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { AsyncPipe, NgFor } from "@angular/common";
import { Component, OnInit } from "@angular/core";
import { MatCardModule } from "@angular/material/card";
import { MatIconModule } from "@angular/material/icon";
import { TranslateModule } from "@ngx-translate/core";
import { ReferentieTabelService } from "../admin/referentie-tabel.service";
import { FoutAfhandelingService } from "./fout-afhandeling.service";

@Component({
  selector: "zac-fout-afhandeling",
  templateUrl: "./fout-afhandeling.component.html",
  styleUrls: ["./fout-afhandeling.component.less"],
  standalone: true,
  imports: [AsyncPipe, NgFor, MatCardModule, MatIconModule, TranslateModule],
})
export class FoutAfhandelingComponent implements OnInit {
  protected bericht: string | null = null;
  protected foutmelding: string | null = null;
  protected serverErrorTexts = this.referentieTabelService.listServerErrorTexts();

  constructor(
    private readonly foutAfhandelingService: FoutAfhandelingService,
    private readonly referentieTabelService: ReferentieTabelService,
  ) {}

  ngOnInit() {
    this.foutmelding = this.foutAfhandelingService.foutmelding;
    this.bericht = this.foutAfhandelingService.bericht;
  }
}
