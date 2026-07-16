/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.enkelvoudiginformatieobject

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.checkUnnecessaryStub
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import jakarta.persistence.EntityManager
import jakarta.persistence.TypedQuery
import jakarta.persistence.criteria.CriteriaBuilder
import jakarta.persistence.criteria.CriteriaQuery
import jakarta.persistence.criteria.Path
import jakarta.persistence.criteria.Predicate
import jakarta.persistence.criteria.Root
import nl.info.client.zgw.drc.DrcClientService
import nl.info.zac.enkelvoudiginformatieobject.model.EnkelvoudigInformatieObjectLock
import nl.info.zac.enkelvoudiginformatieobject.model.createEnkelvoudigInformatieObjectLock
import java.util.UUID

class EnkelvoudigInformatieObjectLockServiceTest : BehaviorSpec({
    val entityManager = mockk<EntityManager>()
    val criteriaBuilder = mockk<CriteriaBuilder>()
    val criteriaQuery = mockk<CriteriaQuery<EnkelvoudigInformatieObjectLock>>()
    val root = mockk<Root<EnkelvoudigInformatieObjectLock>>()
    val path = mockk<Path<Any>>()
    val predicate = mockk<Predicate>()
    val typedQuery = mockk<TypedQuery<EnkelvoudigInformatieObjectLock>>()
    val drcClientService = mockk<DrcClientService>()
    val service = EnkelvoudigInformatieObjectLockService(entityManager, drcClientService)

    afterEach { checkUnnecessaryStub() }

    fun setupCriteriaChain(results: List<EnkelvoudigInformatieObjectLock>) {
        every { entityManager.criteriaBuilder } returns criteriaBuilder
        every { criteriaBuilder.createQuery(EnkelvoudigInformatieObjectLock::class.java) } returns criteriaQuery
        every { criteriaQuery.from(EnkelvoudigInformatieObjectLock::class.java) } returns root
        every { root.get<Any>("enkelvoudiginformatieobjectUUID") } returns path
        every { criteriaBuilder.equal(path, any<UUID>()) } returns predicate
        every { criteriaQuery.select(root) } returns criteriaQuery
        every { criteriaQuery.where(predicate) } returns criteriaQuery
        every { entityManager.createQuery(criteriaQuery) } returns typedQuery
        every { typedQuery.resultList } returns results
    }

    context("createLock") {
        given("A valid information object UUID and user ID") {
            val fakeUuid = UUID.randomUUID()
            val fakeUserId = "fakeUserId"
            val fakeLockValue = "fakeLockValue"

            every { drcClientService.lockEnkelvoudigInformatieobject(fakeUuid) } returns fakeLockValue
            every { entityManager.persist(any()) } just runs
            every { entityManager.flush() } just runs

            `when`("createLock is called") {
                val result = service.createLock(fakeUuid, fakeUserId)

                then("the DRC client is called and the lock entity is persisted and returned") {
                    verify { drcClientService.lockEnkelvoudigInformatieobject(fakeUuid) }
                    verify { entityManager.persist(any()) }
                    verify { entityManager.flush() }
                    result.enkelvoudiginformatieobjectUUID shouldBe fakeUuid
                    result.userId shouldBe fakeUserId
                    result.lock shouldBe fakeLockValue
                }
            }
        }
    }

    context("findLock") {
        given("A lock exists for the given UUID") {
            val fakeUuid = UUID.randomUUID()
            val fakeLock = createEnkelvoudigInformatieObjectLock(enkelvoudigInformatieObjectUUID = fakeUuid)
            setupCriteriaChain(listOf(fakeLock))

            `when`("findLock is called") {
                val result = service.findLock(fakeUuid)

                then("the lock entity is returned") {
                    result shouldBe fakeLock
                }
            }
        }

        given("No lock exists for the given UUID") {
            val fakeUuid = UUID.randomUUID()
            setupCriteriaChain(emptyList())

            `when`("findLock is called") {
                val result = service.findLock(fakeUuid)

                then("null is returned") {
                    result shouldBe null
                }
            }
        }
    }

    context("readLock") {
        given("A lock exists for the given UUID") {
            val fakeUuid = UUID.randomUUID()
            val fakeLock = createEnkelvoudigInformatieObjectLock(enkelvoudigInformatieObjectUUID = fakeUuid)
            setupCriteriaChain(listOf(fakeLock))

            `when`("readLock is called") {
                val result = service.readLock(fakeUuid)

                then("the lock entity is returned") {
                    result shouldBe fakeLock
                }
            }
        }

        given("No lock exists for the given UUID") {
            val fakeUuid = UUID.randomUUID()
            setupCriteriaChain(emptyList())

            `when`("readLock is called") {
                val exception = shouldThrow<EnkelvoudigInformatieObjectLockNotFoundException> {
                    service.readLock(fakeUuid)
                }

                then("EnkelvoudigInformatieObjectLockNotFoundException is thrown") {
                    exception.message!!.contains(fakeUuid.toString()) shouldBe true
                }
            }
        }
    }

    context("deleteLock") {
        given("A lock exists for the given UUID") {
            val fakeUuid = UUID.randomUUID()
            val fakeLock = createEnkelvoudigInformatieObjectLock(
                enkelvoudigInformatieObjectUUID = fakeUuid,
                lock = "fakeLockValue"
            )
            setupCriteriaChain(listOf(fakeLock))
            every { drcClientService.unlockEnkelvoudigInformatieobject(fakeUuid, "fakeLockValue") } just runs
            every { entityManager.remove(fakeLock) } just runs
            every { entityManager.flush() } just runs

            `when`("deleteLock is called") {
                service.deleteLock(fakeUuid)

                then("the DRC client is called to unlock and the entity is removed") {
                    verify { drcClientService.unlockEnkelvoudigInformatieobject(fakeUuid, "fakeLockValue") }
                    verify { entityManager.remove(fakeLock) }
                    verify { entityManager.flush() }
                }
            }
        }

        given("No lock exists for the given UUID") {
            val fakeUuid = UUID.randomUUID()
            setupCriteriaChain(emptyList())

            `when`("deleteLock is called") {
                service.deleteLock(fakeUuid)

                then("neither the DRC client nor the entity manager remove method is called") {
                    verify(exactly = 0) { drcClientService.unlockEnkelvoudigInformatieobject(any(), any()) }
                    verify(exactly = 0) { entityManager.remove(any()) }
                }
            }
        }
    }
})
