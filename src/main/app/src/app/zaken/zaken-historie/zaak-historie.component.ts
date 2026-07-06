/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { NgIf } from "@angular/common";
import {
  AfterViewInit,
  Component,
  effect,
  inject,
  input,
  ViewChild,
} from "@angular/core";
import { MatSort, MatSortModule } from "@angular/material/sort";
import { MatTableDataSource, MatTableModule } from "@angular/material/table";
import { MatTooltipModule } from "@angular/material/tooltip";
import { TranslateModule } from "@ngx-translate/core";
import { injectQuery, QueryClient } from "@tanstack/angular-query-experimental";
import { DatumPipe } from "../../shared/pipes/datum.pipe";
import { EmptyPipe } from "../../shared/pipes/empty.pipe";
import { LocationPipe } from "../../shared/pipes/location.pipe";
import { MimetypeToExtensionPipe } from "../../shared/pipes/mimetypeToExtension.pipe";
import { ReadMoreComponent } from "../../shared/read-more/read-more.component";
import { GeneratedType } from "../../shared/utils/generated-types";
import { ZakenService } from "../zaken.service";

@Component({
  selector: "zac-zaak-historie",
  templateUrl: "./zaak-historie.component.html",
  styleUrls: ["./zaak-historie.component.less"],
  standalone: true,
  imports: [
    NgIf,
    MatSortModule,
    MatTableModule,
    MatTooltipModule,
    TranslateModule,
    DatumPipe,
    EmptyPipe,
    LocationPipe,
    MimetypeToExtensionPipe,
    ReadMoreComponent,
  ],
})
export class ZaakHistorieComponent implements AfterViewInit {
  private readonly zakenService = inject(ZakenService);
  private readonly queryClient = inject(QueryClient);

  readonly zaak = input.required<GeneratedType<"RestZaak">>();

  protected readonly historieQuery = injectQuery(() =>
    this.zakenService.listHistorieVoorZaakQuery(this.zaak().uuid),
  );

  protected readonly historie =
    new MatTableDataSource<GeneratedType<"RestTaskHistoryLine">>();
  protected readonly historieColumns = [
    "datum",
    "gebruiker",
    "wijziging",
    "actie",
    "oudeWaarde",
    "nieuweWaarde",
    "toelichting",
  ] as const;

  @ViewChild("historieSort") private historieSort!: MatSort;

  constructor() {
    effect(() => {
      this.historie.data = this.historieQuery.data() ?? [];
    });
  }

  ngAfterViewInit() {
    this.historie.sortingDataAccessor = (item, property) => {
      switch (property) {
        case "datum":
          return String(item.datumTijd);
        case "gebruiker":
          return (item as unknown as { door: string }).door;
        default:
          return String(item[property as keyof typeof item]);
      }
    };

    this.historie.sort = this.historieSort;
  }

  loadHistorie() {
    this.queryClient.invalidateQueries({
      queryKey: this.zakenService.listHistorieVoorZaakQuery(this.zaak().uuid)
        .queryKey,
    });
  }
}
