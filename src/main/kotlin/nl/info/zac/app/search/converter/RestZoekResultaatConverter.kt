/*
 *
 *  * SPDX-FileCopyrightText: 2025 Lifely
 *  * SPDX-License-Identifier: EUPL-1.2+
 *
 */
package nl.info.zac.app.search.converter

import jakarta.inject.Inject
import net.atos.client.zgw.zrc.ZrcClientService
import net.atos.zac.policy.PolicyService
import net.atos.zac.search.model.FilterParameters
import net.atos.zac.search.model.FilterResultaat
import net.atos.zac.search.model.FilterVeld
import net.atos.zac.search.model.ZoekResultaat
import net.atos.zac.search.model.zoekobject.DocumentZoekObject
import net.atos.zac.search.model.zoekobject.TaakZoekObject
import net.atos.zac.search.model.zoekobject.ZaakZoekObject
import net.atos.zac.search.model.zoekobject.ZoekObject
import net.atos.zac.search.model.zoekobject.ZoekObjectType
import nl.info.client.zgw.util.extractUuid
import nl.info.client.zgw.ztc.ZtcClientService
import nl.info.zac.app.search.model.AbstractRestZoekObject
import nl.info.zac.app.search.model.RestZaakKoppelenZoekObject
import nl.info.zac.app.search.model.RestZaakZoekObject
import nl.info.zac.app.search.model.RestZoekParameters
import nl.info.zac.app.search.model.RestZoekResultaat
import nl.info.zac.app.search.model.toRestDocumentZoekObject
import nl.info.zac.app.search.model.toRestTaakZoekObject
import nl.info.zac.app.search.model.toRestZaakZoekObject
import nl.info.zac.util.AllOpen
import nl.info.zac.util.NoArgConstructor
import java.util.UUID

@NoArgConstructor
@AllOpen
class RestZoekResultaatConverter @Inject constructor(
    private val zrcClientService: ZrcClientService,
    private val ztcClientService: ZtcClientService,
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

    fun convert(restZaakZoekObject: RestZaakZoekObject, informationObjectTypeUuid: UUID) =
        RestZaakKoppelenZoekObject(
            id = restZaakZoekObject.id,
            type = restZaakZoekObject.type,
            identificatie = restZaakZoekObject.identificatie,
            omschrijving = restZaakZoekObject.omschrijving,
            toelichting = restZaakZoekObject.toelichting,
            documentKoppelen = restZaakZoekObject.identificatie.let {
                zrcClientService.readZaakByID(it).zaaktype.extractUuid().let {
                    ztcClientService.readZaaktype(it).informatieobjecttypen.any {
                        it.extractUuid() == informationObjectTypeUuid
                    }
                }
            }
        )
}
