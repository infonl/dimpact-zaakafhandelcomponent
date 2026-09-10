/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { inject, Injectable } from "@angular/core";
import { QueryClient } from "@tanstack/angular-query-experimental";
import { UtilService } from "../../../core/service/util.service";
import { KlantGegevens } from "../../../klanten/model/klanten/klant-gegevens";
import { runMutation } from "../../../shared/http/run-mutation";
import { GeneratedType } from "../../../shared/utils/generated-types";
import { BetrokkeneIdentificatie } from "../../model/betrokkeneIdentificatie";
import { ZaakDialogService } from "../../zaak-dialog.service";
import { ZakenService } from "../../zaken.service";
import { ZaakSideActionService } from "./zaak-side-action.service";

type Zaak = GeneratedType<"RestZaak">;

@Injectable()
export class ZaakBetrokkenenService {
  private readonly queryClient = inject(QueryClient);
  private readonly utilService = inject(UtilService);
  private readonly zaakDialogService = inject(ZaakDialogService);
  private readonly zakenService = inject(ZakenService);
  private readonly sideActions = inject(ZaakSideActionService);

  initiatorGeselecteerd(zaak: Zaak, initiator: GeneratedType<"RestPersoon">) {
    this.sideActions.close();

    if (zaak.initiatorIdentificatie) {
      this.zaakDialogService
        .openWijzigInitiator(initiator.naam, (reden) =>
          runMutation(this.queryClient, this.zakenService.updateInitiator(), {
            zaakUUID: zaak.uuid,
            betrokkeneIdentificatie: new BetrokkeneIdentificatie(initiator),
            toelichting: reden,
          }),
        )
        .afterClosed()
        .subscribe((updatedZaak) =>
          this.reportNewInitiator("msg.initiator.gewijzigd", updatedZaak),
        );
      return;
    }

    runMutation(this.queryClient, this.zakenService.updateInitiator(), {
      zaakUUID: zaak.uuid,
      betrokkeneIdentificatie: new BetrokkeneIdentificatie(initiator),
    }).subscribe({
      next: (updatedZaak) =>
        this.reportNewInitiator("msg.initiator.gekoppeld", updatedZaak),
      // the mutation already reported the failure; this keeps it from being
      // raised a second time as an unhandled error
      error: () => undefined,
    });
  }

  private reportNewInitiator(notification: string, updatedZaak?: Zaak) {
    if (!updatedZaak) return;

    const naam = [
      updatedZaak.initiatorIdentificatie?.kvkNummer,
      updatedZaak.initiatorIdentificatie?.vestigingsnummer,
    ].filter(Boolean);
    this.utilService.openSnackbar(notification, {
      naam: naam.join(" - "),
    });
  }

  deleteInitiator(zaak: Zaak) {
    this.zaakDialogService
      .openOntkoppelInitiator((reden) =>
        runMutation(this.queryClient, this.zakenService.deleteInitiator(), {
          zaakUuid: zaak.uuid,
          reden,
        }),
      )
      .afterClosed()
      .subscribe((result) => {
        this.sideActions.clear();
        if (!result) return;

        this.utilService.openSnackbar("msg.initiator.ontkoppelen.uitgevoerd");
      });
  }

  betrokkeneGeselecteerd(zaak: Zaak, klantgegevens: KlantGegevens) {
    this.sideActions.close();
    runMutation(this.queryClient, this.zakenService.createBetrokkene(), {
      zaakUUID: zaak.uuid,
      roltypeUUID: klantgegevens.betrokkeneRoltype.uuid!,
      roltoelichting: klantgegevens.betrokkeneToelichting,
      betrokkeneIdentificatie: new BetrokkeneIdentificatie(klantgegevens.klant),
    }).subscribe({
      next: () => {
        this.utilService.openSnackbar("msg.betrokkene.gekoppeld", {
          roltype: klantgegevens.betrokkeneRoltype.naam,
        });
        this.invalidateBetrokkenen(zaak);
      },
      // the mutation already reported the failure; this keeps it from being
      // raised a second time as an unhandled error
      error: () => undefined,
    });
  }

  invalidateBetrokkenen(zaak: Zaak) {
    this.queryClient.invalidateQueries({
      queryKey: this.zakenService.listBetrokkenenVoorZaakQuery(zaak.uuid)
        .queryKey,
    });
  }
}
