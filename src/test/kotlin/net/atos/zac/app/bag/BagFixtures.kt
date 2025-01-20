/*
 * SPDX-FileCopyrightText: 2025 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */

package net.atos.zac.app.bag

import net.atos.client.bag.model.generated.AdresseerbaarObjectIOHal
import net.atos.client.bag.model.generated.Ligplaats
import net.atos.client.bag.model.generated.LigplaatsIOHal
import net.atos.client.bag.model.generated.PointGeoJSON
import net.atos.client.bag.model.generated.PolygonGeoJSON
import net.atos.client.bag.model.generated.PuntOfVlak
import net.atos.client.bag.model.generated.Standplaats
import net.atos.client.bag.model.generated.StandplaatsIOHal
import net.atos.client.bag.model.generated.StatusPlaats
import net.atos.client.bag.model.generated.StatusVerblijfsobject
import net.atos.client.bag.model.generated.Verblijfsobject
import net.atos.client.bag.model.generated.VerblijfsobjectIOHal
import java.math.BigDecimal

fun createLigplaatsAdresseerbaarObject(status: StatusPlaats) =
    AdresseerbaarObjectIOHal().apply {
        ligplaats = LigplaatsIOHal().apply {
            ligplaats = Ligplaats().apply {
                this.status = status
                this.geometrie = createPolygonGeoJSON()
            }
        }
    }

fun createStandplaatsAdresseerbaarObject(status: StatusPlaats) =
    AdresseerbaarObjectIOHal().apply {
        standplaats = StandplaatsIOHal().apply {
            standplaats = Standplaats().apply {
                this.status = status
                this.geometrie = createPolygonGeoJSON()
            }
        }
    }

fun createVerblijfsAdresseerbaarObject(status: StatusVerblijfsobject) =
    AdresseerbaarObjectIOHal().apply {
        verblijfsobject = VerblijfsobjectIOHal().apply {
            verblijfsobject = Verblijfsobject().apply {
                this.status = status
                this.geometrie = createPuntOfVlak()
            }
        }
    }

fun createPuntOfVlak() = PuntOfVlak().apply {
    punt = PointGeoJSON().apply {
        type = PointGeoJSON.TypeEnum.POINT
        coordinates = createCoordinates()
    }
}

fun createPolygonGeoJSON() = PolygonGeoJSON().apply {
    type = PolygonGeoJSON.TypeEnum.POLYGON
    coordinates = listOf(listOf(createCoordinates())) as List<List<List<BigDecimal?>?>?>?
}

fun createCoordinates() = listOf<BigDecimal>(BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO)
