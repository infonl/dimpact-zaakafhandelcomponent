/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.zac.app.bag.model

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import nl.info.zac.app.zaak.model.RestCoordinates
import nl.info.zac.app.zaak.model.RestGeometry
import nl.info.zac.app.zaak.model.RestGeometryType

class RestBagAdresTest : BehaviorSpec({
    val adresseerbaarObjectGeometry = RestGeometry(
        type = RestGeometryType.POINT,
        point = RestCoordinates(5.1, 52.2)
    )
    val pandGeometry = RestGeometry(
        type = RestGeometryType.POLYGON,
        polygon = listOf(listOf(RestCoordinates(5.0, 52.0)))
    )

    context("The geometry of a REST address") {
        given("An address without an adresseerbaar object and without panden") {
            val restBagAdres = RESTBAGAdres()

            `when`("the geometry is requested") {
                val geometry = restBagAdres.geometry

                then("there is no geometry") {
                    geometry.shouldBeNull()
                }
            }
        }

        given("An address with only an adresseerbaar object with a geometry") {
            val restBagAdres = RESTBAGAdres().apply {
                adresseerbaarObject = RESTAdresseerbaarObject().apply { geometry = adresseerbaarObjectGeometry }
            }

            `when`("the geometry is requested") {
                val geometry = restBagAdres.geometry

                then("the geometry of the adresseerbaar object is returned") {
                    geometry shouldBe adresseerbaarObjectGeometry
                }
            }
        }

        given("An address with only panden of which the first has a geometry") {
            val restBagAdres = RESTBAGAdres().apply {
                panden = listOf(RESTPand().apply { geometry = pandGeometry }, RESTPand())
            }

            `when`("the geometry is requested") {
                val geometry = restBagAdres.geometry

                then("the geometry of the first pand is returned") {
                    geometry shouldBe pandGeometry
                }
            }
        }

        given("An address with an adresseerbaar object and a pand that both have a geometry") {
            val restBagAdres = RESTBAGAdres().apply {
                adresseerbaarObject = RESTAdresseerbaarObject().apply { geometry = adresseerbaarObjectGeometry }
                panden = listOf(RESTPand().apply { geometry = pandGeometry })
            }

            `when`("the geometry is requested") {
                val geometry = restBagAdres.geometry

                then("a geometry collection with both geometries is returned") {
                    geometry shouldBe RestGeometry(
                        type = RestGeometryType.GEOMETRY_COLLECTION,
                        geometrycollection = listOf(adresseerbaarObjectGeometry, pandGeometry)
                    )
                }
            }
        }
    }

    context("The description of a REST address") {
        given("An address") {
            val restBagAdres = RESTBAGAdres().apply {
                openbareRuimteNaam = "fakeStraat"
                huisnummerWeergave = "12A"
                postcode = "1234AB"
                woonplaatsNaam = "fakeWoonplaats"
            }

            `when`("the description is requested") {
                val omschrijving = restBagAdres.omschrijving

                then("it contains the street, huisnummer, postcode and woonplaats") {
                    omschrijving shouldBe "fakeStraat 12A, 1234AB fakeWoonplaats"
                }
            }
        }
    }
})
