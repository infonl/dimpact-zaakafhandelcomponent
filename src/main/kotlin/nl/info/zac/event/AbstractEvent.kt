/*
 * SPDX-FileCopyrightText: 2021 Atos, 2023 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.event

import java.io.Serializable
import java.time.Instant

@Suppress("SerialVersionUIDInSerializableClass")
abstract class AbstractEvent<TYPE, ID : Any> : Serializable {
    var timestamp: Long = 0
        private set

    lateinit var opcode: Opcode
        private set

    lateinit var objectId: ID
        private set

    abstract val objectType: TYPE

    /**
     * Constructor for the sake of JAXB
     */
    constructor()

    constructor(opcode: Opcode, objectId: ID) {
        this.timestamp = Instant.now().epochSecond
        this.opcode = opcode
        this.objectId = objectId
    }

    override fun equals(other: Any?): Boolean {
        if (other === this) return true
        // use getClass instead of instanceof, so that events of different subclasses are never equal
        if (other == null || javaClass != other.javaClass) return false
        other as AbstractEvent<*, *>
        return opcode == other.opcode && objectType == other.objectType && objectId == other.objectId
    }

    override fun hashCode(): Int {
        var result = opcode.hashCode()
        result = 31 * result + objectType.hashCode()
        result = 31 * result + objectId.hashCode()
        return result
    }

    override fun toString() = "${javaClass.simpleName} $opcode $objectType $objectId"
}
