/*
 * SPDX-FileCopyrightText: 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
@file:Suppress("PackageName")

package nl.info.client.or.`object`

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.checkUnnecessaryStub
import io.mockk.every
import io.mockk.mockk
import nl.info.client.or.`object`.model.createModelObject

class ObjectsClientServiceTest : BehaviorSpec({
    val objectsClient = mockk<ObjectsClient>()
    val service = ObjectsClientService(objectsClient)

    afterEach {
        checkUnnecessaryStub()
    }

    context("Reading an object") {
        given("An existing object") {
            val modelObject = createModelObject()
            every { objectsClient.objectRead(modelObject.uuid) } returns modelObject

            `when`("the object is read using the UUID") {
                val returnedObject = service.readObject(modelObject.uuid)

                then("the object is returned") {
                    returnedObject shouldBe modelObject
                }
            }
        }
    }
})
