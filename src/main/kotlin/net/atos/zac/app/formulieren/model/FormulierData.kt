/*
 * SPDX-FileCopyrightText: 2024 Atos
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.zac.app.formulieren.model

import net.atos.zac.util.time.LocalDateAdapter
import org.apache.commons.lang3.BooleanUtils
import org.apache.commons.lang3.StringUtils
import java.time.LocalDate

class FormulierData(taakData: MutableMap<String?, Any?>) {
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

    val zaakVariabelen: MutableMap<String?, Any?>

    init {
        this.zaakVariabelen = HashMap<String?, Any?>(taakData)
        if (taakData.containsKey(ZAAK_OPSCHORTEN) && taakData.get(ZAAK_OPSCHORTEN) != null) {
            if (BooleanUtils.TRUE == taakData.get(ZAAK_OPSCHORTEN)) {
                this.zaakOpschorten = true
            }
            zaakVariabelen.remove(ZAAK_OPSCHORTEN)
        }
        if (taakData.containsKey(ZAAK_HERVATTEN) && taakData.get(ZAAK_HERVATTEN) != null) {
            if (BooleanUtils.TRUE == taakData.get(ZAAK_HERVATTEN)) {
                this.zaakHervatten = true
            }
            zaakVariabelen.remove(ZAAK_HERVATTEN)
        }
        if (taakData.containsKey(TAAK_FATALE_DATUM) && taakData.get(TAAK_FATALE_DATUM) != null) {
            this.taakFataleDatum = LocalDateAdapter().adaptFromJson(taakData.get(TAAK_FATALE_DATUM).toString())
            zaakVariabelen.remove(TAAK_FATALE_DATUM)
        }
        if (taakData.containsKey(TOELICHTING) && taakData.get(TOELICHTING) != null) {
            this.toelichting = taakData.get(TOELICHTING).toString()
            zaakVariabelen.remove(TOELICHTING)
        }
        if (taakData.containsKey(TAAK_TOEKENNEN_GROEP) && taakData.get(TAAK_TOEKENNEN_GROEP) != null) {
            this.taakToekennenGroep = taakData.get(TAAK_TOEKENNEN_GROEP).toString()
            zaakVariabelen.remove(TAAK_TOEKENNEN_GROEP)
        }
        if (taakData.containsKey(TAAK_TOEKENNEN_MEDEWERKER) && taakData.get(TAAK_TOEKENNEN_MEDEWERKER) != null) {
            this.taakToekennenMedewerker = taakData.get(TAAK_TOEKENNEN_MEDEWERKER).toString()
            zaakVariabelen.remove(TAAK_TOEKENNEN_MEDEWERKER)
        }
        if (taakData.containsKey(MAIL_BIJLAGEN) && taakData.get(MAIL_BIJLAGEN) != null) {
            if (StringUtils.isNotBlank(taakData.get(MAIL_BIJLAGEN).toString())) {
                this.mailBijlagen = taakData.get(MAIL_BIJLAGEN).toString()
            }
            zaakVariabelen.remove(MAIL_BIJLAGEN)
        }
        if (taakData.containsKey(DOCUMENTEN_VERZENDEN) && taakData.get(DOCUMENTEN_VERZENDEN) != null) {
            if (StringUtils.isNotBlank(taakData.get(DOCUMENTEN_VERZENDEN).toString())) {
                this.documentenVerzenden = taakData.get(DOCUMENTEN_VERZENDEN).toString()
            }
            zaakVariabelen.remove(DOCUMENTEN_VERZENDEN)
        }
        if (taakData.containsKey(DOCUMENTEN_VERZENDEN_DATUM) && taakData.get(DOCUMENTEN_VERZENDEN_DATUM) != null) {
            this.documentenVerzendenDatum =
                LocalDateAdapter().adaptFromJson(taakData.get(DOCUMENTEN_VERZENDEN_DATUM).toString())
            zaakVariabelen.remove(DOCUMENTEN_VERZENDEN_DATUM)
        } else {
            this.documentenVerzendenDatum = LocalDate.now()
        }
        if (taakData.containsKey(DOCUMENTEN_ONDERTEKENEN) && taakData.get(DOCUMENTEN_ONDERTEKENEN) != null) {
            if (StringUtils.isNotBlank(taakData.get(DOCUMENTEN_ONDERTEKENEN).toString())) {
                this.documentenOndertekenen = taakData.get(DOCUMENTEN_ONDERTEKENEN).toString()
            }
            zaakVariabelen.remove(DOCUMENTEN_ONDERTEKENEN)
        }
    }

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
}
