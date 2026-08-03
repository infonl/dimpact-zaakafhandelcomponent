/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.itest.client

import io.kotest.matchers.shouldBe
import nl.info.zac.itest.config.ItestConfiguration.COMMUNICATIEKANAAL_TEST_1
import nl.info.zac.itest.config.ItestConfiguration.DOCUMENT_VERTROUWELIJKHEIDS_AANDUIDING_OPENBAAR
import nl.info.zac.itest.config.ItestConfiguration.ZAAK_OMSCHRIJVING
import nl.info.zac.itest.config.TestUser
import org.json.JSONTokener
import java.net.HttpURLConnection
import java.time.ZonedDateTime
import java.util.UUID

/**
 * Creates a zaak like [ZacClient.createZaak], but since the create endpoint only returns the zaak
 * identification, this additionally retrieves and returns the full zaak so that callers can
 * assert on/extract fields (such as `uuid`) beyond the identification.
 */
@Suppress("LongParameterList")
fun ZacClient.createZaakAndRetrieve(
    zaakTypeUUID: UUID,
    groupId: String,
    groupName: String,
    behandelaarId: String? = null,
    behandelaarName: String? = null,
    description: String? = ZAAK_OMSCHRIJVING,
    toelichting: String? = null,
    startDate: ZonedDateTime,
    communicatiekanaal: String? = COMMUNICATIEKANAAL_TEST_1,
    vertrouwelijkheidaanduiding: String? = DOCUMENT_VERTROUWELIJKHEIDS_AANDUIDING_OPENBAAR,
    testUser: TestUser
): ResponseContent {
    val createResponse = createZaak(
        zaakTypeUUID = zaakTypeUUID,
        groupId = groupId,
        groupName = groupName,
        behandelaarId = behandelaarId,
        behandelaarName = behandelaarName,
        description = description,
        toelichting = toelichting,
        startDate = startDate,
        communicatiekanaal = communicatiekanaal,
        vertrouwelijkheidaanduiding = vertrouwelijkheidaanduiding,
        testUser = testUser
    )
    createResponse.code shouldBe HttpURLConnection.HTTP_OK
    val zaakIdentification = JSONTokener(createResponse.bodyAsString).nextValue() as String
    return retrieveZaak(zaakIdentification, testUser)
}
