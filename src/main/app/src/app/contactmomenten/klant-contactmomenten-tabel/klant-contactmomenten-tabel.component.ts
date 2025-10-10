/*
 * SPDX-FileCopyrightText: 2022 Atos
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { Component, effect, inject, input, ViewChild } from "@angular/core";
import { MatPaginator } from "@angular/material/paginator";
import { MatTableDataSource } from "@angular/material/table";
import { injectMutation } from "@tanstack/angular-query-experimental";
import { UtilService } from "../../core/service/util.service";
import { GeneratedType } from "../../shared/utils/generated-types";
import { BetrokkeneIdentificatie } from "../../zaken/model/betrokkeneIdentificatie";
import { ContactmomentenService } from "../contactmomenten.service";

@Component({
  selector: "zac-klant-contactmomenten-tabel",
  templateUrl: "./klant-contactmomenten-tabel.component.html",
  styleUrls: ["./klant-contactmomenten-tabel.component.less"],
})
export class KlantContactmomentenTabelComponent {
  private readonly contactmomentenService = inject(ContactmomentenService);
  private readonly utilService = inject(UtilService);

  protected readonly klant = input<
    GeneratedType<"RestPersoon"> | GeneratedType<"RestBedrijf">
  >();

  @ViewChild(MatPaginator) paginator!: MatPaginator;
  dataSource = new MatTableDataSource<GeneratedType<"RestContactmoment">>();
  protected readonly columns = [
    "registratiedatum",
    "kanaal",
    "initiatiefnemer",
    "medewerker",
    "tekst",
  ] as const;

  protected readonly contactMomentenMutation = injectMutation(() => ({
    ...this.contactmomentenService.listContactmomenten(),
    onSuccess: ({ resultaten }) => {
      this.dataSource.data = resultaten ?? [];
      this.paginator.pageIndex = 0;
      this.paginator.length = resultaten?.length ?? 0;
    },
  }));

  constructor() {
    effect(() => {
      const klant = this.klant();
      if (!klant) return;

      this.contactMomentenMutation.mutate({
        betrokkene: new BetrokkeneIdentificatie(klant),
        page: 0,
      });
    });

    effect(() => {
      this.utilService.setLoading(this.contactMomentenMutation.isPending());
    });
  }
}
