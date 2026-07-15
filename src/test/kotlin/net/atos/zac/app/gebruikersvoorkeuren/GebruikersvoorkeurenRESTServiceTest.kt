/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.zac.app.gebruikersvoorkeuren

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.checkUnnecessaryStub
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import jakarta.enterprise.inject.Instance
import net.atos.zac.gebruikersvoorkeuren.GebruikersvoorkeurenService
import net.atos.zac.gebruikersvoorkeuren.model.TabelInstellingen
import net.atos.zac.gebruikersvoorkeuren.model.Werklijst
import net.atos.zac.gebruikersvoorkeuren.model.createDashboardCardInstelling
import net.atos.zac.gebruikersvoorkeuren.model.createTabelInstellingen
import net.atos.zac.gebruikersvoorkeuren.model.createZoekopdracht
import nl.info.zac.authentication.LoggedInUser
import nl.info.zac.authentication.createLoggedInUser
import nl.info.zac.policy.PolicyService

class GebruikersvoorkeurenRESTServiceTest : BehaviorSpec({
    val gebruikersvoorkeurenService = mockk<GebruikersvoorkeurenService>()

    @Suppress("UNCHECKED_CAST")
    val loggedInUserInstance = mockk<Instance<LoggedInUser>>()
    val policyService = mockk<PolicyService>()

    val service = GebruikersvoorkeurenRESTService().also { instance ->
        instance.javaClass.getDeclaredField("gebruikersvoorkeurenService").also {
            it.isAccessible = true
            it.set(instance, gebruikersvoorkeurenService)
        }
        instance.javaClass.getDeclaredField("loggedInUserInstance").also {
            it.isAccessible = true
            it.set(instance, loggedInUserInstance)
        }
        instance.javaClass.getDeclaredField("policyService").also {
            it.isAccessible = true
            it.set(instance, policyService)
        }
    }

    val fakeLoggedInUser = createLoggedInUser(id = "fakeUserId1")

    afterEach { checkUnnecessaryStub() }

    context("listZoekopdrachten") {
        given("Zoekopdrachten exist for the logged-in user") {
            val fakeZoekopdracht = createZoekopdracht(medewerkerID = "fakeUserId1")
            every { loggedInUserInstance.get() } returns fakeLoggedInUser
            every { gebruikersvoorkeurenService.listZoekopdrachten(any()) } returns listOf(fakeZoekopdracht)

            `when`("listZoekopdrachten is called with werklijst MIJN_ZAKEN") {
                val result = service.listZoekopdrachten(Werklijst.MIJN_ZAKEN)

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
                service.deleteZoekopdracht(42L)

                then("GebruikersvoorkeurenService.deleteZoekopdracht is called") {
                    verify { gebruikersvoorkeurenService.deleteZoekopdracht(42L) }
                }
            }
        }
    }

    context("createOrUpdateZoekopdracht") {
        given("A REST zoekopdracht") {
            val restZoekopdracht = net.atos.zac.app.gebruikersvoorkeuren.model.RESTZoekopdracht()
            val fakeZoekopdracht = createZoekopdracht()
            every { loggedInUserInstance.get() } returns fakeLoggedInUser
            every { gebruikersvoorkeurenService.createZoekopdracht(any()) } returns fakeZoekopdracht

            `when`("createOrUpdateZoekopdracht is called") {
                val result = service.createOrUpdateZoekopdracht(restZoekopdracht)

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
                val result = service.readTabelGegevens(Werklijst.MIJN_TAKEN)

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
                service.updateAantalItemsPerPagina(Werklijst.MIJN_ZAKEN, validAantal)

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
                service.updateAantalItemsPerPagina(Werklijst.MIJN_ZAKEN, tooLarge)

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
                val result = service.listDashboardCards()

                then("GebruikersvoorkeurenService.listDashboardCards is called and list is returned") {
                    verify { gebruikersvoorkeurenService.listDashboardCards("fakeUserId1") }
                    result.size shouldBe 1
                }
            }
        }
    }
})
