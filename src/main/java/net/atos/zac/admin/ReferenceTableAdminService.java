/*
 * SPDX-FileCopyrightText: 2022 Atos, 2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */

package net.atos.zac.admin;

import static jakarta.transaction.Transactional.TxType.REQUIRED;
import static jakarta.transaction.Transactional.TxType.SUPPORTS;
import static net.atos.zac.util.ValidationUtil.valideerObject;

import java.util.List;
import java.util.stream.Collectors;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Root;
import jakarta.transaction.Transactional;

import net.atos.zac.admin.model.HumanTaskReferentieTabel;
import net.atos.zac.admin.model.ReferenceTable;
import net.atos.zac.shared.exception.FoutmeldingException;

@ApplicationScoped
@Transactional(SUPPORTS)
public class ReferenceTableAdminService {
    private static final String UNIQUE_CONSTRAINT = "Er bestaat al een referentietabel met de code \"%s\".";
    private static final String FOREIGN_KEY_CONSTRAINT = "Deze referentietabel wordt gebruikt (voor: %s) en kan niet verwijderd worden.";

    @PersistenceContext(unitName = "ZaakafhandelcomponentPU")
    private EntityManager entityManager;

    private ReferenceTableService referenceTableService;

    /**
     * Default no-arg constructor, required by Weld.
     */
    public ReferenceTableAdminService() {
    }

    @Inject
    public ReferenceTableAdminService(
            final ReferenceTableService referenceTableService
    ) {
        this.referenceTableService = referenceTableService;
    }

    @Transactional(REQUIRED)
    public ReferenceTable newReferenceTable() {
        final ReferenceTable nieuw = new ReferenceTable();
        nieuw.setCode(getUniqueCodeForReferenceTable(1, referenceTableService.listReferenceTables()));
        nieuw.setNaam("Nieuwe referentietabel");
        return nieuw;
    }

    @Transactional(REQUIRED)
    public ReferenceTable createReferenceTable(final ReferenceTable referenceTable) {
        return updateReferenceTable(referenceTable);
    }

    @Transactional(REQUIRED)
    public ReferenceTable updateReferenceTable(final ReferenceTable referenceTable) {
        valideerObject(referenceTable);
        referenceTableService.findReferenceTable(referenceTable.getCode())
                .ifPresent(existing -> {
                    if (!existing.getId().equals(referenceTable.getId())) {
                        throw new FoutmeldingException(String.format(UNIQUE_CONSTRAINT, referenceTable.getCode()));
                    }
                });
        return entityManager.merge(referenceTable);
    }

    @Transactional(REQUIRED)
    public void deleteReferenceTable(final long id) {
        final ReferenceTable tabel = entityManager.find(ReferenceTable.class, id);
        final CriteriaBuilder builder = entityManager.getCriteriaBuilder();
        final CriteriaQuery<HumanTaskReferentieTabel> query = builder.createQuery(HumanTaskReferentieTabel.class);
        final Root<HumanTaskReferentieTabel> root = query.from(HumanTaskReferentieTabel.class);
        query.select(root).where(builder.equal(root.get("tabel").get("id"), tabel.getId()));
        final List<HumanTaskReferentieTabel> resultList = entityManager.createQuery(query).getResultList();
        if (!resultList.isEmpty()) {
            throw new FoutmeldingException(String.format(FOREIGN_KEY_CONSTRAINT, resultList.stream()
                    .map(HumanTaskReferentieTabel::getVeld)
                    .distinct()
                    .collect(Collectors.joining(", "))
            ));
        }
        entityManager.remove(tabel);
    }

    private String getUniqueCodeForReferenceTable(final int i, final List<ReferenceTable> list) {
        final String code = "TABEL" + (1 < i ? i : "");
        if (list.stream()
                .anyMatch(referentieTabel -> code.equals(referentieTabel.getCode()))) {
            return getUniqueCodeForReferenceTable(i + 1, list);
        }
        return code;
    }
}
