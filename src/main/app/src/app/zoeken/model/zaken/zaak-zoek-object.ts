/*
 * SPDX-FileCopyrightText: 2022 Atos, 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { GeneratedType } from "../../../shared/utils/generated-types";

/**
 * @deprecated - use the `GeneratedType`
 */
export type ZaakZoekObject =
  GeneratedType<"AbstractRestZoekObjectExtendsAbstractRestZoekObject"> & {
    id: string;
    type: GeneratedType<"ZoekObjectType">;
    identificatie: string;
    omschrijving: string;
    toelichting: string;
    registratiedatum: string;
    startdatum: string;
    einddatumGepland: string;
    einddatum: string;
    uiterlijkeEinddatumAfdoening: string;
    publicatiedatum: string;
    communicatiekanaal: string;
    vertrouwelijkheidaanduiding: string;
    archiefActiedatum: string;
    archiefNominatie: string;
    isAfgehandeld: boolean;
    isZaakspecifiekGeautoriseerd: boolean;
    groepId: string;
    groepNaam: string;
    behandelaarNaam: string;
    behandelaarGebruikersnaam: string;
    initiatorIdentificatie: string;
    locatie: string;
    isVerlengd: boolean;
    duurVerlenging: string;
    redenVerlenging: string;
    isOpgeschort: boolean;
    redenOpschorting: string;
    zaaktypeUuid: string;
    zaaktypeOmschrijving: string;
    resultaattypeOmschrijving: string;
    resultaatToelichting: string;
    statustypeOmschrijving: string;
    isDeelzaak: boolean;
    isHoofdzaak: boolean;
    isHeropend: boolean;
    statusToelichting: string;
    rechten: GeneratedType<"RestZaakRechten">;
    indicaties: GeneratedType<"ZaakIndicatie">[];
    betrokkenen: Record<GeneratedType<"OmschrijvingGeneriekEnum">, string[]>;
  };
