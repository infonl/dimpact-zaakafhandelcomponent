/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.client.officeconverter

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import jakarta.ws.rs.core.HttpHeaders.AUTHORIZATION
import jakarta.ws.rs.core.MultivaluedHashMap
import jakarta.ws.rs.core.MultivaluedMap

class OfficeConverterClientHeadersFactoryTest : BehaviorSpec({
    val incomingHeaders: MultivaluedMap<String, String> = MultivaluedHashMap()

    given("an office converter username and password") {
        val officeConverterClientHeadersFactory = OfficeConverterClientHeadersFactory(
            username = "fakeOfficeConverterUsername",
            password = "fakeOfficeConverterPassword"
        )

        `when`("the outgoing headers are updated") {
            val outgoingHeaders = officeConverterClientHeadersFactory.update(incomingHeaders, MultivaluedHashMap())

            then("a basic authentication header is added") {
                outgoingHeaders.getFirst(AUTHORIZATION) shouldBe
                    "Basic ZmFrZU9mZmljZUNvbnZlcnRlclVzZXJuYW1lOmZha2VPZmZpY2VDb252ZXJ0ZXJQYXNzd29yZA=="
            }
        }
    }
})
