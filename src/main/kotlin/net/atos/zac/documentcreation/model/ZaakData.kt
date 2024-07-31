/*
 * SPDX-FileCopyrightText: 2022 Atos
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.zac.documentcreation.model

import jakarta.json.bind.annotation.JsonbDateFormat
import net.atos.zac.documentcreation.converter.DocumentCreationDataConverter
import java.time.LocalDate

class ZaakData {
    var zaaktype: String? = null

    var identificatie: String? = null

    var omschrijving: String? = null

    var toelichting: String? = null

    @JsonbDateFormat(DocumentCreationDataConverter.Companion.DATE_FORMAT)
    var registratiedatum: LocalDate? = null

    @JsonbDateFormat(DocumentCreationDataConverter.Companion.DATE_FORMAT)
    var startdatum: LocalDate? = null

    @JsonbDateFormat(DocumentCreationDataConverter.Companion.DATE_FORMAT)
    var einddatumGepland: LocalDate? = null

    @JsonbDateFormat(DocumentCreationDataConverter.Companion.DATE_FORMAT)
    var uiterlijkeEinddatumAfdoening: LocalDate? = null

    @JsonbDateFormat(DocumentCreationDataConverter.Companion.DATE_FORMAT)
    var einddatum: LocalDate? = null

    var communicatiekanaal: String? = null

    var vertrouwelijkheidaanduiding: String? = null

    var verlengingReden: String? = null

    var opschortingReden: String? = null

    var resultaat: String? = null

    var status: String? = null

    var besluit: String? = null

    var groep: String? = null

    var behandelaar: String? = null
}
