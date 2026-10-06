/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.admin

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.checkUnnecessaryStub
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import nl.info.client.zgw.ztc.ZtcClientService
import nl.info.client.zgw.ztc.model.createResultaatType
import nl.info.client.zgw.ztc.model.createZaakType
import nl.info.zac.admin.model.ZaakbeeindigReden
import nl.info.zac.admin.model.ZaaktypeCompletionParameters
import nl.info.zac.admin.model.createZaaktypeCmmnConfiguration
import java.net.URI
import java.util.UUID

class ResultaattypeReferenceServiceTest : BehaviorSpec({
    val ztcClientService = mockk<ZtcClientService>()
    val resultaattypeReferenceService = ResultaattypeReferenceService(ztcClientService)

    afterEach {
        checkUnnecessaryStub()
    }

    fun createCompletionParameters(resultaattypeUuid: UUID, resultaattypeOmschrijving: String?) =
        ZaaktypeCompletionParameters().apply {
            zaakbeeindigReden = ZaakbeeindigReden().apply {
                id = 1L
                naam = "fakeZaakbeeindigReden"
            }
            resultaattype = resultaattypeUuid
            this.resultaattypeOmschrijving = resultaattypeOmschrijving
        }

    context("Filling the omschrijvingen of the resultaattype references") {
        given("a configuration with a niet-ontvankelijk resultaattype and a zaakbeeindig parameter") {
            val nietOntvankelijkResultaattypeUuid = UUID.randomUUID()
            val completionResultaattypeUuid = UUID.randomUUID()
            val zaaktypeConfiguration = createZaaktypeCmmnConfiguration(
                nietOntvankelijkResultaattype = nietOntvankelijkResultaattypeUuid,
                zaaktypeCompletionParameters = setOf(
                    createCompletionParameters(completionResultaattypeUuid, resultaattypeOmschrijving = null)
                )
            )
            every {
                ztcClientService.readResultaattype(nietOntvankelijkResultaattypeUuid)
            } returns createResultaatType(omschrijving = "fakeNietOntvankelijk")
            every {
                ztcClientService.readResultaattype(completionResultaattypeUuid)
            } returns createResultaatType(omschrijving = "fakeIngetrokken")

            `when`("the omschrijvingen are filled") {
                resultaattypeReferenceService.fillOmschrijvingen(zaaktypeConfiguration)

                then("every reference holds the omschrijving of its resultaattype") {
                    zaaktypeConfiguration.nietOntvankelijkResultaattypeOmschrijving shouldBe "fakeNietOntvankelijk"
                    zaaktypeConfiguration.getZaakbeeindigParameters().single().resultaattypeOmschrijving shouldBe
                        "fakeIngetrokken"
                }
            }
        }

        given("a configuration without a niet-ontvankelijk resultaattype and with a stale omschrijving") {
            val zaaktypeConfiguration = createZaaktypeCmmnConfiguration().apply {
                nietOntvankelijkResultaattype = null
                nietOntvankelijkResultaattypeOmschrijving = "fakeStaleOmschrijving"
            }

            `when`("the omschrijvingen are filled") {
                resultaattypeReferenceService.fillOmschrijvingen(zaaktypeConfiguration)

                then("the niet-ontvankelijk omschrijving is cleared without reading a resultaattype") {
                    zaaktypeConfiguration.nietOntvankelijkResultaattypeOmschrijving shouldBe null
                    verify(exactly = 0) { ztcClientService.readResultaattype(any<UUID>()) }
                }
            }
        }
    }

    context("Reading the resultaattype of a reference") {
        val zaaktype = createZaakType()
        val zaaktypeUuid = UUID.randomUUID()

        given("a reference whose omschrijving matches a resultaattype of the zaaktype version") {
            val storedResultaattypeUuid = UUID.randomUUID()
            val currentResultaattype = createResultaatType(
                url = URI("https://example.com/resultaattypen/${UUID.randomUUID()}"),
                omschrijving = "fakeToegekend"
            )
            val zaaktypeConfiguration = createZaaktypeCmmnConfiguration(
                zaaktypeUUID = zaaktypeUuid,
                nietOntvankelijkResultaattype = storedResultaattypeUuid
            ).apply { nietOntvankelijkResultaattypeOmschrijving = "fakeToegekend" }
            every { ztcClientService.readZaaktype(zaaktypeUuid) } returns zaaktype
            every {
                ztcClientService.readResultaattypen(zaaktype.url)
            } returns listOf(createResultaatType(omschrijving = "fakeAfgewezen"), currentResultaattype)

            `when`("the niet-ontvankelijk resultaattype is read") {
                val resultaattype = resultaattypeReferenceService.readNietOntvankelijkResultaattype(zaaktypeConfiguration)

                then("the resultaattype with that omschrijving is returned, not the one with the stored UUID") {
                    resultaattype shouldBe currentResultaattype
                    verify(exactly = 0) { ztcClientService.readResultaattype(storedResultaattypeUuid) }
                }
            }
        }

        given("a reference without an omschrijving") {
            val storedResultaattypeUuid = UUID.randomUUID()
            val storedResultaattype = createResultaatType()
            val zaaktypeConfiguration = createZaaktypeCmmnConfiguration(
                zaaktypeUUID = zaaktypeUuid,
                zaaktypeCompletionParameters = setOf(
                    createCompletionParameters(storedResultaattypeUuid, resultaattypeOmschrijving = null)
                )
            )
            every { ztcClientService.readResultaattype(storedResultaattypeUuid) } returns storedResultaattype

            `when`("the resultaattype of the zaakbeeindig parameter is read") {
                val resultaattype = resultaattypeReferenceService.readResultaattype(
                    zaaktypeConfiguration.getZaakbeeindigParameters().single()
                )

                then("the resultaattype with the stored UUID is returned") {
                    resultaattype shouldBe storedResultaattype
                }
            }
        }

        given("a reference whose omschrijving is missing from the zaaktype version") {
            val storedResultaattypeUuid = UUID.randomUUID()
            val storedResultaattype = createResultaatType()
            val zaaktypeConfiguration = createZaaktypeCmmnConfiguration(
                zaaktypeUUID = zaaktypeUuid,
                zaaktypeCompletionParameters = setOf(
                    createCompletionParameters(storedResultaattypeUuid, resultaattypeOmschrijving = "fakeIngetrokken")
                )
            )
            every { ztcClientService.readZaaktype(zaaktypeUuid) } returns zaaktype
            every {
                ztcClientService.readResultaattypen(zaaktype.url)
            } returns listOf(createResultaatType(omschrijving = "fakeToegekend"))
            every { ztcClientService.readResultaattype(storedResultaattypeUuid) } returns storedResultaattype

            `when`("the resultaattype of the zaakbeeindig parameter is read") {
                val resultaattype = resultaattypeReferenceService.readResultaattype(
                    zaaktypeConfiguration.getZaakbeeindigParameters().single()
                )

                then("the resultaattype with the stored UUID is returned") {
                    resultaattype shouldBe storedResultaattype
                }
            }
        }

        given("a configuration without a niet-ontvankelijk resultaattype") {
            val zaaktypeConfiguration = createZaaktypeCmmnConfiguration().apply { nietOntvankelijkResultaattype = null }

            `when`("the niet-ontvankelijk resultaattype is read") {
                val resultaattype = resultaattypeReferenceService.readNietOntvankelijkResultaattype(zaaktypeConfiguration)

                then("no resultaattype is returned") {
                    resultaattype shouldBe null
                }
            }
        }
    }
})
