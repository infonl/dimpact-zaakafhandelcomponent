/*
 * SPDX-FileCopyrightText: 2024 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

package nl.info.zac.signalering

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.date.shouldBeAfter
import io.kotest.matchers.date.shouldBeBefore
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain
import io.mockk.checkUnnecessaryStub
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.spyk
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
import kotlinx.coroutines.Dispatchers
import net.atos.zac.event.EventingService
import net.atos.zac.flowable.task.FlowableTaskService
import net.atos.zac.signalering.model.Signalering
import net.atos.zac.signalering.model.SignaleringSubject
import net.atos.zac.signalering.model.SignaleringTarget
import net.atos.zac.signalering.model.SignaleringType
import net.atos.zac.signalering.model.SignaleringZoekParameters
import net.atos.zac.websocket.event.ScreenEvent
import net.atos.zac.websocket.event.ScreenEventType
import nl.info.client.zgw.drc.DrcClientService
import nl.info.client.zgw.drc.model.generated.VertrouwelijkheidaanduidingEnum
import nl.info.client.zgw.model.createMedewerkerIdentificatie
import nl.info.client.zgw.model.createRolMedewerker
import nl.info.client.zgw.model.createZaak
import nl.info.client.zgw.shared.ZgwApiService
import nl.info.client.zgw.zrc.ZrcClientService
import nl.info.client.zgw.zrc.model.generated.Zaak
import nl.info.test.org.flowable.task.api.createTestTask
import nl.info.zac.app.informatieobjecten.converter.RestInformatieobjectConverter
import nl.info.zac.app.signalering.model.RestSignaleringPageParameters
import nl.info.zac.app.zaak.converter.RestZaakOverzichtConverter
import nl.info.zac.app.zaak.model.createRESTZaakOverzicht
import nl.info.zac.authentication.LoggedInUser
import nl.info.zac.authentication.createLoggedInUser
import nl.info.zac.exception.InputValidationFailedException
import nl.info.zac.identity.model.createGroup
import nl.info.zac.identity.model.createUser
import nl.info.zac.mail.MailService
import nl.info.zac.mail.model.createMailAdres
import nl.info.zac.mailtemplates.model.MailGegevens
import nl.info.zac.mailtemplates.model.createMailTemplate
import nl.info.zac.policy.PolicyService
import nl.info.zac.policy.output.createTaakRechten
import nl.info.zac.search.SearchService
import nl.info.zac.search.model.SorteerRichting
import nl.info.zac.search.model.SorteerVeld
import nl.info.zac.search.model.zoekobject.ZoekObjectType
import nl.info.zac.signalering.model.InvalidSignalering
import nl.info.zac.signalering.model.createSignalering
import nl.info.zac.signalering.model.createSignaleringType
import nl.info.zac.signalering.model.createSignaleringZoekParameters
import org.flowable.task.api.TaskInfo
import java.time.LocalDate
import java.time.ZonedDateTime
import java.util.UUID
import java.util.logging.Handler
import java.util.logging.Level
import java.util.logging.LogRecord
import java.util.logging.Logger

@Suppress("LargeClass")
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
    val criteriaQuery = mockk<CriteriaQuery<Signalering>>()
    val predicate = mockk<Predicate>()
    val order = mockk<Order>()
    val typedQuery = mockk<TypedQuery<Signalering>>()
    val searchService = mockk<SearchService>()
    val zgwApiService = mockk<ZgwApiService>()
    val policyService = mockk<PolicyService>()
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
        loggedInUserInstance = loggedInUserInstance,
        searchService = searchService,
        zgwApiService = zgwApiService,
        policyService = policyService,
        dispatcher = Dispatchers.IO
    )

    afterEach {
        checkUnnecessaryStub()
    }

    given("signaleringen older than two days") {
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

        `when`("signaleringen older than two days are deleted") {
            val deletedCount = signaleringService.deleteOldSignaleringen(deleteOlderThanDays)

            then("old signaleringen have been deleted") {
                deletedCount shouldBe 2
                with(zoneDateTimeSlot.captured) {
                    this shouldBeAfter now.minusDays(deleteOlderThanDays)
                    // allow for some seconds difference for slow running tests
                    this shouldBeBefore now.minusDays(deleteOlderThanDays).plusSeconds(10)
                }
            }
        }
    }

    given("A task signalering of type 'task expired'") {
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
        val gemeenteMailAdres = createMailAdres("fake-gemeente@example.com")
        val signaleringMail = SignaleringTarget.Mail("testName", "test@example.com")
        val mailTemplate = createMailTemplate()
        val mailBody = "fakeMailBody"
        val mailGegevensSlot = slot<MailGegevens>()
        every { signaleringenMailHelper.getTargetMail(signalering) } returns signaleringMail
        every { mailService.getGemeenteMailAdres() } returns gemeenteMailAdres
        every { signaleringenMailHelper.getMailTemplate(signalering) } returns mailTemplate
        every { flowableTaskService.readTask(task.id) } returns task
        every { zrcClientService.readZaak(zaak.uuid) } returns zaak
        every { mailService.sendMail(capture(mailGegevensSlot), any()) } returns mailBody

        `when`("the signalering email is sent") {
            signaleringService.sendSignalering(signalering)

            then("the signalering is sent to the correct service") {
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
                    vertrouwelijkheidaanduiding shouldBe VertrouwelijkheidaanduidingEnum.OPENBAAR
                }
            }
        }
    }

    given("A zaken signalering of type ZAAK_OP_NAAM sorted by SIGNALERING_TIJDSTIP DESC (the default)") {
        val signalering = createSignalering()
        val zaak = createZaak()
        val restZaakOverzicht = createRESTZaakOverzicht()
        val pageNumber = 0
        val pageSize = 5
        val restPageParameters = RestSignaleringPageParameters(
            page = pageNumber,
            rows = pageSize,
            sortField = SorteerVeld.SIGNALERING_TIJDSTIP,
            sortOrder = SorteerRichting.DESC
        )
        val loggedInUser = createLoggedInUser()

        every { entityManager.criteriaBuilder } returns criteriaBuilder
        every { entityManager.createQuery(criteriaQuery) } returns typedQuery

        every { criteriaBuilder.createQuery(Signalering::class.java) } returns criteriaQuery
        every { criteriaBuilder.equal(pathTarget, SignaleringTarget.USER) } returns predicate
        every { criteriaBuilder.equal(pathTarget, loggedInUser.id) } returns predicate
        every { criteriaBuilder.equal(pathTarget, SignaleringSubject.ZAAK) } returns predicate
        every { criteriaBuilder.and(*anyVararg<Predicate>()) } returns predicate
        every { criteriaBuilder.desc(pathTijdstip) } returns order

        every { criteriaQuery.from(Signalering::class.java) } returns rootSignalering
        every { criteriaQuery.select(rootSignalering) } returns criteriaQuery
        every { criteriaQuery.where(any<Predicate>()) } returns criteriaQuery
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
        every { zgwApiService.findBehandelaarMedewerkerRoleForZaak(any()) } returns createRolMedewerker(
            medewerkerIdentificatie = createMedewerkerIdentificatie(identificatie = loggedInUser.id)
        )
        every { restZaakOverzichtConverter.convertForDisplayIfReadable(zaak, loggedInUser) } returns restZaakOverzicht

        `when`("listing first page of zaken signaleringen") {
            val result = signaleringService.listCheckedZakenSignaleringenPage(
                SignaleringType.Type.ZAAK_OP_NAAM,
                restPageParameters,
                loggedInUser
            ).valid

            then("paging is used to return the signalering") {
                result.size shouldBe 1
                verify(exactly = 1) {
                    typedQuery.firstResult = pageNumber
                    typedQuery.maxResults = pageSize
                    typedQuery.resultList
                }
            }
        }
    }

    given("Three zaak signaleringen and a request to sort by identificatie ascending") {
        val signaleringA = createSignalering()
        val signaleringB = createSignalering()
        val signaleringC = createSignalering()
        val zaakA = createZaak()
        val zaakB = createZaak()
        val zaakC = createZaak()
        val overzichtCharlie = createRESTZaakOverzicht().apply { identificatie = "ZAAK-C" }
        val overzichtAlpha = createRESTZaakOverzicht().apply { identificatie = "ZAAK-A" }
        val overzichtBravo = createRESTZaakOverzicht().apply { identificatie = "ZAAK-B" }
        val pageParameters = RestSignaleringPageParameters(
            page = 0,
            rows = 2,
            sortField = SorteerVeld.ZAAK_IDENTIFICATIE,
            sortOrder = SorteerRichting.ASC
        )
        val loggedInUser = createLoggedInUser()

        every { entityManager.criteriaBuilder } returns criteriaBuilder
        every { entityManager.createQuery(criteriaQuery) } returns typedQuery
        every { criteriaBuilder.createQuery(Signalering::class.java) } returns criteriaQuery
        every { criteriaBuilder.equal(pathTarget, SignaleringTarget.USER) } returns predicate
        every { criteriaBuilder.equal(pathTarget, loggedInUser.id) } returns predicate
        every { criteriaBuilder.equal(pathTarget, SignaleringSubject.ZAAK) } returns predicate
        every { criteriaBuilder.and(*anyVararg<Predicate>()) } returns predicate
        every { criteriaBuilder.desc(pathTijdstip) } returns order
        every { criteriaQuery.from(Signalering::class.java) } returns rootSignalering
        every { criteriaQuery.select(rootSignalering) } returns criteriaQuery
        every { criteriaQuery.where(any<Predicate>()) } returns criteriaQuery
        every { criteriaQuery.orderBy(order) } returns criteriaQuery
        every { rootSignalering.get<Any>("targettype") } returns pathTarget
        every { rootSignalering.get<Any>("target") } returns pathTarget
        every { rootSignalering.get<Any>("type") } returns pathTarget
        every { rootSignalering.get<Long>("tijdstip") } returns pathTijdstip
        every { pathTarget.get<Any>("id") } returns pathTarget
        every { pathTarget.`in`(listOf("ZAAK_OP_NAAM")) } returns predicate
        every { pathTarget.get<Any>("subjecttype") } returns pathTarget
        every { typedQuery.resultList } returns listOf(signaleringA, signaleringB, signaleringC)

        every { zrcClientService.readZaak(UUID.fromString(signaleringA.subject)) } returns zaakA
        every { zrcClientService.readZaak(UUID.fromString(signaleringB.subject)) } returns zaakB
        every { zrcClientService.readZaak(UUID.fromString(signaleringC.subject)) } returns zaakC
        every { zgwApiService.findBehandelaarMedewerkerRoleForZaak(any()) } returns createRolMedewerker(
            medewerkerIdentificatie = createMedewerkerIdentificatie(identificatie = loggedInUser.id)
        )
        every { restZaakOverzichtConverter.convertForDisplayIfReadable(zaakA, loggedInUser) } returns overzichtCharlie
        every { restZaakOverzichtConverter.convertForDisplayIfReadable(zaakB, loggedInUser) } returns overzichtAlpha
        every { restZaakOverzichtConverter.convertForDisplayIfReadable(zaakC, loggedInUser) } returns overzichtBravo

        `when`("listing the first page") {
            val result = signaleringService.listCheckedZakenSignaleringenPage(
                SignaleringType.Type.ZAAK_OP_NAAM,
                pageParameters,
                loggedInUser
            ).valid

            then("results are sorted by identificatie ascending and sliced to the page size") {
                result shouldBe listOf(overzichtAlpha, overzichtBravo)
            }
            then("the JPA query is run without firstResult / maxResults — paging is applied in memory") {
                verify(exactly = 0) {
                    typedQuery.firstResult = any()
                    typedQuery.maxResults = any()
                }
            }
        }
    }

    given("A request to sort zaken signaleringen by a SorteerVeld this endpoint does not support") {
        val pageParameters = RestSignaleringPageParameters(
            page = 0,
            rows = 5,
            sortField = SorteerVeld.TAAK_NAAM,
            sortOrder = SorteerRichting.ASC
        )
        val loggedInUser = createLoggedInUser()

        `when`("listCheckedZakenSignaleringenPage is called") {
            val exception = shouldThrow<InputValidationFailedException> {
                signaleringService.listCheckedZakenSignaleringenPage(
                    SignaleringType.Type.ZAAK_OP_NAAM,
                    pageParameters,
                    loggedInUser
                )
            }

            then("validation fails before the JPA query runs and the message lists supported fields") {
                exception.message!! shouldContain "TAAK_NAAM"
                exception.message!! shouldContain "ZAAK_IDENTIFICATIE"
                exception.message!! shouldContain "ZAAK_STARTDATUM"
            }
        }
    }

    given("Three zaak signaleringen and a request to sort by startdatum descending on page 1 with two rows per page") {
        val signaleringA = createSignalering()
        val signaleringB = createSignalering()
        val signaleringC = createSignalering()
        val zaakA = createZaak()
        val zaakB = createZaak()
        val zaakC = createZaak()
        val overzichtOld = createRESTZaakOverzicht().apply { startdatum = LocalDate.of(2024, 1, 1) }
        val overzichtMid = createRESTZaakOverzicht().apply { startdatum = LocalDate.of(2025, 1, 1) }
        val overzichtNew = createRESTZaakOverzicht().apply { startdatum = LocalDate.of(2026, 1, 1) }
        val pageParameters = RestSignaleringPageParameters(
            page = 1,
            rows = 2,
            sortField = SorteerVeld.ZAAK_STARTDATUM,
            sortOrder = SorteerRichting.DESC
        )
        val loggedInUser = createLoggedInUser()

        every { entityManager.criteriaBuilder } returns criteriaBuilder
        every { entityManager.createQuery(criteriaQuery) } returns typedQuery
        every { criteriaBuilder.createQuery(Signalering::class.java) } returns criteriaQuery
        every { criteriaBuilder.equal(pathTarget, SignaleringTarget.USER) } returns predicate
        every { criteriaBuilder.equal(pathTarget, loggedInUser.id) } returns predicate
        every { criteriaBuilder.equal(pathTarget, SignaleringSubject.ZAAK) } returns predicate
        every { criteriaBuilder.and(*anyVararg<Predicate>()) } returns predicate
        every { criteriaBuilder.desc(pathTijdstip) } returns order
        every { criteriaQuery.from(Signalering::class.java) } returns rootSignalering
        every { criteriaQuery.select(rootSignalering) } returns criteriaQuery
        every { criteriaQuery.where(any<Predicate>()) } returns criteriaQuery
        every { criteriaQuery.orderBy(order) } returns criteriaQuery
        every { rootSignalering.get<Any>("targettype") } returns pathTarget
        every { rootSignalering.get<Any>("target") } returns pathTarget
        every { rootSignalering.get<Any>("type") } returns pathTarget
        every { rootSignalering.get<Long>("tijdstip") } returns pathTijdstip
        every { pathTarget.get<Any>("id") } returns pathTarget
        every { pathTarget.`in`(listOf("ZAAK_OP_NAAM")) } returns predicate
        every { pathTarget.get<Any>("subjecttype") } returns pathTarget
        every { typedQuery.resultList } returns listOf(signaleringA, signaleringB, signaleringC)

        every { zrcClientService.readZaak(UUID.fromString(signaleringA.subject)) } returns zaakA
        every { zrcClientService.readZaak(UUID.fromString(signaleringB.subject)) } returns zaakB
        every { zrcClientService.readZaak(UUID.fromString(signaleringC.subject)) } returns zaakC
        every { zgwApiService.findBehandelaarMedewerkerRoleForZaak(any()) } returns createRolMedewerker(
            medewerkerIdentificatie = createMedewerkerIdentificatie(identificatie = loggedInUser.id)
        )
        every { restZaakOverzichtConverter.convertForDisplayIfReadable(zaakA, loggedInUser) } returns overzichtMid
        every { restZaakOverzichtConverter.convertForDisplayIfReadable(zaakB, loggedInUser) } returns overzichtNew
        every { restZaakOverzichtConverter.convertForDisplayIfReadable(zaakC, loggedInUser) } returns overzichtOld

        `when`("listing the second page") {
            val result = signaleringService.listCheckedZakenSignaleringenPage(
                SignaleringType.Type.ZAAK_OP_NAAM,
                pageParameters,
                loggedInUser
            ).valid

            then("results are sorted by startdatum descending and the second page is returned") {
                result shouldBe listOf(overzichtOld)
            }
        }
    }

    given("Three zaak signaleringen with distinct zaaktype and omschrijving values") {
        val signaleringA = createSignalering()
        val signaleringB = createSignalering()
        val signaleringC = createSignalering()
        val zaakA = createZaak()
        val zaakB = createZaak()
        val zaakC = createZaak()
        val overzichtZonnepanelen = createRESTZaakOverzicht().apply {
            zaaktype = "Zonnepanelen"
            omschrijving = "Bravo"
        }
        val overzichtAanvraag = createRESTZaakOverzicht().apply {
            zaaktype = "Aanvraag"
            omschrijving = "Charlie"
        }
        val overzichtMelding = createRESTZaakOverzicht().apply {
            zaaktype = "Melding"
            omschrijving = "Alpha"
        }
        val loggedInUser = createLoggedInUser()

        every { entityManager.criteriaBuilder } returns criteriaBuilder
        every { entityManager.createQuery(criteriaQuery) } returns typedQuery
        every { criteriaBuilder.createQuery(Signalering::class.java) } returns criteriaQuery
        every { criteriaBuilder.equal(pathTarget, SignaleringTarget.USER) } returns predicate
        every { criteriaBuilder.equal(pathTarget, loggedInUser.id) } returns predicate
        every { criteriaBuilder.equal(pathTarget, SignaleringSubject.ZAAK) } returns predicate
        every { criteriaBuilder.and(*anyVararg<Predicate>()) } returns predicate
        every { criteriaBuilder.desc(pathTijdstip) } returns order
        every { criteriaQuery.from(Signalering::class.java) } returns rootSignalering
        every { criteriaQuery.select(rootSignalering) } returns criteriaQuery
        every { criteriaQuery.where(any<Predicate>()) } returns criteriaQuery
        every { criteriaQuery.orderBy(order) } returns criteriaQuery
        every { rootSignalering.get<Any>("targettype") } returns pathTarget
        every { rootSignalering.get<Any>("target") } returns pathTarget
        every { rootSignalering.get<Any>("type") } returns pathTarget
        every { rootSignalering.get<Long>("tijdstip") } returns pathTijdstip
        every { pathTarget.get<Any>("id") } returns pathTarget
        every { pathTarget.`in`(listOf("ZAAK_OP_NAAM")) } returns predicate
        every { pathTarget.get<Any>("subjecttype") } returns pathTarget
        every { typedQuery.resultList } returns listOf(signaleringA, signaleringB, signaleringC)

        every { zrcClientService.readZaak(UUID.fromString(signaleringA.subject)) } returns zaakA
        every { zrcClientService.readZaak(UUID.fromString(signaleringB.subject)) } returns zaakB
        every { zrcClientService.readZaak(UUID.fromString(signaleringC.subject)) } returns zaakC
        every { zgwApiService.findBehandelaarMedewerkerRoleForZaak(any()) } returns createRolMedewerker(
            medewerkerIdentificatie = createMedewerkerIdentificatie(identificatie = loggedInUser.id)
        )
        every { restZaakOverzichtConverter.convertForDisplayIfReadable(zaakA, loggedInUser) } returns overzichtZonnepanelen
        every { restZaakOverzichtConverter.convertForDisplayIfReadable(zaakB, loggedInUser) } returns overzichtAanvraag
        every { restZaakOverzichtConverter.convertForDisplayIfReadable(zaakC, loggedInUser) } returns overzichtMelding

        `when`("sorting by ZAAK_ZAAKTYPE ascending") {
            val result = signaleringService.listCheckedZakenSignaleringenPage(
                SignaleringType.Type.ZAAK_OP_NAAM,
                RestSignaleringPageParameters(
                    page = 0,
                    rows = 5,
                    sortField = SorteerVeld.ZAAK_ZAAKTYPE,
                    sortOrder = SorteerRichting.ASC
                ),
                loggedInUser
            ).valid

            then("rows are returned in zaaktype-ascending order") {
                result shouldBe listOf(overzichtAanvraag, overzichtMelding, overzichtZonnepanelen)
            }
        }

        `when`("sorting by ZAAK_OMSCHRIJVING ascending") {
            val result = signaleringService.listCheckedZakenSignaleringenPage(
                SignaleringType.Type.ZAAK_OP_NAAM,
                RestSignaleringPageParameters(
                    page = 0,
                    rows = 5,
                    sortField = SorteerVeld.ZAAK_OMSCHRIJVING,
                    sortOrder = SorteerRichting.ASC
                ),
                loggedInUser
            ).valid

            then("rows are returned in omschrijving-ascending order") {
                result shouldBe listOf(overzichtMelding, overzichtZonnepanelen, overzichtAanvraag)
            }
        }
    }

    given("No zaak signaleringen exist for the user and a sort is requested") {
        val loggedInUser = createLoggedInUser()
        val pageParameters = RestSignaleringPageParameters(
            page = 0,
            rows = 5,
            sortField = SorteerVeld.ZAAK_IDENTIFICATIE,
            sortOrder = SorteerRichting.ASC
        )

        every { entityManager.criteriaBuilder } returns criteriaBuilder
        every { entityManager.createQuery(criteriaQuery) } returns typedQuery
        every { criteriaBuilder.createQuery(Signalering::class.java) } returns criteriaQuery
        every { criteriaBuilder.equal(pathTarget, SignaleringTarget.USER) } returns predicate
        every { criteriaBuilder.equal(pathTarget, loggedInUser.id) } returns predicate
        every { criteriaBuilder.equal(pathTarget, SignaleringSubject.ZAAK) } returns predicate
        every { criteriaBuilder.and(*anyVararg<Predicate>()) } returns predicate
        every { criteriaBuilder.desc(pathTijdstip) } returns order
        every { criteriaQuery.from(Signalering::class.java) } returns rootSignalering
        every { criteriaQuery.select(rootSignalering) } returns criteriaQuery
        every { criteriaQuery.where(any<Predicate>()) } returns criteriaQuery
        every { criteriaQuery.orderBy(order) } returns criteriaQuery
        every { rootSignalering.get<Any>("targettype") } returns pathTarget
        every { rootSignalering.get<Any>("target") } returns pathTarget
        every { rootSignalering.get<Any>("type") } returns pathTarget
        every { rootSignalering.get<Long>("tijdstip") } returns pathTijdstip
        every { pathTarget.get<Any>("id") } returns pathTarget
        every { pathTarget.`in`(listOf("ZAAK_OP_NAAM")) } returns predicate
        every { pathTarget.get<Any>("subjecttype") } returns pathTarget
        every { typedQuery.resultList } returns emptyList()

        `when`("listing the page") {
            val result = signaleringService.listCheckedZakenSignaleringenPage(
                SignaleringType.Type.ZAAK_OP_NAAM,
                pageParameters,
                loggedInUser
            ).valid

            then("an empty list is returned without invoking the ZRC client") {
                result shouldBe emptyList()
                verify(exactly = 0) { zrcClientService.readZaak(any<UUID>()) }
            }
        }
    }

    val pathId = mockk<Path<Long>>()

    fun stubListSignaleringen(
        signaleringenZoekParameters: SignaleringZoekParameters,
        listedSignaleringen: List<Signalering>
    ) {
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
        every { criteriaQuery.where(any<Predicate>()) } returns criteriaQuery
        every { criteriaQuery.orderBy(any<Order>()) } returns criteriaQuery
        every { criteriaBuilder.desc(pathTarget) } returns order
        every { entityManager.createQuery(any<CriteriaQuery<Signalering>>()).resultList } returns listedSignaleringen
    }

    fun stubDeleteSignaleringenWithIds(signaleringIds: List<Long>, deletedCount: Int) {
        every { criteriaBuilder.createCriteriaDelete(Signalering::class.java) } returns criteriaDeleteSignalering
        every { criteriaDeleteSignalering.from(Signalering::class.java) } returns rootSignalering
        every { rootSignalering.get<Long>("id") } returns pathId
        every { pathId.`in`(signaleringIds) } returns predicate
        every { criteriaDeleteSignalering.where(predicate) } returns criteriaDeleteSignalering
        every { entityManager.createQuery(criteriaDeleteSignalering) } returns query
        every { query.executeUpdate() } returns deletedCount
    }

    fun createSignaleringWithId(signaleringId: Long) =
        spyk(createSignalering(targetUser = createUser(id = "fakeId1"))) { every { id } returns signaleringId }

    given("No zaak signaleringen for the given signalering zoek parameters with a subject type and subject") {
        val signaleringenZoekParameters = createSignaleringZoekParameters()
        stubListSignaleringen(signaleringenZoekParameters = signaleringenZoekParameters, listedSignaleringen = emptyList())

        `when`("signaleringen are requested to be deleted") {
            val deletedCount = signaleringService.deleteSignaleringen(signaleringenZoekParameters)

            then("nothing is deleted, no delete query is run and no screen event is sent") {
                deletedCount shouldBe 0
                verify(exactly = 0) { pathId.`in`(any<Collection<*>>()) }
                verify(exactly = 0) { eventingService.send(any<ScreenEvent>()) }
            }
        }
    }

    given(
        """
        A zaak signalering that a concurrent request deleted after it was listed,
        and corresponding signalering zoek parameters with a subject type and subject
        """
    ) {
        val signaleringenZoekParameters = createSignaleringZoekParameters()
        stubListSignaleringen(
            signaleringenZoekParameters = signaleringenZoekParameters,
            listedSignaleringen = listOf(createSignaleringWithId(1L))
        )
        stubDeleteSignaleringenWithIds(signaleringIds = listOf(1L), deletedCount = 0)

        `when`("signaleringen are requested to be deleted") {
            val deletedCount = signaleringService.deleteSignaleringen(signaleringenZoekParameters)

            then("no signalering is deleted, without an error, and no screen event is sent") {
                deletedCount shouldBe 0
                verify(exactly = 0) { eventingService.send(any<ScreenEvent>()) }
            }
        }
    }

    given(
        """
        Two zaak signaleringen each with the same target and corresponding signalering zoek parameters with a subject type and subject
        """
    ) {
        val signaleringenZoekParameters = createSignaleringZoekParameters()
        stubListSignaleringen(
            signaleringenZoekParameters = signaleringenZoekParameters,
            listedSignaleringen = listOf(createSignaleringWithId(1L), createSignaleringWithId(2L))
        )
        stubDeleteSignaleringenWithIds(signaleringIds = listOf(1L, 2L), deletedCount = 2)
        every { eventingService.send(any<ScreenEvent>()) } returns Unit

        `when`("signaleringen are requested to be deleted") {
            val deletedCount = signaleringService.deleteSignaleringen(signaleringenZoekParameters)

            then(
                """
                only the two listed signaleringen are deleted in one bulk delete, so a signalering that is created
                after the listing is not deleted
                """
            ) {
                deletedCount shouldBe 2
                verify(exactly = 1) { pathId.`in`(listOf(1L, 2L)) }
            }
            and("only one screen event is sent, because both signaleringen have the same target and type") {
                verify(exactly = 1) { eventingService.send(any<ScreenEvent>()) }
            }
        }
    }

    given(
        """
        Two taken signaleringen of type TAAK_OP_NAAM of which only the taak of the first belongs to a zaakspecifiek
        geautoriseerde zaak
        """
    ) {
        val loggedInUser = createLoggedInUser()
        val taakSignaleringType = createSignaleringType(
            type = SignaleringType.Type.TAAK_OP_NAAM,
            subjecttype = SignaleringSubject.TAAK
        )
        val zaakspecifiekGeautoriseerdeTaak = createTestTask(
            id = "fakeTaakId1",
            assignee = loggedInUser.id,
            caseVariables = mapOf(
                "zaakIdentificatie" to "fakeZaakIdentificatie1",
                "zaaktypeOmschrijving" to "fakeZaaktypeOmschrijving"
            )
        )
        val otherTaak = createTestTask(
            id = "fakeTaakId2",
            assignee = loggedInUser.id,
            caseVariables = mapOf(
                "zaakIdentificatie" to "fakeZaakIdentificatie2",
                "zaaktypeOmschrijving" to "fakeZaaktypeOmschrijving"
            )
        )
        every { loggedInUserInstance.get() } returns loggedInUser
        every { entityManager.criteriaBuilder } returns criteriaBuilder
        every { entityManager.createQuery(criteriaQuery) } returns typedQuery
        every { criteriaBuilder.createQuery(Signalering::class.java) } returns criteriaQuery
        every { criteriaBuilder.equal(pathTarget, SignaleringTarget.USER) } returns predicate
        every { criteriaBuilder.equal(pathTarget, loggedInUser.id) } returns predicate
        every { criteriaBuilder.equal(pathTarget, SignaleringSubject.TAAK) } returns predicate
        every { criteriaBuilder.and(*anyVararg<Predicate>()) } returns predicate
        every { criteriaBuilder.desc(pathTijdstip) } returns order
        every { criteriaQuery.from(Signalering::class.java) } returns rootSignalering
        every { criteriaQuery.select(rootSignalering) } returns criteriaQuery
        every { criteriaQuery.where(any<Predicate>()) } returns criteriaQuery
        every { criteriaQuery.orderBy(order) } returns criteriaQuery
        every { rootSignalering.get<Any>("targettype") } returns pathTarget
        every { rootSignalering.get<Any>("target") } returns pathTarget
        every { rootSignalering.get<Any>("type") } returns pathTarget
        every { rootSignalering.get<Long>("tijdstip") } returns pathTijdstip
        every { pathTarget.get<Any>("id") } returns pathTarget
        every { pathTarget.`in`(listOf("TAAK_OP_NAAM")) } returns predicate
        every { pathTarget.get<Any>("subjecttype") } returns pathTarget
        every { typedQuery.resultList } returns listOf(
            createSignalering(type = taakSignaleringType, zaak = null, taskInfo = zaakspecifiekGeautoriseerdeTaak),
            createSignalering(type = taakSignaleringType, zaak = null, taskInfo = otherTaak)
        )
        every { flowableTaskService.readTask("fakeTaakId1") } returns zaakspecifiekGeautoriseerdeTaak
        every { flowableTaskService.readTask("fakeTaakId2") } returns otherTaak
        every { policyService.readTaakRechten(any<TaskInfo>()) } returns createTaakRechten()
        every {
            searchService.listZaakspecifiekGeautoriseerdeWaarden(
                waarden = listOf("fakeTaakId1", "fakeTaakId2"),
                type = ZoekObjectType.TAAK,
                veld = "id"
            )
        } returns setOf("fakeTaakId1")

        `when`("the taken signaleringen are listed") {
            val restSignaleringTaskSummaries = signaleringService.listCheckedTakenSignaleringen(
                SignaleringType.Type.TAAK_OP_NAAM
            ).valid

            then("only the taak of the zaakspecifiek geautoriseerde zaak is marked as such") {
                restSignaleringTaskSummaries.map { it.id to it.isZaakspecifiekGeautoriseerd } shouldBe listOf(
                    "fakeTaakId1" to true,
                    "fakeTaakId2" to false
                )
            }
        }
    }

    fun stubUserSignaleringenQuery(loggedInUser: LoggedInUser, type: SignaleringType.Type, subjecttype: SignaleringSubject) {
        every { entityManager.criteriaBuilder } returns criteriaBuilder
        every { entityManager.createQuery(criteriaQuery) } returns typedQuery
        every { criteriaBuilder.createQuery(Signalering::class.java) } returns criteriaQuery
        every { criteriaBuilder.equal(pathTarget, SignaleringTarget.USER) } returns predicate
        every { criteriaBuilder.equal(pathTarget, loggedInUser.id) } returns predicate
        every { criteriaBuilder.equal(pathTarget, subjecttype) } returns predicate
        every { criteriaBuilder.and(*anyVararg<Predicate>()) } returns predicate
        every { criteriaBuilder.desc(pathTijdstip) } returns order
        every { criteriaQuery.from(Signalering::class.java) } returns rootSignalering
        every { criteriaQuery.select(rootSignalering) } returns criteriaQuery
        every { criteriaQuery.where(any<Predicate>()) } returns criteriaQuery
        every { criteriaQuery.orderBy(order) } returns criteriaQuery
        every { rootSignalering.get<Any>("targettype") } returns pathTarget
        every { rootSignalering.get<Any>("target") } returns pathTarget
        every { rootSignalering.get<Any>("type") } returns pathTarget
        every { rootSignalering.get<Long>("tijdstip") } returns pathTijdstip
        every { pathTarget.get<Any>("id") } returns pathTarget
        every { pathTarget.`in`(listOf(type.toString())) } returns predicate
        every { pathTarget.get<Any>("subjecttype") } returns pathTarget
    }

    fun createZaakSignalering(zaak: Zaak, targetUserId: String = "fakeLoggedInUserName") =
        createSignalering(zaak = zaak, targetUser = createUser(id = targetUserId))

    fun createZaakSignaleringWithId(signaleringId: Long, zaak: Zaak, targetUserId: String = "fakeLoggedInUserName") =
        spyk(createZaakSignalering(zaak = zaak, targetUserId = targetUserId)) { every { id } returns signaleringId }

    fun createBehandelaarRol(userId: String) =
        createRolMedewerker(medewerkerIdentificatie = createMedewerkerIdentificatie(identificatie = userId))

    fun captureSignaleringServiceWarnings(): MutableList<LogRecord> {
        val logRecords = mutableListOf<LogRecord>()
        Logger.getLogger(SignaleringService::class.java.name).addHandler(object : Handler() {
            override fun publish(logRecord: LogRecord) {
                if (logRecord.level == Level.WARNING) logRecords.add(logRecord)
            }
            override fun flush() = Unit
            override fun close() = Unit
        })
        return logRecords
    }

    context("Deleting the zaak signaleringen of former behandelaren") {
        given("a zaak with behandelaar B and user signaleringen of B and of the previous behandelaar A") {
            val zaak = createZaak()
            stubListSignaleringen(
                signaleringenZoekParameters = SignaleringZoekParameters(SignaleringSubject.ZAAK, zaak.uuid.toString()),
                listedSignaleringen = listOf(
                    createZaakSignaleringWithId(signaleringId = 1L, zaak = zaak, targetUserId = "fakeUserA"),
                    createZaakSignalering(zaak = zaak, targetUserId = "fakeUserB")
                )
            )
            every { pathTarget.get<Any>("id") } returns pathTarget
            every { pathTarget.`in`(any<Collection<*>>()) } returns predicate
            every { zrcClientService.readZaak(zaak.uuid) } returns zaak
            every { zgwApiService.findBehandelaarMedewerkerRoleForZaak(zaak) } returns createBehandelaarRol("fakeUserB")
            stubDeleteSignaleringenWithIds(signaleringIds = listOf(1L), deletedCount = 1)
            every { eventingService.send(any<ScreenEvent>()) } returns Unit

            `when`("the zaak signaleringen of former behandelaren are deleted") {
                val deletedCount = signaleringService.deleteZaakSignaleringenOfFormerBehandelaren(zaak.uuid)

                then("only the signalering of A is deleted") {
                    deletedCount shouldBe 1
                    verify(exactly = 1) { pathId.`in`(listOf(1L)) }
                }
            }
        }

        given("a zaak without behandelaar and a user and a group signalering for it") {
            val zaak = createZaak()
            stubListSignaleringen(
                signaleringenZoekParameters = SignaleringZoekParameters(SignaleringSubject.ZAAK, zaak.uuid.toString()),
                listedSignaleringen = listOf(
                    createZaakSignaleringWithId(signaleringId = 1L, zaak = zaak, targetUserId = "fakeUserA"),
                    createSignalering(zaak = zaak, targetGroup = createGroup())
                )
            )
            every { pathTarget.get<Any>("id") } returns pathTarget
            every { pathTarget.`in`(any<Collection<*>>()) } returns predicate
            every { zrcClientService.readZaak(zaak.uuid) } returns zaak
            every { zgwApiService.findBehandelaarMedewerkerRoleForZaak(zaak) } returns null
            stubDeleteSignaleringenWithIds(signaleringIds = listOf(1L), deletedCount = 1)
            every { eventingService.send(any<ScreenEvent>()) } returns Unit

            `when`("the zaak signaleringen of former behandelaren are deleted") {
                val deletedCount = signaleringService.deleteZaakSignaleringenOfFormerBehandelaren(zaak.uuid)

                then("the user signalering is deleted and the group signalering is kept") {
                    deletedCount shouldBe 1
                    verify(exactly = 1) { pathId.`in`(listOf(1L)) }
                }
            }
        }
    }

    context("Listing zaken signaleringen that are no longer valid") {
        given("a page with a signalering of a zaak that now has another behandelaar and one of a valid zaak") {
            val loggedInUser = createLoggedInUser(id = "fakeLoggedInUserName")
            val reassignedZaak = createZaak()
            val zaak = createZaak()
            val overzicht = createRESTZaakOverzicht()
            val staleSignalering = createZaakSignalering(zaak = reassignedZaak)
            stubUserSignaleringenQuery(loggedInUser, SignaleringType.Type.ZAAK_OP_NAAM, SignaleringSubject.ZAAK)
            every { typedQuery.setFirstResult(0) } returns typedQuery
            every { typedQuery.setMaxResults(2) } returns typedQuery
            every { typedQuery.resultList } returns listOf(staleSignalering, createZaakSignalering(zaak = zaak))
            every { zrcClientService.readZaak(reassignedZaak.uuid) } returns reassignedZaak
            every { zrcClientService.readZaak(zaak.uuid) } returns zaak
            every {
                zgwApiService.findBehandelaarMedewerkerRoleForZaak(reassignedZaak)
            } returns createBehandelaarRol("fakeOtherUserName")
            every { zgwApiService.findBehandelaarMedewerkerRoleForZaak(zaak) } returns createBehandelaarRol(loggedInUser.id)
            every { restZaakOverzichtConverter.convertForDisplayIfReadable(zaak, loggedInUser) } returns overzicht

            `when`("the first page of two zaken is listed") {
                val checkedZakenSignaleringen = signaleringService.listCheckedZakenSignaleringenPage(
                    SignaleringType.Type.ZAAK_OP_NAAM,
                    RestSignaleringPageParameters(
                        page = 0,
                        rows = 2,
                        sortField = SorteerVeld.SIGNALERING_TIJDSTIP,
                        sortOrder = SorteerRichting.DESC
                    ),
                    loggedInUser
                )

                then("only the valid zaak is returned as valid") {
                    checkedZakenSignaleringen.valid shouldBe listOf(overzicht)
                }
                and("the stale signalering is returned as invalid with its reason, without deleting it") {
                    checkedZakenSignaleringen.invalid.map { it.signalering to it.reason } shouldBe listOf(
                        staleSignalering to "the user is not the behandelaar of the zaak"
                    )
                    verify(exactly = 0) { criteriaBuilder.createCriteriaDelete(Signalering::class.java) }
                }
            }
        }

        given("a signalering of a zaak the user is behandelaar of but cannot read, and one of a valid zaak") {
            val loggedInUser = createLoggedInUser(id = "fakeLoggedInUserName")
            val unreadableZaak = createZaak()
            val zaak = createZaak()
            val overzicht = createRESTZaakOverzicht()
            val unreadableSignalering = createZaakSignalering(zaak = unreadableZaak)
            stubUserSignaleringenQuery(loggedInUser, SignaleringType.Type.ZAAK_OP_NAAM, SignaleringSubject.ZAAK)
            every { typedQuery.resultList } returns listOf(unreadableSignalering, createZaakSignalering(zaak = zaak))
            every { zrcClientService.readZaak(unreadableZaak.uuid) } returns unreadableZaak
            every { zrcClientService.readZaak(zaak.uuid) } returns zaak
            every { zgwApiService.findBehandelaarMedewerkerRoleForZaak(any()) } returns createBehandelaarRol(loggedInUser.id)
            every { restZaakOverzichtConverter.convertForDisplayIfReadable(unreadableZaak, loggedInUser) } returns null
            every { restZaakOverzichtConverter.convertForDisplayIfReadable(zaak, loggedInUser) } returns overzicht

            `when`("the zaken signaleringen are listed sorted by identificatie") {
                val checkedZakenSignaleringen = signaleringService.listCheckedZakenSignaleringenPage(
                    SignaleringType.Type.ZAAK_OP_NAAM,
                    RestSignaleringPageParameters(
                        page = 0,
                        rows = 5,
                        sortField = SorteerVeld.ZAAK_IDENTIFICATIE,
                        sortOrder = SorteerRichting.ASC
                    ),
                    loggedInUser
                )

                then("only the readable zaak is valid and the other signalering is invalid because of the read rights") {
                    checkedZakenSignaleringen.valid shouldBe listOf(overzicht)
                    checkedZakenSignaleringen.invalid.map { it.signalering to it.reason } shouldBe listOf(
                        unreadableSignalering to "the user has no read rights on the zaak"
                    )
                }
            }
        }
    }

    given(
        """
        taken signaleringen of a taak assigned to another user, an unassigned taak, a taak the user cannot read
        and a valid taak
        """
    ) {
        val loggedInUser = createLoggedInUser(id = "fakeLoggedInUserName")
        val taakSignaleringType = createSignaleringType(
            type = SignaleringType.Type.TAAK_OP_NAAM,
            subjecttype = SignaleringSubject.TAAK
        )
        val caseVariables = mapOf(
            "zaakIdentificatie" to "fakeZaakIdentificatie",
            "zaaktypeOmschrijving" to "fakeZaaktypeOmschrijving"
        )
        val reassignedTaak = createTestTask(id = "fakeTaakId1", assignee = "fakeOtherUserName", caseVariables = caseVariables)
        val unassignedTaak = createTestTask(id = "fakeTaakId2", caseVariables = caseVariables)
        val unreadableTaak = createTestTask(id = "fakeTaakId3", assignee = loggedInUser.id, caseVariables = caseVariables)
        val taak = createTestTask(id = "fakeTaakId4", assignee = loggedInUser.id, caseVariables = caseVariables)
        val taken = listOf(reassignedTaak, unassignedTaak, unreadableTaak, taak)
        val signaleringen = taken.map { createSignalering(type = taakSignaleringType, zaak = null, taskInfo = it) }
        every { loggedInUserInstance.get() } returns loggedInUser
        stubUserSignaleringenQuery(loggedInUser, SignaleringType.Type.TAAK_OP_NAAM, SignaleringSubject.TAAK)
        every { typedQuery.resultList } returns signaleringen
        taken.forEach { every { flowableTaskService.readTask(it.id) } returns it }
        every { policyService.readTaakRechten(unreadableTaak) } returns createTaakRechten(lezen = false)
        every { policyService.readTaakRechten(taak) } returns createTaakRechten()
        every {
            searchService.listZaakspecifiekGeautoriseerdeWaarden(
                waarden = listOf("fakeTaakId4"),
                type = ZoekObjectType.TAAK,
                veld = "id"
            )
        } returns emptySet()

        `when`("the taken signaleringen are listed") {
            val checkedTakenSignaleringen = signaleringService.listCheckedTakenSignaleringen(
                SignaleringType.Type.TAAK_OP_NAAM
            )

            then("only the valid taak is returned as valid") {
                checkedTakenSignaleringen.valid.map { it.id } shouldBe listOf("fakeTaakId4")
            }
            and("the other signaleringen are returned as invalid with their reasons") {
                checkedTakenSignaleringen.invalid.map { it.signalering to it.reason } shouldBe listOf(
                    signaleringen[0] to "the user is not the assignee of the taak",
                    signaleringen[1] to "the user is not the assignee of the taak",
                    signaleringen[2] to "the user has no read rights on the taak"
                )
            }
        }
    }

    given("two invalid signaleringen with the same target and type") {
        val zaak = createZaak()
        val warnings = captureSignaleringServiceWarnings()
        val screenEvents = mutableListOf<ScreenEvent>()
        every { entityManager.criteriaBuilder } returns criteriaBuilder
        stubDeleteSignaleringenWithIds(signaleringIds = listOf(1L, 2L), deletedCount = 2)
        every { eventingService.send(capture(screenEvents)) } returns Unit

        `when`("the invalid signaleringen are deleted") {
            val deletedCount = signaleringService.deleteInvalidSignaleringen(
                listOf(
                    InvalidSignalering(
                        signalering = createZaakSignaleringWithId(signaleringId = 1L, zaak = zaak),
                        reason = "the user is not the behandelaar of the zaak"
                    ),
                    InvalidSignalering(
                        signalering = createZaakSignaleringWithId(signaleringId = 2L, zaak = zaak),
                        reason = "the user has no read rights on the zaak"
                    )
                )
            )

            then("both are deleted in one bulk delete") {
                deletedCount shouldBe 2
                verify(exactly = 1) { pathId.`in`(listOf(1L, 2L)) }
            }
            and("a SIGNALERINGEN screen event is sent, so the dashboard card loads the page again") {
                screenEvents.map { it.objectType } shouldBe listOf(ScreenEventType.SIGNALERINGEN)
            }
            and("a warning per signalering names the signalering and the reason, but not the user") {
                warnings.map { it.message } shouldBe listOf(
                    "Deleting signalering with id '1' of type 'ZAAK_OP_NAAM' for ZAAK '${zaak.uuid}', because " +
                        "the user is not the behandelaar of the zaak.",
                    "Deleting signalering with id '2' of type 'ZAAK_OP_NAAM' for ZAAK '${zaak.uuid}', because " +
                        "the user has no read rights on the zaak."
                )
            }
        }
    }
})
