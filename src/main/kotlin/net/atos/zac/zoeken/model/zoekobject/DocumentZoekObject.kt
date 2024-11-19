/*
 * SPDX-FileCopyrightText: 2022 Atos, 2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.zac.zoeken.model.zoekobject

import net.atos.client.zgw.drc.model.generated.StatusEnum
import net.atos.zac.zoeken.model.DocumentIndicatie
import nl.lifely.zac.util.NoArgConstructor
import org.apache.solr.client.solrj.beans.Field
import java.util.Date
import java.util.EnumSet
import java.util.Locale

@NoArgConstructor // required for Java bean inspection
data class DocumentZoekObject(
    @Field("id")
    private var id: String,

    @Field
    private var type: String,

    @Field("informatieobject_identificatie")
    var identificatie: String? = null,

    @Field("informatieobject_titel")
    var titel: String? = null,

    @Field("informatieobject_beschrijving")
    var beschrijving: String? = null,

    @Field("informatieobject_zaaktypeUuid")
    var zaaktypeUuid: String? = null,

    @Field("informatieobject_zaaktypeIdentificatie")
    var zaaktypeIdentificatie: String? = null,

    @Field("informatieobject_zaaktypeOmschrijving")
    var zaaktypeOmschrijving: String? = null,

    @Field("informatieobject_zaakId")
    var zaakIdentificatie: String? = null,

    @Field("informatieobject_zaakUuid")
    var zaakUuid: String? = null,

    @Field("informatieobject_zaakAfgehandeld")
    var isZaakAfgehandeld: Boolean = false,

    @Field("informatieobject_zaakRelatie")
    var zaakRelatie: String? = null,

    @Field("informatieobject_creatiedatum")
    var creatiedatum: Date? = null,

    @Field("informatieobject_registratiedatum")
    var registratiedatum: Date? = null,

    @Field("informatieobject_ontvangstdatum")
    var ontvangstdatum: Date? = null,

    @Field("informatieobject_verzenddatum")
    var verzenddatum: Date? = null,

    @Field("informatieobject_ondertekeningDatum")
    var ondertekeningDatum: Date? = null,

    @Field("informatieobject_ondertekeningSoort")
    var ondertekeningSoort: String? = null,

    @Field("informatieobject_vertrouwelijkheidaanduiding")
    var vertrouwelijkheidaanduiding: String? = null,

    @Field("informatieobject_auteur")
    var auteur: String? = null,

    @Field("informatieobject_status")
    private var status: String? = null,

    @Field("informatieobject_formaat")
    var formaat: String? = null,

    @Field("informatieobject_versie")
    var versie: Long = 0,

    @Field("informatieobject_bestandsnaam")
    var bestandsnaam: String? = null,

    @Field("informatieobject_bestandsomvang")
    var bestandsomvang: Long = 0,

    @Field("informatieobject_documentType")
    var documentType: String? = null,

    @Field("informatieobject_inhoudUrl")
    var inhoudUrl: String? = null,

    @Field("informatieobject_vergrendeldDoorNaam")
    var vergrendeldDoorNaam: String? = null,

    @Field("informatieobject_vergrendeldDoorGebruikersnaam")
    var vergrendeldDoorGebruikersnaam: String? = null,

    @Field("informatieobject_indicaties")
    private var indicaties: MutableList<String> = mutableListOf(),

    @Field("informatieobject_indicaties_sort")
    private var indicatiesVolgorde: Long = 0
) : ZoekObject {
    override fun getId() = id

    override fun getType() = ZoekObjectType.valueOf(type)

    fun setType(type: ZoekObjectType) {
        this.type = type.toString()
    }

    fun getStatus() = status?.uppercase(Locale.getDefault())?.let { StatusEnum.valueOf(it) }

    fun setStatus(status: StatusEnum) {
        this.status = status.toString()
    }

    fun isIndicatie(indicatie: DocumentIndicatie) = indicaties.contains(indicatie.name) == true

    fun getDocumentIndicaties(): EnumSet<DocumentIndicatie> {
        val documentIndicaties = EnumSet.noneOf<DocumentIndicatie>(DocumentIndicatie::class.java)
        for (indicatie in indicaties) {
            documentIndicaties.add(DocumentIndicatie.valueOf(indicatie))
        }
        return documentIndicaties
    }

    fun setIndicatie(indicatie: DocumentIndicatie, value: Boolean) {
        updateIndicaties(indicatie, value)
        updateIndicatieVolgorde(indicatie, value)
    }

    private fun updateIndicaties(indicatie: DocumentIndicatie, value: Boolean) {
        val key = indicatie.name
        if (value) {
            if (!indicaties.contains(key)) {
                indicaties.add(key)
            }
        } else {
            indicaties.remove(key)
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
