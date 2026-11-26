/*
 * SPDX-FileCopyrightText: 2021 - 2022 Atos, 2024-2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import {
  Component,
  computed,
  DestroyRef,
  effect,
  inject,
  OnInit,
  signal,
  untracked,
  viewChild,
} from "@angular/core";
import { MatButtonModule } from "@angular/material/button";
import { MatCardModule } from "@angular/material/card";
import { MatFormFieldModule } from "@angular/material/form-field";
import { MatIconModule } from "@angular/material/icon";
import { MatPaginatorModule, PageEvent } from "@angular/material/paginator";
import { MatSelectChange, MatSelectModule } from "@angular/material/select";
import {
  MatSidenavContainer,
  MatSidenavModule,
} from "@angular/material/sidenav";
import { MatSortModule, Sort } from "@angular/material/sort";
import { MatTableDataSource, MatTableModule } from "@angular/material/table";
import { RouterLink } from "@angular/router";
import { TranslatePipe } from "@ngx-translate/core";
import {
  injectQuery,
  keepPreviousData,
  QueryClient,
} from "@tanstack/angular-query-experimental";
import { WebsocketService } from "../../core/websocket/websocket.service";
import { TypedMatCellDefDirective } from "../../shared/directives/typed-mat-cell-def.directive";
import { StaleTimes } from "../../shared/http/zac-query-client";
import { DatumPipe } from "../../shared/pipes/datum.pipe";
import { EmptyPipe } from "../../shared/pipes/empty.pipe";
import { ReadMoreComponent } from "../../shared/read-more/read-more.component";
import { SideNavComponent } from "../../shared/side-nav/side-nav.component";
import { SessionStorageUtil } from "../../shared/storage/session-storage.util";
import { DateRangeFilterComponent } from "../../shared/table-zoek-filters/date-range-filter/date-range-filter.component";
import { ToggleFilterComponent } from "../../shared/table-zoek-filters/toggle-filter/toggle-filter.component";
import { ToggleSwitchOptions } from "../../shared/table-zoek-filters/toggle-filter/toggle-switch-options";
import { GeneratedType } from "../../shared/utils/generated-types";
import { AdminComponent } from "../admin/admin.component";
import { ZaakafhandelParametersService } from "../zaakafhandel-parameters.service";
import {
  toZaaktypeConfigurationListQuery,
  ZAAKAFHANDELPARAMETERS_PAGE_SIZE,
  ZaakafhandelParametersListParameters,
  ZaakafhandelParametersSortColumn,
} from "./zaakafhandel-parameters-list-parameters";

@Component({
  templateUrl: "./parameters.component.html",
  styleUrls: ["./parameters.component.less"],
  standalone: true,
  imports: [
    RouterLink,
    TranslatePipe,
    MatSidenavModule,
    MatCardModule,
    MatTableModule,
    TypedMatCellDefDirective,
    MatSortModule,
    MatPaginatorModule,
    MatFormFieldModule,
    MatSelectModule,
    MatIconModule,
    MatButtonModule,
    SideNavComponent,
    ToggleFilterComponent,
    DateRangeFilterComponent,
    ReadMoreComponent,
    DatumPipe,
    EmptyPipe,
  ],
})
export class ParametersComponent extends AdminComponent implements OnInit {
  private readonly zaakafhandelParametersService = inject(
    ZaakafhandelParametersService,
  );
  private readonly websocketService = inject(WebsocketService);
  private readonly queryClient = inject(QueryClient);
  private readonly destroyRef = inject(DestroyRef);

  protected readonly sideNavContainer =
    viewChild.required<MatSidenavContainer>("sideNavContainer");

  protected readonly pageSize = ZAAKAFHANDELPARAMETERS_PAGE_SIZE;
  protected readonly filterParameters = signal(
    new ZaakafhandelParametersListParameters("geldig", "desc"),
  );
  protected parameters = new MatTableDataSource<
    GeneratedType<"RestZaaktypeConfigurationOverzicht">
  >();

  private readonly parametersQuery = injectQuery(() => ({
    ...this.zaakafhandelParametersService.listZaakafhandelParameters(
      toZaaktypeConfigurationListQuery(this.filterParameters()),
    ),
    placeholderData: keepPreviousData,
    staleTime: StaleTimes.Instant,
  }));
  protected readonly loading = computed(() => this.parametersQuery.isPending());
  protected readonly hasNoParameters = computed(
    () => !this.parametersQuery.data()?.resultaten?.length,
  );
  protected readonly totaal = computed(
    () => this.parametersQuery.data()?.totaal ?? 0,
  );

  private storedParameterFilters = "zaakafhandelParametersListParameters";

  protected readonly zaaktypes = computed(
    () => this.parametersQuery.data()?.filterZaaktypes ?? [],
  );
  protected readonly processDefinitions = computed(
    () => this.parametersQuery.data()?.filterProcessDefinitions ?? [],
  );

  constructor() {
    super();
    const zaaktypeConfiguratiesListener = this.websocketService.addListener(
      "UPDATED",
      "ZAAKTYPE_CONFIGURATIES",
      "zaaktype-configuraties",
      () =>
        void this.queryClient.invalidateQueries({
          queryKey: ["/rest/zaakafhandelparameters"],
        }),
    );
    this.destroyRef.onDestroy(() =>
      this.websocketService.removeListener(zaaktypeConfiguratiesListener),
    );
    effect(() => {
      this.parameters.data = [
        ...(this.parametersQuery.data()?.resultaten ?? []),
      ];
    });
    effect(() => {
      if (
        !this.parametersQuery.isSuccess() ||
        this.parametersQuery.isPlaceholderData() ||
        this.parametersQuery.isFetching()
      ) {
        return;
      }
      const lastPage = Math.max(
        Math.ceil(this.totaal() / this.pageSize) - 1,
        0,
      );
      if (untracked(this.filterParameters).page > lastPage) {
        untracked(() => this.updateFilterParameters({ page: lastPage }));
      }
    });
  }

  ngOnInit() {
    this.setupMenu("title.parameters");
    this.filterParameters.set(
      SessionStorageUtil.getItem(
        this.storedParameterFilters,
        new ZaakafhandelParametersListParameters("geldig", "desc"),
      ),
    );
  }

  protected applyFilter(options: {
    event: ToggleSwitchOptions | MatSelectChange | string | number | undefined;
    filter: keyof ZaakafhandelParametersListParameters;
  }) {
    const { event, filter } = options;
    this.updateFilterParameters({
      [filter]: typeof event === "object" ? event?.value : event,
      page: 0,
    });
  }

  protected applyDateRangeFilter(
    filter: "beginGeldigheid" | "eindeGeldigheid",
    range: GeneratedType<"RestDateRange">,
  ) {
    this.updateFilterParameters({ [filter]: range, page: 0 });
  }

  protected applySort({ active, direction }: Sort) {
    this.updateFilterParameters({ sort: active, order: direction, page: 0 });
  }

  protected applyPage({ pageIndex }: PageEvent) {
    this.updateFilterParameters({ page: pageIndex });
  }

  private updateFilterParameters(
    changes: Partial<ZaakafhandelParametersListParameters>,
  ) {
    this.filterParameters.update((filterParameters) => ({
      ...filterParameters,
      ...changes,
    }));
    SessionStorageUtil.setItem(
      this.storedParameterFilters,
      this.filterParameters(),
    );
  }

  protected sortColumn(column: ZaakafhandelParametersSortColumn) {
    return column;
  }

  protected compareZaaktype = (
    zaaktype1?: GeneratedType<"RestZaaktypeFilterOptie">,
    zaaktype2?: GeneratedType<"RestZaaktypeFilterOptie">,
  ) => {
    return zaaktype1?.identificatie === zaaktype2?.identificatie;
  };

  protected compareProcessDefinition = (
    processDefinition1?: GeneratedType<"RestProcessDefinitionOverzicht">,
    processDefinition2?: GeneratedType<"RestProcessDefinitionOverzicht">,
  ) => {
    return processDefinition1?.key === processDefinition2?.key;
  };
}
