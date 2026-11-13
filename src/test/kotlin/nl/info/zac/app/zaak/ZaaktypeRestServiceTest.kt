/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.app.zaak

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.mockk.checkUnnecessaryStub
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import jakarta.enterprise.inject.Instance
import jakarta.ws.rs.NotFoundException
import nl.info.client.zgw.util.extractUuid
import nl.info.client.zgw.zrc.model.generated.Zaak
import nl.info.client.zgw.ztc.ZtcClientService
import nl.info.client.zgw.ztc.model.createZaakType
import nl.info.client.zgw.ztc.model.generated.VertrouwelijkheidaanduidingEnum
import nl.info.zac.admin.ZaaktypeConfigurationService
import nl.info.zac.admin.model.createBetrokkeneKoppelingen
import nl.info.zac.admin.model.createZaaktypeBpmnConfiguration
import nl.info.zac.admin.model.createZaaktypeCmmnConfiguration
import nl.info.zac.app.shared.RestVertrouwelijkheidaanduiding
import nl.info.zac.app.zaak.model.RestZaaktypeAanmaakInstellingen
import nl.info.zac.app.zaak.model.RestZaaktypeKeuze
import nl.info.zac.authentication.LoggedInUser
import nl.info.zac.authentication.createLoggedInUser
import nl.info.zac.configuration.ConfigurationService
import nl.info.zac.flowable.ZaakProcessService
import nl.info.zac.healthcheck.createZaaktypeInrichtingscheck
import nl.info.zac.policy.PolicyService
import nl.info.zac.policy.exception.PolicyException
import java.net.URI
import java.time.LocalDate
import java.util.UUID

