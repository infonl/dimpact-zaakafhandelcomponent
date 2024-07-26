/*
 * SPDX-FileCopyrightText: 2022 Atos
 * SPDX-License-Identifier: EUPL-1.2+
 */

package net.atos.zac.admin;

import static jakarta.transaction.Transactional.TxType.SUPPORTS;

import java.util.List;
import java.util.Optional;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Root;
import jakarta.transaction.Transactional;

import net.atos.zac.admin.model.ReferenceTable;

@ApplicationScoped
@Transactional(SUPPORTS)
public class ReferenceTableService {

    @PersistenceContext(unitName = "ZaakafhandelcomponentPU")
    private EntityManager entityManager;

    public List<ReferenceTable> listReferenceTables() {
        final CriteriaBuilder builder = entityManager.getCriteriaBuilder();
        final CriteriaQuery<ReferenceTable> query = builder.createQuery(ReferenceTable.class);
        final Root<ReferenceTable> root = query.from(ReferenceTable.class);
        query.orderBy(builder.asc(root.get("naam")));
        query.select(root);
        return entityManager.createQuery(query).getResultList();
    }

    public ReferenceTable readReferenceTable(final long id) {
        final ReferenceTable referenceTable = entityManager.find(ReferenceTable.class, id);
        if (referenceTable != null) {
            return referenceTable;
        } else {
            throw new RuntimeException("%s with id=%d not found".formatted(ReferenceTable.class.getSimpleName(), id));
        }
    }

    public ReferenceTable readReferenceTable(final String code) {
        return findReferenceTable(code)
                .orElseThrow(
                        () -> new RuntimeException("%s with code='%s' not found".formatted(ReferenceTable.class.getSimpleName(), code))
                );
    }

    public Optional<ReferenceTable> findReferenceTable(final String code) {
        final CriteriaBuilder builder = entityManager.getCriteriaBuilder();
        final CriteriaQuery<ReferenceTable> query = builder.createQuery(ReferenceTable.class);
        final Root<ReferenceTable> root = query.from(ReferenceTable.class);
        query.select(root).where(builder.equal(root.get("code"), code));
        final List<ReferenceTable> resultList = entityManager.createQuery(query).getResultList();
        return resultList.isEmpty() ? Optional.empty() : Optional.of(resultList.getFirst());
    }
}
