/*
 * SPDX-FileCopyrightText: 2022 Atos
 * SPDX-License-Identifier: EUPL-1.2+
 */

package net.atos.zac.documenten;

import static net.atos.zac.util.ValidationUtil.valideerObject;
import static nl.info.client.zgw.util.UriUtilsKt.extractUuid;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.transaction.Transactional;

import org.apache.commons.lang3.StringUtils;

import net.atos.client.zgw.drc.model.generated.EnkelvoudigInformatieObject;
import net.atos.client.zgw.shared.util.DateTimeUtil;
import net.atos.client.zgw.zrc.model.Zaak;
import net.atos.zac.authentication.LoggedInUser;
import net.atos.zac.documenten.model.OntkoppeldDocument;
import net.atos.zac.documenten.model.OntkoppeldDocumentListParameters;
import net.atos.zac.documenten.model.OntkoppeldeDocumentenResultaat;
import net.atos.zac.search.model.DatumRange;
import net.atos.zac.shared.model.SorteerRichting;


@ApplicationScoped
@Transactional
public class OntkoppeldeDocumentenService {

    private static final String LIKE = "%%%s%%";

    @PersistenceContext(unitName = "ZaakafhandelcomponentPU")
    private EntityManager entityManager;

    @Inject
    private Instance<LoggedInUser> loggedInUserInstance;


    public OntkoppeldDocument create(
            final EnkelvoudigInformatieObject informatieobject,
            final Zaak zaak,
            final String reden
    ) {
        final OntkoppeldDocument ontkoppeldDocument = new OntkoppeldDocument();
        ontkoppeldDocument.setDocumentID(informatieobject.getIdentificatie());
        ontkoppeldDocument.setDocumentUUID(extractUuid(informatieobject.getUrl()));
        ontkoppeldDocument.setCreatiedatum(informatieobject.getCreatiedatum().atStartOfDay(ZoneId.systemDefault()));
        ontkoppeldDocument.setTitel(informatieobject.getTitel());
        ontkoppeldDocument.setBestandsnaam(informatieobject.getBestandsnaam());
        ontkoppeldDocument.setOntkoppeldOp(ZonedDateTime.now());
        ontkoppeldDocument.setOntkoppeldDoor(loggedInUserInstance.get().getId());
        ontkoppeldDocument.setZaakID(zaak.getIdentificatie());
        ontkoppeldDocument.setReden(reden);
        valideerObject(ontkoppeldDocument);
        entityManager.persist(ontkoppeldDocument);
        return ontkoppeldDocument;
    }

    public OntkoppeldeDocumentenResultaat getResultaat(final OntkoppeldDocumentListParameters listParameters) {
        return new OntkoppeldeDocumentenResultaat(list(listParameters), count(listParameters),
                getOntkoppeldDoor(listParameters));
    }

    public OntkoppeldDocument read(final UUID enkelvoudiginformatieobjectUUID) {
        final CriteriaBuilder builder = entityManager.getCriteriaBuilder();
        final CriteriaQuery<OntkoppeldDocument> query = builder.createQuery(OntkoppeldDocument.class);
        final Root<OntkoppeldDocument> root = query.from(OntkoppeldDocument.class);
        query.select(root).where(builder.equal(root.get("documentUUID"), enkelvoudiginformatieobjectUUID));
        return entityManager.createQuery(query).getSingleResult();
    }

    public Optional<OntkoppeldDocument> find(final long id) {
        final var ontkoppeldDocument = entityManager.find(OntkoppeldDocument.class, id);
        return ontkoppeldDocument != null ? Optional.of(ontkoppeldDocument) : Optional.empty();
    }

    private int count(final OntkoppeldDocumentListParameters listParameters) {
        final CriteriaBuilder builder = entityManager.getCriteriaBuilder();
        final CriteriaQuery<Long> query = builder.createQuery(Long.class);
        final Root<OntkoppeldDocument> root = query.from(OntkoppeldDocument.class);
        query.select(builder.count(root));
        query.where(getWhere(listParameters, root));
        final Long result = entityManager.createQuery(query).getSingleResult();
        if (result == null) {
            return 0;
        }
        return result.intValue();
    }

