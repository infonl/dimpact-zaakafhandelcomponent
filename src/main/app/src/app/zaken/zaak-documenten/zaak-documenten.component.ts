/*
 * SPDX-FileCopyrightText: 2022 Atos, 2024 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { SelectionModel } from "@angular/cdk/collections";
import { NgClass, NgFor, NgIf } from "@angular/common";
import {
  AfterViewInit,
  Component,
  computed,
  effect,
  inject,
  input,
  output,
  signal,
  ViewChild,
} from "@angular/core";
import { Validators } from "@angular/forms";
import { MatIconAnchor, MatIconButton } from "@angular/material/button";
import { MatCardModule } from "@angular/material/card";
import { MatCheckbox, MatCheckboxChange } from "@angular/material/checkbox";
import { MatDialog } from "@angular/material/dialog";
import { MatIconModule } from "@angular/material/icon";
import { MatMenuModule } from "@angular/material/menu";
import { MatSlideToggleModule } from "@angular/material/slide-toggle";
import { MatSort, MatSortModule } from "@angular/material/sort";
import { MatTableDataSource, MatTableModule } from "@angular/material/table";
import { Router, RouterLink } from "@angular/router";
import { TranslatePipe, TranslateService } from "@ngx-translate/core";
import { injectQuery, QueryClient } from "@tanstack/angular-query-experimental";
import { map } from "rxjs/operators";
import { UtilService } from "../../core/service/util.service";
import { ObjectType } from "../../core/websocket/model/object-type";
import { Opcode } from "../../core/websocket/model/opcode";
import { ScreenEvent } from "../../core/websocket/model/screen-event";
import { WebsocketService } from "../../core/websocket/websocket.service";
import { InformatieObjectenService } from "../../informatie-objecten/informatie-objecten.service";
import {
  FileFormat,
  FileFormatUtil,
} from "../../informatie-objecten/model/file-format";
import { FileIcon } from "../../informatie-objecten/model/file-icon";
import { GekoppeldeZaakEnkelvoudigInformatieobject } from "../../informatie-objecten/model/gekoppelde.zaak.enkelvoudig.informatieobject";
import { detailExpand } from "../../shared/animations/animations";
import { DialogData } from "../../shared/dialog/dialog-data";
import { DialogComponent } from "../../shared/dialog/dialog.component";
import { DocumentIconComponent } from "../../shared/document-icon/document-icon.component";
import { DocumentViewerComponent } from "../../shared/document-viewer/document-viewer.component";
import { IndicatiesLayout } from "../../shared/indicaties/indicaties.component";
import { InformatieObjectIndicatiesComponent } from "../../shared/indicaties/informatie-object-indicaties/informatie-object-indicaties.component";
import { TextareaFormFieldBuilder } from "../../shared/material-form-builder/form-components/textarea/textarea-form-field-builder";
import { BestandsomvangPipe } from "../../shared/pipes/bestandsomvang.pipe";
import { DatumPipe } from "../../shared/pipes/datum.pipe";
import { EmptyPipe } from "../../shared/pipes/empty.pipe";
import { VertrouwelijkaanduidingToTranslationKeyPipe } from "../../shared/pipes/vertrouwelijkaanduiding-to-translation-key.pipe";
import { GeneratedType } from "../../shared/utils/generated-types";
import { ZakenService } from "../zaken.service";

const LIST_QUERY_KEY = "/rest/informatieobjecten/informatieobjectenList";

const BASE_COLUMNS = [
  "downloaden",
  "titel",
  "informatieobjectTypeOmschrijving",
  "bestandsomvang",
  "status",
  "vertrouwelijkheidaanduiding",
  "registratiedatumTijd",
  "auteur",
  "indicaties",
  "url",
];

const GEKOPPELDE_COLUMNS = [
  "downloaden",
  "titel",
  "zaakIdentificatie",
  "relatieType",
  "informatieobjectTypeOmschrijving",
  "bestandsomvang",
  "status",
  "vertrouwelijkheidaanduiding",
  "registratiedatumTijd",
  "auteur",
  "indicaties",
  "url",
];

@Component({
  selector: "zac-zaak-documenten",
  templateUrl: "./zaak-documenten.component.html",
  styleUrls: ["./zaak-documenten.component.less"],
  animations: [detailExpand],
  standalone: true,
  imports: [
    NgIf,
    NgFor,
    NgClass,
    RouterLink,
    MatCardModule,
    MatSlideToggleModule,
    MatTableModule,
    MatSortModule,
    MatCheckbox,
    MatIconModule,
    MatIconAnchor,
    MatIconButton,
    MatMenuModule,
    TranslatePipe,
    DocumentIconComponent,
    DocumentViewerComponent,
    InformatieObjectIndicatiesComponent,
    EmptyPipe,
    BestandsomvangPipe,
    DatumPipe,
    VertrouwelijkaanduidingToTranslationKeyPipe,
  ],
})
export class ZaakDocumentenComponent implements AfterViewInit {
  private readonly informatieObjectenService = inject(InformatieObjectenService);
  private readonly websocketService = inject(WebsocketService);
  private readonly utilService = inject(UtilService);
  private readonly zakenService = inject(ZakenService);
  private readonly dialog = inject(MatDialog);
  private readonly translate = inject(TranslateService);
  private readonly router = inject(Router);
  private readonly queryClient = inject(QueryClient);

  readonly indicatiesLayout = IndicatiesLayout;
  readonly zaak = input.required<GeneratedType<"RestZaak">>();
  readonly documentMoveToCase =
    output<GeneratedType<"RestEnkelvoudigInformatieobject">>();

  protected readonly heeftGerelateerdeZaken = computed(
    () => (this.zaak().gerelateerdeZaken?.length ?? 0) > 0,
  );

  selectAll = false;
  protected readonly gekoppeld = signal(true);

  protected readonly documentColumns = computed(() =>
    this.gekoppeld() ? GEKOPPELDE_COLUMNS : BASE_COLUMNS,
  );

  private readonly documentenQuery = injectQuery(() =>
    this.informatieObjectenService.listEnkelvoudigInformatieobjectenQuery({
      zaakUUID: this.zaak().uuid,
      gekoppeldeZaakDocumenten: this.gekoppeld(),
    }),
  );

  protected readonly isLoadingResults = computed(() =>
    this.documentenQuery.isFetching(),
  );

  @ViewChild("documentenTable", { read: MatSort, static: true })
  docSort!: MatSort;

  enkelvoudigInformatieObjecten = new MatTableDataSource<
    GeneratedType<"RestEnkelvoudigInformatieobject">
  >();
  documentPreviewRow?: GeneratedType<"RestEnkelvoudigInformatieobject"> | null;
  downloadAlsZipSelection = new SelectionModel<
    GeneratedType<"RestEnkelvoudigInformatieobject">
  >(true, []);

  constructor() {
    // Feed query results into the MatTableDataSource so Material sorting keeps working.
    effect(() => {
      const documenten = this.documentenQuery.data();
      if (documenten) {
        this.enkelvoudigInformatieObjecten.data =
          documenten as unknown as GekoppeldeZaakEnkelvoudigInformatieobject[];
      }
    });

    effect((onCleanup) => {
      const zaak = this.zaak();
      const websocketListeners = [
        this.websocketService.addListener(
          Opcode.UPDATED,
          ObjectType.ZAAK_INFORMATIEOBJECTEN,
          zaak.uuid,
          (event) => this.onZaakInformatieobjectenUpdated(event),
        ),
        this.websocketService.addListener(
          Opcode.UPDATED,
          ObjectType.ZAAK_BESLUITEN,
          zaak.uuid,
          () => this.reloadDocumenten(),
        ),
      ];
      onCleanup(() =>
        this.websocketService.removeListeners(websocketListeners),
      );
    });
  }

  ngAfterViewInit() {
    this.enkelvoudigInformatieObjecten.sort = this.docSort;
  }

  updateDocumentList() {
    return this.reloadDocumenten();
  }

  private reloadDocumenten() {
    return this.queryClient.invalidateQueries({
      queryKey: [LIST_QUERY_KEY, this.zaak().uuid],
    });
  }

  private onZaakInformatieobjectenUpdated(event?: ScreenEvent) {
    if (event?.objectId.detail) {
      this.informatieObjectenService
        .readEnkelvoudigInformatieobjectByZaakInformatieobjectUUID(
          event.objectId.detail,
        )
        .subscribe((enkelvoudigInformatieobject) => {
          this.utilService
            .openSnackbarAction(
              "msg.document.toegevoegd.aan.zaak",
              "actie.document.bekijken",
              { document: enkelvoudigInformatieobject.titel },
              7,
            )
            .subscribe(() => {
              this.router.navigate([
                "/informatie-objecten",
                enkelvoudigInformatieobject.uuid,
              ]);
            });
        });
    }

    this.reloadDocumenten();
  }

  emitDocumentMove(row: GeneratedType<"RestEnkelvoudigInformatieobject">) {
    this.documentMoveToCase.emit(row);
  }

  documentOntkoppelen(
    informatieobject: GeneratedType<"RestEnkelvoudigInformatieobject"> & {
      loading?: boolean;
    },
  ) {
    if (!informatieobject.uuid) return;

    informatieobject["loading"] = true;
    this.utilService.setLoading(true);
    this.informatieObjectenService
      .listZaakIdentificatiesForInformatieobject(informatieobject.uuid)
      .pipe(
        map((zaakIDs) => {
          delete informatieobject["loading"];
          this.utilService.setLoading(false);
          return zaakIDs
            .filter((zaakID) => zaakID !== this.zaak().identificatie)
            .join(", ");
        }),
      )
      .subscribe((zaakIDs) => {
        let melding: string;
        if (zaakIDs) {
          melding = this.translate.instant(
            "msg.document.ontkoppelen.meerdere.zaken.bevestigen",
            { zaken: zaakIDs, document: informatieobject.titel },
          );
        } else {
          melding = this.translate.instant(
            "msg.document.ontkoppelen.bevestigen",
            { document: informatieobject.titel },
          );
        }
        const dialogData = new DialogData<unknown, { reden: string }>({
          formFields: [
            new TextareaFormFieldBuilder()
              .id("reden")
              .label("reden")
              .validators(Validators.required)
              .maxlength(200)
              .build(),
          ],
          callback: ({ reden }) =>
            this.zakenService.ontkoppelInformatieObject({
              zaakUUID: this.zaak().uuid,
              documentUUID: informatieobject.uuid!,
              reden: reden,
            }),
          melding,
          confirmButtonActionKey: "actie.document.ontkoppelen",
          icon: "link_off",
        });
        this.dialog
          .open(DialogComponent, {
            data: dialogData,
          })
          .afterClosed()
          .subscribe((result) => {
            if (result) {
              this.reloadDocumenten();
              this.utilService.openSnackbar(
                "msg.document.ontkoppelen.uitgevoerd",
                { document: informatieobject.titel },
              );
            }
          });
      });
  }

  isPreviewBeschikbaar(formaat: FileFormat) {
    return FileFormatUtil.isPreviewAvailable(formaat);
  }

  getZaakUuidVanInformatieObject(
    informatieObject: GekoppeldeZaakEnkelvoudigInformatieobject,
  ) {
    return informatieObject.zaakUUID ?? this.zaak().uuid;
  }

  updateSelected(document: GeneratedType<"RestEnkelvoudigInformatieobject">) {
    this.downloadAlsZipSelection.toggle(document);
  }

  downloadAlsZip() {
    const uuids = this.downloadAlsZipSelection.selected.map(
      ({ uuid }) => uuid!,
    );

    this.downloadAlsZipSelection.clear();
    this.selectAll = false;

    return this.informatieObjectenService
      .getZIPDownload(uuids)
      .subscribe((response) => {
        this.utilService.downloadBlobResponse(
          response,
          this.zaak().identificatie,
        );
      });
  }

  updateAll($event?: MatCheckboxChange) {
    this.selectAll = !this.selectAll;
    if (!$event) return;

    this.enkelvoudigInformatieObjecten.data.forEach((document) => {
      if ($event.checked) {
        this.downloadAlsZipSelection.select(document);
        return;
      }
      this.downloadAlsZipSelection.deselect(document);
    });
  }

  getDownloadURL(informatieObject: GekoppeldeZaakEnkelvoudigInformatieobject) {
    return this.informatieObjectenService.getDownloadURL(
      informatieObject.uuid!,
    );
  }

  getFileIcon(
    filename: string,
  ): FileIcon | { color: string; icon: string; type: string } {
    return FileIcon.getIconByBestandsnaam(filename);
  }

  getFileTooltip(filetype: string): string {
    return this.translate.instant("bestandstype", {
      type: filetype.toUpperCase(),
    });
  }

  isBewerkenToegestaan(
    enkelvoudigInformatieobject: GeneratedType<"RestEnkelvoudigInformatieobject">,
  ) {
    return (
      Boolean(enkelvoudigInformatieobject.rechten?.wijzigen) &&
      FileFormatUtil.isOffice(enkelvoudigInformatieobject.formaat as FileFormat) // The backend converter supports other formats (such as .txt), but only allow office formats in the UI
    );
  }

  bewerken(
    enkelvoudigInformatieobject: GeneratedType<"RestEnkelvoudigInformatieobject">,
  ) {
    this.informatieObjectenService
      .editEnkelvoudigInformatieObjectInhoud(
        enkelvoudigInformatieobject.uuid!,
        this.zaak().uuid,
      )
      .subscribe((url) => {
        window.open(url);
      });
  }
}
