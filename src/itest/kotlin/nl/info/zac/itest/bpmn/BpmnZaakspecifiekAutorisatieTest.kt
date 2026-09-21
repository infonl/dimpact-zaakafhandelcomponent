/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.itest.bpmn

import io.github.oshai.kotlinlogging.KotlinLogging
import io.kotest.assertions.nondeterministic.eventually
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import nl.info.zac.itest.client.ItestHttpClient
import nl.info.zac.itest.client.OpenZaakClient
import nl.info.zac.itest.client.ZacClient
import nl.info.zac.itest.client.createZaakAndRetrieve
import nl.info.zac.itest.config.BEHANDELAAR_1
import nl.info.zac.itest.config.COORDINATOR_1
import nl.info.zac.itest.config.GROUP_BEHANDELAARS_TEST_1
import nl.info.zac.itest.config.GROUP_COORDINATORS_TEST_1
import nl.info.zac.itest.config.ItestConfiguration.DATE_TIME_2000_01_01
import nl.info.zac.itest.config.ItestConfiguration.ROLTYPE_NAME_ZAAKSPECIFIEK_GEAUTORISEERDE_MEDEWERKER
import nl.info.zac.itest.config.ItestConfiguration.ZAAKTYPE_BPMN_TEST_1_DESCRIPTION
import nl.info.zac.itest.config.ItestConfiguration.ZAAKTYPE_BPMN_TEST_1_UUID
import nl.info.zac.itest.config.ItestConfiguration.ZAC_API_URI
import org.json.JSONObject
import java.net.HttpURLConnection.HTTP_OK
import java.util.UUID
import kotlin.time.Duration.Companion.seconds

/**
 * Verifies that a zaakspecifiek geautoriseerde BPMN zaak that is reassigned by the process itself -
 * through the `UpdateZaakAssignmentDelegate` service task of the integration test process definition,
 * rather than through the ZAC 'toekennen' endpoint - leaves the behandelaar it replaces with access to
 * the zaak.
 */
class BpmnZaakspecifiekAutorisatieTest : BehaviorSpec({
    val logger = KotlinLogging.logger {}
    val itestHttpClient = ItestHttpClient()
    val zacClient = ZacClient(itestHttpClient)
    val openZaakClient = OpenZaakClient(itestHttpClient)

    fun zaakspecifiekGeautoriseerdeMedewerkerIds(zaakUuid: UUID) =
        JSONObject(openZaakClient.getRolesForZaak(zaakUuid).bodyAsString)
            .getJSONArray("results")
            .let { results -> (0 until results.length()).map(results::getJSONObject) }
            .filter { it.getString("omschrijving") == ROLTYPE_NAME_ZAAKSPECIFIEK_GEAUTORISEERDE_MEDEWERKER }
            .map { it.getJSONObject("betrokkeneIdentificatie").getString("identificatie") }

    given(
        """
        a BPMN zaak of a zaaktype that supports zaakspecifieke autorisatie, with a behandelaar that does
        not hold the zaakspecifiek_geautoriseerd application role, marked as zaakspecifiek geautoriseerd
        """
    ) {
        lateinit var zaakIdentificatie: String
        lateinit var zaakUuid: UUID
        zacClient.createZaakAndRetrieve(
            zaakTypeUUID = ZAAKTYPE_BPMN_TEST_1_UUID,
            groupId = GROUP_BEHANDELAARS_TEST_1.name,
            groupName = GROUP_BEHANDELAARS_TEST_1.description,
            behandelaarId = BEHANDELAAR_1.username,
            behandelaarName = BEHANDELAAR_1.displayName,
            startDate = DATE_TIME_2000_01_01,
            testUser = BEHANDELAAR_1
        ).run {
            val responseBody = bodyAsString
            logger.info { "Response: $responseBody" }
            code shouldBe HTTP_OK
            JSONObject(responseBody).run {
                zaakUuid = getString("uuid").run(UUID::fromString)
                zaakIdentificatie = getString("identificatie")
            }
        }
        itestHttpClient.performPatchRequest(
            url = "$ZAC_API_URI/zaken/zaak/$zaakUuid",
            requestBodyAsString = """
                {
                    "zaak": {
                        "omschrijving": "itestOmschrijving",
                        "isZaakspecifiekGeautoriseerd": true
                    },
                    "reden": "fakeReason"
                }
            """.trimIndent(),
            testUser = BEHANDELAAR_1
        ).run {
            logger.info { "Response: $bodyAsString" }
            code shouldBe HTTP_OK
            JSONObject(bodyAsString).getBoolean("isZaakspecifiekGeautoriseerd") shouldBe true
        }

        `when`("the behandelaar completes the form task that hands the zaak to another group for approval") {
            zacClient.submitFormData(
                bpmnZaakUuid = zaakUuid,
                taakData = """
                    {
                        "zaakIdentificatie": "$zaakIdentificatie",
                        "initiator": null,
                        "zaaktypeOmschrijving": "$ZAAKTYPE_BPMN_TEST_1_DESCRIPTION",
                        "firstName": "Name",
                        "AM_TeamBehandelaar_Groep": "${GROUP_COORDINATORS_TEST_1.name}",
                        "AM_TeamBehandelaar_Medewerker": "${COORDINATOR_1.username}",
                        "SD_SmartDocuments_Template": "OpenZaakTest",
                        "SD_SmartDocuments_Create": false,
                        "RT_ReferenceTable_Values": "Post",
                        "ZK_Result": "Verleend",
                        "ZK_Status": "Afgerond"
                    }
                """.trimIndent(),
                testUser = BEHANDELAAR_1
            ).run {
                JSONObject(this).getString("status") shouldBe "AFGEROND"
            }

            then("the process assigns the zaak to the behandelaar it names, who can read it") {
                eventually(30.seconds) {
                    with(zacClient.retrieveZaak(zaakUuid, COORDINATOR_1)) {
                        code shouldBe HTTP_OK
                        JSONObject(bodyAsString).getJSONObject("behandelaar").getString("id") shouldBe
                            COORDINATOR_1.username
                    }
                }
            }

            and("the behandelaar the process replaced keeps access as a zaakspecifiek geautoriseerde medewerker") {
                zaakspecifiekGeautoriseerdeMedewerkerIds(zaakUuid) shouldBe listOf(BEHANDELAAR_1.username)
                with(zacClient.retrieveZaak(zaakUuid, BEHANDELAAR_1)) {
                    code shouldBe HTTP_OK
                    JSONObject(bodyAsString).getJSONObject("rechten").getBoolean("lezen") shouldBe true
                }
            }
        }
    }
})
