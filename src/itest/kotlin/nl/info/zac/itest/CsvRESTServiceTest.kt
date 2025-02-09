/*
 * SPDX-FileCopyrightText: 2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.itest

import com.github.doyaaaaaken.kotlincsv.dsl.context.InsufficientFieldsRowBehaviour.EMPTY_STRING
import com.github.doyaaaaaken.kotlincsv.dsl.csvReader
import io.github.oshai.kotlinlogging.KotlinLogging
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import nl.info.zac.itest.client.ItestHttpClient
import nl.info.zac.itest.config.ItestConfiguration.HTTP_STATUS_OK
import nl.info.zac.itest.config.ItestConfiguration.TEST_SPEC_ORDER_AFTER_REINDEXING
import nl.info.zac.itest.config.ItestConfiguration.TOTAL_COUNT_ZAKEN
import nl.info.zac.itest.config.ItestConfiguration.TOTAL_COUNT_ZAKEN_AFGEROND
import nl.info.zac.itest.config.ItestConfiguration.ZAC_API_URI
import org.junit.jupiter.api.Order

/**
 * Since we run this test after [IndexerenRESTServiceTest], we expect
 * all created and still open zaken up to that point to be present in the search index
 * which is used to generate the CSV.
 * The number of CSV rows is expected to be equal to the number of open zaken + 1 for the header row.
 */
const val CSV_ROWS_EXPECTED = TOTAL_COUNT_ZAKEN - TOTAL_COUNT_ZAKEN_AFGEROND + 1

const val CSV_FIELD_IDENTIFICATIE = "identificatie"
const val CSV_FIELD_AFGEHANDELD = "afgehandeld"
const val CSV_FIELD_ARCHIEF_ACTIE_DATUM = "archiefActiedatum"
const val CSV_FIELD_ARCHIEF_NOMINATIE = "archiefNominatie"

@Order(TEST_SPEC_ORDER_AFTER_REINDEXING)
@Suppress("MagicNumber")
class CsvRESTServiceTest : BehaviorSpec({
    val itestHttpClient = ItestHttpClient()
    val logger = KotlinLogging.logger {}

    val headerRowFields = listOf(
        "aantalOpenstaandeTaken",
        CSV_FIELD_AFGEHANDELD,
        CSV_FIELD_ARCHIEF_ACTIE_DATUM,
        CSV_FIELD_ARCHIEF_NOMINATIE,
        "bagObjectIDs",
        "behandelaarGebruikersnaam",
        "behandelaarNaam",
        "betrokkenen",
        "communicatiekanaal",
        "duurVerlenging",
        "einddatum",
        "einddatumGepland",
        "groepID",
        "groepNaam",
        CSV_FIELD_IDENTIFICATIE,
        "initiatorIdentificatie",
        "locatie",
        "objectId",
        "omschrijving",
        "publicatiedatum",
        "redenOpschorting",
        "redenVerlenging",
        "registratiedatum",
        "resultaatToelichting",
        "resultaattypeOmschrijving",
        "startdatum",
        "statusDatumGezet",
        "statusEindstatus",
        "statusToelichting",
        "statustypeOmschrijving",
        "toegekend",
        "toelichting",
        "uiterlijkeEinddatumAfdoening",
        "vertrouwelijkheidaanduiding",
        "zaakIndicaties",
        "zaaktypeOmschrijving",
        "aantalOpenstaandeTaken",
        "afgehandeld",
        "archiefActiedatum",
        "archiefNominatie",
        "bagObjectIDs"
    )

    Given("Two open zaken") {

        When("all the open zaken are exported") {
            val response = itestHttpClient.performJSONPostRequest(
                url = "$ZAC_API_URI/csv/export",
                requestBodyAsString = """
                   {
                    "filtersType":"ZoekParameters",
                    "alleenMijnZaken":false,
                    "alleenOpenstaandeZaken":true,
                    "alleenAfgeslotenZaken":false,
                    "alleenMijnTaken":false,
                    "zoeken":{},
                    "filters":{},
                    "datums":{},
                    "rows":10,
                    "page":0,
                    "type":"ZAAK"
                    }
                """.trimIndent()
            )

            Then(
                """"
                 the response is a CSV string containing a header row followed by two rows
                 one for each open zaak
                """
            ) {
                response.code shouldBe HTTP_STATUS_OK
                val responseBody = response.body!!.string()
                logger.info { "Response: $responseBody" }

                val csvReader = csvReader {
                    delimiter = ';'
                    // the value rows in the zaken export CSVs contains values other than are
                    // defined in the header row, so we convert them to empty strings
                    insufficientFieldsRowBehaviour = EMPTY_STRING
                }
                val csvRows = csvReader.readAll(responseBody)
                logger.info { "CSV rows: $csvRows" }
                csvRows.size shouldBe CSV_ROWS_EXPECTED
                csvRows[0] shouldContainExactly headerRowFields
                csvRows.filterIndexed { index, _ -> index > 0 }.forEach {
                    // because we have set the insufficientFieldsRowBehaviour in our CSV reader to
                    // set missing fields to empty strings, we expect the same number of fields
                    // as in our header row
                    // in reality we only have 36 fields in the value rows (strangely enough)
                    it.size shouldBe headerRowFields.size
                    // note that we do not check the 'aantalOpenstaandeTaken' field
                    // because currently sometimes this data may not have been indexed by Solr just yet
                    it[headerRowFields.indexOf(CSV_FIELD_AFGEHANDELD)] shouldBe "Nee"
                    it[headerRowFields.indexOf(CSV_FIELD_ARCHIEF_ACTIE_DATUM)] shouldBe ""
                    it[headerRowFields.indexOf(CSV_FIELD_ARCHIEF_NOMINATIE)] shouldBe ""
                    // checking other fields is left for the future since
                    // the CSV export functionality is expected to change quite a bit
                }
            }
        }
    }
})
