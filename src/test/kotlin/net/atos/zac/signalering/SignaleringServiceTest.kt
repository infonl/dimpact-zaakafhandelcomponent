package net.atos.zac.signalering

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.date.shouldBeAfter
import io.kotest.matchers.date.shouldBeBefore
import io.kotest.matchers.shouldBe
import io.mockk.checkUnnecessaryStub
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import jakarta.persistence.EntityManager
import jakarta.persistence.Query
import jakarta.persistence.criteria.CriteriaBuilder
import jakarta.persistence.criteria.CriteriaDelete
import jakarta.persistence.criteria.Path
import jakarta.persistence.criteria.Predicate
import jakarta.persistence.criteria.Root
import net.atos.client.zgw.drc.DrcClientService
import net.atos.client.zgw.zrc.ZrcClientService
import net.atos.client.zgw.zrc.model.createZaak
import net.atos.zac.app.zaak.converter.RestZaakOverzichtConverter
import net.atos.zac.event.EventingService
import net.atos.zac.flowable.createTestTask
import net.atos.zac.flowable.task.FlowableTaskService
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
import java.time.ZonedDateTime

class SignaleringServiceTest : BehaviorSpec({
    val drcClientService = mockk<DrcClientService>()
    val eventingService = mockk<EventingService>()
    val flowableTaskService = mockk<FlowableTaskService>()
    val mailService = mockk<MailService>()
    val signaleringenMailHelper = mockk<SignaleringMailHelper>()
    val zrcClientService = mockk<ZrcClientService>()
    val restZaakOverzichtConverter = mockk<RestZaakOverzichtConverter>()
    val entityManager = mockk<EntityManager>()
    val criteriaBuilder = mockk<CriteriaBuilder>()
    val criteriaDeleteSignalering = mockk<CriteriaDelete<Signalering>>()
    val rootSignalering = mockk<Root<Signalering>>()
    val pathTijdstip = mockk<Path<Long>>()
    val query = mockk<Query>()

    val signaleringService = SignaleringService(
        entityManager = entityManager,
        drcClientService = drcClientService,
        eventingService = eventingService,
        flowableTaskService = flowableTaskService,
        mailService = mailService,
        signaleringenMailHelper = signaleringenMailHelper,
        zrcClientService = zrcClientService,
        restZaakOverzichtConverter = restZaakOverzichtConverter
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
        every { mailService.gemeenteMailAdres } returns gemeenteMailAdres
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
})
