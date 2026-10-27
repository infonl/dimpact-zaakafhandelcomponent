/*
 * SPDX-FileCopyrightText: 2023 Atos, 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import {
  Component,
  computed,
  inject,
  input,
  linkedSignal,
} from "@angular/core";
import { MatButtonModule } from "@angular/material/button";
import { MatCardModule } from "@angular/material/card";
import { MatIconModule } from "@angular/material/icon";
import { MatPaginatorModule, PageEvent } from "@angular/material/paginator";
import { MatSlideToggleModule } from "@angular/material/slide-toggle";
import { MatSortModule, Sort } from "@angular/material/sort";
import { MatTableModule } from "@angular/material/table";
import { RouterModule } from "@angular/router";
import { TranslatePipe } from "@ngx-translate/core";
import { injectLastSuccessfulSearch } from "../../shared/http/last-successful-search";
import { DatumPipe } from "../../shared/pipes/datum.pipe";
import { EmptyPipe } from "../../shared/pipes/empty.pipe";
import { DateRangeFilterComponent } from "../../shared/table-zoek-filters/date-range-filter/date-range-filter.component";
import { FacetFilterComponent } from "../../shared/table-zoek-filters/facet-filter/facet-filter.component";
import { TekstFilterComponent } from "../../shared/table-zoek-filters/tekst-filter/tekst-filter.component";
import { GeneratedType } from "../../shared/utils/generated-types";
import { ZaakIdentificatieComponent } from "../../shared/zaak-identificatie/zaak-identificatie.component";
import { ZaakZoekObject } from "../../zoeken/model/zaken/zaak-zoek-object";
import {
  getDefaultZoekParameters,
  ZoekParameters,
} from "../../zoeken/model/zoek-parameters";
import { ZoekResultaat } from "../../zoeken/model/zoek-resultaat";
import { ZoekenService } from "../../zoeken/zoeken.service";

@Component({
  selector: "zac-bag-zaken-tabel",
  templateUrl: "./bag-zaken-tabel.component.html",
  styleUrls: ["./bag-zaken-tabel.component.less"],
  standalone: true,
  imports: [
    ZaakIdentificatieComponent,
    MatCardModule,
    MatTableModule,
    MatSortModule,
    MatPaginatorModule,
    MatSlideToggleModule,
    MatIconModule,
    MatButtonModule,
    RouterModule,
    TranslatePipe,
    TekstFilterComponent,
    FacetFilterComponent,
    DateRangeFilterComponent,
    DatumPipe,
    EmptyPipe,
  ],
})
export class BagZakenTabelComponent {
  private readonly zoekenService = inject(ZoekenService);

  readonly BagObjectIdentificatie = input.required<string>();
  protected readonly columns = [
    "identificatie",
    "status",
    "groep",
    "behandelaar",
    "startdatum",
    "zaaktype",
    "omschrijving",
    "url",
  ] as const;
  protected readonly filterColumns = this.columns.map((n) => n + "_filter");

  private readonly requestedZoekParameters = linkedSignal<
    string,
    ZoekParameters
  >({
    source: () => this.BagObjectIdentificatie(),
    computation: (bagObjectIdentificatie, previous) => {
      const zoekParameters = previous?.value ?? {
        ...getDefaultZoekParameters(),
        type: "ZAAK",
        rows: 10,
        alleenOpenstaandeZaken: true,
      };
      return {
        ...zoekParameters,
        page: 0,
        zoeken: {
          ...zoekParameters.zoeken,
          ZAAK_BAGOBJECTEN: bagObjectIdentificatie,
        },
      };
    },
  });

  protected readonly search = injectLastSuccessfulSearch({
    requested: this.requestedZoekParameters,
    query: (zoekParameters) => this.zoekenService.list(zoekParameters),
  });

  protected readonly zoekParameters = this.search.requested;
  protected readonly zoekResultaat = computed(
    (): ZoekResultaat<ZaakZoekObject> =>
      (this.search.data() as ZoekResultaat<ZaakZoekObject> | undefined) ?? {
        resultaten: [],
        totaal: 0,
        filters: {},
        foutmelding: "",
      },
  );

  protected filterChanged(
    veld: string,
    filter: GeneratedType<"FilterParameters">,
  ) {
    const { filters } = this.zoekParameters();
    this.searchFirstPage({ filters: { ...filters, [veld]: filter } });
  }

  protected zoekenChanged(veld: string, waarde: string) {
    const { zoeken } = this.zoekParameters();
    this.searchFirstPage({ zoeken: { ...zoeken, [veld]: waarde } });
  }

  protected datumChanged(veld: string, range: GeneratedType<"RestDatumRange">) {
    const { datums } = this.zoekParameters();
    this.searchFirstPage({ datums: { ...datums, [veld]: range } });
  }

  protected afgerondeZakenChanged(inclusiefAfgerondeZaken: boolean) {
    this.searchFirstPage({ alleenOpenstaandeZaken: !inclusiefAfgerondeZaken });
  }

  protected sortChanged({ active, direction }: Sort) {
    this.searchFirstPage({
      sorteerVeld: active as GeneratedType<"SorteerVeld">,
      sorteerRichting: direction,
    });
  }

  protected pageChanged({ pageIndex, pageSize }: PageEvent) {
    this.search.request({
      ...this.zoekParameters(),
      page: pageIndex,
      rows: pageSize,
    });
  }

  private searchFirstPage(changes: Partial<ZoekParameters>) {
    this.search.request({ ...this.zoekParameters(), ...changes, page: 0 });
  }
}
