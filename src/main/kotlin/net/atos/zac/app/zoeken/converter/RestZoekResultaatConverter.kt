/*
 * SPDX-FileCopyrightText: 2022 Atos, 2025 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.zac.app.zoeken.converter

import jakarta.inject.Inject
import net.atos.zac.app.zoeken.model.AbstractRestZoekObject
import net.atos.zac.app.zoeken.model.RestZoekParameters
import net.atos.zac.app.zoeken.model.RestZoekResultaat
import net.atos.zac.app.zoeken.model.toRestDocumentZoekObject
import net.atos.zac.app.zoeken.model.toRestTaakZoekObject
import net.atos.zac.app.zoeken.model.toRestZaakZoekObject
import net.atos.zac.policy.PolicyService
import net.atos.zac.zoeken.model.FilterParameters
import net.atos.zac.zoeken.model.FilterResultaat
import net.atos.zac.zoeken.model.FilterVeld
import net.atos.zac.zoeken.model.ZoekResultaat
import net.atos.zac.zoeken.model.zoekobject.DocumentZoekObject
import net.atos.zac.zoeken.model.zoekobject.TaakZoekObject
import net.atos.zac.zoeken.model.zoekobject.ZaakZoekObject
import net.atos.zac.zoeken.model.zoekobject.ZoekObject
import net.atos.zac.zoeken.model.zoekobject.ZoekObjectType
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
            zoekResultaat.items.map { convert(it) },
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

    private fun convert(zoekObject: ZoekObject): AbstractRestZoekObject =
        when (zoekObject.getType()) {
            ZoekObjectType.ZAAK -> (zoekObject as ZaakZoekObject).toRestZaakZoekObject(
                policyService.readZaakRechten(zoekObject)
            )
            ZoekObjectType.TAAK -> (zoekObject as TaakZoekObject).toRestTaakZoekObject(
                policyService.readTaakRechten(zoekObject)
            )
            ZoekObjectType.DOCUMENT -> (zoekObject as DocumentZoekObject).toRestDocumentZoekObject(
                policyService.readDocumentRechten(zoekObject)
            )
        }
}
