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
import nl.info.client.zgw.zrc.util.isZaakspecifiekGeautoriseerd
import nl.info.client.zgw.ztc.ZtcClientService
import nl.info.zac.identity.IdentityService
import nl.info.zac.identity.model.getFullName
import nl.info.client.zgw.zrc.model.generated.Zaak
import nl.info.zac.search.model.zoekobject.TaakZoekObject
import nl.info.zac.search.model.zoekobject.ZoekObjectType
import org.flowable.identitylink.api.IdentityLinkInfo
import org.flowable.identitylink.api.IdentityLinkType
import org.flowable.task.api.TaskInfo
import java.util.UUID

class TaakZoekObjectConverter @Inject constructor(
    private val identityService: IdentityService,
    private val flowableTaskService: FlowableTaskService,
    private val ztcClientService: ZtcClientService,
    private val zrcClientService: ZrcClientService
) : AbstractZoekObjectConverter<TaakZoekObject>() {

    override fun convert(id: String): TaakZoekObject =
        convert(id, zrcClientService::isZaakspecifiekGeautoriseerd)

    /**
     * Converts [id], looking up the zaakspecifiek geautoriseerd flag through [isZaakspecifiekGeautoriseerd]
     * instead of always calling [ZrcClientService.isZaakspecifiekGeautoriseerd] directly. Used by
     * [nl.info.zac.search.IndexingService] to memoize that lookup per zaak UUID across the taken of one zaak.
     */
    override fun convert(id: String, isZaakspecifiekGeautoriseerd: (UUID) -> Boolean): TaakZoekObject {
        val taskInfo = flowableTaskService.readTask(id)
        val zaak = zrcClientService.readZaak(TaakVariabelenService.readZaakUUID(taskInfo))
        return convert(id, taskInfo, zaak, isZaakspecifiekGeautoriseerd)
    }

    /**
     * Converts [id], using the already-retrieved [zaak] instead of reading it again via
     * [ZrcClientService.readZaak]. Used by [nl.info.zac.search.IndexingService]'s zaak-driven combined
     * reindex, which already retrieved [zaak] for its own [nl.info.zac.search.model.zoekobject.ZaakZoekObject]
     * conversion.
     */
    fun convert(id: String, zaak: Zaak, isZaakspecifiekGeautoriseerd: (UUID) -> Boolean): TaakZoekObject =
        convert(id, flowableTaskService.readTask(id), zaak, isZaakspecifiekGeautoriseerd)

    override fun supports(objectType: ZoekObjectType) = objectType == ZoekObjectType.TAAK

    private fun convert(
        id: String,
        taskInfo: TaskInfo,
        zaak: Zaak,
        isZaakspecifiekGeautoriseerd: (UUID) -> Boolean
    ): TaakZoekObject {
        // read from the task's own zaakUUID variable, not zaak.uuid, so that a taak's zaak reference
        // is always taken from the taak itself, even if [zaak] were ever supplied for a different zaak
        val zaakUUID = TaakVariabelenService.readZaakUUID(taskInfo)
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
            this.isZaakspecifiekGeautoriseerd = isZaakspecifiekGeautoriseerd(zaakUUID)
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
                    groepID = group.name
                    groepNaam = group.description
                }
            }
        }
    }

    private fun extractGroupId(identityLinks: List<IdentityLinkInfo>): String? =
        identityLinks.firstOrNull { it.type == IdentityLinkType.CANDIDATE }?.groupId
}
