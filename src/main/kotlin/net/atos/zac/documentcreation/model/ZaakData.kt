/*
 * SPDX-FileCopyrightText: 2022 Atos, 2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.zac.documentcreation.model

import jakarta.json.bind.annotation.JsonbDateFormat
import net.atos.zac.documentcreation.converter.DocumentCreationDataConverter.Companion.DATE_FORMAT
import java.time.LocalDate

data class ZaakData(
    val zaaktype: String? = null,

    val identificatie: String? = null,

    val omschrijving: String? = null,

    val toelichting: String? = null,

    @field:JsonbDateFormat(DATE_FORMAT)
    val registratiedatum: LocalDate? = null,

    @field:JsonbDateFormat(DATE_FORMAT)
    val startdatum: LocalDate? = null,

    @field:JsonbDateFormat(DATE_FORMAT)
    val einddatumGepland: LocalDate? = null,

    @field:JsonbDateFormat(DATE_FORMAT)
    val uiterlijkeEinddatumAfdoening: LocalDate? = null,

    @field:JsonbDateFormat(DATE_FORMAT)
    val einddatum: LocalDate? = null,

    val communicatiekanaal: String? = null,

    val vertrouwelijkheidaanduiding: String? = null,

    val verlengingReden: String? = null,

    val opschortingReden: String? = null,

    val resultaat: String? = null,

    val status: String? = null,

    val besluit: String? = null,

    val groep: String? = null,

    val behandelaar: String? = null
)
