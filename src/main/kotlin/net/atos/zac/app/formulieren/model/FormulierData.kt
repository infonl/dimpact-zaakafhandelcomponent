/*
 * SPDX-FileCopyrightText: 2024 Atos, 2025 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.zac.app.formulieren.model

import net.atos.zac.util.time.LocalDateAdapter
import java.time.LocalDate

class FormulierData(taakData: Map<String, Any>) {
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
        if (taakData.containsKey(ZAAK_OPSCHORTEN) && taakData[ZAAK_OPSCHORTEN] != null) {
            if (true.toString() == taakData[ZAAK_OPSCHORTEN]) {
                this.zaakOpschorten = true
            }
            zaakVariabelen.remove(ZAAK_OPSCHORTEN)
        }
        if (taakData.containsKey(ZAAK_HERVATTEN) && taakData[ZAAK_HERVATTEN] != null) {
            if (true.toString() == taakData[ZAAK_HERVATTEN]) {
                this.zaakHervatten = true
            }
            zaakVariabelen.remove(ZAAK_HERVATTEN)
        }
        if (taakData.containsKey(TAAK_FATALE_DATUM) && taakData[TAAK_FATALE_DATUM] != null) {
            this.taakFataleDatum = LocalDateAdapter().adaptFromJson(taakData[TAAK_FATALE_DATUM].toString())
            zaakVariabelen.remove(TAAK_FATALE_DATUM)
        }
        if (taakData.containsKey(TOELICHTING) && taakData[TOELICHTING] != null) {
            this.toelichting = taakData[TOELICHTING].toString()
            zaakVariabelen.remove(TOELICHTING)
        }
        if (taakData.containsKey(TAAK_TOEKENNEN_GROEP) && taakData[TAAK_TOEKENNEN_GROEP] != null) {
            this.taakToekennenGroep = taakData[TAAK_TOEKENNEN_GROEP].toString()
            zaakVariabelen.remove(TAAK_TOEKENNEN_GROEP)
        }
        if (taakData.containsKey(TAAK_TOEKENNEN_MEDEWERKER) && taakData[TAAK_TOEKENNEN_MEDEWERKER] != null) {
            this.taakToekennenMedewerker = taakData[TAAK_TOEKENNEN_MEDEWERKER].toString()
            zaakVariabelen.remove(TAAK_TOEKENNEN_MEDEWERKER)
        }
        if (taakData.containsKey(MAIL_BIJLAGEN) && taakData[MAIL_BIJLAGEN] != null) {
            if (taakData[MAIL_BIJLAGEN].toString().isNotBlank()) {
                this.mailBijlagen = taakData[MAIL_BIJLAGEN].toString()
            }
            zaakVariabelen.remove(MAIL_BIJLAGEN)
        }
        if (taakData.containsKey(DOCUMENTEN_VERZENDEN) && taakData[DOCUMENTEN_VERZENDEN] != null) {
            if (taakData[DOCUMENTEN_VERZENDEN].toString().isNotBlank()) {
                this.documentenVerzenden = taakData[DOCUMENTEN_VERZENDEN].toString()
            }
            zaakVariabelen.remove(DOCUMENTEN_VERZENDEN)
        }
        if (taakData.containsKey(DOCUMENTEN_VERZENDEN_DATUM) && taakData[DOCUMENTEN_VERZENDEN_DATUM] != null) {
            this.documentenVerzendenDatum =
                LocalDateAdapter().adaptFromJson(taakData[DOCUMENTEN_VERZENDEN_DATUM].toString())
            zaakVariabelen.remove(DOCUMENTEN_VERZENDEN_DATUM)
        } else {
            this.documentenVerzendenDatum = LocalDate.now()
        }
        if (taakData.containsKey(DOCUMENTEN_ONDERTEKENEN) && taakData[DOCUMENTEN_ONDERTEKENEN] != null) {
            if (taakData[DOCUMENTEN_ONDERTEKENEN].toString().isNotBlank()) {
                this.documentenOndertekenen = taakData[DOCUMENTEN_ONDERTEKENEN].toString()
            }
            zaakVariabelen.remove(DOCUMENTEN_ONDERTEKENEN)
        }
    }
}
