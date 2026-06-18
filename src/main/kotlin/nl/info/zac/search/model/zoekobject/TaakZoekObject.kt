/*
 * SPDX-FileCopyrightText: 2022 Atos, 2024 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.search.model.zoekobject

import com.fasterxml.jackson.annotation.JsonProperty
import nl.info.zac.app.task.model.TaakStatus
import nl.info.zac.util.NoArgConstructor
import java.util.Date

@NoArgConstructor // required for Java bean inspection
data class TaakZoekObject(
    @JsonProperty("id")
    private var id: String,

    @JsonProperty("type")
    private var type: String,

    @JsonProperty("taak_naam")
    var naam: String? = null,

    @JsonProperty("taak_toelichting")
    var toelichting: String? = null,

    @JsonProperty("taak_status")
    private var status: String? = null,

    @JsonProperty("taak_zaaktypeUuid")
    var zaaktypeUuid: String? = null,

    @JsonProperty("taak_zaaktypeIdentificatie")
    var zaaktypeIdentificatie: String? = null,

    @JsonProperty("taak_zaaktypeOmschrijving")
    var zaaktypeOmschrijving: String? = null,

    @JsonProperty("taak_zaakUuid")
    var zaakUUID: String? = null,

    @JsonProperty("taak_zaakId")
    var zaakIdentificatie: String? = null,

    @JsonProperty("taak_creatiedatum")
    var creatiedatum: Date? = null,

    @JsonProperty("taak_toekenningsdatum")
    var toekenningsdatum: Date? = null,

    @JsonProperty("taak_fataledatum")
    var fataledatum: Date? = null,

    @JsonProperty("taak_groepId")
    var groepID: String? = null,

    @JsonProperty("taak_groepNaam")
    var groepNaam: String? = null,

    @JsonProperty("taak_behandelaarNaam")
    var behandelaarNaam: String? = null,

    @JsonProperty(BEHANDELAAR_ID_FIELD)
    var behandelaarGebruikersnaam: String? = null,

    @JsonProperty("taak_data")
    var taakData: List<String> = listOf(),

    @JsonProperty("taak_informatie")
    var taakInformatie: List<String> = listOf(),

    @JsonProperty(ZaakZoekObject.TOELICHTING_FIELD)
    var zaakToelichting: String? = null,

    @JsonProperty(ZaakZoekObject.OMSCHRIJVING_FIELD)
    var zaakOmschrijving: String? = null,

    @JsonProperty(ZoekObject.Companion.IS_TOEGEKEND_FIELD)
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
