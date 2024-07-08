package net.atos.zac.app.informatieobjecten.converter.historie

import net.atos.client.zgw.shared.model.audit.AuditTrailRegel
import net.atos.zac.app.audit.model.RESTHistorieActie
import net.atos.zac.app.audit.model.RESTHistorieRegel
import net.atos.zac.app.diff
import net.atos.zac.app.stringProperty

private const val STATUS = "status"
private const val INFORMATIEOBJECT = "informatieobject"
private const val GEBRUIKSRECHTEN = "gebruiksrechten"
private const val OMSCHRIJVING_VOORWAARDEN = "omschrijvingVoorwaarden"

private const val CREATE = "create"
private const val DESTROY = "destroy"
private const val UPDATE = "update"
private const val PARTIAL_UPDATE = "partial_update"

fun convertInformatieObjectRESTHistorieRegel(auditTrail: AuditTrailRegel): List<RESTHistorieRegel> {
    val old = auditTrail.wijzigingen.oud as? Map<*, *>
    val new = auditTrail.wijzigingen.nieuw as? Map<*, *>
    val actie = convertActie(auditTrail.resource, auditTrail.actie)
    return when {
        auditTrail.actie == PARTIAL_UPDATE &&
            old != null &&
            new != null
        -> old.diff(new).map { convertLine(auditTrail, actie, it) }

        auditTrail.actie == CREATE ||
            auditTrail.actie == UPDATE ||
            auditTrail.actie == DESTROY
        -> listOf(convertLine(auditTrail, old, new))

        else -> emptyList()
    }
}

private fun convertLine(
    auditTrail: AuditTrailRegel,
    actie: RESTHistorieActie?,
    change: Map.Entry<Any?, Pair<*, Any?>>
): RESTHistorieRegel =
    RESTHistorieRegel(
        change.key.toString(),
        convertValue(change.key, change.value.first),
        convertValue(change.key, change.value.second)
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

private fun convertValue(resource: String, obj: Map<*, *>, resourceWeergave: String?): String? = when (resource) {
    GEBRUIKSRECHTEN -> obj.stringProperty(OMSCHRIJVING_VOORWAARDEN)
    else -> resourceWeergave
}

private fun convertActie(resource: String?, action: String?): RESTHistorieActie? {
    val restHistorieActie = when {
        resource == INFORMATIEOBJECT && action == CREATE -> RESTHistorieActie.AANGEMAAKT
        resource == STATUS -> RESTHistorieActie.GEWIJZIGD
        action == CREATE -> RESTHistorieActie.GEKOPPELD
        action == DESTROY -> RESTHistorieActie.ONTKOPPELD
        action == UPDATE || action == PARTIAL_UPDATE -> RESTHistorieActie.GEWIJZIGD
        else -> null
    }
    return restHistorieActie
}

private fun convertValue(key: Any?, value: Any?) = when {
    key == STATUS && value is String -> "informatieobject.status.$value"
    else -> value?.toString()
}

private fun convertResource(key: Any?) = when (key) {
    "beginRegistratie" -> "registratiedatum"
    else -> key.toString()
}
