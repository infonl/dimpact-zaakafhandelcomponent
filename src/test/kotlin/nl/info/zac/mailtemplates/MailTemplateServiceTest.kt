/*
 * SPDX-FileCopyrightText: 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.mailtemplates

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.Runs
import io.mockk.checkUnnecessaryStub
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.verify
import jakarta.persistence.EntityManager
import jakarta.persistence.TypedQuery
import jakarta.persistence.criteria.CriteriaBuilder
import jakarta.persistence.criteria.CriteriaQuery
import jakarta.persistence.criteria.Path
import jakarta.persistence.criteria.Predicate
import jakarta.persistence.criteria.Root
import nl.info.zac.admin.MailTemplateKoppelingenService
import nl.info.zac.admin.ZaaktypeConfigurationService
import nl.info.zac.exception.ErrorCode.ERROR_CODE_MAIL_TEMPLATE_IS_IN_USE_BY_ZAAKTYPE_CONFIGURATION
import nl.info.zac.exception.ErrorCode.ERROR_CODE_SYSTEM_MAIL_TEMPLATE_CANNOT_BE_CONVERTED
import nl.info.zac.exception.ErrorCode.ERROR_CODE_SYSTEM_MAIL_TEMPLATE_CANNOT_BE_DELETED
import nl.info.zac.exception.InputValidationFailedException
import nl.info.zac.mailtemplates.exception.MailTemplateNotConfiguredException
import nl.info.zac.mailtemplates.exception.MailTemplateNotFoundException
import nl.info.zac.mailtemplates.model.Mail
import nl.info.zac.mailtemplates.model.MailTemplate
import nl.info.zac.mailtemplates.model.createMailTemplate
import java.util.UUID

class MailTemplateServiceTest : BehaviorSpec({
    val entityManager = mockk<EntityManager>()
    val criteriaBuilder = mockk<CriteriaBuilder>()
    val criteriaQuery = mockk<CriteriaQuery<MailTemplate>>(relaxed = true)
    val predicate = mockk<Predicate>()
    val root = mockk<Root<MailTemplate>>()
    val mailTemplateNamePath = mockk<Path<String>>()
    val typedQuery = mockk<TypedQuery<MailTemplate>>()
    val zaaktypeConfigurationService = mockk<ZaaktypeConfigurationService>()
    val mailTemplateKoppelingenService = mockk<MailTemplateKoppelingenService>()
    val mailTemplateService = MailTemplateService(
        entityManager,
        zaaktypeConfigurationService,
        mailTemplateKoppelingenService
    )

    afterEach {
        checkUnnecessaryStub()
    }

    context("Mail templates can be deleted") {
        given("a normal mail template that no zaaktype configuration links") {
            val mailTemplateId = 1234L
            val mailTemplate = createMailTemplate(id = mailTemplateId, isDefaultMailTemplate = false)

            every { entityManager.find(MailTemplate::class.java, mailTemplateId) } returns mailTemplate
            every { entityManager.remove(mailTemplate) } just Runs
            clearMocks(zaaktypeConfigurationService, mailTemplateKoppelingenService)
            every { mailTemplateKoppelingenService.listZaaktypeUuidsLinkedTo(mailTemplateId) } returns emptyList()

            `when`("delete is called with the ID") {
                mailTemplateService.delete(mailTemplateId)

                then("it removes the mail template and leaves the cached zaaktype configurations alone") {
                    verify(exactly = 1) { entityManager.remove(mailTemplate) }
                    verify(exactly = 0) {
                        zaaktypeConfigurationService.evict(any())
                        zaaktypeConfigurationService.clearManagedCache()
                    }
                }
            }
        }

        given("a system mail template") {
            val mailTemplateId = 1234L
            val mailTemplate = createMailTemplate(id = mailTemplateId, isDefaultMailTemplate = true)
            every { entityManager.find(MailTemplate::class.java, mailTemplateId) } returns mailTemplate
            clearMocks(firstMock = entityManager, answers = false, recordedCalls = true, childMocks = false)
            clearMocks(mailTemplateKoppelingenService)

            `when`("delete is called with the ID") {
                val inputValidationFailedException = shouldThrow<InputValidationFailedException> {
                    mailTemplateService.delete(mailTemplateId)
                }

                then(
                    "it is refused with the error code for a system mail template, before the links are checked, " +
                        "so that a linked system mail template gets the same error"
                ) {
                    inputValidationFailedException.errorCode shouldBe ERROR_CODE_SYSTEM_MAIL_TEMPLATE_CANNOT_BE_DELETED
                    verify(exactly = 0) {
                        entityManager.remove(any())
                        mailTemplateKoppelingenService.listZaaktypeUuidsLinkedTo(any())
                    }
                }
            }
        }

        given("a normal mail template that a zaaktype configuration links") {
            val mailTemplateId = 1234L
            val mailTemplate = createMailTemplate(id = mailTemplateId, isDefaultMailTemplate = false)
            every { entityManager.find(MailTemplate::class.java, mailTemplateId) } returns mailTemplate
            clearMocks(firstMock = entityManager, answers = false, recordedCalls = true, childMocks = false)
            clearMocks(mailTemplateKoppelingenService)
            every {
                mailTemplateKoppelingenService.listZaaktypeUuidsLinkedTo(mailTemplateId)
            } returns listOf(UUID.randomUUID())

            `when`("delete is called with the ID") {
                val inputValidationFailedException = shouldThrow<InputValidationFailedException> {
                    mailTemplateService.delete(mailTemplateId)
                }

                then("it is refused with the error code for a linked mail template, instead of the database error") {
                    inputValidationFailedException.errorCode shouldBe
                        ERROR_CODE_MAIL_TEMPLATE_IS_IN_USE_BY_ZAAKTYPE_CONFIGURATION
                    verify(exactly = 0) { entityManager.remove(any()) }
                }
            }
        }
    }

    context("Default mail templates can be found for a mail type") {
        val mail = Mail.ZAAK_ONTVANKELIJK
        val mailTemplate = createMailTemplate()

        given("a default MailTemplate exists for the given mail type") {
            every { entityManager.criteriaBuilder } returns criteriaBuilder
            every { criteriaBuilder.createQuery(MailTemplate::class.java) } returns criteriaQuery
            every { criteriaQuery.from(MailTemplate::class.java) } returns root
            every { root.get<String>(MailTemplate.MAIL) } returns mailTemplateNamePath
            every { root.get<String>(MailTemplate.DEFAULT_MAILTEMPLATE) } returns mailTemplateNamePath
            every { criteriaBuilder.equal(mailTemplateNamePath, mail) } returns predicate
            every { criteriaBuilder.equal(mailTemplateNamePath, true) } returns predicate
            every { criteriaBuilder.and(predicate, predicate) } returns predicate
            every { entityManager.createQuery(criteriaQuery) } returns typedQuery
            every { typedQuery.resultList } returns listOf(mailTemplate)

            `when`("findDefaultMailtemplate is called with the mail type") {
                val result = mailTemplateService.findDefaultMailtemplate(mail)

                then("it should return the default mail template") {
                    result shouldBe mailTemplate
                }
            }
        }

        given("no default mail template exists for the given mail type") {
            every { entityManager.criteriaBuilder } returns criteriaBuilder
            every { criteriaBuilder.createQuery(MailTemplate::class.java) } returns criteriaQuery
            every { criteriaQuery.from(MailTemplate::class.java) } returns root
            every { root.get<String>(MailTemplate.MAIL) } returns mailTemplateNamePath
            every { root.get<String>(MailTemplate.DEFAULT_MAILTEMPLATE) } returns mailTemplateNamePath
            every { criteriaBuilder.equal(mailTemplateNamePath, mail) } returns predicate
            every { criteriaBuilder.equal(mailTemplateNamePath, true) } returns predicate
            every { criteriaBuilder.and(predicate, predicate) } returns predicate
            every { entityManager.createQuery(criteriaQuery) } returns typedQuery
            every { typedQuery.resultList } returns emptyList()

            `when`("findDefaultMailtemplate is called with the mail type") {
                val result = mailTemplateService.findDefaultMailtemplate(mail)

                then("it should return null") {
                    result shouldBe null
                }
            }
        }
    }

    context("Mail templates can be found by name") {
        val mailTemplateName = "fakeTemplateName"

        given("a mail template exists for the given name") {
            val mailTemplate = createMailTemplate()
            every { entityManager.criteriaBuilder } returns criteriaBuilder
            every { criteriaBuilder.createQuery(MailTemplate::class.java) } returns criteriaQuery
            every { criteriaQuery.from(MailTemplate::class.java) } returns root
            every { root.get<String>("mailTemplateNaam") } returns mailTemplateNamePath
            every { criteriaBuilder.equal(mailTemplateNamePath, mailTemplateName) } returns predicate
            every { entityManager.createQuery(criteriaQuery) } returns typedQuery
            every { typedQuery.resultList } returns listOf(mailTemplate)

            `when`("findMailtemplateByName is called") {
                val result = mailTemplateService.findMailtemplateByName(mailTemplateName)

                then("it should return the matching mail template") {
                    result shouldBe mailTemplate
                }
            }
        }

        given("no mail template exists for the given name") {
            every { entityManager.criteriaBuilder } returns criteriaBuilder
            every { criteriaBuilder.createQuery(MailTemplate::class.java) } returns criteriaQuery
            every { criteriaQuery.from(MailTemplate::class.java) } returns root
            every { root.get<String>("mailTemplateNaam") } returns mailTemplateNamePath
            every { criteriaBuilder.equal(mailTemplateNamePath, mailTemplateName) } returns predicate
            every { entityManager.createQuery(criteriaQuery) } returns typedQuery
            every { typedQuery.resultList } returns emptyList()

            `when`("findMailtemplateByName is called") {
                val result = mailTemplateService.findMailtemplateByName(mailTemplateName)

                then("it should return null") {
                    result shouldBe null
                }
            }
        }
    }

    context("All mail templates can be listed") {
        given("multiple mail templates exist") {
            val mailTemplates = listOf(createMailTemplate(), createMailTemplate())
            every { entityManager.criteriaBuilder } returns criteriaBuilder
            every { criteriaBuilder.createQuery(MailTemplate::class.java) } returns criteriaQuery
            every { criteriaQuery.from(MailTemplate::class.java) } returns root
            every { criteriaQuery.orderBy(criteriaBuilder.asc(root.get<String>("mailTemplateNaam"))) } returns criteriaQuery
            every { criteriaQuery.select(root) } returns criteriaQuery
            every { entityManager.createQuery(criteriaQuery) } returns typedQuery
            every { typedQuery.resultList } returns mailTemplates

            `when`("listMailtemplates is called") {
                val result = mailTemplateService.listMailtemplates()

                then("it should return all mail templates") {
                    result shouldBe mailTemplates
                }
            }
        }
    }

    context("Mail templates can be read by mail type") {
        val mail = Mail.ZAAK_ONTVANKELIJK
        val mailTemplate = createMailTemplate()

        given("a mail template exists for the given mail type") {
            every { entityManager.criteriaBuilder } returns criteriaBuilder
            every { criteriaBuilder.createQuery(MailTemplate::class.java) } returns criteriaQuery
            every { criteriaQuery.from(MailTemplate::class.java) } returns root
            every { root.get<String>(MailTemplate.MAIL) } returns mailTemplateNamePath
            every { root.get<String>(MailTemplate.DEFAULT_MAILTEMPLATE) } returns mailTemplateNamePath
            every { criteriaBuilder.equal(mailTemplateNamePath, mail) } returns predicate
            every { criteriaBuilder.equal(mailTemplateNamePath, true) } returns predicate
            every { criteriaBuilder.and(predicate, predicate) } returns predicate
            every { entityManager.createQuery(criteriaQuery) } returns typedQuery
            every { typedQuery.resultList } returns listOf(mailTemplate)

            `when`("readDefaultMailTemplate is called with the mail type") {
                val result = mailTemplateService.readDefaultMailTemplate(mail)

                then("it should return the mail template") {
                    result shouldBe mailTemplate
                }
            }
        }

        given("no mail template exists for the given mail type") {
            every { entityManager.criteriaBuilder } returns criteriaBuilder
            every { criteriaBuilder.createQuery(MailTemplate::class.java) } returns criteriaQuery
            every { criteriaQuery.from(MailTemplate::class.java) } returns root
            every { root.get<String>(MailTemplate.MAIL) } returns mailTemplateNamePath
            every { root.get<String>(MailTemplate.DEFAULT_MAILTEMPLATE) } returns mailTemplateNamePath
            every { criteriaBuilder.equal(mailTemplateNamePath, mail) } returns predicate
            every { criteriaBuilder.equal(mailTemplateNamePath, true) } returns predicate
            every { criteriaBuilder.and(predicate, predicate) } returns predicate
            every { entityManager.createQuery(criteriaQuery) } returns typedQuery
            every { typedQuery.resultList } returns emptyList()

            `when`("readDefaultMailTemplate is called with the mail type") {
                val exception = shouldThrow<MailTemplateNotConfiguredException> {
                    mailTemplateService.readDefaultMailTemplate(mail)
                }
                then("it should throw MailTemplateNotConfiguredException") {
                    exception.message shouldBe
                        "No default mail template configured for mail type '${mail.name}'. It must be configured by an administrator."
                }
            }
        }
    }

    context("Mail templates can be read by ID") {
        val mailTemplateId = 1234L
        val mailTemplate = createMailTemplate()

        given("a mail template exists for the given ID") {
            every { entityManager.find(MailTemplate::class.java, mailTemplateId) } returns mailTemplate

            `when`("readMailtemplate is called with the ID") {
                val result = mailTemplateService.readMailTemplate(mailTemplateId)

                then("it should return the mail template") {
                    result shouldBe mailTemplate
                }
            }
        }

        given("no mail template exists for the given ID") {
            every { entityManager.find(MailTemplate::class.java, mailTemplateId) } returns null

            `when`("readMailtemplate is called with the ID") {
                val exception = shouldThrow<MailTemplateNotFoundException> {
                    mailTemplateService.readMailTemplate(mailTemplateId)
                }
                then("it should throw MailTemplateNotFoundException") {
                    exception shouldBe MailTemplateNotFoundException(mailTemplateId)
                }
            }
        }
    }

    context("Mail templates can be created") {
        val mailTemplate = createMailTemplate(isDefaultMailTemplate = false)

        given("a new mail template with an ID") {
            every { entityManager.persist(mailTemplate) } just Runs

            `when`("createMailtemplate is called") {
                val result = mailTemplateService.createMailtemplate(mailTemplate)

                then("it should reset the ID to 0 and persist the mail template") {
                    mailTemplate.id shouldBe 0L
                    verify(exactly = 1) { entityManager.persist(mailTemplate) }
                    result shouldBe mailTemplate
                }
            }
        }

        given("a new mail template without an ID") {
            val newMailTemplate = createMailTemplate(id = 0L, isDefaultMailTemplate = false)
            every { entityManager.persist(newMailTemplate) } just Runs

            `when`("createMailtemplate is called") {
                val result = mailTemplateService.createMailtemplate(newMailTemplate)

                then("it should persist the mail template") {
                    verify(exactly = 1) { entityManager.persist(newMailTemplate) }
                    result shouldBe newMailTemplate
                }
            }
        }

        given("a new mail template with the system flag on") {
            val systemMailTemplate = createMailTemplate(id = 0L, isDefaultMailTemplate = true)
            clearMocks(firstMock = entityManager, answers = false, recordedCalls = true, childMocks = false)

            `when`("createMailtemplate is called") {
                val inputValidationFailedException = shouldThrow<InputValidationFailedException> {
                    mailTemplateService.createMailtemplate(systemMailTemplate)
                }

                then(
                    "it is refused with the error code for converting a system mail template, and nothing is " +
                        "persisted, so that each mail type keeps the one system mail template that ZAC created"
                ) {
                    inputValidationFailedException.errorCode shouldBe ERROR_CODE_SYSTEM_MAIL_TEMPLATE_CANNOT_BE_CONVERTED
                    verify(exactly = 0) { entityManager.persist(any()) }
                }
            }
        }
    }

    context("Mail templates can be updated") {
        val mailTemplateId = 1234L
        val existingTemplate = createMailTemplate(id = mailTemplateId)
        val updatedTemplate = createMailTemplate(
            id = 5678L, // Different ID that should be ignored
            name = "updatedName",
            onderwerp = "updatedOnderwerp",
            body = "updatedBody"
        )

        given("an existing system mail template and a change of its name, subject and body") {
            every { entityManager.find(MailTemplate::class.java, mailTemplateId) } returns existingTemplate
            every { entityManager.merge(existingTemplate) } returns existingTemplate
            clearMocks(zaaktypeConfigurationService, mailTemplateKoppelingenService)
            val linkedZaaktypeUuids = listOf(UUID.randomUUID(), UUID.randomUUID())
            every { mailTemplateKoppelingenService.listZaaktypeUuidsLinkedTo(mailTemplateId) } returns linkedZaaktypeUuids
            linkedZaaktypeUuids.forEach { every { zaaktypeConfigurationService.evict(it) } just Runs }

            `when`("updateMailtemplate is called") {
                val result = mailTemplateService.updateMailtemplate(mailTemplateId, updatedTemplate)

                then("it should update the existing template fields and preserve the original ID") {
                    existingTemplate.id shouldBe mailTemplateId // Original ID preserved
                    existingTemplate.mailTemplateNaam shouldBe "updatedName"
                    existingTemplate.onderwerp shouldBe "updatedOnderwerp"
                    existingTemplate.body shouldBe "updatedBody"
                    verify(exactly = 1) { entityManager.merge(existingTemplate) }
                    result shouldBe existingTemplate
                }

                then(
                    "the cached configuration of each zaaktype that links the template is evicted, " +
                        "so that it no longer holds the old mail template"
                ) {
                    linkedZaaktypeUuids.forEach { verify(exactly = 1) { zaaktypeConfigurationService.evict(it) } }
                    verify(exactly = 0) { zaaktypeConfigurationService.clearManagedCache() }
                }
            }
        }

        mapOf(
            "the system flag off" to createMailTemplate(isDefaultMailTemplate = false),
            "another mail type" to createMailTemplate(mail = Mail.ZAAK_ALGEMEEN, isDefaultMailTemplate = true)
        ).forEach { (change, requestedTemplate) ->
            given("an existing system mail template and a request with $change and a changed subject") {
                val systemMailTemplate = createMailTemplate(
                    id = mailTemplateId,
                    onderwerp = "fakeOriginalOnderwerp",
                    mail = Mail.SIGNALERING_TAAK_OP_NAAM,
                    isDefaultMailTemplate = true
                )
                requestedTemplate.onderwerp = "fakeChangedOnderwerp"
                every { entityManager.find(MailTemplate::class.java, mailTemplateId) } returns systemMailTemplate
                clearMocks(firstMock = entityManager, answers = false, recordedCalls = true, childMocks = false)

                `when`("updateMailtemplate is called") {
                    val inputValidationFailedException = shouldThrow<InputValidationFailedException> {
                        mailTemplateService.updateMailtemplate(mailTemplateId, requestedTemplate)
                    }

                    then(
                        "it is refused with the error code for converting a system mail template, " +
                            "and the template keeps its mail type, its system flag and its subject"
                    ) {
                        inputValidationFailedException.errorCode shouldBe
                            ERROR_CODE_SYSTEM_MAIL_TEMPLATE_CANNOT_BE_CONVERTED
                        with(systemMailTemplate) {
                            mail shouldBe Mail.SIGNALERING_TAAK_OP_NAAM
                            isDefaultMailtemplate shouldBe true
                            onderwerp shouldBe "fakeOriginalOnderwerp"
                        }
                        verify(exactly = 0) { entityManager.merge(any<MailTemplate>()) }
                    }
                }
            }
        }

        given("an existing normal mail template and a request with the system flag on") {
            val normalMailTemplate = createMailTemplate(id = mailTemplateId, isDefaultMailTemplate = false)
            every { entityManager.find(MailTemplate::class.java, mailTemplateId) } returns normalMailTemplate
            clearMocks(firstMock = entityManager, answers = false, recordedCalls = true, childMocks = false)

            `when`("updateMailtemplate is called") {
                val inputValidationFailedException = shouldThrow<InputValidationFailedException> {
                    mailTemplateService.updateMailtemplate(
                        mailTemplateId,
                        createMailTemplate(isDefaultMailTemplate = true)
                    )
                }

                then(
                    "it is refused with the error code for converting a system mail template, " +
                        "and the template stays a normal mail template"
                ) {
                    inputValidationFailedException.errorCode shouldBe ERROR_CODE_SYSTEM_MAIL_TEMPLATE_CANNOT_BE_CONVERTED
                    normalMailTemplate.isDefaultMailtemplate shouldBe false
                    verify(exactly = 0) { entityManager.merge(any<MailTemplate>()) }
                }
            }
        }

        given("a non-existent mail template") {
            every { entityManager.find(MailTemplate::class.java, mailTemplateId) } returns null

            `when`("updateMailtemplate is called") {
                val exception = shouldThrow<MailTemplateNotFoundException> {
                    mailTemplateService.updateMailtemplate(mailTemplateId, updatedTemplate)
                }
                then("it should throw MailTemplateNotFoundException") {
                    exception shouldBe MailTemplateNotFoundException(mailTemplateId)
                }
            }
        }
    }

    context("Mail templates can be stored (backward compatibility)") {
        given("a new mail template") {
            val newMailTemplate = createMailTemplate(isDefaultMailTemplate = false)
            every { entityManager.find(MailTemplate::class.java, newMailTemplate.id) } returns null
            every { entityManager.persist(newMailTemplate) } just Runs
            clearMocks(zaaktypeConfigurationService)

            `when`("storeMailtemplate is called") {
                val result = mailTemplateService.storeMailtemplate(newMailTemplate)

                then("it should delegate to createMailtemplate and persist the mail template") {
                    newMailTemplate.id shouldBe 0L // ID should be reset
                    verify(exactly = 1) { entityManager.persist(newMailTemplate) }
                    result shouldBe newMailTemplate
                }

                then("no cached zaaktype configuration is evicted, because none of them holds a new template") {
                    verify(exactly = 0) {
                        zaaktypeConfigurationService.evict(any())
                        zaaktypeConfigurationService.clearManagedCache()
                    }
                }
            }
        }

        given("an existing mail template") {
            val existingMailTemplate = createMailTemplate(id = 5678L)
            val existingTemplate = createMailTemplate(id = 5678L)
            every { entityManager.find(MailTemplate::class.java, existingMailTemplate.id) } returns existingTemplate
            every { entityManager.merge(existingTemplate) } returns existingTemplate
            clearMocks(zaaktypeConfigurationService, mailTemplateKoppelingenService)
            val linkedZaaktypeUuid = UUID.randomUUID()
            every { mailTemplateKoppelingenService.listZaaktypeUuidsLinkedTo(5678L) } returns listOf(linkedZaaktypeUuid)
            every { zaaktypeConfigurationService.evict(linkedZaaktypeUuid) } just Runs

            `when`("storeMailtemplate is called with an existing ID") {
                val result = mailTemplateService.storeMailtemplate(existingMailTemplate)

                then("it should delegate to updateMailtemplate and merge the mail template") {
                    // Verify that the existing template fields were updated
                    existingTemplate.mailTemplateNaam shouldBe existingMailTemplate.mailTemplateNaam
                    existingTemplate.onderwerp shouldBe existingMailTemplate.onderwerp
                    existingTemplate.body shouldBe existingMailTemplate.body
                    existingTemplate.mail shouldBe existingMailTemplate.mail
                    existingTemplate.isDefaultMailtemplate shouldBe existingMailTemplate.isDefaultMailtemplate
                    verify(exactly = 1) { entityManager.merge(existingTemplate) }
                    result shouldBe existingTemplate
                }

                then("the cached configuration of the zaaktype that links the template is evicted once") {
                    verify(exactly = 1) { zaaktypeConfigurationService.evict(linkedZaaktypeUuid) }
                    verify(exactly = 0) { zaaktypeConfigurationService.clearManagedCache() }
                }
            }
        }
    }
})
