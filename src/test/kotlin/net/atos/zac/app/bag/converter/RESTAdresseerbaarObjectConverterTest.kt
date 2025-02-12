/*
 * SPDX-FileCopyrightText: 2025 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */

package net.atos.zac.app.bag.converter

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.checkUnnecessaryStub
import net.atos.client.bag.model.generated.StatusPlaats
import net.atos.client.bag.model.generated.StatusVerblijfsobject
import net.atos.client.bag.model.generated.TypeAdresseerbaarObject
import net.atos.zac.app.bag.createLigplaatsAdresseerbaarObject
import net.atos.zac.app.bag.createStandplaatsAdresseerbaarObject
import net.atos.zac.app.bag.createVerblijfsAdresseerbaarObject

class RESTAdresseerbaarObjectConverterTest : BehaviorSpec({
    beforeEach {
        checkUnnecessaryStub()
    }

    Given("Ligplaats addressbaar object") {
        val adresseerbaarObjectIOHal = createLigplaatsAdresseerbaarObject(StatusPlaats.PLAATS_AANGEWEZEN)

        When("converted to rest representation") {
            val result = RestAdresseerbaarObjectConverter.convertToREST(adresseerbaarObjectIOHal)

            Then("it should return the correct data") {
                with(result) {
                    typeAdresseerbaarObject shouldBe TypeAdresseerbaarObject.LIGPLAATS
                    status shouldBe "Plaats aangewezen"
                    vboDoel shouldBe null
                    vboOppervlakte shouldBe 0
                    with(geometry) {
                        type shouldBe "Polygon"
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

    Given("Standplaats adresseerbaar object") {
        val adresseerbaarObjectIOHal = createStandplaatsAdresseerbaarObject(StatusPlaats.PLAATS_AANGEWEZEN)

        When("converted to rest representation") {
            val result = RestAdresseerbaarObjectConverter.convertToREST(adresseerbaarObjectIOHal)

            Then("it should return the correct data") {
                with(result) {
                    typeAdresseerbaarObject shouldBe TypeAdresseerbaarObject.STANDPLAATS
                    status shouldBe "Plaats aangewezen"
                    vboDoel shouldBe null
                    vboOppervlakte shouldBe 0
                    with(geometry) {
                        type shouldBe "Polygon"
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

    Given("Verblijfs addressbaar object") {
        val adresseerbaarObjectIOHal = createVerblijfsAdresseerbaarObject(StatusVerblijfsobject.VERBLIJFSOBJECT_GEVORMD)

        When("converted to rest representation") {
            val result = RestAdresseerbaarObjectConverter.convertToREST(adresseerbaarObjectIOHal)

            Then("it should return the correct data") {
                with(result) {
                    typeAdresseerbaarObject shouldBe TypeAdresseerbaarObject.VERBLIJFSOBJECT
                    status shouldBe "Verblijfsobject gevormd"
                    vboDoel shouldBe ""
                    vboOppervlakte shouldBe 0
                    with(geometry) {
                        type shouldBe "Point"
                        polygon shouldBe null
                        point!!.latitude shouldBe 0.0
                        point!!.longitude shouldBe 0.0
                        geometrycollection shouldBe null
                    }
                }
            }
        }
    }
})
