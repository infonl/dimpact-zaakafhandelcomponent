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
import { ComponentType } from "@angular/cdk/portal";
import { MatDialog } from "@angular/material/dialog";
import { MatPaginator, PageEvent } from "@angular/material/paginator";
import { MatSort } from "@angular/material/sort";
import { MatTable } from "@angular/material/table";
import { ActivatedRoute } from "@angular/router";
import { TranslateService } from "@ngx-translate/core";
import { ObjectType } from "src/app/core/websocket/model/object-type";
import { Opcode } from "src/app/core/websocket/model/opcode";
import { BatchProcessService } from "src/app/shared/batch-progress/batch-process.service";
import { UtilService } from "../../core/service/util.service";
import { GebruikersvoorkeurenService } from "../../gebruikersvoorkeuren/gebruikersvoorkeuren.service";
import { IdentityService } from "../../identity/identity.service";
import { ColumnPickerValue } from "../../shared/dynamic-table/column-picker/column-picker-value";
import { WerklijstComponent } from "../../shared/dynamic-table/datasource/werklijst-component";
import { ZoekenColumn } from "../../shared/dynamic-table/model/zoeken-column";
import { TextIcon } from "../../shared/edit/text-icon";
import { DateConditionals } from "../../shared/utils/date-conditionals";
import { GeneratedType } from "../../shared/utils/generated-types";
import { TaakZoekObject } from "../../zoeken/model/taken/taak-zoek-object";
import { ZoekenService } from "../../zoeken/zoeken.service";
import { TakenVerdelenDialogComponent } from "../taken-verdelen-dialog/taken-verdelen-dialog.component";
import { TakenVrijgevenDialogComponent } from "../taken-vrijgeven-dialog/taken-vrijgeven-dialog.component";
import { TakenService } from "../taken.service";
import { TakenWerkvoorraadDatasource } from "./taken-werkvoorraad-datasource";

