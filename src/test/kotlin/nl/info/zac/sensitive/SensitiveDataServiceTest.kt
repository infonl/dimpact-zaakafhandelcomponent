/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.sensitive

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import java.util.UUID
import java.util.concurrent.ForkJoinPool
import java.util.concurrent.TimeUnit

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

    given("sensitive data whose UUID key has expired while the data still refers to that key") {
        val data = "sensitive-data-with-expired-key"
        val expiredUuid = UUID.randomUUID()
        SensitiveDataService.dataToUuidStorage.put(data, expiredUuid)

        `when`("the data is put into the service again") {
            val uuid = sensitiveDataService.put(data)

            then("a new UUID is returned that can be used to retrieve the data") {
                uuid shouldNotBe expiredUuid
                sensitiveDataService.get(uuid) shouldBe data
            }
        }
    }

    given("sensitive data that is stored under a new UUID key while an older UUID key for that data is still being removed") {
        val data = "sensitive-data-with-late-removal"
        val currentUuid = sensitiveDataService.put(data)
        val olderUuid = UUID.randomUUID()
        SensitiveDataService.uuidToDataStorage.put(olderUuid, data)

        `when`("the removal of the older UUID key completes") {
            SensitiveDataService.uuidToDataStorage.invalidate(olderUuid)
            // Caffeine runs removal listeners asynchronously on the common pool by default
            ForkJoinPool.commonPool().awaitQuiescence(5, TimeUnit.SECONDS)

            then("the data still refers to the new UUID key") {
                sensitiveDataService.put(data) shouldBe currentUuid
                sensitiveDataService.get(currentUuid) shouldBe data
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
