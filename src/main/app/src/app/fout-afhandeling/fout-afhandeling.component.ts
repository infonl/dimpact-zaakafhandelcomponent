/*
 * SPDX-FileCopyrightText: 2021 Atos, 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { Component, OnInit } from "@angular/core";
import { ReferentieTabelService } from "../admin/referentie-tabel.service";
import { FoutAfhandelingService } from "./fout-afhandeling.service";

@Component({
  selector: "zac-fout-afhandeling",
  templateUrl: "./fout-afhandeling.component.html",
  styleUrls: ["./fout-afhandeling.component.less"],
  standalone: false,
})
export class FoutAfhandelingComponent implements OnInit {
  bericht: string | null = null;
  foutmelding: string | null = null;
  serverErrorTexts = this.referentieTabelService.listServerErrorTexts();

  constructor(
    private readonly foutAfhandelingService: FoutAfhandelingService,
    private readonly referentieTabelService: ReferentieTabelService,
  ) {}

  ngOnInit() {
    this.foutmelding = this.foutAfhandelingService.foutmelding;
    this.bericht = this.foutAfhandelingService.bericht;
  }
}
