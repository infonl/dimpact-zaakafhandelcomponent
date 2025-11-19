/*
 * SPDX-FileCopyrightText: 2022 Atos, 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.zac.admin;

import static net.atos.zac.util.ValidationUtil.valideerObject;

import java.util.List;
import java.util.Optional;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Root;
import jakarta.transaction.Transactional;

import nl.info.zac.admin.model.ZaaktypeCmmnMailtemplateParameters;

@ApplicationScoped
@Transactional
public class MailTemplateKoppelingenService {
    private EntityManager entityManager;

    /**
     * Default no-arg constructor, required by Weld.
     */
    public MailTemplateKoppelingenService() {
    }

    @Inject
    public MailTemplateKoppelingenService(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    public Optional<ZaaktypeCmmnMailtemplateParameters> find(final long id) {
        final var mailtemplateKoppeling = entityManager.find(ZaaktypeCmmnMailtemplateParameters.class, id);
        return mailtemplateKoppeling != null ? Optional.of(mailtemplateKoppeling) : Optional.empty();
    }

    public void delete(final Long id) {
        find(id).ifPresent(entityManager::remove);
    }

    public ZaaktypeCmmnMailtemplateParameters storeMailtemplateKoppeling(
            final ZaaktypeCmmnMailtemplateParameters zaaktypeCmmnMailtemplateParameters
    ) {
        valideerObject(zaaktypeCmmnMailtemplateParameters);
        if (zaaktypeCmmnMailtemplateParameters.getId() != null && find(zaaktypeCmmnMailtemplateParameters.getId()).isPresent()) {
            return entityManager.merge(zaaktypeCmmnMailtemplateParameters);
        } else {
            entityManager.persist(zaaktypeCmmnMailtemplateParameters);
            return zaaktypeCmmnMailtemplateParameters;
        }
    }

    public ZaaktypeCmmnMailtemplateParameters readMailtemplateKoppeling(final long id) {
        final ZaaktypeCmmnMailtemplateParameters zaaktypeCmmnMailtemplateParameters = entityManager.find(
                ZaaktypeCmmnMailtemplateParameters.class, id);
        if (zaaktypeCmmnMailtemplateParameters != null) {
            return zaaktypeCmmnMailtemplateParameters;
        } else {
            throw new RuntimeException(String.format("%s with id=%d not found",
                    ZaaktypeCmmnMailtemplateParameters.class.getSimpleName(), id));
        }
    }

    public List<ZaaktypeCmmnMailtemplateParameters> listMailtemplateKoppelingen() {
        final CriteriaBuilder builder = entityManager.getCriteriaBuilder();
        final CriteriaQuery<ZaaktypeCmmnMailtemplateParameters> query = builder.createQuery(ZaaktypeCmmnMailtemplateParameters.class);
        final Root<ZaaktypeCmmnMailtemplateParameters> root = query.from(ZaaktypeCmmnMailtemplateParameters.class);
        query.select(root);
        return entityManager.createQuery(query).getResultList();
    }
}
