/*
 * SPDX-FileCopyrightText: 2022 Atos, 2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.zac.search

import jakarta.enterprise.context.ApplicationScoped
import jakarta.enterprise.inject.Instance
import jakarta.inject.Inject
import net.atos.zac.search.IndexingService.Companion.SOLR_CORE
import net.atos.zac.search.model.FilterParameters
import net.atos.zac.search.model.FilterResultaat
import net.atos.zac.search.model.FilterVeld
import net.atos.zac.search.model.FilterWaarde
import net.atos.zac.search.model.SorteerVeld
import net.atos.zac.search.model.ZoekParameters
import net.atos.zac.search.model.ZoekResultaat
import net.atos.zac.search.model.ZoekVeld
import net.atos.zac.search.model.zoekobject.ZoekObject
import net.atos.zac.search.model.zoekobject.ZoekObjectType
import net.atos.zac.shared.model.SorteerRichting
import net.atos.zac.solr.encoded
import net.atos.zac.solr.quoted
import nl.info.zac.authentication.LoggedInUser
import nl.info.zac.util.AllOpen
import nl.info.zac.util.NoArgConstructor
import org.apache.solr.client.solrj.SolrClient
import org.apache.solr.client.solrj.SolrQuery
import org.apache.solr.client.solrj.SolrServerException
import org.apache.solr.client.solrj.impl.Http2SolrClient
import org.apache.solr.common.params.SimpleParams
import org.eclipse.microprofile.config.ConfigProvider
import java.io.IOException
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@ApplicationScoped
@AllOpen
@NoArgConstructor
class SearchService @Inject constructor(
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

    fun zoek(zoekParameters: ZoekParameters): ZoekResultaat<out ZoekObject> {
        val query = SolrQuery("*:*")
        applyAllowedZaaktypenPolicy(query)
        zoekParameters.type?.let { query.addFilterQuery("type:${zoekParameters.type}") }
        getFilterQueriesForZoekenParameters(zoekParameters).forEach(query::addFilterQuery)
        getFilterQueriesForDatumsParameters(zoekParameters).forEach(query::addFilterQuery)
        zoekParameters.getFilters().forEach { (filter, filterParameters) ->
            query.addFacetField("{!ex=$filter}${filter.veld}")
            if (filterParameters.values.isNotEmpty()) {
                query.addFilterQuery(getFilterQueryForWaardenParameter(filterParameters, filter))
            }
        }
        zoekParameters.getFilterQueries()
            .forEach { (veld: String, waarde: String) -> query.addFilterQuery("$veld:${quoted(waarde)}") }
        query.facetMinCount = 1
        query.setFacetMissing(!zoekParameters.isGlobaalZoeken())
        query.setFacet(true)
        query.setParam("q.op", SimpleParams.AND_OPERATOR)
        query.rows = zoekParameters.rows
        query.start = zoekParameters.start
        if (zoekParameters.sortering.richting != SorteerRichting.NONE) {
            query.addSort(
                zoekParameters.sortering.sorteerVeld.veld,
                if (zoekParameters.sortering.richting == SorteerRichting.DESCENDING) SolrQuery.ORDER.desc else SolrQuery.ORDER.asc
            )
        }

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
            val zoekResultaat = ZoekResultaat(zoekObjecten, response.results.numFound)
            response.facetFields.forEach { facetField ->
                val facetVeld = FilterVeld.fromValue(facetField.name)
                val values = facetField.values
                    .filter { it.count > 0 }
                    .map { FilterResultaat(it.name ?: FilterWaarde.LEEG.toString(), it.count) }
                zoekResultaat.addFilter(facetVeld, values.toMutableList())
            }
            return zoekResultaat
        } catch (ioException: IOException) {
            throw SearchException(
                "Failed to perform Solr search query",
                ioException
            )
        } catch (solrServerException: SolrServerException) {
            throw SearchException(
                "Failed to perform Solr search query",
                solrServerException
            )
        }
    }

    private fun getFilterQueryForWaardenParameter(
        filterParameters: FilterParameters,
        filter: FilterVeld
    ): String {
        val special = filterParameters.values.singleOrNull()
        return when {
            FilterWaarde.LEEG.isEqualTo(special) -> "{!tag=$filter}!${filter.veld}:(*)"
            FilterWaarde.NIET_LEEG.isEqualTo(special) -> "{!tag=$filter}${filter.veld}:(*)"
            else -> "{!tag=$filter}${if (filterParameters.inverse) "-" else ""}" +
                "${filter.veld}:(${filterParameters.values.joinToString(" OR ") { quoted(it) }})"
        }
    }

    private fun getFilterQueriesForDatumsParameters(zoekParameters: ZoekParameters): List<String> =
        zoekParameters.datums.map { (dateField, date) ->
            val from = date.van?.let { DateTimeFormatter.ISO_INSTANT.format(it.atStartOfDay(ZoneId.systemDefault())) } ?: "*"
            val to = date.tot?.let { DateTimeFormatter.ISO_INSTANT.format(it.atStartOfDay(ZoneId.systemDefault())) } ?: "*"
            "${dateField.veld}:[$from TO $to]"
        }

    private fun getFilterQueriesForZoekenParameters(zoekParameters: ZoekParameters): List<String> =
        zoekParameters.getZoeken().mapNotNull { (searchField, text) ->
            if (text.isNotBlank()) {
                val queryText = if (searchField == ZoekVeld.ZAAK_IDENTIFICATIE || searchField == ZoekVeld.TAAK_ZAAK_ID) {
                    "(*${encoded(text)}*)"
                } else {
                    "(${encoded(text)})"
                }
                "${searchField.veld}:$queryText"
            } else {
                null
            }
        }

    private fun applyAllowedZaaktypenPolicy(query: SolrQuery) {
        // signaleringen job does not have a logged-in user so check if logged-in user is present
        loggedInUserInstance.get()?.let { loggedInUser ->
            if (!loggedInUser.isAuthorisedForAllZaaktypen()) {
                val filterQuery = if (loggedInUser.geautoriseerdeZaaktypen.isNullOrEmpty()) {
                    "$ZAAKTYPE_OMSCHRIJVING_VELD:$NON_EXISTING_ZAAKTYPE"
                } else {
                    loggedInUser.geautoriseerdeZaaktypen.joinToString(
                        " OR "
                    ) { "$ZAAKTYPE_OMSCHRIJVING_VELD:${quoted(it)}" }
                }
                query.addFilterQuery(filterQuery)
            }
        }
    }
}
