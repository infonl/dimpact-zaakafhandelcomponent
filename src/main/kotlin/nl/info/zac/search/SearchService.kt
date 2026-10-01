/*
 * SPDX-FileCopyrightText: 2022 Atos, 2024 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.search

import jakarta.enterprise.context.ApplicationScoped
import jakarta.enterprise.inject.Instance
import jakarta.inject.Inject
import nl.info.client.pabc.ROLE_NAME_ZAAKSPECIFIEK_GEAUTORISEERD
import nl.info.zac.authentication.LoggedInUser
import nl.info.zac.policy.PolicyService
import nl.info.zac.search.IndexingService.Companion.SOLR_CORE
import nl.info.zac.search.model.FilterParameters
import nl.info.zac.search.model.FilterResultaat
import nl.info.zac.search.model.FilterVeld
import nl.info.zac.search.model.FilterWaarde
import nl.info.zac.search.model.SorteerVeld
import nl.info.zac.search.model.ZoekParameters
import nl.info.zac.search.model.ZoekResultaat
import nl.info.zac.search.model.ZoekVeld
import nl.info.zac.search.model.zoekobject.ZoekObject
import nl.info.zac.search.model.zoekobject.ZoekObjectType
import nl.info.zac.shared.model.SorteerRichting
import nl.info.zac.solr.SolrClientFactory
import nl.info.zac.solr.encoded
import nl.info.zac.solr.quoted
import nl.info.zac.util.AllOpen
import nl.info.zac.util.NoArgConstructor
import org.apache.solr.client.solrj.SolrClient
import org.apache.solr.client.solrj.SolrQuery
import org.apache.solr.client.solrj.SolrRequest
import org.apache.solr.client.solrj.SolrServerException
import org.apache.solr.common.params.SimpleParams
import java.io.IOException
import java.time.ZoneId
import java.time.format.DateTimeFormatter.ISO_INSTANT

@ApplicationScoped
@AllOpen
@NoArgConstructor
class SearchService @Inject constructor(
    private val loggedInUserInstance: Instance<LoggedInUser>,
    private val policyService: PolicyService,
    solrClientFactory: SolrClientFactory
) {
    companion object {
        private lateinit var solrClient: SolrClient

        private val NON_EXISTING_ZAAKTYPE = quoted("-NON-EXISTING-ZAAKTYPE-")
        private const val ZAAKTYPE_OMSCHRIJVING_VELD = "zaaktypeOmschrijving"

        // The terms query parser does not support escaping, and a zaaktype omschrijving may contain the default
        // separator, a comma, but is not expected to contain a newline.
        private const val TERMS_SEPARATOR = "\n"

        // A request parameter rather than inline values, because inside a boolean query the end of a nested
        // terms query's values cannot be told apart from the rest of the query.
        private const val ZAAKTYPEN_ZONDER_ZAAKSPECIFIEK_GEAUTORISEERD_PARAMETER =
            "zaaktypenZonderZaakspecifiekGeautoriseerd"
    }

    init {
        solrClient = solrClientFactory.createSolrClient(SOLR_CORE)
    }

    @Suppress("LongMethod")
    fun search(zoekParameters: ZoekParameters): ZoekResultaat<out ZoekObject> {
        val query = SolrQuery("*:*")
        getAllowedZaaktypenFilterQuery()?.let(query::addFilterQuery)
        addZaakspecifiekGeautoriseerdFilterQuery(query)
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
                if (zoekParameters.sortering.richting == SorteerRichting.DESCENDING) {
                    SolrQuery.ORDER.desc
                } else {
                    SolrQuery.ORDER.asc
                }
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

        return solrSearch(query)
    }

    private fun solrSearch(query: SolrQuery): ZoekResultaat<out ZoekObject> {
        try {
            // POST, because the filter queries for a user with many zaaktypen exceed the maximum header size of a GET
            val response = solrClient.query(query, SolrRequest.METHOD.POST)
            val zoekObjecten = response.results
                .map {
                    val zoekObjectType = ZoekObjectType.valueOf(it["type"].toString())
                    solrClient.binder.getBean(zoekObjectType.zoekObjectClass, it)
                }
            val zoekResultaat = ZoekResultaat(zoekObjecten, response.results.numFound)
            response.facetFields?.forEach { facetField ->
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
            val from = date.van?.let {
                ISO_INSTANT.format(it.atStartOfDay(ZoneId.systemDefault()))
            } ?: "*"
            val to = date.tot?.let {
                ISO_INSTANT.format(it.atStartOfDay(ZoneId.systemDefault()))
            } ?: "*"
            "${dateField.veld}:[$from TO $to]"
        }

    private fun getFilterQueriesForZoekenParameters(zoekParameters: ZoekParameters): List<String> =
        zoekParameters.getZoeken().mapNotNull { (searchField, text) ->
            if (text.isNotBlank()) {
                val queryText = if (
                    searchField == ZoekVeld.ZAAK_IDENTIFICATIE || searchField == ZoekVeld.TAAK_ZAAK_ID
                ) {
                    "(*${encoded(text)}* OR *${encoded(text.uppercase())}* OR *${encoded(text.lowercase())})"
                } else {
                    "(${encoded(text)})"
                }
                "${searchField.veld}:$queryText"
            } else {
                null
            }
        }

    // Mirrors OPA's `lezen` rules, which evaluate the roles for the zaaktype together with the overall roles.
    private fun getAllowedZaaktypenFilterQuery(): String? =
        loggedInUserInstance.get()?.let { loggedInUser ->
            val leesrollen = policyService.readLeesrollen()
            val allowedZaaktypen = loggedInUser.applicationRolesPerZaaktype
                .filterValues { roles -> (roles + loggedInUser.overallRoles).any(leesrollen::contains) }
                .keys
            if (allowedZaaktypen.isEmpty()) {
                "$ZAAKTYPE_OMSCHRIJVING_VELD:$NON_EXISTING_ZAAKTYPE"
            } else {
                "{!terms f=$ZAAKTYPE_OMSCHRIJVING_VELD separator='$TERMS_SEPARATOR'}" +
                    allowedZaaktypen.joinToString(TERMS_SEPARATOR)
            }
        }

    // Excludes zaakspecifiek geautoriseerde rows for zaaktypen the user holds a role for but not the
    // zaakspecifiek_geautoriseerd flag; adds nothing when every allowed zaaktype has the flag (or none is
    // allowed). Mirrors OPA's `user.rollen`, which also grants the flag for every zaaktype once it is held
    // as an overall role (i.e. one not scoped to a specific zaaktype), and OPA's exception for medewerkers
    // individually authorised for a zaak.
    private fun addZaakspecifiekGeautoriseerdFilterQuery(query: SolrQuery) {
        val loggedInUser = loggedInUserInstance.get() ?: return
        val zaaktypenWithoutFlag = getZaaktypenWithoutZaakspecifiekGeautoriseerd(loggedInUser)
        if (zaaktypenWithoutFlag.isNotEmpty()) {
            query.set(
                ZAAKTYPEN_ZONDER_ZAAKSPECIFIEK_GEAUTORISEERD_PARAMETER,
                zaaktypenWithoutFlag.joinToString(TERMS_SEPARATOR)
            )
            query.addFilterQuery(
                "-({!terms f=$ZAAKTYPE_OMSCHRIJVING_VELD separator='$TERMS_SEPARATOR' " +
                    "v=\$$ZAAKTYPEN_ZONDER_ZAAKSPECIFIEK_GEAUTORISEERD_PARAMETER} " +
                    "AND ${ZoekObject.ZAAKSPECIFIEK_GEAUTORISEERD_FIELD}:true " +
                    "AND -${ZoekObject.ZAAK_GEAUTORISEERDE_MEDEWERKERS_FIELD}:${quoted(loggedInUser.id)})"
            )
        }
    }

    private fun getZaaktypenWithoutZaakspecifiekGeautoriseerd(loggedInUser: LoggedInUser) =
        if (ROLE_NAME_ZAAKSPECIFIEK_GEAUTORISEERD in loggedInUser.overallRoles) {
            emptySet()
        } else {
            loggedInUser.applicationRolesPerZaaktype
                .filterValues { roles -> ROLE_NAME_ZAAKSPECIFIEK_GEAUTORISEERD !in roles }
                .keys
        }
}
