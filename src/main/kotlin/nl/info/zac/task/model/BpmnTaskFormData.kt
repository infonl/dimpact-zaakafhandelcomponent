/*
 * SPDX-FileCopyrightText: 2024 Atos, 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.task.model

import net.atos.zac.util.time.LocalDateAdapter
import java.time.LocalDate

class BpmnTaskFormData(taakData: Map<String, Any>) {
    var zaakOpschorten: Boolean = false

    var zaakHervatten: Boolean = false

    var taakFataleDatum: LocalDate? = null

    var taakToekennenGroep: String? = null

    var taakToekennenMedewerker: String? = null

    var toelichting: String? = null

    var mailBijlagen: String? = null

    var documentenVerzendenDatum: LocalDate? = null

    var documentenVerzenden: String? = null

    var documentenOndertekenen: String? = null

    val zaakVariabelen: MutableMap<String, Any> = taakData.toMutableMap()

    companion object {
        private const val ZAAK_OPSCHORTEN = "zaak-opschorten"

        private const val ZAAK_HERVATTEN = "zaak-hervatten"

        private const val TAAK_FATALE_DATUM = "taak-fatale-datum"

        private const val TAAK_TOEKENNEN_GROEP = "taak-toekennen-groep"

        private const val TAAK_TOEKENNEN_MEDEWERKER = "taak-toekennen-behandelaar"

        private const val DOCUMENTEN_VERZENDEN = "documenten-verzenden"

        private const val DOCUMENTEN_VERZENDEN_DATUM = "documenten-verzenden-datum"

        private const val DOCUMENTEN_ONDERTEKENEN = "documenten-onderteken"

        private const val MAIL_BIJLAGEN = "mail-bijlagen"

        private const val TOELICHTING = "toelichting"
    }

    init {
        taakData[ZAAK_OPSCHORTEN]?.let {
            zaakOpschorten = it.toString().toBoolean()
            zaakVariabelen.remove(ZAAK_OPSCHORTEN)
        }
        taakData[ZAAK_HERVATTEN]?.let {
            zaakHervatten = it.toString().toBoolean()
            zaakVariabelen.remove(ZAAK_HERVATTEN)
        }
        taakData[TAAK_FATALE_DATUM]?.let {
            taakFataleDatum = LocalDateAdapter().adaptFromJson(it.toString())
            zaakVariabelen.remove(TAAK_FATALE_DATUM)
        }
        taakData[TOELICHTING]?.let {
            toelichting = it.toString()
            zaakVariabelen.remove(TOELICHTING)
        }
        taakData[TAAK_TOEKENNEN_GROEP]?.let {
            taakToekennenGroep = it.toString()
            zaakVariabelen.remove(TAAK_TOEKENNEN_GROEP)
        }
        taakData[TAAK_TOEKENNEN_MEDEWERKER]?.let {
            taakToekennenMedewerker = it.toString()
            zaakVariabelen.remove(TAAK_TOEKENNEN_MEDEWERKER)
        }
        taakData[MAIL_BIJLAGEN]?.toString()?.also {
            if (it.isNotBlank()) mailBijlagen = it
            zaakVariabelen.remove(MAIL_BIJLAGEN)
        }
        taakData[DOCUMENTEN_VERZENDEN]?.toString()?.also {
            if (it.isNotBlank()) documentenVerzenden = it
            zaakVariabelen.remove(DOCUMENTEN_VERZENDEN)
        }
        taakData[DOCUMENTEN_VERZENDEN_DATUM]?.let {
            documentenVerzendenDatum = LocalDateAdapter().adaptFromJson(it.toString())
            zaakVariabelen.remove(DOCUMENTEN_VERZENDEN_DATUM)
        } ?: run {
            documentenVerzendenDatum = LocalDate.now()
        }
        taakData[DOCUMENTEN_ONDERTEKENEN]?.toString()?.also {
            if (it.isNotBlank()) documentenOndertekenen = it
            zaakVariabelen.remove(DOCUMENTEN_ONDERTEKENEN)
        }
    }
}
