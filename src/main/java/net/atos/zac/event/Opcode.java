/*
 * SPDX-FileCopyrightText: 2021 Atos
 * SPDX-License-Identifier: EUPL-1.2+
 */

package net.atos.zac.event;

import java.util.EnumSet;
import java.util.Set;

/**
 * Enumeration containing the operations as used by the {@link AbstractEvent}.
 * Maps to opcode.ts
 */
public enum Opcode {

    /**
     * Indication that the mentioned object has been added.
     */
    CREATED,

    /**
     * Indication that the mentioned object has been updated.
     */
    UPDATED,

    /**
     * Indication that the mentioned object has been deleted.
     */
    DELETED,

    /**
     * Indication that something has happened to the mentioned object.
     */
    ANY,

    /**
     * Indication that the mentioned object was skipped during handling of the event.
     */
    SKIPPED;

    /**
     * Returns a set of all opcodes defined in this enum except for {@link #ANY}.
     */
    public static Set<Opcode> any() {
        return EnumSet.complementOf(EnumSet.of(ANY));
    }
}
