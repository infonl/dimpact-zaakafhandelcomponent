/*
 * SPDX-FileCopyrightText: 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.zac.gebruikersvoorkeuren

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.checkUnnecessaryStub
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import jakarta.persistence.EntityManager
import net.atos.zac.gebruikersvoorkeuren.model.createZoekopdracht
import nl.info.zac.signalering.SignaleringService

class GebruikersvoorkeurenServiceTest : BehaviorSpec({
    val entityManager = mockk<EntityManager>()
    val signaleringService = mockk<SignaleringService>()
    val gebruikersvoorkeurenService = GebruikersvoorkeurenService(entityManager, signaleringService)

    beforeEach {
        checkUnnecessaryStub()
    }

    Given("A zoekopdracht that already exists") {
        val zoekopdracht = createZoekopdracht()
        val mergedZoekopdracht = createZoekopdracht(
            id = zoekopdracht.id,
            name = "mergedZoekopdracht"
        )

        every { entityManager.merge(zoekopdracht) } returns mergedZoekopdracht

        When("the create zoekopdracht function is called") {
            val result = gebruikersvoorkeurenService.createZoekopdracht(zoekopdracht)

            Then("it should merge the zoekopdracht and return the uprated zoekopdracht") {
                result shouldBe mergedZoekopdracht
                verify { entityManager.merge(zoekopdracht) }
            }
        }
    }
})
