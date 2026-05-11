/*
 * SPDX-FileCopyrightText: 2021 - 2022 Atos, 2024 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { NgFor, NgIf, NgSwitch, NgSwitchCase, NgSwitchDefault, SlicePipe } from "@angular/common";
import { SelectionModel } from "@angular/cdk/collections";
import { CdkDrag, CdkDropList } from "@angular/cdk/drag-drop";
import { ComponentType } from "@angular/cdk/portal";
import {
  AfterViewInit,
  Component,
  OnDestroy,
  OnInit,
  signal,
  ViewChild,
} from "@angular/core";
import { MatBadge } from "@angular/material/badge";
import { MatButton, MatIconAnchor, MatIconButton } from "@angular/material/button";
import { MatCheckbox } from "@angular/material/checkbox";
import { MatDialog } from "@angular/material/dialog";
import { MatIcon } from "@angular/material/icon";
import { MatPaginator, MatPaginatorModule, PageEvent } from "@angular/material/paginator";
import { MatSort, MatSortModule } from "@angular/material/sort";
import { MatTable, MatTableModule } from "@angular/material/table";
import { RouterLink } from "@angular/router";
import { TranslatePipe } from "@ngx-translate/core";
import { TranslateService } from "@ngx-translate/core";
import { injectQuery } from "@tanstack/angular-query-experimental";
import { ObjectType } from "src/app/core/websocket/model/object-type";
import { Opcode } from "src/app/core/websocket/model/opcode";
import { BatchProcessService } from "src/app/shared/batch-progress/batch-process.service";
import { ActivatedRoute } from "@angular/router";
import { UtilService } from "../../core/service/util.service";
import { GebruikersvoorkeurenService } from "../../gebruikersvoorkeuren/gebruikersvoorkeuren.service";
import { ZoekopdrachtComponent } from "../../gebruikersvoorkeuren/zoekopdracht/zoekopdracht.component";
import { IdentityService } from "../../identity/identity.service";
import { ColumnPickerComponent } from "../../shared/dynamic-table/column-picker/column-picker.component";
import { ColumnPickerValue } from "../../shared/dynamic-table/column-picker/column-picker-value";
import { WerklijstComponent } from "../../shared/dynamic-table/datasource/werklijst-component";
import { ZoekenColumn } from "../../shared/dynamic-table/model/zoeken-column";
import { TextIcon } from "../../shared/edit/text-icon";
import { ExportButtonComponent } from "../../shared/export-button/export-button.component";
import { DagenPipe } from "../../shared/pipes/dagen.pipe";
import { DatumPipe } from "../../shared/pipes/datum.pipe";
import { EmptyPipe } from "../../shared/pipes/empty.pipe";
import { StaticTextComponent } from "../../shared/static-text/static-text.component";
import { DateRangeFilterComponent } from "../../shared/table-zoek-filters/date-range-filter/date-range-filter.component";
import { FacetFilterComponent } from "../../shared/table-zoek-filters/facet-filter/facet-filter.component";
import { TekstFilterComponent } from "../../shared/table-zoek-filters/tekst-filter/tekst-filter.component";
import { DateConditionals } from "../../shared/utils/date-conditionals";
import { GeneratedType } from "../../shared/utils/generated-types";
import { TaakZoekObject } from "../../zoeken/model/taken/taak-zoek-object";
import { ZoekenService } from "../../zoeken/zoeken.service";
import { TakenVerdelenDialogComponent } from "../taken-verdelen-dialog/taken-verdelen-dialog.component";
import { TakenVrijgevenDialogComponent } from "../taken-vrijgeven-dialog/taken-vrijgeven-dialog.component";
import { TakenService } from "../taken.service";
import { TakenWerkvoorraadDatasource } from "./taken-werkvoorraad-datasource";
import { detailExpand } from "../../shared/animations/animations";

@Component({
  templateUrl: "./taken-werkvoorraad.component.html",
  styleUrls: ["./taken-werkvoorraad.component.less"],
  animations: [detailExpand],
  standalone: true,
  imports: [
    CdkDrag,
    CdkDropList,
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
    ZoekopdrachtComponent,
    ColumnPickerComponent,
  ],
})
export class TakenWerkvoorraadComponent
  extends WerklijstComponent
  implements AfterViewInit, OnInit, OnDestroy
{
  protected selection = new SelectionModel<TaakZoekObject>(true, []);
  protected dataSource: TakenWerkvoorraadDatasource;
  @ViewChild(MatPaginator) private paginator!: MatPaginator;
  @ViewChild(MatSort) private sort!: MatSort;
  @ViewChild(MatTable) private table!: MatTable<TaakZoekObject>;
  protected expandedRow: TaakZoekObject | null = null;
  protected readonly zoekenColumn = ZoekenColumn;

  protected fataledatumIcon = new TextIcon(
    DateConditionals.provideFormControlValue(DateConditionals.isExceeded),
    "report_problem",
    "warningVerlopen_icon",
    "msg.datum.overschreden",
    "error",
  );

  protected takenLoading = signal(false);
  private toekenning?: {
    groep?: GeneratedType<"RestGroup">;
    medewerker?: GeneratedType<"RestUser">;
  };

  private readonly loggedInUserQuery = injectQuery(() =>
    this.identityService.readLoggedInUser(),
  );

  constructor(
    public override route: ActivatedRoute,
    private takenService: TakenService,
    protected utilService: UtilService,
    private identityService: IdentityService,
    private dialog: MatDialog,
    private zoekenService: ZoekenService,
    public override gebruikersvoorkeurenService: GebruikersvoorkeurenService,
    private translateService: TranslateService,
    private batchProcessService: BatchProcessService,
  ) {
    super();
    this.dataSource = new TakenWerkvoorraadDatasource(
      this.zoekenService,
      this.utilService,
    );
  }

  override ngOnInit() {
    super.ngOnInit();
    this.utilService.setTitle("title.taken.werkvoorraad");
    this.dataSource.initColumns(this.defaultColumns());
  }

  ngAfterViewInit() {
    this.dataSource.setViewChilds(this.paginator, this.sort);
    this.table.dataSource = this.dataSource;
  }

  protected showAssignToMe(taakZoekObject: TaakZoekObject) {
    if (!taakZoekObject.rechten.toekennen) return false;
    const loggedInUser = this.loggedInUserQuery.data();
    if (!loggedInUser) return false;
    if (loggedInUser.id === taakZoekObject.behandelaarGebruikersnaam)
      return false;
    return loggedInUser.groupIds?.includes(taakZoekObject.groepID) ?? false;
  }

  protected assignToMe(taakZoekObject: TaakZoekObject, event: MouseEvent) {
    event.stopPropagation();
    this.takenService
      .toekennenAanIngelogdeMedewerkerVanuitLijst({
        taakId: taakZoekObject.id,
        zaakUuid: taakZoekObject.zaakUuid,
        groepId: null as unknown as string,
      })
      .subscribe(({ behandelaar }) => {
        if (!behandelaar) return;

        taakZoekObject.behandelaarNaam = behandelaar.naam;
        taakZoekObject.behandelaarGebruikersnaam = behandelaar.id;
        this.utilService.openSnackbar("msg.taak.toegekend", {
          behandelaar: behandelaar.naam,
        });
      });
  }

  protected isAllSelected(): boolean {
    const numSelected = this.selection.selected.length;
    const numRows = this.dataSource.data.length;
    return numSelected === numRows;
  }

  protected masterToggle() {
    if (this.isAllSelected()) {
      this.selection.clear();
      return;
    }
    this.selection.select(...this.dataSource.data);
  }

  protected checkboxLabel(row?: TaakZoekObject): string {
    if (!row) {
      return `actie.alles.${
        this.isAllSelected() ? "deselecteren" : "selecteren"
      }`;
    }

    return `actie.${
      this.selection.isSelected(row) ? "deselecteren" : "selecteren"
    }`;
  }

  protected isSelected(): boolean {
    return this.selection.selected.length > 0;
  }

  protected countSelected(checkIfTaskHasHandler = false): number {
    return this.selection.selected.filter(
      ({ behandelaarGebruikersnaam }) =>
        !checkIfTaskHasHandler || !!behandelaarGebruikersnaam,
    ).length;
  }

  protected openVerdelenScherm() {
    this.handleAssignOrReleaseWorkflow(TakenVerdelenDialogComponent);
  }

  protected openVrijgevenScherm() {
    this.handleAssignOrReleaseWorkflow(TakenVrijgevenDialogComponent, true);
  }

  protected isAfterDate(datum: Date) {
    return DateConditionals.isExceeded(datum);
  }

  protected defaultColumns(): Map<ZoekenColumn, ColumnPickerValue> {
    const columns = new Map([
      [ZoekenColumn.SELECT, ColumnPickerValue.STICKY],
      [ZoekenColumn.NAAM, ColumnPickerValue.VISIBLE],
      [ZoekenColumn.ZAAK_IDENTIFICATIE, ColumnPickerValue.VISIBLE],
      [ZoekenColumn.ZAAK_OMSCHRIJVING, ColumnPickerValue.VISIBLE],
      [ZoekenColumn.ZAAK_TOELICHTING, ColumnPickerValue.HIDDEN],
      [ZoekenColumn.ZAAKTYPE_OMSCHRIJVING, ColumnPickerValue.VISIBLE],
      [ZoekenColumn.CREATIEDATUM, ColumnPickerValue.VISIBLE],
      [ZoekenColumn.FATALEDATUM, ColumnPickerValue.VISIBLE],
      [ZoekenColumn.DAGEN_TOT_FATALEDATUM, ColumnPickerValue.HIDDEN],
      [ZoekenColumn.GROEP, ColumnPickerValue.VISIBLE],
      [ZoekenColumn.BEHANDELAAR, ColumnPickerValue.VISIBLE],
      [ZoekenColumn.TOELICHTING, ColumnPickerValue.HIDDEN],
      [ZoekenColumn.URL, ColumnPickerValue.STICKY],
    ]);
    if (!this.werklijstRechten.zakenTakenVerdelen) {
      columns.delete(ZoekenColumn.SELECT);
    }
    return columns;
  }

  override getWerklijst(): GeneratedType<"Werklijst"> {
    return "WERKVOORRAAD_TAKEN";
  }

  protected override paginatorChanged($event: PageEvent) {
    super.paginatorChanged($event);
    this.selection.clear();
  }

  protected resetColumns() {
    this.dataSource.resetColumns();
  }

  protected filtersChange() {
    this.selection.clear();
    this.dataSource.filtersChanged();
  }

  private handleAssignOrReleaseWorkflow<T>(
    dialogComponent: ComponentType<T>,
    release = false,
  ) {
    const screenEventResourceId = crypto.randomUUID();
    const tasks = this.selection.selected.filter(
      ({ behandelaarGebruikersnaam }) =>
        !release || !!behandelaarGebruikersnaam,
    );

    this.batchProcessService.subscribe({
      ids: tasks.map(({ id }) => id),
      progressSubscription: {
        opcode: Opcode.ANY,
        objectType: ObjectType.TAAK,
        onNotification: (id, event) => {
          if (event.opcode !== Opcode.UPDATED) return;

          const taak = this.dataSource.data.find((task) => task.id === id);
          if (!taak) return;

          taak.groepNaam = this.toekenning?.groep?.naam ?? taak.groepNaam;
          taak.groepID = this.toekenning?.groep?.id ?? taak.groepID;

          taak.behandelaarGebruikersnaam = this.toekenning?.medewerker?.id;
          taak.behandelaarNaam = this.toekenning?.medewerker?.naam;
        },
      },
      finalSubscription: {
        objectType: ObjectType.ANY,
        opcode: Opcode.UPDATED,
        screenEventResourceId,
      },
      finally: () => {
        this.selection.clear();
        this.dataSource.load(5_000); // We need to give the indexing service some time to finish
        this.takenLoading.set(false);
        this.batchProcessService.stop();
      },
    });

    this.dialog
      .open(dialogComponent, {
        data: {
          taken: tasks,
          screenEventResourceId,
        },
      })
      .beforeClosed()
      .subscribe((result) => {
        this.toekenning = typeof result === "object" ? result : undefined;
        if (!result) {
          this.batchProcessService.stop();
          return;
        }

        const message =
          tasks.length === 1
            ? this.translateService.instant(
                release ? "msg.vrijgegeven.taak" : "msg.verdeeld.taak",
              )
            : this.translateService.instant(
                release ? "msg.vrijgegeven.taken" : "msg.verdeeld.taken",
                {
                  aantal: tasks.length,
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
