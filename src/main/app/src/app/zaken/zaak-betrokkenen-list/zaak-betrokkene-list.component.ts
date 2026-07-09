/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { Component, effect, inject, input } from "@angular/core";
import { Validators } from "@angular/forms";
import { MatButtonModule } from "@angular/material/button";
import { MatDialog } from "@angular/material/dialog";
import { MatIconModule } from "@angular/material/icon";
import { MatTableDataSource, MatTableModule } from "@angular/material/table";
import { MatTooltipModule } from "@angular/material/tooltip";
import { TranslateModule, TranslateService } from "@ngx-translate/core";
import { injectQuery, QueryClient } from "@tanstack/angular-query-experimental";
import { UtilService } from "../../core/service/util.service";
import { WebsocketListener } from "../../core/websocket/model/websocket-listener";
import { WebsocketService } from "../../core/websocket/websocket.service";
import { KlantenService } from "../../klanten/klanten.service";
import { DialogData } from "../../shared/dialog/dialog-data";
import { DialogComponent } from "../../shared/dialog/dialog.component";
import { TextareaFormFieldBuilder } from "../../shared/material-form-builder/form-components/textarea/textarea-form-field-builder";
import { DatumPipe } from "../../shared/pipes/datum.pipe";
import { ReadMoreComponent } from "../../shared/read-more/read-more.component";
import { GeneratedType } from "../../shared/utils/generated-types";
import { BetrokkeneIdentificatie } from "../model/betrokkeneIdentificatie";
import { BetrokkeneLinkComponent } from "../zaak-betrokkenen/betrokkene-link.component";
import { ZakenService } from "../zaken.service";

@Component({
  selector: "zac-zaak-betrokkene-list",
  templateUrl: "./zaak-betrokkene-list.component.html",
  styleUrls: ["./zaak-betrokkene-list.component.less"],
  standalone: true,
  imports: [
    MatButtonModule,
    MatIconModule,
    MatTableModule,
    MatTooltipModule,
    TranslateModule,
    ReadMoreComponent,
    BetrokkeneLinkComponent,
  ],
})
export class ZaakBetrokkeneListComponent {
  private readonly zakenService = inject(ZakenService);
  private readonly klantenService = inject(KlantenService);
  private readonly websocketService = inject(WebsocketService);
  private readonly dialog = inject(MatDialog);
  private readonly translate = inject(TranslateService);
  private readonly utilService = inject(UtilService);
  private readonly queryClient = inject(QueryClient);
  private readonly datumPipe = new DatumPipe("nl");

  readonly zaak = input.required<GeneratedType<"RestZaak">>();
  readonly zaakRollenListener = input.required<WebsocketListener>();

  protected readonly betrokkenenQuery = injectQuery(() =>
    this.zakenService.listBetrokkenenVoorZaakQuery(this.zaak().uuid),
  );

  protected readonly betrokkenen = new MatTableDataSource<
    GeneratedType<"RestZaakBetrokkene">
  >();

  protected readonly betrokkenenColumns = [
    "roltype",
    "betrokkenegegevens",
    "betrokkeneidentificatie",
    "roltoelichting",
    "actions",
  ] as const;

  constructor() {
    effect(() => {
      this.betrokkenen.data = this.betrokkenenQuery.data() ?? [];
    });
  }

  protected async betrokkeneGegevensOphalen(
    betrokkene: GeneratedType<"RestZaakBetrokkene"> & {
      gegevens?: string | null;
    },
  ) {
    betrokkene["gegevens"] = "LOADING";
    switch (betrokkene.type) {
      case "NATUURLIJK_PERSOON": {
        const persoon = await this.queryClient.ensureQueryData(
          this.klantenService.readPersoon(
            betrokkene.temporaryPersonId!,
            this.zaak().zaaktype.uuid,
          ),
        );
        betrokkene["gegevens"] = persoon.naam;
        if (persoon.geboortedatum) {
          betrokkene["gegevens"] += `, ${this.datumPipe.transform(
            persoon.geboortedatum,
          )}`;
        }
        if (persoon.verblijfplaats)
          betrokkene["gegevens"] += `,\n${persoon.verblijfplaats}`;
        break;
      }
      case "NIET_NATUURLIJK_PERSOON":
      case "VESTIGING": {
        const betrokkeneIdentificatie = new BetrokkeneIdentificatie(betrokkene);

        const bedrijf = await this.queryClient.ensureQueryData(
          this.klantenService.readBedrijf(betrokkeneIdentificatie),
        );

        if (!bedrijf) return;

        betrokkene["gegevens"] = bedrijf.naam;
        if (bedrijf.adres?.volledigAdres)
          betrokkene["gegevens"] += `,\n${bedrijf.adres.volledigAdres}`;
        break;
      }
      case "ORGANISATORISCHE_EENHEID":
      case "MEDEWERKER": {
        betrokkene["gegevens"] = "-";
        break;
      }
    }
  }

  protected deleteBetrokkene(betrokkene: GeneratedType<"RestZaakBetrokkene">) {
    this.websocketService.suspendListener(this.zaakRollenListener());
    const betrokkeneIdentificatie: string =
      betrokkene.roltype +
      " " +
      (betrokkene.vestigingsnummer ??
        betrokkene.kvkNummer ??
        betrokkene.bsn ??
        betrokkene.naam);
    this.dialog
      .open(DialogComponent, {
        data: new DialogData<unknown, { reden: string }>({
          formFields: [
            new TextareaFormFieldBuilder()
              .id("reden")
              .label("reden")
              .validators(Validators.required)
              .build(),
          ],
          callback: ({ reden }) =>
            this.zakenService.deleteBetrokkene(betrokkene.rolid, reden),
          melding: this.translate.instant(
            "msg.betrokkene.ontkoppelen.bevestigen",
            {
              betrokkene: betrokkeneIdentificatie,
            },
          ),
          confirmButtonActionKey: "actie.betrokkene.ontkoppelen",
          icon: "link_off",
        }),
      })
      .afterClosed()
      .subscribe((result) => {
        if (result) {
          this.utilService.openSnackbar(
            "msg.betrokkene.ontkoppelen.uitgevoerd",
            { betrokkene: betrokkeneIdentificatie },
          );
          this.queryClient.invalidateQueries({
            queryKey: this.zakenService.listBetrokkenenVoorZaakQuery(
              this.zaak().uuid,
            ).queryKey,
          });
        }
      });
  }
}
