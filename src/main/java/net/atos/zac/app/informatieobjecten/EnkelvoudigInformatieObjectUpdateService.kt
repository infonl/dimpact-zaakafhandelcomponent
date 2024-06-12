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
import net.atos.client.zgw.drc.model.generated.EnkelvoudigInformatieObjectWithLockData
import net.atos.client.zgw.drc.model.generated.Ondertekening
import net.atos.zac.authentication.LoggedInUser
import net.atos.zac.enkelvoudiginformatieobject.EnkelvoudigInformatieObjectLockService
import net.atos.zac.enkelvoudiginformatieobject.model.EnkelvoudigInformatieObjectLock
import org.apache.commons.lang3.ObjectUtils
import java.time.LocalDate
import java.util.UUID

@ApplicationScoped
@Transactional
class EnkelvoudigInformatieObjectUpdateService {
    @Inject
    private val enkelvoudigInformatieObjectLockService: EnkelvoudigInformatieObjectLockService? = null

    @Inject
    private val drcClientService: DrcClientService? = null

    @Inject
    private val loggedInUserInstance: Instance<LoggedInUser>? = null

    fun verzendEnkelvoudigInformatieObject(uuid: UUID?, verzenddatum: LocalDate?, toelichting: String?) {
        val update = EnkelvoudigInformatieObjectWithLockData()
        update.verzenddatum = verzenddatum
        updateEnkelvoudigInformatieObjectWithLockData(
            uuid, update, if (ObjectUtils.isNotEmpty(toelichting)) "%s: %s".formatted(
                VERZEND_TOELICHTING_PREFIX, toelichting
            ) else VERZEND_TOELICHTING_PREFIX
        )
    }

    fun ondertekenEnkelvoudigInformatieObject(uuid: UUID?) {
        val update = EnkelvoudigInformatieObjectWithLockData()
        val ondertekening = Ondertekening()
        ondertekening.soort = Ondertekening.SoortEnum.DIGITAAL
        ondertekening.datum = LocalDate.now()
        update.ondertekening = ondertekening
        update.status = EnkelvoudigInformatieObjectWithLockData.StatusEnum.DEFINITIEF
        updateEnkelvoudigInformatieObjectWithLockData(uuid, update, ONDERTEKENEN_TOELICHTING)
    }

    fun updateEnkelvoudigInformatieObjectWithLockData(
        uuid: UUID?,
        update: EnkelvoudigInformatieObjectWithLockData,
        toelichting: String?
    ): EnkelvoudigInformatieObjectWithLockData {
        var tempLock: EnkelvoudigInformatieObjectLock? = null
        try {
            val existingLock = enkelvoudigInformatieObjectLockService!!.findLock(uuid!!)
            if (existingLock != null) {
                update.lock = existingLock.lock
            } else {
                tempLock = enkelvoudigInformatieObjectLockService.createLock(uuid, loggedInUserInstance!!.get().id)
                update.lock = tempLock.lock
            }
            return drcClientService!!.updateEnkelvoudigInformatieobject(uuid, update, toelichting)
        } finally {
            if (tempLock != null) {
                enkelvoudigInformatieObjectLockService!!.deleteLock(uuid!!)
            }
        }
    }

    companion object {
        private const val VERZEND_TOELICHTING_PREFIX = "Per post"

        private const val ONDERTEKENEN_TOELICHTING = "Door ondertekenen"
    }
}
