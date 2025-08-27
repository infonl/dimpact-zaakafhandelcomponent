/*
 * SPDX-FileCopyrightText: 2022 Atos, 2024 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { Injectable } from "@angular/core";
import { PutBody, ZacHttpClient } from "../shared/http/zac-http-client";
import { GeneratedType } from "../shared/utils/generated-types";
import { Observable, of } from "rxjs";

@Injectable({
  providedIn: "root",
})
export class KlantenService {
  constructor(private readonly zacHttpClient: ZacHttpClient) {}

  /* istanbul ignore next */
  readPersoon(bsn: string, audit: { context: string; action: string }) {
    return this.zacHttpClient.GET("/rest/klanten/persoon/{bsn}", {
      path: { bsn },
      header: {
        "X-Verwerking": `${audit.context}@${audit.action}`,
      },
    });
  }

  readBedrijf(
    betrokkeneIdentificatie: GeneratedType<"BetrokkeneIdentificatie">,
  ): Observable<unknown> {
    switch (betrokkeneIdentificatie.type) {
      case "VN":
        return this.readVestiging(
          betrokkeneIdentificatie.kvkNummer ?? null,
          betrokkeneIdentificatie.vestigingsnummer ?? "",
        );
      case "RSIN":
        return this.readRechtspersoon(
          betrokkeneIdentificatie.kvkNummer ?? null,
          betrokkeneIdentificatie.rsinNummer ?? null,
        );
      case "BSN":
      default:
        return of(null);
    }
  }

  /* istanbul ignore next */
  private readRechtspersoon(
    kvkNummer: string | null,
    rsinNummer: string | null,
  ) {
    if (kvkNummer) {
      // to be built in backend
      of(null);
    }

    if (!rsinNummer) {
      return of(null).pipe(() => {
        throw new Error("rsinNummer is required");
      });
    }

    // legacy solution
    return this.zacHttpClient.GET("/rest/klanten/rechtspersoon/rsin/{rsin}", {
      path: { rsin: rsinNummer ?? "823807071" },
    });
  }

  /* istanbul ignore next */
  private readVestiging(kvkNummer: string | null, vestigingsnummer: string) {
    if (kvkNummer && vestigingsnummer) {
      return this.zacHttpClient.GET(
        "/rest/klanten/vestiging/{vestigingsnummer}/{kvkNummer}",
        {
          path: { vestigingsnummer, kvkNummer },
        },
      );
    }

    // legacy solution
    return this.zacHttpClient.GET(
      "/rest/klanten/vestiging/{vestigingsnummer}",
      { path: { vestigingsnummer } },
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
  listPersonen(
    body: PutBody<"/rest/klanten/personen">,
    audit: { context: string; action: string },
  ) {
    return this.zacHttpClient.PUT("/rest/klanten/personen", body, {
      header: {
        "X-Verwerking": `${audit.context}@${audit.action}`,
      },
    });
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
