package net.atos.zac.app.zaken.converter

import jakarta.inject.Inject
import jakarta.json.bind.Jsonb
import jakarta.json.bind.JsonbBuilder
import net.atos.client.zgw.shared.util.URIUtil
import net.atos.client.zgw.zrc.model.Rol
import net.atos.client.zgw.zrc.model.generated.AuditTrail
import net.atos.client.zgw.ztc.ZtcClientService
import net.atos.zac.app.audit.model.Actie
import net.atos.zac.app.audit.model.RESTHistorieRegelV2
import java.net.URI

class ZaakRESTHistorieRegelConverter @Inject constructor(
    private val ztcClientService: ZtcClientService
) {
    companion object {
        val builder: Jsonb = JsonbBuilder.create()
    }
    fun convertZaakRESTHistorieRegel(auditTrailRegel: AuditTrail): RESTHistorieRegelV2? = convertActie(
        auditTrailRegel.resource,
        auditTrailRegel.actie
    )?.let {
        @Suppress("UsePropertyAccessSyntax")
        RESTHistorieRegelV2(
            convertGegeven(
                auditTrailRegel.resource,
                auditTrailRegel.wijzigingen.getNieuw() ?: auditTrailRegel.wijzigingen.getOud()
            ),
            it,
            getWaarde(auditTrailRegel.resource, auditTrailRegel.wijzigingen.getOud()),
            getWaarde(auditTrailRegel.resource, auditTrailRegel.wijzigingen.getNieuw()),
            auditTrailRegel.aanmaakdatum.toZonedDateTime(),
            auditTrailRegel.gebruikersWeergave,
            auditTrailRegel.toelichting
        )
    }

    private fun convertGegeven(resource: String, obj: Any?): String = when (resource) {
        "rol" -> ((obj as? HashMap<*, *>)?.get("roltype") as? String)
            ?.let(URI::create)
            ?.let(URIUtil::parseUUIDFromResourceURI)
            ?.let(ztcClientService::readRoltype)
            ?.omschrijving
        else -> resource
    } ?: resource

    private fun convertActie(resource: String, actie: String): Actie? = when {
        resource == "status" -> Actie.GEWIJZIGD
        resource == "zaak" && actie == "create" -> Actie.AANGEMAAKT
        else -> when (actie) {
            "create" -> Actie.GEKOPPELD
            "destroy" -> Actie.ONTKOPPELD
            "update" -> Actie.GEWIJZIGD
            "partial_update" -> Actie.GEWIJZIGD
            else -> null
        }
    }

    private fun getWaarde(resource: String, obj: Any?): String? = (obj as? HashMap<*, *>)?.let {
        when (resource) {
            "zaak" -> obj["identificatie"] as? String
            "rol" -> obj.getTypedValue(Rol::class.java).naam
            "zaakinformatieobject" -> obj["titel"] as? String
            "status" -> (obj["statustype"] as? String)
                ?.let(URI::create)
                ?.let(ztcClientService::readStatustype)
                ?.omschrijving
            else -> obj["url"] as? String
        }
    }

    private fun <T> HashMap<*, *>.getTypedValue(type: Class<T>): T =
        builder.toJson(this)
            .let { builder.fromJson(it, type) }
}
