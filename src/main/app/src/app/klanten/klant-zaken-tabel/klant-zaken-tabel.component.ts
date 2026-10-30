/*
 * SPDX-FileCopyrightText: 2022 Atos, 2024 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { NgClass } from "@angular/common";
import {
  Component,
  computed,
  inject,
  input,
  linkedSignal,
} from "@angular/core";
import { MatButtonModule } from "@angular/material/button";
import { MatCardModule } from "@angular/material/card";
import { MatFormFieldModule } from "@angular/material/form-field";
import { MatIconModule } from "@angular/material/icon";
import { MatPaginatorModule, PageEvent } from "@angular/material/paginator";
import { MatSelectModule } from "@angular/material/select";
import { MatSlideToggleModule } from "@angular/material/slide-toggle";
import { MatSortModule, Sort } from "@angular/material/sort";
import { MatTableModule } from "@angular/material/table";
import { RouterLink } from "@angular/router";
import { TranslatePipe } from "@ngx-translate/core";
import { injectQuery } from "@tanstack/angular-query-experimental";
import { injectLastSuccessfulSearch } from "../../shared/http/last-successful-search";
import { DatumPipe } from "../../shared/pipes/datum.pipe";
import { EmptyPipe } from "../../shared/pipes/empty.pipe";
import { DateRangeFilterComponent } from "../../shared/table-zoek-filters/date-range-filter/date-range-filter.component";
import { FacetFilterComponent } from "../../shared/table-zoek-filters/facet-filter/facet-filter.component";
import { TekstFilterComponent } from "../../shared/table-zoek-filters/tekst-filter/tekst-filter.component";
import { GeneratedType } from "../../shared/utils/generated-types";
import { ZaakIdentificatieComponent } from "../../shared/zaak-identificatie/zaak-identificatie.component";
import { BetrokkeneIdentificatie } from "../../zaken/model/betrokkeneIdentificatie";
import { ZaakZoekObject } from "../../zoeken/model/zaken/zaak-zoek-object";
import {
  getDefaultZoekParameters,
  hasActiveSearchFilters,
  ZoekParameters,
} from "../../zoeken/model/zoek-parameters";
import { ZoekResultaat } from "../../zoeken/model/zoek-resultaat";
import { ZoekVeld } from "../../zoeken/model/zoek-veld";
import { ZoekenService } from "../../zoeken/zoeken.service";
import { KlantenService } from "../klanten.service";

type KlantZakenZoekopdracht = {
  zoekParameters: ZoekParameters;
  /** The roltype the klant must have in the zaak; `null` for any. */
  betrokkenheid: string | null;
  betrokkeneIdentificatie: string;
};

@Component({
  selector: "zac-klant-zaken-tabel",
  templateUrl: "./klant-zaken-tabel.component.html",
  styleUrls: ["./klant-zaken-tabel.component.less"],
  standalone: true,
  imports: [
    ZaakIdentificatieComponent,
    NgClass,
    MatCardModule,
    MatTableModule,
    MatSortModule,
    MatPaginatorModule,
    MatSlideToggleModule,
    MatFormFieldModule,
    MatSelectModule,
    MatButtonModule,
    MatIconModule,
    RouterLink,
    TranslatePipe,
    DatumPipe,
    EmptyPipe,
    TekstFilterComponent,
    FacetFilterComponent,
    DateRangeFilterComponent,
  ],
})
export class KlantZakenTabelComponent {
  private readonly zoekenService = inject(ZoekenService);
  private readonly klantenService = inject(KlantenService);

  protected readonly klant =
    input.required<GeneratedType<"RestBedrijf" | "RestPersoon">>();

  protected readonly columns = [
    "identificatie",
    "betrokkene",
    "status",
    "groep",
    "behandelaar",
    "startdatum",
    "zaaktype",
    "omschrijving",
    "url",
  ] as const;
  protected readonly filterColumns = this.columns.map((n) => n + "_filter");

  protected readonly distinctRoltypenQuery = injectQuery(() => ({
    ...this.klantenService.listRoltypen(),
    select: (roltypen) => [...new Set(roltypen.map(({ naam }) => naam))],
  }));

  private readonly requestedZoekopdracht = linkedSignal<
    string,
    KlantZakenZoekopdracht
  >({
    source: () =>
      this.getBetrokkeneIdentificatie(
        new BetrokkeneIdentificatie(this.klant()),
      ),
    computation: (betrokkeneIdentificatie, previous) => {
      if (!previous) {
        return {
          zoekParameters: {
            ...getDefaultZoekParameters(),
            type: "ZAAK",
            rows: 10,
            alleenOpenstaandeZaken: true,
          },
          betrokkenheid: null,
          betrokkeneIdentificatie,
        };
      }
      return {
        ...previous.value,
        zoekParameters: { ...previous.value.zoekParameters, page: 0 },
        betrokkeneIdentificatie,
      };
    },
  });

