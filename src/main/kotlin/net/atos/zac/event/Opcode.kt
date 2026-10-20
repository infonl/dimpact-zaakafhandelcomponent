/*
 * SPDX-FileCopyrightText: 2021 Atos, 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.zac.event

import java.util.EnumSet

/**
 * Enumeration containing the operations as used by the [AbstractEvent].
 * Maps to opcode.ts
 */
enum class Opcode {
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

    companion object {
        /**
         * Returns a set of all opcodes defined in this enum except for [ANY].
         */
        fun any(): Set<Opcode> = EnumSet.complementOf(EnumSet.of(ANY))
    }
}
