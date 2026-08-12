/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.app.admin

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.mockk.checkUnnecessaryStub
import io.mockk.every
import io.mockk.mockk
import nl.info.client.zgw.ztc.ZtcClientService
import nl.info.client.zgw.ztc.model.createResultaatType
import nl.info.client.zgw.ztc.model.createZaakType
import nl.info.zac.admin.ZaaktypeHelperService
import nl.info.zac.admin.model.ZaakbeeindigReden
import nl.info.zac.admin.model.ZaaktypeCmmnConfiguration
import nl.info.zac.admin.model.ZaaktypeCompletionParameters
import nl.info.zac.admin.model.createZaaktypeCmmnConfiguration
import java.net.URI
import java.util.UUID

/**
 * Regression tests for PZ-12241: when a new version of a zaaktype is published, the 'zaak beeindigen gegevens'
 * (completion parameters) must be matched to the new resultaattypen by omschrijving. A shadowed lambda parameter
 * caused every previous resultaattype to be mapped to the *first* resultaattype of the new zaaktype instead.
 */
class ZaaktypeHelperServiceTest : BehaviorSpec({
    val ztcClientService = mockk<ZtcClientService>()
    val zaaktypeHelperService = ZaaktypeHelperService(ztcClientService)

    afterEach {
        checkUnnecessaryStub()
    }

    fun resultaattypeUri(uuid: UUID) = URI("https://example.com/resultaattype/$uuid")

    fun zaakbeeindigReden(id: Long, naam: String) = ZaakbeeindigReden().apply {
        this.id = id
        this.naam = naam
    }

    fun completionParameter(
        zaaktypeCmmnConfiguration: ZaaktypeCmmnConfiguration,
        zaakbeeindigReden: ZaakbeeindigReden,
        resultaattype: UUID
    ) = ZaaktypeCompletionParameters().apply {
        this.zaaktypeConfiguration = zaaktypeCmmnConfiguration
        this.zaakbeeindigReden = zaakbeeindigReden
        this.resultaattype = resultaattype
    }

    context("mapZaakbeeindigGegevens") {
        given("a previous configuration whose resultaattypen are not the first ones of the new zaaktype") {
            val previousAfgebrokenUuid = UUID.randomUUID()
            val previousToegekendUuid = UUID.randomUUID()
            val previousNietOntvankelijkUuid = UUID.randomUUID()

            val newVerlengdUuid = UUID.randomUUID()
            val newToegekendUuid = UUID.randomUUID()
            val newNietOntvankelijkUuid = UUID.randomUUID()
            val newAfgebrokenUuid = UUID.randomUUID()

            // the new zaaktype deliberately lists 'Verlengd' first so that a mapping which always picks the
            // first resultaattype produces the wrong result
            val newZaaktype = createZaakType(
                resultTypes = listOf(
                    resultaattypeUri(newVerlengdUuid),
                    resultaattypeUri(newToegekendUuid),
                    resultaattypeUri(newNietOntvankelijkUuid),
                    resultaattypeUri(newAfgebrokenUuid)
                )
            )

            every { ztcClientService.readResultaattype(resultaattypeUri(newVerlengdUuid)) } returns
                createResultaatType(url = resultaattypeUri(newVerlengdUuid), omschrijving = "Verlengd")
            every { ztcClientService.readResultaattype(resultaattypeUri(newToegekendUuid)) } returns
                createResultaatType(url = resultaattypeUri(newToegekendUuid), omschrijving = "Toegekend")
            every { ztcClientService.readResultaattype(resultaattypeUri(newNietOntvankelijkUuid)) } returns
                createResultaatType(url = resultaattypeUri(newNietOntvankelijkUuid), omschrijving = "Niet ontvankelijk")
            every { ztcClientService.readResultaattype(resultaattypeUri(newAfgebrokenUuid)) } returns
                createResultaatType(url = resultaattypeUri(newAfgebrokenUuid), omschrijving = "Afgebroken")
            every { ztcClientService.readResultaattype(previousAfgebrokenUuid) } returns
                createResultaatType(url = resultaattypeUri(previousAfgebrokenUuid), omschrijving = "Afgebroken")
            every { ztcClientService.readResultaattype(previousToegekendUuid) } returns
                createResultaatType(url = resultaattypeUri(previousToegekendUuid), omschrijving = "Toegekend")
            every { ztcClientService.readResultaattype(previousNietOntvankelijkUuid) } returns
                createResultaatType(
                    url = resultaattypeUri(previousNietOntvankelijkUuid),
                    omschrijving = "Niet ontvankelijk"
                )

            val previousZaaktypeCmmnConfiguration = createZaaktypeCmmnConfiguration(
                nietOntvankelijkResultaattype = previousNietOntvankelijkUuid
            )
            val zaakbeeindigRedenAfgebroken = zaakbeeindigReden(id = 1L, naam = "Zaak is afgebroken")
            val zaakbeeindigRedenToegekend = zaakbeeindigReden(id = 2L, naam = "Zaak is toegekend")
            previousZaaktypeCmmnConfiguration.setZaakbeeindigParameters(
                listOf(
                    completionParameter(
                        previousZaaktypeCmmnConfiguration,
                        zaakbeeindigRedenAfgebroken,
                        previousAfgebrokenUuid
                    ),
                    completionParameter(
                        previousZaaktypeCmmnConfiguration,
                        zaakbeeindigRedenToegekend,
                        previousToegekendUuid
                    )
                )
            )
            val newZaaktypeCmmnConfiguration = createZaaktypeCmmnConfiguration(
                zaaktypeUUID = UUID.randomUUID()
            )

            `when`("the zaakbeeindig gegevens are mapped to the new configuration") {
                zaaktypeHelperService.mapZaakbeeindigGegevens(
                    previousZaaktypeCmmnConfiguration,
                    newZaaktypeCmmnConfiguration,
                    newZaaktype
                )

                then("every parameter is matched on omschrijving instead of on position") {
                    val zaakbeeindigParameters = newZaaktypeCmmnConfiguration.getZaakbeeindigParameters()
                    zaakbeeindigParameters shouldHaveSize 2
                    zaakbeeindigParameters.first {
                        it.zaakbeeindigReden.id == 1L
                    }.resultaattype shouldBe newAfgebrokenUuid
                    zaakbeeindigParameters.first {
                        it.zaakbeeindigReden.id == 2L
                    }.resultaattype shouldBe newToegekendUuid
                }

                then("the niet-ontvankelijk resultaattype is matched on omschrijving as well") {
                    newZaaktypeCmmnConfiguration.nietOntvankelijkResultaattype shouldBe newNietOntvankelijkUuid
                }
            }
        }

        given("a previous configuration with a resultaattype that no longer exists in the new zaaktype") {
            val previousRemovedUuid = UUID.randomUUID()
            val previousNietOntvankelijkUuid = UUID.randomUUID()
            val newToegekendUuid = UUID.randomUUID()

            val newZaaktype = createZaakType(resultTypes = listOf(resultaattypeUri(newToegekendUuid)))

            every { ztcClientService.readResultaattype(resultaattypeUri(newToegekendUuid)) } returns
                createResultaatType(url = resultaattypeUri(newToegekendUuid), omschrijving = "Toegekend")
            every { ztcClientService.readResultaattype(previousRemovedUuid) } returns
                createResultaatType(url = resultaattypeUri(previousRemovedUuid), omschrijving = "Verwijderd")
            every { ztcClientService.readResultaattype(previousNietOntvankelijkUuid) } returns
                createResultaatType(
                    url = resultaattypeUri(previousNietOntvankelijkUuid),
                    omschrijving = "Niet ontvankelijk"
                )

            val previousZaaktypeCmmnConfiguration = createZaaktypeCmmnConfiguration(
                nietOntvankelijkResultaattype = previousNietOntvankelijkUuid
            )
            previousZaaktypeCmmnConfiguration.setZaakbeeindigParameters(
                listOf(
                    completionParameter(
                        previousZaaktypeCmmnConfiguration,
                        zaakbeeindigReden(id = 1L, naam = "Zaak is verwijderd"),
                        previousRemovedUuid
                    )
                )
            )
            val newZaaktypeCmmnConfiguration = createZaaktypeCmmnConfiguration(zaaktypeUUID = UUID.randomUUID())

            `when`("the zaakbeeindig gegevens are mapped to the new configuration") {
                zaaktypeHelperService.mapZaakbeeindigGegevens(
                    previousZaaktypeCmmnConfiguration,
                    newZaaktypeCmmnConfiguration,
                    newZaaktype
                )

                then("the unmatched parameters are not copied instead of being mapped to an arbitrary resultaattype") {
                    newZaaktypeCmmnConfiguration.getZaakbeeindigParameters() shouldHaveSize 0
                    newZaaktypeCmmnConfiguration.nietOntvankelijkResultaattype.shouldBeNull()
                }
            }
        }
    }

    context("updateZaakbeeindigGegevens") {
        given("an existing configuration whose zaaktype got new resultaattype UUIDs") {
            val previousToegekendUuid = UUID.randomUUID()
            val previousNietOntvankelijkUuid = UUID.randomUUID()
            val newAfgebrokenUuid = UUID.randomUUID()
            val newToegekendUuid = UUID.randomUUID()
            val newNietOntvankelijkUuid = UUID.randomUUID()

            val newZaaktype = createZaakType(
                resultTypes = listOf(
                    resultaattypeUri(newAfgebrokenUuid),
                    resultaattypeUri(newToegekendUuid),
                    resultaattypeUri(newNietOntvankelijkUuid)
                )
            )

            every { ztcClientService.readResultaattype(resultaattypeUri(newAfgebrokenUuid)) } returns
                createResultaatType(url = resultaattypeUri(newAfgebrokenUuid), omschrijving = "Afgebroken")
            every { ztcClientService.readResultaattype(resultaattypeUri(newToegekendUuid)) } returns
                createResultaatType(url = resultaattypeUri(newToegekendUuid), omschrijving = "Toegekend")
            every { ztcClientService.readResultaattype(resultaattypeUri(newNietOntvankelijkUuid)) } returns
                createResultaatType(url = resultaattypeUri(newNietOntvankelijkUuid), omschrijving = "Niet ontvankelijk")
            every { ztcClientService.readResultaattype(previousToegekendUuid) } returns
                createResultaatType(url = resultaattypeUri(previousToegekendUuid), omschrijving = "Toegekend")
            every { ztcClientService.readResultaattype(previousNietOntvankelijkUuid) } returns
                createResultaatType(
                    url = resultaattypeUri(previousNietOntvankelijkUuid),
                    omschrijving = "Niet ontvankelijk"
                )

            val zaaktypeCmmnConfiguration = createZaaktypeCmmnConfiguration(
                nietOntvankelijkResultaattype = previousNietOntvankelijkUuid
            )
            zaaktypeCmmnConfiguration.setZaakbeeindigParameters(
                listOf(
                    completionParameter(
                        zaaktypeCmmnConfiguration,
                        zaakbeeindigReden(id = 2L, naam = "Zaak is toegekend"),
                        previousToegekendUuid
                    )
                )
            )

            `when`("the zaakbeeindig gegevens are updated in place") {
                zaaktypeHelperService.updateZaakbeeindigGegevens(zaaktypeCmmnConfiguration, newZaaktype)

                then("the stored resultaattypen are updated instead of being silently discarded") {
                    val zaakbeeindigParameters = zaaktypeCmmnConfiguration.getZaakbeeindigParameters()
                    zaakbeeindigParameters shouldHaveSize 1
                    zaakbeeindigParameters.first().resultaattype shouldBe newToegekendUuid
                    zaaktypeCmmnConfiguration.nietOntvankelijkResultaattype shouldBe newNietOntvankelijkUuid
                }
            }
        }
    }
})
