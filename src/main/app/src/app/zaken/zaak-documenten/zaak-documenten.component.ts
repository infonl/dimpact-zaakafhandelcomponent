/*
 * SPDX-FileCopyrightText: 2022 Atos, 2024 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { SelectionModel } from "@angular/cdk/collections";
import {
  AfterViewInit,
  Component,
  computed,
  effect,
  inject,
  input,
  output,
  signal,
  viewChild,
} from "@angular/core";
import {
  MatButton,
  MatIconAnchor,
  MatIconButton,
} from "@angular/material/button";
import { MatCardModule } from "@angular/material/card";
import { MatCheckbox, MatCheckboxChange } from "@angular/material/checkbox";
import { MatIconModule } from "@angular/material/icon";
import { MatMenuModule } from "@angular/material/menu";
import { MatSlideToggleModule } from "@angular/material/slide-toggle";
import { MatSort, MatSortModule } from "@angular/material/sort";
import { MatTableDataSource, MatTableModule } from "@angular/material/table";
import { MatTooltip } from "@angular/material/tooltip";
import { Router, RouterLink } from "@angular/router";
import { TranslatePipe, TranslateService } from "@ngx-translate/core";
import { injectQuery, QueryClient } from "@tanstack/angular-query-experimental";
import { map } from "rxjs/operators";
import { UtilService } from "../../core/service/util.service";
import { ScreenEvent } from "../../core/websocket/model/screen-event";
import { WebsocketService } from "../../core/websocket/websocket.service";
import { DocumentDialogService } from "../../informatie-objecten/document-dialog.service";
import { InformatieObjectenService } from "../../informatie-objecten/informatie-objecten.service";
import {
  FileFormat,
  FileFormatUtil,
} from "../../informatie-objecten/model/file-format";
import { FileIcon } from "../../informatie-objecten/model/file-icon";
import { GekoppeldeZaakEnkelvoudigInformatieobject } from "../../informatie-objecten/model/gekoppelde.zaak.enkelvoudig.informatieobject";
import { detailExpand } from "../../shared/animations/animations";
import { DocumentIconComponent } from "../../shared/document-icon/document-icon.component";
import { DocumentViewerComponent } from "../../shared/document-viewer/document-viewer.component";
import { injectMutation } from "../../shared/http/inject-mutation";
import { runMutation } from "../../shared/http/run-mutation";
import { IndicatiesLayout } from "../../shared/indicaties/indicaties.component";
import { InformatieObjectIndicatiesComponent } from "../../shared/indicaties/informatie-object-indicaties/informatie-object-indicaties.component";
import { BestandsomvangPipe } from "../../shared/pipes/bestandsomvang.pipe";
import { DatumPipe } from "../../shared/pipes/datum.pipe";
import { EmptyPipe } from "../../shared/pipes/empty.pipe";
import { I18nKeyPipe } from "../../shared/pipes/i18n-key.pipe";
import { VertrouwelijkaanduidingToTranslationKeyPipe } from "../../shared/pipes/vertrouwelijkaanduiding-to-translation-key.pipe";
import { RefetchErrorHintComponent } from "../../shared/refetch-error-hint/refetch-error-hint.component";
import { GeneratedType } from "../../shared/utils/generated-types";
import { ZaakIdentificatieComponent } from "../../shared/zaak-identificatie/zaak-identificatie.component";
import { ZakenService } from "../zaken.service";

const LIST_QUERY_KEY = "/rest/informatieobjecten/informatieobjectenList";

const BASE_COLUMNS = [
  "downloaden",
  "preview",
  "bestandstype",
  "titel",
  "identificatie",
  "informatieobjectTypeOmschrijving",
  "bestandsomvang",
  "status",
  "vertrouwelijkheidaanduiding",
  "registratiedatumTijd",
  "auteur",
  "indicaties",
  "url",
];

const DATA_COLUMN_DEFINITIONS: ReadonlyArray<{
  column: string;
  header?: string;
}> = [
  { column: "identificatie" },
  { column: "zaakIdentificatie" },
  { column: "relatieType" },
  {
    column: "informatieobjectTypeOmschrijving",
    header: "informatieobjectTypeOmschrijving.kort",
  },
  { column: "bestandsomvang", header: "bestandsomvang.kort" },
  { column: "status" },
  {
    column: "vertrouwelijkheidaanduiding",
    header: "vertrouwelijkheidaanduiding.kort",
  },
  { column: "registratiedatumTijd" },
  { column: "auteur" },
];

const GEKOPPELDE_COLUMNS = [
  "downloaden",
  "preview",
  "bestandstype",
  "titel",
  "identificatie",
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
    RefetchErrorHintComponent,
    ZaakIdentificatieComponent,
    RouterLink,
    MatButton,
    MatCardModule,
    MatSlideToggleModule,
    MatTableModule,
    MatSortModule,
    MatCheckbox,
    MatIconModule,
    MatIconAnchor,
    MatIconButton,
    MatMenuModule,
    MatTooltip,
    TranslatePipe,
    DocumentIconComponent,
    DocumentViewerComponent,
    InformatieObjectIndicatiesComponent,
    EmptyPipe,
    I18nKeyPipe,
    BestandsomvangPipe,
    DatumPipe,
    VertrouwelijkaanduidingToTranslationKeyPipe,
  ],
})
export class ZaakDocumentenComponent implements AfterViewInit {
  private readonly informatieObjectenService = inject(
    InformatieObjectenService,
  );
  private readonly websocketService = inject(WebsocketService);
  private readonly utilService = inject(UtilService);
  private readonly zakenService = inject(ZakenService);
  private readonly documentDialogService = inject(DocumentDialogService);
  private readonly translate = inject(TranslateService);
  private readonly router = inject(Router);
  private readonly queryClient = inject(QueryClient);

  private readonly zipDownloadMutation = injectMutation(
    () => this.informatieObjectenService.getZIPDownload(),
    {
      onSuccess: (zip) =>
        this.utilService.downloadBlobResponse(zip, this.zaak().identificatie),
    },
  );

  readonly indicatiesLayout = IndicatiesLayout;
  readonly zaak = input.required<GeneratedType<"RestZaak">>();
  readonly documentMoveToCase =
    output<GeneratedType<"RestEnkelvoudigInformatieobject">>();

  private readonly zaakUuid = computed(() => this.zaak().uuid);

  protected readonly heeftGerelateerdeZaken = computed(
    () => (this.zaak().gerelateerdeZaken?.length ?? 0) > 0,
  );

  protected readonly linkedDocumentsEnabled = signal(true);

  protected readonly includeLinkedDocuments = computed(
    () => this.heeftGerelateerdeZaken() && this.linkedDocumentsEnabled(),
  );

  protected readonly dataColumnDefinitions = DATA_COLUMN_DEFINITIONS;

  protected readonly documentColumns = computed(() =>
    this.includeLinkedDocuments() ? GEKOPPELDE_COLUMNS : BASE_COLUMNS,
  );

  protected readonly documentenQuery = injectQuery(() =>
    this.informatieObjectenService.listEnkelvoudigInformatieobjecten({
      zaakUUID: this.zaakUuid(),
      shouldIncludeGekoppeldeZaakDocumenten: this.includeLinkedDocuments(),
    }),
  );

  // Joined so the listener effect below only re-runs when the set changes.
  private readonly documentUuids = computed(() =>
    (this.documentenQuery.data() ?? [])
      .map(({ uuid }) => uuid)
      .filter(Boolean)
      .join(","),
  );

  protected readonly isLoadingResults = computed(() =>
    this.documentenQuery.isFetching(),
  );

  protected selectAll = false;

  readonly docSort = viewChild.required("documentenTable", { read: MatSort });

  enkelvoudigInformatieObjecten = new MatTableDataSource<
    GeneratedType<"RestEnkelvoudigInformatieobject">
  >();
  documentPreviewRow?: GeneratedType<"RestEnkelvoudigInformatieobject"> | null;
  downloadAlsZipSelection = new SelectionModel<
    GeneratedType<"RestEnkelvoudigInformatieobject">
  >(true, []);

  constructor() {
    // Feed query results into the MatTableDataSource so Material sorting keeps working.
    // Use `?? []` so the table clears while transitioning to an uncached key (zaak/toggle change)
    // instead of briefly showing the previous case's documents. An in-place refetch
    // (websocket invalidation) keeps data() populated for the same key, so this never clears
    // mid-refetch
    effect(() => {
      this.enkelvoudigInformatieObjecten.data = (this.documentenQuery.data() ??
        []) as unknown as GekoppeldeZaakEnkelvoudigInformatieobject[];
    });

    let previousZaak: GeneratedType<"RestZaak"> | undefined;
    effect(() => {
      const zaak = this.zaak();
      if (previousZaak && previousZaak !== zaak) {
        this.reloadDocumenten();
      }
      previousZaak = zaak;
    });

    effect((onCleanup) => {
      const zaakUuid = this.zaakUuid();
      const websocketListeners = [
        this.websocketService.addListener(
          "UPDATED",
          "ZAAK_INFORMATIEOBJECTEN",
          zaakUuid,
          (event) => this.onZaakInformatieobjectenUpdated(event),
        ),
        this.websocketService.addListener(
          "UPDATED",
          "ZAAK_BESLUITEN",
          zaakUuid,
          () => this.reloadDocumenten(),
        ),
      ];
      onCleanup(() =>
        this.websocketService.removeListeners(websocketListeners),
      );
    });

    // Document changes are announced per informatieobject, not on the zaak.
    effect((onCleanup) => {
      const documentUuids = this.documentUuids();
      if (!documentUuids) return;
      const websocketListeners = documentUuids
        .split(",")
        .map((documentUuid) =>
          this.websocketService.addListener(
            "UPDATED",
            "ENKELVOUDIG_INFORMATIEOBJECT",
            documentUuid,
            () => this.reloadDocumenten(),
          ),
        );
      onCleanup(() =>
        this.websocketService.removeListeners(websocketListeners),
      );
    });
  }

  ngAfterViewInit() {
    this.enkelvoudigInformatieObjecten.sort = this.docSort();
  }

  updateDocumentList() {
    return this.reloadDocumenten();
  }

  private reloadDocumenten() {
    return this.queryClient.invalidateQueries({
      queryKey: [LIST_QUERY_KEY, { zaakUUID: this.zaakUuid() }],
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
        this.documentDialogService
          .openOntkoppelDocument(melding, (reden) =>
            runMutation(
              this.queryClient,
              this.zakenService.ontkoppelInformatieObject(informatieobject),
              {
                zaakUUID: this.zaak().uuid,
                documentUUID: informatieobject.uuid!,
                reden,
              },
            ),
          )
          .afterClosed()
          .subscribe((result) => {
            if (result) {
              this.reloadDocumenten();
            }
          });
      });
  }

  isPreviewBeschikbaar(formaat: FileFormat) {
    return FileFormatUtil.isPreviewAvailable(formaat);
  }

  protected togglePreview(
    document: GeneratedType<"RestEnkelvoudigInformatieobject">,
  ) {
    this.documentPreviewRow =
      this.documentPreviewRow === document ? null : document;
  }

  protected onLinkedDocumentsToggle(enabled: boolean) {
    this.linkedDocumentsEnabled.set(enabled);
    this.downloadAlsZipSelection.clear();
    this.selectAll = false;
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

    this.zipDownloadMutation.mutate(uuids);
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
      Boolean(enkelvoudigInformatieobject.rechten?.canWijzigen) &&
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
