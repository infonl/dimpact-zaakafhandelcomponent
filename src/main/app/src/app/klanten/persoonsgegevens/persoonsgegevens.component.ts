/*
 * SPDX-FileCopyrightText: 2022 Atos, 2024 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { Component, EventEmitter, Output, input } from "@angular/core";
import { toObservable } from "@angular/core/rxjs-interop";
import { of, shareReplay, switchMap } from "rxjs";
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

  isVerwijderbaar = input<boolean | undefined>(false);
  isWijzigbaar = input<boolean | undefined>(false);
  bsn = input<string | undefined | null>(null);
  zaakIdentificatie = input.required<string>();
  action = input.required<string>();

  bsn$ = toObservable(this.bsn);

  persoon$ = this.bsn$.pipe(
    switchMap((bsn) => {
      if (!bsn) return of(undefined);
      return this.klantenService.readPersoon(bsn, {
        context: this.zaakIdentificatie(),
        action: this.action(),
      });
    }),
    shareReplay({ bufferSize: 1, refCount: true }),
  );

  constructor(private klantenService: KlantenService) {}

  protected readonly indicatiesLayout = IndicatiesLayout;
}
