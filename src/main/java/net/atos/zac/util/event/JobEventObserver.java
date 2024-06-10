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

import net.atos.zac.signalering.SignaleringJob;

/**
 * This bean listens for JobEvents and handles them by starting the relevant job.
 */
@Named
@ApplicationScoped
public class JobEventObserver {
    private static final Logger LOG = Logger.getLogger(JobEventObserver.class.getName());

    @Inject
    private SignaleringJob signaleringJob;

    public void onFire(final @ObservesAsync JobEvent event) {
        try {
            LOG.fine(() -> String.format("Job event ontvangen: %s", event.toString()));
            event.delay();
            if (Objects.requireNonNull(event.getObjectId()) == JobId.SIGNALERINGEN_JOB) {
                signaleringJob.signaleringenVerzenden();
            }
        } catch (final Throwable ex) {
            LOG.log(Level.SEVERE, "asynchronous guard", ex);
        }
    }
}
