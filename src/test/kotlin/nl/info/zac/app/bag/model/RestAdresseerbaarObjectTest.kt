/*
 * SPDX-FileCopyrightText: 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

package nl.info.zac.app.bag.model

import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.checkUnnecessaryStub
import nl.info.zac.app.bag.createLigplaatsAdresseerbaarObject
import nl.info.zac.app.bag.createStandplaatsAdresseerbaarObject
import nl.info.zac.app.bag.createVerblijfsAdresseerbaarObject
import nl.info.client.bag.model.generated.StatusPlaats
import nl.info.client.bag.model.generated.StatusVerblijfsobject
import nl.info.client.bag.model.generated.TypeAdresseerbaarObject
import nl.info.zac.app.zaak.model.RestGeometryType

class RestAdresseerbaarObjectTest : BehaviorSpec({
    afterEach {
        checkUnnecessaryStub()
    }

    given("Ligplaats addressbaar object") {
        val adresseerbaarObjectIOHal = createLigplaatsAdresseerbaarObject(StatusPlaats.PLAATS_AANGEWEZEN)

        `when`("converted to rest representation") {
            val result = adresseerbaarObjectIOHal.toRestAdresseerbaarObject()

            then("it should return the correct data") {
                with(result) {
                    typeAdresseerbaarObject shouldBe TypeAdresseerbaarObject.LIGPLAATS
                    status shouldBe "Plaats aangewezen"
                    vboDoel shouldBe null
                    vboOppervlakte shouldBe 0
                    with(geometry.shouldNotBeNull()) {
                        type shouldBe RestGeometryType.POLYGON
                        point shouldBe null
                        with(polygon!!) {
                            size shouldBe 1
                            get(0)[0].latitude shouldBe 0.0
                            get(0)[0].longitude shouldBe 0.0
                        }
                        geometrycollection shouldBe null
                    }
                }
            }
        }
    }

    given("Standplaats adresseerbaar object") {
        val adresseerbaarObjectIOHal = createStandplaatsAdresseerbaarObject(StatusPlaats.PLAATS_AANGEWEZEN)

        `when`("converted to rest representation") {
            val result = adresseerbaarObjectIOHal.toRestAdresseerbaarObject()

            then("it should return the correct data") {
                with(result) {
                    typeAdresseerbaarObject shouldBe TypeAdresseerbaarObject.STANDPLAATS
                    status shouldBe "Plaats aangewezen"
                    vboDoel shouldBe null
                    vboOppervlakte shouldBe 0
                    with(geometry.shouldNotBeNull()) {
                        type shouldBe RestGeometryType.POLYGON
                        point shouldBe null
                        with(polygon!!) {
                            size shouldBe 1
                            get(0)[0].latitude shouldBe 0.0
                            get(0)[0].longitude shouldBe 0.0
                        }
                        geometrycollection shouldBe null
                    }
                }
            }
        }
    }

    given("Verblijfs addressbaar object") {
        val adresseerbaarObjectIOHal = createVerblijfsAdresseerbaarObject(StatusVerblijfsobject.VERBLIJFSOBJECT_GEVORMD)

        `when`("converted to rest representation") {
            val result = adresseerbaarObjectIOHal.toRestAdresseerbaarObject()

            then("it should return the correct data") {
                with(result) {
                    typeAdresseerbaarObject shouldBe TypeAdresseerbaarObject.VERBLIJFSOBJECT
                    status shouldBe "Verblijfsobject gevormd"
                    vboDoel shouldBe ""
                    vboOppervlakte shouldBe 0
                    with(geometry.shouldNotBeNull()) {
                        type shouldBe RestGeometryType.POINT
                        polygon shouldBe null
                        point!!.latitude shouldBe 0.0
                        point!!.longitude shouldBe 0.0
                        geometrycollection shouldBe null
                    }
                }
            }
        }
    }

    context("The description of a REST adresseerbaar object") {
        given("An adresseerbaar object with a type and an identificatie") {
            val restAdresseerbaarObject = RestAdresseerbaarObject().apply {
                typeAdresseerbaarObject = TypeAdresseerbaarObject.LIGPLAATS
                identificatie = "fakeIdentificatie"
            }

            `when`("the description is requested") {
                val omschrijving = restAdresseerbaarObject.omschrijving

                then("it contains the type and the identificatie") {
                    omschrijving shouldBe "${TypeAdresseerbaarObject.LIGPLAATS} fakeIdentificatie"
                }
            }
        }

        given("An adresseerbaar object without a type") {
            val restAdresseerbaarObject = RestAdresseerbaarObject().apply { identificatie = "fakeIdentificatie" }

            `when`("the description is requested") {
                val omschrijving = restAdresseerbaarObject.omschrijving

                then("it contains only the identificatie, without 'null'") {
                    omschrijving shouldBe "fakeIdentificatie"
                }
            }
        }
    }
})
