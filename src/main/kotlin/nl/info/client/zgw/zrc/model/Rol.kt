/*
 * SPDX-FileCopyrightText: 2021 Atos, 2023, 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.client.zgw.zrc.model

import jakarta.json.bind.annotation.JsonbDateFormat
import jakarta.json.bind.annotation.JsonbTypeDeserializer
import nl.info.client.zgw.util.DATE_TIME_FORMAT_WITH_MILLISECONDS
import nl.info.client.zgw.zrc.jsonb.RolJsonbDeserializer
import nl.info.client.zgw.zrc.model.generated.BetrokkeneTypeEnum
import nl.info.client.zgw.zrc.model.generated.IndicatieMachtigingEnum
import nl.info.client.zgw.ztc.model.generated.RolType
import java.net.URI
import java.time.ZonedDateTime
import java.util.Objects
import java.util.UUID

@JsonbTypeDeserializer(RolJsonbDeserializer::class)
abstract class Rol<T> {
    /**
     * URL-referentie naar dit object.
     * Dit is de unieke identificatie en locatie van dit object.
     */
    var url: URI? = null
        private set

    /**
     * Unieke resource identifier (UUID4)
     */
    var uuid: UUID? = null
        private set

    /**
     * URL-referentie naar de ZAAK.
     * - Required
     */
    var zaak: URI? = null
        private set

    /**
     * URL-referentie naar een betrokkene gerelateerd aan de ZAAK.
     */
    var betrokkene: URI? = null

    /**
     * De generieke betrokkene.
     * Can be null, according to the ZGW API, and this does occur in practice in certain circumstances.
     * - Required
     */
    var betrokkeneIdentificatie: T? = null
        private set

    /**
     * Betrokkene type
     * - Required
     */
    var betrokkeneType: BetrokkeneTypeEnum? = null
        private set

    /**
     * URL-referentie naar een roltype binnen het ZAAKTYPE van de ZAAK.
     * - Required
     */
    var roltype: URI? = null
        private set

    /**
     * Omschrijving van de aard van de ROL, afgeleid uit het ROLTYPE.
     */
    var omschrijving: String? = null
        private set

    /**
     * Algemeen gehanteerde benaming van de aard van de ROL, afgeleid uit het ROLTYPE.
     * Uitleg bij mogelijke waarden:
     * 'adviseur' - (Adviseur) Kennis in dienst stellen van de behandeling van (een deel van) een zaak.
     * 'behandelaar' - (Behandelaar) De vakinhoudelijke behandeling doen van (een deel van) een zaak.
     * 'belanghebbende' - (Belanghebbende) Vanuit eigen en objectief belang rechtstreeks betrokken zijn bij de behandeling en/of de uitkomst
     * van een zaak.
     * 'beslisser' - (Beslisser) Nemen van besluiten die voor de uitkomst van een zaak noodzakelijk zijn.
     * 'initiator' - (Initiator) Aanleiding geven tot de start van een zaak
     * 'klantcontacter' - (Klantcontacter) Het eerste aanspreekpunt zijn voor vragen van burgers en bedrijven
     * 'zaakcoordinator' - (Zaakcoordinator) Er voor zorg dragen dat de behandeling van de zaak in samenhang uitgevoerd wordt conform de
     * daarover gemaakte afspraken.
     * 'mede_initiator' - 'Mede-initiator'
     */
    var omschrijvingGeneriek: String? = null
        private set

    /**
     * Roltoelichting
     * - Required
     */
    var roltoelichting: String? = null
        private set

    /**
     * De datum waarop dit object is geregistreerd.
     */
    @JsonbDateFormat(DATE_TIME_FORMAT_WITH_MILLISECONDS)
    var registratiedatum: ZonedDateTime? = null
        private set

    var indicatieMachtiging: IndicatieMachtigingEnum? = null

    protected constructor()

    /**
     * For testing purposes only where a rol with a UUID is needed.
     */
    protected constructor(
        uuid: UUID,
        roltype: RolType,
        betrokkeneType: BetrokkeneTypeEnum,
        betrokkeneIdentificatie: T?,
        roltoelichting: String
    ) {
        this.uuid = uuid
        this.betrokkeneIdentificatie = betrokkeneIdentificatie
        this.betrokkeneType = betrokkeneType
        this.roltype = roltype.url
        this.roltoelichting = roltoelichting
        this.omschrijving = roltype.omschrijving
        this.omschrijvingGeneriek = roltype.omschrijvingGeneriek.name.lowercase()
    }

    /**
     * Constructor with required attributes for POST and PUT requests
     */
    protected constructor(
        zaak: URI,
        roltype: RolType,
        betrokkeneType: BetrokkeneTypeEnum,
        betrokkeneIdentificatie: T?,
        roltoelichting: String
    ) {
        this.zaak = zaak
        this.betrokkeneIdentificatie = betrokkeneIdentificatie
        this.betrokkeneType = betrokkeneType
        this.roltype = roltype.url
        this.roltoelichting = roltoelichting
        this.omschrijving = roltype.omschrijving
        this.omschrijvingGeneriek = roltype.omschrijvingGeneriek.name.lowercase()
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) {
            return true
        }
        if (other == null || javaClass != other.javaClass) {
            return false
        }
        @Suppress("UNCHECKED_CAST")
        val rol = other as Rol<T>
        return equalBetrokkeneRol(rol) && equalBetrokkeneIdentificatie(rol.betrokkeneIdentificatie)
    }

    override fun hashCode(): Int = Objects.hash(roltype, betrokkeneType, hashCodeBetrokkeneIdentificatie())

    fun equalBetrokkeneRol(other: Rol<*>): Boolean = betrokkeneType == other.betrokkeneType && roltype == other.roltype

    protected abstract fun equalBetrokkeneIdentificatie(identificatie: T?): Boolean

    protected abstract fun hashCodeBetrokkeneIdentificatie(): Int

    abstract val naam: String?

    /**
     * Can be null, according to the ZGW API, and this does occur in practice in certain circumstances.
     */
    abstract val identificatienummer: String?

    companion object {
        const val BETROKKENE_TYPE_NAAM = "betrokkeneType"
    }
}
