/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.app.admin

import io.kotest.core.spec.style.BehaviorSpec
import io.mockk.checkUnnecessaryStub
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import nl.info.client.zgw.ztc.ZtcClientService
import nl.info.client.zgw.ztc.model.createResultaatType
import nl.info.client.zgw.ztc.model.createZaakType
import nl.info.zac.admin.ZaaktypeHelperService
import nl.info.zac.admin.model.ZaakbeeindigReden
import nl.info.zac.admin.model.ZaaktypeCompletionParameters
import nl.info.zac.admin.model.createZaaktypeCmmnConfiguration
import java.net.URI
import java.util.UUID

class ZaaktypeHelperServiceTest : BehaviorSpec({
    val ztcClientService = mockk<ZtcClientService>()
    val zaaktypeHelperService = ZaaktypeHelperService(ztcClientService)

    beforeEach {
        checkUnnecessaryStub()
    }

    Context("updateZaakbeeindigGegevens") {
        Given("A zaaktype configuration with a nietOntvankelijkResultaattype") {
            val previousResultaattypeUuid = UUID.randomUUID()
            val previousResultaattypeUri = URI("http://example.com/resultaattype/$previousResultaattypeUuid")
            val newResultaattypeUuid = UUID.randomUUID()
            val newResultaattypeUri = URI("http://example.com/resultaattype/$newResultaattypeUuid")

            val previousResultaattype = createResultaatType(
                url = previousResultaattypeUri,
                omschrijving = "Test Resultaat"
            )
            val newResultaattype = createResultaatType(
                url = newResultaattypeUri,
                omschrijving = "Test Resultaat"
            )

            val zaaktypeConfiguration = createZaaktypeCmmnConfiguration(
                nietOntvankelijkResultaattype = previousResultaattypeUuid
            )

            val newZaaktype = createZaakType(
                resultTypes = listOf(newResultaattypeUri)
            )

            every { ztcClientService.readResultaattype(newResultaattypeUri) } returns newResultaattype
            every { ztcClientService.readResultaattype(previousResultaattypeUuid) } returns previousResultaattype

            When("updateZaakbeeindigGegevens is called") {
                zaaktypeHelperService.updateZaakbeeindigGegevens(zaaktypeConfiguration, newZaaktype)

                Then("the ZTC client service should be called to read the resultaattypes") {
                    verify(exactly = 1) {
                        ztcClientService.readResultaattype(newResultaattypeUri)
                        ztcClientService.readResultaattype(previousResultaattypeUuid)
                    }
                }
            }
        }

        Given("A zaaktype configuration without a nietOntvankelijkResultaattype") {
            val newResultaattypeUri = URI("http://example.com/resultaattype/${UUID.randomUUID()}")
            val newResultaattype = createResultaatType(url = newResultaattypeUri)

            val zaaktypeConfiguration = createZaaktypeCmmnConfiguration(
                zaaktypeCompletionParameters = emptySet()
            ).apply {
                nietOntvankelijkResultaattype = null
            }

            val newZaaktype = createZaakType(resultTypes = listOf(newResultaattypeUri))

            every { ztcClientService.readResultaattype(newResultaattypeUri) } returns newResultaattype

            When("updateZaakbeeindigGegevens is called") {
                zaaktypeHelperService.updateZaakbeeindigGegevens(zaaktypeConfiguration, newZaaktype)

                Then("only the new resultaattype should be read") {
                    verify(exactly = 1) {
                        ztcClientService.readResultaattype(newResultaattypeUri)
                    }
                }
            }
        }

        Given("A zaaktype configuration with zaakbeeindig parameters") {
            val previousResultaattypeUuid = UUID.randomUUID()
            val previousResultaattypeUri = URI("http://example.com/resultaattype/$previousResultaattypeUuid")
            val newResultaattypeUuid = UUID.randomUUID()
            val newResultaattypeUri = URI("http://example.com/resultaattype/$newResultaattypeUuid")

            val previousResultaattype = createResultaatType(
                url = previousResultaattypeUri,
                omschrijving = "Afgewezen"
            )
            val newResultaattype = createResultaatType(
                url = newResultaattypeUri,
                omschrijving = "Afgewezen"
            )

            val zaakbeeindigReden = ZaakbeeindigReden().apply {
                id = 1L
                naam = "Test Reden"
            }

            val zaaktypeConfiguration = createZaaktypeCmmnConfiguration(
                zaaktypeCompletionParameters = emptySet()
            ).apply {
                nietOntvankelijkResultaattype = null
            }

            val completionParameter = ZaaktypeCompletionParameters().apply {
                id = 100L
                this.zaaktypeConfiguration = zaaktypeConfiguration
            }
            completionParameter.zaakbeeindigReden = zaakbeeindigReden
            completionParameter.resultaattype = previousResultaattypeUuid

            zaaktypeConfiguration.setZaakbeeindigParameters(mutableSetOf(completionParameter))

            val newZaaktype = createZaakType(resultTypes = listOf(newResultaattypeUri))

            every { ztcClientService.readResultaattype(newResultaattypeUri) } returns newResultaattype
            every { ztcClientService.readResultaattype(previousResultaattypeUuid) } returns previousResultaattype

            When("updateZaakbeeindigGegevens is called") {
                zaaktypeHelperService.updateZaakbeeindigGegevens(zaaktypeConfiguration, newZaaktype)

                Then("the completion parameters should be processed") {
                    verify(exactly = 1) {
                        ztcClientService.readResultaattype(newResultaattypeUri)
                        ztcClientService.readResultaattype(previousResultaattypeUuid)
                    }
                }
            }
        }
    }
})
