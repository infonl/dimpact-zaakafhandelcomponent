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

    beforeEach {
        checkUnnecessaryStub()
    }

    Given("A new valid note") {
        val note = createNote()
        every { entityManager.persist(note) } returns Unit

        When("createNote is called") {
            val result = noteService.createNote(note)

            Then("it should persist the note and return it") {
                result shouldBe note
                verify(exactly = 1) {
                    entityManager.persist(note)
                }
            }
        }
    }

    Given("A new note with a blank text field") {
        val note = createNote(
            text = ""
        )

        When("createNote is called") {
            val exception = shouldThrow<ConstraintViolationException> {
                noteService.createNote(note)
            }
            Then("it should throw an ConstraintViolationException") {
                exception.message shouldBe "text: must not be blank"
            }
        }
    }

    Given("An existing valid note") {
        val note = createNote()
        val updatedNote = createNote()
        every { entityManager.merge(note) } returns updatedNote

        When("createNote is called") {
            val result = noteService.updateNote(note)

            Then("it should merge the updated note and return it") {
                result shouldBe updatedNote
                verify(exactly = 1) {
                    entityManager.merge(note)
                }
            }
        }
    }

    Given("An existing note with a blank text field") {
        val note = createNote(
            text = ""
        )

        When("createNote is called") {
            val exception = shouldThrow<ConstraintViolationException> {
                noteService.updateNote(note)
            }
            Then("it should throw an ConstraintViolationException") {
                exception.message shouldBe "text: must not be blank"
            }
        }
    }

    Given("A note ID for an existing note") {
        val noteId = 123L
        val note = createNote(id = noteId)
        every { entityManager.find(Note::class.java, noteId) } returns note
        every { entityManager.remove(note) } just Runs

        When("createNote is called") {
            val result = noteService.deleteNote(noteId)

            Then("it should merge the updated note and return it") {
                verify(exactly = 1) {
                    entityManager.remove(note)
                }
            }
        }
    }
})
