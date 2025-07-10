/*
 * SPDX-FileCopyrightText: 2022 Atos, 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.zac.mailtemplates

import jakarta.enterprise.context.ApplicationScoped
import jakarta.inject.Inject
import jakarta.persistence.EntityManager
import jakarta.transaction.Transactional
import net.atos.zac.mailtemplates.model.Mail
import net.atos.zac.mailtemplates.model.MailTemplate
import net.atos.zac.util.ValidationUtil
import nl.info.zac.util.AllOpen
import nl.info.zac.util.NoArgConstructor
import java.util.Optional

@ApplicationScoped
@Transactional
@AllOpen
@NoArgConstructor
class MailTemplateService @Inject constructor(
    private val entityManager: EntityManager
) {
    fun findMailtemplate(id: Long): Optional<MailTemplate> {
        val mailTemplate = entityManager.find(MailTemplate::class.java, id)
        return if (mailTemplate != null) Optional.of(mailTemplate) else Optional.empty<MailTemplate>()
    }

    fun findMailtemplateByName(mailTemplateName: String): Optional<MailTemplate> {
        val builder = entityManager.criteriaBuilder
        val query = builder.createQuery(MailTemplate::class.java)
        val root = query.from(MailTemplate::class.java)
        val predicate = builder.equal(root.get<Any>("mailTemplateNaam"), mailTemplateName)
        query.select(root)
        query.where(predicate)
        return entityManager.createQuery(query)
            .getResultList()
            .stream()
            .findFirst()
    }

    fun delete(id: Long) = findMailtemplate(id).ifPresent { entityManager.remove(it) }

    fun storeMailtemplate(mailTemplate: MailTemplate): MailTemplate {
        ValidationUtil.valideerObject(mailTemplate)
        if (mailTemplate.id != 0L && findMailtemplate(mailTemplate.id).isPresent) {
            return entityManager.merge(mailTemplate)
        } else {
            entityManager.persist(mailTemplate)
            return mailTemplate
        }
    }

    fun findDefaultMailtemplate(mail: Mail): Optional<MailTemplate> {
        val builder = entityManager.criteriaBuilder
        val query = builder.createQuery(MailTemplate::class.java)
        val root = query.from(MailTemplate::class.java)
        val equalPredicate = builder.equal(root.get<Any>(MailTemplate.MAIL), mail)
        val defaultPredicate = builder.equal(root.get<Any>(MailTemplate.DEFAULT_MAILTEMPLATE), true)
        val finalPredicate = builder.and(equalPredicate, defaultPredicate)
        query.select(root).where(finalPredicate)
        val resultList = entityManager.createQuery(query).getResultList()
        return if (resultList.isEmpty()) {
            Optional.empty<MailTemplate>()
        } else {
            Optional.of<MailTemplate>(
                resultList.first()
            )
        }
    }

    @Suppress("TooGenericExceptionThrown")
    fun readMailtemplate(mail: Mail): MailTemplate =
        findDefaultMailtemplate(mail)
            .orElseThrow {
                RuntimeException("${MailTemplate::class.java.getSimpleName()} for '$mail' not found")
            }

    @Suppress("TooGenericExceptionThrown")
    fun readMailtemplate(id: Long): MailTemplate {
        val mailTemplate = entityManager.find<MailTemplate?>(MailTemplate::class.java, id)
        if (mailTemplate != null) {
            return mailTemplate
        } else {
            throw RuntimeException("${MailTemplate::class.java.getSimpleName()} with id=$id not found")
        }
    }

    fun listMailtemplates(): List<MailTemplate> {
        val builder = entityManager.criteriaBuilder
        val query = builder.createQuery(MailTemplate::class.java)
        val root = query.from(MailTemplate::class.java)
        query.orderBy(builder.asc(root.get<Any>("mailTemplateNaam")))
        query.select(root)
        return entityManager.createQuery(query).getResultList()
    }

    fun listKoppelbareMailtemplates(): List<MailTemplate> {
        val builder = entityManager.criteriaBuilder
        val query = builder.createQuery(MailTemplate::class.java)
        val root = query.from(MailTemplate::class.java)
        query.where(root.get<Any>(MailTemplate.MAIL).`in`(Mail.getKoppelbareMails()))
        return entityManager.createQuery(query).getResultList()
    }
}
