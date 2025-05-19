/*
 * SPDX-FileCopyrightText: 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.app.note

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.mockk.Runs
import io.mockk.checkUnnecessaryStub
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.verify
import nl.info.zac.app.note.converter.NoteConverter
import nl.info.zac.note.NoteService
import nl.info.zac.note.createNote
import nl.info.zac.policy.PolicyService
import nl.info.zac.policy.exception.PolicyException
import java.util.UUID

class NoteRestServiceTest : BehaviorSpec({
    val noteService = mockk<NoteService>()
    val noteConverter = mockk<NoteConverter>()
    val policyService = mockk<PolicyService>()
    val noteRestService = NoteRestService(
        noteService = noteService,
        noteConverter = noteConverter,
        policyService = policyService
    )

    beforeEach {
        checkUnnecessaryStub()
    }

    Given("Existing notes a logged in user with permission to read notes") {
        val zaakUUID = UUID.randomUUID()
        val notes = listOf(createNote(), createNote())
        val restNotes = listOf(createRestNote(), createRestNote())

        every { policyService.readNotitieRechten().lezen } returns true
        every { noteService.listNotesForZaak(zaakUUID) } returns notes
        notes.forEachIndexed { index, note ->
            every { noteConverter.toRestNote(note) } returns restNotes[index]
        }

        When("listNotes is called") {
            val result = noteRestService.listNotes(zaakUUID)

            Then("it should return a list of RestNotes") {
                result shouldBe restNotes
            }
        }
    }

    Given("New note input data and a user with permissions to create notes") {
        val restNote = createRestNote()
        val createdNote = createNote()
        val createdRestNote = createRestNote()
        every { policyService.readNotitieRechten().wijzigen } returns true
        every { noteService.createNote(any()) } returns createdNote
        every { noteConverter.toRestNote(createdNote) } returns createdRestNote

        When("createNote is called") {
            val result = noteRestService.createNote(restNote)

            Then("it should return the created RestNote") {
                result shouldBe createdRestNote
                verify(exactly = 1) {
                    noteService.createNote(any())
                }
            }
        }
    }

    Given("An existing node and node update data and a user with permissions to update notes") {
        val restNote = createRestNote()
        val updatedNote = createNote()
        val updatedRestNote = createRestNote()

        every { policyService.readNotitieRechten().wijzigen } returns true
        every { noteService.updateNote(any()) } returns updatedNote
        every { noteConverter.toRestNote(updatedNote) } returns updatedRestNote

        When("updateNote is called") {
            val result = noteRestService.updateNote(restNote)

            Then("it should return the updated RestNote") {
                result shouldBe updatedRestNote
                verify(exactly = 1) {
                    noteService.updateNote(any())
                }
            }
        }
    }

    Given("An existing node and a user with permissions to update notes") {
        val nodeId = 123L
        every { policyService.readNotitieRechten().wijzigen } returns true
        every { noteService.deleteNote(nodeId) } just Runs

        When("deleteNode is called") {
            noteRestService.deleteNote(nodeId)

            Then("the node is deleted") {
                verify(exactly = 1) {
                    noteService.deleteNote(nodeId)
                }
            }
        }
    }

    Given("A user who does not have permission to read notes") {
        val zaakUUID = UUID.randomUUID()
        every { policyService.readNotitieRechten().lezen } returns false

        When("listNotes is called") {
            val exception = shouldThrow<PolicyException> {
                noteRestService.listNotes(zaakUUID)
            }
            Then("it should throw a PolicyException") {
                exception shouldNotBe null
            }
        }
    }

    Given("A user who does not have permission to create notes") {
        val restNote = createRestNote()
        every { policyService.readNotitieRechten().wijzigen } returns false

        When("createNote is called") {
            val exception = shouldThrow<PolicyException> {
                noteRestService.createNote(restNote)
            }

            Then("it should throw a PolicyException") {
                exception shouldNotBe null
            }
        }

        When("deleteNote is called") {
            val exception = shouldThrow<PolicyException> {
                noteRestService.deleteNote(123L)
            }

            Then("it should throw a PolicyException") {
                exception shouldNotBe null
            }
        }
    }
})
