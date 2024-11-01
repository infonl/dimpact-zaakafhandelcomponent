package net.atos.zac.zoeken.converter

import jakarta.inject.Inject
import net.atos.client.zgw.shared.util.URIUtil
import net.atos.client.zgw.shared.util.URIUtil.parseUUIDFromResourceURI
import net.atos.client.zgw.zrc.ZrcClientService
import net.atos.client.zgw.ztc.ZtcClientService
import net.atos.zac.flowable.task.FlowableTaskService
import net.atos.zac.flowable.task.TaakVariabelenService
import net.atos.zac.flowable.task.TaakVariabelenService.readTaskData
import net.atos.zac.flowable.task.TaakVariabelenService.readTaskInformation
import net.atos.zac.flowable.util.TaskUtil
import net.atos.zac.flowable.util.TaskUtil.getTaakStatus
import net.atos.zac.identity.IdentityService
import net.atos.zac.identity.model.getFullName
import net.atos.zac.zoeken.model.index.ZoekObjectType
import net.atos.zac.zoeken.model.zoekobject.TaakZoekObject
import org.flowable.identitylink.api.IdentityLinkInfo
import org.flowable.identitylink.api.IdentityLinkType

class TaakZoekObjectConverter @Inject constructor(
    val identityService: IdentityService,
    val flowableTaskService: FlowableTaskService,
    val ztcClientService: ZtcClientService,
    val zrcClientService: ZrcClientService
) : AbstractZoekObjectConverter<TaakZoekObject>() {

    override fun convert(id: String): TaakZoekObject {
        val taskInfo = flowableTaskService.readTask(id)
        val taakZoekObject = TaakZoekObject().apply {
            naam = taskInfo.name
            this.id = taskInfo.id
            type = ZoekObjectType.TAAK
            creatiedatum = taskInfo.createTime
            toekenningsdatum = taskInfo.claimTime
            fataledatum = taskInfo.dueDate
            toelichting = taskInfo.description
        }

        if (taskInfo.assignee != null) {
            val user = identityService.readUser(taskInfo.assignee)
            taakZoekObject.behandelaarNaam = user.getFullName()
            taakZoekObject.behandelaarGebruikersnaam = user.id
            taakZoekObject.isToegekend = true
        }

        taakZoekObject.status = getTaakStatus(taskInfo)
        val groupID = extractGroupId(taskInfo.identityLinks)
        if (groupID != null) {
            val group = identityService.readGroup(groupID)
            taakZoekObject.groepID = group.id
            taakZoekObject.groepNaam = group.name
        }

        val zaaktype = ztcClientService.readZaaktype(TaakVariabelenService.readZaaktypeUUID(taskInfo))
        taakZoekObject.zaaktypeIdentificatie = zaaktype.identificatie
        taakZoekObject.zaaktypeOmschrijving = zaaktype.omschrijving
        taakZoekObject.zaaktypeUuid = parseUUIDFromResourceURI(zaaktype.url).toString()

        val zaakUUID = TaakVariabelenService.readZaakUUID(taskInfo)
        taakZoekObject.zaakUUID = zaakUUID.toString()
        taakZoekObject.zaakIdentificatie = TaakVariabelenService.readZaakIdentificatie(taskInfo)

        val zaak = zrcClientService.readZaak(zaakUUID)
        taakZoekObject.zaakOmschrijving = zaak.omschrijving
        taakZoekObject.zaakToelichting = zaak.toelichting
        taakZoekObject.taakData = readTaskData(taskInfo).entries.map { "${it.key}|${it.value}" }
        taakZoekObject.taakInformatie = readTaskInformation(taskInfo).entries.map { "${it.key}|${it.value}" }

        return taakZoekObject
    }

    override fun supports(objectType: ZoekObjectType): Boolean = objectType == ZoekObjectType.TAAK

    private fun extractGroupId(identityLinks: List<IdentityLinkInfo>): String? =
        identityLinks.firstOrNull { it.type == IdentityLinkType.CANDIDATE }?.groupId
}
