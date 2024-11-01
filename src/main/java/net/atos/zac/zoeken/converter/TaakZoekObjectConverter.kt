package net.atos.zac.zoeken.converter

import jakarta.inject.Inject
import net.atos.client.zgw.shared.util.URIUtil.parseUUIDFromResourceURI
import net.atos.client.zgw.zrc.ZrcClientService
import net.atos.client.zgw.ztc.ZtcClientService
import net.atos.zac.flowable.task.FlowableTaskService
import net.atos.zac.flowable.task.TaakVariabelenService
import net.atos.zac.flowable.task.TaakVariabelenService.readTaskData
import net.atos.zac.flowable.task.TaakVariabelenService.readTaskInformation
import net.atos.zac.flowable.task.TaakVariabelenService.readZaakIdentificatie
import net.atos.zac.flowable.task.TaakVariabelenService.readZaaktypeUUID
import net.atos.zac.flowable.util.TaskUtil.getTaakStatus
import net.atos.zac.identity.IdentityService
import net.atos.zac.identity.model.getFullName
import net.atos.zac.zoeken.model.index.ZoekObjectType
import net.atos.zac.zoeken.model.zoekobject.TaakZoekObject
import org.flowable.identitylink.api.IdentityLinkInfo
import org.flowable.identitylink.api.IdentityLinkType

class TaakZoekObjectConverter @Inject constructor(
    private val identityService: IdentityService,
    private val flowableTaskService: FlowableTaskService,
    private val ztcClientService: ZtcClientService,
    private val zrcClientService: ZrcClientService
) : AbstractZoekObjectConverter<TaakZoekObject>() {

    override fun convert(id: String): TaakZoekObject {
        val taskInfo = flowableTaskService.readTask(id)
        val zaakUUID = TaakVariabelenService.readZaakUUID(taskInfo)
        val zaak = zrcClientService.readZaak(zaakUUID)
        val zaaktype = ztcClientService.readZaaktype(readZaaktypeUUID(taskInfo))
        return TaakZoekObject().apply {
            naam = taskInfo.name
            this.id = taskInfo.id
            type = ZoekObjectType.TAAK
            creatiedatum = taskInfo.createTime
            toekenningsdatum = taskInfo.claimTime
            fataledatum = taskInfo.dueDate
            toelichting = taskInfo.description
            status = getTaakStatus(taskInfo)
            zaaktypeIdentificatie = zaaktype.identificatie
            zaaktypeOmschrijving = zaaktype.omschrijving
            zaaktypeUuid = parseUUIDFromResourceURI(zaaktype.url).toString()
            this.zaakUUID = zaakUUID.toString()
            zaakIdentificatie = readZaakIdentificatie(taskInfo)
            zaakOmschrijving = zaak.omschrijving
            zaakToelichting = zaak.toelichting
            taakData = readTaskData(taskInfo).entries.map { "${it.key}|${it.value}" }
            taakInformatie = readTaskInformation(taskInfo).entries.map { "${it.key}|${it.value}" }
            taskInfo.assignee?.let {
                identityService.readUser(it).let { user ->
                    behandelaarNaam = user.getFullName()
                    behandelaarGebruikersnaam = user.id
                }
                isToegekend = true
            }
            extractGroupId(taskInfo.identityLinks)?.let {
                identityService.readGroup(it).let { group ->
                    groepID = group.id
                    groepNaam = group.name
                }
            }
        }
    }

    override fun supports(objectType: ZoekObjectType) = objectType == ZoekObjectType.TAAK

    private fun extractGroupId(identityLinks: List<IdentityLinkInfo>): String? =
        identityLinks.firstOrNull { it.type == IdentityLinkType.CANDIDATE }?.groupId
}
