/*
 * SPDX-FileCopyrightText: 2022 Atos, 2024 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.search.converter

import jakarta.inject.Inject
import net.atos.zac.flowable.task.FlowableTaskService
import net.atos.zac.flowable.task.TaakVariabelenService
import net.atos.zac.flowable.task.TaakVariabelenService.readTaskData
import net.atos.zac.flowable.task.TaakVariabelenService.readTaskInformation
import net.atos.zac.flowable.task.TaakVariabelenService.readZaakIdentificatie
import net.atos.zac.flowable.task.TaakVariabelenService.readZaaktypeUUID
import net.atos.zac.flowable.util.TaskUtil.getTaakStatus
import nl.info.client.zgw.util.extractUuid
import nl.info.client.zgw.zrc.ZrcClientService
import nl.info.client.zgw.ztc.ZtcClientService
import nl.info.zac.identity.IdentityService
import nl.info.zac.identity.model.getFullName
import nl.info.zac.search.model.zoekobject.TaakZoekObject
import nl.info.zac.search.model.zoekobject.ZoekObjectType
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
        return TaakZoekObject(
            id = id,
            type = ZoekObjectType.TAAK.name
        ).apply {
            naam = taskInfo.name
            creatiedatum = taskInfo.createTime
            toekenningsdatum = taskInfo.claimTime
            fataledatum = taskInfo.dueDate
            toelichting = taskInfo.description
            setStatus(getTaakStatus(taskInfo))
            zaaktypeIdentificatie = zaaktype.identificatie
            zaaktypeOmschrijving = zaaktype.omschrijving
            zaaktypeUuid = zaaktype.url.extractUuid().toString()
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
