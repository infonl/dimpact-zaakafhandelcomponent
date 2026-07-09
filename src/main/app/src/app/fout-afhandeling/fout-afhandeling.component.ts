/*
 * SPDX-FileCopyrightText: 2021 Atos
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { Component, OnInit } from "@angular/core";
import { ReferentieTabelService } from "../admin/referentie-tabel.service";
import { FoutAfhandelingService } from "./fout-afhandeling.service";

@Component({
  selector: "zac-fout-afhandeling",
  templateUrl: "./fout-afhandeling.component.html",
  styleUrls: ["./fout-afhandeling.component.less"],
})
export class FoutAfhandelingComponent implements OnInit {
  bericht: string;
  foutmelding: string;
  serverErrorTexts = this.referentieTabelService.listServerErrorTexts();

  constructor(
    private service: FoutAfhandelingService,
    private referentieTabelService: ReferentieTabelService,
  ) {}

  ngOnInit(): void {
    this.bericht = this.service.bericht;
    this.foutmelding = this.service.foutmelding;
  }
}
