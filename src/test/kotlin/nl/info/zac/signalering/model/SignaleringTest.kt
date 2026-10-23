/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.signalering.model

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import nl.info.client.zgw.model.createZaak
import nl.info.test.org.flowable.task.api.createTestTask
import nl.info.zac.identity.model.createGroup
import nl.info.zac.identity.model.createUser

class SignaleringTest : BehaviorSpec({
    context("Setting the subject of a signalering") {
        given("a signalering with a zaak signaleringtype") {
            val signalering = Signalering().apply {
                type = createSignaleringType(SignaleringType.Type.ZAAK_OP_NAAM, SignaleringSubject.ZAAK)
            }
            val zaak = createZaak()

            `when`("a zaak is set as subject") {
                signalering.setSubject(zaak)

                then("the subject is the zaak UUID") {
                    signalering.subject shouldBe zaak.uuid.toString()
                    signalering.subjecttype shouldBe SignaleringSubject.ZAAK
                }
            }

            `when`("a task is set as subject") {
                val illegalArgumentException = shouldThrow<IllegalArgumentException> {
                    signalering.setSubject(createTestTask())
                }

                then("an exception is thrown because the signaleringtype expects a zaak") {
                    illegalArgumentException.message shouldBe "SignaleringType ZAAK_OP_NAAM expects a ZAAK-type subject"
                }
            }
        }
    }

    context("Setting the target of a signalering") {
        given("a signalering") {
            val signalering = Signalering()

            `when`("a group is set as target") {
                signalering.setTarget(createGroup(id = "fakeGroupName"))

                then("the target is the group name") {
                    signalering.targettype shouldBe SignaleringTarget.GROUP
                    signalering.target shouldBe "fakeGroupName"
                }
            }

            `when`("a user is set as target") {
                signalering.setTarget(createUser(id = "fakeUserId"))

                then("the target is the user id") {
                    signalering.targettype shouldBe SignaleringTarget.USER
                    signalering.target shouldBe "fakeUserId"
                }
            }
        }
    }

    context("Setting the detail of a signalering") {
        given("a signalering") {
            val signalering = Signalering()

            `when`("a signalering detail is set") {
                signalering.setDetailFromSignaleringDetail(SignaleringDetail.FATALE_DATUM)

                then("the detail is the name of the signalering detail") {
                    signalering.detail shouldBe "FATALE_DATUM"
                }
            }
        }
    }
})
