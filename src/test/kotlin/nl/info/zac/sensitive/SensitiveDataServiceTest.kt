/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.sensitive

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import java.util.UUID

class SensitiveDataServiceTest : BehaviorSpec({
    val sensitiveDataService = SensitiveDataService()

    given("a string of sensitive data") {
        `when`("the data is put into the service") {
            val data = "some-sensitive-data"

            val uuid = sensitiveDataService.put(data)

            then("a UUID is returned") {
                uuid shouldNotBe null
            }

            then("the data can be retrieved using the UUID") {
                val retrieved = sensitiveDataService.get(uuid)
                retrieved shouldBe data
            }
        }

        `when`("the same data is put into the service multiple times") {
            val data = "fakeSensitiveData"

            val uuid1 = sensitiveDataService.put(data)
            val uuid2 = sensitiveDataService.put(data)

            then("the same UUID is returned each time") {
                uuid1 shouldBe uuid2
            }
        }
    }

    given("multiple strings of sensitive data") {
        val data1 = "sensitive-data-1"
        val data2 = "sensitive-data-2"

        `when`("the data strings are put into the service") {
            val uuid1 = sensitiveDataService.put(data1)
            val uuid2 = sensitiveDataService.put(data2)

            then("different UUIDs are returned") {
                uuid1 shouldNotBe uuid2
            }

            then("each data string can be retrieved using its respective UUID") {
                sensitiveDataService.get(uuid1) shouldBe data1
                sensitiveDataService.get(uuid2) shouldBe data2
            }
        }
    }

    given("a UUID that is not in the service") {
        val randomUuid = UUID.randomUUID()

        `when`("retrieving data with that UUID") {
            val retrievedData = sensitiveDataService.get(randomUuid)

            then("null is returned") {
                retrievedData shouldBe null
            }
        }
    }
})
