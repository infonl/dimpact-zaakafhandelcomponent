/*
 * SPDX-FileCopyrightText: 2022 Atos, 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.zac.document;

import static net.atos.zac.util.ValidationUtil.valideerObject;
import static nl.info.client.zgw.util.ZgwUriUtilsKt.extractUuid;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.transaction.Transactional;

import org.apache.commons.lang3.StringUtils;

import net.atos.client.zgw.shared.util.DateTimeUtil;
import net.atos.zac.document.model.DetachedDocument;
import net.atos.zac.document.model.DetachedDocumentListParameters;
import net.atos.zac.document.model.DetachedDocumentResult;
import nl.info.client.zgw.drc.model.generated.EnkelvoudigInformatieObject;
import nl.info.client.zgw.zrc.model.generated.Zaak;
import nl.info.zac.authentication.LoggedInUser;
import nl.info.zac.search.model.DatumRange;
import nl.info.zac.shared.model.SorteerRichting;

@ApplicationScoped
@Transactional
public class DetachedDocumentService {

    private static final String LIKE = "%%%s%%";

    private EntityManager entityManager;
    private Instance<LoggedInUser> loggedInUserInstance;

    @SuppressWarnings("unused")
    public DetachedDocumentService() {
        // Default constructor for CDI
    }

    @Inject
    public DetachedDocumentService(
            final EntityManager entityManager,
            final Instance<LoggedInUser> loggedInUserInstance
    ) {
        this.entityManager = entityManager;
        this.loggedInUserInstance = loggedInUserInstance;
    }


    public DetachedDocument create(
            final EnkelvoudigInformatieObject informatieobject,
            final Zaak zaak,
            final String reden
    ) {
        final DetachedDocument detachedDocument = new DetachedDocument();
        detachedDocument.setDocumentID(informatieobject.getIdentificatie());
        detachedDocument.setDocumentUUID(extractUuid(informatieobject.getUrl()));
        detachedDocument.setCreatiedatum(informatieobject.getCreatiedatum());
        detachedDocument.setTitel(informatieobject.getTitel());
        detachedDocument.setBestandsnaam(informatieobject.getBestandsnaam());
        detachedDocument.setOntkoppeldOp(ZonedDateTime.now());
        detachedDocument.setOntkoppeldDoor(loggedInUserInstance.get().getId());
        detachedDocument.setZaakID(zaak.getIdentificatie());
        detachedDocument.setReden(reden);
        valideerObject(detachedDocument);
        entityManager.persist(detachedDocument);
        return detachedDocument;
    }

    public DetachedDocumentResult getResultaat(final DetachedDocumentListParameters listParameters) {
        return new DetachedDocumentResult(
                list(listParameters),
                count(listParameters),
                getOntkoppeldDoor(listParameters)
        );
    }

    /**
     * Returns the detach document for the provided enkelvoudiginformatieobject UUID, if it exists,
     * or null otherwise.
     *
     * @param enkelvoudiginformatieobjectUUID the enkelvoudiginformatieobject UUID
     * @return the detached document, or null if no detached document exists for the enkelvoudiginformatieobject UUID
     */
    public DetachedDocument read(final UUID enkelvoudiginformatieobjectUUID) {
        final CriteriaBuilder builder = entityManager.getCriteriaBuilder();
        final CriteriaQuery<DetachedDocument> query = builder.createQuery(DetachedDocument.class);
        final Root<DetachedDocument> root = query.from(DetachedDocument.class);
        query.select(root).where(builder.equal(root.get("documentUUID"), enkelvoudiginformatieobjectUUID));
        final List<DetachedDocument> resultList = entityManager.createQuery(query).getResultList();
        if (!resultList.isEmpty()) {
            return resultList.getFirst();
        } else {
            return null;
        }
    }

    public Optional<DetachedDocument> find(final long id) {
        final var detachedDocument = entityManager.find(DetachedDocument.class, id);
        return detachedDocument != null ? Optional.of(detachedDocument) : Optional.empty();
    }

    private int count(final DetachedDocumentListParameters listParameters) {
        final CriteriaBuilder builder = entityManager.getCriteriaBuilder();
        final CriteriaQuery<Long> query = builder.createQuery(Long.class);
        final Root<DetachedDocument> root = query.from(DetachedDocument.class);
        query.select(builder.count(root));
        query.where(getWhere(listParameters, root));
        final Long result = entityManager.createQuery(query).getSingleResult();
        if (result == null) {
            return 0;
        }
        return result.intValue();
    }

    private List<DetachedDocument> list(final DetachedDocumentListParameters listParameters) {
        final CriteriaBuilder builder = entityManager.getCriteriaBuilder();
        final CriteriaQuery<DetachedDocument> query = builder.createQuery(DetachedDocument.class);
        final Root<DetachedDocument> root = query.from(DetachedDocument.class);
        if (listParameters.getSorting() != null) {
            if (listParameters.getSorting().getDirection() == SorteerRichting.ASCENDING) {
                query.orderBy(builder.asc(root.get(listParameters.getSorting().getField())));
            } else {
                query.orderBy(builder.desc(root.get(listParameters.getSorting().getField())));
            }
        }
        query.where(getWhere(listParameters, root));
        final TypedQuery<DetachedDocument> emQuery = entityManager.createQuery(query);
        if (listParameters.getPaging() != null) {
            emQuery.setFirstResult(listParameters.getPaging().getFirstResult());
            emQuery.setMaxResults(listParameters.getPaging().getMaxResults());
        }
        return emQuery.getResultList();
    }

    private List<String> getOntkoppeldDoor(final DetachedDocumentListParameters listParameters) {
        final CriteriaBuilder builder = entityManager.getCriteriaBuilder();
        final CriteriaQuery<String> query = builder.createQuery(String.class);
        final Root<DetachedDocument> root = query.from(DetachedDocument.class);
        query.select(root.get(DetachedDocument.ONTKOPPELD_DOOR_PROPERTY_NAME)).distinct(true);
        query.where(getWhere(listParameters, root));
        return entityManager.createQuery(query).getResultList();
    }

    public void delete(final Long id) {
        find(id).ifPresent(detachedDocument -> entityManager.remove(detachedDocument));
    }

    public void delete(final UUID uuid) {
        final DetachedDocument detachedDocument = read(uuid);
        if (detachedDocument != null) {
            entityManager.remove(detachedDocument);
        }
    }

    private Predicate getWhere(
            final DetachedDocumentListParameters listParameters,
            final Root<DetachedDocument> root
    ) {
        final CriteriaBuilder builder = entityManager.getCriteriaBuilder();
        final List<Predicate> predicates = new ArrayList<>();
        if (StringUtils.isNotBlank(listParameters.getZaakID())) {
            predicates.add(
                    builder.like(root.get(DetachedDocument.ZAAK_ID_PROPERTY_NAME), LIKE.formatted(listParameters.getZaakID())));
        }
        if (StringUtils.isNotBlank(listParameters.getTitel())) {
            String titel = LIKE.formatted(listParameters.getTitel().toLowerCase().replace(" ", "%"));
            predicates.add(builder.like(builder.lower(root.get(DetachedDocument.TITEL_PROPERTY_NAME)), titel));
        }
        if (StringUtils.isNotBlank(listParameters.getReden())) {
            String reden = LIKE.formatted(listParameters.getReden().toLowerCase().replace(" ", "%"));
            predicates.add(builder.like(builder.lower(root.get(DetachedDocument.REDEN_PROPERTY_NAME)), reden));
        }

        if (StringUtils.isNotBlank(listParameters.getOntkoppeldDoor())) {
            predicates.add(
                    builder.equal(root.get(DetachedDocument.ONTKOPPELD_DOOR_PROPERTY_NAME), listParameters.getOntkoppeldDoor()));
        }
        addDatumRangePredicates(listParameters.getCreatiedatum(), DetachedDocument.CREATIEDATUM_PROPERTY_NAME, predicates, root,
                builder);
        addDatumRangePredicates(listParameters.getOntkoppeldOp(), DetachedDocument.ONTKOPPELD_OP_PROPERTY_NAME, predicates, root,
                builder);

        return builder.and(predicates.toArray(new Predicate[0]));
    }


    private void addDatumRangePredicates(
            final DatumRange datumRange,
            final String veld,
            final List<Predicate> predicates,
            final Root<DetachedDocument> root,
            final CriteriaBuilder builder
    ) {
        if (datumRange != null) {
            if (datumRange.getVan() != null) {
                predicates.add(builder.greaterThanOrEqualTo(root.get(veld),
                        DateTimeUtil.convertToDateTime(datumRange.getVan())));
            }
            if (datumRange.getTot() != null) {
                predicates.add(builder.lessThanOrEqualTo(root.get(veld),
                        DateTimeUtil.convertToDateTime(datumRange.getTot()).plusDays(1)
                                .minusSeconds(1)));
            }
        }
    }

}
