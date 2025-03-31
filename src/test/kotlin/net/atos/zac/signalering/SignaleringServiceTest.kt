/*
 * SPDX-FileCopyrightText: 2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */

package net.atos.zac.signalering

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.date.shouldBeAfter
import io.kotest.matchers.date.shouldBeBefore
import io.kotest.matchers.shouldBe
import io.mockk.checkUnnecessaryStub
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.slot
import io.mockk.verify
import jakarta.enterprise.inject.Instance
import jakarta.persistence.EntityManager
import jakarta.persistence.Query
import jakarta.persistence.TypedQuery
import jakarta.persistence.criteria.CriteriaBuilder
import jakarta.persistence.criteria.CriteriaDelete
import jakarta.persistence.criteria.CriteriaQuery
import jakarta.persistence.criteria.Order
import jakarta.persistence.criteria.Path
import jakarta.persistence.criteria.Predicate
import jakarta.persistence.criteria.Root
import net.atos.client.zgw.drc.DrcClientService
import net.atos.client.zgw.zrc.ZrcClientService
import net.atos.zac.app.informatieobjecten.converter.RestInformatieobjectConverter
import net.atos.zac.app.shared.RestPageParameters
import net.atos.zac.event.EventingService
import net.atos.zac.flowable.createTestTask
import net.atos.zac.flowable.task.FlowableTaskService
import net.atos.zac.flowable.task.TaakVariabelenService
import net.atos.zac.identity.model.createUser
import net.atos.zac.mail.MailService
import net.atos.zac.mail.model.createMailAdres
import net.atos.zac.mailtemplates.model.MailGegevens
import net.atos.zac.mailtemplates.model.createMailTemplate
import net.atos.zac.signalering.model.Signalering
import net.atos.zac.signalering.model.SignaleringSubject
import net.atos.zac.signalering.model.SignaleringTarget
import net.atos.zac.signalering.model.SignaleringType
import net.atos.zac.signalering.model.createSignalering
import net.atos.zac.signalering.model.createSignaleringType
import net.atos.zac.signalering.model.createSignaleringZoekParameters
import net.atos.zac.websocket.event.ScreenEvent
import nl.info.client.zgw.model.createZaak
import nl.info.zac.app.zaak.converter.RestZaakOverzichtConverter
import nl.info.zac.app.zaak.model.createRESTZaakOverzicht
import nl.info.zac.authentication.LoggedInUser
import java.time.ZonedDateTime
import java.util.UUID

