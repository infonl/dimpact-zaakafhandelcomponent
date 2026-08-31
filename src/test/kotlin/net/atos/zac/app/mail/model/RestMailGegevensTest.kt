/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.zac.app.mail.model

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import nl.info.client.zgw.drc.model.generated.VertrouwelijkheidaanduidingEnum
import nl.info.zac.app.shared.RestVertrouwelijkheidaanduiding

class RestMailGegevensTest : BehaviorSpec({
    val gemeenteNaam = "fakeGemeenteNaam"

    context("toMailGegevens extension function") {
        given("a RestMailGegevens with a vertrouwelijkheidaanduiding") {
            val restMailGegevens = createRestMailGegevens(
                verzender = "verzender@example.com",
                ontvanger = "ontvanger@example.com",
                vertrouwelijkheidaanduiding = RestVertrouwelijkheidaanduiding.GEHEIM
            ).apply {
                replyTo = "replyto@example.com"
                bijlagen = "fakeAttachmentUuid"
                createDocumentFromMail = true
            }

            `when`("toMailGegevens is called") {
                val mailGegevens = restMailGegevens.toMailGegevens(gemeenteNaam)

                then("the domain MailGegevens carries the equivalent ZGW confidentiality enum value") {
                    mailGegevens.vertrouwelijkheidaanduiding shouldBe VertrouwelijkheidaanduidingEnum.GEHEIM
                }

                and("the remaining fields are mapped correctly") {
                    mailGegevens.from.email shouldBe "verzender@example.com"
                    mailGegevens.from.name shouldBe gemeenteNaam
                    mailGegevens.to.email shouldBe "ontvanger@example.com"
                    mailGegevens.replyTo?.email shouldBe "replyto@example.com"
                    mailGegevens.replyTo?.name shouldBe gemeenteNaam
                    mailGegevens.subject shouldBe "fakeOnderwerp"
                    mailGegevens.body shouldBe "fakeBody"
                    mailGegevens.attachments shouldBe listOf("fakeAttachmentUuid")
                    mailGegevens.isCreateDocumentFromMail shouldBe true
                }
            }
        }

        given("a RestMailGegevens without a replyTo address") {
            val restMailGegevens = createRestMailGegevens()

            `when`("toMailGegevens is called") {
                val mailGegevens = restMailGegevens.toMailGegevens(gemeenteNaam)

                then("the domain MailGegevens has no replyTo") {
                    mailGegevens.replyTo.shouldBeNull()
                }
            }
        }
    }
})
