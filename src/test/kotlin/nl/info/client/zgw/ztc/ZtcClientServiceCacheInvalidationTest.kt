/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.client.zgw.ztc

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.checkUnnecessaryStub
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import nl.info.client.zgw.shared.model.Results
import nl.info.client.zgw.util.extractUuid
import nl.info.client.zgw.ztc.model.createBesluitType
import nl.info.client.zgw.ztc.model.createInformatieObjectType
import nl.info.client.zgw.ztc.model.createRolType
import nl.info.client.zgw.ztc.model.createStatusType
import nl.info.client.zgw.ztc.model.createZaakType
import nl.info.client.zgw.ztc.model.createZaakTypeInformatieObjectType
import nl.info.client.zgw.ztc.model.generated.BesluitType
import nl.info.client.zgw.ztc.model.generated.InformatieObjectType
import nl.info.client.zgw.ztc.model.generated.RolType
import nl.info.client.zgw.ztc.model.generated.StatusType
import nl.info.client.zgw.ztc.model.generated.ZaakType
import java.net.URI
import java.util.UUID

class ZtcClientServiceCacheInvalidationTest : BehaviorSpec({
    fun createZtcClientService(ztcClient: ZtcClient) = ZtcClientService(
        ztcClient = ztcClient,
        cacheMaxSize = 100L,
        cacheExpirationSeconds = 86_400L
    )

    afterEach {
        checkUnnecessaryStub()
    }

    fun ZtcClient.stubTypeLists(
        zaakTypes: List<ZaakType>,
        statusTypes: List<StatusType> = emptyList(),
        rolTypes: List<RolType> = emptyList(),
        besluitTypes: List<BesluitType> = emptyList(),
        informatieObjectTypeByZaakType: Map<ZaakType, InformatieObjectType> = emptyMap()
    ) {
        val informatieObjectTypes = informatieObjectTypeByZaakType.values.toList()
        val zaakTypeInformatieObjectTypes = informatieObjectTypeByZaakType.map { (zaakType, informatieObjectType) ->
            createZaakTypeInformatieObjectType(zaaktype = zaakType.url, informatieobjecttype = informatieObjectType.url)
        }
        every { zaaktypeList(any()) } returns Results(zaakTypes, zaakTypes.size)
        every { statustypeList(any()) } returns Results(statusTypes, statusTypes.size)
        every { roltypeList(any()) } returns Results(rolTypes, rolTypes.size)
        every { besluittypeList(any()) } returns Results(besluitTypes, besluitTypes.size)
        every { informatieobjecttypeList(any()) } returns Results(informatieObjectTypes, informatieObjectTypes.size)
        every {
            zaaktypeinformatieobjecttypeList(any())
        } returns Results(zaakTypeInformatieObjectTypes, zaakTypeInformatieObjectTypes.size)
        every { resultaattypeList(any()) } returns Results(emptyList(), 0)
        every { eigenschapList(any()) } returns Results(emptyList(), 0)
    }

    given("two zaaktypen whose related types have been refreshed into the caches") {
        val invalidatedZtcClient = mockk<ZtcClient>()
        val invalidatedZtcClientService = createZtcClientService(invalidatedZtcClient)
        val catalogusUri = URI("https://example.com/catalogussen/${UUID.randomUUID()}")
        val changedZaakType = createZaakType()
        val otherZaakType = createZaakType()
        val otherStatusType = createStatusType().apply { zaaktype = otherZaakType.url }
        invalidatedZtcClient.stubTypeLists(
            zaakTypes = listOf(changedZaakType, otherZaakType),
            statusTypes = listOf(createStatusType().apply { zaaktype = changedZaakType.url }, otherStatusType),
            rolTypes = listOf(
                createRolType(zaakTypeUri = changedZaakType.url),
                createRolType(zaakTypeUri = otherZaakType.url)
            )
        )
        invalidatedZtcClientService.refreshCaches(catalogusUri)

        `when`("a notification for one of the zaaktypen invalidates it") {
            invalidatedZtcClientService.invalidateZaaktype(changedZaakType.url)

            then(
                """that zaaktype, its related types and the zaaktype list are removed from the caches, and the
                types of the other zaaktype stay in the caches"""
            ) {
                with(invalidatedZtcClientService.estimatedCacheSizes()) {
                    get("ZTC UUID -> ZaakType") shouldBe 1
                    get("ZTC URI -> List<ZaakType>") shouldBe 0
                    get("ZTC URI -> List<StatusType>") shouldBe 1
                    get("ZTC UUID -> StatusType") shouldBe 1
                    get("ZTC URI -> List<ResultaatType>") shouldBe 1
                    get("ZTC URI -> List<RolType>") shouldBe 1
                    get("ZTC UUID -> RolType") shouldBe 1
                }
                invalidatedZtcClientService.readStatustypen(otherZaakType.url) shouldBe listOf(otherStatusType)
                verify(exactly = 1) { invalidatedZtcClient.statustypeList(any()) }
            }
        }
    }

    given("a besluittype that is linked to one of three zaaktypen in the caches") {
        val invalidatedZtcClient = mockk<ZtcClient>()
        val invalidatedZtcClientService = createZtcClientService(invalidatedZtcClient)
        val catalogusUri = URI("https://example.com/catalogussen/${UUID.randomUUID()}")
        val linkedZaakType = createZaakType()
        val newlyLinkedZaakType = createZaakType()
        val unrelatedZaakType = createZaakType()
        val besluitType = createBesluitType(zaaktypen = listOf(linkedZaakType.url))
        invalidatedZtcClient.stubTypeLists(
            zaakTypes = listOf(linkedZaakType, newlyLinkedZaakType, unrelatedZaakType),
            besluitTypes = listOf(besluitType)
        )
        invalidatedZtcClientService.refreshCaches(catalogusUri)
        every {
            invalidatedZtcClient.besluittypeRead(besluitType.url.extractUuid())
        } returns createBesluitType(
            url = besluitType.url,
            zaaktypen = listOf(linkedZaakType.url, newlyLinkedZaakType.url)
        )

        `when`("an update notification invalidates the besluittype after it was linked to another zaaktype") {
            invalidatedZtcClientService.invalidateBesluittype(besluitType.url)

            then(
                """the besluittype and the besluittype lists of both linked zaaktypen are removed from the caches,
                and the besluittype list of the unrelated zaaktype stays"""
            ) {
                with(invalidatedZtcClientService.estimatedCacheSizes()) {
                    get("ZTC UUID -> BesluitType") shouldBe 0
                    get("ZTC URI -> List<BesluitType>") shouldBe 1
                }
                invalidatedZtcClientService.readBesluittypen(unrelatedZaakType.url) shouldBe emptyList()
            }
        }
    }

    given("a besluittype that is linked to one of two zaaktypen in the caches and that is deleted") {
        val invalidatedZtcClient = mockk<ZtcClient>()
        val invalidatedZtcClientService = createZtcClientService(invalidatedZtcClient)
        val catalogusUri = URI("https://example.com/catalogussen/${UUID.randomUUID()}")
        val linkedZaakType = createZaakType()
        val unrelatedZaakType = createZaakType()
        val besluitType = createBesluitType(zaaktypen = listOf(linkedZaakType.url))
        invalidatedZtcClient.stubTypeLists(
            zaakTypes = listOf(linkedZaakType, unrelatedZaakType),
            besluitTypes = listOf(besluitType)
        )
        invalidatedZtcClientService.refreshCaches(catalogusUri)

        `when`("a delete notification invalidates the besluittype") {
            invalidatedZtcClientService.invalidateBesluittype(besluitType.url, isDeleted = true)

            then(
                """the besluittype and the besluittype list that contains it are removed from the caches, without
                reading the deleted besluittype from the ZTC"""
            ) {
                with(invalidatedZtcClientService.estimatedCacheSizes()) {
                    get("ZTC UUID -> BesluitType") shouldBe 0
                    get("ZTC URI -> List<BesluitType>") shouldBe 1
                }
                verify(exactly = 0) { invalidatedZtcClient.besluittypeRead(any()) }
            }
        }
    }

    given("a besluittype in the caches that cannot be read from the ZTC") {
        val invalidatedZtcClient = mockk<ZtcClient>()
        val invalidatedZtcClientService = createZtcClientService(invalidatedZtcClient)
        val catalogusUri = URI("https://example.com/catalogussen/${UUID.randomUUID()}")
        val linkedZaakType = createZaakType()
        val unrelatedZaakType = createZaakType()
        val besluitType = createBesluitType(zaaktypen = listOf(linkedZaakType.url))
        invalidatedZtcClient.stubTypeLists(
            zaakTypes = listOf(linkedZaakType, unrelatedZaakType),
            besluitTypes = listOf(besluitType)
        )
        invalidatedZtcClientService.refreshCaches(catalogusUri)
        every {
            invalidatedZtcClient.besluittypeRead(besluitType.url.extractUuid())
        } throws RuntimeException("fakeExceptionMessage")

        `when`("an update notification invalidates the besluittype") {
            invalidatedZtcClientService.invalidateBesluittype(besluitType.url)

            then(
                """all besluittype lists are removed from the caches, because the zaaktypen that the besluittype
                is linked to now are unknown"""
            ) {
                with(invalidatedZtcClientService.estimatedCacheSizes()) {
                    get("ZTC UUID -> BesluitType") shouldBe 0
                    get("ZTC URI -> List<BesluitType>") shouldBe 0
                }
            }
        }
    }

    given("two zaaktypen in the caches that each have their own informatieobjecttype") {
        val invalidatedZtcClient = mockk<ZtcClient>()
        val invalidatedZtcClientService = createZtcClientService(invalidatedZtcClient)
        val catalogusUri = URI("https://example.com/catalogussen/${UUID.randomUUID()}")
        val zaakType = createZaakType()
        val otherZaakType = createZaakType()
        val changedInformatieObjectType = createInformatieObjectType()
        val otherInformatieObjectType = createInformatieObjectType()
        invalidatedZtcClient.stubTypeLists(
            zaakTypes = listOf(zaakType, otherZaakType),
            informatieObjectTypeByZaakType = mapOf(
                zaakType to changedInformatieObjectType,
                otherZaakType to otherInformatieObjectType
            )
        )
        invalidatedZtcClientService.refreshCaches(catalogusUri)

        `when`("a notification for one of the informatieobjecttypen invalidates it") {
            invalidatedZtcClientService.invalidateInformatieobjecttype(changedInformatieObjectType.url)

            then(
                """that informatieobjecttype and the informatieobjecttype list that contains it are removed from the
                caches, and the zaaktype-informatieobjecttypen and the other informatieobjecttype stay"""
            ) {
                with(invalidatedZtcClientService.estimatedCacheSizes()) {
                    get("ZTC UUID -> InformatieObjectType") shouldBe 1
                    get("ZTC URI -> List<InformatieObjectType>") shouldBe 1
                    get("ZTC URI -> List<ZaakTypeInformatieObjectType>") shouldBe 2
                }
                invalidatedZtcClientService.readInformatieobjecttypen(otherZaakType.url) shouldBe
                    listOf(otherInformatieObjectType)
            }
        }
    }
})