    private List<OntkoppeldDocument> list(final OntkoppeldDocumentListParameters listParameters) {
        final CriteriaBuilder builder = entityManager.getCriteriaBuilder();
        final CriteriaQuery<OntkoppeldDocument> query = builder.createQuery(OntkoppeldDocument.class);
        final Root<OntkoppeldDocument> root = query.from(OntkoppeldDocument.class);
        if (listParameters.getSorting() != null) {
            if (listParameters.getSorting().getDirection() == SorteerRichting.ASCENDING) {
                query.orderBy(builder.asc(root.get(listParameters.getSorting().getField())));
            } else {
                query.orderBy(builder.desc(root.get(listParameters.getSorting().getField())));
            }
        }
        query.where(getWhere(listParameters, root));
        final TypedQuery<OntkoppeldDocument> emQuery = entityManager.createQuery(query);
        if (listParameters.getPaging() != null) {
            emQuery.setFirstResult(listParameters.getPaging().getFirstResult());
            emQuery.setMaxResults(listParameters.getPaging().getMaxResults());
        }
        return emQuery.getResultList();
    }

    private List<String> getOntkoppeldDoor(final OntkoppeldDocumentListParameters listParameters) {
        final CriteriaBuilder builder = entityManager.getCriteriaBuilder();
        final CriteriaQuery<String> query = builder.createQuery(String.class);
        final Root<OntkoppeldDocument> root = query.from(OntkoppeldDocument.class);
        query.select(root.get(OntkoppeldDocument.ONTKOPPELD_DOOR)).distinct(true);
        query.where(getWhere(listParameters, root));
        return entityManager.createQuery(query).getResultList();
    }

    public void delete(final Long id) {
        find(id).ifPresent(ontkoppeldDocument -> entityManager.remove(ontkoppeldDocument));
    }

    public void delete(final UUID uuid) {
        final OntkoppeldDocument ontkoppeldDocument = read(uuid);
        if (ontkoppeldDocument != null) {
            entityManager.remove(ontkoppeldDocument);
        }
    }

    private Predicate getWhere(
            final OntkoppeldDocumentListParameters listParameters,
            final Root<OntkoppeldDocument> root
    ) {
        final CriteriaBuilder builder = entityManager.getCriteriaBuilder();
        final List<Predicate> predicates = new ArrayList<>();
        if (StringUtils.isNotBlank(listParameters.getZaakID())) {
            predicates.add(
                    builder.like(root.get(OntkoppeldDocument.ZAAK_ID), LIKE.formatted(listParameters.getZaakID())));
        }
        if (StringUtils.isNotBlank(listParameters.getTitel())) {
            String titel = LIKE.formatted(listParameters.getTitel().toLowerCase().replace(" ", "%"));
            predicates.add(builder.like(builder.lower(root.get(OntkoppeldDocument.TITEL)), titel));
        }
        if (StringUtils.isNotBlank(listParameters.getReden())) {
            String reden = LIKE.formatted(listParameters.getReden().toLowerCase().replace(" ", "%"));
            predicates.add(builder.like(builder.lower(root.get(OntkoppeldDocument.REDEN)), reden));
        }

        if (StringUtils.isNotBlank(listParameters.getOntkoppeldDoor())) {
            predicates.add(
                    builder.equal(root.get(OntkoppeldDocument.ONTKOPPELD_DOOR), listParameters.getOntkoppeldDoor()));
        }
        addDatumRangePredicates(listParameters.getCreatiedatum(), OntkoppeldDocument.CREATIEDATUM, predicates, root,
                builder);
        addDatumRangePredicates(listParameters.getOntkoppeldOp(), OntkoppeldDocument.ONTKOPPELD_OP, predicates, root,
                builder);

        return builder.and(predicates.toArray(new Predicate[0]));
    }


    private void addDatumRangePredicates(
            final DatumRange datumRange,
            final String veld,
            final List<Predicate> predicates,
            final Root<OntkoppeldDocument> root,
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
