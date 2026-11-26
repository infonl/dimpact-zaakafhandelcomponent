/*
 * SPDX-FileCopyrightText: 2022 Atos, 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.util.event

import jakarta.enterprise.context.ApplicationScoped
import jakarta.enterprise.event.ObservesAsync
import jakarta.inject.Inject
import jakarta.inject.Named
import nl.info.zac.log.ZacLogger
import nl.info.zac.signalering.ZaakTaskDueDateEmailNotificationService
import nl.info.zac.util.AllOpen
import nl.info.zac.util.NoArgConstructor
import nl.info.zac.util.log.JobEventHandlingFailed
import nl.info.zac.util.log.JobEventReceived

/**
 * This bean listens for job events and handles them by calling the corresponding service.
 */
@Named
@ApplicationScoped
@NoArgConstructor
@AllOpen
class JobEventObserver @Inject constructor(
    private val zaakTaskDueDateEmailNotificationService: ZaakTaskDueDateEmailNotificationService
) {
    companion object {
        private val LOG = ZacLogger.getLogger(JobEventObserver::class)
    }

    @Suppress("TooGenericExceptionCaught")
    fun onFire(@ObservesAsync event: JobEvent) {
        try {
            LOG.fine { JobEventReceived(jobEvent = event.toString()) }
            when (event.objectId) {
                JobId.SIGNALERINGEN_JOB -> zaakTaskDueDateEmailNotificationService.sendDueDateEmailNotifications()
            }
        } catch (throwable: Throwable) {
            LOG.severe(JobEventHandlingFailed, throwable)
        }
    }
}
