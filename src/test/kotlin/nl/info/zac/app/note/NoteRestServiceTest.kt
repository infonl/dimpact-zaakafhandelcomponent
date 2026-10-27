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
import io.mockk.slot
import io.mockk.verify
import jakarta.enterprise.inject.Instance
import nl.info.client.zgw.model.createZaak
import nl.info.client.zgw.zrc.ZrcClientService
import nl.info.zac.app.note.converter.NoteConverter
import nl.info.zac.app.note.exception.NoteNotFoundException
import nl.info.zac.authentication.LoggedInUser
import nl.info.zac.authentication.createLoggedInUser
import nl.info.zac.event.EventingService
import nl.info.zac.note.NoteService
import nl.info.zac.note.createNote
import nl.info.zac.note.model.Note
import nl.info.zac.policy.PolicyService
import nl.info.zac.policy.exception.PolicyException
import nl.info.zac.policy.output.createZaakRechten
import nl.info.zac.websocket.event.ScreenEvent
import nl.info.zac.websocket.event.ScreenEventType
import java.util.UUID

class NoteRestServiceTest : BehaviorSpec({
    val noteService = mockk<NoteService>()
    val noteConverter = mockk<NoteConverter>()
    val policyService = mockk<PolicyService>()
    val eventingService = mockk<EventingService>()
    val zrcClientService = mockk<ZrcClientService>()
    val loggedInUserInstance = mockk<Instance<LoggedInUser>>()
    val loggedInUser = createLoggedInUser(username = "fakeAuthor")
    val noteRestService = NoteRestService(
        noteService = noteService,
        noteConverter = noteConverter,
        policyService = policyService,
        eventingService = eventingService,
        zrcClientService = zrcClientService,
        loggedInUserInstance = loggedInUserInstance
    )

    afterEach {
        checkUnnecessaryStub()
    }

    given("Existing notes a logged in user with permission to read notes") {
        val zaakUUID = UUID.randomUUID()
        val notes = listOf(createNote(), createNote())
        val restNotes = listOf(createRestNote(), createRestNote())

        val zaak = createZaak(uuid = zaakUUID)
        every { loggedInUserInstance.get() } returns loggedInUser
        every { policyService.readNotitieRechten().canLezen } returns true
        every { zrcClientService.readZaak(zaakUUID) } returns zaak
        every { policyService.readZaakRechten(zaak, loggedInUser) } returns createZaakRechten(lezen = true)
        every { noteService.listNotesForZaak(zaakUUID) } returns notes
        notes.forEachIndexed { index, note ->
            every { noteConverter.toRestNote(note) } returns restNotes[index]
        }

        `when`("listNotes is called") {
            val result = noteRestService.listNotes(zaakUUID)

            then("it should return a list of RestNotes") {
                result shouldBe restNotes
            }
        }
    }

    given("New note input data with another user as author and a user with permissions to create notes") {
        val zaak = createZaak()
        val restNote = createRestNote(zaakUUID = zaak.uuid, employeeUsername = "fakeOtherUser")
        val createdNote = createNote()
        val createdRestNote = createRestNote()
        val noteSlot = slot<Note>()
        every { loggedInUserInstance.get() } returns loggedInUser
        every { policyService.readNotitieRechten().canWijzigen } returns true
        every { zrcClientService.readZaak(zaak.uuid) } returns zaak
        every { policyService.readZaakRechten(zaak, loggedInUser) } returns createZaakRechten(lezen = true)
        every { noteService.createNote(capture(noteSlot)) } returns createdNote
        every { noteConverter.toRestNote(createdNote) } returns createdRestNote
        every { eventingService.send(any<ScreenEvent>()) } just Runs

        `when`("createNote is called") {
            val result = noteRestService.createNote(restNote)

            then(
                "the note is created for the zaak with the logged-in user as author, and the zaak screens are notified"
            ) {
                result shouldBe createdRestNote
                with(noteSlot.captured) {
                    zaakUUID shouldBe zaak.uuid
                    employeeUsername shouldBe loggedInUser.username
                }
                verify(exactly = 1) {
                    noteService.createNote(any())
                    eventingService.send(ScreenEventType.ZAAK_NOTITIES.updated(createdNote.zaakUUID))
                }
            }
        }
    }

    given(
        "A note written by the logged-in user on a zaak the user may read, and update data that changes the author and the zaak"
    ) {
        val zaak = createZaak()
        val existingNote = createNote(id = 123L, zaakUUID = zaak.uuid, employeeUsername = loggedInUser.username)
        val originalZaakUUID = existingNote.zaakUUID
        val restNote = createRestNote(
            id = 123L,
            zaakUUID = UUID.randomUUID(),
            text = "fakeUpdatedText",
            employeeUsername = "fakeOtherUser"
        )
        val updatedRestNote = createRestNote()
        every { loggedInUserInstance.get() } returns loggedInUser
        every { policyService.readNotitieRechten().canWijzigen } returns true
        every { noteService.findNote(123L) } returns existingNote
        every { zrcClientService.readZaak(zaak.uuid) } returns zaak
        every { policyService.readZaakRechten(zaak, loggedInUser) } returns createZaakRechten(lezen = true)
        every { noteService.updateNote(existingNote) } returns existingNote
        every { noteConverter.toRestNote(existingNote) } returns updatedRestNote
        every { eventingService.send(any<ScreenEvent>()) } just Runs

        `when`("updateNote is called") {
            val result = noteRestService.updateNote(restNote)

            then("only the text is updated, the author and zaak are kept, and the zaak screens are notified") {
                result shouldBe updatedRestNote
                with(existingNote) {
                    text shouldBe "fakeUpdatedText"
                    employeeUsername shouldBe loggedInUser.username
                    zaakUUID shouldBe originalZaakUUID
                }
                verify(exactly = 1) {
                    noteService.updateNote(existingNote)
                    eventingService.send(ScreenEventType.ZAAK_NOTITIES.updated(originalZaakUUID))
                }
            }
        }
    }

    given("A note written by another user and a user with permissions to update notes") {
        val existingNote = createNote(id = 123L, employeeUsername = "fakeOtherUser")
        val restNote = createRestNote(id = 123L, employeeUsername = loggedInUser.username)
        every { loggedInUserInstance.get() } returns loggedInUser
        every { policyService.readNotitieRechten().canWijzigen } returns true
        every { noteService.findNote(123L) } returns existingNote

        `when`("updateNote is called") {
            val exception = shouldThrow<PolicyException> {
                noteRestService.updateNote(restNote)
            }

            then("it should throw a PolicyException and not update the note") {
                exception shouldNotBe null
                verify(exactly = 0) {
                    noteService.updateNote(any())
                }
            }
        }

        `when`("deleteNote is called") {
            val exception = shouldThrow<PolicyException> {
                noteRestService.deleteNote(123L)
            }

            then("it should throw a PolicyException and not delete the note") {
                exception shouldNotBe null
                verify(exactly = 0) {
                    noteService.deleteNote(any())
                }
            }
        }
    }

    given("Update data for a note that does not exist") {
        val restNote = createRestNote(id = 456L)
        every { policyService.readNotitieRechten().canWijzigen } returns true
        every { noteService.findNote(456L) } returns null

        `when`("updateNote is called") {
            val exception = shouldThrow<NoteNotFoundException> {
                noteRestService.updateNote(restNote)
            }

            then("it should throw a NoteNotFoundException") {
                exception.message shouldBe "Note with id '456' not found"
            }
        }
    }

    given("A user with permission to read notes but without permission to read the zaak") {
        val zaak = createZaak()
        every { loggedInUserInstance.get() } returns loggedInUser
        every { policyService.readNotitieRechten().canLezen } returns true
        every { zrcClientService.readZaak(zaak.uuid) } returns zaak
        every { policyService.readZaakRechten(zaak, loggedInUser) } returns createZaakRechten(lezen = false)

        `when`("listNotes is called") {
            val exception = shouldThrow<PolicyException> {
                noteRestService.listNotes(zaak.uuid)
            }

            then("it should throw a PolicyException and not list the notes") {
                exception shouldNotBe null
                verify(exactly = 0) {
                    noteService.listNotesForZaak(any())
                }
            }
        }
    }

    given("A user with permission to update notes but without permission to read the zaak") {
        val zaak = createZaak()
        every { loggedInUserInstance.get() } returns loggedInUser
        every { policyService.readNotitieRechten().canWijzigen } returns true
        every { zrcClientService.readZaak(zaak.uuid) } returns zaak
        every { policyService.readZaakRechten(zaak, loggedInUser) } returns createZaakRechten(lezen = false)

        `when`("createNote is called") {
            val exception = shouldThrow<PolicyException> {
                noteRestService.createNote(createRestNote(zaakUUID = zaak.uuid))
            }

            then("it should throw a PolicyException and not create the note") {
                exception shouldNotBe null
                verify(exactly = 0) {
                    noteService.createNote(any())
                }
            }
        }
    }

    given("A note written by the logged-in user on a zaak the user may read, and a user with permissions to update notes") {
        val nodeId = 123L
        val zaak = createZaak()
        val deletedNote = createNote(zaakUUID = zaak.uuid, employeeUsername = loggedInUser.username)
        every { loggedInUserInstance.get() } returns loggedInUser
        every { policyService.readNotitieRechten().canWijzigen } returns true
        every { noteService.findNote(nodeId) } returns deletedNote
        every { zrcClientService.readZaak(zaak.uuid) } returns zaak
        every { policyService.readZaakRechten(zaak, loggedInUser) } returns createZaakRechten(lezen = true)
        every { noteService.deleteNote(nodeId) } returns deletedNote
        every { eventingService.send(any<ScreenEvent>()) } just Runs

        `when`("deleteNote is called") {
            noteRestService.deleteNote(nodeId)

            then("the note is deleted and the zaak screens are notified") {
                verify(exactly = 1) {
                    noteService.deleteNote(nodeId)
                    eventingService.send(ScreenEventType.ZAAK_NOTITIES.updated(deletedNote.zaakUUID))
                }
            }
        }
    }

    given("A note written by the logged-in user on a zaak the user may not read") {
        val zaak = createZaak()
        val existingNote = createNote(id = 123L, zaakUUID = zaak.uuid, employeeUsername = loggedInUser.username)
        val restNote = createRestNote(id = 123L, zaakUUID = zaak.uuid, employeeUsername = loggedInUser.username)
        every { loggedInUserInstance.get() } returns loggedInUser
        every { policyService.readNotitieRechten().canWijzigen } returns true
        every { noteService.findNote(123L) } returns existingNote
        every { zrcClientService.readZaak(zaak.uuid) } returns zaak
        every { policyService.readZaakRechten(zaak, loggedInUser) } returns createZaakRechten(lezen = false)

        `when`("updateNote is called") {
            val exception = shouldThrow<PolicyException> {
                noteRestService.updateNote(restNote)
            }

            then("it should throw a PolicyException and not update the note") {
                exception shouldNotBe null
                verify(exactly = 0) {
                    noteService.updateNote(any())
                }
            }
        }

        `when`("deleteNote is called") {
            val exception = shouldThrow<PolicyException> {
                noteRestService.deleteNote(123L)
            }

            then("it should throw a PolicyException and not delete the note") {
                exception shouldNotBe null
                verify(exactly = 0) {
                    noteService.deleteNote(any())
                }
            }
        }
    }

    given("A note ID for a note that no longer exists") {
        val nodeId = 456L
        every { policyService.readNotitieRechten().canWijzigen } returns true
        every { noteService.findNote(nodeId) } returns null

        `when`("deleteNote is called") {
            noteRestService.deleteNote(nodeId)

            then("nothing is deleted and no event is sent") {
                verify(exactly = 0) {
                    noteService.deleteNote(any())
                    eventingService.send(any<ScreenEvent>())
                }
            }
        }
    }

    given("A user who does not have permission to read notes") {
        val zaakUUID = UUID.randomUUID()
        every { policyService.readNotitieRechten().canLezen } returns false

        `when`("listNotes is called") {
            val exception = shouldThrow<PolicyException> {
                noteRestService.listNotes(zaakUUID)
            }
            then("it should throw a PolicyException") {
                exception shouldNotBe null
            }
        }
    }

    given("A user who does not have permission to create notes") {
        val restNote = createRestNote()
        every { policyService.readNotitieRechten().canWijzigen } returns false

        `when`("createNote is called") {
            val exception = shouldThrow<PolicyException> {
                noteRestService.createNote(restNote)
            }

            then("it should throw a PolicyException") {
                exception shouldNotBe null
            }
        }

        `when`("deleteNote is called") {
            val exception = shouldThrow<PolicyException> {
                noteRestService.deleteNote(123L)
            }

            then("it should throw a PolicyException") {
                exception shouldNotBe null
            }
        }
    }
})
