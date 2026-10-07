/*
 * SPDX-FileCopyrightText: 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

package nl.info.zac.app.bag

import nl.info.zac.app.bag.model.BagObjectType
import nl.info.zac.app.bag.model.RestBagAdres
import nl.info.zac.app.bag.model.RestListAdressenParameters
import nl.info.zac.app.bag.model.RestWoonplaats
import nl.info.client.bag.model.generated.AdresseerbaarObjectIOHal
import nl.info.client.bag.model.generated.Ligplaats
import nl.info.client.bag.model.generated.LigplaatsIOHal
import nl.info.client.bag.model.generated.PointGeoJSON
import nl.info.client.bag.model.generated.PuntOfVlak
import nl.info.client.bag.model.generated.Standplaats
import nl.info.client.bag.model.generated.StandplaatsIOHal
import nl.info.client.bag.model.generated.StatusPlaats
import nl.info.client.bag.model.generated.StatusVerblijfsobject
import nl.info.client.bag.model.generated.Surface
import nl.info.client.bag.model.generated.Verblijfsobject
import nl.info.client.bag.model.generated.VerblijfsobjectIOHal
import java.math.BigDecimal

fun createLigplaatsAdresseerbaarObject(status: StatusPlaats) =
    AdresseerbaarObjectIOHal().apply {
        ligplaats = LigplaatsIOHal().apply {
            ligplaats = Ligplaats().apply {
                this.status = status
                this.geometrie = createSurface()
            }
        }
    }

fun createRestBagAdres() = RestBagAdres().apply {
    huisnummer = 1
    postcode = "1234AB"
    woonplaats = createRestWoonplaats()
}

fun createRestListAdressenParameters(
    bagObjectType: BagObjectType = BagObjectType.ADRES,
    trefwoorden: String = "fakeText",
    postcode: String = "1234AB",
    huisnummer: Int = 1,
) = RestListAdressenParameters().apply {
    this.type = bagObjectType
    this.trefwoorden = trefwoorden
    this.postcode = postcode
    this.huisnummer = huisnummer
}

fun createRestWoonplaats() = RestWoonplaats().apply {
    naam = "Amsterdam"
}

fun createStandplaatsAdresseerbaarObject(status: StatusPlaats) =
    AdresseerbaarObjectIOHal().apply {
        standplaats = StandplaatsIOHal().apply {
            standplaats = Standplaats().apply {
                this.status = status
                this.geometrie = createSurface()
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

fun createSurface() = Surface().apply {
    type = Surface.TypeEnum.POLYGON
    coordinates = listOf(listOf(createCoordinates())) as List<List<List<BigDecimal?>?>?>?
}

fun createCoordinates() = listOf<BigDecimal>(BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO)
