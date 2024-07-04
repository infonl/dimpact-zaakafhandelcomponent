package net.atos.zac.app.zaken.converter.historie

import jakarta.inject.Inject
import net.atos.client.zgw.shared.model.audit.ZRCAuditTrailRegel
import net.atos.client.zgw.shared.util.URIUtil
import net.atos.client.zgw.zrc.model.Rol
import net.atos.client.zgw.zrc.model.zaakobjecten.Zaakobject
import net.atos.client.zgw.zrc.model.zaakobjecten.ZaakobjectProductaanvraag
import net.atos.client.zgw.ztc.ZtcClientService
import net.atos.zac.app.audit.model.RESTHistorieRegel
import java.net.URI

private const val CREATE = "create"
private const val DESTROY = "destroy"
private const val UPDATE = "update"
private const val PARTIAL_UPDATE = "partial_update"

private const val IDENTIFICATIE = "identificatie"
private const val KLANTCONTACT = "klantcontact"
private const val OBJECT_TYPE = "objectType"
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
    private val ztcClientService: ZtcClientService,
    private val restZaakHistoriePartialUpdateConverter: RESTZaakHistoriePartialUpdateConverter
) {
    fun convertZaakRESTHistorieRegel(auditTrail: ZRCAuditTrailRegel): List<RESTHistorieRegel> {
        val old = auditTrail.wijzigingen.oud as? Map<*, *>
        val new = auditTrail.wijzigingen.nieuw as? Map<*, *>
        return when {
            auditTrail.actie == PARTIAL_UPDATE &&
                old != null &&
                new != null
            -> restZaakHistoriePartialUpdateConverter.convertPartialUpdate(auditTrail, old, new)

            auditTrail.actie == CREATE ||
                auditTrail.actie == UPDATE ||
                auditTrail.actie == DESTROY
            -> listOf(convertLine(auditTrail, old, new))

            else -> emptyList()
        }
    }

    private fun convertLine(auditTrail: ZRCAuditTrailRegel, old: Map<*, *>?, new: Map<*, *>?): RESTHistorieRegel =
        RESTHistorieRegel(
            (old ?: new)?.let { convertResource(auditTrail.resource, it) },
            old?.let { convertValue(auditTrail.resource, it, auditTrail.resourceWeergave) },
            new?.let { convertValue(auditTrail.resource, it, auditTrail.resourceWeergave) }
        ).apply {
            datumTijd = auditTrail.aanmaakdatum
            toelichting = auditTrail.toelichting
            door = auditTrail.gebruikersWeergave
        }

    private fun convertResource(resource: String, obj: Map<*, *>): String = when (resource) {
        ROL -> obj.stringProperty(ROLTYPE)
            ?.let(URI::create)
            ?.let(URIUtil::parseUUIDFromResourceURI)
            ?.let(ztcClientService::readRoltype)
            ?.omschrijving
        ZAAKOBJECT -> obj.stringProperty(OBJECT_TYPE)
        else -> resource
    } ?: resource

    private fun convertValue(resource: String, obj: Map<*, *>, resourceWeergave: String?): String? =
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
                    else -> "objecttype.${it?.waarde}"
                }
            }
            else -> null
        }
}
