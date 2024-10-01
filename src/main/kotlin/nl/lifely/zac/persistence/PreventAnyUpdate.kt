package nl.lifely.zac.persistence

import jakarta.persistence.PrePersist
import jakarta.persistence.PreRemove
import jakarta.persistence.PreUpdate

/**
 * EntityListeners class to prevent any update of the entity
 */
class PreventAnyUpdate {
    @PrePersist
    fun onPrePersist(o: Any?) {
        throw IllegalStateException("Attempt to persist an entity of type " + o?.javaClass)
    }

    @PreUpdate
    fun onPreUpdate(o: Any?) {
        throw IllegalStateException("Attempt to update an entity of type " + o?.javaClass)
    }

    @PreRemove
    fun onPreRemove(o: Any?) {
        throw IllegalStateException("Attempt to remove an entity of type " + o?.javaClass)
    }
}
