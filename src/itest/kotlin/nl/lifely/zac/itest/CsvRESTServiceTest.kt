package nl.lifely.zac.itest

import com.github.doyaaaaaken.kotlincsv.dsl.context.InsufficientFieldsRowBehaviour.EMPTY_STRING
import com.github.doyaaaaaken.kotlincsv.dsl.csvReader
import io.github.oshai.kotlinlogging.KotlinLogging
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import nl.lifely.zac.itest.client.ItestHttpClient
import nl.lifely.zac.itest.config.ItestConfiguration.TEST_SPEC_ORDER_AFTER_TASK_COMPLETED
import nl.lifely.zac.itest.config.ItestConfiguration.ZAAK_1_IDENTIFICATION
import nl.lifely.zac.itest.config.ItestConfiguration.ZAAK_2_IDENTIFICATION
import nl.lifely.zac.itest.config.ItestConfiguration.ZAC_API_URI
import org.junit.jupiter.api.Order
import org.mockserver.model.HttpStatusCode

const val CSV_ROWS_EXPECTED = 3
const val CSV_FIELD_IDENTIFICATIE = "identificatie"
const val CSV_FIELD_AFGEHANDELD = "afgehandeld"
const val CSV_FIELD_ARCHIEF_ACTIE_DATUM = "archiefActiedatum"
const val CSV_FIELD_ARCHIEF_NOMINATIE = "archiefNominatie"

@Order(TEST_SPEC_ORDER_AFTER_TASK_COMPLETED)
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
        "bagObjectIDs",
        "behandelaarGebruikersnaam"
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
                response.code shouldBe HttpStatusCode.OK_200.code()
                val responseBody = response.body!!.string()
                logger.info { "Response: $responseBody" }

                val csvReader = csvReader {
                    delimiter = ';'
                    // the value rows in the zaken export CSVs contains values than are
                    // defined in the header row, so we convert them to empty strings
                    insufficientFieldsRowBehaviour = EMPTY_STRING
                }
                val csvRows = csvReader.readAll(responseBody)
                logger.info { "CSV rows: $csvRows" }
                // since we run this test after IndexerenRESTServiceTest, we expect only
                // two zaken to be present in the search index which is used to generate the CSV
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
                // the order of the zaken in the search query used to generate the CSV
                // should always be the same
                csvRows[1].run {
                    get(headerRowFields.indexOf(CSV_FIELD_IDENTIFICATIE)) shouldBe ZAAK_2_IDENTIFICATION
                }
                csvRows[2].run {
                    get(headerRowFields.indexOf(CSV_FIELD_IDENTIFICATIE)) shouldBe ZAAK_1_IDENTIFICATION
                }
            }
        }
    }
})
