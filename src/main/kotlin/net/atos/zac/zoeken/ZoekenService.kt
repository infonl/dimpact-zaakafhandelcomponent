/*
 * SPDX-FileCopyrightText: 2022 Atos, 2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.zac.zoeken

import jakarta.enterprise.context.ApplicationScoped
import jakarta.enterprise.inject.Instance
import jakarta.inject.Inject
import net.atos.zac.authentication.LoggedInUser
import net.atos.zac.shared.model.SorteerRichting
import net.atos.zac.solr.encoded
import net.atos.zac.solr.quoted
import net.atos.zac.zoeken.IndexingService.Companion.SOLR_CORE
import net.atos.zac.zoeken.model.DatumRange
import net.atos.zac.zoeken.model.DatumVeld
import net.atos.zac.zoeken.model.FilterParameters
import net.atos.zac.zoeken.model.FilterResultaat
import net.atos.zac.zoeken.model.FilterVeld
import net.atos.zac.zoeken.model.FilterWaarde
import net.atos.zac.zoeken.model.SorteerVeld
import net.atos.zac.zoeken.model.ZoekObject
import net.atos.zac.zoeken.model.ZoekParameters
import net.atos.zac.zoeken.model.ZoekResultaat
import net.atos.zac.zoeken.model.ZoekVeld
import net.atos.zac.zoeken.model.index.ZoekObjectType
import nl.lifely.zac.util.AllOpen
import nl.lifely.zac.util.NoArgConstructor
import org.apache.commons.collections4.CollectionUtils
import org.apache.commons.lang3.StringUtils
import org.apache.solr.client.solrj.SolrClient
import org.apache.solr.client.solrj.SolrQuery
import org.apache.solr.client.solrj.SolrServerException
import org.apache.solr.client.solrj.impl.Http2SolrClient
import org.apache.solr.client.solrj.response.FacetField
import org.apache.solr.common.params.SimpleParams
import org.eclipse.microprofile.config.ConfigProvider
import java.io.IOException
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.function.Consumer
import java.util.stream.Collectors

@ApplicationScoped
@AllOpen
@NoArgConstructor
class ZoekenService @Inject constructor(
    private val loggedInUserInstance: Instance<LoggedInUser>
) {
    companion object {
        private lateinit var solrClient: SolrClient

        private val NON_EXISTING_ZAAKTYPE = quoted("-NON-EXISTING-ZAAKTYPE-")
        private const val ZAAKTYPE_OMSCHRIJVING_VELD = "zaaktypeOmschrijving"
    }

    init {
        solrClient = Http2SolrClient.Builder(
            "${ConfigProvider.getConfig().getValue("solr.url", String::class.java)}/solr/$SOLR_CORE"
        ).build()
    }

    @Suppress("TooGenericExceptionThrown", "CyclomaticComplexMethod", "LongMethod")
    fun zoek(zoekParameters: ZoekParameters): ZoekResultaat<out ZoekObject> {
        val query = SolrQuery("*:*")

        // Signaleringen job does not have a logged-in user so check if it is present
        loggedInUserInstance.get()?.also {
            applyAllowedZaaktypenPolicy(query)
        }
        zoekParameters.type?.let {
            query.addFilterQuery("type:${zoekParameters.type}")
        }
        zoekParameters.zoeken.forEach { (searchField: ZoekVeld, text: String) ->
            if (text.isNotBlank()) {
                if (searchField == ZoekVeld.ZAAK_IDENTIFICATIE || searchField == ZoekVeld.TAAK_ZAAK_ID) {
                    query.addFilterQuery("${searchField.veld}:(*${encoded(text)}*)")
                } else {
                    query.addFilterQuery("${searchField.veld}:(${encoded(text)})")
                }
            }
        }
        zoekParameters.datums.forEach { (dateField: DatumVeld, date: DatumRange) ->
            query.addFilterQuery(
                "${dateField.veld}:[${date.van?.let {
                    "*"
                } ?: {
                    DateTimeFormatter.ISO_INSTANT.format(
                        date.van.atStartOfDay(ZoneId.systemDefault())
                    )
                }} TO ${if (date.tot == null) {
                    "*"
                } else {
                    DateTimeFormatter.ISO_INSTANT.format(
                        date.tot.atStartOfDay(ZoneId.systemDefault())
                    )
                }}]"
            )
        }
        zoekParameters.filters
            .forEach { (filterVeld: FilterVeld) ->
                query.addFacetField("{!ex=$filterVeld}${filterVeld.veld}")
            }
        zoekParameters.filters.forEach { (filter: FilterVeld, filterParameters: FilterParameters) ->
            if (CollectionUtils.isNotEmpty(filterParameters.waarden)) {
                val special = if (filterParameters.waarden.size == 1) {
                    filterParameters.waarden.first()
                } else {
                    null
                }
                if (FilterWaarde.LEEG.`is`(special)) {
                    query.addFilterQuery(
                        "{!tag=$filter}!${filter.veld}:(*)"
                    )
                } else if (FilterWaarde.NIET_LEEG.`is`(special)) {
                    query.addFilterQuery(
                        "{!tag=$filter}${filter.veld}:(*)"
                    )
                } else {
                    query.addFilterQuery(
                        "{!tag=$filter}${if (filterParameters.inverse) "-" else StringUtils.EMPTY}" +
                            // TODO; remove stream
                            "${filter.veld}:(${filterParameters.waarden.stream()
                                .map { value: String -> quoted(value) }
                                .collect(Collectors.joining(" OR "))})"
                    )
                }
            }
        }
        zoekParameters.filterQueries
            .forEach { (veld: String, waarde: String) ->
                query.addFilterQuery("$veld:${quoted(waarde)}")
            }
        query.setFacetMinCount(1)
        query.setFacetMissing(!zoekParameters.isGlobaalZoeken)
        query.setFacet(true)
        query.setParam("q.op", SimpleParams.AND_OPERATOR)
        query.setRows(zoekParameters.rows)
        query.setStart(zoekParameters.start)
        query.addSort(
            zoekParameters.sortering.sorteerVeld.veld,
            if (zoekParameters.sortering
                    .richting == SorteerRichting.DESCENDING
            ) {
                SolrQuery.ORDER.desc
            } else {
                SolrQuery.ORDER.asc
            }
        )
        if (zoekParameters.sortering.sorteerVeld != SorteerVeld.CREATED) {
            query.addSort(SorteerVeld.CREATED.veld, SolrQuery.ORDER.desc)
        }
        if (zoekParameters.sortering.sorteerVeld != SorteerVeld.ZAAK_IDENTIFICATIE) {
            query.addSort(SorteerVeld.ZAAK_IDENTIFICATIE.veld, SolrQuery.ORDER.desc)
        }
        // sort on 'id' field so that results (from the same query) always have the same order
        query.addSort("id", SolrQuery.ORDER.desc)
        try {
            val response = solrClient.query(query)
            val zoekObjecten = response.results
                .map {
                    val zoekObjectType = ZoekObjectType.valueOf(it["type"].toString())
                    solrClient.binder.getBean(zoekObjectType.zoekObjectClass, it)
                }
            val zoekResultaat = ZoekResultaat(
                zoekObjecten,
                response.results.numFound
            )
            response.facetFields.forEach(
                Consumer { facetField: FacetField ->
                    val facetVeld = FilterVeld.fromValue(facetField.name)
                    val waardes = mutableListOf<FilterResultaat>()
                    facetField.values
                        .filter { it.count > 0 }
                        .forEach {
                            waardes.add(
                                FilterResultaat(
                                    if (it.name == null) FilterWaarde.LEEG.toString() else it.name,
                                    it.count
                                )
                            )
                        }
                    zoekResultaat.addFilter(facetVeld, waardes)
                }
            )
            return zoekResultaat
        } catch (ioException: IOException) {
            throw RuntimeException(ioException)
        } catch (solrServerException: SolrServerException) {
            throw RuntimeException(solrServerException)
        }
    }

    private fun applyAllowedZaaktypenPolicy(query: SolrQuery) {
        val loggedInUser = loggedInUserInstance.get()
        if (!loggedInUser.isAuthorisedForAllZaaktypen()) {
            if (loggedInUser.geautoriseerdeZaaktypen!!.isEmpty()) {
                query.addFilterQuery("$ZAAKTYPE_OMSCHRIJVING_VELD:$NON_EXISTING_ZAAKTYPE")
            } else {
                query.addFilterQuery(
                    loggedInUser.geautoriseerdeZaaktypen
                        // TODO; remove stream() call
                        .stream()
                        .map { quoted(it) }
                        .map { "$ZAAKTYPE_OMSCHRIJVING_VELD:$it" }
                        .collect(Collectors.joining(" OR "))
                )
            }
        }
    }
}
