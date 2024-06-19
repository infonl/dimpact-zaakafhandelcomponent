/*
 * SPDX-FileCopyrightText: 2021 Atos
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { Component, OnInit } from "@angular/core";
import { FoutAfhandelingService } from "./fout-afhandeling.service";
import {Observable} from "rxjs";

@Component({
  selector: "zac-fout-afhandeling",
  templateUrl: "./fout-afhandeling.component.html",
  styleUrls: ["./fout-afhandeling.component.less"],
})
export class FoutAfhandelingComponent implements OnInit {
  bericht: string;
  foutmelding: string;
  serverErrorTexts: Observable<string[]>;

  constructor(private service: FoutAfhandelingService) {}

  ngOnInit(): void {
    this.bericht = this.service.bericht;
    this.foutmelding = this.service.foutmelding;
    this.serverErrorTexts = this.service.serverErrorTexts;
  }
}
