/*
 * SPDX-FileCopyrightText: 2021 Atos, 2023 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

package net.atos.zac.event;

import java.io.Serializable;
import java.time.Instant;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import jakarta.json.bind.annotation.JsonbTransient;

public abstract class AbstractEvent<TYPE, ID> implements Serializable {
    private static final Logger LOG = Logger.getLogger(AbstractEvent.class.getName());

    private long timestamp;

    private Opcode opcode;

    private ID objectId;

    @JsonbTransient
    private int delay;

    /**
     * Constructor for the sake of JAXB
     */
    public AbstractEvent() {
        super();
    }

    public AbstractEvent(final Opcode opcode, final ID objectId) {
        this.timestamp = Instant.now().getEpochSecond();
        this.opcode = opcode;
        this.objectId = objectId;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public Opcode getOpcode() {
        return opcode;
    }

    public abstract TYPE getObjectType();

    public ID getObjectId() {
        return objectId;
    }

    public void delay() {
        if (0 < delay) {
            try {
                TimeUnit.SECONDS.sleep(delay);
            } catch (InterruptedException e) {
                LOG.log(Level.WARNING, "Thread interrupted", e);
                Thread.currentThread().interrupt();
            }
        }
    }

    public void setDelay(final int seconds) {
        this.delay = seconds;
    }

    @Override
    public boolean equals(final Object obj) {
        // snel antwoord
        if (obj == this) {
            return true;
        }
        // gebruik getClass i.p.v. instanceof, maar dan wel met de null check
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        // cast en vergelijk
        final AbstractEvent<?, ?> other = (AbstractEvent<?, ?>) obj;
        if (getOpcode() != other.getOpcode()) {
            return false;
        }
        if (!getObjectType().equals(other.getObjectType())) {
            return false;
        }
        return getObjectId().equals(other.getObjectId());
    }

    @Override
    public int hashCode() {
        int result = getOpcode().hashCode();
        result = 31 * result + getObjectType().hashCode();
        result = 31 * result + getObjectId().hashCode();
        return result;
    }

    @Override
    public String toString() {
        final String className = 0 < delay ? getClass().getSimpleName() + String.format("+%ds", delay) : getClass().getSimpleName();
        return String.format("%s %s %s %s", className, getOpcode(), getObjectType(), getObjectId());
    }
}
