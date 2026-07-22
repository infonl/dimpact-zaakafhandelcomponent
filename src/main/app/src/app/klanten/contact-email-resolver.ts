/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { inject, Injectable } from "@angular/core";
import { catchError, map, Observable, of } from "rxjs";
import { GeneratedType } from "../shared/utils/generated-types";
import { KlantenService } from "./klanten.service";

@Injectable({ providedIn: "root" })
export class ContactEmailResolver {
  private readonly klantenService = inject(KlantenService);

  /**
   * Resolves the contact e-mail address for a zaak. The zaak-specific contact
   * e-mail address takes precedence over the initiator's e-mail address.
   */
  resolve(zaak: GeneratedType<"RestZaak">): Observable<string | null> {
    const emailAddress = zaak.zaakSpecificContactDetails?.emailAddress;
    if (emailAddress) return of(emailAddress);

    const temporaryPersonId = zaak.initiatorIdentificatie?.temporaryPersonId;
    if (!temporaryPersonId) return of(null);

    return this.klantenService
      .getContactDetailsForPerson(temporaryPersonId)
      .pipe(
        map((contactDetails) => contactDetails?.emailadres ?? null),
        catchError((error) => {
          console.error("Failed to resolve contact email address", error);
          return of(null);
        }),
      );
  }
}
