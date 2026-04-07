/*
 * SPDX-FileCopyrightText: 2022 Atos, 2024 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { NgIf } from "@angular/common";
import {
  Component,
  computed,
  effect,
  inject,
  input,
  output,
  signal,
} from "@angular/core";
import { MatButtonModule } from "@angular/material/button";
import { MatExpansionModule } from "@angular/material/expansion";
import { MatIconModule } from "@angular/material/icon";
import { MatProgressSpinnerModule } from "@angular/material/progress-spinner";
import { MatTooltipModule } from "@angular/material/tooltip";
import { RouterLink } from "@angular/router";
import { TranslateModule } from "@ngx-translate/core";
import { injectQuery } from "@tanstack/angular-query-experimental";
import { IndicatiesLayout } from "../../shared/indicaties/indicaties.component";
import { PersoonIndicatiesComponent } from "../../shared/indicaties/persoon-indicaties/persoon-indicaties.component";
import { DatumPipe } from "../../shared/pipes/datum.pipe";
import { EmptyPipe } from "../../shared/pipes/empty.pipe";
import { StaticTextComponent } from "../../shared/static-text/static-text.component";
import { GeneratedType } from "../../shared/utils/generated-types";
import { KlantenService } from "../klanten.service";

@Component({
  selector: "zac-persoongegevens",
  styleUrls: ["./persoonsgegevens.component.less"],
  templateUrl: "./persoonsgegevens.component.html",
  standalone: true,
  imports: [
    NgIf,
    MatExpansionModule,
    MatProgressSpinnerModule,
    MatIconModule,
    MatTooltipModule,
    MatButtonModule,
    RouterLink,
    TranslateModule,
    DatumPipe,
    EmptyPipe,
    PersoonIndicatiesComponent,
    StaticTextComponent,
  ],
})
export class PersoonsgegevensComponent {
  private readonly klantenService = inject(KlantenService);

  protected zaak = input.required<GeneratedType<"RestZaak">>();

  protected delete = output<GeneratedType<"RestPersoon">>();
  protected edit = output<GeneratedType<"RestPersoon">>();

  protected readonly temporaryPersonId = computed(
    () => this.zaak().initiatorIdentificatie?.temporaryPersonId ?? "",
  );

  protected readonly persoonQuery = injectQuery(() =>
    this.klantenService.readPersoon(
      this.temporaryPersonId(),
      this.zaak().zaaktype.uuid,
    ),
  );

  protected readonly isDisabled = signal(false);

  protected readonly indicatiesLayout = IndicatiesLayout;

  constructor() {
    effect(() => {
      this.isDisabled.set(
        this.persoonQuery.isLoading() || !!this.persoonQuery.error(),
      );
    });
  }
}
