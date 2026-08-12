/*
 * SPDX-FileCopyrightText: 2021 Atos, 2025, 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.client.zgw.zrc.model.zaakobjecten

import jakarta.json.bind.annotation.JsonbTransient
import jakarta.json.bind.annotation.JsonbTypeDeserializer
import nl.info.client.zgw.zrc.jsonb.ZaakObjectJsonbDeserializer
import nl.info.client.zgw.zrc.model.generated.ObjectTypeEnum
import java.net.URI
import java.util.UUID

/**
 * Zaakobject
 */
@JsonbTypeDeserializer(ZaakObjectJsonbDeserializer::class)
abstract class Zaakobject {
    /**
     * URL-referentie naar dit object. Dit is de unieke identificatie en locatie van dit object
     * - readOnly
     * Always present on a deserialized read result; absent from [ZaakobjectRequest].
     */
    lateinit var url: URI

    /**
     * Unieke resource identifier (UUID4)
     * - readOnly
     * Always present on a deserialized read result; absent from [ZaakobjectRequest].
     */
    lateinit var uuid: UUID

    /**
     * URL-referentie naar de ZAAK
     * - required
     */
    lateinit var zaak: URI

    /**
     * URL-referentie naar de resource die het OBJECT beschrijft
     */
    var `object`: URI? = null

    /**
     * Beschrijft het type OBJECT gerelateerd aan de ZAAK
     * - required
     */
    lateinit var objectType: ObjectTypeEnum

    /**
     * Beschrijft het type OBJECT als `objectType` de waarde "overige" heeft
     * - maxLength: 100
     * - pattern: '[a-z\_]+'
     */
    var objectTypeOverige: String? = null

    /**
     * Omschrijving van de betrekking tussen de ZAAK en het OBJECT
     * - maxLength: 80
     */
    var relatieomschrijving: String? = null

    /**
     * Constructor for JSONB deserialization
     */
    protected constructor()

    /**
     * Constructor with required attributes
     */
    protected constructor(zaakUri: URI, objectUri: URI?, objectType: ObjectTypeEnum) {
        this.zaak = zaakUri
        this.`object` = objectUri
        this.objectType = objectType
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) {
            return true
        }
        if (other == null || javaClass != other.javaClass) {
            return false
        }
        val that = other as Zaakobject
        return zaak == that.zaak &&
            `object` == that.`object` &&
            objectType == that.objectType &&
            objectTypeOverige == that.objectTypeOverige
    }

    override fun hashCode(): Int {
        var result = zaak.hashCode()
        result = 31 * result + (`object`?.hashCode() ?: 0)
        result = 31 * result + objectType.hashCode()
        result = 31 * result + (objectTypeOverige?.hashCode() ?: 0)
        return result
    }

    @get:JsonbTransient
    val isBagObject: Boolean
        get() = when (objectType) {
            ObjectTypeEnum.ADRES, ObjectTypeEnum.PAND, ObjectTypeEnum.OPENBARE_RUIMTE, ObjectTypeEnum.WOONPLAATS -> true
            ObjectTypeEnum.OVERIGE -> ZaakobjectNummeraanduiding.OBJECT_TYPE_OVERIGE == objectTypeOverige
            else -> false
        }

    @get:JsonbTransient
    abstract val waarde: String?

    override fun toString(): String =
        "Zaakobject{" +
            "url=$url" +
            ", uuid=$uuid" +
            ", zaak=$zaak" +
            ", object=${`object`}" +
            ", objectType=$objectType" +
            ", objectTypeOverige='$objectTypeOverige'" +
            ", relatieomschrijving='$relatieomschrijving'" +
            '}'
}
