/*
 * SPDX-FileCopyrightText: 2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.lifely.zac.itest

import io.github.oshai.kotlinlogging.KotlinLogging
import io.kotest.core.spec.Order
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import nl.lifely.zac.itest.client.ItestHttpClient
import nl.lifely.zac.itest.config.ItestConfiguration.COMMUNICATIEKANAAL_TEST_1
import nl.lifely.zac.itest.config.ItestConfiguration.DATE_2024_01_01
import nl.lifely.zac.itest.config.ItestConfiguration.DATE_2024_01_31
import nl.lifely.zac.itest.config.ItestConfiguration.DOCUMENT_VERTROUWELIJKHEIDS_AANDUIDING_OPENBAAR
import nl.lifely.zac.itest.config.ItestConfiguration.OPEN_FORMULIEREN_PRODUCTAANVRAAG_FORMULIER_1_BRON_KENMERK
import nl.lifely.zac.itest.config.ItestConfiguration.OPEN_FORMULIEREN_PRODUCTAANVRAAG_FORMULIER_2_BRON_KENMERK
import nl.lifely.zac.itest.config.ItestConfiguration.TEST_GROUP_A_DESCRIPTION
import nl.lifely.zac.itest.config.ItestConfiguration.TEST_GROUP_A_ID
import nl.lifely.zac.itest.config.ItestConfiguration.TEST_KVK_VESTIGINGSNUMMER_1
import nl.lifely.zac.itest.config.ItestConfiguration.TEST_PERSON_2_BSN
import nl.lifely.zac.itest.config.ItestConfiguration.TEST_PERSON_3_BSN
import nl.lifely.zac.itest.config.ItestConfiguration.TEST_PERSON_HENDRIKA_JANSE_BSN
import nl.lifely.zac.itest.config.ItestConfiguration.TEST_SPEC_ORDER_AFTER_TASK_COMPLETED
import nl.lifely.zac.itest.config.ItestConfiguration.ZAAKTYPE_INDIENEN_AANSPRAKELIJKSTELLING_DOOR_DERDEN_BEHANDELEN_DESCRIPTION
import nl.lifely.zac.itest.config.ItestConfiguration.ZAAKTYPE_MELDING_KLEIN_EVENEMENT_DESCRIPTION
import nl.lifely.zac.itest.config.ItestConfiguration.ZAAK_DESCRIPTION_1
import nl.lifely.zac.itest.config.ItestConfiguration.ZAAK_DESCRIPTION_2
import nl.lifely.zac.itest.config.ItestConfiguration.ZAAK_MANUAL_1_IDENTIFICATION
import nl.lifely.zac.itest.config.ItestConfiguration.ZAAK_PRODUCTAANVRAAG_1_IDENTIFICATION
import nl.lifely.zac.itest.config.ItestConfiguration.ZAAK_PRODUCTAANVRAAG_1_START_DATE
import nl.lifely.zac.itest.config.ItestConfiguration.ZAAK_PRODUCTAANVRAAG_2_IDENTIFICATION
import nl.lifely.zac.itest.config.ItestConfiguration.ZAAK_PRODUCTAANVRAAG_2_START_DATE
import nl.lifely.zac.itest.config.ItestConfiguration.ZAAK_PRODUCTAANVRAAG_2_UITERLIJKE_EINDDATUM_AFDOENING
import nl.lifely.zac.itest.config.ItestConfiguration.ZAC_API_URI
import nl.lifely.zac.itest.config.ItestConfiguration.zaakManual2Identification
import nl.lifely.zac.itest.util.shouldEqualJsonIgnoringExtraneousFields

@Order(TEST_SPEC_ORDER_AFTER_TASK_COMPLETED)
class ZoekenRESTServiceTest : BehaviorSpec({
    val itestHttpClient = ItestHttpClient()
    val logger = KotlinLogging.logger {}

    Given("""Multiple zaken have been created and are indexed""") {
        When(
            """the search endpoint is called to search for all objects of type 'ZAAK'"""
        ) {
            val response = itestHttpClient.performPutRequest(
                url = "$ZAC_API_URI/zoeken/list",
                requestBodyAsString = """
                   {
                    "filtersType": "ZoekParameters",
                    "alleenMijnZaken": false,
                    "alleenOpenstaandeZaken": true,
                    "alleenAfgeslotenZaken": false,
                    "alleenMijnTaken": false,
                    "zoeken": {},
                    "filters": {},
                    "datums": {},
                    "rows": 10,
                    "page":0,
                    "type":"ZAAK"
                    }
                """.trimIndent()
            )
            Then(
                """the response is successful and the search results include the indexed zaken"""
            ) {
                val responseBody = response.body!!.string()
                logger.info { "Response: $responseBody" }
                response.isSuccessful shouldBe true
                responseBody shouldEqualJsonIgnoringExtraneousFields """
                {
                  "foutmelding" : "",
                  "resultaten" : [ {
                    "identificatie" : "$zaakManual2Identification",
                    "type" : "ZAAK",
                    "aantalOpenstaandeTaken" : 1,
                    "afgehandeld" : false,
                    "betrokkenen" : {
                      "Behandelaar" : [ "$TEST_GROUP_A_ID" ]
                    },
                    "communicatiekanaal" : "$COMMUNICATIEKANAAL_TEST_1",
                    "groepId" : "$TEST_GROUP_A_ID",
                    "groepNaam" : "$TEST_GROUP_A_DESCRIPTION",
                    "indicatieDeelzaak" : false,
                    "indicatieHeropend" : false,
                    "indicatieHoofdzaak" : false,
                    "indicatieOpschorting" : false,
                    "indicatieVerlenging" : false,
                    "indicaties" : [ ],
                    "omschrijving" : "$ZAAK_DESCRIPTION_1",
                    "rechten" : {
                      "afbreken" : true,
                      "behandelen" : true,
                      "bekijkenZaakdata" : true,
                      "creeerenDocument" : true,
                      "heropenen" : true,
                      "lezen" : true,
                      "toekennen" : true,
                      "toevoegenBagObject" : true,
                      "toevoegenBetrokkeneBedrijf" : true,
                      "toevoegenBetrokkenePersoon" : true,
                      "toevoegenInitiatorBedrijf" : true,
                      "toevoegenInitiatorPersoon" : true,
                      "versturenEmail" : true,
                      "versturenOntvangstbevestiging" : true,
                      "verwijderenBetrokkene" : true,
                      "verwijderenInitiator" : true,
                      "wijzigen" : true,
                      "wijzigenDoorlooptijd" : true
                    },
                    "startdatum" : "$DATE_2024_01_01",
                    "statusToelichting" : "Status gewijzigd",
                    "statustypeOmschrijving" : "Intake",
                    "uiterlijkeEinddatumAfdoening" : "$DATE_2024_01_31",
                    "vertrouwelijkheidaanduiding" : "$DOCUMENT_VERTROUWELIJKHEIDS_AANDUIDING_OPENBAAR",
                    "zaaktypeOmschrijving" : "$ZAAKTYPE_INDIENEN_AANSPRAKELIJKSTELLING_DOOR_DERDEN_BEHANDELEN_DESCRIPTION"
                  }, {
                    "identificatie" : "$ZAAK_MANUAL_1_IDENTIFICATION",
                    "type" : "ZAAK",
                    "aantalOpenstaandeTaken" : 0,
                    "afgehandeld" : false,
                    "betrokkenen" : {
                      "Belanghebbende" : [ "$TEST_PERSON_HENDRIKA_JANSE_BSN", "$TEST_PERSON_HENDRIKA_JANSE_BSN" ],
                      "Behandelaar" : [ "$TEST_GROUP_A_ID" ]
                    },
                    "communicatiekanaal" : "$COMMUNICATIEKANAAL_TEST_1",
                    "groepId" : "$TEST_GROUP_A_ID",
                    "groepNaam" : "$TEST_GROUP_A_DESCRIPTION",
                    "indicatieDeelzaak" : false,
                    "indicatieHeropend" : false,
                    "indicatieHoofdzaak" : false,
                    "indicatieOpschorting" : false,
                    "indicatieVerlenging" : false,
                    "indicaties" : [ ],
                    "omschrijving" : "$ZAAK_DESCRIPTION_2",
                    "rechten" : {
                      "afbreken" : true,
                      "behandelen" : true,
                      "bekijkenZaakdata" : true,
                      "creeerenDocument" : true,
                      "heropenen" : true,
                      "lezen" : true,
                      "toekennen" : true,
                      "toevoegenBagObject" : true,
                      "toevoegenBetrokkeneBedrijf" : true,
                      "toevoegenBetrokkenePersoon" : true,
                      "toevoegenInitiatorBedrijf" : true,
                      "toevoegenInitiatorPersoon" : true,
                      "versturenEmail" : true,
                      "versturenOntvangstbevestiging" : true,
                      "verwijderenBetrokkene" : true,
                      "verwijderenInitiator" : true,
                      "wijzigen" : true,
                      "wijzigenDoorlooptijd" : true
                    },
                    "statusToelichting" : "Status gewijzigd",
                    "statustypeOmschrijving" : "Intake",
                    "vertrouwelijkheidaanduiding" : "$DOCUMENT_VERTROUWELIJKHEIDS_AANDUIDING_OPENBAAR",
                    "zaaktypeOmschrijving" : "$ZAAKTYPE_MELDING_KLEIN_EVENEMENT_DESCRIPTION"
                  }, {
                    "identificatie" : "$ZAAK_PRODUCTAANVRAAG_2_IDENTIFICATION",
                    "type" : "ZAAK",
                    "aantalOpenstaandeTaken" : 0,
                    "afgehandeld" : false,
                    "betrokkenen" : {
                      "Bewindvoerder" : [ "$TEST_PERSON_2_BSN", "$TEST_PERSON_3_BSN" ],
                      "Medeaanvrager" : [ "$TEST_PERSON_3_BSN" ],
                      "Melder" : [ "$TEST_KVK_VESTIGINGSNUMMER_1" ],
                      "Behandelaar" : [ "$TEST_GROUP_A_ID" ]
                    },
                    "communicatiekanaal" : "E-formulier",
                    "groepId" : "test-group-a",
                    "groepNaam" : "Test group A",
                    "indicatieDeelzaak" : false,
                    "indicatieHeropend" : false,
                    "indicatieHoofdzaak" : false,
                    "indicatieOpschorting" : false,
                    "indicatieVerlenging" : false,
                    "indicaties" : [ ],
                    "initiatorIdentificatie" : "$TEST_KVK_VESTIGINGSNUMMER_1",
                    "omschrijving" : "Aangemaakt vanuit open-forms met kenmerk '$OPEN_FORMULIEREN_PRODUCTAANVRAAG_FORMULIER_2_BRON_KENMERK'",
                    "rechten" : {
                      "afbreken" : true,
                      "behandelen" : true,
                      "bekijkenZaakdata" : true,
                      "creeerenDocument" : true,
                      "heropenen" : true,
                      "lezen" : true,
                      "toekennen" : true,
                      "toevoegenBagObject" : true,
                      "toevoegenBetrokkeneBedrijf" : true,
                      "toevoegenBetrokkenePersoon" : true,
                      "toevoegenInitiatorBedrijf" : true,
                      "toevoegenInitiatorPersoon" : true,
                      "versturenEmail" : true,
                      "versturenOntvangstbevestiging" : true,
                      "verwijderenBetrokkene" : true,
                      "verwijderenInitiator" : true,
                      "wijzigen" : true,
                      "wijzigenDoorlooptijd" : true
                    },
                    "startdatum" : "$ZAAK_PRODUCTAANVRAAG_2_START_DATE",
                    "statusToelichting" : "Status gewijzigd",
                    "statustypeOmschrijving" : "Intake",
                    "uiterlijkeEinddatumAfdoening" : "$ZAAK_PRODUCTAANVRAAG_2_UITERLIJKE_EINDDATUM_AFDOENING",
                    "vertrouwelijkheidaanduiding" : "$DOCUMENT_VERTROUWELIJKHEIDS_AANDUIDING_OPENBAAR",
                    "zaaktypeOmschrijving" : "$ZAAKTYPE_MELDING_KLEIN_EVENEMENT_DESCRIPTION"
                  }, {
                    "identificatie" : "$ZAAK_PRODUCTAANVRAAG_1_IDENTIFICATION",
                    "type" : "ZAAK",
                    "aantalOpenstaandeTaken" : 1,
                    "afgehandeld" : false,
                    "betrokkenen" : {
                      "Bewindvoerder" : [ "$TEST_PERSON_2_BSN", "$TEST_PERSON_3_BSN" ],
                      "Medeaanvrager" : [ "$TEST_PERSON_2_BSN" ],
                      "Melder" : [ "$TEST_PERSON_HENDRIKA_JANSE_BSN" ]
                    },
                    "communicatiekanaal" : "E-formulier",
                    "groepId" : "$TEST_GROUP_A_ID",
                    "groepNaam" : "$TEST_GROUP_A_DESCRIPTION",
                    "indicatieDeelzaak" : false,
                    "indicatieHeropend" : false,
                    "indicatieHoofdzaak" : false,
                    "indicatieOpschorting" : false,
                    "indicatieVerlenging" : false,
                    "indicaties" : [ ],
                    "initiatorIdentificatie" : "$TEST_PERSON_HENDRIKA_JANSE_BSN",
                    "omschrijving" : "Aangemaakt vanuit open-forms met kenmerk '$OPEN_FORMULIEREN_PRODUCTAANVRAAG_FORMULIER_1_BRON_KENMERK'",
                    "rechten" : {
                      "afbreken" : true,
                      "behandelen" : true,
                      "bekijkenZaakdata" : true,
                      "creeerenDocument" : true,
                      "heropenen" : true,
                      "lezen" : true,
                      "toekennen" : true,
                      "toevoegenBagObject" : true,
                      "toevoegenBetrokkeneBedrijf" : true,
                      "toevoegenBetrokkenePersoon" : true,
                      "toevoegenInitiatorBedrijf" : true,
                      "toevoegenInitiatorPersoon" : true,
                      "versturenEmail" : true,
                      "versturenOntvangstbevestiging" : true,
                      "verwijderenBetrokkene" : true,
                      "verwijderenInitiator" : true,
                      "wijzigen" : true,
                      "wijzigenDoorlooptijd" : true
                    },
                    "startdatum" : "$ZAAK_PRODUCTAANVRAAG_1_START_DATE",
                    "statusToelichting" : "Status gewijzigd",
                    "statustypeOmschrijving" : "Intake",
                    "vertrouwelijkheidaanduiding" : "$DOCUMENT_VERTROUWELIJKHEIDS_AANDUIDING_OPENBAAR",
                    "zaaktypeOmschrijving" : "$ZAAKTYPE_MELDING_KLEIN_EVENEMENT_DESCRIPTION"
                  } ],
                  "totaal" : 4.0,
                  "filters" : {
                    "ZAAKTYPE" : [ {
                      "aantal" : 3,
                      "naam" : "$ZAAKTYPE_MELDING_KLEIN_EVENEMENT_DESCRIPTION"
                    }, {
                      "aantal" : 1,
                      "naam" : "$ZAAKTYPE_INDIENEN_AANSPRAKELIJKSTELLING_DOOR_DERDEN_BEHANDELEN_DESCRIPTION"
                    } ],                 
                    "GROEP" : [ {
                      "aantal" : 4,
                      "naam" : "$TEST_GROUP_A_DESCRIPTION"
                    } ],
                    "ZAAK_STATUS" : [ {
                      "aantal" : 4,
                      "naam" : "Intake"
                    } ],
                    "ZAAK_RESULTAAT" : [ {
                      "aantal" : 4,
                      "naam" : "-NULL-"
                    } ],
                    "ZAAK_INDICATIES" : [ {
                      "aantal" : 4,
                      "naam" : "-NULL-"
                    } ],
                    "ZAAK_COMMUNICATIEKANAAL" : [ {
                      "aantal" : 2,
                      "naam" : "E-formulier"
                    }, {
                      "aantal" : 2,
                      "naam" : "$COMMUNICATIEKANAAL_TEST_1"
                    } ],
                    "ZAAK_VERTROUWELIJKHEIDAANDUIDING" : [ {
                      "aantal" : 4,
                      "naam" : "$DOCUMENT_VERTROUWELIJKHEIDS_AANDUIDING_OPENBAAR"
                    } ],
                    "ZAAK_ARCHIEF_NOMINATIE" : [ {
                      "aantal" : 4,
                      "naam" : "-NULL-"
                    } ]
                  }
                }
                """.trimIndent()
            }
        }
    }
})
