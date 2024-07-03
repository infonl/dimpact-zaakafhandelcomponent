/*
 * SPDX-FileCopyrightText: 2022 Atos, 2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.zac.app.informatieobjecten

import jakarta.enterprise.context.ApplicationScoped
import jakarta.enterprise.inject.Instance
import jakarta.inject.Inject
import jakarta.transaction.Transactional
import net.atos.client.zgw.drc.DrcClientService
import net.atos.client.zgw.drc.model.generated.EnkelvoudigInformatieObject
import net.atos.client.zgw.drc.model.generated.EnkelvoudigInformatieObjectWithLockRequest
import net.atos.client.zgw.drc.model.generated.OndertekeningRequest
import net.atos.client.zgw.drc.model.generated.SoortEnum
import net.atos.client.zgw.drc.model.generated.StatusEnum
import net.atos.zac.authentication.LoggedInUser
import net.atos.zac.enkelvoudiginformatieobject.EnkelvoudigInformatieObjectLockService
import net.atos.zac.enkelvoudiginformatieobject.model.EnkelvoudigInformatieObjectLock
import nl.lifely.zac.util.AllOpen
import nl.lifely.zac.util.NoArgConstructor
import java.time.LocalDate
import java.util.UUID

@ApplicationScoped
@Transactional
@NoArgConstructor
@AllOpen
class EnkelvoudigInformatieObjectUpdateService @Inject constructor(
    private val enkelvoudigInformatieObjectLockService: EnkelvoudigInformatieObjectLockService,
    private val drcClientService: DrcClientService,
    private val loggedInUserInstance: Instance<LoggedInUser>
) {

    companion object {
        private const val VERZEND_TOELICHTING_PREFIX = "Per post"
        private const val ONDERTEKENEN_TOELICHTING = "Door ondertekenen"
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
