/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.app.zaak.model

import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import nl.info.client.bag.model.generated.PuntOfVlak
import nl.info.zac.app.bag.createPuntOfVlak
import nl.info.zac.app.bag.createSurface
import java.math.BigDecimal

class RestGeometryTest : BehaviorSpec({
    context("Converting a BAG punt or vlak to a REST geometry") {
        given("A punt") {
            val puntOfVlak = createPuntOfVlak().apply {
                punt.coordinates = listOf(BigDecimal("5.1"), BigDecimal("52.2"))
            }

            `when`("it is converted") {
                val restGeometry = puntOfVlak.toRestGeometry()

                then("a point geometry with longitude and latitude is returned") {
                    with(restGeometry.shouldNotBeNull()) {
                        type shouldBe RestGeometryType.POINT
                        point!!.longitude shouldBe 5.1
                        point!!.latitude shouldBe 52.2
                        polygon shouldBe null
                    }
                }
            }
        }

        given("A vlak") {
            val puntOfVlak = PuntOfVlak().apply { vlak = createSurface() }

            `when`("it is converted") {
                val restGeometry = puntOfVlak.toRestGeometry()

                then("a polygon geometry is returned") {
                    with(restGeometry.shouldNotBeNull()) {
                        type shouldBe RestGeometryType.POLYGON
                        point shouldBe null
                        polygon!!.size shouldBe 1
                    }
                }
            }
        }

        given("Neither a punt nor a vlak") {
            `when`("it is converted") {
                val restGeometry = PuntOfVlak().toRestGeometry()

                then("no geometry is returned") {
                    restGeometry.shouldBeNull()
                }
            }
        }
    }
})
