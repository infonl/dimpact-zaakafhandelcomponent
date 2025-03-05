/*
 * SPDX-FileCopyrightText: 2022 Atos, 2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { Component, EventEmitter, Output, input } from "@angular/core";
import { toObservable } from "@angular/core/rxjs-interop";
import { shareReplay, switchMap } from "rxjs";
import { IndicatiesLayout } from "../../shared/indicaties/indicaties.component";
import { GeneratedType } from "../../shared/utils/generated-types";
import { KlantenService } from "../klanten.service";

@Component({
  selector: "zac-persoongegevens",
  styleUrls: ["./persoonsgegevens.component.less"],
  templateUrl: "./persoonsgegevens.component.html",
})
export class PersoonsgegevensComponent {
  @Output() delete = new EventEmitter<GeneratedType<"RestPersoon">>();
  @Output() edit = new EventEmitter<GeneratedType<"RestPersoon">>();

  isVerwijderbaar = input<boolean>();
  isWijzigbaar = input<boolean>();
  bsn = input<string>();

  bsn$ = toObservable(this.bsn);

  persoon$ = this.bsn$.pipe(
    switchMap((bsn) => this.klantenService.readPersoon(bsn)),
    shareReplay(1),
  );

  constructor(private klantenService: KlantenService) {}

  protected readonly indicatiesLayout = IndicatiesLayout;
}
