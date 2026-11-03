/*
 * SPDX-FileCopyrightText: 2022 Atos, 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import {
  AfterViewInit,
  Component,
  effect,
  inject,
  input,
  signal,
  untracked,
  viewChild,
} from "@angular/core";
import { MatCardModule } from "@angular/material/card";
import { MatPaginator, MatPaginatorModule } from "@angular/material/paginator";
import { MatTableModule } from "@angular/material/table";
import { TranslatePipe } from "@ngx-translate/core";
import { QueryClient } from "@tanstack/angular-query-experimental";
import { startWith, switchMap, tap } from "rxjs/operators";
import { UtilService } from "../../core/service/util.service";
import { PutBody } from "../../shared/http/http-client";
import { runQuery } from "../../shared/http/run-query";
import { DatumPipe } from "../../shared/pipes/datum.pipe";
import { EmptyPipe } from "../../shared/pipes/empty.pipe";
import { GeneratedType } from "../../shared/utils/generated-types";
import { ContactmomentenService } from "../contactmomenten.service";

@Component({
  selector: "zac-klant-contactmomenten-tabel",
  templateUrl: "./klant-contactmomenten-tabel.component.html",
  styleUrls: ["./klant-contactmomenten-tabel.component.less"],
  standalone: true,
  imports: [
    MatCardModule,
    MatTableModule,
    MatPaginatorModule,
    TranslatePipe,
    DatumPipe,
    EmptyPipe,
  ],
})
export class KlantContactmomentenTabelComponent implements AfterViewInit {
  private readonly contactmomentenService = inject(ContactmomentenService);
  private readonly utilService = inject(UtilService);

  readonly bsn = input<string>();
  readonly vestigingsnummer = input<string>();
  readonly paginator = viewChild.required(MatPaginator);
  protected readonly contactmomenten = signal<
    GeneratedType<"RestContactmoment">[]
  >([]);
  protected readonly totaal = signal(0);
  protected columns: string[] = [
    "registratiedatum",
    "kanaal",
    "initiatiefnemer",
    "medewerker",
    "tekst",
  ];
  private listParameters: PutBody<"/rest/klanten/contactmomenten"> = {
    page: 0,
  };
  private init = false;
  protected readonly isLoadingResults = signal(true);

  private readonly queryClient = inject(QueryClient);

  constructor() {
    effect(() => {
      this.bsn();
      this.vestigingsnummer();
      untracked(() => {
        if (this.init) {
          this.paginator().pageIndex = 0;
          this.paginator().page.emit();
        }
      });
    });
  }

  ngAfterViewInit(): void {
    this.init = true;
    const stopLoading = () => {
      this.isLoadingResults.set(false);
      this.utilService.setLoading(false);
    };
    this.paginator()
      .page.pipe(
        startWith({}),
        switchMap(() => {
          this.isLoadingResults.set(true);
          this.utilService.setLoading(true);
          return runQuery(this.queryClient, this.loadContactmomenten());
        }),
        tap({ next: stopLoading, error: stopLoading }),
      )
      .subscribe((resultaat) => {
        this.totaal.set(resultaat.totaal ?? 0);
        this.contactmomenten.set(resultaat.resultaten ?? []);
      });
  }

  private loadContactmomenten() {
    this.listParameters.page = this.paginator().pageIndex;
    this.listParameters.bsn = this.bsn();
    this.listParameters.vestigingsnummer = this.vestigingsnummer();
    return this.contactmomentenService.listContactmomenten(this.listParameters);
  }
}
