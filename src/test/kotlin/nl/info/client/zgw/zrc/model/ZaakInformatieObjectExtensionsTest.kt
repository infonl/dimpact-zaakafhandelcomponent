/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.client.zgw.zrc.model

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.checkUnnecessaryStub
import nl.info.client.zgw.model.createZaakInformatieobjectForReads
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
})
