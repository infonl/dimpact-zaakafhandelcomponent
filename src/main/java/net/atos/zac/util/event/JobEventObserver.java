/*
 * SPDX-FileCopyrightText: 2022 Atos
 * SPDX-License-Identifier: EUPL-1.2+
 */

package net.atos.zac.util.event;

import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.ObservesAsync;
import jakarta.inject.Inject;
import jakarta.inject.Named;

import nl.info.zac.signalering.ZaakTaskDueDateEmailNotificationService;

/**
 * This bean listens for job events and handles them by calling the corresponding service.
 */
@Named
@ApplicationScoped
public class JobEventObserver {
    private static final Logger LOG = Logger.getLogger(JobEventObserver.class.getName());

    private ZaakTaskDueDateEmailNotificationService zaakTaskDueDateEmailNotificationService;

    /**
     * Default no-arg constructor, required by Weld.
     */
    public JobEventObserver() {
    }

    @Inject
    public JobEventObserver(final ZaakTaskDueDateEmailNotificationService zaakTaskDueDateEmailNotificationService) {
        this.zaakTaskDueDateEmailNotificationService = zaakTaskDueDateEmailNotificationService;
    }

    public void onFire(final @ObservesAsync JobEvent event) {
        try {
            LOG.fine(() -> String.format("Job event ontvangen: %s", event.toString()));
            event.delay();
            if (Objects.requireNonNull(event.getObjectId()) == JobId.SIGNALERINGEN_JOB) {
                zaakTaskDueDateEmailNotificationService.sendDueDateEmailNotifications();
            }
        } catch (final Throwable ex) {
            LOG.log(Level.SEVERE, "asynchronous guard", ex);
        }
    }
}
