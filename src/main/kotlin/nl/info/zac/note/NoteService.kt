/*
 * SPDX-FileCopyrightText: 2021 Atos, 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.note

import jakarta.enterprise.context.ApplicationScoped
import jakarta.inject.Inject
import jakarta.persistence.EntityManager
import jakarta.transaction.Transactional
import jakarta.transaction.Transactional.TxType.REQUIRED
import jakarta.transaction.Transactional.TxType.SUPPORTS
import net.atos.zac.util.ValidationUtil
import net.atos.zac.util.ValidationUtil.valideerObject
import nl.info.zac.note.model.Note
import nl.info.zac.note.model.Note.Companion.ZAAK_UUID_FIELD
import nl.info.zac.util.AllOpen
import nl.info.zac.util.NoArgConstructor
import java.util.UUID

@ApplicationScoped
@Transactional(SUPPORTS)
@NoArgConstructor
@AllOpen
class NoteService @Inject constructor(
    private val entityManager: EntityManager
) {
    @Transactional(REQUIRED)
    fun createNote(note: Note): Note {
        valideerObject(note)
        entityManager.persist(note)
        return note
    }

    fun listNotesForZaak(zaakUUID: UUID): List<Note> {
        val builder = entityManager.criteriaBuilder
        val query = builder.createQuery(Note::class.java)
        val root = query.from(Note::class.java)
        query.select(root).where(builder.equal(root.get<Any>(ZAAK_UUID_FIELD), zaakUUID))
        return entityManager.createQuery(query).getResultList()
    }

    @Transactional(REQUIRED)
    fun updateNote(note: Note): Note {
        valideerObject(note)
        return entityManager.merge(note)
    }

    @Transactional(REQUIRED)
    fun deleteNote(notitieId: Long) {
        val note = entityManager.find(Note::class.java, notitieId)
        entityManager.remove(note)
    }
}
