/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.app.gebruikersvoorkeuren

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.checkUnnecessaryStub
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.slot
import io.mockk.verify
import jakarta.enterprise.inject.Instance
import nl.info.zac.gebruikersvoorkeuren.GebruikersvoorkeurenService
import nl.info.zac.app.gebruikersvoorkeuren.model.RestDashboardCardInstelling
import nl.info.zac.app.gebruikersvoorkeuren.model.RestZoekopdracht
import nl.info.zac.gebruikersvoorkeuren.model.DashboardCardId
import nl.info.zac.gebruikersvoorkeuren.model.DashboardCardInstelling
import nl.info.zac.gebruikersvoorkeuren.model.TabelInstellingen
import nl.info.zac.gebruikersvoorkeuren.model.Werklijst
import nl.info.zac.gebruikersvoorkeuren.model.Zoekopdracht
import nl.info.zac.gebruikersvoorkeuren.model.ZoekopdrachtListParameters
import nl.info.zac.gebruikersvoorkeuren.model.createDashboardCardInstelling
import nl.info.zac.gebruikersvoorkeuren.model.createTabelInstellingen
import nl.info.zac.gebruikersvoorkeuren.model.createZoekopdracht
import nl.info.zac.authentication.LoggedInUser
import nl.info.zac.authentication.createLoggedInUser
import nl.info.zac.policy.PolicyService
import net.atos.zac.signalering.model.SignaleringType

