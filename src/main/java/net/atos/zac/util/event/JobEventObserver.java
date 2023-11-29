/*
 * SPDX-FileCopyrightText: 2022 Atos
 * SPDX-License-Identifier: EUPL-1.2+
 */

package net.atos.zac.util.event;

import java.util.logging.Level;
import java.util.logging.Logger;

import jakarta.annotation.ManagedBean;
import jakarta.enterprise.event.ObservesAsync;
import jakarta.inject.Inject;

import net.atos.zac.signalering.SignaleringenJob;

/**
 * This bean listens for JobEvents and handles them by starting the relevant job.
 */
@ManagedBean
public class JobEventObserver {
    private static final Logger LOG = Logger.getLogger(JobEventObserver.class.getName());

    @Inject
    private SignaleringenJob signaleringenJob;

    public void onFire(final @ObservesAsync JobEvent event) {
        try {
            LOG.fine(() -> String.format("Job event ontvangen: %s", event.toString()));
            event.delay();
            switch (event.getObjectId()) {
                case SIGNALERINGEN_JOB -> signaleringenJob.signaleringenVerzenden();
            }
        } catch (final Throwable ex) {
            LOG.log(Level.SEVERE, "asynchronous guard", ex);
        }
    }
}
