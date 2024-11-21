/*
 * SPDX-FileCopyrightText: 2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.zac.history

import jakarta.inject.Inject
import net.atos.client.zgw.shared.model.audit.ZRCAuditTrailRegel
import net.atos.client.zgw.shared.util.URIUtil
import net.atos.client.zgw.zrc.ZrcClientService
import net.atos.client.zgw.zrc.model.Objecttype
import net.atos.client.zgw.zrc.model.Rol
import net.atos.client.zgw.zrc.model.zaakobjecten.Zaakobject
import net.atos.client.zgw.zrc.model.zaakobjecten.ZaakobjectProductaanvraag
import net.atos.client.zgw.ztc.ZtcClientService
import net.atos.zac.history.converter.ZaakHistoryPartialUpdateConverter
import net.atos.zac.history.model.HistoryAction
import net.atos.zac.history.model.HistoryLine
import nl.lifely.zac.util.asMapWithKeyOfString
import nl.lifely.zac.util.getTypedValue
import nl.lifely.zac.util.stringProperty
import java.net.URI
import java.util.UUID

private const val ACTION_CREATE = "create"
private const val ACTION_DESTROY = "destroy"
private const val ACTION_UPDATE = "update"
private const val ACTION_PARTIAL_UPDATE = "partial_update"

private const val IDENTIFICATIE = "identificatie"
private const val KLANTCONTACT = "klantcontact"
private const val RESULTAAT = "resultaat"
private const val RESULTAATTYPE = "resultaattype"
private const val ROL = "rol"
private const val ROLTYPE = "roltype"
private const val STATUS = "status"
private const val STATUSTYPE = "statustype"
private const val TITEL = "titel"
private const val ZAAK = "zaak"
private const val ZAAKINFORMATIEOBJECT = "zaakinformatieobject"
private const val ZAAKOBJECT = "zaakobject"

class ZaakHistoryService @Inject constructor(
    private val zrcClientService: ZrcClientService,
    private val ztcClientService: ZtcClientService,
    private val zaakHistoryPartialUpdateConverter: ZaakHistoryPartialUpdateConverter
) {
    fun getZaakHistory(zaakUUID: UUID): List<HistoryLine> {
        val auditTrail = zrcClientService.listAuditTrail(zaakUUID)
        return auditTrail
            .flatMap(::convertZaakHistoryLine)
            .sortedByDescending { it.datumTijd }
    }

    private fun convertZaakHistoryLine(auditTrail: ZRCAuditTrailRegel): List<HistoryLine> {
        val old = (auditTrail.wijzigingen.oud as? Map<*, *>)?.asMapWithKeyOfString()
        val new = (auditTrail.wijzigingen.nieuw as? Map<*, *>)?.asMapWithKeyOfString()
        return when {
            auditTrail.actie == ACTION_PARTIAL_UPDATE &&
                old != null &&
                new != null
            -> zaakHistoryPartialUpdateConverter.convertPartialUpdate(
                auditTrail,
                convertActie(auditTrail.resource, auditTrail.actie),
                old,
                new
            )

            auditTrail.actie == ACTION_CREATE ||
                auditTrail.actie == ACTION_UPDATE ||
                auditTrail.actie == ACTION_DESTROY
            -> listOfNotNull(convertLine(auditTrail, old, new))

            else -> emptyList()
        }
    }

    private fun convertLine(auditTrail: ZRCAuditTrailRegel, old: Map<String, *>?, new: Map<String, *>?): HistoryLine? =
        (old ?: new)
            ?.let { convertResource(auditTrail.resource, it) }
            ?.let { resource ->
                HistoryLine(
                    resource,
                    old?.let { convertValue(auditTrail.resource, it, auditTrail.resourceWeergave) },
                    new?.let { convertValue(auditTrail.resource, it, auditTrail.resourceWeergave) }
                )
            }
            ?.apply {
                datumTijd = auditTrail.aanmaakdatum
                toelichting = auditTrail.toelichting
                door = auditTrail.gebruikersWeergave
                actie = convertActie(auditTrail.resource, auditTrail.actie)
            }

    private fun convertResource(resource: String, obj: Map<String, *>): String? = when (resource) {
        ROL -> obj.stringProperty(ROLTYPE)
            ?.let(URI::create)
            ?.let(URIUtil::parseUUIDFromResourceURI)
            ?.let(ztcClientService::readRoltype)
            ?.omschrijving
        ZAAKOBJECT -> obj.getTypedValue(Zaakobject::class.java)
            ?.let(::getObjectType)
            ?.let { "objecttype.$it" }
        else -> resource
    }

    private fun convertValue(resource: String, obj: Map<String, *>, resourceWeergave: String?): String? =
        when (resource) {
            ZAAK -> obj.stringProperty(IDENTIFICATIE)
            ROL -> obj.getTypedValue(Rol::class.java)?.naam
            ZAAKINFORMATIEOBJECT -> obj.stringProperty(TITEL)
            KLANTCONTACT -> resourceWeergave
            RESULTAAT -> obj.stringProperty(RESULTAATTYPE)
                ?.let(URI::create)
                ?.let(ztcClientService::readResultaattype)
                ?.omschrijving
            STATUS -> obj.stringProperty(STATUSTYPE)
                ?.let(URI::create)
                ?.let(ztcClientService::readStatustype)
                ?.omschrijving
            ZAAKOBJECT -> obj.getTypedValue(Zaakobject::class.java).let {
                when {
                    it is ZaakobjectProductaanvraag -> null
                    else -> it?.waarde
                }
            }
            else -> null
        }

    private fun convertActie(resource: String?, action: String?): HistoryAction? = when {
        resource == ZAAK && action == ACTION_CREATE -> HistoryAction.AANGEMAAKT
        resource == STATUS -> HistoryAction.GEWIJZIGD
        action == ACTION_CREATE -> HistoryAction.GEKOPPELD
        action == ACTION_DESTROY -> HistoryAction.ONTKOPPELD
        action == ACTION_UPDATE || action == ACTION_PARTIAL_UPDATE -> HistoryAction.GEWIJZIGD
        else -> null
    }

    private fun getObjectType(obj: Zaakobject): String? = when {
        obj is ZaakobjectProductaanvraag -> null
        obj.objectType == Objecttype.OVERIGE -> obj.objectTypeOverige
        else -> obj.objectType.toString()
    }
}