@Component({
  templateUrl: "./taken-werkvoorraad.component.html",
  styleUrls: ["./taken-werkvoorraad.component.less"],
  animations: [detailExpand],
})
export class TakenWerkvoorraadComponent
  extends WerklijstComponent
  implements AfterViewInit, OnInit, OnDestroy
{
  selection = new SelectionModel<TaakZoekObject>(true, []);
  dataSource: TakenWerkvoorraadDatasource;
  @ViewChild(MatPaginator) paginator!: MatPaginator;
  @ViewChild(MatSort) sort!: MatSort;
  @ViewChild(MatTable) table!: MatTable<TaakZoekObject>;
  ingelogdeMedewerker?: GeneratedType<"RestLoggedInUser">;
  expandedRow: TaakZoekObject | null = null;
  readonly zoekenColumn = ZoekenColumn;

  fataledatumIcon = new TextIcon(
    DateConditionals.provideFormControlValue(DateConditionals.isExceeded),
    "report_problem",
    "warningVerlopen_icon",
    "msg.datum.overschreden",
    "error",
  );

  takenLoading = signal(false);
  toekenning?: {
    groep?: GeneratedType<"RestGroup">;
    medewerker?: GeneratedType<"RestUser">;
  };

  constructor(
    public route: ActivatedRoute,
    private takenService: TakenService,
    public utilService: UtilService,
    private identityService: IdentityService,
    public dialog: MatDialog,
    private zoekenService: ZoekenService,
    public gebruikersvoorkeurenService: GebruikersvoorkeurenService,
    private translateService: TranslateService,
    private batchProcessService: BatchProcessService,
  ) {
    super();
    this.dataSource = new TakenWerkvoorraadDatasource(
      this.zoekenService,
      this.utilService,
    );
  }
  ngOnInit(): void {
    super.ngOnInit();
    this.utilService.setTitle("title.taken.werkvoorraad");
    this.getIngelogdeMedewerker();
    this.dataSource.initColumns(this.defaultColumns());
  }

  ngAfterViewInit(): void {
    this.dataSource.setViewChilds(this.paginator, this.sort);
    this.table.dataSource = this.dataSource;
  }

  private getIngelogdeMedewerker() {
    this.identityService.readLoggedInUser().subscribe((ingelogdeMedewerker) => {
      this.ingelogdeMedewerker = ingelogdeMedewerker;
    });
  }

  showAssignToMe(taakZoekObject: TaakZoekObject) {
    return (
      taakZoekObject.rechten.toekennen &&
      this.ingelogdeMedewerker?.id !==
        taakZoekObject.behandelaarGebruikersnaam &&
      Boolean(
        this.ingelogdeMedewerker?.groupIds?.includes(taakZoekObject.groepID),
      )
    );
  }

  assignToMe(taakZoekObject: TaakZoekObject, event: MouseEvent) {
    event.stopPropagation();
    this.takenService
      .toekennenAanIngelogdeMedewerkerVanuitLijst(taakZoekObject)
      .subscribe(({ behandelaar }) => {
        if (!behandelaar) return;

        taakZoekObject.behandelaarNaam = behandelaar.naam;
        taakZoekObject.behandelaarGebruikersnaam = behandelaar.id;
        this.utilService.openSnackbar("msg.taak.toegekend", {
          behandelaar: behandelaar.naam,
        });
      });
  }

  /** Whether the number of selected elements matches the total number of rows. */
  isAllSelected(): boolean {
    const numSelected = this.selection.selected.length;
    const numRows = this.dataSource.data.length;
    return numSelected === numRows;
  }

  /** Selects all rows if they are not all selected; otherwise clear selection. */
  masterToggle() {
    if (this.isAllSelected()) {
      this.selection.clear();
      return;
    }
    this.selection.select(...this.dataSource.data);
  }

  /** The label for the checkbox on the passed row */
  checkboxLabel(row?: TaakZoekObject): string {
    if (!row) {
      return `actie.alles.${
        this.isAllSelected() ? "deselecteren" : "selecteren"
      }`;
    }

    return `actie.${
      this.selection.isSelected(row) ? "deselecteren" : "selecteren"
    }`;
  }

  isSelected(): boolean {
    return this.selection.selected.length > 0;
  }

  countSelected(checkIfTaskHasHandler = false): number {
    return this.selection.selected.filter(
      ({ behandelaarGebruikersnaam }) =>
        !checkIfTaskHasHandler || !!behandelaarGebruikersnaam,
    ).length;
  }

  openVerdelenScherm() {
    this.handleAssignOrReleaseWorkflow(TakenVerdelenDialogComponent);
  }

  openVrijgevenScherm() {
    this.handleAssignOrReleaseWorkflow(TakenVrijgevenDialogComponent, true);
  }

  isAfterDate(datum: Date) {
    return DateConditionals.isExceeded(datum);
  }

  defaultColumns(): Map<ZoekenColumn, ColumnPickerValue> {
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

  getWerklijst(): GeneratedType<"Werklijst"> {
    return "WERKVOORRAAD_TAKEN";
  }

  paginatorChanged($event: PageEvent): void {
    super.paginatorChanged($event);
    this.selection.clear();
  }

  resetColumns(): void {
    this.dataSource.resetColumns();
  }

  filtersChange(): void {
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

          if (!taak || !this.toekenning) return;
          taak.groepNaam = this.toekenning.groep?.naam || taak.groepNaam;
          taak.groepID = this.toekenning.groep?.id || taak.groepID;

          taak.behandelaarGebruikersnaam =
            this.toekenning.medewerker?.id ?? undefined;
          taak.behandelaarNaam = this.toekenning.medewerker?.naam ?? undefined;
        },
      },
      finalSubscription: {
        objectType: ObjectType.ANY,
        opcode: Opcode.UPDATED,
        screenEventResourceId,
      },
      finally: () => {
        this.selection.clear();
        this.dataSource.load();
        this.takenLoading.set(false);
        this.batchProcessService.stop();
      },
    });

    const dialogRef = this.dialog.open(dialogComponent, {
      data: {
        taken: tasks,
        screenEventResourceId,
      },
    });

    dialogRef.afterClosed().subscribe((result) => {
      if (!result) {
        this.batchProcessService.stop();
        return;
      }

      this.toekenning = result;
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

  ngOnDestroy(): void {
    // Make sure when returning to this component, the very first page is loaded
    this.dataSource.zoekopdrachtResetToFirstPage();
  }
}
