/*
 * SPDX-FileCopyrightText: 2022 Atos, 2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.zac.zoeken.model

import java.util.Collections
import java.util.EnumSet

enum class FilterVeld(val veld: String) {
    TYPE("type"),
    ZAAKTYPE("zaaktypeOmschrijving"),
    BEHANDELAAR("behandelaarNaam"),
    GROEP("groepNaam"),
    TOEGEKEND("isToegekend"),

    ZAAK_STATUS("zaak_statustypeOmschrijving"),
    ZAAK_ZAAKTYPE("zaak_zaaktypeOmschrijving"),
    ZAAK_ZAAKTYPE_UUID("zaak_zaaktypeUuid"),
    ZAAK_BEHANDELAAR("zaak_behandelaarNaam"),
    ZAAK_GROEP("zaak_groepNaam"),
    ZAAK_RESULTAAT("zaak_resultaattypeOmschrijving"),
    ZAAK_INDICATIES("zaak_indicaties"),
    ZAAK_COMMUNICATIEKANAAL("zaak_communicatiekanaal"),
    ZAAK_VERTROUWELIJKHEIDAANDUIDING("zaak_vertrouwelijkheidaanduiding"),
    ZAAK_ARCHIEF_NOMINATIE("zaak_archiefNominatie"),

    TAAK_NAAM("taak_naam"),
    TAAK_STATUS("taak_status"),
    TAAK_ZAAKTYPE("taak_zaaktypeOmschrijving"),
    TAAK_BEHANDELAAR("taak_behandelaarNaam"),
    TAAK_GROEP("taak_groepNaam"),

    DOCUMENT_STATUS("informatieobject_status"),
    DOCUMENT_TYPE("informatieobject_documentType"),
    DOCUMENT_VERGRENDELD_DOOR("informatieobject_vergrendeldDoorNaam"),
    DOCUMENT_INDICATIES("informatieobject_indicaties");

    companion object {
        @JvmField
        val zaakFacetten: Set<FilterVeld> = Collections.unmodifiableSet(
            EnumSet.of(
                ZAAKTYPE,
                ZAAK_STATUS,
                BEHANDELAAR,
                GROEP,
                ZAAK_RESULTAAT,
                ZAAK_VERTROUWELIJKHEIDAANDUIDING,
                ZAAK_COMMUNICATIEKANAAL,
                ZAAK_ARCHIEF_NOMINATIE,
                ZAAK_INDICATIES
            )
        )

        @JvmField
        val documentFacetten: Set<FilterVeld> = Collections.unmodifiableSet(
            EnumSet.of(DOCUMENT_STATUS, DOCUMENT_TYPE, DOCUMENT_VERGRENDELD_DOOR, ZAAKTYPE, DOCUMENT_INDICATIES)
        )

        @JvmField
        val taakFacetten: Set<FilterVeld> = Collections.unmodifiableSet(
            EnumSet.of(TAAK_NAAM, TAAK_STATUS, GROEP, BEHANDELAAR, ZAAKTYPE)
        )

        @JvmField
        val facetten: Set<FilterVeld> = Collections.unmodifiableSet(
            EnumSet.of(
                TYPE, ZAAKTYPE, TOEGEKEND, BEHANDELAAR, GROEP, ZAAK_STATUS, ZAAK_INDICATIES, ZAAK_RESULTAAT,
                ZAAK_VERTROUWELIJKHEIDAANDUIDING, ZAAK_COMMUNICATIEKANAAL, ZAAK_ARCHIEF_NOMINATIE,
                TAAK_NAAM, TAAK_STATUS, DOCUMENT_STATUS, DOCUMENT_INDICATIES, DOCUMENT_TYPE,
                DOCUMENT_VERGRENDELD_DOOR
            )
        )

        fun fromValue(sortField: String): FilterVeld =
            entries.toTypedArray().firstOrNull { it.veld == sortField }
                ?: throw IllegalArgumentException("Unsupported sort field: '$sortField'")
    }
}
