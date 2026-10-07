/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.admin.model

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeSameInstanceAs

class UserModifiableZaaktypeConfigurationDataTest : BehaviorSpec({
    val zaaktypeConfiguration = createZaaktypeCmmnConfiguration()

    given("a set with a zaakafzender and a desired zaakafzender with the same mail but another replyTo") {
        val zaakafzender = createZaakAfzender(
            id = 1L,
            zaaktypeConfiguration = zaaktypeConfiguration,
            replyTo = "old@example.com"
        )
        val zaakafzenders = mutableSetOf(zaakafzender)
        val desiredZaakafzender = createZaakAfzender(
            id = null,
            zaaktypeConfiguration = zaaktypeConfiguration,
            replyTo = "new@example.com"
        )

        `when`("the set is merged with the desired zaakafzender") {
            zaakafzenders.mergeWith(listOf(desiredZaakafzender)) { it.mail }

            then("the changes are applied to the existing zaakafzender, so that it keeps its id") {
                zaakafzenders.single() shouldBeSameInstanceAs zaakafzender
                zaakafzender.id shouldBe 1L
                zaakafzender.replyTo shouldBe "new@example.com"
            }
        }
    }

    given("a set with a zaakafzender and a desired zaakafzender with another mail") {
        val zaakafzender = createZaakAfzender(zaaktypeConfiguration = zaaktypeConfiguration)
        val zaakafzenders = mutableSetOf(zaakafzender)
        val desiredZaakafzender = createZaakAfzender(
            id = 5678L,
            zaaktypeConfiguration = zaaktypeConfiguration,
            mail = "other@example.com"
        )

        `when`("the set is merged with the existing and the desired zaakafzender") {
            zaakafzenders.mergeWith(listOf(zaakafzender, desiredZaakafzender)) { it.mail }

            then("the desired zaakafzender is added without an id, so that JPA inserts it as a new row") {
                zaakafzenders shouldContainExactlyInAnyOrder listOf(zaakafzender, desiredZaakafzender)
                desiredZaakafzender.id shouldBe null
            }
        }
    }

    given("a set with two zaakafzenders") {
        val keptZaakafzender = createZaakAfzender(zaaktypeConfiguration = zaaktypeConfiguration, mail = "kept@example.com")
        val removedZaakafzender = createZaakAfzender(
            zaaktypeConfiguration = zaaktypeConfiguration,
            mail = "removed@example.com"
        )
        val zaakafzenders = mutableSetOf(keptZaakafzender, removedZaakafzender)

        `when`("the set is merged with only one of them") {
            zaakafzenders.mergeWith(listOf(keptZaakafzender)) { it.mail }

            then("the zaakafzender that is not desired is removed") {
                zaakafzenders shouldContainExactly setOf(keptZaakafzender)
            }
        }
    }

    given("a set with a zaakafzender and two desired zaakafzenders with the same mail") {
        val zaakafzender = createZaakAfzender(zaaktypeConfiguration = zaaktypeConfiguration)
        val zaakafzenders = mutableSetOf(zaakafzender)
        val desiredZaakafzenders = listOf(
            createZaakAfzender(zaaktypeConfiguration = zaaktypeConfiguration, replyTo = "first@example.com"),
            createZaakAfzender(zaaktypeConfiguration = zaaktypeConfiguration, replyTo = "second@example.com")
        )

        `when`("the set is merged with the desired zaakafzenders") {
            val exception = shouldThrow<IllegalArgumentException> {
                zaakafzenders.mergeWith(desiredZaakafzenders) { it.mail }
            }

            then("the merge fails and leaves the set unchanged, so that no desired zaakafzender is silently dropped") {
                exception.message shouldBe "Desired elements have duplicate keys"
                zaakafzenders shouldContainExactly setOf(zaakafzender)
                zaakafzender.replyTo shouldBe "replyTo@example.com"
            }
        }
    }
})