class ZaaktypeRestServiceTest : BehaviorSpec({
    val configurationService = mockk<ConfigurationService>()
    val loggedInUserInstance = mockk<Instance<LoggedInUser>>()
    val policyService = mockk<PolicyService>()
    val zaakProcessService = mockk<ZaakProcessService>()
    val zaaktypeConfigurationService = mockk<ZaaktypeConfigurationService>()
    val ztcClientService = mockk<ZtcClientService>()
    val zaaktypeRestService = ZaaktypeRestService(
        configurationService = configurationService,
        loggedInUserInstance = loggedInUserInstance,
        policyService = policyService,
        zaakProcessService = zaakProcessService,
        zaaktypeConfigurationService = zaaktypeConfigurationService,
        ztcClientService = ztcClientService
    )

    afterEach {
        checkUnnecessaryStub()
    }

    context("Listing the zaaktypen that the user can start") {
        given(
            """
        Two existing CMMN zaaktypes in the configured catalogue for which the logged in user is authorised
        and which are valid on the current date
        """
        ) {
            val defaultCatalogueURI = URI("https://example.com/fakeCatalogue")
            val now = LocalDate.now()
            val zaaktypes = listOf(
                createZaakType(
                    omschrijving = "Zaaktype 1",
                    identification = "ZAAKTYPE1",
                    beginGeldigheid = now.minusDays(1)
                ),
                createZaakType(
                    omschrijving = "Zaaktype 2",
                    identification = "ZAAKTYPE2",
                    beginGeldigheid = now.minusDays(2),
                    eindeGeldigheid = now.plusDays(1)
                )
            )
            val restZaaktypeKeuzes = zaaktypes.map {
                RestZaaktypeKeuze(uuid = it.url.extractUuid(), omschrijving = it.omschrijving)
            }
            val zaaktypeInrichtingscheck = createZaaktypeInrichtingscheck()
            clearMocks(policyService)
            every { loggedInUserInstance.get() } returns createLoggedInUser(
                applicationRolesPerZaaktype = mapOf("Zaaktype 1" to setOf("fakeStartrol"), "Zaaktype 2" to setOf("fakeStartrol"))
            )
            every { policyService.readStartrollen() } returns setOf("fakeStartrol")
            every { ztcClientService.listZaaktypen(defaultCatalogueURI) } returns zaaktypes
            zaaktypes.forEach {
                every { zaakProcessService.isZaaktypeReady(any(), it.url) } returns zaaktypeInrichtingscheck.isValide
                every {
                    zaaktypeConfigurationService.findConfiguration(it.url.extractUuid())
                } returns createZaaktypeCmmnConfiguration(groupId = "fakeGroupId")
            }
            every { configurationService.readDefaultCatalogusURI() } returns defaultCatalogueURI

            `when`("the zaaktypes are requested") {
                val returnedRestZaaktypes = zaaktypeRestService.listZaaktypen(startbaar = true)

                then("both zaaktypes are returned and the start roles are read from OPA once for all zaaktypes") {
                    verify(exactly = 1) {
                        ztcClientService.listZaaktypen(defaultCatalogueURI)
                        policyService.readStartrollen()
                    }
                    returnedRestZaaktypes shouldBe restZaaktypeKeuzes
                }
            }
        }

        given("Two CMMN and one BPMN zaaktypes valid on the current date") {
            val defaultCatalogueURI = URI("https://example.com/fakeCatalogue")
            val now = LocalDate.now()
            val zaakType1UUID = UUID.randomUUID()
            val zaakType2UUID = UUID.randomUUID()
            val zaakType3UUID = UUID.randomUUID()
            val zaaktypes = listOf(
                createZaakType(
                    omschrijving = "Zaaktype 1",
                    identification = "ZAAKTYPE1",
                    uri = URI("https://example.com/zaaktypes/$zaakType1UUID"),
                    beginGeldigheid = now.minusDays(1)
                ),
                createZaakType(
                    omschrijving = "Zaaktype 2",
                    identification = "ZAAKTYPE2",
                    uri = URI("https://example.com/zaaktypes/$zaakType2UUID"),
                    beginGeldigheid = now.minusDays(1)
                ),
                createZaakType(
                    omschrijving = "Zaaktype 3",
                    identification = "ZAAKTYPE3",
                    uri = URI("https://example.com/zaaktypes/$zaakType3UUID"),
                    beginGeldigheid = now.minusDays(1)
                ),
            )
            val restZaaktypeKeuze1 = RestZaaktypeKeuze(uuid = zaakType1UUID, omschrijving = "Zaaktype 1")
            val restZaaktypeKeuze2 = RestZaaktypeKeuze(uuid = zaakType2UUID, omschrijving = "Zaaktype 2")
            val restZaaktypeKeuze3 = RestZaaktypeKeuze(uuid = zaakType3UUID, omschrijving = "Zaaktype 3")
            val restZaaktypeKeuzes = listOf(restZaaktypeKeuze1, restZaaktypeKeuze2, restZaaktypeKeuze3)
            val zaaktypeInrichtingscheck = createZaaktypeInrichtingscheck()
            zaaktypes.slice(0..1).forEach {
                every { zaakProcessService.isZaaktypeReady(any(), it.url) } returns zaaktypeInrichtingscheck.isValide
                every {
                    zaaktypeConfigurationService.findConfiguration(it.url.extractUuid())
                } returns createZaaktypeCmmnConfiguration(groupId = "fakeGroupId")
            }
            zaaktypes.last().let {
                every { zaakProcessService.isZaaktypeReady(any(), it.url) } returns true
                every {
                    zaaktypeConfigurationService.findConfiguration(it.url.extractUuid())
                } returns createZaaktypeBpmnConfiguration(groupId = "fakeGroupId")
            }

            every { policyService.readStartrollen() } returns setOf("fakeStartrol")

            and("the user has a start role for all zaaktypes") {
                every { loggedInUserInstance.get() } returns createLoggedInUser(
                    applicationRolesPerZaaktype = zaaktypes.associate { it.omschrijving to setOf("fakeStartrol") }
                )

                every { configurationService.readDefaultCatalogusURI() } returns defaultCatalogueURI
                every { ztcClientService.listZaaktypen(defaultCatalogueURI) } returns zaaktypes

                `when`("the zaaktypes are listed") {
                    val returnedRestZaaktypes = zaaktypeRestService.listZaaktypen(startbaar = true)

                    then("all zaaktypes are returned") {
                        verify(exactly = 1) {
                            ztcClientService.listZaaktypen(defaultCatalogueURI)
                        }
                        verify(exactly = 3) {
                            zaaktypeConfigurationService.findConfiguration(any<UUID>())
                        }
                        returnedRestZaaktypes shouldBe restZaaktypeKeuzes
                    }
                }
            }
            and("the user has no start role for one of the CMMN zaaktypes") {
                clearMocks(ztcClientService, zaaktypeConfigurationService, answers = false)
                every { loggedInUserInstance.get() } returns createLoggedInUser(
                    applicationRolesPerZaaktype = mapOf(
                        zaaktypes[0].omschrijving to setOf("fakeStartrol"),
                        zaaktypes[1].omschrijving to setOf("fakeOtherRole"),
                        zaaktypes[2].omschrijving to setOf("fakeStartrol")
                    )
                )

                `when`("the zaaktypes are listed") {
                    val returnedRestZaaktypes = zaaktypeRestService.listZaaktypen(startbaar = true)

                    then("only the zaaktypes for which the user is authorised are returned") {
                        verify(exactly = 1) {
                            ztcClientService.listZaaktypen(defaultCatalogueURI)
                        }
                        verify(exactly = 2) {
                            zaaktypeConfigurationService.findConfiguration(any<UUID>())
                        }
                        returnedRestZaaktypes shouldBe listOf(restZaaktypeKeuze1, restZaaktypeKeuze3)
                    }
                }
            }
            and("the user has a start role and a role that is not a start role for every zaaktype") {
                clearMocks(ztcClientService, zaaktypeConfigurationService, answers = false)
                every { loggedInUserInstance.get() } returns createLoggedInUser(
                    applicationRolesPerZaaktype = zaaktypes.associate {
                        it.omschrijving to setOf("fakeStartrol", "fakeOtherRole")
                    }
                )

                `when`("the zaaktypes are listed") {
                    val returnedRestZaaktypes = zaaktypeRestService.listZaaktypen(startbaar = true)

                    then("all zaaktypes are returned, because one start role is enough to start a zaak") {
                        returnedRestZaaktypes shouldBe restZaaktypeKeuzes
                    }
                }
            }
            and("the user has no roles at all") {
                clearMocks(ztcClientService, zaaktypeConfigurationService, answers = false)
                every { loggedInUserInstance.get() } returns createLoggedInUser(
                    applicationRolesPerZaaktype = emptyMap(),
                    overallRoles = emptySet()
                )

                `when`("the zaaktypes are listed") {
                    val returnedRestZaaktypes = zaaktypeRestService.listZaaktypen(startbaar = true)

                    then("no zaaktypes are returned and the ZAC configuration of the zaaktypes is not read") {
                        returnedRestZaaktypes shouldBe emptyList()
                        verify(exactly = 0) {
                            zaaktypeConfigurationService.findConfiguration(any<UUID>())
                        }
                    }
                }
            }
        }

        given("A concept zaaktype, a zaaktype not yet valid and an expired zaaktype") {
            val defaultCatalogueURI = URI("https://example.com/fakeCatalogueWithInvalidZaaktypes")
            val now = LocalDate.now()
            val zaaktypes = listOf(
                createZaakType(omschrijving = "fakeConceptZaaktype", concept = true, beginGeldigheid = now.minusDays(1)),
                createZaakType(omschrijving = "fakeFutureZaaktype", beginGeldigheid = now.plusDays(1)),
                createZaakType(
                    omschrijving = "fakeExpiredZaaktype",
                    beginGeldigheid = now.minusDays(10),
                    eindeGeldigheid = now.minusDays(1)
                )
            )
            clearMocks(policyService)
            every { loggedInUserInstance.get() } returns createLoggedInUser()
            every { configurationService.readDefaultCatalogusURI() } returns defaultCatalogueURI
            every { ztcClientService.listZaaktypen(defaultCatalogueURI) } returns zaaktypes

            `when`("the zaaktypes are listed") {
                val returnedRestZaaktypes = zaaktypeRestService.listZaaktypen(startbaar = true)

                then("no zaaktypes are returned and the start roles are not read from OPA") {
                    returnedRestZaaktypes shouldBe emptyList()
                    verify(exactly = 0) { policyService.readStartrollen() }
                }
            }
        }

        given("Two valid versions of a CMMN zaaktype with the same omschrijving") {
            val defaultCatalogueURI = URI("https://example.com/fakeCatalogueWithTwoVersions")
            val now = LocalDate.now()
            val zaaktypes = listOf(
                createZaakType(omschrijving = "fakeVersionedZaaktype", beginGeldigheid = now.minusDays(2)),
                createZaakType(omschrijving = "fakeVersionedZaaktype", beginGeldigheid = now.minusDays(1))
            )
            val restZaaktypeKeuzes = zaaktypes.map {
                RestZaaktypeKeuze(uuid = it.url.extractUuid(), omschrijving = "fakeVersionedZaaktype")
            }
            clearMocks(policyService)
            every { loggedInUserInstance.get() } returns createLoggedInUser(
                applicationRolesPerZaaktype = mapOf("fakeVersionedZaaktype" to setOf("fakeStartrol"))
            )
            every { policyService.readStartrollen() } returns setOf("fakeStartrol")
            every { configurationService.readDefaultCatalogusURI() } returns defaultCatalogueURI
            every { ztcClientService.listZaaktypen(defaultCatalogueURI) } returns zaaktypes
            zaaktypes.forEachIndexed { index, zaaktype ->
                every {
                    zaaktypeConfigurationService.findConfiguration(zaaktype.url.extractUuid())
                } returns createZaaktypeCmmnConfiguration(groupId = "fakeGroupId")
                every { zaakProcessService.isZaaktypeReady(any(), zaaktype.url) } returns createZaaktypeInrichtingscheck().isValide
            }

            `when`("the zaaktypes are listed") {
                val returnedRestZaaktypes = zaaktypeRestService.listZaaktypen(startbaar = true)

                then("both versions are returned and the start roles are read from OPA once") {
                    returnedRestZaaktypes shouldBe restZaaktypeKeuzes
                    verify(exactly = 1) { policyService.readStartrollen() }
                }
            }
        }

        given("A zaaktype without ZAC configuration and a zaaktype with an invalid CMMN configuration") {
            val defaultCatalogueURI = URI("https://example.com/fakeCatalogueWithUnconfiguredZaaktypes")
            val now = LocalDate.now()
            val unconfiguredZaaktype = createZaakType(
                omschrijving = "fakeUnconfiguredZaaktype",
                beginGeldigheid = now.minusDays(1)
            )
            val invalidlyConfiguredZaaktype = createZaakType(
                omschrijving = "fakeInvalidlyConfiguredZaaktype",
                beginGeldigheid = now.minusDays(1)
            )
            every { configurationService.readDefaultCatalogusURI() } returns defaultCatalogueURI
            every {
                ztcClientService.listZaaktypen(defaultCatalogueURI)
            } returns listOf(unconfiguredZaaktype, invalidlyConfiguredZaaktype)
            every { loggedInUserInstance.get() } returns createLoggedInUser(
                applicationRolesPerZaaktype = mapOf(
                    unconfiguredZaaktype.omschrijving to setOf("fakeStartrol"),
                    invalidlyConfiguredZaaktype.omschrijving to setOf("fakeStartrol")
                )
            )
            every { policyService.readStartrollen() } returns setOf("fakeStartrol")
            every {
                zaaktypeConfigurationService.findConfiguration(unconfiguredZaaktype.url.extractUuid())
            } returns null
            every {
                zaaktypeConfigurationService.findConfiguration(invalidlyConfiguredZaaktype.url.extractUuid())
            } returns createZaaktypeCmmnConfiguration(groupId = null)

            `when`("the zaaktypes are listed") {
                val returnedRestZaaktypes = zaaktypeRestService.listZaaktypen(startbaar = true)

                then("no zaaktypes are returned and the inrichtingscheck is not run for them") {
                    returnedRestZaaktypes shouldBe emptyList()
                    verify(exactly = 0) {
                        zaakProcessService.isZaaktypeReady(any(), unconfiguredZaaktype.url)
                        zaakProcessService.isZaaktypeReady(any(), invalidlyConfiguredZaaktype.url)
                    }
                }
            }
        }

        given("A valid CMMN zaaktype and a user without roles for it, but with an overall start role") {
            val defaultCatalogueURI = URI("https://example.com/fakeCatalogueForOverallRoles")
            val zaaktype = createZaakType(omschrijving = "fakeZaaktypeWithoutRoles", beginGeldigheid = LocalDate.now().minusDays(1))
            every { loggedInUserInstance.get() } returns createLoggedInUser(overallRoles = setOf("fakeStartrol"))
            every { policyService.readStartrollen() } returns setOf("fakeStartrol")
            every { configurationService.readDefaultCatalogusURI() } returns defaultCatalogueURI
            every { ztcClientService.listZaaktypen(defaultCatalogueURI) } returns listOf(zaaktype)
            every {
                zaaktypeConfigurationService.findConfiguration(zaaktype.url.extractUuid())
            } returns createZaaktypeCmmnConfiguration(groupId = "fakeGroupId")
            every { zaakProcessService.isZaaktypeReady(any(), zaaktype.url) } returns true

            `when`("the zaaktypes are listed") {
                val returnedRestZaaktypes = zaaktypeRestService.listZaaktypen(startbaar = true)

                then("the zaaktype is returned, because the overall roles count for every zaaktype") {
                    returnedRestZaaktypes shouldBe listOf(
                        RestZaaktypeKeuze(uuid = zaaktype.url.extractUuid(), omschrijving = "fakeZaaktypeWithoutRoles")
                    )
                }
            }
        }

        given("A valid CMMN zaaktype and a user with only a non-start role for it") {
            val defaultCatalogueURI = URI("https://example.com/fakeCatalogueForNonStartRoles")
            val zaaktype = createZaakType(omschrijving = "fakeZaaktypeWithReadRole", beginGeldigheid = LocalDate.now().minusDays(1))
            every { loggedInUserInstance.get() } returns createLoggedInUser(
                applicationRolesPerZaaktype = mapOf(zaaktype.omschrijving to setOf("fakeReadRole"))
            )
            every { policyService.readStartrollen() } returns setOf("fakeStartrol")
            every { configurationService.readDefaultCatalogusURI() } returns defaultCatalogueURI
            every { ztcClientService.listZaaktypen(defaultCatalogueURI) } returns listOf(zaaktype)

            `when`("the zaaktypes are listed") {
                val returnedRestZaaktypes = zaaktypeRestService.listZaaktypen(startbaar = true)

                then("no zaaktypes are returned and the ZAC configuration is not read") {
                    returnedRestZaaktypes shouldBe emptyList()
                    verify(exactly = 0) { zaaktypeConfigurationService.findConfiguration(zaaktype.url.extractUuid()) }
                }
            }
        }
    }

    context("Reading the settings of a zaaktype for zaak creation") {
        given(
            """
            A valid CMMN zaaktype that the user may start, with vertrouwelijkheidaanduiding openbaar, a default groep,
            a default behandelaar, the KVK koppeling switched on and the BRP koppeling switched off
            """
        ) {
            val defaultCatalogueURI = URI("https://example.com/fakeCatalogueForCreationSettings")
            val zaaktype = createZaakType(
                omschrijving = "fakeOfferedZaaktype",
                beginGeldigheid = LocalDate.now().minusDays(1),
                vertrouwelijkheidaanduiding = VertrouwelijkheidaanduidingEnum.OPENBAAR
            )
            val zaaktypeUuid = zaaktype.url.extractUuid()
            val zaaktypeCmmnConfiguration = createZaaktypeCmmnConfiguration(
                zaaktypeUUID = zaaktypeUuid,
                groupId = "fakeDefaultGroepId",
                defaultBehandelaarId = "fakeDefaultBehandelaarId",
                zaaktypeBetrokkeneParameters = createBetrokkeneKoppelingen(kvkKoppelen = true, brpKoppelen = false)
            )
            every { loggedInUserInstance.get() } returns createLoggedInUser(
                applicationRolesPerZaaktype = mapOf(zaaktype.omschrijving to setOf("fakeStartrol"))
            )
            every { policyService.readStartrollen() } returns setOf("fakeStartrol")
            every { configurationService.readDefaultCatalogusURI() } returns defaultCatalogueURI
            every { ztcClientService.listZaaktypen(defaultCatalogueURI) } returns listOf(zaaktype)
            every { zaaktypeConfigurationService.findConfiguration(zaaktypeUuid) } returns zaaktypeCmmnConfiguration
            every { zaaktypeConfigurationService.readConfiguration(zaaktypeUuid) } returns zaaktypeCmmnConfiguration
            every { zaakProcessService.isZaaktypeReady(zaaktypeCmmnConfiguration, zaaktype.url) } returns true

            `when`("the settings of the zaaktype are read") {
                val restZaaktypeAanmaakInstellingen = zaaktypeRestService.readZaaktype(zaaktypeUuid)

                then("the settings that the Zaak aanmaken form applies are returned") {
                    restZaaktypeAanmaakInstellingen shouldBe RestZaaktypeAanmaakInstellingen(
                        uuid = zaaktypeUuid,
                        omschrijving = "fakeOfferedZaaktype",
                        vertrouwelijkheidaanduiding = RestVertrouwelijkheidaanduiding.OPENBAAR,
                        defaultGroepId = "fakeDefaultGroepId",
                        defaultBehandelaarId = "fakeDefaultBehandelaarId",
                        isKvkKoppelenEnabled = true,
                        isBrpKoppelenEnabled = false
                    )
                }
            }
        }

        given("A valid CMMN zaaktype that the user may not start") {
            val defaultCatalogueURI = URI("https://example.com/fakeCatalogueForForbiddenCreationSettings")
            val zaaktype = createZaakType(
                omschrijving = "fakeForbiddenZaaktype",
                beginGeldigheid = LocalDate.now().minusDays(1)
            )
            every { loggedInUserInstance.get() } returns createLoggedInUser(
                applicationRolesPerZaaktype = mapOf(zaaktype.omschrijving to setOf("fakeReadRole"))
            )
            every { policyService.readStartrollen() } returns setOf("fakeStartrol")
            every { configurationService.readDefaultCatalogusURI() } returns defaultCatalogueURI
            every { ztcClientService.listZaaktypen(defaultCatalogueURI) } returns listOf(zaaktype)

            `when`("the settings of the zaaktype are read") {
                val policyException = shouldThrow<PolicyException> {
                    zaaktypeRestService.readZaaktype(zaaktype.url.extractUuid())
                }

                then("the request is refused without reading the configuration of the zaaktype") {
                    policyException shouldNotBe null
                    verify(exactly = 0) {
                        zaaktypeConfigurationService.findConfiguration(zaaktype.url.extractUuid())
                        zaaktypeConfigurationService.readConfiguration(zaaktype.url.extractUuid())
                    }
                }
            }
        }

        given("A zaaktype that the user may not start and that is not valid today") {
            val defaultCatalogueURI = URI("https://example.com/fakeCatalogueForForbiddenExpiredZaaktype")
            val zaaktype = createZaakType(
                omschrijving = "fakeForbiddenExpiredZaaktype",
                beginGeldigheid = LocalDate.now().minusDays(10),
                eindeGeldigheid = LocalDate.now().minusDays(1)
            )
            every { loggedInUserInstance.get() } returns createLoggedInUser(
                applicationRolesPerZaaktype = mapOf(zaaktype.omschrijving to setOf("fakeReadRole"))
            )
            every { policyService.readStartrollen() } returns setOf("fakeStartrol")
            every { configurationService.readDefaultCatalogusURI() } returns defaultCatalogueURI
            every { ztcClientService.listZaaktypen(defaultCatalogueURI) } returns listOf(zaaktype)

            `when`("the settings of the zaaktype are read") {
                val policyException = shouldThrow<PolicyException> {
                    zaaktypeRestService.readZaaktype(zaaktype.url.extractUuid())
                }

                then("the request is refused, because the start right is checked first") {
                    policyException shouldNotBe null
                }
            }
        }

        given("A zaaktype that the user may start, but whose configuration has no groep") {
            val defaultCatalogueURI = URI("https://example.com/fakeCatalogueForInvalidCreationSettings")
            val zaaktype = createZaakType(
                omschrijving = "fakeInvalidlyConfiguredZaaktype",
                beginGeldigheid = LocalDate.now().minusDays(1)
            )
            every { loggedInUserInstance.get() } returns createLoggedInUser(
                applicationRolesPerZaaktype = mapOf(zaaktype.omschrijving to setOf("fakeStartrol"))
            )
            every { policyService.readStartrollen() } returns setOf("fakeStartrol")
            every { configurationService.readDefaultCatalogusURI() } returns defaultCatalogueURI
            every { ztcClientService.listZaaktypen(defaultCatalogueURI) } returns listOf(zaaktype)
            every {
                zaaktypeConfigurationService.findConfiguration(zaaktype.url.extractUuid())
            } returns createZaaktypeCmmnConfiguration(groupId = null)

            `when`("the settings of the zaaktype are read") {
                val notFoundException = shouldThrow<NotFoundException> {
                    zaaktypeRestService.readZaaktype(zaaktype.url.extractUuid())
                }

                then("the zaaktype is not found, because it is not offered for zaak creation") {
                    notFoundException.message shouldBe
                        "Zaaktype '${zaaktype.url.extractUuid()}' is not offered for zaak creation"
                }
            }
        }

        given("A zaaktype UUID that is not in the default catalogus") {
            val defaultCatalogueURI = URI("https://example.com/fakeCatalogueWithoutTheZaaktype")
            val unknownZaaktypeUuid = UUID.randomUUID()
            every { configurationService.readDefaultCatalogusURI() } returns defaultCatalogueURI
            every { ztcClientService.listZaaktypen(defaultCatalogueURI) } returns listOf(createZaakType())

            `when`("the settings of the zaaktype are read") {
                val notFoundException = shouldThrow<NotFoundException> {
                    zaaktypeRestService.readZaaktype(unknownZaaktypeUuid)
                }

                then("the zaaktype is not found") {
                    notFoundException.message shouldBe
                        "Zaaktype '$unknownZaaktypeUuid' is not offered for zaak creation"
                }
            }
        }
    }

    context("Listing the zaaktypen without the startbaar filter") {
        given("A zaaktype the user may start, a concept zaaktype and a zaaktype the user may not start") {
            val defaultCatalogueURI = URI("https://example.com/fakeCatalogueForTheUnfilteredList")
            val zaaktypes = listOf(
                createZaakType(omschrijving = "fakeStartableZaaktype"),
                createZaakType(omschrijving = "fakeConceptZaaktype", concept = true),
                createZaakType(omschrijving = "fakeForbiddenZaaktype")
            )
            clearMocks(policyService, loggedInUserInstance)
            every { configurationService.readDefaultCatalogusURI() } returns defaultCatalogueURI
            every { ztcClientService.listZaaktypen(defaultCatalogueURI) } returns zaaktypes

            `when`("the zaaktypen are listed without a filter") {
                val returnedRestZaaktypes = zaaktypeRestService.listZaaktypen(startbaar = null)

                then("every zaaktype of the catalogus is returned without checking the rights of the user") {
                    returnedRestZaaktypes shouldBe zaaktypes.map {
                        RestZaaktypeKeuze(uuid = it.url.extractUuid(), omschrijving = it.omschrijving)
                    }
                    verify(exactly = 0) {
                        loggedInUserInstance.get()
                        policyService.readStartrollen()
                    }
                }
            }
        }
    }

    context("Listing the zaaktypen that the user cannot start") {
        given("A zaaktype the user may start, a concept zaaktype and a zaaktype the user may not start") {
            val defaultCatalogueURI = URI("https://example.com/fakeCatalogueForTheNotStartableList")
            val startableZaaktype = createZaakType(omschrijving = "fakeStartableZaaktype")
            val conceptZaaktype = createZaakType(omschrijving = "fakeConceptZaaktype", concept = true)
            val forbiddenZaaktype = createZaakType(omschrijving = "fakeForbiddenZaaktype")
            clearMocks(policyService)
            every { loggedInUserInstance.get() } returns createLoggedInUser(
                applicationRolesPerZaaktype = mapOf("fakeStartableZaaktype" to setOf("fakeStartrol"))
            )
            every { policyService.readStartrollen() } returns setOf("fakeStartrol")
            every { configurationService.readDefaultCatalogusURI() } returns defaultCatalogueURI
            every {
                ztcClientService.listZaaktypen(defaultCatalogueURI)
            } returns listOf(startableZaaktype, conceptZaaktype, forbiddenZaaktype)
            every {
                zaaktypeConfigurationService.findConfiguration(startableZaaktype.url.extractUuid())
            } returns createZaaktypeCmmnConfiguration(groupId = "fakeGroupId")
            every { zaakProcessService.isZaaktypeReady(any(), startableZaaktype.url) } returns true

            `when`("the zaaktypen are listed with startbaar=false") {
                val returnedRestZaaktypes = zaaktypeRestService.listZaaktypen(startbaar = false)

                then("only the zaaktypen that the user cannot start are returned") {
                    returnedRestZaaktypes shouldBe listOf(conceptZaaktype, forbiddenZaaktype).map {
                        RestZaaktypeKeuze(uuid = it.url.extractUuid(), omschrijving = it.omschrijving)
                    }
                }
            }
        }
    }
})
