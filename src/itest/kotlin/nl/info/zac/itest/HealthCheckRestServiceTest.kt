/*
 * SPDX-FileCopyrightText: 2023 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.itest

import io.github.oshai.kotlinlogging.KotlinLogging
import io.kotest.assertions.json.shouldEqualJson
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import nl.info.zac.itest.client.ItestHttpClient
import nl.info.zac.itest.config.BEHEERDER_1
import nl.info.zac.itest.config.ItestConfiguration.DATE_2023_09_21
import nl.info.zac.itest.config.ItestConfiguration.DATE_2023_10_01
import nl.info.zac.itest.config.ItestConfiguration.DATE_2025_01_01
import nl.info.zac.itest.config.ItestConfiguration.DATE_2025_07_01
import nl.info.zac.itest.config.ItestConfiguration.DATE_2026_07_23
import nl.info.zac.itest.config.ItestConfiguration.VERTROUWELIJKHEIDAANDUIDING_OPENBAAR
import nl.info.zac.itest.config.ItestConfiguration.ZAAKTYPE_BPMN_TEST_1_DESCRIPTION
import nl.info.zac.itest.config.ItestConfiguration.ZAAKTYPE_BPMN_TEST_1_IDENTIFICATIE
import nl.info.zac.itest.config.ItestConfiguration.ZAAKTYPE_BPMN_TEST_1_UUID
import nl.info.zac.itest.config.ItestConfiguration.ZAAKTYPE_BPMN_TEST_2_DESCRIPTION
import nl.info.zac.itest.config.ItestConfiguration.ZAAKTYPE_BPMN_TEST_2_IDENTIFICATIE
import nl.info.zac.itest.config.ItestConfiguration.ZAAKTYPE_BPMN_TEST_2_UUID
import nl.info.zac.itest.config.ItestConfiguration.ZAAKTYPE_BPMN_TEST_3_DESCRIPTION
import nl.info.zac.itest.config.ItestConfiguration.ZAAKTYPE_BPMN_TEST_3_IDENTIFICATIE
import nl.info.zac.itest.config.ItestConfiguration.ZAAKTYPE_BPMN_TEST_3_UUID
import nl.info.zac.itest.config.ItestConfiguration.ZAAKTYPE_BPMN_TEST_4_DESCRIPTION
import nl.info.zac.itest.config.ItestConfiguration.ZAAKTYPE_BPMN_TEST_4_IDENTIFICATIE
import nl.info.zac.itest.config.ItestConfiguration.ZAAKTYPE_BPMN_TEST_4_UUID
import nl.info.zac.itest.config.ItestConfiguration.ZAAKTYPE_BPMN_TEST_5_DESCRIPTION
import nl.info.zac.itest.config.ItestConfiguration.ZAAKTYPE_BPMN_TEST_5_IDENTIFICATIE
import nl.info.zac.itest.config.ItestConfiguration.ZAAKTYPE_BPMN_TEST_5_UUID
import nl.info.zac.itest.config.ItestConfiguration.ZAAKTYPE_CMMN_TEST_1_DESCRIPTION
import nl.info.zac.itest.config.ItestConfiguration.ZAAKTYPE_CMMN_TEST_1_IDENTIFICATIE
import nl.info.zac.itest.config.ItestConfiguration.ZAAKTYPE_CMMN_TEST_1_UUID
import nl.info.zac.itest.config.ItestConfiguration.ZAAKTYPE_CMMN_TEST_2_DESCRIPTION
import nl.info.zac.itest.config.ItestConfiguration.ZAAKTYPE_CMMN_TEST_2_IDENTIFICATIE
import nl.info.zac.itest.config.ItestConfiguration.ZAAKTYPE_CMMN_TEST_2_UUID
import nl.info.zac.itest.config.ItestConfiguration.ZAAKTYPE_CMMN_TEST_3_DESCRIPTION
import nl.info.zac.itest.config.ItestConfiguration.ZAAKTYPE_CMMN_TEST_3_IDENTIFICATIE
import nl.info.zac.itest.config.ItestConfiguration.ZAAKTYPE_CMMN_TEST_3_UUID
import nl.info.zac.itest.config.ItestConfiguration.ZAAKTYPE_CMMN_TEST_4_DESCRIPTION
import nl.info.zac.itest.config.ItestConfiguration.ZAAKTYPE_CMMN_TEST_4_IDENTIFICATIE
import nl.info.zac.itest.config.ItestConfiguration.ZAAKTYPE_CMMN_TEST_4_DOEL
import nl.info.zac.itest.config.ItestConfiguration.ZAAKTYPE_CMMN_TEST_4_UUID
import nl.info.zac.itest.config.ItestConfiguration.ZAC_API_URI
import java.net.HttpURLConnection.HTTP_OK

class HealthCheckRestServiceTest : BehaviorSpec({
    val logger = KotlinLogging.logger {}
    val itestHttpClient = ItestHttpClient()

    given(
        """
            Default communicatiekanalen referentietabel data is provisioned on startup,
            and a logged-in beheerder
            """
    ) {
        `when`("the check on the existence of the e-formulier communicatiekanaal is performed") {
            val response = itestHttpClient.performGetRequest(
                "$ZAC_API_URI/health-check/bestaat-communicatiekanaal-eformulier",
                testUser = BEHEERDER_1
            )
            val responseBody = response.bodyAsString
            logger.info { "Response: $responseBody" }

            then("the response should be a 200 OK with a response body 'true'") {
                response.code shouldBe HTTP_OK
                responseBody shouldBe "true"
            }
        }
    }

    given("Zaak types are configured correctly and a logged-in beheerder") {
        `when`("the check for zaak types validity is performed") {
            val response = itestHttpClient.performGetRequest(
                url = "$ZAC_API_URI/health-check/zaaktypes",
                testUser = BEHEERDER_1
            )
            val responseBody = response.bodyAsString
            logger.info { "Response: $responseBody" }

            then("the response should be a 200 OK") {
                response.code shouldBe HTTP_OK
            }
            and("the response body should contain all the performed checks") {
                responseBody shouldEqualJson """
                    [
                      {
                        "aantalBehandelaarroltypen": 1,
                        "aantalInitiatorroltypen": 1,
                        "isBrpInstellingenCorrect": true,
                        "isBesluittypeAanwezig": true,
                        "hasWaarschuwingen": false,
                        "isInformatieobjecttypeEmailAanwezig": true,
                        "isZaakspecifiekeAutorisatieEigenschapAanwezig": false,
                        "isZaakspecifiekeAutorisatieRoltypeAanwezig": false,
                        "isResultaattypeAanwezig": true,
                        "resultaattypesMetVerplichtBesluit": [
                          "Opgelegd - Verval besluit",
                          "Opgelegd - Ingang besluit"
                        ],
                        "isRolOverigeAanwezig": true,
                        "isStatustypeAanvullendeInformatieVereist": true,
                        "isStatustypeAfgerondAanwezig": true,
                        "isStatustypeAfgerondLaatsteVolgnummer": true,
                        "isStatustypeHeropendAanwezig": true,
                        "isStatustypeInBehandelingAanwezig": true,
                        "isStatustypeIntakeAanwezig": true,
                        "isValide": true,
                        "isZaakafhandelParametersValide": true,
                        "zaaktype": {
                          "beginGeldigheid": "$DATE_2026_07_23",
                          "doel": "$ZAAKTYPE_CMMN_TEST_4_DOEL",
                          "identificatie": "$ZAAKTYPE_CMMN_TEST_4_IDENTIFICATIE",
                          "isNuGeldig": true,
                          "omschrijving": "$ZAAKTYPE_CMMN_TEST_4_DESCRIPTION",
                          "hasServicenorm": false,
                          "uuid": "$ZAAKTYPE_CMMN_TEST_4_UUID",
                          "versiedatum": "$DATE_2023_10_01",
                          "vertrouwelijkheidaanduiding": "$VERTROUWELIJKHEIDAANDUIDING_OPENBAAR"
                        }
                      },
                      {
                        "aantalBehandelaarroltypen": 1,
                        "aantalInitiatorroltypen": 1,
                        "isBesluittypeAanwezig": false,
                        "isBrpInstellingenCorrect": true,
                        "hasWaarschuwingen": false,
                        "isInformatieobjecttypeEmailAanwezig": false,
                        "isZaakspecifiekeAutorisatieEigenschapAanwezig": false,
                        "isZaakspecifiekeAutorisatieRoltypeAanwezig": false,
                        "isResultaattypeAanwezig": true,
                        "resultaattypesMetVerplichtBesluit": [],
                        "isRolOverigeAanwezig": false,
                        "isStatustypeAanvullendeInformatieVereist": false,
                        "isStatustypeAfgerondAanwezig": true,
                        "isStatustypeAfgerondLaatsteVolgnummer": true,
                        "isStatustypeHeropendAanwezig": false,
                        "isStatustypeInBehandelingAanwezig": false,
                        "isStatustypeIntakeAanwezig": false,
                        "isValide": false,
                        "isZaakafhandelParametersValide": true,
                        "zaaktype": {
                          "beginGeldigheid": "$DATE_2025_01_01",
                          "doel": "$ZAAKTYPE_BPMN_TEST_5_DESCRIPTION",
                          "identificatie": "$ZAAKTYPE_BPMN_TEST_5_IDENTIFICATIE",
                          "isNuGeldig": true,
                          "omschrijving": "$ZAAKTYPE_BPMN_TEST_5_DESCRIPTION",
                          "hasServicenorm": false,
                          "uuid": "$ZAAKTYPE_BPMN_TEST_5_UUID",
                          "versiedatum": "$DATE_2025_01_01",
                          "vertrouwelijkheidaanduiding": "$VERTROUWELIJKHEIDAANDUIDING_OPENBAAR"
                        }
                      },
                      {
                        "aantalBehandelaarroltypen": 1,
                        "aantalInitiatorroltypen": 1,
                        "isBesluittypeAanwezig": false,
                        "isBrpInstellingenCorrect": true,
                        "hasWaarschuwingen": false,
                        "isInformatieobjecttypeEmailAanwezig": false,
                        "isZaakspecifiekeAutorisatieEigenschapAanwezig": false,
                        "isZaakspecifiekeAutorisatieRoltypeAanwezig": false,
                        "isResultaattypeAanwezig": true,
                        "resultaattypesMetVerplichtBesluit": [],
                        "isRolOverigeAanwezig": false,
                        "isStatustypeAanvullendeInformatieVereist": false,
                        "isStatustypeAfgerondAanwezig": true,
                        "isStatustypeAfgerondLaatsteVolgnummer": true,
                        "isStatustypeHeropendAanwezig": false,
                        "isStatustypeInBehandelingAanwezig": false,
                        "isStatustypeIntakeAanwezig": false,
                        "isValide": false,
                        "isZaakafhandelParametersValide": true,
                        "zaaktype": {
                          "beginGeldigheid": "$DATE_2025_01_01",
                          "doel": "$ZAAKTYPE_BPMN_TEST_4_DESCRIPTION",
                          "identificatie": "$ZAAKTYPE_BPMN_TEST_4_IDENTIFICATIE",
                          "isNuGeldig": true,
                          "omschrijving": "$ZAAKTYPE_BPMN_TEST_4_DESCRIPTION",
                          "hasServicenorm": false,
                          "uuid": "$ZAAKTYPE_BPMN_TEST_4_UUID",
                          "versiedatum": "$DATE_2025_01_01",
                          "vertrouwelijkheidaanduiding": "$VERTROUWELIJKHEIDAANDUIDING_OPENBAAR"
                        }
                      },
                      {
                        "aantalBehandelaarroltypen": 1,
                        "aantalInitiatorroltypen": 1,
                        "isBesluittypeAanwezig": false,
                        "isBrpInstellingenCorrect": true,
                        "hasWaarschuwingen": false,
                        "isInformatieobjecttypeEmailAanwezig": true,
                        "isZaakspecifiekeAutorisatieEigenschapAanwezig": false,
                        "isZaakspecifiekeAutorisatieRoltypeAanwezig": false,
                        "isResultaattypeAanwezig": true,
                        "resultaattypesMetVerplichtBesluit": [],
                        "isRolOverigeAanwezig": false,
                        "isStatustypeAanvullendeInformatieVereist": false,
                        "isStatustypeAfgerondAanwezig": true,
                        "isStatustypeAfgerondLaatsteVolgnummer": true,
                        "isStatustypeHeropendAanwezig": false,
                        "isStatustypeInBehandelingAanwezig": false,
                        "isStatustypeIntakeAanwezig": false,
                        "isValide": false,
                        "isZaakafhandelParametersValide": true,
                        "zaaktype": {
                          "beginGeldigheid": "$DATE_2025_01_01",
                          "doel": "$ZAAKTYPE_BPMN_TEST_3_DESCRIPTION",
                          "identificatie": "$ZAAKTYPE_BPMN_TEST_3_IDENTIFICATIE",
                          "isNuGeldig": true,
                          "omschrijving": "$ZAAKTYPE_BPMN_TEST_3_DESCRIPTION",
                          "hasServicenorm": false,
                          "uuid": "$ZAAKTYPE_BPMN_TEST_3_UUID",
                          "versiedatum": "$DATE_2025_01_01",
                          "vertrouwelijkheidaanduiding": "$VERTROUWELIJKHEIDAANDUIDING_OPENBAAR"
                        }
                      },
                      {
                        "aantalBehandelaarroltypen": 1,
                        "aantalInitiatorroltypen": 1,
                        "isBesluittypeAanwezig": false,
                        "isBrpInstellingenCorrect": true,
                        "hasWaarschuwingen": false,
                        "isInformatieobjecttypeEmailAanwezig": true,
                        "isZaakspecifiekeAutorisatieEigenschapAanwezig": false,
                        "isZaakspecifiekeAutorisatieRoltypeAanwezig": false,
                        "isResultaattypeAanwezig": true,
                        "resultaattypesMetVerplichtBesluit": [],
                        "isRolOverigeAanwezig": true,
                        "isStatustypeAanvullendeInformatieVereist": true,
                        "isStatustypeAfgerondAanwezig": true,
                        "isStatustypeAfgerondLaatsteVolgnummer": true,
                        "isStatustypeHeropendAanwezig": true,
                        "isStatustypeInBehandelingAanwezig": true,
                        "isStatustypeIntakeAanwezig": true,
                        "isValide": true,
                        "isZaakafhandelParametersValide": true,
                        "zaaktype": {
                          "beginGeldigheid": "$DATE_2025_01_01",
                          "doel": "$ZAAKTYPE_BPMN_TEST_2_DESCRIPTION",
                          "identificatie": "$ZAAKTYPE_BPMN_TEST_2_IDENTIFICATIE",
                          "isNuGeldig": true,
                          "omschrijving": "$ZAAKTYPE_BPMN_TEST_2_DESCRIPTION",
                          "hasServicenorm": false,
                          "uuid": "$ZAAKTYPE_BPMN_TEST_2_UUID",
                          "versiedatum": "$DATE_2025_01_01",
                          "vertrouwelijkheidaanduiding": "$VERTROUWELIJKHEIDAANDUIDING_OPENBAAR"
                        }
                      },
                      {
                        "aantalBehandelaarroltypen": 1,
                        "aantalInitiatorroltypen": 1,
                        "isBesluittypeAanwezig": false,
                        "isBrpInstellingenCorrect": true,
                        "hasWaarschuwingen": false,
                        "isInformatieobjecttypeEmailAanwezig": true,
                        "isZaakspecifiekeAutorisatieEigenschapAanwezig": false,
                        "isZaakspecifiekeAutorisatieRoltypeAanwezig": false,
                        "isResultaattypeAanwezig": true,
                        "resultaattypesMetVerplichtBesluit": [],
                        "isRolOverigeAanwezig": true,
                        "isStatustypeAanvullendeInformatieVereist": true,
                        "isStatustypeAfgerondAanwezig": true,
                        "isStatustypeAfgerondLaatsteVolgnummer": true,
                        "isStatustypeHeropendAanwezig": true,
                        "isStatustypeInBehandelingAanwezig": true,
                        "isStatustypeIntakeAanwezig": true,
                        "isValide": true,
                        "isZaakafhandelParametersValide": true,
                        "zaaktype": {
                          "beginGeldigheid": "$DATE_2025_07_01",
                          "doel": "$ZAAKTYPE_CMMN_TEST_1_DESCRIPTION",
                          "identificatie": "$ZAAKTYPE_CMMN_TEST_1_IDENTIFICATIE",
                          "isNuGeldig": true,
                          "omschrijving": "$ZAAKTYPE_CMMN_TEST_1_DESCRIPTION",
                          "hasServicenorm": false,
                          "uuid": "$ZAAKTYPE_CMMN_TEST_1_UUID",
                          "versiedatum": "$DATE_2025_07_01",
                          "vertrouwelijkheidaanduiding": "$VERTROUWELIJKHEIDAANDUIDING_OPENBAAR"
                        }
                      },
                      {
                        "aantalBehandelaarroltypen": 1,
                        "aantalInitiatorroltypen": 1,
                        "isBesluittypeAanwezig": false,
                        "isBrpInstellingenCorrect": true,
                        "hasWaarschuwingen": false,
                        "isInformatieobjecttypeEmailAanwezig": true,
                        "isZaakspecifiekeAutorisatieEigenschapAanwezig": true,
                        "isZaakspecifiekeAutorisatieRoltypeAanwezig": true,
                        "isResultaattypeAanwezig": true,
                        "resultaattypesMetVerplichtBesluit": [],
                        "isRolOverigeAanwezig": true,
                        "isStatustypeAanvullendeInformatieVereist": true,
                        "isStatustypeAfgerondAanwezig": true,
                        "isStatustypeAfgerondLaatsteVolgnummer": true,
                        "isStatustypeHeropendAanwezig": true,
                        "isStatustypeInBehandelingAanwezig": true,
                        "isStatustypeIntakeAanwezig": true,
                        "isValide": true,
                        "isZaakafhandelParametersValide": true,
                        "zaaktype": {
                          "beginGeldigheid": "$DATE_2025_01_01",
                          "doel": "$ZAAKTYPE_BPMN_TEST_1_DESCRIPTION",
                          "identificatie": "$ZAAKTYPE_BPMN_TEST_1_IDENTIFICATIE",
                          "isNuGeldig": true,
                          "omschrijving": "$ZAAKTYPE_BPMN_TEST_1_DESCRIPTION",
                          "hasServicenorm": false,
                          "uuid": "$ZAAKTYPE_BPMN_TEST_1_UUID",
                          "versiedatum": "$DATE_2025_01_01",
                          "vertrouwelijkheidaanduiding": "$VERTROUWELIJKHEIDAANDUIDING_OPENBAAR"
                        }
                      },
                      {
                        "aantalBehandelaarroltypen": 1,
                        "aantalInitiatorroltypen": 1,
                        "isBesluittypeAanwezig": true,
                        "isBrpInstellingenCorrect": true,
                        "hasWaarschuwingen": false,
                        "isInformatieobjecttypeEmailAanwezig": true,
                        "isZaakspecifiekeAutorisatieEigenschapAanwezig": true,
                        "isZaakspecifiekeAutorisatieRoltypeAanwezig": true,
                        "isResultaattypeAanwezig": true,
                        "resultaattypesMetVerplichtBesluit": [],
                        "isRolOverigeAanwezig": true,
                        "isStatustypeAanvullendeInformatieVereist": true,
                        "isStatustypeAfgerondAanwezig": true,
                        "isStatustypeAfgerondLaatsteVolgnummer": true,
                        "isStatustypeHeropendAanwezig": true,
                        "isStatustypeInBehandelingAanwezig": true,
                        "isStatustypeIntakeAanwezig": true,
                        "isValide": true,
                        "isZaakafhandelParametersValide": true,
                        "zaaktype": {
                          "beginGeldigheid": "$DATE_2023_10_01",
                          "doel": "$ZAAKTYPE_CMMN_TEST_2_DESCRIPTION",
                          "identificatie": "$ZAAKTYPE_CMMN_TEST_2_IDENTIFICATIE",
                          "isNuGeldig": true,
                          "omschrijving": "$ZAAKTYPE_CMMN_TEST_2_DESCRIPTION",
                          "hasServicenorm": false,
                          "uuid": "$ZAAKTYPE_CMMN_TEST_2_UUID",
                          "versiedatum": "$DATE_2023_10_01",
                          "vertrouwelijkheidaanduiding": "$VERTROUWELIJKHEIDAANDUIDING_OPENBAAR"
                        }
                      },
                      {
                        "aantalBehandelaarroltypen": 1,
                        "aantalInitiatorroltypen": 1,
                        "isBrpInstellingenCorrect": true,
                        "isBesluittypeAanwezig": false,
                        "hasWaarschuwingen": false,
                        "isInformatieobjecttypeEmailAanwezig": true,
                        "isZaakspecifiekeAutorisatieEigenschapAanwezig": false,
                        "isZaakspecifiekeAutorisatieRoltypeAanwezig": false,
                        "isResultaattypeAanwezig": true,
                        "resultaattypesMetVerplichtBesluit": [],
                        "isRolOverigeAanwezig": true,
                        "isStatustypeAanvullendeInformatieVereist": true,
                        "isStatustypeAfgerondAanwezig": true,
                        "isStatustypeAfgerondLaatsteVolgnummer": true,
                        "isStatustypeHeropendAanwezig": true,
                        "isStatustypeInBehandelingAanwezig": true,
                        "isStatustypeIntakeAanwezig": true,
                        "isValide": true,
                        "isZaakafhandelParametersValide": true,
                        "zaaktype": {
                          "beginGeldigheid": "$DATE_2023_09_21",
                          "doel": "$ZAAKTYPE_CMMN_TEST_3_DESCRIPTION",
                          "identificatie": "$ZAAKTYPE_CMMN_TEST_3_IDENTIFICATIE",
                          "isNuGeldig": true,
                          "omschrijving": "$ZAAKTYPE_CMMN_TEST_3_DESCRIPTION",
                          "hasServicenorm": false,
                          "uuid": "$ZAAKTYPE_CMMN_TEST_3_UUID",
                          "versiedatum": "$DATE_2023_09_21",
                          "vertrouwelijkheidaanduiding": "$VERTROUWELIJKHEIDAANDUIDING_OPENBAAR"
                        }
                      }
                    ]
                """.trimIndent()
            }
        }
    }
})
