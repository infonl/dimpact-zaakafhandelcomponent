package net.atos.zac.app.zaken.converter.historie

import jakarta.inject.Inject
import net.atos.client.zgw.shared.util.URIUtil
import net.atos.client.zgw.zrc.model.Rol
import net.atos.client.zgw.zrc.model.generated.AuditTrail
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
    fun convertZaakRESTHistorieRegel(auditTrailRegel: AuditTrail): List<RESTHistorieRegel> {
        val oud = auditTrailRegel.wijzigingen.oud
        val nieuw = auditTrailRegel.wijzigingen.nieuw
        return when {
            auditTrailRegel.actie == PARTIAL_UPDATE && oud is HashMap<*, *> && nieuw is HashMap<*, *>
            -> restZaakHistoriePartialUpdateConverter.handlePartialUpdate(auditTrailRegel, oud, nieuw)
            auditTrailRegel.actie == CREATE ||
                auditTrailRegel.actie == UPDATE ||
                auditTrailRegel.actie == DESTROY
            -> listOf(handleSingleChange(auditTrailRegel))
            else -> emptyList()
        }
    }

    private fun handleSingleChange(auditTrailRegel: AuditTrail): RESTHistorieRegel {
        val regel = RESTHistorieRegel(
            convertGegeven(
                auditTrailRegel.resource,
                auditTrailRegel.wijzigingen.nieuw ?: auditTrailRegel.wijzigingen.oud
            ),
            convertWaarde(
                auditTrailRegel.resource,
                auditTrailRegel.wijzigingen.oud,
                auditTrailRegel.resourceWeergave
            ),
            convertWaarde(
                auditTrailRegel.resource,
                auditTrailRegel.wijzigingen.nieuw,
                auditTrailRegel.resourceWeergave
            )
        )
        regel.datumTijd = auditTrailRegel.aanmaakdatum.toZonedDateTime()
        regel.toelichting = auditTrailRegel.toelichting
        regel.door = auditTrailRegel.gebruikersWeergave
        return regel
    }

    private fun convertGegeven(resource: String, obj: Any?): String = when (resource) {
        ROL -> (obj as? HashMap<*, *>)?.stringProperty(ROLTYPE)
            ?.let(URI::create)
            ?.let(URIUtil::parseUUIDFromResourceURI)
            ?.let(ztcClientService::readRoltype)
            ?.omschrijving
        ZAAKOBJECT -> (obj as? HashMap<*, *>)?.stringProperty(OBJECT_TYPE)
        else -> resource
    } ?: resource

    private fun convertWaarde(resource: String, obj: Any?, resourceWeergave: String): String? = (obj as? HashMap<*, *>)
        ?.let {
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
        }
}
