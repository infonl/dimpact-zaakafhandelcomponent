/*
 * SPDX-FileCopyrightText: 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

package nl.info.zac.configuratie

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.string.shouldContain
import nl.info.client.brp.util.createBrpConfiguration
import java.util.Optional

class BrpConfigurationTest : BehaviorSpec({

    Given("No BRP audit log provider specified") {
        val brpConfiguration = createBrpConfiguration(auditLogProvider = Optional.empty())

        When("reading BRP audit log provider") {
            val exception = shouldThrow<IllegalArgumentException> {
                brpConfiguration.readBrpProtocolleringProvider()
            }

            Then("Exception is thrown") {
                BrpConfiguration.SUPPORTED_PROTOCOLLERING_PROVIDERS.forEach {
                    exception.message shouldContain it
                }
            }
        }
    }

    Given("Invalid BRP audit log provider specified") {
        val brpConfiguration = createBrpConfiguration(auditLogProvider = Optional.of("FakeProvider"))

        When("reading BRP audit log provider") {
            val exception = shouldThrow<IllegalArgumentException> {
                brpConfiguration.readBrpProtocolleringProvider()
            }

            Then("Exception is thrown") {
                exception.message shouldContain "FakeProvider"
            }
        }
    }
})
