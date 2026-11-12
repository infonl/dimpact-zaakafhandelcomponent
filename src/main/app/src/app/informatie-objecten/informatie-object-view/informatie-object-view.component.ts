/*
 * SPDX-FileCopyrightText: 2021 Atos, 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { Location } from "@angular/common";
import {
  AfterViewInit,
  Component,
  computed,
  effect,
  inject,
  OnDestroy,
  OnInit,
  signal,
  untracked,
  viewChild,
} from "@angular/core";
import { MatButtonModule } from "@angular/material/button";
import { MatCardModule } from "@angular/material/card";
import { MatDialog } from "@angular/material/dialog";
import { MatDividerModule } from "@angular/material/divider";
import { MatExpansionModule } from "@angular/material/expansion";
import { MatIconModule } from "@angular/material/icon";
import {
  MatSidenav,
  MatSidenavContainer,
  MatSidenavModule,
} from "@angular/material/sidenav";
import { MatSort, MatSortModule } from "@angular/material/sort";
import { MatTableDataSource, MatTableModule } from "@angular/material/table";
import { MatTabsModule } from "@angular/material/tabs";
import { MatTooltipModule } from "@angular/material/tooltip";
import { ActivatedRoute, Router, RouterModule } from "@angular/router";
import { TranslatePipe, TranslateService } from "@ngx-translate/core";
import { injectQuery, QueryClient } from "@tanstack/angular-query-experimental";
import { from, Observable, of } from "rxjs";
import { map, tap } from "rxjs/operators";
import { AsyncButtonMenuItem } from "src/app/shared/side-nav/menu-item/subscription-button-menu-item";
import { UtilService } from "../../core/service/util.service";
import { WebsocketListener } from "../../core/websocket/model/websocket-listener";
import { WebsocketService } from "../../core/websocket/websocket.service";
import { ActionsViewComponent } from "../../shared/abstract-view/actions-view-component";
import {
  ConfirmDialogComponent,
  ConfirmDialogData,
} from "../../shared/confirm-dialog/confirm-dialog.component";
import { TypedMatCellDefDirective } from "../../shared/directives/typed-mat-cell-def.directive";
import { DocumentIconComponent } from "../../shared/document-icon/document-icon.component";
import { DocumentViewerComponent } from "../../shared/document-viewer/document-viewer.component";
import { runMutation } from "../../shared/http/run-mutation";
import { IndicatiesLayout } from "../../shared/indicaties/indicaties.component";
import { InformatieObjectIndicatiesComponent } from "../../shared/indicaties/informatie-object-indicaties/informatie-object-indicaties.component";
import {
  findOrigin,
  navigateToBreadcrumb,
} from "../../shared/navigation/navigation-origin";
import { BestandsomvangPipe } from "../../shared/pipes/bestandsomvang.pipe";
import { DatumPipe } from "../../shared/pipes/datum.pipe";
import { EmptyPipe } from "../../shared/pipes/empty.pipe";
import { I18nKeyPipe } from "../../shared/pipes/i18n-key.pipe";
import { I18nLabelPipe } from "../../shared/pipes/i18n-label.pipe";
import { MimetypeToExtensionPipe } from "../../shared/pipes/mimetypeToExtension.pipe";
import { VertrouwelijkaanduidingToTranslationKeyPipe } from "../../shared/pipes/vertrouwelijkaanduiding-to-translation-key.pipe";
import { ReadMoreComponent } from "../../shared/read-more/read-more.component";
import { ButtonMenuItem } from "../../shared/side-nav/menu-item/button-menu-item";
import { HeaderMenuItem } from "../../shared/side-nav/menu-item/header-menu-item";
import { HrefMenuItem } from "../../shared/side-nav/menu-item/href-menu-item";
import { MenuItem } from "../../shared/side-nav/menu-item/menu-item";
import { SideNavComponent } from "../../shared/side-nav/side-nav.component";
import { StaticTextComponent } from "../../shared/static-text/static-text.component";
import { GeneratedType } from "../../shared/utils/generated-types";
import { ZaakIdentificatieComponent } from "../../shared/zaak-identificatie/zaak-identificatie.component";
import { ZakenService } from "../../zaken/zaken.service";
import { DocumentDialogService } from "../document-dialog.service";
import { InformatieObjectEditComponent } from "../informatie-object-edit/informatie-object-edit.component";
import { InformatieObjectenService } from "../informatie-objecten.service";
import { FileFormat, FileFormatUtil } from "../model/file-format";

@Component({
  templateUrl: "./informatie-object-view.component.html",
  styleUrls: ["./informatie-object-view.component.less"],
  standalone: true,
  imports: [
    ZaakIdentificatieComponent,
    MatButtonModule,
    MatCardModule,
    MatDividerModule,
    MatExpansionModule,
    MatIconModule,
    MatSortModule,
    MatSidenavModule,
    MatTableModule,
    TypedMatCellDefDirective,
    MatTabsModule,
    MatTooltipModule,
    RouterModule,
    TranslatePipe,
    DocumentViewerComponent,
    DocumentIconComponent,
    InformatieObjectIndicatiesComponent,
    BestandsomvangPipe,
    DatumPipe,
    EmptyPipe,
    I18nKeyPipe,
    I18nLabelPipe,
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
  private readonly informatieObjectenService = inject(
    InformatieObjectenService,
  );
  private readonly route = inject(ActivatedRoute);
  private readonly utilService = inject(UtilService);
  private readonly websocketService = inject(WebsocketService);
  private readonly router = inject(Router);
  private readonly location = inject(Location);
  private readonly translate = inject(TranslateService);
  private readonly dialog = inject(MatDialog);
  private readonly zakenService = inject(ZakenService);
  private readonly documentDialogService = inject(DocumentDialogService);

  private readonly queryClient = inject(QueryClient);

  private readonly informatieObjectUuid = signal<string | null>(null);
  private readonly laatsteVersieQuery = injectQuery(() => ({
    ...this.informatieObjectenService.readEnkelvoudigInformatieobject(
      this.informatieObjectUuid()!,
    ),
    enabled: !!this.informatieObjectUuid(),
    structuralSharing: false,
  }));
  private readonly zaakInformatieobjectenQuery = injectQuery(() => ({
    ...this.informatieObjectenService.listZaakInformatieobjecten(
      this.informatieObjectUuid()!,
    ),
    enabled: !!this.informatieObjectUuid(),
    structuralSharing: false,
  }));
  private readonly historieQuery = injectQuery(() => ({
    ...this.informatieObjectenService.listHistorie(
      this.informatieObjectUuid()!,
    ),
    enabled: !!this.informatieObjectUuid(),
  }));
  private volgtLaatsteVersie = false;
  private zaakGeladen = false;

  readonly indicatiesLayout = IndicatiesLayout;
  readonly infoObject = signal<
    GeneratedType<"RestEnkelvoudigInformatieobject">
  >(undefined!);
  readonly laatsteVersieInfoObject = signal<
    GeneratedType<"RestEnkelvoudigInformatieobject"> | undefined
  >(undefined);
  readonly zaakInformatieObjecten = signal<
    GeneratedType<"RestZaakInformatieobject">[]
  >([]);
  readonly zaak = signal<GeneratedType<"RestZaak"> | undefined>(undefined);
  readonly documentNieuweVersieGegevens = signal<
    GeneratedType<"RestEnkelvoudigInformatieObjectVersieGegevens"> | undefined
  >(undefined);
  readonly documentPreviewBeschikbaar = signal(false);
  readonly menu = signal<MenuItem[]>([]);
  readonly activeSideAction = signal<string | null>(null);
  readonly versieInformatie = computed<string | null>(() => {
    const laatsteVersie = this.laatsteVersieInfoObject();
    if (!laatsteVersie) return null;

    return this.translate.instant("versie.x.van", {
      versie: this.infoObject()?.versie,
      laatsteVersie: laatsteVersie.versie,
    });
  });
  historie = new MatTableDataSource<GeneratedType<"HistoryLine">>();
  protected readonly historieRegels = computed(
    () => this.historieQuery.data() ?? [],
  );

  historieColumns: string[] = [
    "datum",
    "gebruiker",
    "wijziging",
    "actie",
    "oudeWaarde",
    "nieuweWaarde",
    "toelichting",
  ];

  readonly actionsSidenav = viewChild.required<MatSidenav>("actionsSidenav");
  readonly sideNavContainer =
    viewChild.required<MatSidenavContainer>("sideNavContainer");
  readonly sort = viewChild(MatSort);
  private documentListener?: WebsocketListener;

  constructor() {
    super();

    effect(() => {
      const laatsteVersie = this.laatsteVersieQuery.data();
      if (!laatsteVersie) return;
      untracked(() => this.verwerkLaatsteVersie(laatsteVersie));
    });

    effect(() => {
      const zaakInformatieObjecten = this.zaakInformatieobjectenQuery.data();
      if (!zaakInformatieObjecten) return;
      untracked(() => {
        this.zaakInformatieObjecten.set(zaakInformatieObjecten);
        this.loadZaak();
      });
    });

    effect(() => {
      this.historie.data = this.historieRegels();
    });
  }

  ngOnInit() {
    this.subscriptions$.push(
      this.route.data.subscribe((data) => {
        this.infoObject.set(
          data.informatieObject as GeneratedType<"RestEnkelvoudigInformatieobject">,
        );
        this.volgtLaatsteVersie = false;
        if (this.informatieObjectUuid() !== this.infoObject().uuid) {
          this.laatsteVersieInfoObject.set(undefined);
          this.zaakGeladen = false;
          this.informatieObjectUuid.set(this.infoObject().uuid!);
        }
        if (this.zaakGeladen && this.laatsteVersieInfoObject()) {
          this.toevoegenActies();
        }
        this.documentPreviewBeschikbaar.set(
          FileFormatUtil.isPreviewAvailable(
            this.infoObject().formaat as FileFormat,
          ),
        );

        this.documentListener = this.websocketService.addListener(
          "UPDATED",
          "ENKELVOUDIG_INFORMATIEOBJECT",
          this.infoObject().uuid!,
          () => this.herlaadInformatieObject(),
        );
      }),
    );
  }

  ngAfterViewInit() {
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
    this.historie.sort = this.sort() ?? null;
  }

  ngOnDestroy() {
    this.websocketService.removeListener(this.documentListener);
  }

  private toevoegenActies() {
    const menu: MenuItem[] = [new HeaderMenuItem("informatieobject")];
    const infoObject = this.infoObject();
    const laatsteVersieInfoObject = this.laatsteVersieInfoObject();
    const zaak = this.zaak();

    if (laatsteVersieInfoObject?.rechten?.canLezen) {
      menu.push(
        new HrefMenuItem(
          "actie.downloaden",
          this.informatieObjectenService.getDownloadURL(
            infoObject.uuid!,
            infoObject.versie,
          ),
          "save_alt",
        ),
      );
    }

    if (laatsteVersieInfoObject?.rechten?.canToevoegenNieuweVersie && zaak) {
      menu.push(
        new ButtonMenuItem(
          "actie.nieuwe.versie.toevoegen",
          () => {
            this.queryClient
              .query(
                this.informatieObjectenService.readHuidigeVersieEnkelvoudigInformatieObject(
                  infoObject.uuid!,
                ),
              )
              .then((infoObject) => {
                this.documentNieuweVersieGegevens.set(infoObject);
                void this.actionsSidenav().open();
              })
              .catch(() => undefined);
          },
          "difference",
        ),
      );
    }

    if (
      zaak &&
      laatsteVersieInfoObject?.rechten?.canWijzigen &&
      FileFormatUtil.isOffice(infoObject.formaat as FileFormat)
    ) {
      menu.push(
        new ButtonMenuItem(
          "actie.bewerken",
          () => {
            from(
              this.queryClient.query(
                this.informatieObjectenService.editEnkelvoudigInformatieObjectInhoud(
                  infoObject.uuid!,
                  zaak!.uuid!,
                ),
              ),
            ).subscribe((url) => {
              window.open(url);
            });
          },
          "edit",
        ),
      );
    }

    if (
      !laatsteVersieInfoObject?.gelockedDoor &&
      laatsteVersieInfoObject?.rechten?.canVergrendelen
    ) {
      const button = new ButtonMenuItem(
        "actie.lock",
        () => {
          button.disabled = true;
          runMutation(
            this.queryClient,
            this.informatieObjectenService.lockInformatieObject(
              infoObject.uuid!,
              zaak!.uuid!,
            ),
            undefined as never,
          ).subscribe({
            next: () => this.herlaadInformatieObject(),
            // we only need to do this on error, because on success we get a new button
            error: () => {
              button.disabled = false;
            },
          });
        },
        "lock",
      );
      menu.push(button);
    }

    if (
      laatsteVersieInfoObject?.gelockedDoor &&
      laatsteVersieInfoObject?.rechten?.canOntgrendelen
    ) {
      const button = new ButtonMenuItem(
        "actie.unlock",
        () => {
          button.disabled = true;
          runMutation(
            this.queryClient,
            this.informatieObjectenService.unlockInformatieObject(
              infoObject.uuid!,
              zaak?.uuid,
            ),
            undefined as never,
          ).subscribe({
            next: () => this.herlaadInformatieObject(),
            // we only need to do this on error, because on success we get a new button
            error: () => {
              button.disabled = false;
            },
          });
        },
        "lock_open",
      );
      menu.push(button);
    }

    if (
      laatsteVersieInfoObject?.rechten?.canVerwijderen &&
      !laatsteVersieInfoObject?.isBesluitDocument
    ) {
      menu.push(
        new ButtonMenuItem(
          "actie.verwijderen",
          () => this.openDocumentVerwijderenDialog(),
          "delete",
        ),
      );
    }

    if (
      !laatsteVersieInfoObject?.ondertekening &&
      laatsteVersieInfoObject?.rechten?.canOndertekenen
    ) {
      menu.push(
        new ButtonMenuItem(
          "actie.ondertekenen",
          () => this.openDocumentOndertekenenDialog(),
          "fact_check",
        ),
      );
    }

    if (
      zaak &&
      laatsteVersieInfoObject?.rechten?.canConverteren &&
      FileFormatUtil.isOffice(infoObject.formaat as FileFormat)
    ) {
      menu.push(
        new AsyncButtonMenuItem(
          "actie.converteren",
          () =>
            runMutation(
              this.queryClient,
              this.informatieObjectenService.convertInformatieObjectToPDF(
                infoObject.uuid!,
                zaak!.uuid!,
              ),
              undefined as never,
            ).pipe(map(() => undefined)),
          "picture_as_pdf",
        ),
      );
    }
    this.menu.set(menu);
  }

  private herlaadInformatieObject() {
    this.volgtLaatsteVersie = true;
    void this.informatieObjectenService.invalidateInformatieobject(
      this.infoObject().uuid!,
    );
  }

  private verwerkLaatsteVersie(
    laatsteVersie: GeneratedType<"RestEnkelvoudigInformatieobject">,
  ) {
    this.laatsteVersieInfoObject.set(laatsteVersie);
    if (this.volgtLaatsteVersie) {
      this.infoObject.set(laatsteVersie);
      this.documentPreviewBeschikbaar.set(
        FileFormatUtil.isPreviewAvailable(laatsteVersie.formaat as FileFormat),
      );
    }
    if (this.zaakGeladen) this.toevoegenActies();
  }

  protected haalVersieOp(versie: number) {
    this.websocketService.removeListener(this.documentListener);
    this.router.navigate([
      "/informatie-objecten",
      this.infoObject().uuid,
      versie,
    ]);
  }

  protected versieToegevoegd(
    informatieobject: GeneratedType<"RestEnkelvoudigInformatieobject">,
  ) {
    if (!informatieobject.versie) return;
    this.haalVersieOp(informatieobject.versie);
  }

  private openDocumentVerwijderenDialog() {
    this.documentDialogService
      .openVerwijderDocument({
        hasZaak: Boolean(this.zaak()),
        documentTitel: this.infoObject()?.titel,
        delete: (reden) => this.deleteEnkelvoudigInformatieObject$(reden),
      })
      .afterClosed()
      .subscribe((result) => {
        this.activeSideAction.set(null);

        if (result) {
          const zaak = this.zaak();
          this.utilService.openSnackbar("msg.document.verwijderen.uitgevoerd", {
            document: this.infoObject().titel,
          });
          const origin = findOrigin(this.location);
          void (origin?.key
            ? navigateToBreadcrumb(origin, this.router)
            : this.router.navigateByUrl(
                zaak
                  ? `/zaken/${zaak.identificatie}`
                  : "/documenten/ontkoppelde",
              ));
        }
      });
  }

  private openDocumentOndertekenenDialog() {
    const dialogData = new ConfirmDialogData(
      {
        key: "msg.document.ondertekenen.bevestigen",
        args: { document: this.infoObject().titel },
      },
      runMutation(
        this.queryClient,
        this.informatieObjectenService.ondertekenInformatieObject(
          this.infoObject().uuid!,
          this.zaak()!.uuid!,
        ),
        undefined as never,
      ),
    );

    this.dialog.open(ConfirmDialogComponent, { data: dialogData });
  }

  private deleteEnkelvoudigInformatieObject$(reden?: string): Observable<void> {
    const uuid = this.infoObject()?.uuid;
    if (!uuid) return of();
    return runMutation(
      this.queryClient,
      this.informatieObjectenService.deleteEnkelvoudigInformatieObject(),
      { uuid, zaakUuid: this.zaak()?.uuid, reden },
    ).pipe(
      tap(() => this.websocketService.suspendListener(this.documentListener)),
    );
  }

  /**
   * De zaak van het document wordt via de gekoppelde zaakinformatieobjecten opgehaald,
   * zodat de knoppen in het linkermenu pas gebouwd worden nadat de zaak (indien aanwezig) bekend is.
   *
   * Als er geen gekoppelde zaak is, is dit een document zonder zaak.
   */
  private loadZaak() {
    const zaakobject = this.zaakInformatieObjecten().at(0);
    if (zaakobject?.zaakIdentificatie) {
      this.zakenService
        .readZaakByID(zaakobject.zaakIdentificatie)
        .subscribe((zaak) => {
          this.zaak.set(zaak);
          this.zaakGeladen = true;
          if (this.laatsteVersieInfoObject()) this.toevoegenActies();
        });
    } else {
      this.zaak.set(undefined);
      this.zaakGeladen = true;
      if (this.laatsteVersieInfoObject()) this.toevoegenActies();
    }
  }
}
