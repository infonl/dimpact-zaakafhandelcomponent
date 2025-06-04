/*
 * SPDX-FileCopyrightText: 2021 Atos, 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.app.note

import jakarta.inject.Inject
import jakarta.inject.Singleton
import jakarta.ws.rs.Consumes
import jakarta.ws.rs.DELETE
import jakarta.ws.rs.GET
import jakarta.ws.rs.PATCH
import jakarta.ws.rs.POST
import jakarta.ws.rs.Path
import jakarta.ws.rs.PathParam
import jakarta.ws.rs.Produces
import jakarta.ws.rs.core.MediaType
import nl.info.zac.app.note.converter.NoteConverter
import nl.info.zac.app.note.model.RestNote
import nl.info.zac.app.note.model.toNote
import nl.info.zac.note.NoteService
import nl.info.zac.policy.PolicyService
import nl.info.zac.policy.assertPolicy
import nl.info.zac.util.AllOpen
import nl.info.zac.util.NoArgConstructor
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
    private val policyService: PolicyService
) {
    @GET
    @Path("/zaken/{uuid}")
    fun listNotes(@PathParam("uuid") zaakUUID: UUID): List<RestNote> {
        assertPolicy(policyService.readNotitieRechten().lezen)
        return noteService.listNotesForZaak(zaakUUID)
            .map(noteConverter::toRestNote)
    }

    @POST
    fun createNote(restNote: RestNote): RestNote {
        assertPolicy(policyService.readNotitieRechten().wijzigen)
        val notitie = noteService.createNote(restNote.toNote())
        return noteConverter.toRestNote(notitie)
    }

    @PATCH
    fun updateNote(restNote: RestNote): RestNote {
        assertPolicy(policyService.readNotitieRechten().wijzigen)
        val updatedNotitie = noteService.updateNote(restNote.toNote())
        return noteConverter.toRestNote(updatedNotitie)
    }

    @DELETE
    @Path("{id}")
    fun deleteNote(@PathParam("id") id: Long) {
        assertPolicy(policyService.readNotitieRechten().wijzigen)
        noteService.deleteNote(id)
    }
}
