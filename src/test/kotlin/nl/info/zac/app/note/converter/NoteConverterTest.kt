/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.app.note.converter

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.checkUnnecessaryStub
import io.mockk.every
import io.mockk.mockk
import jakarta.enterprise.inject.Instance
import nl.info.zac.authentication.LoggedInUser
import nl.info.zac.authentication.createLoggedInUser
import nl.info.zac.identity.IdentityService
import nl.info.zac.identity.model.createUser
import nl.info.zac.note.createNote

class NoteConverterTest : BehaviorSpec({
    val identityService = mockk<IdentityService>()
    val loggedInUserInstance = mockk<Instance<LoggedInUser>>()
    val noteConverter = NoteConverter(identityService, loggedInUserInstance)

    afterEach {
        checkUnnecessaryStub()
    }

    context("toRestNote") {
        given("a Note and the logged-in user is the note's author") {
            val note = createNote(employeeUsername = "fakeAuthor")
            val author = createUser(
                id = "fakeAuthor",
                firstName = "fakeFirstName",
                lastName = "fakeLastName",
                fullName = "fakeFirstName fakeLastName"
            )
            val loggedInUser = createLoggedInUser(id = "fakeAuthor")
            every { identityService.readUser("fakeAuthor") } returns author
            every { loggedInUserInstance.get() } returns loggedInUser

            `when`("toRestNote is called") {
                val result = noteConverter.toRestNote(note)

                then("updatingAllowed is true because logged-in user is the author") {
                    result.updatingAllowed shouldBe true
                }

                then("employeeFullname is first + last name") {
                    result.employeeFullname shouldBe "fakeFirstName fakeLastName"
                }

                then("other fields are mapped correctly") {
                    result.id shouldBe note.id
                    result.zaakUUID shouldBe note.zaakUUID
                    result.text shouldBe note.text
                    result.employeeUsername shouldBe "fakeAuthor"
                }
            }
        }

        given("a Note and the logged-in user is a different user") {
            val note = createNote(employeeUsername = "fakeAuthor")
            val author = createUser(
                id = "fakeAuthor",
                firstName = "fakeFirstName",
                lastName = "fakeLastName",
                fullName = "fakeFirstName fakeLastName"
            )
            val loggedInUser = createLoggedInUser(id = "fakeOtherUserId")
            every { identityService.readUser("fakeAuthor") } returns author
            every { loggedInUserInstance.get() } returns loggedInUser

            `when`("toRestNote is called") {
                val result = noteConverter.toRestNote(note)

                then("updatingAllowed is false") {
                    result.updatingAllowed shouldBe false
                }
            }
        }
    }
})