class GebruikersvoorkeurenRestServiceTest : BehaviorSpec({
    val gebruikersvoorkeurenService = mockk<GebruikersvoorkeurenService>()
    val loggedInUserInstance = mockk<Instance<LoggedInUser>>()
    val policyService = mockk<PolicyService>()

    val gebruikersvoorkeurenRestService = GebruikersvoorkeurenRestService(
        gebruikersvoorkeurenService,
        loggedInUserInstance,
        policyService
    )

    val fakeLoggedInUser = createLoggedInUser(id = "fakeUserId1")

    afterEach { checkUnnecessaryStub() }

    context("listZoekopdrachten") {
        given("Zoekopdrachten exist for the logged-in user") {
            val fakeZoekopdracht = createZoekopdracht(medewerkerID = "fakeUserId1")
            every { loggedInUserInstance.get() } returns fakeLoggedInUser
            every { gebruikersvoorkeurenService.listZoekopdrachten(any()) } returns listOf(fakeZoekopdracht)

            `when`("listZoekopdrachten is called with werklijst MIJN_ZAKEN") {
                val result = gebruikersvoorkeurenRestService.listZoekopdrachten(Werklijst.MIJN_ZAKEN)

                then("GebruikersvoorkeurenService.listZoekopdrachten is called and the list is returned") {
                    verify { gebruikersvoorkeurenService.listZoekopdrachten(any()) }
                    result.size shouldBe 1
                }
            }
        }
    }

    context("deleteZoekopdracht") {
        given("A zoekopdracht with ID 42") {
            every { gebruikersvoorkeurenService.deleteZoekopdracht(42L) } just runs

            `when`("deleteZoekopdracht is called with ID 42") {
                gebruikersvoorkeurenRestService.deleteZoekopdracht(42L)

                then("GebruikersvoorkeurenService.deleteZoekopdracht is called") {
                    verify { gebruikersvoorkeurenService.deleteZoekopdracht(42L) }
                }
            }
        }
    }

    context("createOrUpdateZoekopdracht") {
        given("A REST zoekopdracht") {
            val restZoekopdracht = RestZoekopdracht(
                lijstID = Werklijst.MIJN_ZAKEN,
                naam = "fakeName",
                json = "{}"
            )
            val fakeZoekopdracht = createZoekopdracht()
            every { loggedInUserInstance.get() } returns fakeLoggedInUser
            every { gebruikersvoorkeurenService.createZoekopdracht(any()) } returns fakeZoekopdracht

            `when`("createOrUpdateZoekopdracht is called") {
                val result = gebruikersvoorkeurenRestService.createOrUpdateZoekopdracht(restZoekopdracht)

                then("GebruikersvoorkeurenService.createZoekopdracht is called and result is returned") {
                    verify { gebruikersvoorkeurenService.createZoekopdracht(any()) }
                    result.naam shouldBe fakeZoekopdracht.naam
                }
            }
        }
    }

    context("readTabelGegevens") {
        given("TabelInstellingen exist for werklijst MIJN_TAKEN") {
            val fakeTabelInstellingen = createTabelInstellingen(
                lijstID = Werklijst.MIJN_TAKEN,
                aantalPerPagina = 25
            )
            every { loggedInUserInstance.get() } returns fakeLoggedInUser
            every {
                gebruikersvoorkeurenService.readTabelInstellingen(Werklijst.MIJN_TAKEN, "fakeUserId1")
            } returns fakeTabelInstellingen
            every { policyService.readWerklijstRechten() } returns nl.info.zac.policy.output.createWerklijstRechten()

            `when`("readTabelGegevens is called with werklijst MIJN_TAKEN") {
                val result = gebruikersvoorkeurenRestService.readTabelGegevens(Werklijst.MIJN_TAKEN)

                then("aantalPerPagina from tabelInstellingen is returned") {
                    result.aantalPerPagina shouldBe 25
                }
            }
        }
    }

    context("updateAantalItemsPerPagina within bounds") {
        given("An aantal within the valid bounds") {
            val validAantal = TabelInstellingen.AANTAL_PER_PAGINA_DEFAULT
            every { loggedInUserInstance.get() } returns fakeLoggedInUser
            every { gebruikersvoorkeurenService.updateTabelInstellingen(any()) } just runs

            `when`("updateAantalItemsPerPagina is called with a valid aantal") {
                gebruikersvoorkeurenRestService.updateAantalItemsPerPagina(Werklijst.MIJN_ZAKEN, validAantal)

                then("GebruikersvoorkeurenService.updateTabelInstellingen is called") {
                    verify { gebruikersvoorkeurenService.updateTabelInstellingen(any()) }
                }
            }
        }
    }

    context("updateAantalItemsPerPagina out of bounds") {
        given("An aantal exceeding the maximum") {
            val tooLarge = TabelInstellingen.AANTAL_PER_PAGINA_MAX + 1

            `when`("updateAantalItemsPerPagina is called with too large aantal") {
                gebruikersvoorkeurenRestService.updateAantalItemsPerPagina(Werklijst.MIJN_ZAKEN, tooLarge)

                then("GebruikersvoorkeurenService.updateTabelInstellingen is NOT called") {
                    verify(exactly = 0) { gebruikersvoorkeurenService.updateTabelInstellingen(any()) }
                }
            }
        }
    }

    context("listDashboardCards") {
        given("Dashboard cards exist for the logged-in user") {
            val fakeDashboardCard = createDashboardCardInstelling(medewerkerId = "fakeUserId1")
            every { loggedInUserInstance.get() } returns fakeLoggedInUser
            every { gebruikersvoorkeurenService.listDashboardCards("fakeUserId1") } returns listOf(fakeDashboardCard)

            `when`("listDashboardCards is called") {
                val result = gebruikersvoorkeurenRestService.listDashboardCards()

                then("GebruikersvoorkeurenService.listDashboardCards is called and list is returned") {
                    verify { gebruikersvoorkeurenService.listDashboardCards("fakeUserId1") }
                    result.size shouldBe 1
                }
            }
        }
    }

    context("setZoekopdrachtActief") {
        given("A REST zoekopdracht") {
            val restZoekopdracht = RestZoekopdracht(
                id = 7L,
                lijstID = Werklijst.MIJN_TAKEN,
                naam = "fakeName",
                isActief = true,
                json = "{}"
            )
            val zoekopdrachtSlot = slot<Zoekopdracht>()
            every { loggedInUserInstance.get() } returns fakeLoggedInUser
            every { gebruikersvoorkeurenService.setActief(capture(zoekopdrachtSlot)) } just runs

            `when`("setZoekopdrachtActief is called") {
                gebruikersvoorkeurenRestService.setZoekopdrachtActief(restZoekopdracht)

                then("the zoekopdracht of the logged-in user is made active") {
                    with(zoekopdrachtSlot.captured) {
                        id shouldBe 7L
                        lijstID shouldBe Werklijst.MIJN_TAKEN
                        medewerkerID shouldBe "fakeUserId1"
                        naam shouldBe "fakeName"
                        isActief shouldBe true
                        json shouldBe "{}"
                    }
                }
            }
        }
    }

    context("removeZoekopdrachtActief") {
        given("A werklijst") {
            val listParametersSlot = slot<ZoekopdrachtListParameters>()
            every { loggedInUserInstance.get() } returns fakeLoggedInUser
            every { gebruikersvoorkeurenService.removeActief(capture(listParametersSlot)) } just runs

            `when`("removeZoekopdrachtActief is called with werklijst MIJN_ZAKEN") {
                gebruikersvoorkeurenRestService.removeZoekopdrachtActief(Werklijst.MIJN_ZAKEN)

                then("the active zoekopdracht of the logged-in user for that werklijst is removed") {
                    listParametersSlot.captured.lijstID shouldBe Werklijst.MIJN_ZAKEN
                    listParametersSlot.captured.medewerkerID shouldBe "fakeUserId1"
                }
            }
        }
    }

    context("updateAantalItemsPerPagina below the minimum") {
        given("An aantal below the minimum") {
            val tooSmall = TabelInstellingen.AANTAL_PER_PAGINA_MIN - 1

            `when`("updateAantalItemsPerPagina is called with too small aantal") {
                gebruikersvoorkeurenRestService.updateAantalItemsPerPagina(Werklijst.MIJN_ZAKEN, tooSmall)

                then("GebruikersvoorkeurenService.updateTabelInstellingen is NOT called") {
                    verify(exactly = 0) { gebruikersvoorkeurenService.updateTabelInstellingen(any()) }
                }
            }
        }
    }

    context("updateAantalItemsPerPagina at the minimum") {
        given("An aantal equal to the minimum") {
            val tabelInstellingenSlot = slot<TabelInstellingen>()
            every { loggedInUserInstance.get() } returns fakeLoggedInUser
            every { gebruikersvoorkeurenService.updateTabelInstellingen(capture(tabelInstellingenSlot)) } just runs

            `when`("updateAantalItemsPerPagina is called with the minimum aantal") {
                gebruikersvoorkeurenRestService.updateAantalItemsPerPagina(Werklijst.MIJN_TAKEN, TabelInstellingen.AANTAL_PER_PAGINA_MIN)

                then("the tabel instellingen of the logged-in user are updated") {
                    with(tabelInstellingenSlot.captured) {
                        aantalPerPagina shouldBe TabelInstellingen.AANTAL_PER_PAGINA_MIN
                        lijstID shouldBe Werklijst.MIJN_TAKEN
                        medewerkerID shouldBe "fakeUserId1"
                    }
                }
            }
        }
    }

    context("updateDashboardCards") {
        given("A list of REST dashboard cards") {
            val restDashboardCardInstelling = RestDashboardCardInstelling(
                id = 3L,
                cardId = DashboardCardId.MIJN_ZAKEN,
                signaleringType = SignaleringType.Type.ZAAK_OP_NAAM,
                column = 2,
                row = 1
            )
            val fakeDashboardCard = createDashboardCardInstelling(medewerkerId = "fakeUserId1")
            val dashboardCardsSlot = slot<List<DashboardCardInstelling>>()
            every { loggedInUserInstance.get() } returns fakeLoggedInUser
            every {
                gebruikersvoorkeurenService.updateDashboardCards("fakeUserId1", capture(dashboardCardsSlot))
            } just runs
            every { gebruikersvoorkeurenService.listDashboardCards("fakeUserId1") } returns listOf(fakeDashboardCard)

            `when`("updateDashboardCards is called") {
                val result = gebruikersvoorkeurenRestService.updateDashboardCards(listOf(restDashboardCardInstelling))

                then("the converted cards are stored and the current cards are returned") {
                    with(dashboardCardsSlot.captured.single()) {
                        id shouldBe 3L
                        cardId shouldBe DashboardCardId.MIJN_ZAKEN
                        signaleringType shouldBe SignaleringType.Type.ZAAK_OP_NAAM
                        kolom shouldBe 2
                        volgorde shouldBe 1
                    }
                    result.single().cardId shouldBe fakeDashboardCard.cardId
                }
            }
        }
    }

    context("addDashboardCard") {
        given("A REST dashboard card") {
            val restDashboardCardInstelling = RestDashboardCardInstelling(
                cardId = DashboardCardId.MIJN_TAKEN,
                column = 1,
                row = 0
            )
            val fakeDashboardCard = createDashboardCardInstelling(medewerkerId = "fakeUserId1")
            val dashboardCardSlot = slot<DashboardCardInstelling>()
            every { loggedInUserInstance.get() } returns fakeLoggedInUser
            every {
                gebruikersvoorkeurenService.addDashboardCard("fakeUserId1", capture(dashboardCardSlot))
            } just runs
            every { gebruikersvoorkeurenService.listDashboardCards("fakeUserId1") } returns listOf(fakeDashboardCard)

            `when`("addDashboardCard is called") {
                val result = gebruikersvoorkeurenRestService.addDashboardCard(restDashboardCardInstelling)

                then("the converted card is added and the current cards are returned") {
                    dashboardCardSlot.captured.cardId shouldBe DashboardCardId.MIJN_TAKEN
                    result.single().cardId shouldBe fakeDashboardCard.cardId
                }
            }
        }
    }

    context("deleteDashboardCard") {
        given("A REST dashboard card") {
            val restDashboardCardInstelling = RestDashboardCardInstelling(cardId = DashboardCardId.MIJN_TAKEN)
            val dashboardCardSlot = slot<DashboardCardInstelling>()
            every { loggedInUserInstance.get() } returns fakeLoggedInUser
            every {
                gebruikersvoorkeurenService.deleteDashboardCard("fakeUserId1", capture(dashboardCardSlot))
            } just runs
            every { gebruikersvoorkeurenService.listDashboardCards("fakeUserId1") } returns emptyList()

            `when`("deleteDashboardCard is called") {
                val result = gebruikersvoorkeurenRestService.deleteDashboardCard(restDashboardCardInstelling)

                then("the converted card is deleted and the remaining cards are returned") {
                    dashboardCardSlot.captured.cardId shouldBe DashboardCardId.MIJN_TAKEN
                    result shouldBe emptyList()
                }
            }
        }
    }
})
