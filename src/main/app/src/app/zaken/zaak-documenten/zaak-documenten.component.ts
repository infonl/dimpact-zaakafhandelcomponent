/*
 * SPDX-FileCopyrightText: 2022 Atos, 2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { SelectionModel } from "@angular/cdk/collections";
import {
  AfterViewInit,
  Component,
  EventEmitter,
  Input,
  OnChanges,
  OnDestroy,
  OnInit,
  Output,
  SimpleChanges,
  ViewChild,
} from "@angular/core";
import { FormControl, Validators } from "@angular/forms";
import { MatCheckboxChange } from "@angular/material/checkbox";
import { MatDialog } from "@angular/material/dialog";
import { MatSort } from "@angular/material/sort";
import { MatTableDataSource } from "@angular/material/table";
import { Router } from "@angular/router";
import { TranslateService } from "@ngx-translate/core";
import { map } from "rxjs/operators";
import { UtilService } from "../../core/service/util.service";
import { ObjectType } from "../../core/websocket/model/object-type";
import { Opcode } from "../../core/websocket/model/opcode";
import { ScreenEvent } from "../../core/websocket/model/screen-event";
import { WebsocketListener } from "../../core/websocket/model/websocket-listener";
import { WebsocketService } from "../../core/websocket/websocket.service";
import { InformatieObjectVerplaatsService } from "../../informatie-objecten/informatie-object-verplaats.service";
import { InformatieObjectenService } from "../../informatie-objecten/informatie-objecten.service";
import {
  FileFormat,
  FileFormatUtil,
} from "../../informatie-objecten/model/file-format";
import { FileIcon } from "../../informatie-objecten/model/file-icon";
import { GekoppeldeZaakEnkelvoudigInformatieobject } from "../../informatie-objecten/model/gekoppelde.zaak.enkelvoudig.informatieobject";
import { InformatieobjectZoekParameters } from "../../informatie-objecten/model/informatieobject-zoek-parameters";
import { detailExpand } from "../../shared/animations/animations";
import { DialogData } from "../../shared/dialog/dialog-data";
import { DialogComponent } from "../../shared/dialog/dialog.component";
import { IndicatiesLayout } from "../../shared/indicaties/indicaties.component";
import { TextareaFormFieldBuilder } from "../../shared/material-form-builder/form-components/textarea/textarea-form-field-builder";
import { GeneratedType } from "../../shared/utils/generated-types";
import { Zaak } from "../model/zaak";
import { ZakenService } from "../zaken.service";

@Component({
  selector: "zac-zaak-documenten",
  templateUrl: "./zaak-documenten.component.html",
  styleUrls: ["./zaak-documenten.component.less"],
  animations: [detailExpand],
})
export class ZaakDocumentenComponent
  implements OnInit, AfterViewInit, OnDestroy, OnChanges
{
  readonly indicatiesLayout = IndicatiesLayout;
  @Input({ required: true }) zaak!: Zaak;
  @Output() documentMoveToCase = new EventEmitter<
    GeneratedType<"RestEnkelvoudigInformatieobject">
  >();

  heeftGerelateerdeZaken = false;
  selectAll = false;
  toonGekoppeldeZaakDocumenten = new FormControl(false);
  documentColumns = [
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
  isLoadingResults = true;
  @ViewChild("documentenTable", { read: MatSort, static: true })
  docSort!: MatSort;

  enkelvoudigInformatieObjecten = new MatTableDataSource<
    GeneratedType<"RestEnkelvoudigInformatieobject">
  >();
  documentPreviewRow?: GeneratedType<"RestEnkelvoudigInformatieobject"> | null;
  downloadAlsZipSelection = new SelectionModel<
    GeneratedType<"RestEnkelvoudigInformatieobject">
  >(true, []);

  private websocketListeners: WebsocketListener[] = [];

  constructor(
    private informatieObjectenService: InformatieObjectenService,
    private websocketService: WebsocketService,
    private utilService: UtilService,
    private zakenService: ZakenService,
    private dialog: MatDialog,
    private translate: TranslateService,
    private informatieObjectVerplaatsService: InformatieObjectVerplaatsService,
    private router: Router,
  ) {}

  ngOnInit(): void {
    this.init(this.zaak, false);
  }

  init(zaak: Zaak, reload: boolean) {
    this.heeftGerelateerdeZaken = zaak.gerelateerdeZaken?.length > 0;

    if (reload) {
      this.websocketService.removeListeners(this.websocketListeners);
      this.websocketListeners = [];
    }

    this.websocketListeners.push(
      this.websocketService.addListener(
        Opcode.UPDATED,
        ObjectType.ZAAK_INFORMATIEOBJECTEN,
        zaak.uuid,
        (event) => this.loadInformatieObjecten(event),
      ),
    );

    this.websocketListeners.push(
      this.websocketService.addListener(
        Opcode.UPDATED,
        ObjectType.ZAAK_BESLUITEN,
        zaak.uuid,
        () => this.loadInformatieObjecten(),
      ),
    );

    this.loadInformatieObjecten();
  }

  ngAfterViewInit(): void {
    this.enkelvoudigInformatieObjecten.sort = this.docSort;
  }

  ngOnChanges(changes: SimpleChanges): void {
    if (changes.zaak && !changes.zaak.firstChange) {
      this.init(this.zaak, true);
      this.heeftGerelateerdeZaken = 0 < this.zaak.gerelateerdeZaken.length;
    }
  }

  ngOnDestroy(): void {
    this.websocketService.removeListeners(this.websocketListeners);
  }

  private loadInformatieObjecten(event?: ScreenEvent): void {
    if (event) {
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

    this.searchEnkelvoudigeInformatieObjecten();
  }

  private searchEnkelvoudigeInformatieObjecten(): void {
    const zoekParameters = new InformatieobjectZoekParameters();
    zoekParameters.zaakUUID = this.zaak.uuid;
    zoekParameters.gekoppeldeZaakDocumenten =
      !!this.toonGekoppeldeZaakDocumenten.value;
    this.isLoadingResults = true;

    this.informatieObjectenService
      .listEnkelvoudigInformatieobjecten(zoekParameters)
      .subscribe((objecten) => {
        this.enkelvoudigInformatieObjecten.data =
          objecten as unknown as GekoppeldeZaakEnkelvoudigInformatieobject[];
        this.isLoadingResults = false;
      });
  }

  documentVerplaatsen(
    informatieobject: GeneratedType<"RestEnkelvoudigInformatieobject">,
  ): void {
    this.informatieObjectVerplaatsService.addTeVerplaatsenDocument(
      informatieobject,
      this.zaak.identificatie,
    );
  }

  emitDocumentMove(
    row: GeneratedType<"RestEnkelvoudigInformatieobject">,
  ): void {
    this.documentMoveToCase.emit(row);
  }

  updateDocumentList(): void {
    this.loadInformatieObjecten();
  }

  documentOntkoppelen(
    informatieobject: GeneratedType<"RestEnkelvoudigInformatieobject"> & {
      loading?: boolean;
    },
  ): void {
    if (!informatieobject.uuid) {
      return;
    }

    informatieobject["loading"] = true;
    this.utilService.setLoading(true);
    this.informatieObjectenService
      .listZaakIdentificatiesForInformatieobject(informatieobject.uuid)
      .pipe(
        map((zaakIDs) => {
          delete informatieobject["loading"];
          this.utilService.setLoading(false);
          return zaakIDs
            .filter((zaakID) => zaakID !== this.zaak.identificatie)
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
        const dialogData = new DialogData(
          [
            new TextareaFormFieldBuilder()
              .id("reden")
              .label("reden")
              .validators(Validators.required)
              .build(),
          ],
          (results: Record<string, any>) =>
            this.zakenService.ontkoppelInformatieObject({
              zaakUUID: this.zaak.uuid,
              documentUUID: informatieobject.uuid,
              reden: results?.reden,
            }),
          melding,
        );
        this.dialog
          .open(DialogComponent, {
            data: dialogData,
          })
          .afterClosed()
          .subscribe((result) => {
            if (result) {
              this.searchEnkelvoudigeInformatieObjecten();
              this.utilService.openSnackbar(
                "msg.document.ontkoppelen.uitgevoerd",
                { document: informatieobject.titel },
              );
            }
          });
      });
  }

  isDocumentVerplaatsenDisabled(
    informatieobject: GeneratedType<"RestEnkelvoudigInformatieobject">,
  ): boolean {
    return this.informatieObjectVerplaatsService.isReedsTeVerplaatsen(
      informatieobject.uuid,
    );
  }

  isOntkoppelenDisabled(
    informatieobject: GeneratedType<"RestEnkelvoudigInformatieobject"> & {
      loading?: boolean;
    },
  ): boolean {
    return (
      informatieobject["loading"] ||
      this.informatieObjectVerplaatsService.isReedsTeVerplaatsen(
        informatieobject.uuid,
      )
    );
  }

  isPreviewBeschikbaar(formaat: FileFormat): boolean {
    return FileFormatUtil.isPreviewAvailable(formaat);
  }

  toggleGekoppeldeZaakDocumenten() {
    this.documentColumns = this.toonGekoppeldeZaakDocumenten.value
      ? [
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
        ]
      : [
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
    this.loadInformatieObjecten();
  }

  getZaakUuidVanInformatieObject(
    informatieObject: GekoppeldeZaakEnkelvoudigInformatieobject,
  ): string {
    return informatieObject.zaakUUID
      ? informatieObject.zaakUUID
      : this.zaak.uuid;
  }

  updateSelected(
    $event: MatCheckboxChange,
    document: GeneratedType<"RestEnkelvoudigInformatieobject">,
  ): void {
    if ($event) {
      this.downloadAlsZipSelection.toggle(document);
    }
  }

  downloadAlsZip() {
    const uuids: string[] = [];
    this.downloadAlsZipSelection.selected.forEach((document) => {
      uuids.push(document.uuid);
    });

    this.downloadAlsZipSelection.clear();
    this.selectAll = false;

    return this.informatieObjectenService
      .getZIPDownload(uuids)
      .subscribe((response) => {
        this.utilService.downloadBlobResponse(
          response,
          this.zaak.identificatie,
        );
      });
  }

  updateAll($event?: MatCheckboxChange) {
    this.selectAll = !this.selectAll;
    if (!$event) {
      return;
    }

    this.enkelvoudigInformatieObjecten.data.forEach((document) => {
      if ($event.checked) {
        this.downloadAlsZipSelection.select(document);
        return;
      }
      this.downloadAlsZipSelection.deselect(document);
    });
  }

  getDownloadURL(
    informatieObject: GekoppeldeZaakEnkelvoudigInformatieobject,
  ): string {
    return this.informatieObjectenService.getDownloadURL(informatieObject.uuid);
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
  ): boolean {
    return (
      Boolean(enkelvoudigInformatieobject.rechten?.wijzigen) &&
      FileFormatUtil.isOffice(enkelvoudigInformatieobject.formaat as FileFormat)
    );
  }

  bewerken(
    enkelvoudigInformatieobject: GeneratedType<"RestEnkelvoudigInformatieobject">,
  ) {
    this.informatieObjectenService
      .editEnkelvoudigInformatieObjectInhoud(
        enkelvoudigInformatieobject.uuid,
        this.zaak?.uuid,
      )
      .subscribe((url) => {
        window.open(url);
      });
  }
}
