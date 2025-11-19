/*
 * SPDX-FileCopyrightText: 2022 Atos, 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.mailtemplates

import jakarta.enterprise.context.ApplicationScoped
import jakarta.inject.Inject
import jakarta.persistence.EntityManager
import jakarta.transaction.Transactional
import net.atos.zac.util.ValidationUtil
import nl.info.zac.mailtemplates.exception.MailTemplateNotFoundException
import nl.info.zac.mailtemplates.model.Mail
import nl.info.zac.mailtemplates.model.Mail.Companion.LINKABLE_MAILS
import nl.info.zac.mailtemplates.model.MailTemplate
import nl.info.zac.mailtemplates.model.MailTemplate.Companion.MAIL_TEMPLATE_NAME
import nl.info.zac.util.AllOpen
import nl.info.zac.util.NoArgConstructor

@ApplicationScoped
@Transactional
@AllOpen
@NoArgConstructor
@Suppress("TooManyFunctions")
class MailTemplateService @Inject constructor(
    private val entityManager: EntityManager
) {
    fun delete(id: Long) = findMailtemplate(id)?.let { entityManager.remove(it) }

    fun findDefaultMailtemplate(mail: Mail): MailTemplate? {
        val builder = entityManager.criteriaBuilder
        val query = builder.createQuery(MailTemplate::class.java)
        val root = query.from(MailTemplate::class.java)
        query.select(root).where(
            builder.and(
                builder.equal(root.get<Any>(MailTemplate.MAIL), mail),
                builder.equal(root.get<Any>(MailTemplate.DEFAULT_MAILTEMPLATE), true)
            )
        )
        return entityManager.createQuery(query).resultList.firstOrNull()
    }

    fun findMailtemplateByName(mailTemplateName: String): MailTemplate? {
        val query = entityManager.criteriaBuilder.createQuery(MailTemplate::class.java).apply {
            val root = from(MailTemplate::class.java)
            select(root).where(entityManager.criteriaBuilder.equal(root.get<Any>(MAIL_TEMPLATE_NAME), mailTemplateName))
        }
        return entityManager.createQuery(query).resultList.firstOrNull()
    }

    fun listKoppelbareMailtemplates(): List<MailTemplate> {
        val query = entityManager.criteriaBuilder.createQuery(MailTemplate::class.java).apply {
            val root = from(MailTemplate::class.java)
            where(root.get<Any>(MailTemplate.MAIL).`in`(LINKABLE_MAILS))
        }
        return entityManager.createQuery(query).resultList
    }

    fun listMailtemplates(): List<MailTemplate> {
        val query = entityManager.criteriaBuilder.createQuery(MailTemplate::class.java).apply {
            val root = from(MailTemplate::class.java)
            select(root)
            orderBy(entityManager.criteriaBuilder.asc(root.get<Any>(MAIL_TEMPLATE_NAME)))
        }
        return entityManager.createQuery(query).resultList
    }

    fun readMailtemplate(mail: Mail): MailTemplate =
        findDefaultMailtemplate(mail) ?: throw MailTemplateNotFoundException(mail)

    fun readMailtemplate(id: Long): MailTemplate {
        return entityManager.find(MailTemplate::class.java, id)
            ?: throw MailTemplateNotFoundException(id)
    }

    fun createMailtemplate(mailTemplate: MailTemplate): MailTemplate {
        ValidationUtil.valideerObject(mailTemplate)
        // Ensure ID is not set for new entities - let database auto-generate
        mailTemplate.id = 0
        entityManager.persist(mailTemplate)
        return mailTemplate
    }

    fun updateMailtemplate(id: Long, mailTemplate: MailTemplate): MailTemplate {
        ValidationUtil.valideerObject(mailTemplate)
        val existingTemplate = readMailtemplate(id)

        existingTemplate.apply {
            this.mailTemplateNaam = mailTemplate.mailTemplateNaam
            this.onderwerp = mailTemplate.onderwerp
            this.body = mailTemplate.body
            this.mail = mailTemplate.mail
            this.isDefaultMailtemplate = mailTemplate.isDefaultMailtemplate
        }

        return entityManager.merge(existingTemplate)
    }

    fun storeMailtemplate(mailTemplate: MailTemplate): MailTemplate {
        ValidationUtil.valideerObject(mailTemplate)
        return if (mailTemplate.id != 0L && findMailtemplate(mailTemplate.id) != null) {
            updateMailtemplate(mailTemplate.id, mailTemplate)
        } else {
            createMailtemplate(mailTemplate)
        }
    }

    private fun findMailtemplate(id: Long): MailTemplate? = entityManager.find(MailTemplate::class.java, id)
}
