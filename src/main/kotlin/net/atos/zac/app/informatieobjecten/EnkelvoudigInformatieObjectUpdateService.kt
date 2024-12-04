/*
 * SPDX-FileCopyrightText: 2022 Atos, 2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.zac.app.informatieobjecten

import com.google.common.util.concurrent.Striped
import jakarta.enterprise.context.ApplicationScoped
import jakarta.enterprise.inject.Instance
import jakarta.inject.Inject
import jakarta.transaction.Transactional
import net.atos.client.zgw.drc.DrcClientService
import net.atos.client.zgw.drc.model.generated.EnkelvoudigInformatieObject
import net.atos.client.zgw.drc.model.generated.EnkelvoudigInformatieObjectCreateLockRequest
import net.atos.client.zgw.drc.model.generated.EnkelvoudigInformatieObjectWithLockRequest
import net.atos.client.zgw.drc.model.generated.OndertekeningRequest
import net.atos.client.zgw.drc.model.generated.SoortEnum
import net.atos.client.zgw.drc.model.generated.StatusEnum
import net.atos.client.zgw.shared.ZGWApiService
import net.atos.client.zgw.zrc.model.Zaak
import net.atos.client.zgw.zrc.model.ZaakInformatieobject
import net.atos.zac.authentication.LoggedInUser
import net.atos.zac.configuratie.ConfiguratieService
import net.atos.zac.enkelvoudiginformatieobject.EnkelvoudigInformatieObjectLockService
import net.atos.zac.enkelvoudiginformatieobject.model.EnkelvoudigInformatieObjectLock
import net.atos.zac.flowable.task.FlowableTaskService
import net.atos.zac.flowable.task.TaakVariabelenService
import net.atos.zac.flowable.task.TaakVariabelenService.readTaskDocuments
import net.atos.zac.flowable.task.exception.TaskNotFoundException
import net.atos.zac.policy.PolicyService
import net.atos.zac.policy.PolicyService.assertPolicy
import net.atos.zac.util.UriUtil.uuidFromURI
import nl.lifely.zac.util.AllOpen
import nl.lifely.zac.util.NoArgConstructor
import java.time.LocalDate
import java.util.UUID

@ApplicationScoped
@Transactional
@NoArgConstructor
@AllOpen
@Suppress("LongParameterList")
class EnkelvoudigInformatieObjectUpdateService @Inject constructor(
    private val drcClientService: DrcClientService,
    private val enkelvoudigInformatieObjectLockService: EnkelvoudigInformatieObjectLockService,
    private val flowableTaskService: FlowableTaskService,
    private val loggedInUserInstance: Instance<LoggedInUser>,
    private val policyService: PolicyService,
    private val taakVariabelenService: TaakVariabelenService,
    private val zgwApiService: ZGWApiService
) {

    companion object {
        private const val VERZEND_TOELICHTING_PREFIX = "Per post"
        private const val ONDERTEKENEN_TOELICHTING = "Door ondertekenen"

        private const val CONCURRENCY_LEVEL = 20
        private val stripes = Striped.lazyWeakLock(CONCURRENCY_LEVEL)
    }

    fun createZaakInformatieobjectForZaak(
        zaak: Zaak,
        enkelvoudigInformatieObjectCreateLockRequest: EnkelvoudigInformatieObjectCreateLockRequest,
        taskId: String? = null,
        skipPolicyCheck: Boolean = false,
    ) = zgwApiService.createZaakInformatieobjectForZaak(
        zaak,
        enkelvoudigInformatieObjectCreateLockRequest,
        enkelvoudigInformatieObjectCreateLockRequest.titel,
        enkelvoudigInformatieObjectCreateLockRequest.beschrijving,
        ConfiguratieService.OMSCHRIJVING_VOORWAARDEN_GEBRUIKSRECHTEN
    ).also {
        if (taskId != null) {
            addZaakInformatieobjectToTaak(taskId, it, skipPolicyCheck)
        }
    }

    private fun addZaakInformatieobjectToTaak(
        taskId: String,
        zaakInformatieobject: ZaakInformatieobject,
        skipPolicyCheck: Boolean,
    ) {
        val lock = stripes.get(taskId).also { it.lock() }
        try {
            val task = flowableTaskService.findOpenTask(taskId)
                ?: throw TaskNotFoundException("No open task found with task id: '$taskId'")
            assertPolicy(skipPolicyCheck || policyService.readTaakRechten(task).toevoegenDocument)

            mutableListOf<UUID>().apply {
                addAll(readTaskDocuments(task))
                add(uuidFromURI(zaakInformatieobject.informatieobject))
            }.let {
                taakVariabelenService.setTaakdocumenten(task, it)
            }
        } finally {
            lock.unlock()
        }
    }

    fun verzendEnkelvoudigInformatieObject(uuid: UUID, verzenddatum: LocalDate?, toelichting: String?) =
        EnkelvoudigInformatieObjectWithLockRequest().apply {
            this.verzenddatum = verzenddatum
            updateEnkelvoudigInformatieObjectWithLockData(
                uuid,
                this,
                listOfNotNull(VERZEND_TOELICHTING_PREFIX, toelichting).joinToString(": ")
            )
        }

    fun ondertekenEnkelvoudigInformatieObject(uuid: UUID) {
        EnkelvoudigInformatieObjectWithLockRequest().apply {
            ondertekening = OndertekeningRequest().apply {
                soort = SoortEnum.DIGITAAL
                datum = LocalDate.now()
            }
            status = StatusEnum.DEFINITIEF
            updateEnkelvoudigInformatieObjectWithLockData(uuid, this, ONDERTEKENEN_TOELICHTING)
        }
    }

    fun updateEnkelvoudigInformatieObjectWithLockData(
        uuid: UUID,
        enkelvoudigInformatieObjectWithLockRequest: EnkelvoudigInformatieObjectWithLockRequest,
        toelichting: String?
    ): EnkelvoudigInformatieObject {
        var tempLock: EnkelvoudigInformatieObjectLock? = null
        try {
            val enkelvoudigInformatieObjectLock = enkelvoudigInformatieObjectLockService.findLock(uuid)
                ?: enkelvoudigInformatieObjectLockService.createLock(uuid, loggedInUserInstance.get().id).also {
                    tempLock = it
                }
            enkelvoudigInformatieObjectWithLockRequest.lock = enkelvoudigInformatieObjectLock.lock
            return drcClientService.updateEnkelvoudigInformatieobject(
                uuid,
                enkelvoudigInformatieObjectWithLockRequest,
                toelichting
            )
        } finally {
            if (tempLock != null) {
                enkelvoudigInformatieObjectLockService.deleteLock(uuid)
            }
        }
    }
}
