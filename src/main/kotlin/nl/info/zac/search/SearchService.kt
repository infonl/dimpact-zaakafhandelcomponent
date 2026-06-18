/*
 * SPDX-FileCopyrightText: 2022 Atos, 2024 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.search

import co.elastic.clients.elasticsearch.ElasticsearchClient
import co.elastic.clients.elasticsearch._types.FieldValue
import co.elastic.clients.elasticsearch._types.SortOrder
import co.elastic.clients.elasticsearch._types.aggregations.Aggregate
import co.elastic.clients.elasticsearch._types.aggregations.Aggregation
import co.elastic.clients.elasticsearch._types.mapping.FieldType
import co.elastic.clients.elasticsearch._types.query_dsl.Operator
import co.elastic.clients.elasticsearch._types.query_dsl.Query
import co.elastic.clients.elasticsearch.core.SearchRequest
import co.elastic.clients.json.JsonData
import com.fasterxml.jackson.databind.ObjectMapper
import jakarta.enterprise.context.ApplicationScoped
import jakarta.enterprise.inject.Instance
import jakarta.inject.Inject
import nl.info.zac.authentication.LoggedInUser
import nl.info.zac.search.elasticsearch.ElasticsearchClientProducer
import nl.info.zac.search.elasticsearch.SearchIndex
import nl.info.zac.search.model.DatumVeld
import nl.info.zac.search.model.FilterParameters
import nl.info.zac.search.model.FilterResultaat
import nl.info.zac.search.model.FilterVeld
import nl.info.zac.search.model.FilterWaarde
import nl.info.zac.search.model.SorteerVeld
import nl.info.zac.search.model.ZoekParameters
import nl.info.zac.search.model.ZoekResultaat
import nl.info.zac.search.model.ZoekVeld
import nl.info.zac.search.model.zoekobject.ZaakZoekObject
import nl.info.zac.search.model.zoekobject.ZoekObject
import nl.info.zac.shared.model.SorteerRichting
import nl.info.zac.util.AllOpen
import nl.info.zac.util.NoArgConstructor
import java.time.ZoneId
import java.time.format.DateTimeFormatter.ISO_INSTANT
import java.util.logging.Logger

@ApplicationScoped
@AllOpen
@NoArgConstructor
@Suppress("TooManyFunctions")
class SearchService @Inject constructor(
    private val loggedInUserInstance: Instance<LoggedInUser>,
    private val elasticsearchClient: ElasticsearchClient
) {
    companion object {
        private val LOG = Logger.getLogger(SearchService::class.java.name)
        private val objectMapper: ObjectMapper = ElasticsearchClientProducer.createObjectMapper()

        private const val NON_EXISTING_ZAAKTYPE = "-NON-EXISTING-ZAAKTYPE-"
        private const val ZAAKTYPE_OMSCHRIJVING_VELD = "zaaktypeOmschrijving"
        private const val FACET_VALUES_AGGREGATION = "values"

        private val ZAAK_IDENTIFICATIE_SEARCH_FIELDS = setOf(ZoekVeld.ZAAK_IDENTIFICATIE, ZoekVeld.TAAK_ZAAK_ID)
    }

    fun zoek(zoekParameters: ZoekParameters): ZoekResultaat<out ZoekObject> {
        // Filters that constrain both the results and the facet counts (Solr untagged filter queries).
        val baseFilters = buildBaseFilters(zoekParameters)
        val baseMatches = buildZoekMatches(zoekParameters)

        // Selected facet values are applied as a post filter so that they do not collapse the facet counts;
        // each facet aggregation re-applies all selected values except its own (independent facet filtering).
        val facetSelections = buildFacetSelections(zoekParameters)

        val request = SearchRequest.Builder()
            .index(SearchIndex.ALL_INDEX_NAMES)
            .from(zoekParameters.start)
            .size(zoekParameters.rows)
            .query { query ->
                query.bool { bool ->
                    bool.must(baseMatches)
                    bool.filter(baseFilters)
                }
            }
            .apply {
                if (facetSelections.isNotEmpty()) {
                    postFilter { pf -> pf.bool { bool -> bool.filter(facetSelections.values.toList()) } }
                }
            }
            .aggregations(buildAggregations(zoekParameters, facetSelections))
            .sort(buildSortOptions(zoekParameters))
            .build()

        return executeSearch(request)
    }

    private fun executeSearch(request: SearchRequest): ZoekResultaat<out ZoekObject> {
        try {
            val response = elasticsearchClient.search(request, Map::class.java)
            val zoekObjecten = response.hits().hits().mapNotNull { hit ->
                hit.source()?.let { source ->
                    @Suppress("UNCHECKED_CAST")
                    toZoekObject(hit.index(), source as Map<String, Any?>)
                }
            }
            val count = response.hits().total()?.value() ?: zoekObjecten.size.toLong()
            val zoekResultaat = ZoekResultaat(zoekObjecten, count)
            response.aggregations().forEach { (field, aggregate) ->
                val facetVeld = FilterVeld.fromValue(field)
                val values = extractFacetBuckets(aggregate)
                    .filter { it.second > 0 }
                    .map { FilterResultaat(it.first, it.second) }
                zoekResultaat.addFilter(facetVeld, values.toMutableList())
            }
            return zoekResultaat
        } catch (exception: Exception) {
            throw SearchException("Failed to perform Elasticsearch search query", exception)
        }
    }

    private fun toZoekObject(indexName: String, source: Map<String, Any?>): ZoekObject {
        val objectType = SearchIndex.objectTypeForIndexName(indexName)
        val zoekObject = objectMapper.convertValue(source, objectType.zoekObjectClass)
        if (zoekObject is ZaakZoekObject) {
            source.entries
                .filter { it.key.startsWith(ZaakZoekObject.ZAAK_BETROKKENE_PREFIX) }
                .forEach { (field, value) ->
                    val rol = field.removePrefix(ZaakZoekObject.ZAAK_BETROKKENE_PREFIX)
                    asStringList(value).forEach { zoekObject.addBetrokkene(rol, it) }
                }
        }
        return zoekObject
    }

    /**
     * Untagged filters from the Solr query: the allowed-zaaktypen policy, the optional object-type filter,
     * date-range filters and the extra exact filter queries. These restrict both results and facet counts.
     */
    private fun buildBaseFilters(zoekParameters: ZoekParameters): List<Query> {
        val filters = mutableListOf<Query>()
        buildAllowedZaaktypenFilter()?.let(filters::add)
        zoekParameters.type?.let { type ->
            filters.add(termQuery(FilterVeld.TYPE.veld, type.toString()))
        }
        filters.addAll(buildDatumFilters(zoekParameters))
        zoekParameters.getFilterQueries().forEach { (veld, waarde) -> filters.add(termQuery(veld, waarde)) }
        return filters
    }

    private fun buildZoekMatches(zoekParameters: ZoekParameters): List<Query> =
        zoekParameters.getZoeken().mapNotNull { (searchField, text) ->
            if (text.isBlank()) {
                null
            } else if (searchField in ZAAK_IDENTIFICATIE_SEARCH_FIELDS) {
                // case-insensitive substring match, mirroring the Solr wildcard search on the identificatie
                Query.of { query ->
                    query.wildcard { wildcard ->
                        wildcard.field(searchField.veld).value("*$text*").caseInsensitive(true)
                    }
                }
            } else {
                Query.of { query ->
                    query.match { match -> match.field(searchField.veld).query(text).operator(Operator.And) }
                }
            }
        }

    private fun buildDatumFilters(zoekParameters: ZoekParameters): List<Query> =
        zoekParameters.datums.map { (dateField: DatumVeld, range) ->
            Query.of { query ->
                query.range { rangeQuery ->
                    rangeQuery.untyped { untyped ->
                        untyped.field(dateField.veld)
                        range.van?.let { untyped.gte(JsonData.of(ISO_INSTANT.format(it.atStartOfDay(ZoneId.systemDefault())))) }
                        range.tot?.let { untyped.lte(JsonData.of(ISO_INSTANT.format(it.atStartOfDay(ZoneId.systemDefault())))) }
                        untyped
                    }
                }
            }
        }

    private fun buildFacetSelections(zoekParameters: ZoekParameters): Map<FilterVeld, Query> =
        zoekParameters.getFilters()
            .filterValues { it.values.isNotEmpty() }
            .mapValues { (filter, filterParameters) -> buildFacetSelectionQuery(filter, filterParameters) }

    private fun buildFacetSelectionQuery(filter: FilterVeld, filterParameters: FilterParameters): Query {
        val special = filterParameters.values.singleOrNull()
        return when {
            FilterWaarde.LEEG.isEqualTo(special) ->
                Query.of { query -> query.bool { bool -> bool.mustNot(existsQuery(filter.veld)) } }
            FilterWaarde.NIET_LEEG.isEqualTo(special) -> existsQuery(filter.veld)
            else -> {
                val termsQuery = termsQuery(filter.veld, filterParameters.values)
                if (filterParameters.inverse) {
                    Query.of { query -> query.bool { bool -> bool.mustNot(termsQuery) } }
                } else {
                    termsQuery
                }
            }
        }
    }

    /**
     * One aggregation per available facet field. Each is a `filter` aggregation that re-applies every selected
     * facet value except its own field's, with a `terms` sub-aggregation producing the bucket counts. This
     * reproduces Solr's tagged-filter exclusion so that a facet's own selection does not collapse its options.
     */
    private fun buildAggregations(
        zoekParameters: ZoekParameters,
        facetSelections: Map<FilterVeld, Query>
    ): Map<String, Aggregation> =
        zoekParameters.getFilters().keys.associate { filter ->
            val otherSelections = facetSelections.filterKeys { it != filter }.values.toList()
            val aggregation = Aggregation.of { aggregationBuilder ->
                aggregationBuilder
                    .filter { filterAggregation ->
                        filterAggregation.bool { bool -> bool.filter(otherSelections) }
                    }
                    .aggregations(FACET_VALUES_AGGREGATION) { sub ->
                        sub.terms { terms ->
                            terms.field(filter.veld).size(Int.MAX_VALUE)
                            // include a bucket for documents without a value when not searching globally
                            if (!zoekParameters.isGlobaalZoeken() && filter != FilterVeld.TOEGEKEND) {
                                terms.missing(FilterWaarde.LEEG.toString())
                            }
                            terms
                        }
                    }
            }
            filter.veld to aggregation
        }

    private fun buildSortOptions(zoekParameters: ZoekParameters): List<co.elastic.clients.elasticsearch._types.SortOptions> {
        val sortOptions = mutableListOf<co.elastic.clients.elasticsearch._types.SortOptions>()
        val sorteerVeld = zoekParameters.sortering.sorteerVeld
        if (zoekParameters.sortering.richting != SorteerRichting.NONE) {
            val order = if (zoekParameters.sortering.richting == SorteerRichting.DESCENDING) {
                SortOrder.Desc
            } else {
                SortOrder.Asc
            }
            sortOptions.add(sortOption(sorteerVeld, order))
        }
        if (sorteerVeld != SorteerVeld.CREATED) {
            sortOptions.add(sortOption(SorteerVeld.CREATED, SortOrder.Desc))
        }
        if (sorteerVeld != SorteerVeld.ZAAK_IDENTIFICATIE) {
            sortOptions.add(sortOption(SorteerVeld.ZAAK_IDENTIFICATIE, SortOrder.Desc))
        }
        // sort on 'id' so that results from the same query always have the same order
        sortOptions.add(
            co.elastic.clients.elasticsearch._types.SortOptions.of { options ->
                options.field { field -> field.field("id").order(SortOrder.Desc).unmappedType(FieldType.Keyword) }
            }
        )
        return sortOptions
    }

    private fun sortOption(
        sorteerVeld: SorteerVeld,
        order: SortOrder
    ): co.elastic.clients.elasticsearch._types.SortOptions {
        val (field, unmappedType) = sortFieldAndType(sorteerVeld)
        return co.elastic.clients.elasticsearch._types.SortOptions.of { options ->
            options.field { fieldSort -> fieldSort.field(field).order(order).unmappedType(unmappedType) }
        }
    }

    /**
     * Resolves a [SorteerVeld] to the Elasticsearch field to sort on plus the type used when the field is
     * absent in one of the (multi-index) targets. Analyzed text fields are sorted on their `keyword` sub-field.
     */
    @Suppress("CyclomaticComplexMethod")
    private fun sortFieldAndType(sorteerVeld: SorteerVeld): Pair<String, FieldType> =
        when (sorteerVeld) {
            SorteerVeld.ZAAK_OMSCHRIJVING -> "zaak_omschrijving.keyword" to FieldType.Keyword
            SorteerVeld.ZAAK_TOELICHTING -> "zaak_toelichting.keyword" to FieldType.Keyword
            SorteerVeld.CREATED,
            SorteerVeld.SIGNALERING_TIJDSTIP,
            SorteerVeld.ZAAK_REGISTRATIEDATUM,
            SorteerVeld.ZAAK_STARTDATUM,
            SorteerVeld.ZAAK_STREEFDATUM,
            SorteerVeld.ZAAK_EINDDATUM,
            SorteerVeld.ZAAK_FATALE_DATUM,
            SorteerVeld.ZAAK_ARCHIEF_ACTIEDATUM,
            SorteerVeld.TAAK_CREATIEDATUM,
            SorteerVeld.TAAK_TOEKENNINGSDATUM,
            SorteerVeld.TAAK_FATALEDATUM,
            SorteerVeld.INFORMATIEOBJECT_CREATIEDATUM,
            SorteerVeld.INFORMATIEOBJECT_REGISTRATIEDATUM,
            SorteerVeld.INFORMATIEOBJECT_ONTVANGSTDATUM,
            SorteerVeld.INFORMATIEOBJECT_VERZENDDATUM -> sorteerVeld.veld to FieldType.Date
            SorteerVeld.ZAAK_AANTAL_OPENSTAANDE_TAKEN -> sorteerVeld.veld to FieldType.Integer
            SorteerVeld.ZAAK_INDICATIES_SORT,
            SorteerVeld.INFORMATIEOBJECT_INDICATIES_SORT -> sorteerVeld.veld to FieldType.Long
            else -> sorteerVeld.veld to FieldType.Keyword
        }

    private fun buildAllowedZaaktypenFilter(): Query? =
        // the signaleringen job does not have a logged-in user, so check if a logged-in user is present
        loggedInUserInstance.get()?.let { loggedInUser ->
            val allowedZaaktypen = loggedInUser.applicationRolesPerZaaktype.keys
            if (allowedZaaktypen.isEmpty()) {
                termQuery(ZAAKTYPE_OMSCHRIJVING_VELD, NON_EXISTING_ZAAKTYPE)
            } else {
                termsQuery(ZAAKTYPE_OMSCHRIJVING_VELD, allowedZaaktypen.toList())
            }
        }

    private fun termQuery(field: String, value: String): Query =
        Query.of { query -> query.term { term -> term.field(field).value(FieldValue.of(value)) } }

    private fun termsQuery(field: String, values: List<String>): Query =
        Query.of { query ->
            query.terms { terms ->
                terms.field(field).terms { termsField -> termsField.value(values.map { FieldValue.of(it) }) }
            }
        }

    private fun existsQuery(field: String): Query =
        Query.of { query -> query.exists { exists -> exists.field(field) } }

    private fun extractFacetBuckets(aggregate: Aggregate): List<Pair<String, Long>> {
        val values = aggregate.filter().aggregations()[FACET_VALUES_AGGREGATION] ?: return emptyList()
        return when {
            values.isSterms -> values.sterms().buckets().array().map { it.key().stringValue() to it.docCount() }
            values.isLterms -> values.lterms().buckets().array().map {
                (it.keyAsString() ?: it.key().toString()) to it.docCount()
            }
            values.isDterms -> values.dterms().buckets().array().map {
                (it.keyAsString() ?: it.key().toString()) to it.docCount()
            }
            else -> emptyList()
        }
    }

    private fun asStringList(value: Any?): List<String> =
        when (value) {
            null -> emptyList()
            is Collection<*> -> value.mapNotNull { it?.toString() }
            else -> listOf(value.toString())
        }
}
