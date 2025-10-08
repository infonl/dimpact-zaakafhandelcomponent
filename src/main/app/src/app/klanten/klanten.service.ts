/*
 * SPDX-FileCopyrightText: 2022 Atos, 2024 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { inject, Injectable } from "@angular/core";
import { PutBody } from "../shared/http/http-client";
import { ZacHttpClient } from "../shared/http/zac-http-client";
import { ZacQueryClient } from "../shared/http/zac-query-client";
import { BetrokkeneIdentificatie } from "../zaken/model/betrokkeneIdentificatie";

@Injectable({
  providedIn: "root",
})
export class KlantenService {
  private readonly zacHttpClient = inject(ZacHttpClient);
  private readonly zacQueryClient = inject(ZacQueryClient);

  /* istanbul ignore next */
  readPersoon(bsn: string, zaakIdentification?: string) {
    return this.zacQueryClient.GET("/rest/klanten/persoon/{bsn}", {
      path: { bsn },
      ...(zaakIdentification && {
        header: { "X-ZAAK-ID": zaakIdentification },
      }),
    });
  }

  readBedrijf(betrokkeneIdentificatie: BetrokkeneIdentificatie) {
    switch (betrokkeneIdentificatie.type) {
      case "VN":
        return this.readVestiging(
          betrokkeneIdentificatie.vestigingsnummer,
          betrokkeneIdentificatie.kvkNummer,
        );
      case "RSIN":
        return this.readRechtspersoon(
          betrokkeneIdentificatie.kvkNummer,
          betrokkeneIdentificatie.rsin,
        );
      case "BSN":
      default:
        throw new Error(
          `${KlantenService.name}: Unsupported identificatie type ${betrokkeneIdentificatie.type}`,
        );
    }
  }

  /* istanbul ignore next */
  private readRechtspersoon(kvkNummer?: string | null, rsin?: string | null) {
    if (kvkNummer) {
      return this.zacQueryClient.GET(
        "/rest/klanten/rechtspersoon/kvknummer/{kvkNummer}",
        {
          path: { kvkNummer },
        },
      );
    }

    if (!rsin) {
      throw new Error("Rsin is required for rechtspersoon lookup.");
    }

    // legacy solution
    return this.zacQueryClient.GET("/rest/klanten/rechtspersoon/rsin/{rsin}", {
      path: { rsin },
    });
  }

  /* istanbul ignore next */
  private readVestiging(
    vestigingsnummer?: string | null,
    kvkNummer?: string | null,
  ) {
    if (kvkNummer && vestigingsnummer) {
      return this.zacQueryClient.GET(
        "/rest/klanten/vestiging/{vestigingsnummer}/{kvkNummer}",
        {
          path: { vestigingsnummer, kvkNummer },
        },
      );
    }

    if (!vestigingsnummer) {
      throw new Error("Vestigingsnummer is required for vestiging lookup.");
    }

    // legacy solution
    return this.zacQueryClient.GET(
      "/rest/klanten/vestiging/{vestigingsnummer}",
      {
        path: { vestigingsnummer },
      },
    );
  }

  /* istanbul ignore next */
  readVestigingsprofiel(vestigingsnummer: string) {
    return this.zacHttpClient.GET(
      "/rest/klanten/vestigingsprofiel/{vestigingsnummer}",
      {
        path: { vestigingsnummer },
      },
    );
  }

  /* istanbul ignore next */
  getPersonenParameters() {
    return this.zacHttpClient.GET("/rest/klanten/personen/parameters");
  }

  /* istanbul ignore next */
  listPersonen(body: PutBody<"/rest/klanten/personen">) {
    return this.zacHttpClient.PUT("/rest/klanten/personen", body);
  }

  /* istanbul ignore next */
  listBedrijven(body: PutBody<"/rest/klanten/bedrijven">) {
    return this.zacHttpClient.PUT("/rest/klanten/bedrijven", body);
  }

  /* istanbul ignore next */
  listBetrokkeneRoltypen(zaaktypeUuid: string) {
    return this.zacHttpClient.GET(
      "/rest/klanten/roltype/{zaaktypeUuid}/betrokkene",
      {
        path: { zaaktypeUuid },
      },
    );
  }

  /* istanbul ignore next */
  listRoltypen() {
    return this.zacHttpClient.GET("/rest/klanten/roltype");
  }

  /* istanbul ignore next */
  ophalenContactGegevens(initiatorIdentificatie: string) {
    return this.zacHttpClient.GET(
      "/rest/klanten/contactgegevens/{initiatorIdentificatie}",
      {
        path: { initiatorIdentificatie },
      },
    );
  }
}
