/*
 * SPDX-FileCopyrightText: 2022 Atos, 2024 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.search.model.zoekobject

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonProperty
import net.atos.client.zgw.zrc.model.Rol
import nl.info.zac.search.model.ZaakIndicatie
import nl.info.zac.util.NoArgConstructor
import java.util.Date
import java.util.EnumSet
import kotlin.collections.remove

@NoArgConstructor
data class ZaakZoekObject(
    /**
     * The UUID of the zaak.
     */
    @JsonProperty("id")
    private var id: String,

    @JsonProperty("type")
    private var type: String,

    @JsonProperty("zaak_identificatie")
    var identificatie: String,

    @JsonProperty(OMSCHRIJVING_FIELD)
    var omschrijving: String? = null,

    @JsonProperty(TOELICHTING_FIELD)
    var toelichting: String? = null,

    @JsonProperty("zaak_registratiedatum")
    var registratiedatum: Date? = null,

    @JsonProperty("zaak_archiefNominatie")
    var archiefNominatie: String? = null,

    @JsonProperty("zaak_archiefActiedatum")
    var archiefActiedatum: Date? = null,

    @JsonProperty("zaak_startdatum")
    var startdatum: Date? = null,

    @JsonProperty("zaak_einddatumGepland")
    var einddatumGepland: Date? = null,

    @JsonProperty("zaak_einddatum")
    var einddatum: Date? = null,

    @JsonProperty("zaak_uiterlijkeEinddatumAfdoening")
    var uiterlijkeEinddatumAfdoening: Date? = null,

    @JsonProperty("zaak_publicatiedatum")
    var publicatiedatum: Date? = null,

    @JsonProperty("zaak_communicatiekanaal")
    var communicatiekanaal: String? = null,

    @JsonProperty("zaak_vertrouwelijkheidaanduiding")
    var vertrouwelijkheidaanduiding: String? = null,

    @JsonProperty(AFGEHANDELD_FIELD)
    var isAfgehandeld: Boolean = false,

    @JsonProperty("zaak_groepId")
    var groepID: String? = null,

    @JsonProperty("zaak_groepNaam")
    var groepNaam: String? = null,

    @JsonProperty("zaak_behandelaarNaam")
    var behandelaarNaam: String? = null,

    @JsonProperty(BEHANDELAAR_ID_FIELD)
    var behandelaarGebruikersnaam: String? = null,

    @JsonProperty("zaak_initiatorIdentificatie")
    var initiatorIdentificatie: String? = null,

    @JsonProperty("zaak_initiatorType")
    private var initiatorType: String? = null,

    @JsonProperty("zaak_locatie")
    var locatie: String? = null,

    @JsonProperty("zaak_duurVerlenging")
    var duurVerlenging: String? = null,

    @JsonProperty("zaak_redenVerlenging")
    var redenVerlenging: String? = null,

    @JsonProperty("zaak_redenOpschorting")
    var redenOpschorting: String? = null,

    @JsonProperty("zaak_zaaktypeUuid")
    var zaaktypeUuid: String,

    @JsonProperty("zaak_zaaktypeIdentificatie")
    var zaaktypeIdentificatie: String,

    @JsonProperty("zaak_zaaktypeOmschrijving")
    var zaaktypeOmschrijving: String,

    @JsonProperty("zaak_resultaattypeOmschrijving")
    var resultaattypeOmschrijving: String? = null,

    @JsonProperty("zaak_resultaatToelichting")
    var resultaatToelichting: String? = null,

    @JsonProperty(EINDSTATUS_FIELD)
    var isStatusEindstatus: Boolean = false,

    @JsonProperty("zaak_statustypeOmschrijving")
    var statustypeOmschrijving: String? = null,

    @JsonProperty("zaak_statusDatumGezet")
    var statusDatumGezet: Date? = null,

    @JsonProperty("zaak_statusToelichting")
    var statusToelichting: String? = null,

    @JsonProperty("zaak_aantalOpenstaandeTaken")
    var aantalOpenstaandeTaken: Long = 0,

    @JsonProperty(ZoekObject.Companion.IS_TOEGEKEND_FIELD)
    var isToegekend: Boolean = false,

    @JsonProperty("zaak_indicaties")
    private var indicaties: MutableList<String>? = null,

    @JsonProperty("zaak_indicaties_sort")
    private var indicatiesVolgorde: Long = 0,

    /**
     * Dynamic `zaak_betrokkene_<rol>` fields. Held as a map keyed by the full field name and flattened to
     * top-level index fields by the indexing layer (and reconstructed from them when reading search hits),
     * so it is not (de)serialized directly by Jackson.
     */
    @JsonIgnore
    var betrokkenen: MutableMap<String, MutableList<String>>? = null,

    @JsonProperty("zaak_bagObjecten")
    var bagObjectIDs: List<String>? = null
) : ZoekObject {
    companion object {
        const val AFGEHANDELD_FIELD: String = "zaak_afgehandeld"
        const val BEHANDELAAR_ID_FIELD: String = "zaak_behandelaarGebruikersnaam"
        const val EINDSTATUS_FIELD: String = "zaak_statusEindstatus"
        const val OMSCHRIJVING_FIELD: String = "zaak_omschrijving"
        const val TOELICHTING_FIELD: String = "zaak_toelichting"
        const val ZAAK_BETROKKENE_PREFIX: String = "zaak_betrokkene_"
    }

    override fun getObjectId() = id

    override fun getType() = ZoekObjectType.valueOf(type)

    fun setType(type: ZoekObjectType) {
        this.type = type.toString()
    }

    fun setInitiator(initiatorRole: Rol<*>) {
        this.initiatorIdentificatie = initiatorRole.getIdentificatienummer()
        this.initiatorType = initiatorRole.betrokkeneType.toString()
    }

    fun isIndicatie(indicatie: ZaakIndicatie) = indicaties?.contains(indicatie.name) == true

    fun getZaakIndicaties(): EnumSet<ZaakIndicatie> =
        indicaties?.mapTo(EnumSet.noneOf(ZaakIndicatie::class.java)) { ZaakIndicatie.valueOf(it) }
            ?: EnumSet.noneOf(ZaakIndicatie::class.java)

    fun setIndicatie(indicatie: ZaakIndicatie, value: Boolean) {
        updateIndicaties(indicatie, value)
        updateIndicatieVolgorde(indicatie, value)
    }

    fun addBetrokkene(rol: String, identificatie: String) {
        val key = "$ZAAK_BETROKKENE_PREFIX$rol"
        betrokkenen = betrokkenen ?: mutableMapOf()
        betrokkenen!!.getOrPut(key) { mutableListOf() }.add(identificatie)
    }

    private fun updateIndicaties(indicatie: ZaakIndicatie, value: Boolean) {
        indicaties = indicaties ?: mutableListOf()
        val key = indicatie.name
        if (value) {
            indicaties?.let { if (key !in it) it.add(key) }
        } else {
            indicaties?.remove(key)
        }
    }

    private fun updateIndicatieVolgorde(indicatie: ZaakIndicatie, value: Boolean) {
        val bit = ZaakIndicatie.entries.size - 1 - indicatie.ordinal
        if (value) {
            this.indicatiesVolgorde = this.indicatiesVolgorde or (1L shl bit)
        } else {
            this.indicatiesVolgorde = this.indicatiesVolgorde and (1L shl bit).inv()
        }
    }
}
