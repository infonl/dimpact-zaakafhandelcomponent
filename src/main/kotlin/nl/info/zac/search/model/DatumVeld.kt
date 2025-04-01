/*
 * SPDX-FileCopyrightText: 2022 Atos, 2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.search.model

enum class DatumVeld(val veld: String) {
    STARTDATUM("startdatum"),

    ZAAK_STARTDATUM("zaak_startdatum"),
    ZAAK_EINDDATUM("zaak_einddatum"),
    ZAAK_STREEFDATUM("zaak_einddatumGepland"),
    ZAAK_FATALE_DATUM("zaak_uiterlijkeEinddatumAfdoening"),
    ZAAK_REGISTRATIEDATUM("zaak_registratiedatum"),
    ZAAK_PUBLICATIEDATUM("zaak_publicatiedatum"),
    ZAAK_STATUS_DATUM_GEZET("zaak_statusDatumGezet"),
    ZAAK_ARCHIEF_ACTIEDATUM("zaak_archiefActiedatum"),

    TAAK_CREATIEDATUM("taak_creatiedatum"),
    TAAK_TOEKENNINGSDATUM("taak_toekenningsdatum"),
    TAAK_FATALEDATUM("taak_fataledatum"),

    INFORMATIEOBJECT_CREATIEDATUM("informatieobject_creatiedatum"),
    INFORMATIEOBJECT_REGISTRATIEDATUM("informatieobject_registratiedatum"),
    INFORMATIEOBJECT_ONTVANGSTDATUM("informatieobject_ontvangstdatum"),
    INFORMATIEOBJECT_VERZENDDATUM("informatieobject_verzenddatum")
}
