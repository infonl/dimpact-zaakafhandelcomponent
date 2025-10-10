/*
 * SPDX-FileCopyrightText: 2022 Atos
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { Component, computed, inject, signal } from "@angular/core";
import { ActivatedRoute } from "@angular/router";
import { injectQuery } from "@tanstack/angular-query-experimental";
import { UtilService } from "../../core/service/util.service";
import { TextIcon } from "../../shared/edit/text-icon";
import { GeneratedType } from "../../shared/utils/generated-types";
import { KlantenService } from "../klanten.service";

@Component({
  templateUrl: "./bedrijf-view.component.html",
  styleUrls: ["./bedrijf-view.component.less"],
})
export class BedrijfViewComponent {
  private readonly klantenService = inject(KlantenService);
  private readonly utilService = inject(UtilService);
  private readonly route = inject(ActivatedRoute);

  protected readonly bedrijf = signal<GeneratedType<"RestBedrijf"> | null>(
    null,
  );
  protected readonly vestigingsprofielOphalen = signal(false);

  protected readonly vestigingsprofielOphalenMogelijk = computed(
    () => !!this.bedrijf()?.vestigingsnummer,
  );

  protected readonly vestigingsprofielQuery = injectQuery(() => ({
    ...this.klantenService.readVestigingsprofiel(
      this.bedrijf()!.vestigingsnummer!,
    ),
    enabled: this.vestigingsprofielOphalen(),
  }));

  protected readonly warningIcon = new TextIcon(
    () => true,
    "warning",
    "warning-icon",
    "",
    "error",
  );

  constructor() {
    this.utilService.setTitle("bedrijfsgegevens");
    this.route.data.subscribe((data) => {
      this.bedrijf.set(data.bedrijf);
    });
  }

  protected ophalenVestigingsprofiel() {
    this.vestigingsprofielOphalen.set(true);
  }
}
