/*
 * SPDX-FileCopyrightText: 2022 Atos
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { Component, OnInit } from "@angular/core";
import { ActivatedRoute } from "@angular/router";
import { UtilService } from "../../core/service/util.service";
import { KlantenService } from "../klanten.service";
import { Bedrijf } from "../model/bedrijven/bedrijf";
import { Vestigingsprofiel } from "../model/bedrijven/vestigingsprofiel";

@Component({
  templateUrl: "./bedrijf-view.component.html",
  styleUrls: ["./bedrijf-view.component.less"],
})
export class BedrijfViewComponent implements OnInit {
  bedrijf: Bedrijf;
  vestigingsprofiel: Vestigingsprofiel = null;
  vestigingsprofielOphalenMogelijk = true;

  constructor(
    private utilService: UtilService,
    private _route: ActivatedRoute,
    public klantenService: KlantenService,
  ) {}

  ngOnInit(): void {
    this.utilService.setTitle("bedrijfsgegevens");
    this._route.data.subscribe((data) => {
      this.bedrijf = data.bedrijf;
      this.vestigingsprofielOphalenMogelijk = !!this.bedrijf.vestigingsnummer;
    });
  }

  ophalenVestigingsprofiel() {
    this.vestigingsprofielOphalenMogelijk = false;
    this.klantenService
      .readVestigingsprofiel(this.bedrijf.vestigingsnummer)
      .subscribe((value) => {
        this.vestigingsprofiel = value;
      });
  }
}
