/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.admin.model

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe

class UserModifiableZaaktypeConfigurationDataTest : BehaviorSpec({
    val zaaktypeConfiguration = createZaaktypeCmmnConfiguration()

    given("a set with a zaakafzender whose replyTo changed after it was added to the set") {
        val zaakafzender = createZaakAfzender(
            zaaktypeConfiguration = zaaktypeConfiguration,
            replyTo = "old@example.com"
        )
        val zaakafzenders = mutableSetOf(zaakafzender)
        zaakafzender.replyTo = "new@example.com"
        val candidate = createZaakAfzender(
            id = null,
            zaaktypeConfiguration = zaaktypeConfiguration,
            replyTo = "new@example.com"
        )

        `when`("an equal zaakafzender is set as component") {
            setComponent(zaakafzenders, candidate)

            then("the set still finds the existing zaakafzender, so that no duplicate is added") {
                zaakafzenders shouldContainExactly setOf(zaakafzender)
                (candidate in zaakafzenders) shouldBe true
            }
        }
    }

    given("a set with a zaakafzender") {
        val zaakafzender = createZaakAfzender(
            zaaktypeConfiguration = zaaktypeConfiguration,
            replyTo = "old@example.com"
        )
        val zaakafzenders = mutableSetOf(zaakafzender)
        val candidate = createZaakAfzender(
            id = null,
            zaaktypeConfiguration = zaaktypeConfiguration,
            replyTo = "new@example.com"
        )

        `when`("a zaakafzender with the same mail but another replyTo is set as component") {
            setComponent(zaakafzenders, candidate)

            then("the changes are applied to the existing zaakafzender, which the set still finds") {
                zaakafzenders shouldContainExactly setOf(zaakafzender)
                zaakafzender.replyTo shouldBe "new@example.com"
                (candidate in zaakafzenders) shouldBe true
            }
        }
    }

    given("a set with a zaakafzender and a candidate with another mail") {
        val zaakafzender = createZaakAfzender(zaaktypeConfiguration = zaaktypeConfiguration)
        val zaakafzenders = mutableSetOf(zaakafzender)
        val candidate = createZaakAfzender(
            id = 5678L,
            zaaktypeConfiguration = zaaktypeConfiguration,
            mail = "other@example.com"
        )

        `when`("the candidate is set as component") {
            setComponent(zaakafzenders, candidate)

            then("the candidate is added as a new element without an id") {
                zaakafzenders shouldContainExactly setOf(zaakafzender, candidate)
                candidate.id shouldBe null
            }
        }
    }
})
