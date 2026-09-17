/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { GeneratedType } from "../../../shared/utils/generated-types";

export type InitiatorViewType =
  | "PERSON"
  | "COMPANY"
  | "CONTACT_DETAILS"
  | "ADD";

type Zaak = GeneratedType<"RestZaak">;

function betrokkeneKoppelingen(zaak: Zaak) {
  return zaak.zaaktype.zaakafhandelparameters?.betrokkeneKoppelingen;
}

export function hasZaakData(zaak: Zaak) {
  return Boolean(zaak.zaakdata && Object.keys(zaak.zaakdata).length > 0);
}

export function hasAfleidingswijzeBrondatumEigenschap(zaak: Zaak) {
  // The value returned from the backend is lowercase where the generated TypeScript types expect uppercase.
  const afleidingswijze =
    zaak.resultaat?.resultaattype?.bronArchiefprocedure?.afleidingswijze;
  return afleidingswijze?.toUpperCase() === "EIGENSCHAP";
}

export function hasZaakSpecificContactDetails(zaak: Zaak) {
  const { zaakSpecificContactDetails } = zaak;
  return Boolean(
    zaakSpecificContactDetails?.telephoneNumber ||
      zaakSpecificContactDetails?.emailAddress,
  );
}

export function showInitiator(zaak: Zaak) {
  if (hasZaakSpecificContactDetails(zaak)) return true;

  const koppelingen = betrokkeneKoppelingen(zaak);
  if (!koppelingen) return false;

  return Boolean(koppelingen.brpKoppelen || koppelingen.kvkKoppelen);
}

export function initiatorViewType(zaak: Zaak): InitiatorViewType {
  const koppelingen = betrokkeneKoppelingen(zaak);

  if (koppelingen) {
    const type = zaak.initiatorIdentificatie?.type ?? "";
    if (koppelingen.brpKoppelen && ["BSN"].includes(type)) return "PERSON";
    if (koppelingen.kvkKoppelen && ["VN", "RSIN"].includes(type))
      return "COMPANY";
  }

  if (hasZaakSpecificContactDetails(zaak)) return "CONTACT_DETAILS";

  return "ADD";
}

export function allowBedrijf(zaak: Zaak) {
  return Boolean(
    zaak.rechten.toevoegenInitiatorBedrijf &&
      betrokkeneKoppelingen(zaak)?.kvkKoppelen,
  );
}

export function allowPersoon(zaak: Zaak, hasBrpSearchRight: boolean) {
  return Boolean(
    zaak.rechten.toevoegenInitiatorPersoon &&
      betrokkeneKoppelingen(zaak)?.brpKoppelen &&
      hasBrpSearchRight,
  );
}

export function allowedToAddBetrokkene(zaak: Zaak, hasBrpSearchRight: boolean) {
  const koppelingen = betrokkeneKoppelingen(zaak);
  const brpAllowed =
    Boolean(koppelingen?.brpKoppelen) && zaak.rechten.toevoegenInitiatorPersoon;
  const kvkAllowed =
    Boolean(koppelingen?.kvkKoppelen) && zaak.rechten.toevoegenInitiatorBedrijf;

  return Boolean((brpAllowed && hasBrpSearchRight) || kvkAllowed);
}

export function showBetrokkeneKoppelingen(
  zaak: Zaak,
  betrokkenenCount: number,
) {
  const koppelingen = betrokkeneKoppelingen(zaak);

  return Boolean(
    (koppelingen?.brpKoppelen || koppelingen?.kvkKoppelen) && betrokkenenCount,
  );
}
