/*
 * SPDX-FileCopyrightText: 2021 Atos, 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.app.note

import jakarta.enterprise.inject.Instance
import jakarta.inject.Inject
import jakarta.inject.Singleton
import jakarta.ws.rs.Consumes
import jakarta.ws.rs.DELETE
import jakarta.ws.rs.GET
import jakarta.ws.rs.NotFoundException
import jakarta.ws.rs.PATCH
import jakarta.ws.rs.POST
import jakarta.ws.rs.Path
import jakarta.ws.rs.PathParam
import jakarta.ws.rs.Produces
import jakarta.ws.rs.core.MediaType
import nl.info.client.zgw.zrc.ZrcClientService
import nl.info.zac.app.note.converter.NoteConverter
import nl.info.zac.app.note.model.RestNote
import nl.info.zac.app.note.model.toNote
import nl.info.zac.authentication.LoggedInUser
import nl.info.zac.event.EventingService
import nl.info.zac.note.NoteService
import nl.info.zac.note.model.Note
import nl.info.zac.policy.PolicyService
import nl.info.zac.policy.assertPolicy
import nl.info.zac.util.AllOpen
import nl.info.zac.util.NoArgConstructor
import nl.info.zac.websocket.event.ScreenEventType
import java.time.ZonedDateTime
import java.util.UUID

@Singleton
@Path("notities")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@NoArgConstructor
@AllOpen
class NoteRestService @Inject constructor(
    private val noteService: NoteService,
    private val noteConverter: NoteConverter,
    private val policyService: PolicyService,
    private val eventingService: EventingService,
    private val zrcClientService: ZrcClientService,
    private val loggedInUserInstance: Instance<LoggedInUser>
) {
    @GET
    @Path("/zaken/{uuid}")
    fun listNotes(@PathParam("uuid") zaakUUID: UUID): List<RestNote> {
        assertPolicy(policyService.readNotitieRechten().canLezen)
        assertCanReadZaak(zaakUUID)
        return noteService.listNotesForZaak(zaakUUID)
            .map(noteConverter::toRestNote)
    }

    @POST
    fun createNote(restNote: RestNote): RestNote {
        assertPolicy(policyService.readNotitieRechten().canWijzigen)
        assertCanReadZaak(restNote.zaakUUID)
        val notitie = noteService.createNote(restNote.toNote(employeeUsername = loggedInUserInstance.get().id))
        eventingService.send(ScreenEventType.ZAAK_NOTITIES.updated(notitie.zaakUUID))
        return noteConverter.toRestNote(notitie)
    }

    @PATCH
    fun updateNote(restNote: RestNote): RestNote {
        assertPolicy(policyService.readNotitieRechten().canWijzigen)
        val note = restNote.id?.let(noteService::findNote)
            ?: throw NotFoundException("Note with id '${restNote.id}' not found")
        assertIsAuthor(note)
        val updatedNotitie = note.apply {
            text = restNote.text
            dateTimeLastModified = ZonedDateTime.now()
        }.let(noteService::updateNote)
        eventingService.send(ScreenEventType.ZAAK_NOTITIES.updated(updatedNotitie.zaakUUID))
        return noteConverter.toRestNote(updatedNotitie)
    }

    @DELETE
    @Path("{id}")
    fun deleteNote(@PathParam("id") id: Long) {
        assertPolicy(policyService.readNotitieRechten().canWijzigen)
        val note = noteService.findNote(id) ?: return
        assertIsAuthor(note)
        noteService.deleteNote(id)?.let {
            eventingService.send(ScreenEventType.ZAAK_NOTITIES.updated(it.zaakUUID))
        }
    }

    private fun assertCanReadZaak(zaakUUID: UUID) =
        zrcClientService.readZaak(zaakUUID)
            .let { policyService.readZaakRechten(it, loggedInUserInstance.get()).canLezen }
            .let(::assertPolicy)

    private fun assertIsAuthor(note: Note) = assertPolicy(note.employeeUsername == loggedInUserInstance.get().id)
}
