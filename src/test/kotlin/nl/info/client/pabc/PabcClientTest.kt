/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.client.pabc

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder

class PabcClientTest : BehaviorSpec({
    context("The read role names") {
        given("the application roles that the OPA policies grant lezen for") {
            `when`("the read role names are inspected") {
                then("they are exactly the roles of the lezen rules in the OPA policies") {
                    READ_ROLE_NAMES shouldContainExactlyInAnyOrder listOf(
                        "raadpleger",
                        "behandelaar",
                        "coordinator",
                        "recordmanager",
                        "beheerder"
                    )
                }
            }
        }
    }
})
