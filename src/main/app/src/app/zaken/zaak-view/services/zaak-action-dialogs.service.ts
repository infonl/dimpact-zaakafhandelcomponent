/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { ComponentType } from "@angular/cdk/portal";
import { inject, Injectable, signal } from "@angular/core";
import { MatDialog } from "@angular/material/dialog";
import { QueryClient } from "@tanstack/angular-query-experimental";
import moment from "moment";
import { Observable } from "rxjs";
import { ActieOnmogelijkDialogComponent } from "src/app/fout-afhandeling/dialog/actie-onmogelijk-dialog.component";
import { ZaakafhandelParametersService } from "../../../admin/zaakafhandel-parameters.service";
import { UtilService } from "../../../core/service/util.service";
import { runMutation } from "../../../shared/http/run-mutation";
import { GeneratedType } from "../../../shared/utils/generated-types";
import { toI18nKey } from "../../../shared/utils/i18n-key";
import { TakenService } from "../../../taken/taken.service";
import { IntakeAfrondenDialogComponent } from "../../intake-afronden-dialog/intake-afronden-dialog.component";
import { ZaakAfhandelenDialogComponent } from "../../zaak-afhandelen-dialog/zaak-afhandelen-dialog.component";
import { ZaakBrondatumZettenDialogComponent } from "../../zaak-brondatum-zetten-dialog/zaak-brondatum-zetten-dialog.component";
import { ZaakDialogService } from "../../zaak-dialog.service";
import { ZaakOntkoppelenDialogComponent } from "../../zaak-ontkoppelen/zaak-ontkoppelen-dialog.component";
import { ZaakOpschortenDialogComponent } from "../../zaak-opschorten-dialog/zaak-opschorten-dialog.component";
import { ZaakVerlengenDialogComponent } from "../../zaak-verlengen-dialog/zaak-verlengen-dialog.component";
import { ZakenService } from "../../zaken.service";
import { ZaakSideActionService } from "./zaak-side-action.service";

type Zaak = GeneratedType<"RestZaak">;
type PlanItem = GeneratedType<"RestPlanItem">;

/**
 * Opens the dialogs that act on the zaak as a whole and applies their outcome:
 * refresh the zaak, refresh its taken and tell the user what happened.
 */
@Injectable()
export class ZaakActionDialogsService {
  private readonly dialog = inject(MatDialog);
  private readonly queryClient = inject(QueryClient);
  private readonly takenService = inject(TakenService);
  private readonly utilService = inject(UtilService);
  private readonly zaakDialogService = inject(ZaakDialogService);
  private readonly zaakafhandelParametersService = inject(
    ZaakafhandelParametersService,
  );
  private readonly zakenService = inject(ZakenService);
  private readonly sideActions = inject(ZaakSideActionService);

  /**
   * The opschorting details do not travel with the zaak, so they are fetched
   * separately. Kept here because the hervatten dialog is their only writer.
   */
  readonly opschorting = signal<
    GeneratedType<"RestZaakOpschorting"> | undefined
  >(undefined);

  loadOpschorting(zaak: Zaak) {
    if (!zaak.isOpgeschort) {
      this.opschorting.set(undefined);
      return;
    }

    this.zakenService
      .readOpschortingZaak(zaak.uuid)
      .subscribe((opschorting) => this.opschorting.set(opschorting));
  }

  private onClosed<T>(
    closed: Observable<T>,
    handle: (result: T) => void = () => {},
  ) {
    closed.subscribe((result) => {
      this.sideActions.clear();
      handle(result);
    });
  }

  openPlanItemStarten(zaak: Zaak, planItem: PlanItem) {
    this.sideActions.close();
    const { dialogComponent, dialogData } = this.userEventListenerDialog(
      zaak,
      planItem,
    );

    this.onClosed(
      this.dialog.open(dialogComponent, { data: dialogData }).afterClosed(),
      (result) => {
        if (!result) return;

        if (result === "openBesluitVastleggen") {
          this.sideActions.open("actie.besluit.vastleggen");
          return;
        }

        this.utilService.openSnackbar(
          toI18nKey(
            `msg.planitem.uitgevoerd.${planItem.userEventListenerActie}`,
          ),
        );
        this.zakenService.invalidateZaak(zaak.uuid);
      },
    );
  }

