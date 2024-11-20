package net.atos.zac.app.zaak.converter.historie

import jakarta.inject.Inject
import net.atos.client.zgw.shared.model.audit.ZRCAuditTrailRegel
import net.atos.client.zgw.shared.util.URIUtil
import net.atos.client.zgw.zrc.ZrcClientService
import net.atos.client.zgw.zrc.model.Objecttype
import net.atos.client.zgw.zrc.model.Rol
import net.atos.client.zgw.zrc.model.zaakobjecten.Zaakobject
import net.atos.client.zgw.zrc.model.zaakobjecten.ZaakobjectProductaanvraag
import net.atos.client.zgw.ztc.ZtcClientService
import net.atos.zac.app.audit.CREATE
import net.atos.zac.app.audit.DESTROY
import net.atos.zac.app.audit.IDENTIFICATIE
import net.atos.zac.app.audit.KLANTCONTACT
import net.atos.zac.app.audit.PARTIAL_UPDATE
import net.atos.zac.app.audit.RESULTAAT
import net.atos.zac.app.audit.RESULTAATTYPE
import net.atos.zac.app.audit.ROL
import net.atos.zac.app.audit.ROLTYPE
import net.atos.zac.app.audit.STATUS
import net.atos.zac.app.audit.STATUSTYPE
import net.atos.zac.app.audit.TITEL
import net.atos.zac.app.audit.UPDATE
import net.atos.zac.app.audit.ZAAK
import net.atos.zac.app.audit.ZAAKINFORMATIEOBJECT
import net.atos.zac.app.audit.ZAAKOBJECT
import net.atos.zac.app.audit.model.RESTHistorieActie
import net.atos.zac.app.audit.model.RESTHistorieRegel
import nl.lifely.zac.util.asMapWithKeyOfString
import nl.lifely.zac.util.getTypedValue
import nl.lifely.zac.util.stringProperty
import java.net.URI
import java.util.UUID

private const val CREATE = "create"
private const val DESTROY = "destroy"
private const val UPDATE = "update"
private const val PARTIAL_UPDATE = "partial_update"

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

class RESTZaakHistorieRegelConverter @Inject constructor(
    private val zrcClientService: ZrcClientService,
    private val ztcClientService: ZtcClientService,
    private val restZaakHistoriePartialUpdateConverter: RESTZaakHistoriePartialUpdateConverter
) {
    fun getZaakHistory(zaakUUID: UUID): List<RESTHistorieRegel> {
        val auditTrail = zrcClientService.listAuditTrail(zaakUUID)
        return auditTrail
            .flatMap(::convertZaakRESTHistorieRegel)
            .sortedByDescending { it.datumTijd }
    }

    fun convertZaakRESTHistorieRegel(auditTrail: ZRCAuditTrailRegel): List<RESTHistorieRegel> {
        val old = (auditTrail.wijzigingen.oud as? Map<*, *>)?.asMapWithKeyOfString()
        val new = (auditTrail.wijzigingen.nieuw as? Map<*, *>)?.asMapWithKeyOfString()
        return when {
            auditTrail.actie == PARTIAL_UPDATE &&
                old != null &&
                new != null
            -> restZaakHistoriePartialUpdateConverter.convertPartialUpdate(
                auditTrail,
                convertActie(auditTrail.resource, auditTrail.actie),
                old,
                new
            )

            auditTrail.actie == CREATE ||
                auditTrail.actie == UPDATE ||
                auditTrail.actie == DESTROY
            -> listOfNotNull(convertLine(auditTrail, old, new))

            else -> emptyList()
        }
    }

    private fun convertLine(auditTrail: ZRCAuditTrailRegel, old: Map<String, *>?, new: Map<String, *>?):
        RESTHistorieRegel? =
        (old ?: new)
            ?.let { convertResource(auditTrail.resource, it) }
            ?.let { resource ->
                RESTHistorieRegel(
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

    private fun convertActie(resource: String?, action: String?): RESTHistorieActie? = when {
        resource == ZAAK && action == CREATE -> RESTHistorieActie.AANGEMAAKT
        resource == STATUS -> RESTHistorieActie.GEWIJZIGD
        action == CREATE -> RESTHistorieActie.GEKOPPELD
        action == DESTROY -> RESTHistorieActie.ONTKOPPELD
        action == UPDATE || action == PARTIAL_UPDATE -> RESTHistorieActie.GEWIJZIGD
        else -> null
    }

    private fun getObjectType(obj: Zaakobject): String? = when {
        obj is ZaakobjectProductaanvraag -> null
        obj.objectType == Objecttype.OVERIGE -> obj.objectTypeOverige
        else -> obj.objectType.toString()
    }
}
