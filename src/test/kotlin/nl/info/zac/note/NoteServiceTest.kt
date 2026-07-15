/*
 * SPDX-FileCopyrightText: 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.note

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.Runs
import io.mockk.checkUnnecessaryStub
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.verify
import jakarta.persistence.EntityManager
import jakarta.validation.ConstraintViolationException
import nl.info.zac.note.model.Note

class NoteServiceTest : BehaviorSpec({
    val entityManager = mockk<EntityManager>()
    val noteService = NoteService(entityManager)

    afterEach {
        checkUnnecessaryStub()
    }

    given("A new valid note") {
        val note = createNote()
        every { entityManager.persist(note) } returns Unit

        `when`("the note is created") {
            val result = noteService.createNote(note)

            then("it should persist the note and return it") {
                result shouldBe note
                verify(exactly = 1) {
                    entityManager.persist(note)
                }
            }
        }
    }

    given("A new note with a blank text field") {
        val note = createNote(
            text = ""
        )

        `when`("the note is created") {
            val exception = shouldThrow<ConstraintViolationException> {
                noteService.createNote(note)
            }
            then("it should throw an ConstraintViolationException") {
                exception.message shouldBe "text: must not be blank"
            }
        }
    }

    given("An existing valid note") {
        val note = createNote()
        val updatedNote = createNote()
        every { entityManager.merge(note) } returns updatedNote

        `when`("the note is updated") {
            val result = noteService.updateNote(note)

            then("it should merge the updated note and return it") {
                result shouldBe updatedNote
                verify(exactly = 1) {
                    entityManager.merge(note)
                }
            }
        }
    }

    given("An existing note with a blank text field") {
        val note = createNote(
            text = ""
        )

        `when`("the note is updated") {
            val exception = shouldThrow<ConstraintViolationException> {
                noteService.updateNote(note)
            }
            then("it should throw an ConstraintViolationException") {
                exception.message shouldBe "text: must not be blank"
            }
        }
    }

    given("A note ID for an existing note") {
        val noteId = 123L
        val note = createNote(id = noteId)
        every { entityManager.find(Note::class.java, noteId) } returns note
        every { entityManager.remove(note) } just Runs

        `when`("the note is deleted") {
            noteService.deleteNote(noteId)

            then("it should remove the note") {
                verify(exactly = 1) {
                    entityManager.remove(note)
                }
            }
        }
    }
})
