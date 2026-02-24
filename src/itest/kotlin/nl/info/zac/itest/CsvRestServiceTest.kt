/*
 * SPDX-FileCopyrightText: 2024 INFO.nl
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
import nl.info.zac.itest.client.ZaakHelper
import nl.info.zac.itest.client.ZacClient
import nl.info.zac.itest.config.BEHEERDER_ELK_ZAAKTYPE
import nl.info.zac.itest.config.ItestConfiguration.ZAAKTYPE_TEST_1_UUID
import nl.info.zac.itest.config.ItestConfiguration.ZAAKTYPE_TEST_2_UUID
import nl.info.zac.itest.config.ItestConfiguration.ZAC_API_URI
import java.net.HttpURLConnection.HTTP_OK

const val CSV_FIELD_IDENTIFICATIE = "identificatie"
const val CSV_FIELD_AFGEHANDELD = "afgehandeld"
const val CSV_FIELD_ARCHIEF_ACTIE_DATUM = "archiefActiedatum"
const val CSV_FIELD_ARCHIEF_NOMINATIE = "archiefNominatie"
const val CSV_FIELD_OBJECT_ID = "objectId"

class CsvRestServiceTest : BehaviorSpec({
    val itestHttpClient = ItestHttpClient()
    val zaakHelper = ZaakHelper(ZacClient(itestHttpClient))
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
        CSV_FIELD_OBJECT_ID,
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

    Context("Export to CSV") {
        Given("Two open zaken that are indexed in Solr and a logged-in beheerder") {
            val (zaak1Identification, zaak1Uuid) = zaakHelper.createZaak(
                zaakDescription = "fakeZaak1Description",
                zaaktypeUuid = ZAAKTYPE_TEST_1_UUID,
                indexZaak = true,
                testUser = BEHEERDER_ELK_ZAAKTYPE
            )
            val (zaak2Identification, zaak2Uuid) = zaakHelper.createZaak(
                zaakDescription = "fakeZaak2Description",
                zaaktypeUuid = ZAAKTYPE_TEST_2_UUID,
                indexZaak = true,
                testUser = BEHEERDER_ELK_ZAAKTYPE
            )

            When(
                """a CSV export is requested for open zaken filtering on the zaak identifications
                    of the two created zaken, sorted by zaak identification"""
            ) {
                val response = itestHttpClient.performJSONPostRequest(
                    url = "$ZAC_API_URI/csv/export",
                    requestBodyAsString = """
                        {
                            "alleenMijnZaken": false,
                            "alleenOpenstaandeZaken": true,
                            "alleenAfgeslotenZaken": false,
                            "alleenMijnTaken": false,
                            "zoeken": {},
                            "filters": {
                                "ZAAK_IDENTIFICATIE": { "values": [ "$zaak1Identification", "$zaak2Identification" ] }
                            },
                            "datums": {},
                            "rows": 10,
                            "page": 0,
                            "type": "ZAAK",
                            "sorteerVeld": "ZAAK_IDENTIFICATIE",
                            "sorteerRichting": "asc"
                         }
                    """.trimIndent(),
                    testUser = BEHEERDER_ELK_ZAAKTYPE
                )

                Then(
                    """"
                 the response is a CSV string containing a header row followed by two rows
                 one for each open zaak
                """
                ) {
                    response.code shouldBe HTTP_OK
                    val responseBody = response.bodyAsString
                    logger.info { "Response: $responseBody" }

                    val csvReader = csvReader {
                        delimiter = ';'
                        // the value rows in the zaken export CSVs contains values other than are
                        // defined in the header row, so we convert them to empty strings
                        insufficientFieldsRowBehaviour = EMPTY_STRING
                    }
                    val csvRows = csvReader.readAll(responseBody)
                    csvRows.size shouldBe 1 + 2 // header row + 2 zaak rows
                    csvRows[0] shouldContainExactly headerRowFields
                    with(csvRows[1]) {
                        this[headerRowFields.indexOf(CSV_FIELD_IDENTIFICATIE)] shouldBe zaak1Identification
                        this[headerRowFields.indexOf(CSV_FIELD_OBJECT_ID)] shouldBe zaak1Uuid.toString()
                    }
                    with(csvRows[2]) {
                        this[headerRowFields.indexOf(CSV_FIELD_IDENTIFICATIE)] shouldBe zaak2Identification
                        this[headerRowFields.indexOf(CSV_FIELD_OBJECT_ID)] shouldBe zaak2Uuid.toString()
                    }
                    csvRows.filterIndexed { index, _ -> index > 0 }.forEach {
                        // because we have set the insufficientFieldsRowBehaviour in our CSV reader to
                        // set missing fields to empty strings, we expect the same number of fields
                        // as in our header row
                        // in reality we only have 36 fields in the value rows (strangely enough)
                        it.size shouldBe headerRowFields.size
                        it[headerRowFields.indexOf(CSV_FIELD_AFGEHANDELD)] shouldBe "Nee"
                        it[headerRowFields.indexOf(CSV_FIELD_ARCHIEF_ACTIE_DATUM)] shouldBe ""
                        it[headerRowFields.indexOf(CSV_FIELD_ARCHIEF_NOMINATIE)] shouldBe ""
                    }
                }
            }
        }
    }
})
