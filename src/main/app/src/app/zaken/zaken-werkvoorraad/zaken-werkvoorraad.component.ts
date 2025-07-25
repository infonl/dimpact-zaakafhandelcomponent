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
import { MatDialog } from "@angular/material/dialog";
import { MatPaginator, PageEvent } from "@angular/material/paginator";
import { MatSort } from "@angular/material/sort";
import { MatTable } from "@angular/material/table";
import { UtilService } from "../../core/service/util.service";
import { IdentityService } from "../../identity/identity.service";
import { ColumnPickerValue } from "../../shared/dynamic-table/column-picker/column-picker-value";
import { TextIcon } from "../../shared/edit/text-icon";
import { ZaakZoekObject } from "../../zoeken/model/zaken/zaak-zoek-object";
import { ZoekenService } from "../../zoeken/zoeken.service";
import { ZakenService } from "../zaken.service";

import { ComponentType } from "@angular/cdk/portal";
import { ActivatedRoute } from "@angular/router";
import { TranslateService } from "@ngx-translate/core";
import { firstValueFrom } from "rxjs";
import { ObjectType } from "src/app/core/websocket/model/object-type";
import { Opcode } from "src/app/core/websocket/model/opcode";
import { IndexingService } from "src/app/indexing/indexing.service";
import { BatchProcessService } from "src/app/shared/batch-progress/batch-process.service";
import { DateConditionals } from "src/app/shared/utils/date-conditionals";
import { GebruikersvoorkeurenService } from "../../gebruikersvoorkeuren/gebruikersvoorkeuren.service";
import { WerklijstComponent } from "../../shared/dynamic-table/datasource/werklijst-component";
import { ZoekenColumn } from "../../shared/dynamic-table/model/zoeken-column";
import { IndicatiesLayout } from "../../shared/indicaties/indicaties.component";
import { GeneratedType } from "../../shared/utils/generated-types";
import { ZakenVerdelenDialogComponent } from "../zaken-verdelen-dialog/zaken-verdelen-dialog.component";
import { ZakenVrijgevenDialogComponent } from "../zaken-vrijgeven-dialog/zaken-vrijgeven-dialog.component";
import { ZakenWerkvoorraadDatasource } from "./zaken-werkvoorraad-datasource";

@Component({
  templateUrl: "./zaken-werkvoorraad.component.html",
  styleUrls: ["./zaken-werkvoorraad.component.less"],
  animations: [detailExpand],
})
export class ZakenWerkvoorraadComponent
  extends WerklijstComponent
  implements AfterViewInit, OnInit, OnDestroy
{
  readonly indicatiesLayout = IndicatiesLayout;
  selection = new SelectionModel<ZaakZoekObject>(true, []);
  dataSource: ZakenWerkvoorraadDatasource;
  @ViewChild(MatPaginator) paginator!: MatPaginator;
  @ViewChild(MatSort) sort!: MatSort;
  @ViewChild(MatTable) table!: MatTable<ZaakZoekObject>;
  ingelogdeMedewerker?: GeneratedType<"RestLoggedInUser">;
  expandedRow: ZaakZoekObject | null = null;
  readonly zoekenColumn = ZoekenColumn;

  einddatumGeplandIcon = new TextIcon(
    DateConditionals.provideFormControlValue(DateConditionals.isExceeded),
    "report_problem",
    "warningVerlopen_icon",
    "msg.datum.overschreden",
    "warning",
  );
  uiterlijkeEinddatumAfdoeningIcon = new TextIcon(
    DateConditionals.provideFormControlValue(DateConditionals.isExceeded),
    "report_problem",
    "errorVerlopen_icon",
    "msg.datum.overschreden",
    "error",
  );

  zakenLoading = signal(false);
  toekenning:
    | {
        groep?: GeneratedType<"RestGroup">;
        medewerker?: GeneratedType<"RestUser">;
      }
    | undefined;

  constructor(
    private zakenService: ZakenService,
    public gebruikersvoorkeurenService: GebruikersvoorkeurenService,
    public route: ActivatedRoute,
    private zoekenService: ZoekenService,
    public utilService: UtilService,
    public dialog: MatDialog,
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
  ngOnInit(): void {
    super.ngOnInit();
    this.utilService.setTitle("title.zaken.werkvoorraad");
    this.getIngelogdeMedewerker();
    this.dataSource.initColumns(this.defaultColumns());
  }

  defaultColumns(): Map<ZoekenColumn, ColumnPickerValue> {
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

  getWerklijst(): GeneratedType<"Werklijst"> {
    return "WERKVOORRAAD_ZAKEN";
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

  /** Whether the number of selected elements matches the total number of rows. */
  isAllSelected() {
    const numSelected = this.selection.selected.length;
    const numRows = this.dataSource.data.length;
    return numSelected === numRows;
  }

  isSelected() {
    return this.selection.selected.length > 0;
  }

  countSelected(checkIfZaakHasHandler = false): number {
    return this.selection.selected.filter(
      ({ behandelaarGebruikersnaam }) =>
        !checkIfZaakHasHandler || !!behandelaarGebruikersnaam,
    ).length;
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
  checkboxLabel(row?: ZaakZoekObject): string {
    if (!row) {
      return `actie.alles.${
        this.isAllSelected() ? "deselecteren" : "selecteren"
      }`;
    }

    return `actie.${
      this.selection.isSelected(row) ? "deselecteren" : "selecteren"
    }`;
  }

  paginatorChanged($event: PageEvent): void {
    super.paginatorChanged($event);
    this.selection.clear();
  }

  isAfterDate(datum: Date | moment.Moment | string): boolean {
    return DateConditionals.isExceeded(datum);
  }

  resetColumns(): void {
    this.dataSource.resetColumns();
  }

  filtersChange(): void {
    this.selection.clear();
    this.dataSource.filtersChanged();
  }

  assignToMe(zaakZoekObject: ZaakZoekObject, $event: Event) {
    $event.stopPropagation();

    this.zakenService
      .toekennenAanIngelogdeMedewerkerVanuitLijst(zaakZoekObject.id)
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

  showAssignToMe(zaakZoekObject: ZaakZoekObject): boolean {
    return (
      Boolean(zaakZoekObject.rechten.toekennen) &&
      this.ingelogdeMedewerker?.id !==
        zaakZoekObject.behandelaarGebruikersnaam &&
      Boolean(
        this.ingelogdeMedewerker?.groupIds?.indexOf(zaakZoekObject.groepId),
      )
    );
  }

  openVerdelenScherm(): void {
    this.handleAssigmentOrReleaseWorkflow(ZakenVerdelenDialogComponent);
  }

  openVrijgevenScherm(): void {
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

  ngOnDestroy(): void {
    // Make sure when returning to this component, the very first page is loaded
    this.dataSource.zoekopdrachtResetToFirstPage();
  }
}
