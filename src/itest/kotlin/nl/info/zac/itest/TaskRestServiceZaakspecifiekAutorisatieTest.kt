/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.itest

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import nl.info.zac.itest.client.ItestHttpClient
import nl.info.zac.itest.client.OpenZaakClient
import nl.info.zac.itest.client.TaskHelper
import nl.info.zac.itest.client.ZaakHelper
import nl.info.zac.itest.client.ZacClient
import nl.info.zac.itest.config.BEHANDELAAR_1
import nl.info.zac.itest.config.GROUP_BEHANDELAARS_TEST_1
import nl.info.zac.itest.config.ItestConfiguration.ZAAKTYPE_CMMN_TEST_2_UUID
import nl.info.zac.itest.config.ItestConfiguration.ZAC_API_URI
import nl.info.zac.itest.config.ZAAKSPECIFIEK_AUTORISATIE_BEHANDELAAR_1
import java.net.HttpURLConnection.HTTP_FORBIDDEN
import java.net.HttpURLConnection.HTTP_OK
import java.time.LocalDate

class TaskRestServiceZaakspecifiekAutorisatieTest : BehaviorSpec({
    val itestHttpClient = ItestHttpClient()
    val zacClient = ZacClient(itestHttpClient)
    val zaakHelper = ZaakHelper(zacClient)
    val taskHelper = TaskHelper(zacClient)
    val openZaakClient = OpenZaakClient(itestHttpClient)

    given(
        """
        A CMMN zaak of a zaaktype that supports zaakspecifieke autorisatie has a task started for
        it, and the zaak is then marked as zaakspecifiek geautoriseerd
        """
    ) {
        val (zaakIdentificatie, zaakUuid) = zaakHelper.createZaak(
            zaaktypeUuid = ZAAKTYPE_CMMN_TEST_2_UUID,
            testUser = BEHANDELAAR_1
        )
        val taskId = taskHelper.startAanvullendeInformatieTaskForZaak(
            zaakUuid = zaakUuid,
            zaakIdentificatie = zaakIdentificatie,
            fatalDate = LocalDate.now().plusWeeks(1),
            group = GROUP_BEHANDELAARS_TEST_1,
            testUser = BEHANDELAAR_1
        )
        openZaakClient.createZaakeigenschap(
            zaakUUID = zaakUuid,
            zaaktypeUUID = ZAAKTYPE_CMMN_TEST_2_UUID,
            eigenschapNaam = "ZAAK_GEAUTORISEERD",
            waarde = "true"
        )

        `when`(
            "the task is read by a behandelaar authorized for the zaaktype but without the " +
                "zaakspecifiek_autorisatie_behandelaar role"
        ) {
            val response = itestHttpClient.performGetRequest(
                url = "$ZAC_API_URI/taken/$taskId",
                testUser = BEHANDELAAR_1
            )
            then("the response should be a 403 HTTP response") {
                response.code shouldBe HTTP_FORBIDDEN
            }
        }
        `when`("the task is read by a user holding the zaakspecifiek_autorisatie_behandelaar role") {
            val response = itestHttpClient.performGetRequest(
                url = "$ZAC_API_URI/taken/$taskId",
                testUser = ZAAKSPECIFIEK_AUTORISATIE_BEHANDELAAR_1
            )
            then("the response should be a 200 HTTP response") {
                response.code shouldBe HTTP_OK
            }
        }
    }
})
