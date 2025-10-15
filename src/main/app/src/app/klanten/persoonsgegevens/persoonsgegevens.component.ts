/*
 * SPDX-FileCopyrightText: 2022 Atos, 2024 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { Component, inject, input, output } from "@angular/core";
import { injectQuery } from "@tanstack/angular-query-experimental";
import { IndicatiesLayout } from "../../shared/indicaties/indicaties.component";
import { GeneratedType } from "../../shared/utils/generated-types";
import { KlantenService } from "../klanten.service";

@Component({
  selector: "zac-persoongegevens",
  styleUrls: ["./persoonsgegevens.component.less"],
  templateUrl: "./persoonsgegevens.component.html",
})
export class PersoonsgegevensComponent {
  private readonly klantenService = inject(KlantenService);

  protected isVerwijderbaar = input(false);
  protected isWijzigbaar = input(false);
  protected bsn = input.required<string>();
  protected zaakIdentificatie = input.required<string>();

  protected delete = output<GeneratedType<"RestPersoon">>();
  protected edit = output<GeneratedType<"RestPersoon">>();

  protected readonly persoonQuery = injectQuery(() =>
    this.klantenService.readPersoon(this.bsn(), this.zaakIdentificatie()),
  );

  protected readonly indicatiesLayout = IndicatiesLayout;
}
