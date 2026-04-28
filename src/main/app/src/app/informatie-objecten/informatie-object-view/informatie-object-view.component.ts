/*
 * SPDX-FileCopyrightText: 2021 Atos, 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import {
  AfterViewInit,
  Component,
  OnDestroy,
  OnInit,
  ViewChild,
} from "@angular/core";
import { NgFor, NgIf, NgSwitch, NgSwitchCase } from "@angular/common";
import { Validators } from "@angular/forms";
import { MatButtonModule } from "@angular/material/button";
import { MatCardModule } from "@angular/material/card";
import { MatDividerModule } from "@angular/material/divider";
import { MatExpansionModule } from "@angular/material/expansion";
import { MatIconModule } from "@angular/material/icon";
import { MatSortModule } from "@angular/material/sort";
import { MatSidenav, MatSidenavContainer, MatSidenavModule } from "@angular/material/sidenav";
import { MatTableModule } from "@angular/material/table";
import { MatTabsModule } from "@angular/material/tabs";
import { MatTooltipModule } from "@angular/material/tooltip";
import { ActivatedRoute, Router, RouterModule } from "@angular/router";
import { TranslateModule, TranslateService } from "@ngx-translate/core";
import { MatDialog } from "@angular/material/dialog";
import { MatSort } from "@angular/material/sort";
import { MatTableDataSource } from "@angular/material/table";
import { DocumentViewerComponent } from "../../shared/document-viewer/document-viewer.component";
import { DocumentIconComponent } from "../../shared/document-icon/document-icon.component";
import { InformatieObjectIndicatiesComponent } from "../../shared/indicaties/informatie-object-indicaties/informatie-object-indicaties.component";
import { BestandsomvangPipe } from "../../shared/pipes/bestandsomvang.pipe";
import { DatumPipe } from "../../shared/pipes/datum.pipe";
import { EmptyPipe } from "../../shared/pipes/empty.pipe";
import { MimetypeToExtensionPipe } from "../../shared/pipes/mimetypeToExtension.pipe";
import { VertrouwelijkaanduidingToTranslationKeyPipe } from "../../shared/pipes/vertrouwelijkaanduiding-to-translation-key.pipe";
import { ReadMoreComponent } from "../../shared/read-more/read-more.component";
import { SideNavComponent } from "../../shared/side-nav/side-nav.component";
import { StaticTextComponent } from "../../shared/static-text/static-text.component";
import { InformatieObjectEditComponent } from "../informatie-object-edit/informatie-object-edit.component";
import { Observable, of, throwError } from "rxjs";
import { catchError, tap } from "rxjs/operators";
import { AsyncButtonMenuItem } from "src/app/shared/side-nav/menu-item/subscription-button-menu-item";
import { UtilService } from "../../core/service/util.service";
import { ObjectType } from "../../core/websocket/model/object-type";
import { Opcode } from "../../core/websocket/model/opcode";
import { WebsocketListener } from "../../core/websocket/model/websocket-listener";
import { WebsocketService } from "../../core/websocket/websocket.service";
import { ActionsViewComponent } from "../../shared/abstract-view/actions-view-component";
import {
  ConfirmDialogComponent,
  ConfirmDialogData,
} from "../../shared/confirm-dialog/confirm-dialog.component";
import { DialogData } from "../../shared/dialog/dialog-data";
import { DialogComponent } from "../../shared/dialog/dialog.component";
import { IndicatiesLayout } from "../../shared/indicaties/indicaties.component";
import { InputFormFieldBuilder } from "../../shared/material-form-builder/form-components/input/input-form-field-builder";
import { ButtonMenuItem } from "../../shared/side-nav/menu-item/button-menu-item";
import { HeaderMenuItem } from "../../shared/side-nav/menu-item/header-menu-item";
import { HrefMenuItem } from "../../shared/side-nav/menu-item/href-menu-item";
import { MenuItem } from "../../shared/side-nav/menu-item/menu-item";
import { GeneratedType } from "../../shared/utils/generated-types";
import { ZakenService } from "../../zaken/zaken.service";
import { InformatieObjectenService } from "../informatie-objecten.service";
import { FileFormat, FileFormatUtil } from "../model/file-format";

@Component({
  templateUrl: "./informatie-object-view.component.html",
  styleUrls: ["./informatie-object-view.component.less"],
  standalone: true,
  imports: [
    NgFor,
    NgIf,
    NgSwitch,
    NgSwitchCase,
    MatButtonModule,
    MatCardModule,
    MatDividerModule,
    MatExpansionModule,
    MatIconModule,
    MatSortModule,
    MatSidenavModule,
    MatTableModule,
    MatTabsModule,
    MatTooltipModule,
    RouterModule,
    TranslateModule,
    DocumentViewerComponent,
    DocumentIconComponent,
    InformatieObjectIndicatiesComponent,
    BestandsomvangPipe,
    DatumPipe,
    EmptyPipe,
    MimetypeToExtensionPipe,
    VertrouwelijkaanduidingToTranslationKeyPipe,
    ReadMoreComponent,
    SideNavComponent,
    StaticTextComponent,
    InformatieObjectEditComponent,
  ],
})
export class InformatieObjectViewComponent
  extends ActionsViewComponent
  implements OnInit, AfterViewInit, OnDestroy
{
  readonly indicatiesLayout = IndicatiesLayout;
  infoObject!: GeneratedType<"RestEnkelvoudigInformatieobject">;
  laatsteVersieInfoObject?: GeneratedType<"RestEnkelvoudigInformatieobject">;
  zaakInformatieObjecten: GeneratedType<"RestZaakInformatieobject">[] = [];
  zaak?: GeneratedType<"RestZaak">;
  documentNieuweVersieGegevens?: GeneratedType<"RestEnkelvoudigInformatieObjectVersieGegevens">;
  documentPreviewBeschikbaar = false;
  menu: MenuItem[] = [];
  activeSideAction: string | null = null;
  versieInformatie: string | null = null;
  historie = new MatTableDataSource<GeneratedType<"HistoryLine">>();

  historieColumns: string[] = [
    "datum",
    "gebruiker",
    "wijziging",
    "actie",
    "oudeWaarde",
    "nieuweWaarde",
    "toelichting",
  ];

  @ViewChild("actionsSidenav") actionsSidenav!: MatSidenav;
  @ViewChild("menuSidenav") menuSidenav!: MatSidenav;
  @ViewChild("sideNavContainer") sideNavContainer!: MatSidenavContainer;
  @ViewChild(MatSort) sort!: MatSort;
  private documentListener?: WebsocketListener;

  constructor(
    private informatieObjectenService: InformatieObjectenService,
    private route: ActivatedRoute,
    private readonly utilService: UtilService,
    private websocketService: WebsocketService,
    private router: Router,
    private translate: TranslateService,
    private dialog: MatDialog,
    private zakenService: ZakenService,
  ) {
    super();
  }

  ngOnInit() {
    this.subscriptions$.push(
      this.route.data.subscribe((data) => {
        this.infoObject =
          data.informatieObject as GeneratedType<"RestEnkelvoudigInformatieobject">;
        this.zaak = data.zaak as GeneratedType<"RestZaak">;
        this.informatieObjectenService
          .readEnkelvoudigInformatieobject(
            this.infoObject.uuid!,
            this.zaak?.uuid,
          )
          .subscribe((infoObject) => {
            this.laatsteVersieInfoObject = infoObject;
            this.toevoegenActies();
            this.updateVersieInformatie();
            this.loadZaakInformatieobjecten();
          });
        this.documentPreviewBeschikbaar = FileFormatUtil.isPreviewAvailable(
          this.infoObject.formaat as FileFormat,
        );
        this.utilService.setTitle("title.document", {
          document: this.infoObject.identificatie,
        });

        this.documentListener = this.websocketService.addListener(
          Opcode.UPDATED,
          ObjectType.ENKELVOUDIG_INFORMATIEOBJECT,
          this.infoObject.uuid!,
          () => {
            this.loadInformatieObject();
            this.loadZaakInformatieobjecten();
            this.loadHistorie();
          },
        );

        this.loadHistorie();
      }),
    );
  }

  ngAfterViewInit() {
    super.ngAfterViewInit();
    this.historie.sortingDataAccessor = (item, property) => {
      switch (property) {
        case "datum":
          return item.datumTijd ?? "";
        case "gebruiker":
          return item.door ?? "";
        default:
          return item[property as keyof typeof item] ?? "";
      }
    };
    this.historie.sort = this.sort;
  }

  ngOnDestroy() {
    this.websocketService.removeListener(this.documentListener);
  }

  private toevoegenActies() {
    this.menu = [new HeaderMenuItem("informatieobject")];

    if (this.laatsteVersieInfoObject?.rechten?.lezen) {
      this.menu.push(
        new HrefMenuItem(
          "actie.downloaden",
          this.informatieObjectenService.getDownloadURL(
            this.infoObject.uuid!,
            this.infoObject.versie,
          ),
          "save_alt",
        ),
      );
    }

    if (
      this.laatsteVersieInfoObject?.rechten?.toevoegenNieuweVersie &&
      this.zaak
    ) {
      this.menu.push(
        new ButtonMenuItem(
          "actie.nieuwe.versie.toevoegen",
          () => {
            this.informatieObjectenService
              .readHuidigeVersieEnkelvoudigInformatieObject(
                this.infoObject.uuid!,
              )
              .subscribe((infoObject) => {
                this.documentNieuweVersieGegevens = infoObject;
                void this.actionsSidenav.open();
              });
          },
          "difference",
        ),
      );
    }

    if (
      this.zaak &&
      this.laatsteVersieInfoObject?.rechten?.wijzigen &&
      FileFormatUtil.isOffice(this.infoObject.formaat as FileFormat)
    ) {
      this.menu.push(
        new ButtonMenuItem(
          "actie.bewerken",
          () => {
            this.informatieObjectenService
              .editEnkelvoudigInformatieObjectInhoud(
                this.infoObject.uuid!,
                this.zaak!.uuid!,
              )
              .subscribe((url) => {
                window.open(url);
              });
          },
          "edit",
        ),
      );
    }

    if (
      !this.laatsteVersieInfoObject?.gelockedDoor &&
      this.laatsteVersieInfoObject?.rechten?.vergrendelen
    ) {
      const button = new ButtonMenuItem(
        "actie.lock",
        () => {
          button.disabled = true;
          this.informatieObjectenService
            .lockInformatieObject(this.infoObject.uuid!, this.zaak!.uuid!)
            .pipe(
              catchError((e) => {
                // we only need to do this on error, because on success we get a new button
                button.disabled = false;
                return throwError(() => e);
              }),
            )
            .subscribe();
        },
        "lock",
      );
      this.menu.push(button);
    }

    if (
      this.laatsteVersieInfoObject?.gelockedDoor &&
      this.laatsteVersieInfoObject?.rechten?.ontgrendelen
    ) {
      const button = new ButtonMenuItem(
        "actie.unlock",
        () => {
          button.disabled = true;
          this.informatieObjectenService
            .unlockInformatieObject(this.infoObject.uuid!, this.zaak!.uuid!)
            .pipe(
              catchError((e) => {
                // we only need to do this on error, because on success we get a new button
                button.disabled = false;
                return throwError(() => e);
              }),
            )
            .subscribe();
        },
        "lock_open",
      );
      this.menu.push(button);
    }

    if (
      this.laatsteVersieInfoObject?.rechten?.verwijderen &&
      !this.laatsteVersieInfoObject?.isBesluitDocument
    ) {
      this.menu.push(
        new ButtonMenuItem(
          "actie.verwijderen",
          () => this.openDocumentVerwijderenDialog(),
          "delete",
        ),
      );
    }

    if (
      !this.laatsteVersieInfoObject?.ondertekening &&
      this.laatsteVersieInfoObject?.rechten?.ondertekenen
    ) {
      this.menu.push(
        new ButtonMenuItem(
          "actie.ondertekenen",
          () => this.openDocumentOndertekenenDialog(),
          "fact_check",
        ),
      );
    }

    if (
      this.zaak &&
      this.laatsteVersieInfoObject?.rechten?.converteren &&
      FileFormatUtil.isOffice(this.infoObject.formaat as FileFormat)
    ) {
      this.menu.push(
        new AsyncButtonMenuItem(
          "actie.converteren",
          () =>
            this.informatieObjectenService.convertInformatieObjectToPDF(
              this.infoObject.uuid!,
              this.zaak!.uuid!,
            ),
          "picture_as_pdf",
        ),
      );
    }
  }

  private loadZaakInformatieobjecten() {
    this.informatieObjectenService
      .listZaakInformatieobjecten(this.infoObject.uuid!)
      .subscribe((zaakInformatieObjecten) => {
        this.zaakInformatieObjecten = zaakInformatieObjecten;
        this.loadZaak();
      });
  }

  private loadHistorie() {
    this.informatieObjectenService
      .listHistorie(this.infoObject.uuid!)
      .subscribe((historie) => {
        this.historie.data = historie;
      });
  }

  private loadInformatieObject() {
    this.informatieObjectenService
      .readEnkelvoudigInformatieobject(this.infoObject.uuid!, this.zaak?.uuid)
      .subscribe((infoObject) => {
        this.infoObject = infoObject;
        this.laatsteVersieInfoObject = infoObject;
        this.toevoegenActies();
        this.updateVersieInformatie();
        this.documentPreviewBeschikbaar = FileFormatUtil.isPreviewAvailable(
          this.infoObject.formaat as FileFormat,
        );
      });
  }

  protected haalVersieOp(versie: number) {
    this.websocketService.removeListener(this.documentListener);
    this.router.navigate([
      "/informatie-objecten",
      this.infoObject.uuid,
      versie,
      this.zaak?.uuid,
    ]);
  }

  protected versieToegevoegd(
    informatieobject: GeneratedType<"RestEnkelvoudigInformatieobject">,
  ) {
    if (!informatieobject.versie) return;
    this.haalVersieOp(informatieobject.versie);
  }

  private updateVersieInformatie() {
    this.versieInformatie = this.translate.instant("versie.x.van", {
      versie: this.infoObject?.versie,
      laatsteVersie: this.laatsteVersieInfoObject?.versie,
    });
  }

  private openDocumentVerwijderenDialog() {
    const dialogData = new DialogData<unknown, { reden: string }>({
      formFields: this.zaak
        ? [
            new InputFormFieldBuilder()
              .id("reden")
              .label("actie.document.verwijderen.reden")
              .validators(Validators.required)
              .maxlength(100)
              .build(),
          ]
        : [],
      callback: (results) =>
        this.deleteEnkelvoudigInformatieObject$(results.reden),
      melding: this.translate.instant("msg.document.verwijderen.bevestigen", {
        document: this.infoObject?.titel,
      }),
      confirmButtonActionKey: "actie.document.verwijderen",
      icon: "delete",
    });

    this.dialog
      .open(DialogComponent, { data: dialogData })
      .afterClosed()
      .subscribe((result) => {
        this.activeSideAction = null;

        if (result) {
          this.utilService.openSnackbar("msg.document.verwijderen.uitgevoerd", {
            document: this.infoObject.titel,
          });
          this.router.navigate(
            this.zaak
              ? ["/zaken", this.zaak.identificatie]
              : ["/documenten", "ontkoppelde"],
          );
        }
      });
  }

  private openDocumentOndertekenenDialog() {
    const dialogData = new ConfirmDialogData(
      {
        key: "msg.document.ondertekenen.bevestigen",
        args: { document: this.infoObject.titel },
      },
      this.informatieObjectenService.ondertekenInformatieObject(
        this.infoObject.uuid!,
        this.zaak!.uuid!,
      ),
    );

    this.dialog.open(ConfirmDialogComponent, { data: dialogData });
  }

  private deleteEnkelvoudigInformatieObject$(reden?: string): Observable<void> {
    if (!this.infoObject?.uuid) return of();
    return this.informatieObjectenService
      .deleteEnkelvoudigInformatieObject(this.infoObject.uuid, {
        zaakUuid: this.zaak?.uuid,
        reden,
      })
      .pipe(
        tap(() => this.websocketService.suspendListener(this.documentListener)),
      );
  }

  /**
   * Voor het geval dat er bij navigatie naar het enkelvoudiginformatieobject geen zaak meegegeven is,
   * dan wordt deze via de verkorte zaak gegevens opgehaald.
   *
   * Als er ook geen verkorte zaak gegevens beschikbaar, dan is dit een document zonder zaak.
   */
  private loadZaak() {
    const zaakobject = this.zaakInformatieObjecten.at(0);
    if (!this.zaak && zaakobject?.zaakIdentificatie) {
      this.zakenService
        .readZaakByID(zaakobject.zaakIdentificatie)
        .subscribe((zaak) => {
          this.zaak = zaak;
        });
    }
  }
}