  protected readonly search = injectLastSuccessfulSearch({
    requested: this.requestedZoekopdracht,
    query: (zoekopdracht) =>
      this.zoekenService.list(this.toZoekParameters(zoekopdracht)),
  });

  protected readonly zoekParameters = computed(
    () => this.search.requested().zoekParameters,
  );
  protected readonly betrokkenheid = computed(
    () => this.search.requested().betrokkenheid,
  );
  protected readonly actieveFilters = computed(() =>
    hasActiveSearchFilters(this.toZoekParameters(this.search.requested())),
  );
  protected readonly zoekResultaat = computed(
    (): ZoekResultaat<ZaakZoekObject> =>
      (this.search.data() as ZoekResultaat<ZaakZoekObject> | undefined) ?? {
        resultaten: [],
        totaal: 0,
        filters: {},
        foutmelding: "",
      },
  );

  private toZoekParameters({
    zoekParameters,
    betrokkenheid,
    betrokkeneIdentificatie,
  }: KlantZakenZoekopdracht): ZoekParameters {
    return {
      ...zoekParameters,
      zoeken: {
        ...zoekParameters.zoeken,
        [this.setBetrokkeneFieldToSolrKeyName(betrokkenheid)]:
          betrokkeneIdentificatie,
      },
    };
  }

  private getBetrokkeneIdentificatie(betrokkene: BetrokkeneIdentificatie) {
    if (betrokkene.bsn) return `P-${betrokkene.bsn}`;
    if (betrokkene.vestigingsnummer && betrokkene.kvkNummer)
      return `V-${betrokkene.kvkNummer}-${betrokkene.vestigingsnummer}`;
    if (betrokkene.kvkNummer) return `K-${betrokkene.kvkNummer}`;
    return "";
  }

  protected getBetrokkenheid(zaak: ZaakZoekObject) {
    const betrokkene = new BetrokkeneIdentificatie(this.klant());

    return Object.entries(zaak.betrokkenen || {}).reduce<string[]>(
      (acc, [rol, identifiers]) => {
        const identifierList = identifiers as string[];
        if (betrokkene.bsn && identifierList.includes(`P-${betrokkene.bsn}`)) {
          acc.push(this.makeSolrKeyNameReadableBetrokkeneType(rol));
        }

        if (
          betrokkene.vestigingsnummer &&
          betrokkene.kvkNummer &&
          identifierList.includes(
            `V-${betrokkene.kvkNummer}-${betrokkene.vestigingsnummer}`,
          )
        ) {
          acc.push(this.makeSolrKeyNameReadableBetrokkeneType(rol));
        } else if (
          betrokkene.kvkNummer &&
          identifierList.includes(`K-${betrokkene.kvkNummer}`)
        ) {
          acc.push(this.makeSolrKeyNameReadableBetrokkeneType(rol));
        }
        return acc;
      },
      [],
    );
  }

  private setBetrokkeneFieldToSolrKeyName(betrokkene: string | null): string {
    return betrokkene
      ? "zaak_betrokkene_" + betrokkene.replace(/ /g, "_")
      : ZoekVeld.ZAAK_BETROKKENEN;
  }

  private makeSolrKeyNameReadableBetrokkeneType(fieldName: string): string {
    return fieldName.replace(/_/g, " ");
  }

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

  protected betrokkenheidChanged(betrokkenheid?: string | null) {
    this.searchFirstPage({}, betrokkenheid ?? null);
  }

  protected sortChanged({ active, direction }: Sort) {
    this.searchFirstPage({
      sorteerVeld: active as GeneratedType<"SorteerVeld">,
      sorteerRichting: direction,
    });
  }

  protected pageChanged({ pageIndex, pageSize }: PageEvent) {
    const zoekopdracht = this.search.requested();
    this.search.request({
      ...zoekopdracht,
      zoekParameters: {
        ...zoekopdracht.zoekParameters,
        page: pageIndex,
        rows: pageSize,
      },
    });
  }

  protected clearFilters() {
    this.searchFirstPage(
      {
        zoeken: {},
        filters: {},
        datums: {},
        sorteerVeld: undefined,
        sorteerRichting: "",
      },
      null,
    );
  }

  private searchFirstPage(
    changes: Partial<ZoekParameters>,
    betrokkenheid = this.betrokkenheid(),
  ) {
    const zoekopdracht = this.search.requested();
    this.search.request({
      ...zoekopdracht,
      zoekParameters: { ...zoekopdracht.zoekParameters, ...changes, page: 0 },
      betrokkenheid,
    });
  }
}
