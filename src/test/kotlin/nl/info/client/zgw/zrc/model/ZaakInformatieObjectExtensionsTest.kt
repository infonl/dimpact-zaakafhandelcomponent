/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.client.zgw.zrc.model

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.checkUnnecessaryStub
import jakarta.json.bind.JsonbBuilder
import nl.info.client.zgw.model.createZaakInformatieobjectForReads
import nl.info.client.zgw.zrc.model.generated.ZaakInformatieObject
import java.net.URI
import java.util.UUID

class ZaakInformatieObjectExtensionsTest : BehaviorSpec({
    afterEach { checkUnnecessaryStub() }

    given("a ZaakInformatieObject whose zaak URI ends in a valid UUID") {
        val zaakUUID = UUID.randomUUID()
        val zaakInformatieObject = createZaakInformatieobjectForReads(zaak = URI("https://example.com/zaken/$zaakUUID"))

        `when`("zaakUUID is read") {
            val result = zaakInformatieObject.zaakUUID

            then("it returns the UUID extracted from the zaak URI") {
                result shouldBe zaakUUID
            }
        }
    }

    given("a GET response JSON payload with all fields populated, including titel and beschrijving") {
        val jsonb = JsonbBuilder.create()
        val json = """
            {
                "url": "https://example.com/zaakinformatieobjecten/${UUID.randomUUID()}",
                "uuid": "${UUID.randomUUID()}",
                "informatieobject": "https://example.com/informatieobjecten/${UUID.randomUUID()}",
                "zaak": "https://example.com/zaken/${UUID.randomUUID()}",
                "aardRelatieWeergave": "Hoort bij, omgekeerd: kent",
                "titel": "fakeTitel",
                "beschrijving": "fakeBeschrijving"
            }
        """.trimIndent()

        `when`("it is deserialized via JSON-B") {
            val zaakInformatieObject = jsonb.fromJson(json, ZaakInformatieObject::class.java)

            then("titel and beschrijving are populated, not silently dropped") {
                zaakInformatieObject.titel shouldBe "fakeTitel"
                zaakInformatieObject.beschrijving shouldBe "fakeBeschrijving"
            }
        }
    }
})
