/*
 * SPDX-FileCopyrightText: 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.note

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import jakarta.persistence.EntityManager
import jakarta.validation.ConstraintViolationException

class NoteServiceTest : BehaviorSpec({
    val entityManager = mockk<EntityManager>()
    val noteService = NoteService(entityManager)

    Given("A valid note") {
        val note = createNote()
        every { entityManager.persist(note) } returns Unit

        When("createNote is called") {
            val result = noteService.createNote(note)

            Then("it should persist the note and return it") {
                result shouldBe note
            }
        }
    }

    Given("A note with a blank text field") {
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
})
