/*
 * SPDX-FileCopyrightText: 2022 Atos, 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import {
  Component,
  computed,
  DestroyRef,
  effect,
  inject,
  input,
  linkedSignal,
} from "@angular/core";
import { MatCardModule } from "@angular/material/card";
import { MatPaginatorModule, PageEvent } from "@angular/material/paginator";
import { MatTableModule } from "@angular/material/table";
import { TranslatePipe } from "@ngx-translate/core";
import { UtilService } from "../../core/service/util.service";
import { TypedMatCellDefDirective } from "../../shared/directives/typed-mat-cell-def.directive";
import { PutBody } from "../../shared/http/http-client";
import { injectLastSuccessfulSearch } from "../../shared/http/last-successful-search";
import { DatumPipe } from "../../shared/pipes/datum.pipe";
import { EmptyPipe } from "../../shared/pipes/empty.pipe";
import { ContactmomentenService } from "../contactmomenten.service";

type ContactmomentenSearch = PutBody<"/rest/klanten/contactmomenten">;

@Component({
  selector: "zac-klant-contactmomenten-tabel",
  templateUrl: "./klant-contactmomenten-tabel.component.html",
  styleUrls: ["./klant-contactmomenten-tabel.component.less"],
  standalone: true,
  imports: [
    MatCardModule,
    MatTableModule,
    TypedMatCellDefDirective,
    MatPaginatorModule,
    TranslatePipe,
    DatumPipe,
    EmptyPipe,
  ],
})
export class KlantContactmomentenTabelComponent {
  private readonly contactmomentenService = inject(ContactmomentenService);
  private readonly utilService = inject(UtilService);

  readonly bsn = input<string>();
  readonly vestigingsnummer = input<string>();
  protected columns: string[] = [
    "registratiedatum",
    "kanaal",
    "initiatiefnemer",
    "medewerker",
    "tekst",
  ];

  private readonly requestedSearch = linkedSignal<
    Pick<ContactmomentenSearch, "bsn" | "vestigingsnummer">,
    ContactmomentenSearch
  >({
    source: () => ({
      bsn: this.bsn(),
      vestigingsnummer: this.vestigingsnummer(),
    }),
    computation: (klant) => ({ page: 0, ...klant }),
  });
  private readonly search = injectLastSuccessfulSearch({
    requested: this.requestedSearch,
    query: (search) => this.contactmomentenService.listContactmomenten(search),
  });
  protected readonly page = computed(() => this.search.requested().page ?? 0);
  protected readonly isLoadingResults = computed(() =>
    this.search.query.isFetching(),
  );
  protected readonly contactmomenten = computed(
    () => this.search.data()?.resultaten ?? [],
  );
  protected readonly totaal = computed(() => this.search.data()?.totaal ?? 0);

  constructor() {
    effect(() => this.utilService.setLoading(this.isLoadingResults()));
    inject(DestroyRef).onDestroy(() => this.utilService.setLoading(false));
  }

  protected pageChanged({ pageIndex }: PageEvent) {
    this.search.request({ ...this.search.requested(), page: pageIndex });
  }
}
