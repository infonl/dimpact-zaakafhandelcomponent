/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.zaak

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import io.mockk.checkUnnecessaryStub
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.slot
import io.mockk.verify
import io.mockk.verifyOrder
import nl.info.client.pabc.ROLE_NAME_ZAAKSPECIFIEK_GEAUTORISEERD
import nl.info.client.zgw.model.createMedewerkerIdentificatie
import nl.info.client.zgw.model.createRolMedewerker
import nl.info.client.zgw.model.createRolOrganisatorischeEenheid
import nl.info.client.zgw.model.createZaak
import nl.info.client.zgw.model.createZaakEigenschap
import nl.info.client.zgw.shared.ZgwApiService
import nl.info.client.zgw.zrc.ZrcClientService
import nl.info.client.zgw.zrc.model.Rol
import nl.info.client.zgw.zrc.model.generated.BetrokkeneTypeEnum
import nl.info.client.zgw.zrc.model.generated.MedewerkerIdentificatie
import nl.info.client.zgw.zrc.model.generated.ZaakEigenschap
import nl.info.client.zgw.zrc.util.ZAAKEIGENSCHAP_NAAM_GEAUTORISEERD
import nl.info.client.zgw.ztc.ZtcClientService
import nl.info.client.zgw.ztc.model.createEigenschap
import nl.info.client.zgw.ztc.model.createZaakType
import nl.info.client.zgw.ztc.model.createZaakspecifiekGeautoriseerdeMedewerkerRolType
import nl.info.zac.app.zaak.exception.ZaakWithoutBehandelaarCannotBeMarkedException
import nl.info.zac.app.zaak.exception.ZaakspecifiekGeautoriseerdeMedewerkerRoltypeNotFoundException
import nl.info.zac.app.zaak.exception.ZaakspecifiekGeautoriseerdeZaakCannotBeReleasedException
import nl.info.zac.app.zaak.exception.ZaakspecifiekeAutorisatieCannotBeLiftedException
import nl.info.zac.app.zaak.exception.ZaakspecifiekeAutorisatieNotAllowedException
import nl.info.zac.app.zaak.exception.ZaaktypeNotZaakspecifiekAutoriseerbaarException
import nl.info.zac.authentication.createLoggedInUser
import nl.info.zac.exception.ErrorCode
import nl.info.zac.search.IndexingService
import nl.info.zac.zaak.model.createZaakToewijzing

