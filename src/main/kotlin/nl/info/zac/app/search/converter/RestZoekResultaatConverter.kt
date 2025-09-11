/*
 *
 *  * SPDX-FileCopyrightText: 2025 INFO.nl
 *  * SPDX-License-Identifier: EUPL-1.2+
 *
 */
package nl.info.zac.app.search.converter

import jakarta.inject.Inject
import nl.info.zac.app.search.model.AbstractRestZoekObject
import nl.info.zac.app.search.model.RestZoekParameters
import nl.info.zac.app.search.model.RestZoekResultaat
import nl.info.zac.app.search.model.toRestDocumentZoekObject
import nl.info.zac.app.search.model.toRestTaakZoekObject
import nl.info.zac.app.search.model.toRestZaakKoppelenZoekObject
import nl.info.zac.app.search.model.toRestZaakZoekObject
import nl.info.zac.policy.PolicyService
import nl.info.zac.search.model.FilterParameters
import nl.info.zac.search.model.FilterResultaat
import nl.info.zac.search.model.FilterVeld
import nl.info.zac.search.model.ZoekResultaat
import nl.info.zac.search.model.zoekobject.DocumentZoekObject
import nl.info.zac.search.model.zoekobject.TaakZoekObject
import nl.info.zac.search.model.zoekobject.ZaakZoekObject
import nl.info.zac.search.model.zoekobject.ZoekObject
import nl.info.zac.search.model.zoekobject.ZoekObjectType
import nl.info.zac.util.AllOpen
import nl.info.zac.util.NoArgConstructor

@NoArgConstructor
@AllOpen
class RestZoekResultaatConverter @Inject constructor(
    private val policyService: PolicyService
) {
    fun convert(
        zoekResultaat: ZoekResultaat<out ZoekObject>,
        zoekParameters: RestZoekParameters
    ): RestZoekResultaat<out AbstractRestZoekObject> {
        val restZoekResultaat = RestZoekResultaat(
            zoekResultaat.items.map { it.toAbstractRestZoekObject() },
            zoekResultaat.count
        )
        restZoekResultaat.filters.putAll(zoekResultaat.getFilters())
        zoekParameters.filters?.forEach { (filterVeld: FilterVeld, filters: FilterParameters) ->
            val filterResultaten = restZoekResultaat.filters.getOrPut(filterVeld) { ArrayList() }
            filters.values.forEach { filterValue: String ->
                // if there are no results, keep the current filters
                if (filterResultaten.none { it.naam == filterValue }) {
                    filterResultaten.add(FilterResultaat(filterValue, 0))
                }
            }
        }
        return restZoekResultaat
    }

    fun convert(zoekResultaat: ZoekResultaat<out ZoekObject>, documentLinkableList: List<Boolean>) =
        RestZoekResultaat(
            zoekResultaat.items.mapIndexed { index, result ->
                with(result as ZaakZoekObject) {
                    result.toRestZaakZoekObject(policyService.readZaakRechtenForZaakZoekObject(result))
                        .toRestZaakKoppelenZoekObject(documentLinkableList[index])
                }
            },
            zoekResultaat.count
        )

    private fun ZoekObject.toAbstractRestZoekObject(): AbstractRestZoekObject =
        when (this.getType()) {
            ZoekObjectType.ZAAK -> (this as ZaakZoekObject).toRestZaakZoekObject(
                policyService.readZaakRechtenForZaakZoekObject(this)
            )
            ZoekObjectType.TAAK -> (this as TaakZoekObject).toRestTaakZoekObject(
                policyService.readTaakRechten(this)
            )
            ZoekObjectType.DOCUMENT -> (this as DocumentZoekObject).toRestDocumentZoekObject(
                policyService.readDocumentRechten(this)
            )
        }
}
