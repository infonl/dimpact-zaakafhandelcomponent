/*
 * SPDX-FileCopyrightText: 2022 Atos, 2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.zac.search.model.zoekobject

import net.atos.client.zgw.zrc.model.Rol
import net.atos.zac.search.model.ZaakIndicatie
import nl.info.zac.util.NoArgConstructor
import org.apache.solr.client.solrj.beans.Field
import java.util.Date
import java.util.EnumSet
import java.util.function.Supplier
import java.util.stream.Collectors

@NoArgConstructor // required for Java bean inspection
data class ZaakZoekObject(
    /**
     * The UUID of the zaak.
     */
    @Field
    private var id: String,

    @Field
    private var type: String,

    @Field("zaak_identificatie")
    var identificatie: String? = null,

    @Field(OMSCHRIJVING_FIELD)
    var omschrijving: String? = null,

    @Field(TOELICHTING_FIELD)
    var toelichting: String? = null,

    @Field("zaak_registratiedatum")
    var registratiedatum: Date? = null,

    @Field("zaak_archiefNominatie")
    var archiefNominatie: String? = null,

    @Field("zaak_archiefActiedatum")
    var archiefActiedatum: Date? = null,

    @Field("zaak_startdatum")
    var startdatum: Date? = null,

    @Field("zaak_einddatumGepland")
    var einddatumGepland: Date? = null,

    @Field("zaak_einddatum")
    var einddatum: Date? = null,

    @Field("zaak_uiterlijkeEinddatumAfdoening")
    var uiterlijkeEinddatumAfdoening: Date? = null,

    @Field("zaak_publicatiedatum")
    var publicatiedatum: Date? = null,

    @Field("zaak_communicatiekanaal")
    var communicatiekanaal: String? = null,

    @Field("zaak_vertrouwelijkheidaanduiding")
    var vertrouwelijkheidaanduiding: String? = null,

    @Field(AFGEHANDELD_FIELD)
    var isAfgehandeld: Boolean = false,

    @Field("zaak_groepId")
    var groepID: String? = null,

    @Field("zaak_groepNaam")
    var groepNaam: String? = null,

    @Field("zaak_behandelaarNaam")
    var behandelaarNaam: String? = null,

    @Field(BEHANDELAAR_ID_FIELD)
    var behandelaarGebruikersnaam: String? = null,

    @Field("zaak_initiatorIdentificatie")
    var initiatorIdentificatie: String? = null,

    @Field("zaak_initiatorType")
    private var initiatorType: String? = null,

    @Field("zaak_locatie")
    var locatie: String? = null,

    @Field("zaak_duurVerlenging")
    var duurVerlenging: String? = null,

    @Field("zaak_redenVerlenging")
    var redenVerlenging: String? = null,

    @Field("zaak_redenOpschorting")
    var redenOpschorting: String? = null,

    @Field("zaak_zaaktypeUuid")
    var zaaktypeUuid: String? = null,

    @Field("zaak_zaaktypeIdentificatie")
    var zaaktypeIdentificatie: String? = null,

    @Field("zaak_zaaktypeOmschrijving")
    var zaaktypeOmschrijving: String? = null,

    @Field("zaak_resultaattypeOmschrijving")
    var resultaattypeOmschrijving: String? = null,

    @Field("zaak_resultaatToelichting")
    var resultaatToelichting: String? = null,

    @Field(EINDSTATUS_FIELD)
    var isStatusEindstatus: Boolean = false,

    @Field("zaak_statustypeOmschrijving")
    var statustypeOmschrijving: String? = null,

    @Field("zaak_statusDatumGezet")
    var statusDatumGezet: Date? = null,

    @Field("zaak_statusToelichting")
    var statusToelichting: String? = null,

    @Field("zaak_aantalOpenstaandeTaken")
    var aantalOpenstaandeTaken: Long = 0,

    @Field(ZoekObject.Companion.IS_TOEGEKEND_FIELD)
    var isToegekend: Boolean = false,

    @Field("zaak_indicaties")
    private var indicaties: MutableList<String>? = null,

    @Field("zaak_indicaties_sort")
    private var indicatiesVolgorde: Long = 0,

    @Field("zaak_betrokkene_*")
    var betrokkenen: MutableMap<String, MutableList<String>>? = null,

    @Field("zaak_bagObjecten")
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
        this.initiatorType = initiatorRole.betrokkeneType.toValue()
    }

    fun isIndicatie(indicatie: ZaakIndicatie) = indicaties?.contains(indicatie.name) == true

    fun getZaakIndicaties(): EnumSet<ZaakIndicatie> =
        if (indicaties == null) {
            EnumSet.noneOf<ZaakIndicatie>(ZaakIndicatie::class.java)
        } else {
            indicaties!!.stream().map<ZaakIndicatie>(ZaakIndicatie::valueOf)
                .collect(
                    Collectors.toCollection(
                        Supplier {
                            EnumSet.noneOf<ZaakIndicatie>(ZaakIndicatie::class.java)
                        }
                    )
                )
        }

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
        if (indicaties == null) { indicaties = mutableListOf() }
        val key = indicatie.name
        if (value) {
            if (!indicaties!!.contains(key)) {
                indicaties!!.add(key)
            }
        } else {
            indicaties!!.remove(key)
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
