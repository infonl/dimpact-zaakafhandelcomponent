/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.app.signalering.converter

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.mockk.checkUnnecessaryStub
import io.mockk.every
import io.mockk.mockk
import net.atos.zac.signalering.model.SignaleringSubject
import net.atos.zac.signalering.model.SignaleringTarget
import net.atos.zac.signalering.model.SignaleringType
import nl.info.zac.identity.model.createGroup
import nl.info.zac.identity.model.createUser
import nl.info.zac.signalering.SignaleringService
import nl.info.zac.signalering.model.createRestSignaleringInstellingen
import nl.info.zac.signalering.model.createSignaleringInstellingen
import nl.info.zac.signalering.model.createSignaleringType

class RestSignaleringInstellingenConverterTest : BehaviorSpec({
    val signaleringService = mockk<SignaleringService>()
    val restSignaleringInstellingenConverter = RestSignaleringInstellingenConverter(signaleringService)

    afterEach {
        checkUnnecessaryStub()
    }

    context("convert(SignaleringInstellingen)") {
        given("a SignaleringInstellingen with dashboard-enabled type for a USER owner") {
            val type = createSignaleringType(type = SignaleringType.Type.ZAAK_OP_NAAM)
            val instellingen = createSignaleringInstellingen(
                type = type,
                ownerType = SignaleringTarget.USER,
                isDashboard = true,
                isMail = true
            )

            `when`("convert(SignaleringInstellingen) is called") {
                val result = restSignaleringInstellingenConverter.convert(instellingen)

                then("dashboard is populated because type supports dashboard and owner is USER") {
                    result.dashboard shouldNotBe null
                    result.dashboard shouldBe true
                }

                then("mail is populated because type supports mail") {
                    result.mail shouldNotBe null
                    result.mail shouldBe true
                }
            }
        }

        given("a SignaleringInstellingen with dashboard-enabled type for a GROUP owner") {
            val type = createSignaleringType(type = SignaleringType.Type.ZAAK_OP_NAAM)
            val instellingen = createSignaleringInstellingen(
                type = type,
                ownerType = SignaleringTarget.GROUP,
                isDashboard = true,
                isMail = true
            )

            `when`("convert(SignaleringInstellingen) is called") {
                val result = restSignaleringInstellingenConverter.convert(instellingen)

                then("dashboard is null because owner is GROUP") {
                    result.dashboard shouldBe null
                }

                then("mail is still populated") {
                    result.mail shouldBe true
                }
            }
        }

        given("a SignaleringInstellingen with a type that has no dashboard support (TAAK_VERLOPEN)") {
            val type = createSignaleringType(type = SignaleringType.Type.TAAK_VERLOPEN)
            val instellingen = createSignaleringInstellingen(
                type = type,
                ownerType = SignaleringTarget.USER,
                isDashboard = false,
                isMail = true
            )

            `when`("convert(SignaleringInstellingen) is called") {
                val result = restSignaleringInstellingenConverter.convert(instellingen)

                then("dashboard is null because type does not support dashboard") {
                    result.dashboard shouldBe null
                }
            }
        }
    }

    context("convert(Collection<SignaleringInstellingen>)") {
        given("a collection of SignaleringInstellingen") {
            val type = createSignaleringType()
            val instellingen1 = createSignaleringInstellingen(id = 1L, type = type)
            val instellingen2 = createSignaleringInstellingen(id = 2L, type = type)

            `when`("convert(Collection) is called") {
                val result = restSignaleringInstellingenConverter.convert(listOf(instellingen1, instellingen2))

                then("all items are converted") {
                    result.size shouldBe 2
                    result[0].id shouldBe 1L
                    result[1].id shouldBe 2L
                }
            }
        }
    }

    context("convert(RestSignaleringInstellingen, Group)") {
        given("a RestSignaleringInstellingen and a Group") {
            val type = SignaleringType.Type.ZAAK_OP_NAAM
            val restInstellingen = createRestSignaleringInstellingen(
                type = createSignaleringType(type = type, subjecttype = SignaleringSubject.ZAAK),
                isMail = true
            )
            val group = createGroup(id = "fakeGroupId")
            val domainInstellingen = createSignaleringInstellingen(
                type = createSignaleringType(type = type, subjecttype = SignaleringSubject.ZAAK),
                ownerType = SignaleringTarget.GROUP,
                isMail = false
            )
            every { signaleringService.readInstellingenGroup(type, group.name) } returns domainInstellingen

            `when`("convert(RestSignaleringInstellingen, Group) is called") {
                val result = restSignaleringInstellingenConverter.convert(restInstellingen, group)

                then("isMail is updated from REST model if type supports mail") {
                    result.isMail shouldBe true
                }

                then("isDashboard is always set to false for group") {
                    result.isDashboard shouldBe false
                }
            }
        }
    }

    context("convert(RestSignaleringInstellingen, User)") {
        given("a RestSignaleringInstellingen and a User") {
            val type = SignaleringType.Type.ZAAK_OP_NAAM
            val restInstellingen = createRestSignaleringInstellingen(
                type = createSignaleringType(type = type, subjecttype = SignaleringSubject.ZAAK),
                isDashboard = true,
                isMail = true
            )
            val user = createUser(id = "fakeUserId")
            val domainInstellingen = createSignaleringInstellingen(
                type = createSignaleringType(type = type, subjecttype = SignaleringSubject.ZAAK),
                ownerType = SignaleringTarget.USER,
                isDashboard = false,
                isMail = false
            )
            every { signaleringService.readInstellingenUser(type, user.id) } returns domainInstellingen

            `when`("convert(RestSignaleringInstellingen, User) is called") {
                val result = restSignaleringInstellingenConverter.convert(restInstellingen, user)

                then("isDashboard is updated from REST model if type supports dashboard") {
                    result.isDashboard shouldBe true
                }

                then("isMail is updated from REST model if type supports mail") {
                    result.isMail shouldBe true
                }
            }
        }
    }
})
