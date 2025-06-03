/*
 * SPDX-FileCopyrightText: 2022 Atos
 * SPDX-License-Identifier: EUPL-1.2+
 */

import {
  Component,
  EventEmitter,
  Input,
  OnChanges,
  Output,
} from "@angular/core";
import { GeneratedType } from "../../shared/utils/generated-types";
import { KlantenService } from "../klanten.service";

@Component({
  selector: "zac-bedrijfsgegevens",
  templateUrl: "./bedrijfsgegevens.component.html",
  styleUrls: ["./bedrijfsgegevens.component.less"],
})
export class BedrijfsgegevensComponent implements OnChanges {
  @Input() isVerwijderbaar?: boolean = false;
  @Input() isWijzigbaar?: boolean = false;
  @Input() rsinOfVestigingsnummer?: string | null = null;
  @Output() delete = new EventEmitter<GeneratedType<"RestBedrijf">>();
  @Output() edit = new EventEmitter<GeneratedType<"RestBedrijf">>();

  vestigingsprofielOphalenMogelijk = true;
  vestigingsprofiel: GeneratedType<"RestVestigingsprofiel"> | null = null;
  bedrijf: GeneratedType<"RestBedrijf"> | null = null;
  klantExpanded = false;

  constructor(private klantenService: KlantenService) {}

  ngOnChanges(): void {
    this.bedrijf = null;
    this.vestigingsprofiel = null;
    if (!this.rsinOfVestigingsnummer) return;

    this.klantenService
      .readBedrijf(this.rsinOfVestigingsnummer)
      .subscribe((bedrijf) => {
        this.bedrijf = bedrijf;
        this.klantExpanded = true;
        this.vestigingsprofielOphalenMogelijk = !!this.bedrijf.vestigingsnummer;
      });
  }

  ophalenVestigingsprofiel() {
    this.vestigingsprofielOphalenMogelijk = false;
    if (!this.bedrijf?.vestigingsnummer) return;

    this.klantenService
      .readVestigingsprofiel(this.bedrijf.vestigingsnummer)
      .subscribe((value) => {
        this.vestigingsprofiel = value;
      });
  }
}