@Suppress("LargeClass")
class ZaakspecifiekeAutorisatieServiceTest : BehaviorSpec({
    val zrcClientService = mockk<ZrcClientService>()
    val ztcClientService = mockk<ZtcClientService>()
    val zgwApiService = mockk<ZgwApiService>()
    val indexingService = mockk<IndexingService>()
    val zaakspecifiekeAutorisatieService = ZaakspecifiekeAutorisatieService(
        zrcClientService = zrcClientService,
        ztcClientService = ztcClientService,
        zgwApiService = zgwApiService,
        indexingService = indexingService
    )
    val geautoriseerdZaakEigenschap = createZaakEigenschap(naam = ZAAKEIGENSCHAP_NAAM_GEAUTORISEERD, waarde = "true")

    afterEach {
        checkUnnecessaryStub()
    }

    context("Deciding whether a zaaktype can be zaakspecifiek geautoriseerd at all") {
        given("a zaaktype with both the marking eigenschap and the geautoriseerde medewerker roltype") {
            val zaakType = createZaakType()
            every {
                ztcClientService.findEigenschap(zaakType.url, ZAAKEIGENSCHAP_NAAM_GEAUTORISEERD)
            } returns createEigenschap(naam = ZAAKEIGENSCHAP_NAAM_GEAUTORISEERD)
            every {
                zgwApiService.findZaakspecifiekGeautoriseerdeMedewerkerRoltype(zaakType.url)
            } returns createZaakspecifiekGeautoriseerdeMedewerkerRolType(zaakTypeUri = zaakType.url)

            `when`("the zaaktype is checked") {
                val isZaakspecifiekAutoriseerbaar =
                    zaakspecifiekeAutorisatieService.isZaakspecifiekAutoriseerbaar(zaakType)

                then("it is zaakspecifiek autoriseerbaar") {
                    isZaakspecifiekAutoriseerbaar shouldBe true
                }
            }
        }

        given("a zaaktype with the marking eigenschap but without the geautoriseerde medewerker roltype") {
            val zaakType = createZaakType()
            every {
                ztcClientService.findEigenschap(zaakType.url, ZAAKEIGENSCHAP_NAAM_GEAUTORISEERD)
            } returns createEigenschap(naam = ZAAKEIGENSCHAP_NAAM_GEAUTORISEERD)
            every { zgwApiService.findZaakspecifiekGeautoriseerdeMedewerkerRoltype(zaakType.url) } returns null

            `when`("the zaaktype is checked") {
                val isZaakspecifiekAutoriseerbaar =
                    zaakspecifiekeAutorisatieService.isZaakspecifiekAutoriseerbaar(zaakType)

                then("it is not zaakspecifiek autoriseerbaar, because a replaced behandelaar could not keep access") {
                    isZaakspecifiekAutoriseerbaar shouldBe false
                }
            }
        }

        given("a zaaktype with the geautoriseerde medewerker roltype but without the marking eigenschap") {
            val zaakType = createZaakType()
            every { ztcClientService.findEigenschap(zaakType.url, ZAAKEIGENSCHAP_NAAM_GEAUTORISEERD) } returns null

            `when`("the zaaktype is checked") {
                val isZaakspecifiekAutoriseerbaar =
                    zaakspecifiekeAutorisatieService.isZaakspecifiekAutoriseerbaar(zaakType)

                then("it is not zaakspecifiek autoriseerbaar and the roltype is not looked up") {
                    isZaakspecifiekAutoriseerbaar shouldBe false
                    verify(exactly = 0) {
                        zgwApiService.findZaakspecifiekGeautoriseerdeMedewerkerRoltype(zaakType.url)
                    }
                }
            }
        }
    }

    context("Deciding whether an update marks a zaak as zaakspecifiek geautoriseerd") {
        given("an update that does not mention the marking at all") {
            val zaakType = createZaakType()

            `when`("the decision is made for an unmarked zaak") {
                val shouldMark = zaakspecifiekeAutorisatieService.shouldMarkZaakspecifiekGeautoriseerd(
                    zaakType = zaakType,
                    requestedMarking = null,
                    isAlreadyZaakspecifiekGeautoriseerd = false,
                    currentAndRequestedBehandelaarIds = setOf("fakeBehandelaarId"),
                    loggedInUser = createLoggedInUser(id = "fakeBehandelaarId")
                )

                then("the zaak is not marked and no zaaktype lookup is needed") {
                    shouldMark shouldBe false
                    verify(exactly = 0) { ztcClientService.findEigenschap(any(), any()) }
                }
            }
        }

        given("an unmarked zaak of a zaakspecifiek autoriseerbaar zaaktype with the logged-in user as behandelaar") {
            val zaakType = createZaakType()
            every {
                ztcClientService.findEigenschap(zaakType.url, ZAAKEIGENSCHAP_NAAM_GEAUTORISEERD)
            } returns createEigenschap(naam = ZAAKEIGENSCHAP_NAAM_GEAUTORISEERD)
            every {
                zgwApiService.findZaakspecifiekGeautoriseerdeMedewerkerRoltype(zaakType.url)
            } returns createZaakspecifiekGeautoriseerdeMedewerkerRolType(zaakTypeUri = zaakType.url)

            `when`("the behandelaar asks for the marking") {
                val shouldMark = zaakspecifiekeAutorisatieService.shouldMarkZaakspecifiekGeautoriseerd(
                    zaakType = zaakType,
                    requestedMarking = true,
                    isAlreadyZaakspecifiekGeautoriseerd = false,
                    currentAndRequestedBehandelaarIds = setOf("fakeBehandelaarId"),
                    loggedInUser = createLoggedInUser(id = "fakeBehandelaarId")
                )

                then("the zaak is marked") {
                    shouldMark shouldBe true
                }
            }

            `when`("the behandelaar explicitly asks not to mark the zaak") {
                val shouldMark = zaakspecifiekeAutorisatieService.shouldMarkZaakspecifiekGeautoriseerd(
                    zaakType = zaakType,
                    requestedMarking = false,
                    isAlreadyZaakspecifiekGeautoriseerd = false,
                    currentAndRequestedBehandelaarIds = setOf("fakeBehandelaarId"),
                    loggedInUser = createLoggedInUser(id = "fakeBehandelaarId")
                )

                then("the zaak stays unmarked") {
                    shouldMark shouldBe false
                }
            }
        }

        given("an unmarked zaak of a zaaktype without the marking eigenschap") {
            val zaakType = createZaakType()
            every { ztcClientService.findEigenschap(zaakType.url, ZAAKEIGENSCHAP_NAAM_GEAUTORISEERD) } returns null

            `when`("the behandelaar asks for the marking") {
                val zaaktypeNotZaakspecifiekAutoriseerbaarException =
                    shouldThrow<ZaaktypeNotZaakspecifiekAutoriseerbaarException> {
                        zaakspecifiekeAutorisatieService.shouldMarkZaakspecifiekGeautoriseerd(
                            zaakType = zaakType,
                            requestedMarking = true,
                            isAlreadyZaakspecifiekGeautoriseerd = false,
                            currentAndRequestedBehandelaarIds = setOf("fakeBehandelaarId"),
                            loggedInUser = createLoggedInUser(id = "fakeBehandelaarId")
                        )
                    }

                then("the request is refused with its own error code") {
                    zaaktypeNotZaakspecifiekAutoriseerbaarException.errorCode shouldBe
                        ErrorCode.ERROR_CODE_ZAAKTYPE_NOT_ZAAKSPECIFIEK_AUTORISEERBAAR
                }
            }
        }

        given("an unmarked zaak of a zaaktype without the geautoriseerde medewerker roltype") {
            val zaakType = createZaakType()
            every {
                ztcClientService.findEigenschap(zaakType.url, ZAAKEIGENSCHAP_NAAM_GEAUTORISEERD)
            } returns createEigenschap(naam = ZAAKEIGENSCHAP_NAAM_GEAUTORISEERD)
            every { zgwApiService.findZaakspecifiekGeautoriseerdeMedewerkerRoltype(zaakType.url) } returns null

            `when`("the behandelaar asks for the marking") {
                val zaaktypeNotZaakspecifiekAutoriseerbaarException =
                    shouldThrow<ZaaktypeNotZaakspecifiekAutoriseerbaarException> {
                        zaakspecifiekeAutorisatieService.shouldMarkZaakspecifiekGeautoriseerd(
                            zaakType = zaakType,
                            requestedMarking = true,
                            isAlreadyZaakspecifiekGeautoriseerd = false,
                            currentAndRequestedBehandelaarIds = setOf("fakeBehandelaarId"),
                            loggedInUser = createLoggedInUser(id = "fakeBehandelaarId")
                        )
                    }

                then("the request is refused with its own error code") {
                    zaaktypeNotZaakspecifiekAutoriseerbaarException.errorCode shouldBe
                        ErrorCode.ERROR_CODE_ZAAKTYPE_NOT_ZAAKSPECIFIEK_AUTORISEERBAAR
                }
            }
        }

        given("an unmarked zaak of a zaakspecifiek autoriseerbaar zaaktype without a behandelaar") {
            val zaakType = createZaakType()
            every {
                ztcClientService.findEigenschap(zaakType.url, ZAAKEIGENSCHAP_NAAM_GEAUTORISEERD)
            } returns createEigenschap(naam = ZAAKEIGENSCHAP_NAAM_GEAUTORISEERD)
            every {
                zgwApiService.findZaakspecifiekGeautoriseerdeMedewerkerRoltype(zaakType.url)
            } returns createZaakspecifiekGeautoriseerdeMedewerkerRolType(zaakTypeUri = zaakType.url)

            `when`("an employee asks for the marking") {
                val zaakWithoutBehandelaarCannotBeMarkedException =
                    shouldThrow<ZaakWithoutBehandelaarCannotBeMarkedException> {
                        zaakspecifiekeAutorisatieService.shouldMarkZaakspecifiekGeautoriseerd(
                            zaakType = zaakType,
                            requestedMarking = true,
                            isAlreadyZaakspecifiekGeautoriseerd = false,
                            currentAndRequestedBehandelaarIds = emptySet(),
                            loggedInUser = createLoggedInUser()
                        )
                    }

                then("the request is refused with its own error code") {
                    zaakWithoutBehandelaarCannotBeMarkedException.errorCode shouldBe
                        ErrorCode.ERROR_CODE_ZAAK_WITHOUT_BEHANDELAAR_CANNOT_BE_MARKED
                }
            }
        }

        given("an unmarked zaak whose behandelaar is someone else than the logged-in user") {
            val zaakType = createZaakType(omschrijving = "fakeZaaktypeOmschrijving")
            every {
                ztcClientService.findEigenschap(zaakType.url, ZAAKEIGENSCHAP_NAAM_GEAUTORISEERD)
            } returns createEigenschap(naam = ZAAKEIGENSCHAP_NAAM_GEAUTORISEERD)
            every {
                zgwApiService.findZaakspecifiekGeautoriseerdeMedewerkerRoltype(zaakType.url)
            } returns createZaakspecifiekGeautoriseerdeMedewerkerRolType(zaakTypeUri = zaakType.url)

            `when`("a user who does not hold the zaakspecifiek_geautoriseerd role asks for the marking") {
                val zaakspecifiekeAutorisatieNotAllowedException =
                    shouldThrow<ZaakspecifiekeAutorisatieNotAllowedException> {
                        zaakspecifiekeAutorisatieService.shouldMarkZaakspecifiekGeautoriseerd(
                            zaakType = zaakType,
                            requestedMarking = true,
                            isAlreadyZaakspecifiekGeautoriseerd = false,
                            currentAndRequestedBehandelaarIds = setOf("fakeBehandelaarId"),
                            loggedInUser = createLoggedInUser(id = "fakeOtherUserId")
                        )
                    }

                then("the request is refused with its own error code") {
                    zaakspecifiekeAutorisatieNotAllowedException.errorCode shouldBe
                        ErrorCode.ERROR_CODE_ZAAKSPECIFIEKE_AUTORISATIE_NOT_ALLOWED
                }
            }

            `when`("a user who does not hold the zaakspecifiek_geautoriseerd role takes the zaak over and marks it in one update") {
                val shouldMark = zaakspecifiekeAutorisatieService.shouldMarkZaakspecifiekGeautoriseerd(
                    zaakType = zaakType,
                    requestedMarking = true,
                    isAlreadyZaakspecifiekGeautoriseerd = false,
                    currentAndRequestedBehandelaarIds = setOf("fakeBehandelaarId", "fakeOtherUserId"),
                    loggedInUser = createLoggedInUser(id = "fakeOtherUserId")
                )

                then("the zaak is marked, because the user becomes its behandelaar") {
                    shouldMark shouldBe true
                }
            }

            `when`("a user who holds the zaakspecifiek_geautoriseerd role for the zaaktype asks for the marking") {
                val shouldMark = zaakspecifiekeAutorisatieService.shouldMarkZaakspecifiekGeautoriseerd(
                    zaakType = zaakType,
                    requestedMarking = true,
                    isAlreadyZaakspecifiekGeautoriseerd = false,
                    currentAndRequestedBehandelaarIds = setOf("fakeBehandelaarId"),
                    loggedInUser = createLoggedInUser(
                        id = "fakeOtherUserId",
                        applicationRolesPerZaaktype = mapOf(
                            "fakeZaaktypeOmschrijving" to setOf(ROLE_NAME_ZAAKSPECIFIEK_GEAUTORISEERD)
                        )
                    )
                )

                then("the zaak is marked") {
                    shouldMark shouldBe true
                }
            }

            `when`("a user who holds the zaakspecifiek_geautoriseerd role for every zaaktype asks for the marking") {
                val shouldMark = zaakspecifiekeAutorisatieService.shouldMarkZaakspecifiekGeautoriseerd(
                    zaakType = zaakType,
                    requestedMarking = true,
                    isAlreadyZaakspecifiekGeautoriseerd = false,
                    currentAndRequestedBehandelaarIds = setOf("fakeBehandelaarId"),
                    loggedInUser = createLoggedInUser(
                        id = "fakeOtherUserId",
                        overallRoles = setOf(ROLE_NAME_ZAAKSPECIFIEK_GEAUTORISEERD)
                    )
                )

                then("the zaak is marked") {
                    shouldMark shouldBe true
                }
            }
        }

        given("a zaak that is already marked as zaakspecifiek geautoriseerd") {
            val zaakType = createZaakType()

            `when`("the marking is requested again") {
                val shouldMark = zaakspecifiekeAutorisatieService.shouldMarkZaakspecifiekGeautoriseerd(
                    zaakType = zaakType,
                    requestedMarking = true,
                    isAlreadyZaakspecifiekGeautoriseerd = true,
                    currentAndRequestedBehandelaarIds = setOf("fakeBehandelaarId"),
                    loggedInUser = createLoggedInUser(id = "fakeBehandelaarId")
                )

                then("no second marking is recorded and no zaaktype lookup is needed") {
                    shouldMark shouldBe false
                    verify(exactly = 0) { ztcClientService.findEigenschap(any(), any()) }
                }
            }

            `when`("an attempt is made to lift the marking") {
                val zaakspecifiekeAutorisatieCannotBeLiftedException =
                    shouldThrow<ZaakspecifiekeAutorisatieCannotBeLiftedException> {
                        zaakspecifiekeAutorisatieService.shouldMarkZaakspecifiekGeautoriseerd(
                            zaakType = zaakType,
                            requestedMarking = false,
                            isAlreadyZaakspecifiekGeautoriseerd = true,
                            currentAndRequestedBehandelaarIds = setOf("fakeBehandelaarId"),
                            loggedInUser = createLoggedInUser(id = "fakeBehandelaarId")
                        )
                    }

                then("the request is refused with its own error code") {
                    zaakspecifiekeAutorisatieCannotBeLiftedException.errorCode shouldBe
                        ErrorCode.ERROR_CODE_ZAAKSPECIFIEKE_AUTORISATIE_CANNOT_BE_LIFTED
                }
            }
        }
    }

    context("Reading who a zaak is assigned to and who is individually authorised for it") {
        given("a marked zaak with a groep, a behandelaar and an individually authorised medewerker") {
            val zaak = createZaak()
            val groepRol = createRolOrganisatorischeEenheid(zaakURI = zaak.url)
            val behandelaarRol = createRolMedewerker(
                zaakURI = zaak.url,
                medewerkerIdentificatie = createMedewerkerIdentificatie(identificatie = "fakeBehandelaarId")
            )
            val geautoriseerdeMedewerkerRol = createRolMedewerker(
                zaakURI = zaak.url,
                medewerkerIdentificatie = createMedewerkerIdentificatie(
                    identificatie = "fakeGeautoriseerdeMedewerkerId"
                )
            )
            val rollen = listOf<Rol<*>>(groepRol, behandelaarRol, geautoriseerdeMedewerkerRol)
            every { zrcClientService.listZaakeigenschappen(zaak.uuid) } returns listOf(geautoriseerdZaakEigenschap)
            every { zgwApiService.findGroepForZaak(zaak, rollen) } returns groepRol
            every {
                zgwApiService.listBehandelaarMedewerkerRolesForZaak(zaak, rollen)
            } returns listOf(behandelaarRol)
            every {
                zgwApiService.listZaakspecifiekGeautoriseerdeMedewerkerRolesForZaak(zaak, rollen)
            } returns listOf(geautoriseerdeMedewerkerRol)

            `when`("the toewijzing is read from those pre-fetched rollen") {
                val zaakToewijzing = zaakspecifiekeAutorisatieService.readZaakToewijzing(zaak, rollen)

                then("the groep, the behandelaar and the individually authorised medewerker are all resolved") {
                    zaakToewijzing.groepId shouldBe groepRol.identificatienummer
                    zaakToewijzing.behandelaarId shouldBe "fakeBehandelaarId"
                    zaakToewijzing.isZaakspecifiekGeautoriseerd shouldBe true
                }

                and("the behandelaar and the individually authorised medewerker may both see the zaak") {
                    zaakToewijzing.geautoriseerdeMedewerkerIds shouldBe
                        setOf("fakeBehandelaarId", "fakeGeautoriseerdeMedewerkerId")
                    zaakToewijzing.isGeautoriseerdeMedewerker("fakeGeautoriseerdeMedewerkerId") shouldBe true
                    zaakToewijzing.isGeautoriseerdeMedewerker("fakeOtherMedewerkerId") shouldBe false
                }

                and("the rollen of the zaak are not fetched again") {
                    verify(exactly = 0) { zrcClientService.listRollen(zaak) }
                }
            }
        }

        given("an ordinary zaak with only a behandelaar whose rollen are not pre-fetched") {
            val zaak = createZaak()
            val behandelaarRol = createRolMedewerker(zaakURI = zaak.url)
            val rollen = listOf<Rol<*>>(behandelaarRol)
            every { zrcClientService.listRollen(zaak) } returns rollen
            every { zrcClientService.listZaakeigenschappen(zaak.uuid) } returns emptyList()
            every { zgwApiService.findGroepForZaak(zaak, rollen) } returns null
            every {
                zgwApiService.listBehandelaarMedewerkerRolesForZaak(zaak, rollen)
            } returns listOf(behandelaarRol)
            every {
                zgwApiService.listZaakspecifiekGeautoriseerdeMedewerkerRolesForZaak(zaak, rollen)
            } returns emptyList()

            `when`("the toewijzing is read") {
                val zaakToewijzing = zaakspecifiekeAutorisatieService.readZaakToewijzing(zaak)

                then("the rollen are fetched once and shared by every lookup") {
                    verify(exactly = 1) { zrcClientService.listRollen(zaak) }
                }

                and("the zaak has a behandelaar, no groep and no individual authorisations") {
                    zaakToewijzing.behandelaar shouldBe behandelaarRol
                    zaakToewijzing.groepId shouldBe null
                    zaakToewijzing.isZaakspecifiekGeautoriseerd shouldBe false
                    zaakToewijzing.zaakspecifiekGeautoriseerdeMedewerkers.shouldBeEmpty()
                }
            }
        }

        given("a zaak whose marking the caller already determined") {
            val zaak = createZaak()
            val rollen = emptyList<Rol<*>>()
            every { zgwApiService.findGroepForZaak(zaak, rollen) } returns null
            every { zgwApiService.listBehandelaarMedewerkerRolesForZaak(zaak, rollen) } returns emptyList()
            every {
                zgwApiService.listZaakspecifiekGeautoriseerdeMedewerkerRolesForZaak(zaak, rollen)
            } returns emptyList()

            `when`("the toewijzing is read with that marking") {
                val zaakToewijzing = zaakspecifiekeAutorisatieService.readZaakToewijzing(
                    zaak = zaak,
                    rollen = rollen,
                    isZaakspecifiekGeautoriseerd = true
                )

                then("the marking is taken from the caller instead of from the zaakeigenschappen") {
                    zaakToewijzing.isZaakspecifiekGeautoriseerd shouldBe true
                    verify(exactly = 0) { zrcClientService.listZaakeigenschappen(zaak.uuid) }
                }
            }
        }

        given("a zaak that was given two behandelaar rollen outside ZAC") {
            val zaak = createZaak()
            val firstBehandelaarRol = createRolMedewerker(
                zaakURI = zaak.url,
                medewerkerIdentificatie = createMedewerkerIdentificatie(identificatie = "fakeBehandelaarId1")
            )
            val secondBehandelaarRol = createRolMedewerker(
                zaakURI = zaak.url,
                medewerkerIdentificatie = createMedewerkerIdentificatie(identificatie = "fakeBehandelaarId2")
            )
            val rollen = listOf<Rol<*>>(firstBehandelaarRol, secondBehandelaarRol)
            every { zrcClientService.listZaakeigenschappen(zaak.uuid) } returns listOf(geautoriseerdZaakEigenschap)
            every { zgwApiService.findGroepForZaak(zaak, rollen) } returns null
            every {
                zgwApiService.listBehandelaarMedewerkerRolesForZaak(zaak, rollen)
            } returns listOf(firstBehandelaarRol, secondBehandelaarRol)
            every {
                zgwApiService.listZaakspecifiekGeautoriseerdeMedewerkerRolesForZaak(zaak, rollen)
            } returns emptyList()

            `when`("the toewijzing is read") {
                val zaakToewijzing = zaakspecifiekeAutorisatieService.readZaakToewijzing(zaak, rollen)

                then("both rollen are kept so that they can all be replaced on the next assignment") {
                    zaakToewijzing.behandelaarRollen shouldContainExactly
                        listOf(firstBehandelaarRol, secondBehandelaarRol)
                }

                and("no single behandelaar can be named, while both medewerkers may still see the zaak") {
                    zaakToewijzing.behandelaar shouldBe null
                    zaakToewijzing.behandelaarId shouldBe null
                    zaakToewijzing.geautoriseerdeMedewerkerIds shouldBe
                        setOf("fakeBehandelaarId1", "fakeBehandelaarId2")
                }
            }
        }
    }

    context("Guarding the behandelaar of a zaak in the shared assignment flow") {
        given("a zaakspecifiek geautoriseerde zaak with a behandelaar") {
            val zaakToewijzing = createZaakToewijzing(
                behandelaarRollen = listOf(
                    createRolMedewerker(
                        medewerkerIdentificatie = createMedewerkerIdentificatie(
                            identificatie = "fakeCurrentBehandelaarId"
                        )
                    )
                ),
                isZaakspecifiekGeautoriseerd = true
            )

            `when`("it is released") {
                val zaakspecifiekGeautoriseerdeZaakCannotBeReleasedException =
                    shouldThrow<ZaakspecifiekGeautoriseerdeZaakCannotBeReleasedException> {
                        zaakspecifiekeAutorisatieService.assertBehandelaarMayChange(
                            zaakToewijzing = zaakToewijzing,
                            requestedBehandelaarId = null
                        )
                    }

                then("the release is refused with its own error code") {
                    zaakspecifiekGeautoriseerdeZaakCannotBeReleasedException.errorCode shouldBe
                        ErrorCode.ERROR_CODE_ZAAKSPECIFIEK_GEAUTORISEERDE_ZAAK_CANNOT_BE_RELEASED
                }
            }

            `when`("it is assigned to a group without naming a behandelaar") {
                val zaakspecifiekGeautoriseerdeZaakCannotBeReleasedException =
                    shouldThrow<ZaakspecifiekGeautoriseerdeZaakCannotBeReleasedException> {
                        zaakspecifiekeAutorisatieService.assertBehandelaarMayChange(
                            zaakToewijzing = zaakToewijzing,
                            requestedBehandelaarId = ""
                        )
                    }

                then("the assignment is refused, because it would leave the zaak without a behandelaar") {
                    zaakspecifiekGeautoriseerdeZaakCannotBeReleasedException.errorCode shouldBe
                        ErrorCode.ERROR_CODE_ZAAKSPECIFIEK_GEAUTORISEERDE_ZAAK_CANNOT_BE_RELEASED
                }
            }

            `when`("it is handed over to another behandelaar") {
                zaakspecifiekeAutorisatieService.assertBehandelaarMayChange(
                    zaakToewijzing = zaakToewijzing,
                    requestedBehandelaarId = "fakeOtherBehandelaarId"
                )

                then("the hand-over is allowed") {}
            }

            `when`("it is assigned to the behandelaar it already has") {
                zaakspecifiekeAutorisatieService.assertBehandelaarMayChange(
                    zaakToewijzing = zaakToewijzing,
                    requestedBehandelaarId = "fakeCurrentBehandelaarId"
                )

                then("the assignment is allowed") {}
            }
        }

        given("a zaakspecifiek geautoriseerde zaak whose behandelaar was removed outside ZAC") {
            val zaakToewijzing = createZaakToewijzing(isZaakspecifiekGeautoriseerd = true)

            `when`("it is assigned to a new behandelaar") {
                zaakspecifiekeAutorisatieService.assertBehandelaarMayChange(
                    zaakToewijzing = zaakToewijzing,
                    requestedBehandelaarId = "fakeNewBehandelaarId"
                )

                then("the assignment is allowed, so that the zaak does not stay without a behandelaar") {}
            }

            `when`("it is released") {
                zaakspecifiekeAutorisatieService.assertBehandelaarMayChange(
                    zaakToewijzing = zaakToewijzing,
                    requestedBehandelaarId = null
                )

                then("the release is allowed, because there is no behandelaar to lose") {}
            }
        }

        given("an ordinary zaak with a behandelaar") {
            val zaakToewijzing = createZaakToewijzing(
                behandelaarRollen = listOf(
                    createRolMedewerker(
                        medewerkerIdentificatie = createMedewerkerIdentificatie(
                            identificatie = "fakeCurrentBehandelaarId"
                        )
                    )
                )
            )

            `when`("it is released") {
                zaakspecifiekeAutorisatieService.assertBehandelaarMayChange(
                    zaakToewijzing = zaakToewijzing,
                    requestedBehandelaarId = null
                )

                then("the release is allowed") {}
            }
        }
    }

    context("Checking whether the behandelaar of a zaak can be handed over before anything is written") {
        given("a zaakspecifiek geautoriseerde zaak whose zaaktype does not define the zaakspecifiek geautoriseerde medewerker roltype") {
            val zaak = createZaak()
            every { zgwApiService.findZaakspecifiekGeautoriseerdeMedewerkerRoltype(zaak.zaaktype) } returns null

            `when`("its behandelaar is handed over to another behandelaar") {
                val zaakspecifiekGeautoriseerdeMedewerkerRoltypeNotFoundException =
                    shouldThrow<ZaakspecifiekGeautoriseerdeMedewerkerRoltypeNotFoundException> {
                        zaakspecifiekeAutorisatieService.assertBehandelaarCanBeHandedOver(
                            zaak = zaak,
                            isZaakspecifiekGeautoriseerd = true,
                            currentBehandelaarId = "fakeBehandelaarId",
                            requestedBehandelaarId = "fakeOtherBehandelaarId"
                        )
                    }

                then("the handover is refused, because the previous behandelaar could not keep access") {
                    zaakspecifiekGeautoriseerdeMedewerkerRoltypeNotFoundException.errorCode shouldBe
                        ErrorCode.ERROR_CODE_ZAAKSPECIFIEK_GEAUTORISEERDE_MEDEWERKER_ROLTYPE_NOT_FOUND
                }
            }
        }

        given("a zaakspecifiek geautoriseerde zaak whose zaaktype defines the zaakspecifiek geautoriseerde medewerker roltype") {
            val zaak = createZaak()
            every {
                zgwApiService.findZaakspecifiekGeautoriseerdeMedewerkerRoltype(zaak.zaaktype)
            } returns createZaakspecifiekGeautoriseerdeMedewerkerRolType(zaakTypeUri = zaak.zaaktype)

            `when`("its behandelaar is handed over to another behandelaar") {
                zaakspecifiekeAutorisatieService.assertBehandelaarCanBeHandedOver(
                    zaak = zaak,
                    isZaakspecifiekGeautoriseerd = true,
                    currentBehandelaarId = "fakeBehandelaarId",
                    requestedBehandelaarId = "fakeOtherBehandelaarId"
                )

                then("the handover is allowed") {
                    verify(exactly = 1) { zgwApiService.findZaakspecifiekGeautoriseerdeMedewerkerRoltype(zaak.zaaktype) }
                }
            }
        }

        given("a zaakspecifiek geautoriseerde zaak with a behandelaar") {
            val zaak = createZaak()

            `when`("it is assigned to the behandelaar it already has") {
                zaakspecifiekeAutorisatieService.assertBehandelaarCanBeHandedOver(
                    zaak = zaak,
                    isZaakspecifiekGeautoriseerd = true,
                    currentBehandelaarId = "fakeBehandelaarId",
                    requestedBehandelaarId = "fakeBehandelaarId"
                )

                then("the zaaktype is not consulted, because nobody loses access") {
                    verify(exactly = 0) { zgwApiService.findZaakspecifiekGeautoriseerdeMedewerkerRoltype(any()) }
                }
            }
        }

        given("a zaakspecifiek geautoriseerde zaak without a behandelaar") {
            val zaak = createZaak()

            `when`("it is assigned to a behandelaar") {
                zaakspecifiekeAutorisatieService.assertBehandelaarCanBeHandedOver(
                    zaak = zaak,
                    isZaakspecifiekGeautoriseerd = true,
                    currentBehandelaarId = null,
                    requestedBehandelaarId = "fakeBehandelaarId"
                )

                then("the zaaktype is not consulted, because there is no previous behandelaar to keep authorised") {
                    verify(exactly = 0) { zgwApiService.findZaakspecifiekGeautoriseerdeMedewerkerRoltype(any()) }
                }
            }
        }

        given("a zaak that is not zaakspecifiek geautoriseerd") {
            val zaak = createZaak()

            `when`("its behandelaar is handed over to another behandelaar") {
                zaakspecifiekeAutorisatieService.assertBehandelaarCanBeHandedOver(
                    zaak = zaak,
                    isZaakspecifiekGeautoriseerd = false,
                    currentBehandelaarId = "fakeBehandelaarId",
                    requestedBehandelaarId = "fakeOtherBehandelaarId"
                )

                then("the zaaktype is not consulted, because the previous behandelaar is not kept authorised") {
                    verify(exactly = 0) { zgwApiService.findZaakspecifiekGeautoriseerdeMedewerkerRoltype(any()) }
                }
            }
        }
    }

    context("Granting a medewerker individual access to a zaak") {
        given("a zaak whose zaaktype defines the zaakspecifiek geautoriseerde medewerker roltype") {
            val zaak = createZaak()
            val zaakspecifiekGeautoriseerdeMedewerkerRolType =
                createZaakspecifiekGeautoriseerdeMedewerkerRolType(zaakTypeUri = zaak.zaaktype)
            val medewerkerIdentificatie = createMedewerkerIdentificatie(
                identificatie = "fakePreviousBehandelaarId",
                voorletters = "fakeVoorletters",
                voorvoegselAchternaam = "fakeVoorvoegselAchternaam",
                achternaam = "fakeAchternaam"
            )
            val rolSlot = slot<Rol<*>>()
            every {
                zgwApiService.findZaakspecifiekGeautoriseerdeMedewerkerRoltype(zaak.zaaktype)
            } returns zaakspecifiekGeautoriseerdeMedewerkerRolType
            every {
                zgwApiService.listZaakspecifiekGeautoriseerdeMedewerkerRolesForZaak(zaak)
            } returns emptyList()
            every { zrcClientService.createRol(capture(rolSlot), "fakeReason") } returns createRolMedewerker()

            `when`("a medewerker that does not hold an individual authorisation yet is granted access") {
                val isGranted = zaakspecifiekeAutorisatieService.grantZaakspecifiekeAutorisatie(
                    zaak = zaak,
                    medewerker = medewerkerIdentificatie,
                    reason = "fakeReason"
                )

                then("a zaakspecifiek geautoriseerde medewerker rol is created for them") {
                    isGranted shouldBe true
                    with(rolSlot.captured) {
                        this.zaak shouldBe zaak.url
                        this.roltype shouldBe zaakspecifiekGeautoriseerdeMedewerkerRolType.url
                        betrokkeneType shouldBe BetrokkeneTypeEnum.MEDEWERKER
                        roltoelichting shouldBe "Zaakspecifiek geautoriseerde medewerker van de zaak"
                    }
                }

                and("the rol carries the name of the medewerker, so that it reads as a person in the zaak") {
                    with(rolSlot.captured.betrokkeneIdentificatie as MedewerkerIdentificatie) {
                        identificatie shouldBe "fakePreviousBehandelaarId"
                        voorletters shouldBe "fakeVoorletters"
                        voorvoegselAchternaam shouldBe "fakeVoorvoegselAchternaam"
                        achternaam shouldBe "fakeAchternaam"
                    }
                }
            }
        }

        given("a zaak on which the medewerker already holds an individual authorisation") {
            val zaak = createZaak()
            val medewerkerIdentificatie = createMedewerkerIdentificatie(identificatie = "fakeMedewerkerId")
            every {
                zgwApiService.findZaakspecifiekGeautoriseerdeMedewerkerRoltype(zaak.zaaktype)
            } returns createZaakspecifiekGeautoriseerdeMedewerkerRolType(zaakTypeUri = zaak.zaaktype)
            every {
                zgwApiService.listZaakspecifiekGeautoriseerdeMedewerkerRolesForZaak(zaak)
            } returns listOf(createRolMedewerker(medewerkerIdentificatie = medewerkerIdentificatie))

            `when`("the same medewerker is granted access again") {
                val isGranted = zaakspecifiekeAutorisatieService.grantZaakspecifiekeAutorisatie(
                    zaak = zaak,
                    medewerker = medewerkerIdentificatie,
                    reason = "fakeReason"
                )

                then("no second rol is created") {
                    isGranted shouldBe false
                    verify(exactly = 0) { zrcClientService.createRol(any(), any()) }
                }
            }
        }

        given("a zaak whose zaaktype does not define the zaakspecifiek geautoriseerde medewerker roltype") {
            val zaak = createZaak()
            every { zgwApiService.findZaakspecifiekGeautoriseerdeMedewerkerRoltype(zaak.zaaktype) } returns null

            `when`("a medewerker is granted access") {
                val zaakspecifiekGeautoriseerdeMedewerkerRoltypeNotFoundException =
                    shouldThrow<ZaakspecifiekGeautoriseerdeMedewerkerRoltypeNotFoundException> {
                        zaakspecifiekeAutorisatieService.grantZaakspecifiekeAutorisatie(
                            zaak = zaak,
                            medewerker = createMedewerkerIdentificatie(identificatie = "fakeMedewerkerId"),
                            reason = "fakeReason"
                        )
                    }

                then("the caller is told which roltype the zaaktype is missing") {
                    zaakspecifiekGeautoriseerdeMedewerkerRoltypeNotFoundException.message shouldBe
                        "Roltype 'Zaakspecifiek geautoriseerde medewerker' not found for zaaktype " +
                        "'${zaak.zaaktype}' of zaak with UUID '${zaak.uuid}'"
                }

                and("the failure carries its own error code") {
                    zaakspecifiekGeautoriseerdeMedewerkerRoltypeNotFoundException.errorCode shouldBe
                        ErrorCode.ERROR_CODE_ZAAKSPECIFIEK_GEAUTORISEERDE_MEDEWERKER_ROLTYPE_NOT_FOUND
                }
            }
        }

        given("a behandelaar whose medewerker identificatie carries no identificatie") {
            val zaak = createZaak()

            `when`("that behandelaar is granted access") {
                val isGranted = zaakspecifiekeAutorisatieService.grantZaakspecifiekeAutorisatie(
                    zaak = zaak,
                    medewerker = MedewerkerIdentificatie(),
                    reason = "fakeReason"
                )

                then("nothing is granted and the zaaktype is not consulted") {
                    isGranted shouldBe false
                    verify(exactly = 0) {
                        zgwApiService.findZaakspecifiekGeautoriseerdeMedewerkerRoltype(any())
                        zrcClientService.createRol(any(), any())
                    }
                }
            }
        }
    }

    context("Marking a zaak as zaakspecifiek geautoriseerd") {
        given("an unmarked zaak") {
            val zaak = createZaak()
            val eigenschap = createEigenschap(naam = ZAAKEIGENSCHAP_NAAM_GEAUTORISEERD)
            val zaakEigenschapSlot = slot<ZaakEigenschap>()
            every { zrcClientService.listZaakeigenschappen(zaak.uuid) } returns emptyList()
            every { ztcClientService.readEigenschap(zaak.zaaktype, ZAAKEIGENSCHAP_NAAM_GEAUTORISEERD) } returns eigenschap
            every { zrcClientService.createEigenschap(zaak.uuid, capture(zaakEigenschapSlot)) } returns createZaakEigenschap()
            every { indexingService.addOrUpdateZaak(zaak.uuid, false) } returns true
            every { indexingService.addOrUpdateTakenForZaak(zaak.uuid) } just runs
            every { indexingService.addOrUpdateInformatieobjectenForZaak(zaak.uuid) } just runs

            `when`("the zaak is marked") {
                zaakspecifiekeAutorisatieService.markZaakspecifiekGeautoriseerd(zaak)

                then("the marking is recorded in the zaakregister") {
                    with(zaakEigenschapSlot.captured) {
                        this.eigenschap shouldBe eigenschap.url
                        this.zaak shouldBe zaak.url
                        waarde shouldBe "true"
                    }
                }

                and("the zaak, its taken and its documenten are reindexed right away, in that order") {
                    verifyOrder {
                        zrcClientService.createEigenschap(zaak.uuid, any())
                        indexingService.addOrUpdateZaak(zaak.uuid, false)
                        indexingService.addOrUpdateTakenForZaak(zaak.uuid)
                        indexingService.addOrUpdateInformatieobjectenForZaak(zaak.uuid)
                    }
                }
            }
        }
    }

    context("Reindexing the taken and documenten of a zaak after its behandelaar changed") {
        given("a zaakspecifiek geautoriseerde zaak") {
            val markedZaak = createZaak()
            every { zrcClientService.listZaakeigenschappen(markedZaak.uuid) } returns listOf(geautoriseerdZaakEigenschap)
            every { indexingService.addOrUpdateTakenForZaak(markedZaak.uuid) } just runs
            every { indexingService.addOrUpdateInformatieobjectenForZaak(markedZaak.uuid) } just runs

            `when`("its dependents are reindexed") {
                zaakspecifiekeAutorisatieService.reindexZaakspecifiekeAutorisatieDependents(markedZaak)

                then("its taken and documenten are reindexed so their authorisation data is not stale") {
                    verify(exactly = 1) {
                        indexingService.addOrUpdateTakenForZaak(markedZaak.uuid)
                        indexingService.addOrUpdateInformatieobjectenForZaak(markedZaak.uuid)
                    }
                }
            }
        }

        given("an ordinary zaak") {
            val zaak = createZaak()
            every { zrcClientService.listZaakeigenschappen(zaak.uuid) } returns emptyList()

            `when`("its dependents are reindexed") {
                zaakspecifiekeAutorisatieService.reindexZaakspecifiekeAutorisatieDependents(zaak)

                then("nothing is reindexed, because its taken and documenten carry no authorisation data") {
                    verify(exactly = 0) {
                        indexingService.addOrUpdateTakenForZaak(any())
                        indexingService.addOrUpdateInformatieobjectenForZaak(any())
                    }
                }
            }
        }
    }
})
