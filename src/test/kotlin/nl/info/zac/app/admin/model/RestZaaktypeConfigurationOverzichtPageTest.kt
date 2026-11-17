/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.app.admin.model

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import nl.info.zac.app.admin.model.RestZaaktypeConfigurationSortField.BEGIN_GELDIGHEID
import nl.info.zac.app.admin.model.RestZaaktypeConfigurationSortField.EINDE_GELDIGHEID
import nl.info.zac.app.admin.model.RestZaaktypeConfigurationSortField.OMSCHRIJVING
import nl.info.zac.app.admin.model.RestZaaktypeConfigurationSortField.PROCESS_DEFINITION
import nl.info.zac.app.admin.model.RestZaaktypeConfigurationSortField.VALIDE
import nl.info.zac.app.search.model.RestDatumRange
import nl.info.zac.search.model.SorteerRichting
import java.time.LocalDate

class RestZaaktypeConfigurationOverzichtPageTest : BehaviorSpec({
    @Suppress("LongParameterList")
    fun createOverzicht(
        omschrijving: String,
        identificatie: String = "fakeIdentificatie$omschrijving",
        processDefinition: RestProcessDefinitionOverzicht? = null,
        valide: Boolean = true,
        nuGeldig: Boolean = true,
        beginGeldigheid: LocalDate = LocalDate.of(2026, 1, 1),
        eindeGeldigheid: LocalDate? = null
    ) = createRestZaaktypeConfigurationOverzicht(
        restZaaktypeBeheerOverzicht = createRestZaaktypeBeheerOverzicht(
            identificatie = identificatie,
            omschrijving = omschrijving,
            nuGeldig = nuGeldig,
            beginGeldigheid = beginGeldigheid,
            eindeGeldigheid = eindeGeldigheid
        ),
        restProcessDefinitionOverzicht = processDefinition,
        valide = valide
    )

    fun List<RestZaaktypeConfigurationOverzicht>.omschrijvingen() = map { it.zaaktype.omschrijving }

    context("Paging the zaaktype configurations") {
        given("60 zaaktype configurations in the order of the catalogus") {
            val restZaaktypeConfigurationOverzichten = (1..60).map { createOverzicht(omschrijving = "fakeZaaktype$it") }

            `when`("the third page of 25 rows is requested without a sort field") {
                val restZaaktypeConfigurationOverzichtResultaat = restZaaktypeConfigurationOverzichten
                    .toRestZaaktypeConfigurationOverzichtResultaat(
                        RestZaaktypeConfigurationListParameters(page = 2, rows = 25)
                    )

                then("the last 10 configurations are returned, in the order of the catalogus") {
                    restZaaktypeConfigurationOverzichtResultaat.results.toList().omschrijvingen() shouldBe
                        (51..60).map { "fakeZaaktype$it" }
                }
                and("the total is the number of all configurations") {
                    restZaaktypeConfigurationOverzichtResultaat.resultCount shouldBe 60
                }
            }

            `when`("a page after the last page is requested") {
                val restZaaktypeConfigurationOverzichtResultaat = restZaaktypeConfigurationOverzichten
                    .toRestZaaktypeConfigurationOverzichtResultaat(
                        RestZaaktypeConfigurationListParameters(page = Int.MAX_VALUE, rows = 25)
                    )

                then("no rows are returned, and the total is still the number of all configurations") {
                    restZaaktypeConfigurationOverzichtResultaat.results.shouldBeEmpty()
                    restZaaktypeConfigurationOverzichtResultaat.resultCount shouldBe 60
                }
            }
        }
    }

    context("Filtering the zaaktype configurations") {
        val processDefinition = RestProcessDefinitionOverzicht(key = "fakeKey", naam = "fakeNaam")
        val validConfiguration = createOverzicht(omschrijving = "fakeValide", processDefinition = processDefinition)
        val invalidConfiguration = createOverzicht(omschrijving = "fakeNietValide", valide = false)
        val notValidNowConfiguration = createOverzicht(
            omschrijving = "fakeNietGeldig",
            nuGeldig = false,
            beginGeldigheid = LocalDate.of(2025, 1, 1),
            eindeGeldigheid = LocalDate.of(2025, 12, 31)
        )
        val restZaaktypeConfigurationOverzichten = listOf(
            validConfiguration,
            invalidConfiguration,
            notValidNowConfiguration
        )

        given("a filter on valide, on geldig, on the zaaktype and on the process definition") {
            `when`("the configurations are filtered on valide false") {
                val restZaaktypeConfigurationOverzichtResultaat = restZaaktypeConfigurationOverzichten
                    .toRestZaaktypeConfigurationOverzichtResultaat(
                        RestZaaktypeConfigurationListParameters(isValide = false)
                    )

                then("only the invalid configuration is returned, and the total counts only that one") {
                    restZaaktypeConfigurationOverzichtResultaat.results shouldContainExactly
                        listOf(invalidConfiguration)
                    restZaaktypeConfigurationOverzichtResultaat.resultCount shouldBe 1
                }
            }

            `when`("the configurations are filtered on geldig true and on the process definition") {
                val restZaaktypeConfigurationOverzichtResultaat = restZaaktypeConfigurationOverzichten
                    .toRestZaaktypeConfigurationOverzichtResultaat(
                        RestZaaktypeConfigurationListParameters(isNuGeldig = true, processDefinitionKey = "fakeKey")
                    )

                then("only the configuration that is valid today and bound to that process definition is returned") {
                    restZaaktypeConfigurationOverzichtResultaat.results shouldContainExactly
                        listOf(validConfiguration)
                }
            }

            `when`("the configurations are filtered on the identificatie of a zaaktype") {
                val restZaaktypeConfigurationOverzichtResultaat = restZaaktypeConfigurationOverzichten
                    .toRestZaaktypeConfigurationOverzichtResultaat(
                        RestZaaktypeConfigurationListParameters(zaaktypeIdentificatie = "fakeIdentificatiefakeNietGeldig")
                    )

                then("only the configuration of that zaaktype is returned") {
                    restZaaktypeConfigurationOverzichtResultaat.results shouldContainExactly
                        listOf(notValidNowConfiguration)
                }
            }
        }

        given("a filter on a range of the begin and of the einde geldigheid") {
            `when`("the configurations are filtered on a begin geldigheid in 2025, both ends included") {
                val restZaaktypeConfigurationOverzichtResultaat = restZaaktypeConfigurationOverzichten
                    .toRestZaaktypeConfigurationOverzichtResultaat(
                        RestZaaktypeConfigurationListParameters(
                            beginGeldigheid = RestDatumRange(
                                van = LocalDate.of(2025, 1, 1),
                                tot = LocalDate.of(2025, 12, 31)
                            )
                        )
                    )

                then("only the configuration whose zaaktype began in 2025 is returned") {
                    restZaaktypeConfigurationOverzichtResultaat.results shouldContainExactly
                        listOf(notValidNowConfiguration)
                }
            }

            `when`("the configurations are filtered on an einde geldigheid from a date") {
                val restZaaktypeConfigurationOverzichtResultaat = restZaaktypeConfigurationOverzichten
                    .toRestZaaktypeConfigurationOverzichtResultaat(
                        RestZaaktypeConfigurationListParameters(
                            eindeGeldigheid = RestDatumRange(van = LocalDate.of(2020, 1, 1))
                        )
                    )

                then("the configurations whose zaaktype has no einde geldigheid are not returned") {
                    restZaaktypeConfigurationOverzichtResultaat.results shouldContainExactly
                        listOf(notValidNowConfiguration)
                }
            }

            `when`("the configurations are filtered on an empty range") {
                val restZaaktypeConfigurationOverzichtResultaat = restZaaktypeConfigurationOverzichten
                    .toRestZaaktypeConfigurationOverzichtResultaat(
                        RestZaaktypeConfigurationListParameters(eindeGeldigheid = RestDatumRange())
                    )

                then("all configurations are returned") {
                    restZaaktypeConfigurationOverzichtResultaat.results shouldContainExactly
                        restZaaktypeConfigurationOverzichten
                }
            }
        }

        given("a filter that matches none of the configurations") {
            `when`("the configurations are filtered") {
                val restZaaktypeConfigurationOverzichtResultaat = restZaaktypeConfigurationOverzichten
                    .toRestZaaktypeConfigurationOverzichtResultaat(
                        RestZaaktypeConfigurationListParameters(processDefinitionKey = "fakeUnknownKey")
                    )

                then("the zaaktypes and process definitions to filter on are still those of all configurations") {
                    restZaaktypeConfigurationOverzichtResultaat.results.shouldBeEmpty()
                    restZaaktypeConfigurationOverzichtResultaat.filterZaaktypes.map { it.omschrijving } shouldBe
                        listOf("fakeNietGeldig", "fakeNietValide", "fakeValide")
                    restZaaktypeConfigurationOverzichtResultaat.filterProcessDefinitions shouldBe
                        listOf(processDefinition)
                }
            }
        }
    }

    context("Filter options of the zaaktype configurations") {
        given("two versions of one zaaktype and two zaaktypes bound to the same process definition") {
            val processDefinition = RestProcessDefinitionOverzicht(key = "fakeKey", naam = "fakeNaam")
            val restZaaktypeConfigurationOverzichten = listOf(
                createOverzicht(omschrijving = "fakeVersie1", identificatie = "fakeIdentificatie"),
                createOverzicht(omschrijving = "fakeVersie2", identificatie = "fakeIdentificatie"),
                createOverzicht(omschrijving = "fakeAnder", processDefinition = processDefinition),
                createOverzicht(omschrijving = "fakeNogEen", processDefinition = processDefinition)
            )

            `when`("a page is listed") {
                val restZaaktypeConfigurationOverzichtResultaat = restZaaktypeConfigurationOverzichten
                    .toRestZaaktypeConfigurationOverzichtResultaat(RestZaaktypeConfigurationListParameters())

                then("each zaaktype is offered once, with the omschrijving of its first version, ordered by omschrijving") {
                    restZaaktypeConfigurationOverzichtResultaat.filterZaaktypes shouldBe listOf(
                        RestZaaktypeFilterOptie(identificatie = "fakeIdentificatiefakeAnder", omschrijving = "fakeAnder"),
                        RestZaaktypeFilterOptie(identificatie = "fakeIdentificatiefakeNogEen", omschrijving = "fakeNogEen"),
                        RestZaaktypeFilterOptie(identificatie = "fakeIdentificatie", omschrijving = "fakeVersie1")
                    )
                }
                and("each process definition is offered once") {
                    restZaaktypeConfigurationOverzichtResultaat.filterProcessDefinitions shouldBe
                        listOf(processDefinition)
                }
            }
        }
    }

    context("Order of the filter options") {
        given("zaaktypes and process definitions whose names differ in case and accents") {
            val restZaaktypeConfigurationOverzichten = listOf("Zorg", "aanvraag", "Échte melding", "beroep").map {
                createOverzicht(
                    omschrijving = it,
                    processDefinition = RestProcessDefinitionOverzicht(key = "fakeKey$it", naam = it)
                )
            }

            `when`("a page is listed") {
                val restZaaktypeConfigurationOverzichtResultaat = restZaaktypeConfigurationOverzichten
                    .toRestZaaktypeConfigurationOverzichtResultaat(RestZaaktypeConfigurationListParameters())

                then("the zaaktypes and process definitions are ordered alphabetically, ignoring case and accents") {
                    val alphabeticalOrder = listOf("aanvraag", "beroep", "Échte melding", "Zorg")
                    restZaaktypeConfigurationOverzichtResultaat.filterZaaktypes.map { it.omschrijving } shouldBe
                        alphabeticalOrder
                    restZaaktypeConfigurationOverzichtResultaat.filterProcessDefinitions.map { it.naam } shouldBe
                        alphabeticalOrder
                }
            }
        }
    }

    context("Sorting the zaaktype configurations") {
        val configurationB = createOverzicht(
            omschrijving = "fakeB",
            processDefinition = RestProcessDefinitionOverzicht(key = "fakeKeyB", naam = "fakeModelB"),
            beginGeldigheid = LocalDate.of(2026, 2, 1),
            eindeGeldigheid = LocalDate.of(2027, 1, 1)
        )
        val configurationA = createOverzicht(
            omschrijving = "fakeA",
            processDefinition = RestProcessDefinitionOverzicht(key = "fakeKeyA", naam = "fakeModelA"),
            valide = false,
            beginGeldigheid = LocalDate.of(2026, 3, 1)
        )
        val configurationC = createOverzicht(
            omschrijving = "fakeC",
            beginGeldigheid = LocalDate.of(2026, 1, 1),
            eindeGeldigheid = LocalDate.of(2026, 12, 31)
        )
        val restZaaktypeConfigurationOverzichten = listOf(configurationB, configurationA, configurationC)

        fun sort(sortField: RestZaaktypeConfigurationSortField, sortOrder: SorteerRichting) =
            restZaaktypeConfigurationOverzichten.toRestZaaktypeConfigurationOverzichtResultaat(
                RestZaaktypeConfigurationListParameters(sortField = sortField, sortOrder = sortOrder)
            ).results.toList().omschrijvingen()

        given("three configurations") {
            `when`("they are sorted on omschrijving, ascending and descending") {
                then("they are ordered by the omschrijving of their zaaktype") {
                    sort(OMSCHRIJVING, SorteerRichting.ASC) shouldBe listOf("fakeA", "fakeB", "fakeC")
                    sort(OMSCHRIJVING, SorteerRichting.DESC) shouldBe listOf("fakeC", "fakeB", "fakeA")
                }
            }

            `when`("they are sorted on the process definition") {
                then("a configuration without a process definition is last when ascending and first when descending") {
                    sort(PROCESS_DEFINITION, SorteerRichting.ASC) shouldBe listOf("fakeA", "fakeB", "fakeC")
                    sort(PROCESS_DEFINITION, SorteerRichting.DESC) shouldBe listOf("fakeC", "fakeB", "fakeA")
                }
            }

            `when`("they are sorted on valide, ascending") {
                then("the invalid configuration is first, and the others keep the order of the catalogus") {
                    sort(VALIDE, SorteerRichting.ASC) shouldBe listOf("fakeA", "fakeB", "fakeC")
                }
            }

            `when`("they are sorted on the begin and on the einde geldigheid, ascending") {
                then("a zaaktype without an einde geldigheid is last") {
                    sort(BEGIN_GELDIGHEID, SorteerRichting.ASC) shouldBe listOf("fakeC", "fakeB", "fakeA")
                    sort(EINDE_GELDIGHEID, SorteerRichting.ASC) shouldBe listOf("fakeC", "fakeB", "fakeA")
                }
            }
        }
    }
})
