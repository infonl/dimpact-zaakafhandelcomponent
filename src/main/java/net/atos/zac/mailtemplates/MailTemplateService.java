/*
 * SPDX-FileCopyrightText: 2022 Atos, 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.zac.mailtemplates;

import static net.atos.zac.util.ValidationUtil.valideerObject;

import java.util.List;
import java.util.Optional;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.transaction.Transactional;

import net.atos.zac.mailtemplates.model.Mail;
import net.atos.zac.mailtemplates.model.MailTemplate;

@ApplicationScoped
@Transactional
public class MailTemplateService {
    private EntityManager entityManager;

    /**
     * Default no-arg constructor, required by Weld.
     */
    public MailTemplateService() {
    }

    @Inject
    public MailTemplateService(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    public Optional<MailTemplate> findMailtemplate(final long id) {
        final var mailTemplate = entityManager.find(MailTemplate.class, id);
        return mailTemplate != null ? Optional.of(mailTemplate) : Optional.empty();
    }

    public Optional<MailTemplate> findMailtemplateByName(final String mailTemplateName) {
        final var mailTemplate = entityManager.find(MailTemplate.class, mailTemplateName);
        return mailTemplate != null ? Optional.of(mailTemplate) : Optional.empty();
    }

    public void delete(final Long id) {
        findMailtemplate(id).ifPresent(mailTemplate -> entityManager.remove(mailTemplate));
    }

    public MailTemplate storeMailtemplate(final MailTemplate mailTemplate) {
        valideerObject(mailTemplate);
        if (mailTemplate.getId() != null && findMailtemplate(mailTemplate.getId()).isPresent()) {
            return entityManager.merge(mailTemplate);
        } else {
            entityManager.persist(mailTemplate);
            return mailTemplate;
        }
    }

    public Optional<MailTemplate> findDefaultMailtemplate(final Mail mail) {
        final CriteriaBuilder builder = entityManager.getCriteriaBuilder();
        final CriteriaQuery<MailTemplate> query = builder.createQuery(MailTemplate.class);
        final Root<MailTemplate> root = query.from(MailTemplate.class);
        final Predicate equalPredicate = builder.equal(root.get(MailTemplate.MAIL), mail);
        final Predicate defaultPredicate = builder.equal(root.get(MailTemplate.DEFAULT_MAILTEMPLATE), true);
        final Predicate finalPredicate = builder.and(equalPredicate, defaultPredicate);
        query.select(root).where(finalPredicate);
        final List<MailTemplate> resultList = entityManager.createQuery(query).getResultList();
        return resultList.isEmpty() ? Optional.empty() : Optional.of(resultList.getFirst());
    }

    public MailTemplate readMailtemplate(final Mail mail) {
        return findDefaultMailtemplate(mail)
                .orElseThrow(() -> new RuntimeException(
                        "%s for '%s' not found".formatted(MailTemplate.class.getSimpleName(), mail)));
    }

    public MailTemplate readMailtemplate(final long id) {
        final MailTemplate mailTemplate = entityManager.find(MailTemplate.class, id);
        if (mailTemplate != null) {
            return mailTemplate;
        } else {
            throw new RuntimeException("%s with id=%d not found".formatted(MailTemplate.class.getSimpleName(), id));
        }
    }

    public List<MailTemplate> listMailtemplates() {
        final CriteriaBuilder builder = entityManager.getCriteriaBuilder();
        final CriteriaQuery<net.atos.zac.mailtemplates.model.MailTemplate> query = builder.createQuery(
                net.atos.zac.mailtemplates.model.MailTemplate.class);
        final Root<net.atos.zac.mailtemplates.model.MailTemplate> root = query.from(
                net.atos.zac.mailtemplates.model.MailTemplate.class);
        query.orderBy(builder.asc(root.get("mailTemplateNaam")));
        query.select(root);
        return entityManager.createQuery(query).getResultList();
    }

    public List<MailTemplate> listKoppelbareMailtemplates() {
        final CriteriaBuilder builder = entityManager.getCriteriaBuilder();
        final CriteriaQuery<net.atos.zac.mailtemplates.model.MailTemplate> query = builder.createQuery(
                net.atos.zac.mailtemplates.model.MailTemplate.class);
        final Root<net.atos.zac.mailtemplates.model.MailTemplate> root = query.from(
                net.atos.zac.mailtemplates.model.MailTemplate.class);
        query.where(root.get(MailTemplate.MAIL).in(Mail.getKoppelbareMails()));

        return entityManager.createQuery(query).getResultList();
    }
}
