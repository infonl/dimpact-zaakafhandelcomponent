/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.signalering.model

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import nl.info.zac.identity.model.createUser

class SignaleringVerzondenZoekParametersTest : BehaviorSpec({
    context("Creating search parameters for the verzonden signaleringen of a user") {
        given("a user") {
            val user = createUser(username = "fakeUsername")

            `when`("the search parameters are created for the user") {
                val signaleringVerzondenZoekParameters = SignaleringVerzondenZoekParameters(user)

                then("the user is the target, identified by its username") {
                    with(signaleringVerzondenZoekParameters) {
                        targettype shouldBe SignaleringTarget.USER
                        target shouldBe "fakeUsername"
                    }
                }
            }
        }
    }
})
