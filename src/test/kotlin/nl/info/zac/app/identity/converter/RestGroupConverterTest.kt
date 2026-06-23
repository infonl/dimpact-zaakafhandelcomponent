/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.app.identity.converter

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.checkUnnecessaryStub
import io.mockk.every
import io.mockk.mockk
import nl.info.zac.identity.IdentityService
import nl.info.zac.identity.model.createGroup

class RestGroupConverterTest : BehaviorSpec({
    val identityService = mockk<IdentityService>()
    val restGroupConverter = RestGroupConverter(identityService)

    afterEach {
        checkUnnecessaryStub()
    }

    Context("convertGroupId") {
        Given("a group ID that exists in the identity service") {
            val group = createGroup(id = "fakeGroupId", name = "fakeGroupName")
            every { identityService.readGroup("fakeGroupId") } returns group

            When("convertGroupId is called") {
                val result = restGroupConverter.convertGroupId("fakeGroupId")

                Then("it returns a RestGroup with the correct id and naam") {
                    result.id shouldBe group.name
                    result.naam shouldBe group.description
                    result.active shouldBe group.active
                }
            }
        }
    }
})