class SignaleringServiceTest : BehaviorSpec({
    val drcClientService = mockk<DrcClientService>()
    val eventingService = mockk<EventingService>()
    val flowableTaskService = mockk<FlowableTaskService>()
    val mailService = mockk<MailService>()
    val signaleringenMailHelper = mockk<SignaleringMailHelper>()
    val zrcClientService = mockk<ZrcClientService>()
    val restZaakOverzichtConverter = mockk<RestZaakOverzichtConverter>()
    val restInformatieobjectConverter = mockk<RestInformatieobjectConverter>()
    val entityManager = mockk<EntityManager>()
    val criteriaBuilder = mockk<CriteriaBuilder>()
    val criteriaDeleteSignalering = mockk<CriteriaDelete<Signalering>>()
    val rootSignalering = mockk<Root<Signalering>>()
    val pathTijdstip = mockk<Path<Long>>()
    val pathTarget = mockk<Path<Any>>()
    val query = mockk<Query>()
    val loggedInUserInstance = mockk<Instance<LoggedInUser>>()
    val user = mockk<LoggedInUser>()
    val criteriaQuery = mockk<CriteriaQuery<Signalering>>()
    val predicate = mockk<Predicate>()
    val order = mockk<Order>()
    val typedQuery = mockk<TypedQuery<Signalering>>()

    mockkStatic(TaakVariabelenService::class)

    val signaleringService = SignaleringService(
        entityManager = entityManager,
        drcClientService = drcClientService,
        eventingService = eventingService,
        flowableTaskService = flowableTaskService,
        mailService = mailService,
        signaleringenMailHelper = signaleringenMailHelper,
        zrcClientService = zrcClientService,
        restZaakOverzichtConverter = restZaakOverzichtConverter,
        restInformatieobjectConverter = restInformatieobjectConverter,
        loggedInUserInstance = loggedInUserInstance
    )

    beforeEach {
        checkUnnecessaryStub()
    }

    Given("signaleringen older than two days") {
        val now = ZonedDateTime.now()
        val deleteOlderThanDays = 2L
        val zoneDateTimeSlot = slot<ZonedDateTime>()
        every { entityManager.criteriaBuilder } returns criteriaBuilder
        every { criteriaBuilder.createCriteriaDelete(Signalering::class.java) } returns criteriaDeleteSignalering
        every { criteriaDeleteSignalering.from(Signalering::class.java) } returns rootSignalering
        every { rootSignalering.get<Long>("tijdstip") } returns pathTijdstip
        every { criteriaBuilder.lessThan(any(), capture(zoneDateTimeSlot)) } returns mockk<Predicate>()
        every { criteriaDeleteSignalering.where(any<Predicate>()) } returns criteriaDeleteSignalering
        every { entityManager.createQuery(criteriaDeleteSignalering) } returns query
        every { query.executeUpdate() } returns 2

        When("signaleringen older than two days are deleted") {
            val deletedCount = signaleringService.deleteOldSignaleringen(deleteOlderThanDays)

            Then("old signaleringen have been deleted") {
                deletedCount shouldBe 2
                with(zoneDateTimeSlot.captured) {
                    this shouldBeAfter now.minusDays(deleteOlderThanDays)
                    // allow for some seconds difference for slow running tests
                    this shouldBeBefore now.minusDays(deleteOlderThanDays).plusSeconds(10)
                }
            }
        }
    }

    Given("A task signalering of type 'task expired'") {
        val zaak = createZaak()
        val task = createTestTask(
            scopeType = "cmmn",
            caseVariables = mapOf(
                "zaakUUID" to zaak.uuid
            )
        )
        val signalering = createSignalering(
            taskInfo = task,
            targetUser = createUser(),
            type = createSignaleringType(
                type = SignaleringType.Type.TAAK_VERLOPEN,
                subjecttype = SignaleringSubject.TAAK
            ),
            zaak = null
        )
        val gemeenteMailAdres = createMailAdres("dummy-gemeente@example.com")
        val signaleringMail = SignaleringTarget.Mail("testName", "test@example.com")
        val mailTemplate = createMailTemplate()
        val mailBody = "dummyMailBody"
        val mailGegevensSlot = slot<MailGegevens>()
        every { signaleringenMailHelper.getTargetMail(signalering) } returns signaleringMail
        every { mailService.getGemeenteMailAdres() } returns gemeenteMailAdres
        every { signaleringenMailHelper.getMailTemplate(signalering) } returns mailTemplate
        every { flowableTaskService.readTask(task.id) } returns task
        every { zrcClientService.readZaak(zaak.uuid) } returns zaak
        every { mailService.sendMail(capture(mailGegevensSlot), any()) } returns mailBody

        When("the signalering email is sent") {
            signaleringService.sendSignalering(signalering)

            Then("the signalering is sent to the correct service") {
                verify(exactly = 1) {
                    mailService.sendMail(any(), any())
                }
                with(mailGegevensSlot.captured) {
                    from shouldBe gemeenteMailAdres
                    to.email shouldBe signaleringMail.emailadres
                    to.name shouldBe signaleringMail.naam
                    replyTo shouldBe null
                    subject shouldBe mailTemplate.onderwerp
                    body shouldBe mailTemplate.body
                }
            }
        }
    }

    Given("A zaken signalering of type ZAAK_OP_NAAM") {
        val id = "id"
        val signalering = createSignalering()
        val zaak = createZaak()
        val restZaakOverzicht = createRESTZaakOverzicht()
        val pageNumber = 0
        val pageSize = 5
        val restPageParameters = RestPageParameters(pageNumber, pageSize)

        every { loggedInUserInstance.get() } returns user
        every { user.id } returns id

        every { entityManager.criteriaBuilder } returns criteriaBuilder
        every { entityManager.createQuery(criteriaQuery) } returns typedQuery

        every { criteriaBuilder.createQuery(Signalering::class.java) } returns criteriaQuery
        every { criteriaBuilder.equal(pathTarget, SignaleringTarget.USER) } returns predicate
        every { criteriaBuilder.equal(pathTarget, id) } returns predicate
        every { criteriaBuilder.equal(pathTarget, SignaleringSubject.ZAAK) } returns predicate
        every { criteriaBuilder.and(*anyVararg<Predicate>()) } returns predicate
        every { criteriaBuilder.desc(pathTijdstip) } returns order

        every { criteriaQuery.from(Signalering::class.java) } returns rootSignalering
        every { criteriaQuery.select(rootSignalering) } returns criteriaQuery
        every { criteriaQuery.where(any()) } returns criteriaQuery
        every { criteriaQuery.orderBy(order) } returns criteriaQuery

        every { rootSignalering.get<Any>("targettype") } returns pathTarget
        every { rootSignalering.get<Any>("target") } returns pathTarget
        every { rootSignalering.get<Any>("type") } returns pathTarget
        every { rootSignalering.get<Long>("tijdstip") } returns pathTijdstip

        every { pathTarget.get<Any>("id") } returns pathTarget
        every { pathTarget.`in`(listOf("ZAAK_OP_NAAM")) } returns predicate
        every { pathTarget.get<Any>("subjecttype") } returns pathTarget

        every { typedQuery.setFirstResult(pageNumber) } returns typedQuery
        every { typedQuery.setMaxResults(pageSize) } returns typedQuery
        every { typedQuery.resultList } returns listOf(signalering)

        every { zrcClientService.readZaak(UUID.fromString(signalering.subject)) } returns zaak
        every { restZaakOverzichtConverter.convertForDisplay(zaak) } returns restZaakOverzicht

        When("listing first page of zaken signaleringen") {
            val result = signaleringService.listZakenSignaleringenPage(
                SignaleringType.Type.ZAAK_OP_NAAM,
                restPageParameters
            )

            Then("paging is used to return the signalering") {
                result.size shouldBe 1
                verify(exactly = 1) {
                    typedQuery.setFirstResult(pageNumber)
                    typedQuery.setMaxResults(pageSize)
                    typedQuery.resultList
                }
            }
        }
    }
    Given(
        """
        Two zaak signaleringen and corresponding signalering zoek parameters with a subject type and subject
        """
    ) {
        val signaleringenZoekParameters = createSignaleringZoekParameters()
        val signaleringen = listOf(
            createSignalering(),
            createSignalering()
        )
        every { entityManager.criteriaBuilder } returns criteriaBuilder
        every { criteriaBuilder.createQuery(Signalering::class.java) } returns criteriaQuery
        every { criteriaQuery.from(Signalering::class.java) } returns rootSignalering
        every { criteriaQuery.select(rootSignalering) } returns criteriaQuery
        every { rootSignalering.get<Any>("type") } returns pathTarget
        every { rootSignalering.get<Any>("subject") } returns pathTarget
        every { pathTarget.get<Any>("subjecttype") } returns pathTarget
        every { rootSignalering.get<Any>("tijdstip") } returns pathTarget
        every { criteriaBuilder.equal(pathTarget, signaleringenZoekParameters.subjecttype) } returns predicate
        every { criteriaBuilder.equal(pathTarget, signaleringenZoekParameters.subject) } returns predicate
        every { criteriaBuilder.and(*anyVararg<Predicate>()) } returns predicate
        every { criteriaQuery.where(any()) } returns criteriaQuery
        every { criteriaQuery.orderBy(any<Order>()) } returns criteriaQuery
        every { criteriaBuilder.desc(pathTarget) } returns order
        every { entityManager.createQuery(any<CriteriaQuery<Signalering>>()).resultList } returns signaleringen
        every { entityManager.remove(any<Signalering>()) } returns Unit
        every { eventingService.send(any<ScreenEvent>()) } returns Unit

        When("signaleringen are requested to be deleted") {
            signaleringService.deleteSignaleringen(signaleringenZoekParameters)

            Then("the two signaleringen are deleted and one screen event is sent") {
                verify(exactly = 2) {
                    entityManager.remove(any<Signalering>())
                }
                verify(exactly = 1) {
                    // we expect only one screen event since it concerns to signaleringen
                    // with the same target and type
                    eventingService.send(any<ScreenEvent>())
                }
            }
        }
    }
})
