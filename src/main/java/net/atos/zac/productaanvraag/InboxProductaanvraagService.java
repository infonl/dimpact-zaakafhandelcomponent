/*
 * SPDX-FileCopyrightText: 2022 Atos, 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.zac.productaanvraag;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.transaction.Transactional;

import org.apache.commons.lang3.StringUtils;

import net.atos.zac.productaanvraag.model.InboxProductaanvraag;
import net.atos.zac.productaanvraag.model.InboxProductaanvraagListParameters;
import net.atos.zac.productaanvraag.model.InboxProductaanvraagResultaat;
import nl.info.zac.shared.model.SorteerRichting;

import java.util.logging.Logger;

@ApplicationScoped
@Transactional
public class InboxProductaanvraagService {
    private static final String LIKE = "%%%s%%";
    private static final Logger LOG = Logger.getLogger(InboxProductaanvraagService.class.getName());

    private EntityManager entityManager;

    /**
     * Default no-arg constructor, required by Weld.
     */
    public InboxProductaanvraagService() {
    }

    @Inject
    public InboxProductaanvraagService(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    public void create(final InboxProductaanvraag inboxProductaanvraag) {
        entityManager.persist(inboxProductaanvraag);
    }

    public InboxProductaanvraagResultaat list(final InboxProductaanvraagListParameters listParameters) {
        return new InboxProductaanvraagResultaat(query(listParameters), count(listParameters), listTypes(listParameters));
    }

    private List<String> listTypes(final InboxProductaanvraagListParameters listParameters) {
        final CriteriaBuilder builder = entityManager.getCriteriaBuilder();
        final CriteriaQuery<String> query = builder.createQuery(String.class);
        final Root<InboxProductaanvraag> root = query.from(InboxProductaanvraag.class);
        query.select(root.get(InboxProductaanvraag.TYPE)).distinct(true);
        query.where(getWhere(listParameters, root));
        return entityManager.createQuery(query).getResultList();
    }

    private List<InboxProductaanvraag> query(final InboxProductaanvraagListParameters listParameters) {
        LOG.info("Querying inbox productaanvragen with parameters: " + listParameters);
        final CriteriaBuilder builder = entityManager.getCriteriaBuilder();
        final CriteriaQuery<InboxProductaanvraag> query = builder.createQuery(InboxProductaanvraag.class);
        final Root<InboxProductaanvraag> root = query.from(InboxProductaanvraag.class);
        if (listParameters.getSorting() != null) {
            if (listParameters.getSorting().getDirection() == SorteerRichting.ASCENDING) {
                query.orderBy(builder.asc(root.get(listParameters.getSorting().getField())));
            } else {
                query.orderBy(builder.desc(root.get(listParameters.getSorting().getField())));
            }
        }
        query.where(getWhere(listParameters, root));
        final TypedQuery<InboxProductaanvraag> emQuery = entityManager.createQuery(query);
        if (listParameters.getPaging() != null) {
            emQuery.setFirstResult(listParameters.getPaging().getFirstResult());
            emQuery.setMaxResults(listParameters.getPaging().getMaxResults());
        }
        return emQuery.getResultList();
    }

    public void delete(final Long id) {
        find(id).ifPresent(entityManager::remove);
    }

    public Optional<InboxProductaanvraag> find(final long id) {
        final InboxProductaanvraag inboxProductaanvraag = entityManager.find(InboxProductaanvraag.class, id);
        return inboxProductaanvraag != null ? Optional.of(inboxProductaanvraag) : Optional.empty();
    }

    private int count(final InboxProductaanvraagListParameters listParameters) {
        final CriteriaBuilder builder = entityManager.getCriteriaBuilder();
        final CriteriaQuery<Long> query = builder.createQuery(Long.class);
        final Root<InboxProductaanvraag> root = query.from(InboxProductaanvraag.class);
        query.select(builder.count(root));
        query.where(getWhere(listParameters, root));
        final Long result = entityManager.createQuery(query).getSingleResult();
        if (result == null) {
            return 0;
        }
        return result.intValue();
    }

    private Predicate getWhere(final InboxProductaanvraagListParameters listParameters, final Root<InboxProductaanvraag> root) {
        final CriteriaBuilder builder = entityManager.getCriteriaBuilder();
        final List<Predicate> predicates = new ArrayList<>();
        if (StringUtils.isNotBlank(listParameters.getInitiatorID())) {
            predicates.add(
                    builder.like(root.get(InboxProductaanvraag.INITIATOR), LIKE.formatted(listParameters.getInitiatorID())));
        }

        if (StringUtils.isNotBlank(listParameters.getType())) {
            predicates.add(builder.equal(root.get(InboxProductaanvraag.TYPE), listParameters.getType()));
        }

        if (listParameters.getOntvangstdatum() != null) {
            if (listParameters.getOntvangstdatum().getVan() != null) {
                predicates.add(builder.greaterThanOrEqualTo(root.get(InboxProductaanvraag.ONTVANGSTDATUM), listParameters
                        .getOntvangstdatum().getVan()));
            }
            if (listParameters.getOntvangstdatum().getTot() != null) {
                predicates.add(builder.lessThanOrEqualTo(root.get(InboxProductaanvraag.ONTVANGSTDATUM), listParameters.getOntvangstdatum()
                        .getTot()));
            }
        }
        return builder.and(predicates.toArray(new Predicate[0]));
    }

}
