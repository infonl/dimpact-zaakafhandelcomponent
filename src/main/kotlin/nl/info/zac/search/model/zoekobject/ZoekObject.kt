/*
 * SPDX-FileCopyrightText: 2021 Atos, 2024 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.search.model.zoekobject

interface ZoekObject {
    companion object {
        const val IS_TOEGEKEND_FIELD: String = "isToegekend"

        /**
         * The shared Solr field that [ZaakZoekObject.ZAAKSPECIFIEK_GEAUTORISEERD_FIELD],
         * [TaakZoekObject.ZAAKSPECIFIEK_GEAUTORISEERD_FIELD], and
         * [DocumentZoekObject.ZAAKSPECIFIEK_GEAUTORISEERD_FIELD] are each `copyField`-merged into,
         * mirroring how the three types' `zaaktypeOmschrijving` fields are merged into one shared field.
         */
        const val ZAAKSPECIFIEK_GEAUTORISEERD_FIELD: String = "zaakspecifiekGeautoriseerd"

        /**
         * The shared Solr field that [ZaakZoekObject.ZAAK_GEAUTORISEERDE_MEDEWERKERS_FIELD],
         * [TaakZoekObject.ZAAK_GEAUTORISEERDE_MEDEWERKERS_FIELD], and
         * [DocumentZoekObject.ZAAK_GEAUTORISEERDE_MEDEWERKERS_FIELD] are each `copyField`-merged into.
         *
         * These are the medewerkers individually authorised for the *zaak* the row belongs to, as opposed
         * to those authorised for its whole zaaktype by holding the `zaakspecifiek_geautoriseerd`
         * application role. For a taak these are different people than
         * [TaakZoekObject.BEHANDELAAR_ID_FIELD], the behandelaar of the taak itself.
         */
        const val ZAAK_GEAUTORISEERDE_MEDEWERKERS_FIELD: String = "zaakGeautoriseerdeMedewerkers"
    }

    fun getObjectId(): String

    fun getType(): ZoekObjectType
}
