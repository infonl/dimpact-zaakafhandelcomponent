/*
 * SPDX-FileCopyrightText: 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.zac.gebruikersvoorkeuren

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.optional.shouldBeEmpty
import io.kotest.matchers.optional.shouldBePresent
import io.kotest.matchers.shouldBe
import io.mockk.Runs
import io.mockk.checkUnnecessaryStub
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.verify
import jakarta.persistence.EntityManager
import jakarta.persistence.TypedQuery
import jakarta.persistence.criteria.CriteriaBuilder
import jakarta.persistence.criteria.CriteriaQuery
import jakarta.persistence.criteria.Expression
import jakarta.persistence.criteria.Order
import jakarta.persistence.criteria.Path
import jakarta.persistence.criteria.Predicate
import jakarta.persistence.criteria.Root
import net.atos.zac.gebruikersvoorkeuren.model.DashboardCardId
import net.atos.zac.gebruikersvoorkeuren.model.DashboardCardInstelling
import net.atos.zac.gebruikersvoorkeuren.model.TabelInstellingen
import net.atos.zac.gebruikersvoorkeuren.model.Werklijst
import net.atos.zac.gebruikersvoorkeuren.model.Zoekopdracht
import net.atos.zac.gebruikersvoorkeuren.model.ZoekopdrachtListParameters
import net.atos.zac.gebruikersvoorkeuren.model.createDashboardCardInstelling
import net.atos.zac.gebruikersvoorkeuren.model.createTabelInstellingen
import net.atos.zac.gebruikersvoorkeuren.model.createZoekopdracht
import net.atos.zac.signalering.model.SignaleringInstellingen
import net.atos.zac.signalering.model.SignaleringType
import nl.info.zac.signalering.SignaleringService

class GebruikersvoorkeurenServiceTest : BehaviorSpec({
    val entityManager = mockk<EntityManager>()
    val signaleringService = mockk<SignaleringService>()
    val gebruikersvoorkeurenService = GebruikersvoorkeurenService(entityManager, signaleringService)

    val criteriaBuilder = mockk<CriteriaBuilder>()
    val zoekopdrachtCriteriaQuery = mockk<CriteriaQuery<Zoekopdracht>>(relaxed = true)
    val zoekopdrachtRoot = mockk<Root<Zoekopdracht>>()
    val zoekopdrachtPath = mockk<Path<Any>>()
    val lowerExpression = mockk<Expression<String>>()
    val predicate = mockk<Predicate>()
    val zoekopdrachtTypedQuery = mockk<TypedQuery<Zoekopdracht>>()

    val tabelCriteriaQuery = mockk<CriteriaQuery<TabelInstellingen>>(relaxed = true)
    val tabelRoot = mockk<Root<TabelInstellingen>>()
    val tabelPath = mockk<Path<Any>>()
    val tabelTypedQuery = mockk<TypedQuery<TabelInstellingen>>()

    val dashboardCriteriaQuery = mockk<CriteriaQuery<DashboardCardInstelling>>(relaxed = true)
    val dashboardRoot = mockk<Root<DashboardCardInstelling>>()
    val dashboardPath = mockk<Path<Any>>()
    val dashboardOrder = mockk<Order>()
    val dashboardTypedQuery = mockk<TypedQuery<DashboardCardInstelling>>()

    fun setupZoekopdrachtCriteriaChain() {
        every { entityManager.criteriaBuilder } returns criteriaBuilder
        every { criteriaBuilder.createQuery(Zoekopdracht::class.java) } returns zoekopdrachtCriteriaQuery
        every { zoekopdrachtCriteriaQuery.from(Zoekopdracht::class.java) } returns zoekopdrachtRoot
        every { zoekopdrachtRoot.get<Any>(any<String>()) } returns zoekopdrachtPath
        every { criteriaBuilder.equal(any(), any<Any>()) } returns predicate
        every { criteriaBuilder.and(*anyVararg<Predicate>()) } returns predicate
        every { entityManager.createQuery(zoekopdrachtCriteriaQuery) } returns zoekopdrachtTypedQuery
    }

    fun setupTabelCriteriaChain() {
        every { entityManager.criteriaBuilder } returns criteriaBuilder
        every { criteriaBuilder.createQuery(TabelInstellingen::class.java) } returns tabelCriteriaQuery
        every { tabelCriteriaQuery.from(TabelInstellingen::class.java) } returns tabelRoot
        every { tabelRoot.get<Any>(any<String>()) } returns tabelPath
        every { criteriaBuilder.equal(any(), any<Any>()) } returns predicate
        every { criteriaBuilder.and(*anyVararg<Predicate>()) } returns predicate
        every { entityManager.createQuery(tabelCriteriaQuery) } returns tabelTypedQuery
    }

    fun setupDashboardCriteriaChain() {
        every { entityManager.criteriaBuilder } returns criteriaBuilder
        every { criteriaBuilder.createQuery(DashboardCardInstelling::class.java) } returns dashboardCriteriaQuery
        every { dashboardCriteriaQuery.from(DashboardCardInstelling::class.java) } returns dashboardRoot
        every { dashboardRoot.get<Any>(any<String>()) } returns dashboardPath
        every { criteriaBuilder.equal(any(), any<Any>()) } returns predicate
        every { criteriaBuilder.and(*anyVararg<Predicate>()) } returns predicate
        every { criteriaBuilder.asc(any()) } returns dashboardOrder
        every { entityManager.createQuery(dashboardCriteriaQuery) } returns dashboardTypedQuery
    }

    afterEach {
        checkUnnecessaryStub()
        clearAllMocks()
    }

    Context("Creating a zoekopdracht") {
        Given("A zoekopdracht that already exists") {
            val zoekopdracht = createZoekopdracht()
            val mergedZoekopdracht = createZoekopdracht(
                id = zoekopdracht.id,
                name = "mergedZoekopdracht"
            )

            every { entityManager.merge(zoekopdracht) } returns mergedZoekopdracht

            When("the create zoekopdracht function is called") {
                val result = gebruikersvoorkeurenService.createZoekopdracht(zoekopdracht)

                Then("it should merge the zoekopdracht and return the uprated zoekopdracht") {
                    result shouldBe mergedZoekopdracht
                    verify { entityManager.merge(zoekopdracht) }
                }
            }
        }

        Given("A new zoekopdracht with no ID that does not yet exist") {
            val zoekopdracht = createZoekopdracht(id = null)

            setupZoekopdrachtCriteriaChain()
            every { criteriaBuilder.lower(any()) } returns lowerExpression
            every { zoekopdrachtTypedQuery.resultList } returnsMany listOf(
                emptyList(),
                emptyList()
            )
            every { entityManager.persist(zoekopdracht) } just Runs

            When("the create zoekopdracht function is called") {
                val result = gebruikersvoorkeurenService.createZoekopdracht(zoekopdracht)

                Then("it should persist the zoekopdracht, mark it as active, and return it") {
                    result shouldBe zoekopdracht
                    zoekopdracht.isActief shouldBe true
                    verify { entityManager.persist(zoekopdracht) }
                }
            }
        }

        Given("A new zoekopdracht with no ID and a duplicate already exists") {
            val zoekopdracht = createZoekopdracht(id = null)

            setupZoekopdrachtCriteriaChain()
            every { criteriaBuilder.lower(any()) } returns lowerExpression
            every { zoekopdrachtTypedQuery.resultList } returns listOf(createZoekopdracht())

            When("the create zoekopdracht function is called") {
                Then("it should throw a RuntimeException") {
                    shouldThrow<RuntimeException> {
                        gebruikersvoorkeurenService.createZoekopdracht(zoekopdracht)
                    }
                }
            }
        }
    }

    Context("Finding a zoekopdracht by ID") {
        Given("A zoekopdracht exists with the given ID") {
            val zoekopdracht = createZoekopdracht()

            every { entityManager.find(Zoekopdracht::class.java, zoekopdracht.id) } returns zoekopdracht

            When("findZoekopdracht is called with that ID") {
                val result = gebruikersvoorkeurenService.findZoekopdracht(zoekopdracht.id!!)

                Then("it should return an Optional containing the zoekopdracht") {
                    result.shouldBePresent { it shouldBe zoekopdracht }
                }
            }
        }

        Given("No zoekopdracht exists with the given ID") {
            val id = 999L

            every { entityManager.find(Zoekopdracht::class.java, id) } returns null

            When("findZoekopdracht is called with that ID") {
                val result = gebruikersvoorkeurenService.findZoekopdracht(id)

                Then("it should return an empty Optional") {
                    result.shouldBeEmpty()
                }
            }
        }
    }

    Context("Deleting a zoekopdracht") {
        Given("A zoekopdracht exists with the given ID") {
            val zoekopdracht = createZoekopdracht()

            every { entityManager.find(Zoekopdracht::class.java, zoekopdracht.id) } returns zoekopdracht
            every { entityManager.remove(zoekopdracht) } just Runs

            When("deleteZoekopdracht is called with that ID") {
                gebruikersvoorkeurenService.deleteZoekopdracht(zoekopdracht.id)

                Then("it should remove the zoekopdracht") {
                    verify(exactly = 1) { entityManager.remove(zoekopdracht) }
                }
            }
        }

        Given("No zoekopdracht exists with the given ID") {
            val id = 999L

            every { entityManager.find(Zoekopdracht::class.java, id) } returns null

            When("deleteZoekopdracht is called with that ID") {
                gebruikersvoorkeurenService.deleteZoekopdracht(id)

                Then("it should not attempt to remove anything") {
                    verify(exactly = 0) { entityManager.remove(any()) }
                }
            }
        }
    }

    Context("Listing zoekopdrachten") {
        Given("Zoekopdrachten exist for the given parameters") {
            val zoekopdracht1 = createZoekopdracht(id = 1L)
            val zoekopdracht2 = createZoekopdracht(id = 2L)
            val params = ZoekopdrachtListParameters(Werklijst.MIJN_ZAKEN, "testMedewerker")

            setupZoekopdrachtCriteriaChain()
            every { zoekopdrachtTypedQuery.resultList } returns listOf(zoekopdracht1, zoekopdracht2)

            When("listZoekopdrachten is called") {
                val result = gebruikersvoorkeurenService.listZoekopdrachten(params)

                Then("it should return all matching zoekopdrachten") {
                    result shouldBe listOf(zoekopdracht1, zoekopdracht2)
                }
            }
        }
    }

    Context("Setting the active zoekopdracht") {
        Given("A list of zoekopdrachten where one is currently active") {
            val targetZoekopdracht = createZoekopdracht(id = 2L, actief = false)
            val activeZoekopdracht = createZoekopdracht(id = 1L, actief = true)

            setupZoekopdrachtCriteriaChain()
            every { zoekopdrachtTypedQuery.resultList } returns listOf(activeZoekopdracht, targetZoekopdracht)
            every { entityManager.merge(activeZoekopdracht) } returns activeZoekopdracht
            every { entityManager.merge(targetZoekopdracht) } returns targetZoekopdracht

            When("setActief is called for the target zoekopdracht") {
                gebruikersvoorkeurenService.setActief(targetZoekopdracht)

                Then("target becomes active, previously active is deactivated, and both are merged") {
                    targetZoekopdracht.isActief shouldBe true
                    activeZoekopdracht.isActief shouldBe false
                    verify { entityManager.merge(targetZoekopdracht) }
                    verify { entityManager.merge(activeZoekopdracht) }
                }
            }
        }
    }

    Context("Removing the active zoekopdracht") {
        Given("A list with one active and one inactive zoekopdracht") {
            val activeZoekopdracht = createZoekopdracht(id = 1L, actief = true)
            val inactiveZoekopdracht = createZoekopdracht(id = 2L, actief = false)
            val params = ZoekopdrachtListParameters(Werklijst.MIJN_ZAKEN, "testMedewerker")

            setupZoekopdrachtCriteriaChain()
            every { zoekopdrachtTypedQuery.resultList } returns listOf(activeZoekopdracht, inactiveZoekopdracht)
            every { entityManager.merge(activeZoekopdracht) } returns activeZoekopdracht

            When("removeActief is called") {
                gebruikersvoorkeurenService.removeActief(params)

                Then("the active zoekopdracht is deactivated and merged; the inactive one is not touched") {
                    activeZoekopdracht.isActief shouldBe false
                    verify(exactly = 1) { entityManager.merge(activeZoekopdracht) }
                    verify(exactly = 0) { entityManager.merge(inactiveZoekopdracht) }
                }
            }
        }
    }

    Context("Reading tabel instellingen") {
        Given("Tabel instellingen exist for the given werklijst and medewerker") {
            val tabelInstelling = createTabelInstellingen(aantalPerPagina = 50)

            setupTabelCriteriaChain()
            every { tabelTypedQuery.resultList } returns listOf(tabelInstelling)

            When("readTabelInstellingen is called") {
                val result = gebruikersvoorkeurenService.readTabelInstellingen(Werklijst.MIJN_ZAKEN, "testMedewerker")

                Then("it should return the existing tabel instellingen") {
                    result shouldBe tabelInstelling
                    result.aantalPerPagina shouldBe 50
                }
            }
        }

        Given("No tabel instellingen exist for the given werklijst and medewerker") {
            setupTabelCriteriaChain()
            every { tabelTypedQuery.resultList } returns emptyList()

            When("readTabelInstellingen is called") {
                val result = gebruikersvoorkeurenService.readTabelInstellingen(Werklijst.MIJN_ZAKEN, "testMedewerker")

                Then("it should return default tabel instellingen") {
                    result.lijstID shouldBe Werklijst.MIJN_ZAKEN
                    result.medewerkerID shouldBe "testMedewerker"
                    result.aantalPerPagina shouldBe TabelInstellingen.AANTAL_PER_PAGINA_DEFAULT
                }
            }
        }
    }

    Context("Updating tabel instellingen") {
        Given("Existing tabel instellingen for a medewerker") {
            val existingInstelling = createTabelInstellingen(aantalPerPagina = 25)
            val updatedInstelling = createTabelInstellingen(aantalPerPagina = 50)

            setupTabelCriteriaChain()
            every { tabelTypedQuery.resultList } returns listOf(existingInstelling)
            every { entityManager.merge(existingInstelling) } returns existingInstelling

            When("updateTabelInstellingen is called with new page size") {
                gebruikersvoorkeurenService.updateTabelInstellingen(updatedInstelling)

                Then("the existing instelling is updated and merged") {
                    existingInstelling.aantalPerPagina shouldBe 50
                    verify { entityManager.merge(existingInstelling) }
                }
            }
        }
    }

    Context("Listing dashboard cards") {
        Given("Dashboard cards exist for a medewerker") {
            val card1 = createDashboardCardInstelling(id = 1L, cardId = DashboardCardId.MIJN_TAKEN)
            val card2 = createDashboardCardInstelling(id = 2L, cardId = DashboardCardId.MIJN_ZAKEN)

            setupDashboardCriteriaChain()
            every { dashboardTypedQuery.resultList } returns listOf(card1, card2)

            When("listDashboardCards is called") {
                val result = gebruikersvoorkeurenService.listDashboardCards("testMedewerker")

                Then("it should return all dashboard cards for that medewerker") {
                    result shouldBe listOf(card1, card2)
                }
            }
        }

        Given("No dashboard cards exist for a medewerker") {
            setupDashboardCriteriaChain()
            every { dashboardTypedQuery.resultList } returns emptyList()

            When("listDashboardCards is called") {
                val result = gebruikersvoorkeurenService.listDashboardCards("testMedewerker")

                Then("it should return an empty list") {
                    result.shouldBeEmpty()
                }
            }
        }
    }

    Context("Adding a dashboard card") {
        Given("A card without signalering type and no existing ID") {
            val card = createDashboardCardInstelling(id = null)

            every { entityManager.persist(card) } just Runs

            When("addDashboardCard is called") {
                gebruikersvoorkeurenService.addDashboardCard("testMedewerker", card)

                Then("it should persist the card with the medewerker ID set") {
                    card.medewerkerId shouldBe "testMedewerker"
                    verify(exactly = 1) { entityManager.persist(card) }
                }
            }
        }

        Given("A card with a signalering type and no existing ID") {
            val card = createDashboardCardInstelling(id = null).apply {
                signaleringType = SignaleringType.Type.ZAAK_OP_NAAM
            }
            val instellingen = mockk<SignaleringInstellingen>(relaxed = true)

            every {
                signaleringService.readInstellingenUser(SignaleringType.Type.ZAAK_OP_NAAM, "testMedewerker")
            } returns instellingen
            every { signaleringService.createUpdateOrDeleteInstellingen(instellingen) } returns null
            every { entityManager.persist(card) } just Runs

            When("addDashboardCard is called") {
                gebruikersvoorkeurenService.addDashboardCard("testMedewerker", card)

                Then("it should update the signalering to show on dashboard and persist the card") {
                    verify { instellingen.isDashboard = true }
                    verify { signaleringService.createUpdateOrDeleteInstellingen(instellingen) }
                    verify(exactly = 1) { entityManager.persist(card) }
                }
            }
        }

        Given("A card that already has an ID") {
            val card = createDashboardCardInstelling(id = 42L)

            When("addDashboardCard is called") {
                gebruikersvoorkeurenService.addDashboardCard("testMedewerker", card)

                Then("it should not persist the card again") {
                    verify(exactly = 0) { entityManager.persist(any()) }
                }
            }
        }
    }

    Context("Deleting a dashboard card") {
        Given("A card without signalering type") {
            val card = createDashboardCardInstelling(id = 10L)

            every {
                entityManager.find(DashboardCardInstelling::class.java, card.id)
            } returns card
            every { entityManager.remove(card) } just Runs

            When("deleteDashboardCard is called") {
                gebruikersvoorkeurenService.deleteDashboardCard("testMedewerker", card)

                Then("it should remove the card") {
                    verify(exactly = 1) { entityManager.remove(card) }
                }
            }
        }

        Given("A card with a signalering type") {
            val card = createDashboardCardInstelling(id = 10L).apply {
                signaleringType = SignaleringType.Type.TAAK_OP_NAAM
            }
            val instellingen = mockk<SignaleringInstellingen>(relaxed = true)

            every {
                signaleringService.readInstellingenUser(SignaleringType.Type.TAAK_OP_NAAM, "testMedewerker")
            } returns instellingen
            every { signaleringService.createUpdateOrDeleteInstellingen(instellingen) } returns null
            every {
                entityManager.find(DashboardCardInstelling::class.java, card.id)
            } returns card
            every { entityManager.remove(card) } just Runs

            When("deleteDashboardCard is called") {
                gebruikersvoorkeurenService.deleteDashboardCard("testMedewerker", card)

                Then("it should disable dashboard on signalering and remove the card") {
                    verify { instellingen.isDashboard = false }
                    verify { signaleringService.createUpdateOrDeleteInstellingen(instellingen) }
                    verify(exactly = 1) { entityManager.remove(card) }
                }
            }
        }

        Given("A card with no ID") {
            val card = createDashboardCardInstelling(id = null)

            When("deleteDashboardCard is called") {
                gebruikersvoorkeurenService.deleteDashboardCard("testMedewerker", card)

                Then("it should not attempt to remove anything") {
                    verify(exactly = 0) { entityManager.remove(any()) }
                }
            }
        }
    }

    Context("Updating dashboard cards") {
        Given("Existing cards where one is updated, one is removed, and one is added") {
            val existingCard1 = createDashboardCardInstelling(id = 1L, cardId = DashboardCardId.MIJN_TAKEN, kolom = 0)
            val existingCard2 = createDashboardCardInstelling(id = 2L, cardId = DashboardCardId.MIJN_ZAKEN, kolom = 0)
            val updatedCard1 = createDashboardCardInstelling(id = 1L, cardId = DashboardCardId.MIJN_TAKEN, kolom = 2)
            val newCard = createDashboardCardInstelling(id = null, cardId = DashboardCardId.MIJN_TAKEN_NIEUW)

            setupDashboardCriteriaChain()
            every { dashboardTypedQuery.resultList } returns listOf(existingCard1, existingCard2)
            every { entityManager.persist(existingCard1) } just Runs
            every { entityManager.remove(existingCard2) } just Runs
            every { entityManager.persist(newCard) } just Runs

            When("updateDashboardCards is called with the new card list") {
                gebruikersvoorkeurenService.updateDashboardCards(
                    "testMedewerker",
                    listOf(updatedCard1, newCard)
                )

                Then("existing card is updated, missing card is removed, and new card is persisted") {
                    existingCard1.kolom shouldBe 2
                    verify(exactly = 1) { entityManager.persist(existingCard1) }
                    verify(exactly = 1) { entityManager.remove(existingCard2) }
                    verify(exactly = 1) { entityManager.persist(newCard) }
                }
            }
        }
    }
})
