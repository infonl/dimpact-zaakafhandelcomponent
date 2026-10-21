/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.util

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.mockk.checkUnnecessaryStub
import io.mockk.every
import io.mockk.mockk
import jakarta.ws.rs.core.Response
import nl.info.client.opa.OpaAdminClient

class PoliciesDeployerTest : BehaviorSpec({
    val opaAdminClient = mockk<OpaAdminClient>()
    val policiesDeployer = PoliciesDeployer(opaAdminClient)

    afterEach {
        checkUnnecessaryStub()
    }

    context("Deploying the policies on startup") {
        given("the policies that the policies file lists") {
            val moduleIds = mutableListOf<String>()
            val policies = mutableListOf<String>()
            every { opaAdminClient.policyUpdate(capture(moduleIds), capture(policies)) } returns Response.ok().build()

            `when`("the application starts") {
                policiesDeployer.onStartup(Any())

                then("each policy is deployed with its file name without extension as module id") {
                    moduleIds shouldBe listOf(
                        "rollen",
                        "zaak-rechten",
                        "taak-rechten",
                        "document-rechten",
                        "werklijst-rechten",
                        "notitie-rechten",
                        "overige-rechten",
                        "brp-rechten"
                    )
                }

                and("the content of the policy file is deployed") {
                    policies.first() shouldContain "package net.atos.zac.rol"
                }
            }
        }
    }
})