  private userEventListenerDialog(
    zaak: Zaak,
    planItem: PlanItem,
  ): {
    dialogComponent: ComponentType<unknown>;
    dialogData: { zaak: Zaak; planItem: PlanItem };
  } {
    const dialogData = { zaak, planItem };

    switch (planItem.userEventListenerActie) {
      case "INTAKE_AFRONDEN":
        return { dialogComponent: IntakeAfrondenDialogComponent, dialogData };
      case "ZAAK_AFHANDELEN":
        return {
          dialogComponent: zaak.isOpgeschort
            ? ActieOnmogelijkDialogComponent
            : ZaakAfhandelenDialogComponent,
          dialogData,
        };
      default:
        throw new Error(
          `Niet bestaande UserEventListenerActie: ${planItem.userEventListenerActie}`,
        );
    }
  }

  openAfbreken(zaak: Zaak) {
    this.sideActions.close();

    if (zaak.isOpgeschort) {
      this.dialog.open(ActieOnmogelijkDialogComponent);
      return;
    }

    this.onClosed(
      this.zaakDialogService
        .openAfbreken(
          this.zaakafhandelParametersService.listZaakbeeindigRedenenForZaaktype(
            zaak.zaaktype.uuid,
          ),
          (reden) =>
            runMutation(
              this.queryClient,
              this.zakenService.afbreken(zaak.uuid),
              { zaakbeeindigRedenId: reden.id! },
            ),
        )
        .afterClosed(),
      (result) => {
        if (!result) return;
        void this.takenService.invalidateTakenVoorZaak(zaak.uuid);
      },
    );
  }

  openHeropenen(zaak: Zaak) {
    this.onClosed(
      this.zaakDialogService
        .openHeropenen((reden) =>
          runMutation(
            this.queryClient,
            this.zakenService.heropenen(zaak.uuid),
            {
              reden,
            },
          ),
        )
        .afterClosed(),
      (result) => {
        if (!result) return;
        void this.takenService.invalidateTakenVoorZaak(zaak.uuid);
      },
    );
  }

  openAfsluiten(zaak: Zaak) {
    this.sideActions.close();

    this.onClosed(
      this.dialog
        .open(ZaakAfhandelenDialogComponent, { data: { zaak } })
        .afterClosed(),
      (result) => {
        if (!result) return;
        this.zakenService.invalidateZaak(zaak.uuid);
        void this.takenService.invalidateTakenVoorZaak(zaak.uuid);
        this.utilService.openSnackbar("msg.zaak.afgesloten");
      },
    );
  }

  openBrondatumZetten(zaak: Zaak) {
    this.sideActions.close();

    this.onClosed(
      this.dialog
        .open(ZaakBrondatumZettenDialogComponent, { data: { zaak } })
        .afterClosed(),
      (result) => {
        if (!result) return;
        this.zakenService.invalidateZaak(zaak.uuid);
        void this.takenService.invalidateTakenVoorZaak(zaak.uuid);
        this.utilService.openSnackbar("msg.zaak.brondatum.gezet");
      },
    );
  }

  openOpschorten(zaak: Zaak) {
    this.sideActions.close();

    this.onClosed(
      this.dialog
        .open(ZaakOpschortenDialogComponent, { data: { zaak } })
        .afterClosed(),
    );
  }

  openVerlengen(zaak: Zaak) {
    this.sideActions.close();

    this.onClosed(
      this.dialog
        .open(ZaakVerlengenDialogComponent, { data: { zaak } })
        .afterClosed(),
      (result) => {
        if (!result) return;
        this.zakenService.cacheZaak(result);
        this.utilService.openSnackbar("msg.zaak.verlengd");
      },
    );
  }

  openHervatten(zaak: Zaak) {
    this.sideActions.close();

    const opschorting = this.opschorting();
    const werkelijkeOpschortDuur = moment().diff(
      moment(opschorting?.vanafDatumTijd),
      "days",
    );

    this.onClosed(
      this.zaakDialogService
        .openHervatten(
          {
            duur: werkelijkeOpschortDuur,
            verwachteDuur: opschorting?.duurDagen,
          },
          (reden) =>
            runMutation(
              this.queryClient,
              this.zakenService.resumeZaak(zaak.uuid),
              { reason: reden },
            ),
        )
        .afterClosed(),
      (result) => {
        if (!result) return;
        this.opschorting.set(undefined);
      },
    );
  }

  openZaakOntkoppelen(
    zaak: Zaak,
    gerelateerdeZaak: GeneratedType<"RestGerelateerdeZaak">,
  ) {
    this.onClosed(
      this.dialog
        .open(ZaakOntkoppelenDialogComponent, {
          data: {
            zaakUuid: zaak.uuid,
            gekoppeldeZaakIdentificatie: gerelateerdeZaak.identificatie,
            relatieType: gerelateerdeZaak.relatieType,
          },
        })
        .afterClosed(),
    );
  }
}
