/*
 * SPDX-FileCopyrightText: 2021 Atos, 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { Component, inject, OnInit, signal } from "@angular/core";
import { PageEvent } from "@angular/material/paginator";
import { ActivatedRoute } from "@angular/router";
import { QueryClient } from "@tanstack/angular-query-experimental";
import { GebruikersvoorkeurenService } from "../../../gebruikersvoorkeuren/gebruikersvoorkeuren.service";
import { runMutation } from "../../http/run-mutation";

import { GeneratedType } from "../../utils/generated-types";
import { TabelGegevens } from "../model/tabel-gegevens";

@Component({
  template: "",
  standalone: true,
})
export abstract class WerklijstComponent implements OnInit {
  abstract gebruikersvoorkeurenService: GebruikersvoorkeurenService;
  abstract route: ActivatedRoute;
  protected readonly aantalPerPagina = signal(0);
  protected readonly pageSizeOptions = signal([0]);
  protected readonly werklijstRechten =
    signal<GeneratedType<"RestWerklijstRechten"> | null>(null);

  protected readonly queryClient = inject(QueryClient);

  ngOnInit(): void {
    this.route.data.subscribe((data) => {
      const tabelGegevens: TabelGegevens = data.tabelGegevens;
      this.aantalPerPagina.set(tabelGegevens.aantalPerPagina);
      this.pageSizeOptions.set(tabelGegevens.pageSizeOptions);
      this.werklijstRechten.set(tabelGegevens.werklijstRechten);
    });
  }

  protected paginatorChanged($event: PageEvent) {
    if (this.aantalPerPagina() !== $event.pageSize) {
      this.aantalPerPagina.set($event.pageSize);
      runMutation(
        this.queryClient,
        this.gebruikersvoorkeurenService.updateAantalPerPagina(
          this.getWerklijst(),
          this.aantalPerPagina(),
        ),
        undefined as never,
      ).subscribe();
    }
  }

  abstract getWerklijst(): GeneratedType<"Werklijst">;
}
