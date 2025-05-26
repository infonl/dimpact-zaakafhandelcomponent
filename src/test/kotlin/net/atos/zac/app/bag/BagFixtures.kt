/*
 * SPDX-FileCopyrightText: 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

package net.atos.zac.app.bag

import net.atos.zac.app.bag.model.BAGObjectType
import net.atos.zac.app.bag.model.RESTBAGAdres
import net.atos.zac.app.bag.model.RESTListAdressenParameters
import net.atos.zac.app.bag.model.RESTWoonplaats
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

fun createRESTBAGAdres() = RESTBAGAdres().apply {
    huisnummer = 1
    postcode = "1234AB"
    woonplaats = createRESTWoonplaats()
}

fun createRESTListAdressenParameters(
    bagObjectType: BAGObjectType = BAGObjectType.ADRES,
    trefwoorden: String = "fakeText",
    postcode: String = "1234AB",
    huisnummer: Int = 1,
) = RESTListAdressenParameters().apply {
    this.type = bagObjectType
    this.trefwoorden = trefwoorden
    this.postcode = postcode
    this.huisnummer = huisnummer
}

fun createRESTWoonplaats() = RESTWoonplaats().apply {
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
