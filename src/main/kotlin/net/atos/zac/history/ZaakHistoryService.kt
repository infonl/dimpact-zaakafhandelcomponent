/*
 * SPDX-FileCopyrightText: 2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.zac.history

import jakarta.inject.Inject
import net.atos.client.zgw.shared.model.audit.ZRCAuditTrailRegel
import net.atos.client.zgw.zrc.ZrcClientService
import net.atos.client.zgw.zrc.model.Objecttype
import net.atos.client.zgw.zrc.model.Rol
import net.atos.client.zgw.zrc.model.zaakobjecten.Zaakobject
import net.atos.client.zgw.zrc.model.zaakobjecten.ZaakobjectProductaanvraag
import net.atos.client.zgw.ztc.ZtcClientService
import net.atos.zac.history.converter.ZaakHistoryPartialUpdateConverter
import net.atos.zac.history.model.HistoryAction
import net.atos.zac.history.model.HistoryLine
import net.atos.client.zgw.util.extractUuid
import nl.lifely.zac.util.asMapWithKeyOfString
import nl.lifely.zac.util.getTypedValue
import nl.lifely.zac.util.stringProperty
import java.net.URI
import java.util.UUID

private const val ACTION_CREATE = "create"
private const val ACTION_DESTROY = "destroy"
private const val ACTION_UPDATE = "update"
private const val ACTION_PARTIAL_UPDATE = "partial_update"

private const val RESOURCE_IDENTIFICATIE = "identificatie"
private const val RESOURCE_KLANTCONTACT = "klantcontact"
private const val RESOURCE_RESULTAAT = "resultaat"
private const val RESOURCE_RESULTAATTYPE = "resultaattype"
private const val RESOURCE_ROL = "rol"
private const val RESOURCE_ROLTYPE = "roltype"
private const val RESOURCE_STATUS = "status"
private const val RESOURCE_STATUSTYPE = "statustype"
private const val RESOURCE_ITEL = "titel"
private const val RESOURCE_ZAAK = "zaak"
private const val RESOURCE_ZAAKINFORMATIEOBJECT = "zaakinformatieobject"
private const val RESOURCE_ZAAKOBJECT = "zaakobject"
private const val RESOURCE_EXTENSION = "verlenging"
private const val RESOURCE_SUSPENSION = "opschorting"

class ZaakHistoryService @Inject constructor(
    private val zrcClientService: ZrcClientService,
    private val ztcClientService: ZtcClientService,
    private val zaakHistoryPartialUpdateConverter: ZaakHistoryPartialUpdateConverter
) {
    /**
     * Retrieves the zaak history ('audit trail') using the ZGW ZRC API.
     * Note that the contents of the audit trail lines are not defined by the ZGW ZRC API.
     * While our code to interpret these audit trail lines tries to be lenient,
     * the code is currently dependent on the specific implementation of the audit trail ZGW ZRC API endpoints
     * by [OpenZaak](https://open-zaak.readthedocs.io/).
     */
    fun getZaakHistory(zaakUUID: UUID): List<HistoryLine> {
        val auditTrail = zrcClientService.listAuditTrail(zaakUUID)
        return auditTrail
            .flatMap(::convertZaakHistoryLine)
            // we filter out certain audit trail lines because they add no value
            // and are confusing for the end-user
            .filter { it.attribuutLabel != RESOURCE_EXTENSION && it.attribuutLabel != RESOURCE_SUSPENSION }
            .sortedByDescending { it.datumTijd }
    }

    private fun convertZaakHistoryLine(auditTrailLine: ZRCAuditTrailRegel): List<HistoryLine> {
        val old = (auditTrailLine.wijzigingen.oud as? Map<*, *>)?.asMapWithKeyOfString()
        val new = (auditTrailLine.wijzigingen.nieuw as? Map<*, *>)?.asMapWithKeyOfString()
        return when {
            auditTrailLine.actie == ACTION_PARTIAL_UPDATE &&
                old != null &&
                new != null
            -> zaakHistoryPartialUpdateConverter.convertPartialUpdate(
                auditTrailLine,
                convertActie(auditTrailLine.resource, auditTrailLine.actie),
                old,
                new
            )

            auditTrailLine.actie == ACTION_CREATE ||
                auditTrailLine.actie == ACTION_UPDATE ||
                auditTrailLine.actie == ACTION_DESTROY
            -> listOfNotNull(convertLine(auditTrailLine, old, new))

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
        RESOURCE_ROL -> obj.stringProperty(RESOURCE_ROLTYPE)
            ?.let(URI::create)?.extractUuid()
            ?.let(ztcClientService::readRoltype)
            ?.omschrijving
        RESOURCE_ZAAKOBJECT -> obj.getTypedValue(Zaakobject::class.java)
            ?.let(::getObjectType)
            ?.let { "objecttype.$it" }
        else -> resource
    }

    private fun convertValue(resource: String, obj: Map<String, *>, resourceWeergave: String?): String? =
        when (resource) {
            RESOURCE_ZAAK -> obj.stringProperty(RESOURCE_IDENTIFICATIE)
            RESOURCE_ROL -> obj.getTypedValue(Rol::class.java)?.naam
            RESOURCE_ZAAKINFORMATIEOBJECT -> obj.stringProperty(RESOURCE_ITEL)
            RESOURCE_KLANTCONTACT -> resourceWeergave
            RESOURCE_RESULTAAT -> obj.stringProperty(RESOURCE_RESULTAATTYPE)
                ?.let(URI::create)
                ?.let(ztcClientService::readResultaattype)
                ?.omschrijving
            RESOURCE_STATUS -> obj.stringProperty(RESOURCE_STATUSTYPE)
                ?.let(URI::create)
                ?.let(ztcClientService::readStatustype)
                ?.omschrijving
            RESOURCE_ZAAKOBJECT -> obj.getTypedValue(Zaakobject::class.java).let {
                when {
                    it is ZaakobjectProductaanvraag -> null
                    else -> it?.waarde
                }
            }
            else -> null
        }

    private fun convertActie(resource: String?, action: String?): HistoryAction? = when {
        resource == RESOURCE_ZAAK && action == ACTION_CREATE -> HistoryAction.AANGEMAAKT
        resource == RESOURCE_STATUS -> HistoryAction.GEWIJZIGD
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
