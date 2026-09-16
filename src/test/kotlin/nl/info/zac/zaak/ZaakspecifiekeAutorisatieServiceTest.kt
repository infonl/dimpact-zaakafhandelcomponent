/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.zaak

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
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
import nl.info.client.zgw.model.createZaak
import nl.info.client.zgw.model.createZaakEigenschap
import nl.info.client.zgw.shared.ZgwApiService
import nl.info.client.zgw.zrc.ZrcClientService
import nl.info.client.zgw.zrc.model.generated.ZaakEigenschap
import nl.info.client.zgw.zrc.util.ZAAKEIGENSCHAP_NAAM_GEAUTORISEERD
import nl.info.client.zgw.ztc.ZtcClientService
import nl.info.client.zgw.ztc.model.createEigenschap
import nl.info.client.zgw.ztc.model.createZaakType
import nl.info.zac.app.zaak.exception.ZaakWithoutBehandelaarCannotBeMarkedException
import nl.info.zac.app.zaak.exception.ZaakspecifiekGeautoriseerdeZaakCannotBeReassignedException
import nl.info.zac.app.zaak.exception.ZaakspecifiekGeautoriseerdeZaakCannotBeReleasedException
import nl.info.zac.app.zaak.exception.ZaakspecifiekeAutorisatieCannotBeLiftedException
import nl.info.zac.app.zaak.exception.ZaakspecifiekeAutorisatieNotAllowedException
import nl.info.zac.app.zaak.exception.ZaaktypeNotZaakspecifiekAutoriseerbaarException
import nl.info.zac.authentication.createLoggedInUser
import nl.info.zac.exception.ErrorCode
import nl.info.zac.search.IndexingService

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

    context("Deciding whether an update marks a zaak as zaakspecifiek geautoriseerd") {
        given("an update that does not mention the marking at all") {
            val zaakType = createZaakType()

            `when`("the decision is made for an unmarked zaak") {
                val shouldMark = zaakspecifiekeAutorisatieService.shouldMarkZaakspecifiekGeautoriseerd(
                    zaakType = zaakType,
                    requestedMarking = null,
                    isAlreadyZaakspecifiekGeautoriseerd = false,
                    behandelaarId = "fakeBehandelaarId",
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

            `when`("the behandelaar asks for the marking") {
                val shouldMark = zaakspecifiekeAutorisatieService.shouldMarkZaakspecifiekGeautoriseerd(
                    zaakType = zaakType,
                    requestedMarking = true,
                    isAlreadyZaakspecifiekGeautoriseerd = false,
                    behandelaarId = "fakeBehandelaarId",
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
                    behandelaarId = "fakeBehandelaarId",
                    loggedInUser = createLoggedInUser(id = "fakeBehandelaarId")
                )

                then("the zaak stays unmarked") {
                    shouldMark shouldBe false
                }
            }
        }

        given("an unmarked zaak whose zaaktype is not zaakspecifiek autoriseerbaar") {
            val zaakType = createZaakType()
            every { ztcClientService.findEigenschap(zaakType.url, ZAAKEIGENSCHAP_NAAM_GEAUTORISEERD) } returns null

            `when`("the behandelaar asks for the marking") {
                val exception = shouldThrow<ZaaktypeNotZaakspecifiekAutoriseerbaarException> {
                    zaakspecifiekeAutorisatieService.shouldMarkZaakspecifiekGeautoriseerd(
                        zaakType = zaakType,
                        requestedMarking = true,
                        isAlreadyZaakspecifiekGeautoriseerd = false,
                        behandelaarId = "fakeBehandelaarId",
                        loggedInUser = createLoggedInUser(id = "fakeBehandelaarId")
                    )
                }

                then("the request is refused with its own error code") {
                    exception.errorCode shouldBe ErrorCode.ERROR_CODE_ZAAKTYPE_NOT_ZAAKSPECIFIEK_AUTORISEERBAAR
                }
            }
        }

        given("an unmarked zaak of a zaakspecifiek autoriseerbaar zaaktype without a behandelaar") {
            val zaakType = createZaakType()
            every {
                ztcClientService.findEigenschap(zaakType.url, ZAAKEIGENSCHAP_NAAM_GEAUTORISEERD)
            } returns createEigenschap(naam = ZAAKEIGENSCHAP_NAAM_GEAUTORISEERD)

            `when`("an employee asks for the marking") {
                val exception = shouldThrow<ZaakWithoutBehandelaarCannotBeMarkedException> {
                    zaakspecifiekeAutorisatieService.shouldMarkZaakspecifiekGeautoriseerd(
                        zaakType = zaakType,
                        requestedMarking = true,
                        isAlreadyZaakspecifiekGeautoriseerd = false,
                        behandelaarId = null,
                        loggedInUser = createLoggedInUser()
                    )
                }

                then("the request is refused with its own error code") {
                    exception.errorCode shouldBe ErrorCode.ERROR_CODE_ZAAK_WITHOUT_BEHANDELAAR_CANNOT_BE_MARKED
                }
            }
        }

        given("an unmarked zaak whose behandelaar is someone else than the logged-in user") {
            val zaakType = createZaakType(omschrijving = "fakeZaaktypeOmschrijving")
            every {
                ztcClientService.findEigenschap(zaakType.url, ZAAKEIGENSCHAP_NAAM_GEAUTORISEERD)
            } returns createEigenschap(naam = ZAAKEIGENSCHAP_NAAM_GEAUTORISEERD)

            `when`("a user who does not hold the zaakspecifiek_geautoriseerd role asks for the marking") {
                val exception = shouldThrow<ZaakspecifiekeAutorisatieNotAllowedException> {
                    zaakspecifiekeAutorisatieService.shouldMarkZaakspecifiekGeautoriseerd(
                        zaakType = zaakType,
                        requestedMarking = true,
                        isAlreadyZaakspecifiekGeautoriseerd = false,
                        behandelaarId = "fakeBehandelaarId",
                        loggedInUser = createLoggedInUser(id = "fakeOtherUserId")
                    )
                }

                then("the request is refused with its own error code") {
                    exception.errorCode shouldBe ErrorCode.ERROR_CODE_ZAAKSPECIFIEKE_AUTORISATIE_NOT_ALLOWED
                }
            }

            `when`("a user who holds the zaakspecifiek_geautoriseerd role for the zaaktype asks for the marking") {
                val shouldMark = zaakspecifiekeAutorisatieService.shouldMarkZaakspecifiekGeautoriseerd(
                    zaakType = zaakType,
                    requestedMarking = true,
                    isAlreadyZaakspecifiekGeautoriseerd = false,
                    behandelaarId = "fakeBehandelaarId",
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
                    behandelaarId = "fakeBehandelaarId",
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
                    behandelaarId = "fakeBehandelaarId",
                    loggedInUser = createLoggedInUser(id = "fakeBehandelaarId")
                )

                then("no second marking is recorded and no zaaktype lookup is needed") {
                    shouldMark shouldBe false
                    verify(exactly = 0) { ztcClientService.findEigenschap(any(), any()) }
                }
            }

            `when`("an attempt is made to lift the marking") {
                val exception = shouldThrow<ZaakspecifiekeAutorisatieCannotBeLiftedException> {
                    zaakspecifiekeAutorisatieService.shouldMarkZaakspecifiekGeautoriseerd(
                        zaakType = zaakType,
                        requestedMarking = false,
                        isAlreadyZaakspecifiekGeautoriseerd = true,
                        behandelaarId = "fakeBehandelaarId",
                        loggedInUser = createLoggedInUser(id = "fakeBehandelaarId")
                    )
                }

                then("the request is refused with its own error code") {
                    exception.errorCode shouldBe ErrorCode.ERROR_CODE_ZAAKSPECIFIEKE_AUTORISATIE_CANNOT_BE_LIFTED
                }
            }
        }
    }

    context("Guarding the behandelaar of a zaakspecifiek geautoriseerde zaak in an update") {
        given("a zaak with a behandelaar") {
            `when`("an update names a different behandelaar") {
                val exception = shouldThrow<ZaakspecifiekGeautoriseerdeZaakCannotBeReassignedException> {
                    zaakspecifiekeAutorisatieService.assertBehandelaarNotReassigned(
                        requestedBehandelaarId = "fakeOtherBehandelaarId",
                        currentBehandelaarId = "fakeBehandelaarId"
                    )
                }

                then("the update is refused with its own error code") {
                    exception.errorCode shouldBe
                        ErrorCode.ERROR_CODE_ZAAKSPECIFIEK_GEAUTORISEERDE_ZAAK_CANNOT_BE_REASSIGNED
                }
            }

            `when`("an update names the same behandelaar") {
                zaakspecifiekeAutorisatieService.assertBehandelaarNotReassigned(
                    requestedBehandelaarId = "fakeBehandelaarId",
                    currentBehandelaarId = "fakeBehandelaarId"
                )

                then("the update is allowed") {}
            }

            `when`("an update does not name a behandelaar") {
                zaakspecifiekeAutorisatieService.assertBehandelaarNotReassigned(
                    requestedBehandelaarId = null,
                    currentBehandelaarId = "fakeBehandelaarId"
                )

                then("the update is allowed, because the zaak keeps its behandelaar") {}
            }
        }

        given("a zaak whose behandelaar was removed outside ZAC") {
            `when`("an update names a new behandelaar") {
                zaakspecifiekeAutorisatieService.assertBehandelaarNotReassigned(
                    requestedBehandelaarId = "fakeNewBehandelaarId",
                    currentBehandelaarId = null
                )

                then("the update is allowed, so that the zaak does not stay without a behandelaar") {}
            }
        }
    }

    context("Guarding the behandelaar of a zaak in the shared assignment flow") {
        given("a zaakspecifiek geautoriseerde zaak with a behandelaar") {
            val markedZaak = createZaak()
            every { zrcClientService.listZaakeigenschappen(markedZaak.uuid) } returns listOf(geautoriseerdZaakEigenschap)
            every { zgwApiService.findBehandelaarMedewerkerRoleForZaak(markedZaak) } returns createRolMedewerker(
                medewerkerIdentificatie = createMedewerkerIdentificatie(identificatie = "fakeCurrentBehandelaarId")
            )

            `when`("it is assigned to a different behandelaar") {
                val exception = shouldThrow<ZaakspecifiekGeautoriseerdeZaakCannotBeReassignedException> {
                    zaakspecifiekeAutorisatieService.assertBehandelaarMayChange(markedZaak, "fakeOtherBehandelaarId")
                }

                then("the reassignment is refused with its own error code") {
                    exception.errorCode shouldBe
                        ErrorCode.ERROR_CODE_ZAAKSPECIFIEK_GEAUTORISEERDE_ZAAK_CANNOT_BE_REASSIGNED
                }
            }

            `when`("it is released") {
                val exception = shouldThrow<ZaakspecifiekGeautoriseerdeZaakCannotBeReleasedException> {
                    zaakspecifiekeAutorisatieService.assertBehandelaarMayChange(markedZaak, null)
                }

                then("the release is refused with its own error code") {
                    exception.errorCode shouldBe
                        ErrorCode.ERROR_CODE_ZAAKSPECIFIEK_GEAUTORISEERDE_ZAAK_CANNOT_BE_RELEASED
                }
            }

            `when`("it is assigned to the behandelaar it already has") {
                zaakspecifiekeAutorisatieService.assertBehandelaarMayChange(markedZaak, "fakeCurrentBehandelaarId")

                then("the assignment is allowed") {}
            }
        }

        given("a zaakspecifiek geautoriseerde zaak whose behandelaar was removed outside ZAC") {
            val markedZaak = createZaak()
            every { zrcClientService.listZaakeigenschappen(markedZaak.uuid) } returns listOf(geautoriseerdZaakEigenschap)
            every { zgwApiService.findBehandelaarMedewerkerRoleForZaak(markedZaak) } returns null

            `when`("it is assigned to a new behandelaar") {
                zaakspecifiekeAutorisatieService.assertBehandelaarMayChange(markedZaak, "fakeNewBehandelaarId")

                then("the assignment is allowed, so that the zaak does not stay without a behandelaar") {}
            }
        }

        given("an ordinary zaak") {
            val zaak = createZaak()
            every { zrcClientService.listZaakeigenschappen(zaak.uuid) } returns emptyList()

            `when`("it is assigned to a different behandelaar") {
                zaakspecifiekeAutorisatieService.assertBehandelaarMayChange(zaak, "fakeOtherBehandelaarId")

                then("the assignment is allowed without reading the behandelaar rol") {
                    verify(exactly = 0) { zgwApiService.findBehandelaarMedewerkerRoleForZaak(any()) }
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
