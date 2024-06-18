/*
 * SPDX-FileCopyrightText: 2022 Atos
 * SPDX-License-Identifier: EUPL-1.2+
 */

package net.atos.zac.zaaksturing;

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

import net.atos.zac.shared.exception.FoutmeldingException;
import net.atos.zac.zaaksturing.model.HumanTaskReferentieTabel;
import net.atos.zac.zaaksturing.model.ReferentieTabel;

@ApplicationScoped
@Transactional(SUPPORTS)
public class ReferentieTabelBeheerService {
    private static final String UNIQUE_CONSTRAINT = "Er bestaat al een referentietabel met de code \"%s\".";
    private static final String FOREIGN_KEY_CONSTRAINT = "Deze referentietabel wordt gebruikt (voor: %s) en kan niet verwijderd worden.";

    @PersistenceContext(unitName = "ZaakafhandelcomponentPU")
    private EntityManager entityManager;

    private ReferentieTabelService referentieTabelService;

    /**
     * Default no-arg constructor, required by Weld.
     */
    public ReferentieTabelBeheerService() {
    }

    @Inject
    public ReferentieTabelBeheerService(
            final ReferentieTabelService referentieTabelService
    ) {
        this.referentieTabelService = referentieTabelService;
    }

    @Transactional(REQUIRED)
    public ReferentieTabel newReferentieTabel() {
        final ReferentieTabel nieuw = new ReferentieTabel();
        nieuw.setCode(getUniqueCode(1, referentieTabelService.listReferentieTabellen()));
        nieuw.setNaam("Nieuwe referentietabel");
        return nieuw;
    }

    @Transactional(REQUIRED)
    public ReferentieTabel createReferentieTabel(final ReferentieTabel referentieTabel) {
        return updateReferentieTabel(referentieTabel);
    }

    @Transactional(REQUIRED)
    public ReferentieTabel updateReferentieTabel(final ReferentieTabel referentieTabel) {
        valideerObject(referentieTabel);
        referentieTabelService.findReferentieTabel(referentieTabel.getCode())
                .ifPresent(existing -> {
                    if (!existing.getId().equals(referentieTabel.getId())) {
                        throw new FoutmeldingException(String.format(UNIQUE_CONSTRAINT, referentieTabel.getCode()));
                    }
                });
        return entityManager.merge(referentieTabel);
    }

    @Transactional(REQUIRED)
    public void deleteReferentieTabel(final long id) {
        final ReferentieTabel tabel = entityManager.find(ReferentieTabel.class, id);
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

    private String getUniqueCode(final int i, final List<ReferentieTabel> list) {
        final String code = "TABEL" + (1 < i ? i : "");
        if (list.stream()
                .anyMatch(referentieTabel -> code.equals(referentieTabel.getCode()))) {
            return getUniqueCode(i + 1, list);
        }
        return code;
    }
}
