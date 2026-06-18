/*
 * SPDX-FileCopyrightText: 2022 Atos, 2024 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.search.model.zoekobject

import com.fasterxml.jackson.annotation.JsonProperty
import nl.info.client.zgw.drc.model.generated.StatusEnum
import nl.info.zac.search.model.DocumentIndicatie
import nl.info.zac.util.NoArgConstructor
import java.util.Date
import java.util.EnumSet
import java.util.Locale
import kotlin.collections.remove

@NoArgConstructor // required for Java bean inspection
data class DocumentZoekObject(
    @JsonProperty("id")
    private var id: String,

    @JsonProperty("type")
    private var type: String,

    @JsonProperty("informatieobject_identificatie")
    var identificatie: String? = null,

    @JsonProperty("informatieobject_titel")
    var titel: String? = null,

    @JsonProperty("informatieobject_beschrijving")
    var beschrijving: String? = null,

    @JsonProperty("informatieobject_zaaktypeUuid")
    var zaaktypeUuid: String? = null,

    @JsonProperty("informatieobject_zaaktypeIdentificatie")
    var zaaktypeIdentificatie: String? = null,

    @JsonProperty("informatieobject_zaaktypeOmschrijving")
    var zaaktypeOmschrijving: String? = null,

    @JsonProperty("informatieobject_zaakId")
    var zaakIdentificatie: String? = null,

    @JsonProperty("informatieobject_zaakUuid")
    var zaakUuid: String? = null,

    @JsonProperty("informatieobject_zaakAfgehandeld")
    var isZaakAfgehandeld: Boolean = false,

    @JsonProperty("informatieobject_zaakRelatie")
    var zaakRelatie: String? = null,

    @JsonProperty("informatieobject_creatiedatum")
    var creatiedatum: Date? = null,

    @JsonProperty("informatieobject_registratiedatum")
    var registratiedatum: Date? = null,

    @JsonProperty("informatieobject_ontvangstdatum")
    var ontvangstdatum: Date? = null,

    @JsonProperty("informatieobject_verzenddatum")
    var verzenddatum: Date? = null,

    @JsonProperty("informatieobject_ondertekeningDatum")
    var ondertekeningDatum: Date? = null,

    @JsonProperty("informatieobject_ondertekeningSoort")
    var ondertekeningSoort: String? = null,

    @JsonProperty("informatieobject_vertrouwelijkheidaanduiding")
    var vertrouwelijkheidaanduiding: String? = null,

    @JsonProperty("informatieobject_auteur")
    var auteur: String? = null,

    @JsonProperty("informatieobject_status")
    private var status: String? = null,

    @JsonProperty("informatieobject_formaat")
    var formaat: String? = null,

    @JsonProperty("informatieobject_versie")
    var versie: Long = 0,

    @JsonProperty("informatieobject_bestandsnaam")
    var bestandsnaam: String? = null,

    @JsonProperty("informatieobject_bestandsomvang")
    var bestandsomvang: Long = 0,

    @JsonProperty("informatieobject_documentType")
    var documentType: String? = null,

    @JsonProperty("informatieobject_inhoudUrl")
    var inhoudUrl: String? = null,

    @JsonProperty("informatieobject_vergrendeldDoorNaam")
    var vergrendeldDoorNaam: String? = null,

    @JsonProperty("informatieobject_vergrendeldDoorGebruikersnaam")
    var vergrendeldDoorGebruikersnaam: String? = null,

    @JsonProperty("informatieobject_indicaties")
    private var indicaties: MutableList<String>? = null,

    @JsonProperty("informatieobject_indicaties_sort")
    private var indicatiesVolgorde: Long = 0
) : ZoekObject {
    override fun getObjectId() = id

    override fun getType() = ZoekObjectType.valueOf(type)

    fun setType(type: ZoekObjectType) {
        this.type = type.toString()
    }

    fun getStatus() = status?.uppercase(Locale.getDefault())?.let { StatusEnum.valueOf(it) }

    fun setStatus(status: StatusEnum) {
        this.status = status.toString()
    }

    fun isIndicatie(indicatie: DocumentIndicatie) = indicaties?.contains(indicatie.name) ?: false

    fun getDocumentIndicaties(): EnumSet<DocumentIndicatie> =
        indicaties?.mapTo(EnumSet.noneOf(DocumentIndicatie::class.java)) { DocumentIndicatie.valueOf(it) }
            ?: EnumSet.noneOf(DocumentIndicatie::class.java)

    fun setIndicatie(indicatie: DocumentIndicatie, value: Boolean) {
        updateIndicaties(indicatie, value)
        updateIndicatieVolgorde(indicatie, value)
    }

    private fun updateIndicaties(indicatie: DocumentIndicatie, value: Boolean) {
        indicaties = indicaties ?: mutableListOf()
        val key = indicatie.name
        if (value) {
            indicaties?.let { if (key !in it) it.add(key) }
        } else {
            indicaties?.remove(key)
        }
    }

    private fun updateIndicatieVolgorde(indicatie: DocumentIndicatie, value: Boolean) {
        val bit = DocumentIndicatie.entries.size - 1 - indicatie.ordinal
        indicatiesVolgorde = if (value) {
            indicatiesVolgorde or (1L shl bit)
        } else {
            indicatiesVolgorde and (1L shl bit).inv()
        }
    }
}
