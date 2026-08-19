/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.productaanvraag

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.mockk.checkUnnecessaryStub
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import jakarta.persistence.EntityManager
import jakarta.persistence.Query
import java.util.UUID

class ProductaanvraagClaimRepositoryTest : BehaviorSpec({
    val entityManager = mockk<EntityManager>()
    val query = mockk<Query>()
    val productaanvraagClaimRepository = ProductaanvraagClaimRepository(
        entityManager = entityManager,
        claimTimeoutMinutes = 42
    )
    val productaanvraagObjectUUID = UUID.randomUUID()

    afterEach { checkUnnecessaryStub() }

    context("Claiming a productaanvraag") {
        given("a productaanvraag that is not claimed, or whose claim has gone stale") {
            val sqlStatement = slot<String>()
            every { entityManager.createNativeQuery(capture(sqlStatement)) } returns query
            every { query.setParameter(any<String>(), any()) } returns query
            every { query.executeUpdate() } returns 1

            `when`("the productaanvraag is claimed") {
                val claimed = productaanvraagClaimRepository.claim(productaanvraagObjectUUID)

                then("the claim succeeds using the configured staleness period") {
                    claimed shouldBe true
                    sqlStatement.captured shouldContain "ON CONFLICT"
                    verify(exactly = 1) {
                        query.setParameter("claimTimeoutMinutes", 42)
                    }
                }
            }
        }

        given("a productaanvraag that is already being handled or has already been handled") {
            every { entityManager.createNativeQuery(any<String>()) } returns query
            every { query.setParameter(any<String>(), any()) } returns query
            every { query.executeUpdate() } returns 0

            `when`("the productaanvraag is claimed") {
                val claimed = productaanvraagClaimRepository.claim(productaanvraagObjectUUID)

                then("the claim is rejected") {
                    claimed shouldBe false
                }
            }
        }
    }

    context("Marking a productaanvraag as done") {
        given("a claimed productaanvraag") {
            val sqlStatement = slot<String>()
            every { entityManager.createNativeQuery(capture(sqlStatement)) } returns query
            every { query.setParameter(any<String>(), any()) } returns query
            every { query.executeUpdate() } returns 1

            `when`("the productaanvraag is marked as done") {
                productaanvraagClaimRepository.markDone(productaanvraagObjectUUID)

                then("its claim status is set to 'done'") {
                    sqlStatement.captured shouldContain "SET status = 'DONE'"
                    verify(exactly = 1) {
                        query.setParameter("productaanvraagObjectUUID", productaanvraagObjectUUID.toString())
                    }
                }
            }
        }
    }
})
