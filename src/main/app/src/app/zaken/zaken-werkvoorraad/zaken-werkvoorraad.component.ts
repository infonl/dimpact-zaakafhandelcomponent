/*
 * SPDX-FileCopyrightText: 2021 - 2022 Atos, 2024 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import {
  AfterViewInit,
  Component,
  OnDestroy,
  OnInit,
  signal,
  ViewChild,
} from "@angular/core";

import { detailExpand } from "../../shared/animations/animations";

import { SelectionModel } from "@angular/cdk/collections";
import { CdkDrag, CdkDropList } from "@angular/cdk/drag-drop";
import { ComponentType } from "@angular/cdk/portal";
import {
  NgFor,
  NgIf,
  NgSwitch,
  NgSwitchCase,
  NgSwitchDefault,
  SlicePipe,
} from "@angular/common";
import { MatBadge } from "@angular/material/badge";
import {
  MatButton,
  MatIconAnchor,
  MatIconButton,
} from "@angular/material/button";
import { MatCheckbox } from "@angular/material/checkbox";
import { MatDialog } from "@angular/material/dialog";
import { MatIcon } from "@angular/material/icon";
import {
  MatPaginator,
  MatPaginatorModule,
  PageEvent,
} from "@angular/material/paginator";
import { MatSort, MatSortModule } from "@angular/material/sort";
import { MatTable, MatTableModule } from "@angular/material/table";
import { RouterLink } from "@angular/router";
import { TranslatePipe } from "@ngx-translate/core";
import { UtilService } from "../../core/service/util.service";
import { IdentityService } from "../../identity/identity.service";
import { ColumnPickerValue } from "../../shared/dynamic-table/column-picker/column-picker-value";
import { ColumnPickerComponent } from "../../shared/dynamic-table/column-picker/column-picker.component";
import { WerklijstComponent } from "../../shared/dynamic-table/datasource/werklijst-component";
import { ZoekenColumn } from "../../shared/dynamic-table/model/zoeken-column";
import { TextIcon } from "../../shared/edit/text-icon";
import { ExportButtonComponent } from "../../shared/export-button/export-button.component";
import { IndicatiesLayout } from "../../shared/indicaties/indicaties.component";
import { ZaakIndicatiesComponent } from "../../shared/indicaties/zaak-indicaties/zaak-indicaties.component";
import { DagenPipe } from "../../shared/pipes/dagen.pipe";
import { DatumPipe } from "../../shared/pipes/datum.pipe";
import { EmptyPipe } from "../../shared/pipes/empty.pipe";
import { VertrouwelijkaanduidingToTranslationKeyPipe } from "../../shared/pipes/vertrouwelijkaanduiding-to-translation-key.pipe";
import { StaticTextComponent } from "../../shared/static-text/static-text.component";
import { DateRangeFilterComponent } from "../../shared/table-zoek-filters/date-range-filter/date-range-filter.component";
import { FacetFilterComponent } from "../../shared/table-zoek-filters/facet-filter/facet-filter.component";
import { TekstFilterComponent } from "../../shared/table-zoek-filters/tekst-filter/tekst-filter.component";
import { DateConditionals } from "../../shared/utils/date-conditionals";
import { GeneratedType } from "../../shared/utils/generated-types";
import { ZaakZoekObject } from "../../zoeken/model/zaken/zaak-zoek-object";
import { ZoekenService } from "../../zoeken/zoeken.service";
import { ZakenService } from "../zaken.service";

import { ActivatedRoute } from "@angular/router";
import { TranslateService } from "@ngx-translate/core";
import { injectQuery } from "@tanstack/angular-query-experimental";
import { firstValueFrom } from "rxjs";
import { ObjectType } from "src/app/core/websocket/model/object-type";
import { Opcode } from "src/app/core/websocket/model/opcode";
import { IndexingService } from "src/app/indexing/indexing.service";
import { BatchProcessService } from "src/app/shared/batch-progress/batch-process.service";
import { GebruikersvoorkeurenService } from "../../gebruikersvoorkeuren/gebruikersvoorkeuren.service";
import { ZoekopdrachtComponent } from "../../gebruikersvoorkeuren/zoekopdracht/zoekopdracht.component";
import { ZakenVerdelenDialogComponent } from "../zaken-verdelen-dialog/zaken-verdelen-dialog.component";
import { ZakenVrijgevenDialogComponent } from "../zaken-vrijgeven-dialog/zaken-vrijgeven-dialog.component";
import { ZakenWerkvoorraadDatasource } from "./zaken-werkvoorraad-datasource";

@Component({
  templateUrl: "./zaken-werkvoorraad.component.html",
  styleUrls: ["./zaken-werkvoorraad.component.less"],
  animations: [detailExpand],
  standalone: true,
  imports: [
    CdkDrag,
    CdkDropList,
    ColumnPickerComponent,
    DagenPipe,
    DateRangeFilterComponent,
    DatumPipe,
    EmptyPipe,
    ExportButtonComponent,
    FacetFilterComponent,
    MatBadge,
    MatButton,
    MatCheckbox,
    MatIcon,
    MatIconAnchor,
    MatIconButton,
    MatPaginatorModule,
    MatSortModule,
    MatTableModule,
    NgFor,
    NgIf,
    NgSwitch,
    NgSwitchCase,
    NgSwitchDefault,
    RouterLink,
    SlicePipe,
    StaticTextComponent,
    TekstFilterComponent,
    TranslatePipe,
    VertrouwelijkaanduidingToTranslationKeyPipe,
    ZaakIndicatiesComponent,
    ZoekopdrachtComponent,
  ],
})
export class ZakenWerkvoorraadComponent
  extends WerklijstComponent
  implements AfterViewInit, OnInit, OnDestroy
{
  protected readonly indicatiesLayout = IndicatiesLayout;
  protected selection = new SelectionModel<ZaakZoekObject>(true, []);
  protected dataSource: ZakenWerkvoorraadDatasource;
  @ViewChild(MatPaginator) private paginator!: MatPaginator;
  @ViewChild(MatSort) private sort!: MatSort;
  @ViewChild(MatTable) private table!: MatTable<ZaakZoekObject>;
  protected expandedRow: ZaakZoekObject | null = null;
  protected readonly zoekenColumn = ZoekenColumn;

  protected einddatumGeplandIcon = new TextIcon(
    DateConditionals.provideFormControlValue(DateConditionals.isExceeded),
    "report_problem",
    "warningVerlopen_icon",
    "msg.datum.overschreden",
    "warning",
  );
  protected uiterlijkeEinddatumAfdoeningIcon = new TextIcon(
    DateConditionals.provideFormControlValue(DateConditionals.isExceeded),
    "report_problem",
    "errorVerlopen_icon",
    "msg.datum.overschreden",
    "error",
  );

  protected zakenLoading = signal(false);
  private toekenning:
    | {
        groep?: GeneratedType<"RestGroup">;
        medewerker?: GeneratedType<"RestUser">;
      }
    | undefined;

  private readonly loggedInUserQuery = injectQuery(() =>
    this.identityService.readLoggedInUser(),
  );

  constructor(
    private zakenService: ZakenService,
    public override gebruikersvoorkeurenService: GebruikersvoorkeurenService,
    public override route: ActivatedRoute,
    private zoekenService: ZoekenService,
    protected utilService: UtilService,
    private dialog: MatDialog,
    private identityService: IdentityService,
    private translateService: TranslateService,
    private indexService: IndexingService,
    private batchProcessService: BatchProcessService,
  ) {
    super();
    this.dataSource = new ZakenWerkvoorraadDatasource(
      this.zoekenService,
      this.utilService,
    );
  }

  override ngOnInit() {
    super.ngOnInit();
    this.utilService.setTitle("title.zaken.werkvoorraad");
    this.dataSource.initColumns(this.defaultColumns());
  }

  protected defaultColumns(): Map<ZoekenColumn, ColumnPickerValue> {
    const columns = new Map([
      [ZoekenColumn.SELECT, ColumnPickerValue.STICKY],
      [ZoekenColumn.ZAAK_DOT_IDENTIFICATIE, ColumnPickerValue.VISIBLE],
      [ZoekenColumn.STATUS, ColumnPickerValue.VISIBLE],
      [ZoekenColumn.ZAAKTYPE, ColumnPickerValue.VISIBLE],
      [ZoekenColumn.OMSCHRIJVING, ColumnPickerValue.VISIBLE],
      [ZoekenColumn.GROEP, ColumnPickerValue.VISIBLE],
      [ZoekenColumn.COMMUNICATIEKANAAL, ColumnPickerValue.HIDDEN],
      [ZoekenColumn.VERTROUWELIJKHEIDAANDUIDING, ColumnPickerValue.HIDDEN],
      [ZoekenColumn.STARTDATUM, ColumnPickerValue.VISIBLE],
      [ZoekenColumn.REGISTRATIEDATUM, ColumnPickerValue.HIDDEN],
      [ZoekenColumn.OPENSTAANDE_TAKEN, ColumnPickerValue.HIDDEN],
      [ZoekenColumn.EINDDATUM_GEPLAND, ColumnPickerValue.HIDDEN],
      [ZoekenColumn.DAGEN_TOT_STREEFDATUM, ColumnPickerValue.HIDDEN],
      [ZoekenColumn.BEHANDELAAR, ColumnPickerValue.VISIBLE],
      [ZoekenColumn.UITERLIJKE_EINDDATUM_AFDOENING, ColumnPickerValue.HIDDEN],
      [ZoekenColumn.DAGEN_TOT_FATALEDATUM, ColumnPickerValue.HIDDEN],
      [ZoekenColumn.TOELICHTING, ColumnPickerValue.HIDDEN],
      [ZoekenColumn.INDICATIES, ColumnPickerValue.VISIBLE],
      [ZoekenColumn.URL, ColumnPickerValue.STICKY],
    ]);
    if (!this.werklijstRechten.zakenTakenVerdelen) {
      columns.delete(ZoekenColumn.SELECT);
    }
    return columns;
  }

  override getWerklijst(): GeneratedType<"Werklijst"> {
    return "WERKVOORRAAD_ZAKEN";
  }

  ngAfterViewInit() {
    this.dataSource.setViewChilds(this.paginator, this.sort);
    this.table.dataSource = this.dataSource;
  }

  protected isAllSelected() {
    const numSelected = this.selection.selected.length;
    const numRows = this.dataSource.data.length;
    return numSelected === numRows;
  }

  protected isSelected() {
    return this.selection.selected.length > 0;
  }

  protected countSelected(checkIfZaakHasHandler = false): number {
    return this.selection.selected.filter(
      ({ behandelaarGebruikersnaam }) =>
        !checkIfZaakHasHandler || !!behandelaarGebruikersnaam,
    ).length;
  }

  protected masterToggle() {
    if (this.isAllSelected()) {
      this.selection.clear();
      return;
    }

    this.selection.select(...this.dataSource.data);
  }

  protected checkboxLabel(row?: ZaakZoekObject): string {
    if (!row) {
      return `actie.alles.${
        this.isAllSelected() ? "deselecteren" : "selecteren"
      }`;
    }

    return `actie.${
      this.selection.isSelected(row) ? "deselecteren" : "selecteren"
    }`;
  }

  protected override paginatorChanged($event: PageEvent) {
    super.paginatorChanged($event);
    this.selection.clear();
  }

  protected isAfterDate(datum: Date | string | null | undefined) {
    return DateConditionals.isExceeded(datum ?? null);
  }

  protected resetColumns() {
    this.dataSource.resetColumns();
  }

  protected filtersChange() {
    this.selection.clear();
    this.dataSource.filtersChanged();
  }

  protected assignToMe(zaakZoekObject: ZaakZoekObject, $event: Event) {
    $event.stopPropagation();

    this.zakenService
      .toekennenAanIngelogdeMedewerkerVanuitLijst(
        zaakZoekObject.id,
        zaakZoekObject.groepId,
      )
      .subscribe((zaak) => {
        if (!zaak.behandelaar) {
          return;
        }
        zaakZoekObject.behandelaarNaam = zaak.behandelaar?.naam;
        zaakZoekObject.behandelaarGebruikersnaam = zaak.behandelaar.id;
        this.utilService.openSnackbar("msg.zaak.toegekend", {
          behandelaar: zaak.behandelaar.naam,
        });
      });
  }

  protected showAssignToMe(zaakZoekObject: ZaakZoekObject) {
    if (!zaakZoekObject.rechten.toekennen) return false;
    const loggedInUser = this.loggedInUserQuery.data();
    if (!loggedInUser) return false;
    if (loggedInUser.id === zaakZoekObject.behandelaarGebruikersnaam)
      return false;
    return loggedInUser.groupIds?.includes(zaakZoekObject.groepId) ?? false;
  }

  protected openVerdelenScherm() {
    this.handleAssigmentOrReleaseWorkflow(ZakenVerdelenDialogComponent);
  }

  protected openVrijgevenScherm() {
    this.handleAssigmentOrReleaseWorkflow(ZakenVrijgevenDialogComponent, true);
  }

  private handleAssigmentOrReleaseWorkflow<T>(
    dialogComponent: ComponentType<T>,
    release = false,
  ) {
    const zaken = this.selection.selected.filter(
      ({ behandelaarGebruikersnaam }) =>
        !release || !!behandelaarGebruikersnaam,
    );

    this.batchProcessService.subscribe({
      ids: zaken.map(({ id }) => id),
      progressSubscription: {
        opcode: Opcode.ANY,
        objectType: ObjectType.ZAAK_ROLLEN,
        onNotification: (id, event) => {
          if (event.opcode !== Opcode.UPDATED) return;

          const zaak = this.dataSource.data.find((x) => x.id === id);
          if (!zaak) return;

          zaak.groepNaam = this.toekenning?.groep?.naam ?? zaak.groepNaam;
          zaak.groepId = this.toekenning?.groep?.id ?? zaak.groepId;

          zaak.behandelaarGebruikersnaam =
            this.toekenning?.medewerker?.id ?? "";
          zaak.behandelaarNaam = this.toekenning?.medewerker?.naam ?? "";
        },
      },
      finally: () =>
        firstValueFrom(
          this.indexService.commitPendingChangesToSearchIndex(),
        ).then(() => {
          this.selection.clear();
          this.dataSource.load(5_000); // We need to give the indexing service some time to finish
          this.zakenLoading.set(false);
          this.batchProcessService.stop();
        }),
    });

    this.dialog
      .open(dialogComponent, {
        data: zaken,
      })
      .beforeClosed()
      .subscribe((result) => {
        this.toekenning = typeof result === "object" ? result : undefined;
        if (!result) {
          this.batchProcessService.stop();
          return;
        }

        if (!release) {
          const notChanged = zaken
            .filter(
              (x) =>
                this.toekenning?.groep?.id === x.groepId &&
                this.toekenning.medewerker?.id === x.behandelaarGebruikersnaam,
            )
            .map(({ id }) => id);
          this.batchProcessService.update(notChanged);
        }
        this.zakenLoading.set(true);
        const message =
          zaken.length === 1
            ? this.translateService.instant(
                release ? "msg.vrijgegeven.zaak" : "msg.verdeeld.zaak",
              )
            : this.translateService.instant(
                release ? "msg.vrijgegeven.zaken" : "msg.verdeeld.zaken",
                {
                  aantal: zaken.length,
                },
              );
        this.batchProcessService.showProgress(message, {
          onTimeout: () => {
            this.utilService.openSnackbar("msg.error.timeout");
          },
        });
      });
  }

  ngOnDestroy() {
    // Make sure when returning to this component, the very first page is loaded
    this.dataSource.zoekopdrachtResetToFirstPage();
  }
}
