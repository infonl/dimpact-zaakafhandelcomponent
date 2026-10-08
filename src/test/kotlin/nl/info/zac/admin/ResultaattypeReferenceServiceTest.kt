/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.admin

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.mockk.checkUnnecessaryStub
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import nl.info.client.zgw.ztc.ZtcClientService
import nl.info.client.zgw.ztc.model.createResultaatType
import nl.info.client.zgw.ztc.model.createZaakType
import nl.info.zac.admin.model.createZaaktypeCmmnConfiguration
import nl.info.zac.admin.model.createZaaktypeCompletionParameters
import java.net.URI
import java.util.UUID

class ResultaattypeReferenceServiceTest : BehaviorSpec({
    val ztcClientService = mockk<ZtcClientService>()
    val resultaattypeReferenceService = ResultaattypeReferenceService(ztcClientService)

    afterEach {
        checkUnnecessaryStub()
    }

    given("a resultaattype UUID") {
        val resultaattypeUuid = UUID.randomUUID()
        every {
            ztcClientService.readResultaattype(resultaattypeUuid)
        } returns createResultaatType(omschrijving = "fakeToegekend")

        `when`("its omschrijving is read") {
            val omschrijving = resultaattypeReferenceService.readOmschrijving(resultaattypeUuid)

            then("the omschrijving of the resultaattype in ZTC is returned") {
                omschrijving shouldBe "fakeToegekend"
            }
        }
    }

    context("Finding the resultaattype of a reference") {
        val zaaktype = createZaakType()
        val zaaktypeUuid = UUID.randomUUID()

        given("a niet-ontvankelijk reference whose omschrijving matches a resultaattype of the zaaktype version") {
            val matchingResultaattype = createResultaatType(
                url = URI("https://example.com/resultaattypen/${UUID.randomUUID()}"),
                omschrijving = "fakeToegekend"
            )
            val zaaktypeConfiguration = createZaaktypeCmmnConfiguration(
                zaaktypeUUID = zaaktypeUuid,
                nietOntvankelijkResultaattypeOmschrijving = "fakeToegekend"
            )
            every { ztcClientService.readZaaktype(zaaktypeUuid) } returns zaaktype
            every {
                ztcClientService.readResultaattypen(zaaktype.url)
            } returns listOf(createResultaatType(omschrijving = "fakeAfgewezen"), matchingResultaattype)

            `when`("the niet-ontvankelijk resultaattype is found") {
                val resultaattype = resultaattypeReferenceService.findNietOntvankelijkResultaattype(zaaktypeConfiguration)

                then("the resultaattype of the zaaktype version with that omschrijving is returned") {
                    resultaattype shouldBe matchingResultaattype
                }
            }
        }

        given("a zaakbeeindig parameter whose omschrijving matches a resultaattype of the zaaktype version") {
            val matchingResultaattype = createResultaatType(omschrijving = "fakeIngetrokken")
            val zaaktypeConfiguration = createZaaktypeCmmnConfiguration(
                zaaktypeUUID = zaaktypeUuid,
                zaaktypeCompletionParameters = setOf(
                    createZaaktypeCompletionParameters(resultaattypeOmschrijving = "fakeIngetrokken")
                )
            )
            every { ztcClientService.readZaaktype(zaaktypeUuid) } returns zaaktype
            every { ztcClientService.readResultaattypen(zaaktype.url) } returns listOf(matchingResultaattype)

            `when`("the resultaattype of the zaakbeeindig parameter is found") {
                val resultaattype = resultaattypeReferenceService.findResultaattype(
                    zaaktypeConfiguration.getZaakbeeindigParameters().single()
                )

                then("the resultaattype with that omschrijving is returned") {
                    resultaattype shouldBe matchingResultaattype
                }
            }
        }

        given("a zaakbeeindig parameter whose omschrijving is missing from the zaaktype version") {
            val zaaktypeConfiguration = createZaaktypeCmmnConfiguration(
                zaaktypeUUID = zaaktypeUuid,
                zaaktypeCompletionParameters = setOf(
                    createZaaktypeCompletionParameters(resultaattypeOmschrijving = "fakeIngetrokken")
                )
            )
            every { ztcClientService.readZaaktype(zaaktypeUuid) } returns zaaktype
            every {
                ztcClientService.readResultaattypen(zaaktype.url)
            } returns listOf(createResultaatType(omschrijving = "fakeToegekend"))

            `when`("the resultaattype of the zaakbeeindig parameter is found") {
                val resultaattype = resultaattypeReferenceService.findResultaattype(
                    zaaktypeConfiguration.getZaakbeeindigParameters().single()
                )

                then("no resultaattype is returned") {
                    resultaattype shouldBe null
                }
            }
        }

        given("a configuration without a niet-ontvankelijk resultaattype") {
            val zaaktypeConfiguration = createZaaktypeCmmnConfiguration(nietOntvankelijkResultaattypeOmschrijving = null)

            `when`("the niet-ontvankelijk resultaattype is found") {
                val resultaattype = resultaattypeReferenceService.findNietOntvankelijkResultaattype(zaaktypeConfiguration)

                then("no resultaattype is returned without asking ZTC") {
                    resultaattype shouldBe null
                    verify(exactly = 0) { ztcClientService.readZaaktype(any<UUID>()) }
                }
            }
        }
    }

    context("Reading the niet-ontvankelijk resultaattype to end a zaak with") {
        val zaaktype = createZaakType()
        val zaaktypeUuid = UUID.randomUUID()

        given("a niet-ontvankelijk reference whose omschrijving matches a resultaattype of the zaaktype version") {
            val matchingResultaattype = createResultaatType(omschrijving = "fakeNietOntvankelijk")
            val zaaktypeConfiguration = createZaaktypeCmmnConfiguration(
                zaaktypeUUID = zaaktypeUuid,
                nietOntvankelijkResultaattypeOmschrijving = "fakeNietOntvankelijk"
            )
            every { ztcClientService.readZaaktype(zaaktypeUuid) } returns zaaktype
            every { ztcClientService.readResultaattypen(zaaktype.url) } returns listOf(matchingResultaattype)

            `when`("the niet-ontvankelijk resultaattype is read") {
                val resultaattype = resultaattypeReferenceService.readConfiguredNietOntvankelijkResultaattype(
                    zaaktypeConfiguration
                )

                then("the resultaattype with that omschrijving is returned") {
                    resultaattype shouldBe matchingResultaattype
                }
            }
        }

        given("a niet-ontvankelijk reference whose omschrijving is missing from the zaaktype version") {
            val zaaktypeConfiguration = createZaaktypeCmmnConfiguration(
                zaaktypeUUID = zaaktypeUuid,
                nietOntvankelijkResultaattypeOmschrijving = "fakeNietOntvankelijk"
            )
            every { ztcClientService.readZaaktype(zaaktypeUuid) } returns zaaktype
            every {
                ztcClientService.readResultaattypen(zaaktype.url)
            } returns listOf(createResultaatType(omschrijving = "fakeToegekend"))

            `when`("the niet-ontvankelijk resultaattype is read") {
                val exception = shouldThrow<IllegalStateException> {
                    resultaattypeReferenceService.readConfiguredNietOntvankelijkResultaattype(zaaktypeConfiguration)
                }

                then("it fails with the zaaktype and the omschrijving that cannot be resolved") {
                    exception.message shouldContain zaaktypeUuid.toString()
                    exception.message shouldContain "fakeNietOntvankelijk"
                }
            }
        }

        given("a configuration without a niet-ontvankelijk resultaattype") {
            val zaaktypeConfiguration = createZaaktypeCmmnConfiguration(nietOntvankelijkResultaattypeOmschrijving = null)

            `when`("the niet-ontvankelijk resultaattype is read") {
                val resultaattype = resultaattypeReferenceService.readConfiguredNietOntvankelijkResultaattype(
                    zaaktypeConfiguration
                )

                then("no resultaattype is returned without asking ZTC") {
                    resultaattype shouldBe null
                    verify(exactly = 0) { ztcClientService.readZaaktype(any<UUID>()) }
                }
            }
        }
    }
})
