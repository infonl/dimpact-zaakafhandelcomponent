/*
 * SPDX-FileCopyrightText: 2023 Atos, 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.app.bag.model

import nl.info.client.bag.model.generated.AdresseerbaarObjectIOHal
import nl.info.client.bag.model.generated.Indicatie
import nl.info.client.bag.model.generated.Ligplaats
import nl.info.client.bag.model.generated.Standplaats
import nl.info.client.bag.model.generated.TypeAdresseerbaarObject
import nl.info.client.bag.model.generated.Verblijfsobject
import nl.info.zac.app.zaak.model.RestGeometry
import nl.info.zac.app.zaak.model.toRestGeometry

class RestAdresseerbaarObject : RestBagObject() {
    var typeAdresseerbaarObject: TypeAdresseerbaarObject? = null

    var status: String? = null

    var vboDoel: String? = null

    var vboOppervlakte: Int = 0

    var geometry: RestGeometry? = null

    override val bagObjectType
        get() = BagObjectType.ADRESSEERBAAR_OBJECT

    override val omschrijving
        get() = joinNonBlank(" ", typeAdresseerbaarObject?.toString(), identificatie)
}

fun AdresseerbaarObjectIOHal.toRestAdresseerbaarObject() =
    when {
        ligplaats != null -> ligplaats.ligplaats.toRestAdresseerbaarObject()
        standplaats != null -> standplaats.standplaats.toRestAdresseerbaarObject()
        verblijfsobject != null -> verblijfsobject.verblijfsobject.toRestAdresseerbaarObject()
        else -> error("adresseerbaarObject is leeg")
    }

private fun Ligplaats.toRestAdresseerbaarObject() = RestAdresseerbaarObject().apply {
    typeAdresseerbaarObject = TypeAdresseerbaarObject.LIGPLAATS
    identificatie = this@toRestAdresseerbaarObject.identificatie
    status = this@toRestAdresseerbaarObject.status.toString()
    isGeconstateerd = this@toRestAdresseerbaarObject.geconstateerd == Indicatie.J
    geometry = this@toRestAdresseerbaarObject.geometrie.toRestGeometry()
}

private fun Standplaats.toRestAdresseerbaarObject() = RestAdresseerbaarObject().apply {
    typeAdresseerbaarObject = TypeAdresseerbaarObject.STANDPLAATS
    identificatie = this@toRestAdresseerbaarObject.identificatie
    status = this@toRestAdresseerbaarObject.status.toString()
    isGeconstateerd = this@toRestAdresseerbaarObject.geconstateerd == Indicatie.J
    geometry = this@toRestAdresseerbaarObject.geometrie.toRestGeometry()
}

private fun Verblijfsobject.toRestAdresseerbaarObject() = RestAdresseerbaarObject().apply {
    typeAdresseerbaarObject = TypeAdresseerbaarObject.VERBLIJFSOBJECT
    identificatie = this@toRestAdresseerbaarObject.identificatie
    status = this@toRestAdresseerbaarObject.status.toString()
    isGeconstateerd = this@toRestAdresseerbaarObject.geconstateerd == Indicatie.J
    vboDoel = this@toRestAdresseerbaarObject.gebruiksdoelen.orEmpty().joinToString(", ")
    vboOppervlakte = this@toRestAdresseerbaarObject.oppervlakte ?: 0
    geometry = this@toRestAdresseerbaarObject.geometrie.toRestGeometry()
}
