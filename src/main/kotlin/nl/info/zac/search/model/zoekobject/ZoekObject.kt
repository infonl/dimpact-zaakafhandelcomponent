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
    }

    fun getObjectId(): String

    fun getType(): ZoekObjectType
}
