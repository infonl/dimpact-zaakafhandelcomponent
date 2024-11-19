/*
 * SPDX-FileCopyrightText: 2022 Atos, 2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.zac.zoeken.model.zoekobject

import net.atos.zac.app.task.model.TaakStatus
import nl.lifely.zac.util.NoArgConstructor
import org.apache.solr.client.solrj.beans.Field
import java.util.Date

@NoArgConstructor // required for Java bean inspection
data class TaakZoekObject(
    @Field
    var id: String,

    @Field
    private var type: String,

    @Field("taak_naam")
    var naam: String? = null,

    @Field("taak_toelichting")
    var toelichting: String? = null,

    @Field("taak_status")
    private var status: String? = null,

    @Field("taak_zaaktypeUuid")
    var zaaktypeUuid: String? = null,

    @Field("taak_zaaktypeIdentificatie")
    var zaaktypeIdentificatie: String? = null,

    @Field("taak_zaaktypeOmschrijving")
    var zaaktypeOmschrijving: String? = null,

    @Field("taak_zaakUuid")
    var zaakUUID: String? = null,

    @Field("taak_zaakId")
    var zaakIdentificatie: String? = null,

    @Field("taak_creatiedatum")
    var creatiedatum: Date? = null,

    @Field("taak_toekenningsdatum")
    var toekenningsdatum: Date? = null,

    @Field("taak_fataledatum")
    var fataledatum: Date? = null,

    @Field("taak_groepId")
    var groepID: String? = null,

    @Field("taak_groepNaam")
    var groepNaam: String? = null,

    @Field("taak_behandelaarNaam")
    var behandelaarNaam: String? = null,

    @Field(BEHANDELAAR_ID_FIELD)
    var behandelaarGebruikersnaam: String? = null,

    @Field("taak_data")
    var taakData: List<String> = listOf(),

    @Field("taak_informatie")
    var taakInformatie: List<String> = listOf(),

    @Field(ZaakZoekObject.TOELICHTING_FIELD)
    var zaakToelichting: String? = null,

    @Field(ZaakZoekObject.OMSCHRIJVING_FIELD)
    var zaakOmschrijving: String? = null,

    @Field(ZoekObject.Companion.IS_TOEGEKEND_FIELD)
    var isToegekend: Boolean = false
) : ZoekObject {
    companion object {
        const val BEHANDELAAR_ID_FIELD: String = "taak_behandelaarGebruikersnaam"
    }

    override fun getObjectId() = id

    override fun getType() = ZoekObjectType.valueOf(type)

    fun getStatus() = status?.let { TaakStatus.valueOf(it) }

    fun setStatus(taakStatus: TaakStatus) {
        status = taakStatus.toString()
    }

    fun setType(type: ZoekObjectType) {
        this.type = type.toString()
    }
}
