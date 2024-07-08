package net.atos.zac.app.zaken.converter.historie

import net.atos.client.zgw.shared.model.audit.AuditTrailRegel
import net.atos.zac.app.audit.model.RESTHistorieActie
import net.atos.zac.app.audit.model.RESTHistorieRegel
import net.atos.zac.app.diff
import net.atos.zac.app.stringProperty

private const val CREATE = "create"
private const val DESTROY = "destroy"
private const val UPDATE = "update"
private const val PARTIAL_UPDATE = "partial_update"

private const val BESLUIT = "besluit"
private const val STATUS = "status"
private const val BESLUITINFORMATIEOBJECT = "besluitinformatieobject"
private const val TITEL = "titel"

fun convertBesluitRESTHistorieRegel(auditTrail: AuditTrailRegel): List<RESTHistorieRegel> {
    val old = auditTrail.wijzigingen.oud as? Map<*, *>
    val new = auditTrail.wijzigingen.nieuw as? Map<*, *>
    val actie = convertActie(auditTrail.resource, auditTrail.actie)
    return when {
        auditTrail.actie == PARTIAL_UPDATE &&
            old != null &&
            new != null
        -> old.diff(new).map { convertLine(auditTrail, actie, it) }
        else -> listOf(convertLine(auditTrail, old, new))
    }
}

private fun convertActie(resource: String?, action: String?): RESTHistorieActie? = when {
    resource == BESLUIT && action == CREATE -> RESTHistorieActie.AANGEMAAKT
    resource == STATUS -> RESTHistorieActie.GEWIJZIGD
    action == CREATE -> RESTHistorieActie.GEKOPPELD
    action == DESTROY -> RESTHistorieActie.ONTKOPPELD
    action == UPDATE || action == PARTIAL_UPDATE -> RESTHistorieActie.GEWIJZIGD
    else -> null
}

private fun convertLine(
    auditTrail: AuditTrailRegel,
    actie: RESTHistorieActie?,
    change: Map.Entry<Any?, Pair<*, Any?>>
): RESTHistorieRegel =
    RESTHistorieRegel(
        change.key.toString(),
        change.value.first?.toString(),
        change.value.second?.toString()
    ).apply {
        datumTijd = auditTrail.aanmaakdatum
        door = auditTrail.gebruikersWeergave
        toelichting = auditTrail.toelichting
        this.actie = actie
    }

private fun convertLine(auditTrail: AuditTrailRegel, old: Map<*, *>?, new: Map<*, *>?): RESTHistorieRegel =
    RESTHistorieRegel(
        auditTrail.resource,
        old?.let { convertValue(auditTrail.resource, it, auditTrail.resourceWeergave) },
        new?.let { convertValue(auditTrail.resource, it, auditTrail.resourceWeergave) }
    ).apply {
        datumTijd = auditTrail.aanmaakdatum
        toelichting = auditTrail.toelichting
        door = auditTrail.gebruikersWeergave
        actie = convertActie(auditTrail.resource, auditTrail.actie)
    }

private fun convertValue(resource: String, obj: Map<*, *>, resourceWeergave: String?): String? =
    when (resource) {
        BESLUITINFORMATIEOBJECT -> obj.stringProperty(TITEL)
        else -> resourceWeergave
    }
