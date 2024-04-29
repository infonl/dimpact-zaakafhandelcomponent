/*
 * SPDX-FileCopyrightText: 2021 - 2022 Atos
 * SPDX-License-Identifier: EUPL-1.2+
 */

import {
  AfterViewInit,
  Component,
  Inject,
  OnInit,
  Signal,
  ViewChild,
  computed,
  effect,
  inject,
  signal,
} from "@angular/core";

import { detailExpand } from "../../shared/animations/animations";

import { SelectionModel } from "@angular/cdk/collections";
import { MatDialog } from "@angular/material/dialog";
import { MatPaginator, PageEvent } from "@angular/material/paginator";
import { MatSort } from "@angular/material/sort";
import { MatTable } from "@angular/material/table";
import { UtilService } from "../../core/service/util.service";
import { IdentityService } from "../../identity/identity.service";
import { LoggedInUser } from "../../identity/model/logged-in-user";
import { ColumnPickerValue } from "../../shared/dynamic-table/column-picker/column-picker-value";
import { TextIcon } from "../../shared/edit/text-icon";
import { ZaakZoekObject } from "../../zoeken/model/zaken/zaak-zoek-object";
import { ZoekenService } from "../../zoeken/zoeken.service";
import { ZakenService } from "../zaken.service";

import { ComponentType } from "@angular/cdk/portal";
import {
  MatProgressBar,
  ProgressBarMode,
} from "@angular/material/progress-bar";
import {
  MAT_SNACK_BAR_DATA,
  MatSnackBarLabel,
  MatSnackBarRef,
} from "@angular/material/snack-bar";
import { ActivatedRoute } from "@angular/router";
import { TranslateService } from "@ngx-translate/core";
import { ObjectType } from "src/app/core/websocket/model/object-type";
import { Opcode } from "src/app/core/websocket/model/opcode";
import { WebsocketService } from "src/app/core/websocket/websocket.service";
import { Group } from "src/app/identity/model/group";
import { User } from "src/app/identity/model/user";
import { IndexingService } from "src/app/indexing/indexing.service";
import { DateConditionals } from "src/app/shared/utils/date-conditionals";
import { SorteerVeld } from "src/app/zoeken/model/sorteer-veld";
import { GebruikersvoorkeurenService } from "../../gebruikersvoorkeuren/gebruikersvoorkeuren.service";
import { Werklijst } from "../../gebruikersvoorkeuren/model/werklijst";
import { WerklijstComponent } from "../../shared/dynamic-table/datasource/werklijst-component";
import { ZoekenColumn } from "../../shared/dynamic-table/model/zoeken-column";
import { IndicatiesLayout } from "../../shared/indicaties/indicaties.component";
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
  implements AfterViewInit, OnInit
{
  readonly indicatiesLayout = IndicatiesLayout;
  selection = new SelectionModel<ZaakZoekObject>(true, []);
  dataSource: ZakenWerkvoorraadDatasource;
  @ViewChild(MatPaginator) paginator: MatPaginator;
  @ViewChild(MatSort) sort: MatSort;
  @ViewChild(MatTable) table: MatTable<ZaakZoekObject>;
  ingelogdeMedewerker: LoggedInUser;
  expandedRow: ZaakZoekObject | null;
  readonly zoekenColumn = ZoekenColumn;
  sorteerVeld = SorteerVeld;

  einddatumGeplandIcon: TextIcon = new TextIcon(
    DateConditionals.provideFormControlValue(DateConditionals.isExceeded),
    "report_problem",
    "warningVerlopen_icon",
    "msg.datum.overschreden",
    "warning",
  );
  uiterlijkeEinddatumAfdoeningIcon: TextIcon = new TextIcon(
    DateConditionals.provideFormControlValue(DateConditionals.isExceeded),
    "report_problem",
    "errorVerlopen_icon",
    "msg.datum.overschreden",
    "error",
  );

  zakenLoading = signal(false);
  zakenState = signal<Record<string, boolean>>({});
  zakenValues = computed(() => Object.values(this.zakenState()));
  zakenProgressPercentage = computed(() => {
    const values = this.zakenValues();
    return values.length
      ? Math.round((values.filter((done) => done).length / values.length) * 100)
      : undefined;
  });
  toekenning: { groep?: Group; medewerker?: User } | undefined;

  constructor(
    private zakenService: ZakenService,
    public gebruikersvoorkeurenService: GebruikersvoorkeurenService,
    public route: ActivatedRoute,
    private zoekenService: ZoekenService,
    public utilService: UtilService,
    public dialog: MatDialog,
    private identityService: IdentityService,
    private websocketService: WebsocketService,
    private translateService: TranslateService,
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

  getWerklijst(): Werklijst {
    return Werklijst.WERKVOORRAAD_ZAKEN;
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

  countSelected() {
    return this.selection.selected.length;
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

  isAfterDate(datum): boolean {
    return DateConditionals.isExceeded(datum);
  }

  resetColumns(): void {
    this.dataSource.resetColumns();
  }

  filtersChange(): void {
    this.selection.clear();
    this.dataSource.filtersChanged();
  }

  assignToMe(zaakZoekObject: ZaakZoekObject, $event) {
    $event.stopPropagation();

    this.zakenService
      .toekennenAanIngelogdeMedewerkerVanuitLijst(zaakZoekObject)
      .subscribe((zaak) => {
        zaakZoekObject.behandelaarNaam = zaak.behandelaar.naam;
        zaakZoekObject.behandelaarGebruikersnaam = zaak.behandelaar.id;
        this.utilService.openSnackbar("msg.zaak.toegekend", {
          behandelaar: zaak.behandelaar.naam,
        });
      });
  }

  showAssignToMe(zaakZoekObject: ZaakZoekObject): boolean {
    return (
      zaakZoekObject.rechten.toekennen &&
      this.ingelogdeMedewerker &&
      this.ingelogdeMedewerker.id !==
        zaakZoekObject.behandelaarGebruikersnaam &&
      this.ingelogdeMedewerker.groupIds.indexOf(zaakZoekObject.groepId) >= 0
    );
  }

  openVerdelenScherm(): void {
    this.handleWorkflow(
      ZakenVerdelenDialogComponent,
      "msg.verdeeld.zaak",
      "msg.verdeeld.zaken",
    );
  }

  openVrijgevenScherm(): void {
    this.handleWorkflow(
      ZakenVrijgevenDialogComponent,
      "msg.vrijgegeven.zaak",
      "msg.vrijgegeven.zaken",
    );
  }

  private handleWorkflow<T>(
    dialogComponent: ComponentType<T>,
    singleToken: string,
    multipleToken: string,
  ) {
    const zaken = this.selection.selected;
    const subscriptions = zaken.map(({ id }) => {
      this.zakenState.update((v) => ({
        ...v,
        [id]: false,
      }));
      const subscription = this.websocketService.addListener(
        Opcode.UPDATED,
        ObjectType.ZAAK,
        id,
        () => {
          this.zakenState.update((v) => ({
            ...v,
            [id]: true,
          }));
          const zaak = this.dataSource.data.find((x) => x.id === id);
          if (this.toekenning && zaak) {
            zaak.groepNaam = this.toekenning.groep?.naam || zaak.groepNaam;
            zaak.groepId = this.toekenning.groep?.id || zaak.groepId;
            zaak.behandelaarGebruikersnaam = this.toekenning.medewerker?.id;
            zaak.behandelaarNaam = this.toekenning.medewerker?.naam;
          }
          this.websocketService.removeListener(subscription);
        },
      );
      return subscription;
    });
    const dialogRef = this.dialog.open(dialogComponent, {
      data: zaken,
    });
    dialogRef.afterClosed().subscribe((result) => {
      if (result) {
        this.toekenning = result;
        this.zakenLoading.set(true);
        const message =
          zaken.length === 1
            ? this.translateService.instant(singleToken)
            : this.translateService.instant(multipleToken, {
                aantal: zaken.length,
              });
        this.utilService
          .openSnackbarFromComponent(ZakenWorkflowSnackbar, {
            data: { progressPercentage: this.zakenProgressPercentage, message },
          })
          .afterDismissed()
          .subscribe(() => {
            this.selection.clear();
            this.dataSource.load();
            this.zakenLoading.set(false);
            this.zakenState.set({});
          });
      } else {
        this.websocketService.removeListeners(subscriptions);
      }
    });
  }
}

@Component({
  standalone: true,
  imports: [MatSnackBarLabel, MatProgressBar],
  template: `
    <div matSnackBarLabel>{{ data.message }}</div>
    <mat-progress-bar
      [mode]="progressMode()"
      [value]="data.progressPercentage()"
    ></mat-progress-bar>
  `,
  styles: `
    .mat-mdc-progress-bar {
      --mdc-linear-progress-active-indicator-color: var(
        --mat-snack-bar-button-color
      );
      --mdc-linear-progress-track-color: rgba(255, 64, 129, 0.25);
      position: absolute;
      bottom: 0;
    }
  `,
})
class ZakenWorkflowSnackbar {
  snackBarRef = inject(MatSnackBarRef);
  progressMode = computed<ProgressBarMode>(() => {
    const percentage = this.data.progressPercentage();
    return percentage === 100 || percentage === 0 ? "query" : "determinate";
  });

  constructor(
    @Inject(MAT_SNACK_BAR_DATA)
    public data: { progressPercentage: Signal<number>; message: string },
    index: IndexingService,
  ) {
    effect(() => {
      if (data.progressPercentage() === 100) {
        index.commitPendingChangesToSearchIndex().subscribe(() => {
          this.snackBarRef.dismiss();
        });
      }
    });
  }
}
