/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.app.informatieobjecten.converter

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.checkUnnecessaryStub
import io.mockk.every
import io.mockk.mockk
import nl.info.client.zgw.ztc.ZtcClientService
import nl.info.client.zgw.ztc.model.createInformatieObjectType
import nl.info.client.zgw.ztc.model.generated.VertrouwelijkheidaanduidingEnum
import java.net.URI
import java.util.UUID

class RestInformatieobjecttypeConverterTest : BehaviorSpec({
    val ztcClientService = mockk<ZtcClientService>()
    val restInformatieobjecttypeConverter = RestInformatieobjecttypeConverter(ztcClientService)

    afterEach {
        checkUnnecessaryStub()
    }

    context("convertFromUris") {
        given("a list of informatieobjecttype URIs") {
            val uri1 = URI("https://example.com/informatieobjecttype/${UUID.randomUUID()}")
            val uri2 = URI("https://example.com/informatieobjecttype/${UUID.randomUUID()}")
            val type1 = createInformatieObjectType(
                uri = uri1,
                omschrijving = "fakeOmschrijving1",
                vertrouwelijkheidaanduiding = VertrouwelijkheidaanduidingEnum.OPENBAAR
            )
            val type2 = createInformatieObjectType(
                uri = uri2,
                omschrijving = "fakeOmschrijving2",
                vertrouwelijkheidaanduiding = VertrouwelijkheidaanduidingEnum.INTERN
            )
            every { ztcClientService.readInformatieobjecttype(uri1) } returns type1
            every { ztcClientService.readInformatieobjecttype(uri2) } returns type2

            `when`("convertFromUris is called") {
                val result = restInformatieobjecttypeConverter.convertFromUris(listOf(uri1, uri2))

                then("it returns a list of RestInformatieobjecttype with correct omschrijving") {
                    result.size shouldBe 2
                    result[0].omschrijving shouldBe "fakeOmschrijving1"
                    result[1].omschrijving shouldBe "fakeOmschrijving2"
                }

                then("vertrouwelijkheidaanduiding is mapped as uppercase enum name") {
                    result[0].vertrouwelijkheidaanduiding shouldBe VertrouwelijkheidaanduidingEnum.OPENBAAR.name
                    result[1].vertrouwelijkheidaanduiding shouldBe VertrouwelijkheidaanduidingEnum.INTERN.name
                }
            }
        }

        given("an empty list of URIs") {
            `when`("convertFromUris is called") {
                val result = restInformatieobjecttypeConverter.convertFromUris(emptyList())

                then("it returns an empty list") {
                    result shouldBe emptyList()
                }
            }
        }
    }
})
