/*
 * SPDX-FileCopyrightText: 2021 Atos, 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { Component, inject, OnInit } from "@angular/core";
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
  protected aantalPerPagina = 0;
  protected pageSizeOptions = [0];
  protected werklijstRechten!: GeneratedType<"RestWerklijstRechten">;

  protected readonly queryClient = inject(QueryClient);

  protected constructor() {}

  ngOnInit(): void {
    this.route.data.subscribe((data) => {
      const tabelGegevens: TabelGegevens = data.tabelGegevens;
      this.aantalPerPagina = tabelGegevens.aantalPerPagina;
      this.pageSizeOptions = tabelGegevens.pageSizeOptions;
      this.werklijstRechten = tabelGegevens.werklijstRechten;
    });
  }

  protected paginatorChanged($event: PageEvent) {
    if (this.aantalPerPagina !== $event.pageSize) {
      this.aantalPerPagina = $event.pageSize;
      runMutation(
        this.queryClient,
        this.gebruikersvoorkeurenService.updateAantalPerPagina(
          this.getWerklijst(),
          this.aantalPerPagina,
        ),
        undefined as never,
      ).subscribe();
    }
  }

  abstract getWerklijst(): GeneratedType<"Werklijst">;
}
