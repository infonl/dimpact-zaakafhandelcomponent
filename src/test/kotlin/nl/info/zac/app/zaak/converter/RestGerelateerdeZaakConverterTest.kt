/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.app.zaak.converter

import io.kotest.core.spec.IsolationMode
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.checkUnnecessaryStub
import io.mockk.every
import io.mockk.mockk
import nl.info.client.zgw.model.createZaak
import nl.info.client.zgw.zrc.ZrcClientService
import nl.info.client.zgw.zrc.model.generated.ArchiefnominatieEnum
import nl.info.client.zgw.zrc.model.generated.GerelateerdeZaak
import nl.info.client.zgw.ztc.ZtcClientService
import nl.info.client.zgw.ztc.model.createZaakType
import nl.info.zac.app.zaak.model.RelatieType
import nl.info.zac.authentication.createLoggedInUser
import nl.info.zac.policy.PolicyService
import nl.info.zac.policy.output.createZaakRechten
import java.net.URI
import java.util.UUID

class RestGerelateerdeZaakConverterTest : BehaviorSpec({
    isolationMode = IsolationMode.InstancePerTest

    val zrcClientService = mockk<ZrcClientService>()
    val ztcClientService = mockk<ZtcClientService>()
    val policyService = mockk<PolicyService>()
    val converter = RestGerelateerdeZaakConverter(
        zrcClientService = zrcClientService,
        ztcClientService = ztcClientService,
        policyService = policyService
    )

    afterEach {
        checkUnnecessaryStub()
    }

    Context("Converting a GerelateerdeZaak to RestGerelateerdeZaak") {
        Given("A GerelateerdeZaak with a URL") {
            val fakeZaakUuid = UUID.randomUUID()
            val gerelateerdeZaak = GerelateerdeZaak().apply {
                url = URI("https://example.com/zaak/$fakeZaakUuid")
            }
            val fromZaak = createZaak()
            val zaak = createZaak(uuid = fakeZaakUuid)
            val zaakType = createZaakType()
            val loggedInUser = createLoggedInUser()
            val fromZaakRechten = createZaakRechten()
            every { zrcClientService.readZaak(gerelateerdeZaak.url) } returns zaak
            every { ztcClientService.readZaaktype(zaak.zaaktype) } returns zaakType
            every { policyService.readZaakRechten(zaak, zaakType, loggedInUser) } returns createZaakRechten()

            When("convert is called with the GerelateerdeZaak and loggedInUser") {
                val result = converter.convert(fromZaak, fromZaakRechten, gerelateerdeZaak, loggedInUser)

                Then("the result has relatieType GERELATEERD") {
                    result.relatieType shouldBe RelatieType.GERELATEERD
                }

                Then("the result has the correct identificatie") {
                    result.identificatie shouldBe zaak.identificatie
                }
            }
        }
    }

    Context("Determining ontkoppelen permission for a gerelateerde zaak") {
        Given("relatieType GERELATEERD with koppelen on source and lezen on linked zaak") {
            val fromZaak = createZaak()
            val gerelateerdeZaak = createZaak()
            val zaakType = createZaakType()
            val loggedInUser = createLoggedInUser()
            val fromZaakRechten = createZaakRechten(koppelen = true)
            every { ztcClientService.readZaaktype(gerelateerdeZaak.zaaktype) } returns zaakType
            every {
                policyService.readZaakRechten(gerelateerdeZaak, zaakType, loggedInUser)
            } returns createZaakRechten(lezen = true)

            When("convert is called") {
                val result = converter.convert(
                    fromZaak, fromZaakRechten, gerelateerdeZaak, loggedInUser, RelatieType.GERELATEERD
                )

                Then("ontkoppelen is true") {
                    result.ontkoppelen shouldBe true
                }
            }
        }

        Given("relatieType GERELATEERD without koppelen on source zaak") {
            val fromZaak = createZaak()
            val gerelateerdeZaak = createZaak()
            val zaakType = createZaakType()
            val loggedInUser = createLoggedInUser()
            val fromZaakRechten = createZaakRechten(koppelen = false)
            every { ztcClientService.readZaaktype(gerelateerdeZaak.zaaktype) } returns zaakType
            every {
                policyService.readZaakRechten(gerelateerdeZaak, zaakType, loggedInUser)
            } returns createZaakRechten(lezen = true)

            When("convert is called") {
                val result = converter.convert(
                    fromZaak, fromZaakRechten, gerelateerdeZaak, loggedInUser, RelatieType.GERELATEERD
                )

                Then("ontkoppelen is false") {
                    result.ontkoppelen shouldBe false
                }
            }
        }

        Given("relatieType GERELATEERD without lezen on linked zaak") {
            val fromZaak = createZaak()
            val gerelateerdeZaak = createZaak()
            val zaakType = createZaakType()
            val loggedInUser = createLoggedInUser()
            val fromZaakRechten = createZaakRechten(koppelen = true)
            every { ztcClientService.readZaaktype(gerelateerdeZaak.zaaktype) } returns zaakType
            every {
                policyService.readZaakRechten(gerelateerdeZaak, zaakType, loggedInUser)
            } returns createZaakRechten(lezen = false)

            When("convert is called") {
                val result = converter.convert(
                    fromZaak, fromZaakRechten, gerelateerdeZaak, loggedInUser, RelatieType.GERELATEERD
                )

                Then("ontkoppelen is false") {
                    result.ontkoppelen shouldBe false
                }
            }
        }

        Given("relatieType HOOFDZAAK with koppelen on both zaken and both open") {
            val fromZaak = createZaak()
            val gerelateerdeZaak = createZaak()
            val zaakType = createZaakType()
            val loggedInUser = createLoggedInUser()
            val fromZaakRechten = createZaakRechten(koppelen = true)
            every { ztcClientService.readZaaktype(gerelateerdeZaak.zaaktype) } returns zaakType
            every {
                policyService.readZaakRechten(gerelateerdeZaak, zaakType, loggedInUser)
            } returns createZaakRechten(koppelen = true)

            When("convert is called") {
                val result = converter.convert(
                    fromZaak, fromZaakRechten, gerelateerdeZaak, loggedInUser, RelatieType.HOOFDZAAK
                )

                Then("ontkoppelen is true") {
                    result.ontkoppelen shouldBe true
                }
            }
        }

        Given("relatieType HOOFDZAAK without koppelen on source zaak") {
            val fromZaak = createZaak()
            val gerelateerdeZaak = createZaak()
            val zaakType = createZaakType()
            val loggedInUser = createLoggedInUser()
            val fromZaakRechten = createZaakRechten(koppelen = false)
            every { ztcClientService.readZaaktype(gerelateerdeZaak.zaaktype) } returns zaakType
            every {
                policyService.readZaakRechten(gerelateerdeZaak, zaakType, loggedInUser)
            } returns createZaakRechten(koppelen = true)

            When("convert is called") {
                val result = converter.convert(
                    fromZaak, fromZaakRechten, gerelateerdeZaak, loggedInUser, RelatieType.HOOFDZAAK
                )

                Then("ontkoppelen is false") {
                    result.ontkoppelen shouldBe false
                }
            }
        }

        Given("relatieType HOOFDZAAK without koppelen on linked zaak") {
            val fromZaak = createZaak()
            val gerelateerdeZaak = createZaak()
            val zaakType = createZaakType()
            val loggedInUser = createLoggedInUser()
            val fromZaakRechten = createZaakRechten(koppelen = true)
            every { ztcClientService.readZaaktype(gerelateerdeZaak.zaaktype) } returns zaakType
            every {
                policyService.readZaakRechten(gerelateerdeZaak, zaakType, loggedInUser)
            } returns createZaakRechten(koppelen = false)

            When("convert is called") {
                val result = converter.convert(
                    fromZaak, fromZaakRechten, gerelateerdeZaak, loggedInUser, RelatieType.HOOFDZAAK
                )

                Then("ontkoppelen is false") {
                    result.ontkoppelen shouldBe false
                }
            }
        }

        Given("relatieType HOOFDZAAK with source zaak open and linked zaak closed") {
            val fromZaak = createZaak()
            val gerelateerdeZaak = createZaak(archiefnominatie = ArchiefnominatieEnum.BLIJVEND_BEWAREN)
            val zaakType = createZaakType()
            val loggedInUser = createLoggedInUser()
            val fromZaakRechten = createZaakRechten(koppelen = true)
            every { ztcClientService.readZaaktype(gerelateerdeZaak.zaaktype) } returns zaakType
            every {
                policyService.readZaakRechten(gerelateerdeZaak, zaakType, loggedInUser)
            } returns createZaakRechten(koppelen = true)

            When("convert is called") {
                val result = converter.convert(
                    fromZaak, fromZaakRechten, gerelateerdeZaak, loggedInUser, RelatieType.HOOFDZAAK
                )

                Then("ontkoppelen is false") {
                    result.ontkoppelen shouldBe false
                }
            }
        }

        Given("relatieType HOOFDZAAK with both zaken closed") {
            val fromZaak = createZaak(archiefnominatie = ArchiefnominatieEnum.BLIJVEND_BEWAREN)
            val gerelateerdeZaak = createZaak(archiefnominatie = ArchiefnominatieEnum.BLIJVEND_BEWAREN)
            val zaakType = createZaakType()
            val loggedInUser = createLoggedInUser()
            val fromZaakRechten = createZaakRechten(koppelen = true)
            every { ztcClientService.readZaaktype(gerelateerdeZaak.zaaktype) } returns zaakType
            every {
                policyService.readZaakRechten(gerelateerdeZaak, zaakType, loggedInUser)
            } returns createZaakRechten(koppelen = true)

            When("convert is called") {
                val result = converter.convert(
                    fromZaak, fromZaakRechten, gerelateerdeZaak, loggedInUser, RelatieType.HOOFDZAAK
                )

                Then("ontkoppelen is true") {
                    result.ontkoppelen shouldBe true
                }
            }
        }

        Given("relatieType DEELZAAK with koppelen on both zaken and both open") {
            val fromZaak = createZaak()
            val gerelateerdeZaak = createZaak()
            val zaakType = createZaakType()
            val loggedInUser = createLoggedInUser()
            val fromZaakRechten = createZaakRechten(koppelen = true)
            every { ztcClientService.readZaaktype(gerelateerdeZaak.zaaktype) } returns zaakType
            every {
                policyService.readZaakRechten(gerelateerdeZaak, zaakType, loggedInUser)
            } returns createZaakRechten(koppelen = true)

            When("convert is called") {
                val result = converter.convert(
                    fromZaak, fromZaakRechten, gerelateerdeZaak, loggedInUser, RelatieType.DEELZAAK
                )

                Then("ontkoppelen is true") {
                    result.ontkoppelen shouldBe true
                }
            }
        }
    }
})
