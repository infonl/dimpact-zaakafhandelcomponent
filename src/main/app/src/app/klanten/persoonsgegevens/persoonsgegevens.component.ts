/*
 * SPDX-FileCopyrightText: 2022 Atos, 2024 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { Component, inject, input, output } from "@angular/core";
import { Router } from "@angular/router";
import { injectQuery } from "@tanstack/angular-query-experimental";
import { IndicatiesLayout } from "../../shared/indicaties/indicaties.component";
import { GeneratedType } from "../../shared/utils/generated-types";
import { KlantenService } from "../klanten.service";

@Component({
  selector: "zac-persoongegevens",
  styleUrls: ["./persoonsgegevens.component.less"],
  templateUrl: "./persoonsgegevens.component.html",
  standalone: false,
})
export class PersoonsgegevensComponent {
  private readonly klantenService = inject(KlantenService);
  private readonly router = inject(Router);

  protected isVerwijderbaar = input(false);
  protected isWijzigbaar = input(false);
  protected zaaktypeUuid = input.required<string>();
  protected bsn = input.required<string>();

  protected delete = output<GeneratedType<"RestPersoon">>();
  protected edit = output<GeneratedType<"RestPersoon">>();

  protected readonly persoonQuery = injectQuery(() =>
    this.klantenService.readPersoon(this.bsn(), this.zaaktypeUuid()),
  );

  protected readonly indicatiesLayout = IndicatiesLayout;

  protected openPersoonPagina(event: MouseEvent) {
    event.stopPropagation();

    this.router.navigateByUrl("/persoon", {
      state: { bsn: this.bsn() },
    });
  }
}
