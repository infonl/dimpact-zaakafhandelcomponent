/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.app.search.converter

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.checkUnnecessaryStub
import io.mockk.every
import io.mockk.mockk
import jakarta.enterprise.inject.Instance
import nl.info.zac.app.search.model.createRestZoekParameters
import nl.info.zac.authentication.LoggedInUser
import nl.info.zac.authentication.createLoggedInUser
import nl.info.zac.search.model.FilterVeld
import nl.info.zac.search.model.zoekobject.ZoekObjectType

class RestZoekParametersConverterTest : BehaviorSpec({
    val loggedInUserInstance = mockk<Instance<LoggedInUser>>()
    val restZoekParametersConverter = RestZoekParametersConverter(loggedInUserInstance)

    afterEach {
        checkUnnecessaryStub()
    }

    context("convert") {
        given("a basic RestZoekParameters for ZAAK with page and rows") {
            val params = createRestZoekParameters(
                type = ZoekObjectType.ZAAK,
                rows = 10,
                page = 2,
                sorteerRichting = "asc",
                zoeken = emptyMap(),
                filters = emptyMap(),
                datums = emptyMap(),
                alleenOpenstaandeZaken = false,
                alleenAfgeslotenZaken = false,
                alleenMijnZaken = false,
                alleenMijnTaken = false
            )

            `when`("convert is called") {
                val result = restZoekParametersConverter.convert(params)

                then("start is page * rows") {
                    result.start shouldBe 20
                }

                then("rows is set correctly") {
                    result.rows shouldBe 10
                }

                then("zoekObjectType is ZAAK") {
                    result.type shouldBe ZoekObjectType.ZAAK
                }
            }
        }

        given("a RestZoekParameters with alleenOpenstaandeZaken=true") {
            val params = createRestZoekParameters(
                sorteerRichting = "asc",
                alleenOpenstaandeZaken = true,
                alleenAfgeslotenZaken = false,
                alleenMijnZaken = false,
                alleenMijnTaken = false,
                zoeken = emptyMap(),
                filters = emptyMap(),
                datums = emptyMap()
            )

            `when`("convert is called") {
                val result = restZoekParametersConverter.convert(params)

                then("a filter query for AFGEHANDELD is added with value 'false'") {
                    result.getFilterQueries()["zaak_afgehandeld"] shouldBe "false"
                }
            }
        }

        given("a RestZoekParameters with alleenAfgeslotenZaken=true") {
            val params = createRestZoekParameters(
                sorteerRichting = "asc",
                alleenAfgeslotenZaken = true,
                alleenOpenstaandeZaken = false,
                alleenMijnZaken = false,
                alleenMijnTaken = false,
                zoeken = emptyMap(),
                filters = emptyMap(),
                datums = emptyMap()
            )

            `when`("convert is called") {
                val result = restZoekParametersConverter.convert(params)

                then("a filter query for eindstatus is added") {
                    result.getFilterQueries()["zaak_statusEindstatus"] shouldBe "true"
                }
            }
        }

        given("a RestZoekParameters with alleenMijnZaken=true") {
            val loggedInUser = createLoggedInUser(id = "fakeUserId")
            every { loggedInUserInstance.get() } returns loggedInUser
            val params = createRestZoekParameters(
                sorteerRichting = "asc",
                alleenMijnZaken = true,
                alleenMijnTaken = false,
                alleenOpenstaandeZaken = false,
                alleenAfgeslotenZaken = false,
                zoeken = emptyMap(),
                filters = emptyMap(),
                datums = emptyMap()
            )

            `when`("convert is called") {
                val result = restZoekParametersConverter.convert(params)

                then("a filter query for behandelaarId is added with logged-in user id") {
                    result.getFilterQueries()["zaak_behandelaarGebruikersnaam"] shouldBe "fakeUserId"
                }
            }
        }

        given("a RestZoekParameters with filters") {
            val params = createRestZoekParameters(
                sorteerRichting = "asc",
                zoeken = emptyMap(),
                datums = emptyMap(),
                alleenOpenstaandeZaken = false,
                alleenAfgeslotenZaken = false,
                alleenMijnZaken = false,
                alleenMijnTaken = false
            )

            `when`("convert is called") {
                val result = restZoekParametersConverter.convert(params)

                then("filter velden from the params are present") {
                    result.getFilters().containsKey(FilterVeld.BEHANDELAAR) shouldBe true
                }
            }
        }
    }
})
