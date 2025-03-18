/*
 * SPDX-FileCopyrightText: 2022 Atos
 * SPDX-License-Identifier: EUPL-1.2+
 */

package net.atos.zac.documenten;

import static nl.info.client.zgw.util.UriUtilsKt.extractUuid;

import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import jakarta.enterprise.context.ApplicationScoped;
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

import net.atos.client.zgw.drc.DrcClientService;
import net.atos.client.zgw.shared.util.DateTimeUtil;
import net.atos.client.zgw.zrc.ZrcClientService;
import net.atos.client.zgw.zrc.model.ZaakInformatieobject;
import net.atos.zac.documenten.model.InboxDocument;
import net.atos.zac.documenten.model.InboxDocumentListParameters;
import net.atos.zac.search.model.DatumRange;
import net.atos.zac.shared.model.SorteerRichting;
import nl.info.client.zgw.drc.model.generated.EnkelvoudigInformatieObject;

@ApplicationScoped
@Transactional
public class InboxDocumentenService {

    private static final String LIKE = "%%%s%%";

    @PersistenceContext(unitName = "ZaakafhandelcomponentPU")
    private EntityManager entityManager;

    @Inject
    private ZrcClientService zrcClientService;

    @Inject
    private DrcClientService drcClientService;

    public InboxDocument create(final UUID enkelvoudiginformatieobjectUUID) {
        final EnkelvoudigInformatieObject informatieobject = drcClientService.readEnkelvoudigInformatieobject(
                enkelvoudiginformatieobjectUUID
        );
        final InboxDocument inboxDocument = new InboxDocument();
        inboxDocument.setEnkelvoudiginformatieobjectID(informatieobject.getIdentificatie());
        inboxDocument.setEnkelvoudiginformatieobjectUUID(enkelvoudiginformatieobjectUUID);
        inboxDocument.setCreatiedatum(informatieobject.getCreatiedatum().atStartOfDay(ZoneId.systemDefault()));
        inboxDocument.setTitel(informatieobject.getTitel());
        inboxDocument.setBestandsnaam(informatieobject.getBestandsnaam());
        entityManager.persist(inboxDocument);
        return inboxDocument;
    }

    public Optional<InboxDocument> find(final long id) {
        final var inboxDocument = entityManager.find(InboxDocument.class, id);
        return inboxDocument != null ? Optional.of(inboxDocument) : Optional.empty();
    }

    public Optional<InboxDocument> find(final UUID enkelvoudiginformatieobjectUUID) {
        final CriteriaBuilder builder = entityManager.getCriteriaBuilder();
        final CriteriaQuery<InboxDocument> query = builder.createQuery(InboxDocument.class);
        final Root<InboxDocument> root = query.from(InboxDocument.class);
        query.select(root).where(builder.equal(root.get(InboxDocument.ENKELVOUDIGINFORMATIEOBJECT_UUID),
                enkelvoudiginformatieobjectUUID));
        final List<InboxDocument> resultList = entityManager.createQuery(query).getResultList();
        return resultList.isEmpty() ? Optional.empty() : Optional.of(resultList.getFirst());
    }

    public InboxDocument read(final UUID enkelvoudiginformatieobjectUUID) {
        return find(enkelvoudiginformatieobjectUUID).orElseThrow(() -> new RuntimeException(
                "InboxDocument with uuid '%s' not found.".formatted(enkelvoudiginformatieobjectUUID)));
    }

    public int count(final InboxDocumentListParameters listParameters) {
        final CriteriaBuilder builder = entityManager.getCriteriaBuilder();
        final CriteriaQuery<Long> query = builder.createQuery(Long.class);
        final Root<InboxDocument> root = query.from(InboxDocument.class);
        query.where(getWhere(listParameters, root));
        query.select(builder.count(root));
        final Long result = entityManager.createQuery(query).getSingleResult();
        if (result == null) {
            return 0;
        }
        return result.intValue();
    }

    public void delete(final Long id) {
        find(id).ifPresent(entityManager::remove);
    }

    public void delete(final UUID zaakinformatieobjectUUID) {
        final ZaakInformatieobject zaakInformatieobject = zrcClientService.readZaakinformatieobject(
                zaakinformatieobjectUUID);
        final UUID enkelvoudiginformatieobjectUUID = extractUuid(zaakInformatieobject.getInformatieobject());
        find(enkelvoudiginformatieobjectUUID).ifPresent(entityManager::remove);
    }

    public List<InboxDocument> list(final InboxDocumentListParameters listParameters) {
        final CriteriaBuilder builder = entityManager.getCriteriaBuilder();
        final CriteriaQuery<InboxDocument> query = builder.createQuery(InboxDocument.class);
        final Root<InboxDocument> root = query.from(InboxDocument.class);
        if (listParameters.getSorting() != null) {
            if (listParameters.getSorting().getDirection() == SorteerRichting.ASCENDING) {
                query.orderBy(builder.asc(root.get(listParameters.getSorting().getField())));
            } else {
                query.orderBy(builder.desc(root.get(listParameters.getSorting().getField())));
            }
        }
        query.where(getWhere(listParameters, root));
        final TypedQuery<InboxDocument> emQuery = entityManager.createQuery(query);
        if (listParameters.getPaging() != null) {
            emQuery.setFirstResult(listParameters.getPaging().getFirstResult());
            emQuery.setMaxResults(listParameters.getPaging().getMaxResults());
        }
        return emQuery.getResultList();
    }

    private Predicate getWhere(final InboxDocumentListParameters listParameters, final Root<InboxDocument> root) {
        final CriteriaBuilder builder = entityManager.getCriteriaBuilder();
        final List<Predicate> predicates = new ArrayList<>();
        if (StringUtils.isNotBlank(listParameters.getIdentificatie())) {
            predicates.add(builder.like(root.get(InboxDocument.ENKELVOUDIGINFORMATIEOBJECT_ID),
                    LIKE.formatted(listParameters.getIdentificatie())));
        }
        if (StringUtils.isNotBlank(listParameters.getTitel())) {
            String titel = LIKE.formatted(listParameters.getTitel().toLowerCase().replace(" ", "%"));
            predicates.add(builder.like(builder.lower(root.get(InboxDocument.TITEL)), titel));
        }
        addCreatiedatumPredicates(listParameters.getCreatiedatum(), predicates, root, builder);
        return builder.and(predicates.toArray(new Predicate[0]));
    }

    private void addCreatiedatumPredicates(
            final DatumRange creatiedatum,
            final List<Predicate> predicates,
            final Root<InboxDocument> root,
            final CriteriaBuilder builder
    ) {
        if (creatiedatum != null) {
            if (creatiedatum.getVan() != null) {
                predicates.add(builder.greaterThanOrEqualTo(root.get(InboxDocument.CREATIEDATUM),
                        DateTimeUtil.convertToDateTime(creatiedatum.getVan())));
            }
            if (creatiedatum.getTot() != null) {
                predicates.add(builder.lessThanOrEqualTo(root.get(InboxDocument.CREATIEDATUM),
                        DateTimeUtil.convertToDateTime(creatiedatum.getTot()).plusDays(1)
                                .minusSeconds(1)));
            }
        }
    }
}
