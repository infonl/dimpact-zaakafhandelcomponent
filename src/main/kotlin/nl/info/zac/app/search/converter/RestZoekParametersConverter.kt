/*
 *
 *  * SPDX-FileCopyrightText: 2025 Lifely
 *  * SPDX-License-Identifier: EUPL-1.2+
 *
 */
package nl.info.zac.app.search.converter

import jakarta.enterprise.inject.Instance
import jakarta.inject.Inject
import nl.info.zac.app.search.model.RestDatumRange
import nl.info.zac.app.search.model.RestZoekParameters
import nl.info.zac.authentication.LoggedInUser
import nl.info.zac.search.model.DatumRange
import nl.info.zac.search.model.DatumVeld
import nl.info.zac.search.model.FilterParameters
import nl.info.zac.search.model.FilterVeld
import nl.info.zac.search.model.ZOEK_VELDEN
import nl.info.zac.search.model.ZoekParameters
import nl.info.zac.search.model.ZoekVeld
import nl.info.zac.search.model.zoekobject.TaakZoekObject
import nl.info.zac.search.model.zoekobject.ZaakZoekObject
import nl.info.zac.shared.model.fromValue
import nl.info.zac.util.AllOpen
import nl.info.zac.util.NoArgConstructor
import org.apache.commons.lang3.BooleanUtils

@NoArgConstructor
@AllOpen
class RestZoekParametersConverter @Inject constructor(
    private val loggedInUserInstance: Instance<LoggedInUser>
) {
    fun convert(restZoekParameters: RestZoekParameters): ZoekParameters {
        val zoekParameters = ZoekParameters(restZoekParameters.type)
        restZoekParameters.filters?.forEach { (filterVeld: FilterVeld, filterParameters: FilterParameters) ->
            zoekParameters.addFilter(filterVeld, filterParameters)
        }
        restZoekParameters.datums?.forEach { (key: DatumVeld, value: RestDatumRange) ->
            if (value.hasValue()) {
                zoekParameters.addDatum(key, DatumRange(value.van, value.tot))
            }
        }
        if (restZoekParameters.alleenOpenstaandeZaken) {
            zoekParameters.addFilterQuery(ZaakZoekObject.AFGEHANDELD_FIELD, BooleanUtils.FALSE)
        }
        if (restZoekParameters.alleenAfgeslotenZaken) {
            zoekParameters.addFilterQuery(ZaakZoekObject.EINDSTATUS_FIELD, BooleanUtils.TRUE)
        }
        if (restZoekParameters.alleenMijnZaken) {
            zoekParameters.addFilterQuery(ZaakZoekObject.BEHANDELAAR_ID_FIELD, loggedInUserInstance.get().id)
        }
        if (restZoekParameters.alleenMijnTaken) {
            zoekParameters.addFilterQuery(TaakZoekObject.BEHANDELAAR_ID_FIELD, loggedInUserInstance.get().id)
        }
        restZoekParameters.zoeken?.let {
            it.forEach { (key: String, value: String) ->
                if (ZOEK_VELDEN.contains(key)) {
                    zoekParameters.addZoekVeld(ZoekVeld.valueOf(key), value)
                } else if (key.startsWith(ZaakZoekObject.ZAAK_BETROKKENE_PREFIX)) {
                    zoekParameters.addFilterQuery(key, value)
                }
            }
        }
        restZoekParameters.sorteerVeld?.let {
            zoekParameters.setSortering(it, fromValue(restZoekParameters.sorteerRichting))
        }
        zoekParameters.start = restZoekParameters.page * restZoekParameters.rows
        zoekParameters.rows = restZoekParameters.rows
        return zoekParameters
    }
}
