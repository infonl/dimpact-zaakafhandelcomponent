/*
 * SPDX-FileCopyrightText: 2022 Atos, 2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.zac.app.informatieobjecten

import jakarta.enterprise.context.ApplicationScoped
import jakarta.enterprise.inject.Instance
import jakarta.inject.Inject
import jakarta.transaction.Transactional
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import net.atos.client.zgw.drc.DrcClientService
import net.atos.client.zgw.drc.model.generated.EnkelvoudigInformatieObjectWithLockData
import net.atos.client.zgw.drc.model.generated.Ondertekening
import net.atos.zac.authentication.LoggedInUser
import net.atos.zac.enkelvoudiginformatieobject.EnkelvoudigInformatieObjectLockService
import net.atos.zac.enkelvoudiginformatieobject.model.EnkelvoudigInformatieObjectLock
import net.atos.zac.event.EventingService
import net.atos.zac.websocket.event.ScreenEventType
import nl.lifely.zac.util.AllOpen
import nl.lifely.zac.util.NoArgConstructor
import java.time.LocalDate
import java.util.UUID
import java.util.logging.Logger

@ApplicationScoped
@Transactional
@NoArgConstructor
@AllOpen
class EnkelvoudigInformatieObjectUpdateService @Inject constructor(
    private val enkelvoudigInformatieObjectLockService: EnkelvoudigInformatieObjectLockService,
    private val drcClientService: DrcClientService,
    private val loggedInUserInstance: Instance<LoggedInUser>,
    private val eventingService: EventingService,
) {

    companion object {
        private const val VERZEND_TOELICHTING_PREFIX = "Per post"
        private const val ONDERTEKENEN_TOELICHTING = "Door ondertekenen"

        private const val CHANGE_EVENT_RETRIES = 5
        private const val CHANGE_EVENT_WAIT_MILLISECONDS: Long = 200

        private val LOG = Logger.getLogger(EnkelvoudigInformatieObjectUpdateService::class.java.name)
    }

    fun verzendEnkelvoudigInformatieObject(uuid: UUID, verzenddatum: LocalDate?, toelichting: String?) =
        EnkelvoudigInformatieObjectWithLockData().apply {
            this.verzenddatum = verzenddatum
            updateEnkelvoudigInformatieObjectWithLockData(
                uuid,
                this,
                listOfNotNull(VERZEND_TOELICHTING_PREFIX, toelichting).joinToString(": ")
            )
        }

    fun ondertekenEnkelvoudigInformatieObject(informationObjectUUID: UUID) {
        EnkelvoudigInformatieObjectWithLockData().apply {
            ondertekening = Ondertekening().apply {
                soort = Ondertekening.SoortEnum.DIGITAAL
                datum = LocalDate.now()
            }
            status = EnkelvoudigInformatieObjectWithLockData.StatusEnum.DEFINITIEF
            updateEnkelvoudigInformatieObjectWithLockData(informationObjectUUID, this, ONDERTEKENEN_TOELICHTING)
            CoroutineScope(Dispatchers.IO).launch { sendChangeEvent(informationObjectUUID) }
        }
    }

    private suspend fun sendChangeEvent(informationObjectUUID: UUID) {
        repeat(CHANGE_EVENT_RETRIES) {
            if (drcClientService.readEnkelvoudigInformatieobject(informationObjectUUID).ondertekening?.datum != null) {
                // No notification by OpenZaak for lock/unlock, so we send this on our own
                eventingService.send(ScreenEventType.ENKELVOUDIG_INFORMATIEOBJECT.updated(informationObjectUUID))

                LOG.fine("Change event for for information object with UUID $informationObjectUUID sent")
                return
            }
            delay(CHANGE_EVENT_WAIT_MILLISECONDS)
        }
        LOG.warning(
            "Change event was not sent for information object with UUID $informationObjectUUID as " +
                "desired SIGNED state was not reached after ${CHANGE_EVENT_RETRIES * CHANGE_EVENT_WAIT_MILLISECONDS} ms"
        )
    }

    fun updateEnkelvoudigInformatieObjectWithLockData(
        uuid: UUID,
        update: EnkelvoudigInformatieObjectWithLockData,
        toelichting: String?
    ): EnkelvoudigInformatieObjectWithLockData {
        var tempLock: EnkelvoudigInformatieObjectLock? = null
        try {
            val enkelvoudigInformatieObjectLock = enkelvoudigInformatieObjectLockService.findLock(uuid)
                ?: enkelvoudigInformatieObjectLockService.createLock(uuid, loggedInUserInstance.get().id).also {
                    tempLock = it
                }
            update.lock = enkelvoudigInformatieObjectLock.lock
            return drcClientService.updateEnkelvoudigInformatieobject(uuid, update, toelichting)
        } finally {
            if (tempLock != null) {
                enkelvoudigInformatieObjectLockService.deleteLock(uuid)
            }
        }
    }
}
