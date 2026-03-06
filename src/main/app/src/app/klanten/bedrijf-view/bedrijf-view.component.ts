/*
 * SPDX-FileCopyrightText: 2022 Atos, 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { Component } from "@angular/core";
import { ActivatedRoute } from "@angular/router";
import { UtilService } from "../../core/service/util.service";
import { TextIcon } from "../../shared/edit/text-icon";
import { GeneratedType } from "../../shared/utils/generated-types";
import { KlantenService } from "../klanten.service";

@Component({
  templateUrl: "./bedrijf-view.component.html",
  styleUrls: ["./bedrijf-view.component.less"],
  standalone: false,
})
export class BedrijfViewComponent {
  protected bedrijf: GeneratedType<"RestBedrijf"> | null = null;
  protected vestigingsprofiel: GeneratedType<"RestVestigingsprofiel"> | null =
    null;
  protected vestigingsprofielOphalenMogelijk = true;
  warningIcon = new TextIcon(
    () => true,
    "warning",
    "warning-icon",
    "",
    "error",
  );

  constructor(
    private readonly utilService: UtilService,
    private readonly route: ActivatedRoute,
    private readonly klantenService: KlantenService,
  ) {
    this.utilService.setTitle("bedrijfsgegevens");
    this.route.data.subscribe((data) => {
      this.bedrijf = data.bedrijf;
      this.vestigingsprofielOphalenMogelijk = !!this.bedrijf?.vestigingsnummer;
    });
  }

  protected ophalenVestigingsprofiel() {
    this.vestigingsprofielOphalenMogelijk = false;
    if (!this.bedrijf?.vestigingsnummer) return;

    this.klantenService
      .readVestigingsprofiel(this.bedrijf.vestigingsnummer)
      .subscribe((value) => {
        this.vestigingsprofiel = value;
      });
  }
}
