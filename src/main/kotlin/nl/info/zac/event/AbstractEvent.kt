/*
 * SPDX-FileCopyrightText: 2021 Atos, 2023 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.event

import jakarta.json.bind.annotation.JsonbTransient
import java.io.Serializable
import java.time.Instant
import java.util.concurrent.TimeUnit
import java.util.logging.Level
import java.util.logging.Logger

@Suppress("SerialVersionUIDInSerializableClass")
abstract class AbstractEvent<TYPE, ID : Any> : Serializable {
    companion object {
        private val LOG = Logger.getLogger(AbstractEvent::class.java.name)
    }

    var timestamp: Long = 0
        private set

    lateinit var opcode: Opcode
        private set

    lateinit var objectId: ID
        private set

    @JsonbTransient
    private var delay: Int = 0

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

    fun delay() {
        if (delay > 0) {
            try {
                TimeUnit.SECONDS.sleep(delay.toLong())
            } catch (interruptedException: InterruptedException) {
                LOG.log(Level.WARNING, "Thread interrupted", interruptedException)
                Thread.currentThread().interrupt()
            }
        }
    }

    fun setDelay(seconds: Int) {
        delay = seconds
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

    override fun toString(): String {
        val className = if (delay > 0) "${javaClass.simpleName}+${delay}s" else javaClass.simpleName
        return "$className $opcode $objectType $objectId"
    }
}
