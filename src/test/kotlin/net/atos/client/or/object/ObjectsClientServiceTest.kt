/*
 * SPDX-FileCopyrightText: 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
@file:Suppress("PackageName")

package net.atos.client.or.`object`

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.checkUnnecessaryStub
import io.mockk.every
import io.mockk.mockk
import net.atos.client.or.`object`.model.createModelObject

class ObjectsClientServiceTest : BehaviorSpec({
    val objectsClient = mockk<ObjectsClient>()
    val service = ObjectsClientService(objectsClient)

    beforeEach {
        checkUnnecessaryStub()
    }

    Context("Reading an object") {
        Given("An existing object") {
            val modelObject = createModelObject()
            every { objectsClient.objectRead(modelObject.uuid) } returns modelObject

            When("the object is read using the UUID") {
                val returnedObject = service.readObject(modelObject.uuid)

                Then("the object is returned") {
                    returnedObject shouldBe modelObject
                }
            }
        }
    }
})
