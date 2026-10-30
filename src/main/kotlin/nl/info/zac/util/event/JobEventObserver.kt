/*
 * SPDX-FileCopyrightText: 2022 Atos, 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.util.event

import jakarta.enterprise.context.ApplicationScoped
import jakarta.enterprise.event.ObservesAsync
import jakarta.inject.Inject
import jakarta.inject.Named
import nl.info.zac.signalering.ZaakTaskDueDateEmailNotificationService
import nl.info.zac.util.AllOpen
import nl.info.zac.util.NoArgConstructor
import java.util.logging.Level
import java.util.logging.Logger

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
        private val LOG = Logger.getLogger(JobEventObserver::class.java.name)
    }

    @Suppress("TooGenericExceptionCaught")
    fun onFire(@ObservesAsync event: JobEvent) {
        try {
            LOG.fine { "Job event ontvangen: $event" }
            when (event.objectId) {
                JobId.SIGNALERINGEN_JOB -> zaakTaskDueDateEmailNotificationService.sendDueDateEmailNotifications()
            }
        } catch (throwable: Throwable) {
            LOG.log(Level.SEVERE, "asynchronous guard", throwable)
        }
    }
}
