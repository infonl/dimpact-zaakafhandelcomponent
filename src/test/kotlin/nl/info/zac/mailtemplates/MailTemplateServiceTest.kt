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
import nl.info.zac.mailtemplates.exception.MailTemplateNotFoundException
import nl.info.zac.mailtemplates.model.Mail
import nl.info.zac.mailtemplates.model.MailTemplate
import nl.info.zac.mailtemplates.model.createMailTemplate

class MailTemplateServiceTest : BehaviorSpec({
    val entityManager = mockk<EntityManager>()
    val criteriaBuilder = mockk<CriteriaBuilder>()
    val criteriaQuery = mockk<CriteriaQuery<MailTemplate>>(relaxed = true)
    val predicate = mockk<Predicate>()
    val root = mockk<Root<MailTemplate>>()
    val mailTemplateNamePath = mockk<Path<String>>()
    val typedQuery = mockk<TypedQuery<MailTemplate>>()
    val mailTemplateService = MailTemplateService(entityManager)

    afterEach {
        checkUnnecessaryStub()
    }

    context("Mail templates can be deleted") {
        given("an existing mail template") {
            val mailTemplateId = 1234L
            val mailTemplate = createMailTemplate()

            every { entityManager.find(MailTemplate::class.java, mailTemplateId) } returns mailTemplate
            every { entityManager.remove(mailTemplate) } just Runs

            `when`("delete is called with the ID") {
                mailTemplateService.delete(mailTemplateId)

                then("it should remove the mail template") {
                    verify(exactly = 1) {
                        entityManager.remove(mailTemplate)
                    }
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

            `when`("readMailtemplate is called with the mail type") {
                val result = mailTemplateService.readMailtemplate(mail)

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

            `when`("readMailtemplate is called with the mail type") {
                val exception = shouldThrow<MailTemplateNotFoundException> {
                    mailTemplateService.readMailtemplate(mail)
                }
                then("it should throw MailTemplateNotFoundException") {
                    exception shouldBe MailTemplateNotFoundException(mail)
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
                val result = mailTemplateService.readMailtemplate(mailTemplateId)

                then("it should return the mail template") {
                    result shouldBe mailTemplate
                }
            }
        }

        given("no mail template exists for the given ID") {
            every { entityManager.find(MailTemplate::class.java, mailTemplateId) } returns null

            `when`("readMailtemplate is called with the ID") {
                val exception = shouldThrow<MailTemplateNotFoundException> {
                    mailTemplateService.readMailtemplate(mailTemplateId)
                }
                then("it should throw MailTemplateNotFoundException") {
                    exception shouldBe MailTemplateNotFoundException(mailTemplateId)
                }
            }
        }
    }

    context("Mail templates can be created") {
        val mailTemplate = createMailTemplate()

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
            val newMailTemplate = createMailTemplate(id = 0L)
            every { entityManager.persist(newMailTemplate) } just Runs

            `when`("createMailtemplate is called") {
                val result = mailTemplateService.createMailtemplate(newMailTemplate)

                then("it should persist the mail template") {
                    verify(exactly = 1) { entityManager.persist(newMailTemplate) }
                    result shouldBe newMailTemplate
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

        given("an existing mail template") {
            every { entityManager.find(MailTemplate::class.java, mailTemplateId) } returns existingTemplate
            every { entityManager.merge(existingTemplate) } returns existingTemplate

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
            val newMailTemplate = createMailTemplate()
            every { entityManager.find(MailTemplate::class.java, newMailTemplate.id) } returns null
            every { entityManager.persist(newMailTemplate) } just Runs

            `when`("storeMailtemplate is called") {
                val result = mailTemplateService.storeMailtemplate(newMailTemplate)

                then("it should delegate to createMailtemplate and persist the mail template") {
                    newMailTemplate.id shouldBe 0L // ID should be reset
                    verify(exactly = 1) { entityManager.persist(newMailTemplate) }
                    result shouldBe newMailTemplate
                }
            }
        }

        given("an existing mail template") {
            val existingMailTemplate = createMailTemplate(id = 5678L)
            val existingTemplate = createMailTemplate(id = 5678L)
            every { entityManager.find(MailTemplate::class.java, existingMailTemplate.id) } returns existingTemplate
            every { entityManager.merge(existingTemplate) } returns existingTemplate

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
            }
        }
    }
})
