/*
 *
 *  * SPDX-FileCopyrightText: 2025 Lifely
 *  * SPDX-License-Identifier: EUPL-1.2+
 *
 */
package nl.info.zac.app.search.converter

import jakarta.inject.Inject
import net.atos.zac.policy.PolicyService
import nl.info.zac.app.search.model.AbstractRestZoekObject
import nl.info.zac.app.search.model.RestZaakKoppelenZoekObject
import nl.info.zac.app.search.model.RestZaakZoekObject
import nl.info.zac.app.search.model.RestZoekParameters
import nl.info.zac.app.search.model.RestZoekResultaat
import nl.info.zac.app.search.model.toRestDocumentZoekObject
import nl.info.zac.app.search.model.toRestTaakZoekObject
import nl.info.zac.app.search.model.toRestZaakZoekObject
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

    fun convert(zoekResultaat: ZoekResultaat<out ZoekObject>, documentLinkableList: List<Boolean>) =
        RestZoekResultaat(
            zoekResultaat.items.mapIndexed { index, result ->
                with(result as ZaakZoekObject) {
                    convert(
                        result.toRestZaakZoekObject(policyService.readZaakRechten(result)),
                        documentLinkableList[index]
                    )
                }
            },
            zoekResultaat.count
        )

    private fun convert(restZaakZoekObject: RestZaakZoekObject, documentLinkable: Boolean) =
        RestZaakKoppelenZoekObject(
            id = restZaakZoekObject.id,
            type = restZaakZoekObject.type,
            identificatie = restZaakZoekObject.identificatie,
            omschrijving = restZaakZoekObject.omschrijving,
            toelichting = restZaakZoekObject.toelichting,
            zaaktypeOmschrijving = restZaakZoekObject.zaaktypeOmschrijving,
            statustypeOmschrijving = restZaakZoekObject.statustypeOmschrijving,
            documentKoppelbaar = documentLinkable
        )
}
