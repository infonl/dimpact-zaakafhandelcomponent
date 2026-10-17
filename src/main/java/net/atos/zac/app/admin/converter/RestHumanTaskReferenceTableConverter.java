/*
 * SPDX-FileCopyrightText: 2022 Atos, 2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */

package net.atos.zac.app.admin.converter;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import jakarta.inject.Inject;

import net.atos.zac.admin.model.FormulierVeldDefinitie;
import net.atos.zac.admin.model.HumanTaskReferentieTabel;
import net.atos.zac.app.admin.model.RestHumanTaskReferenceTable;
import nl.info.zac.admin.ReferenceTableService;
import nl.info.zac.admin.model.ReferenceTable;
import nl.info.zac.admin.model.ReferenceTableKt;


public class RestHumanTaskReferenceTableConverter {

    @Inject
    private ReferenceTableService referenceTableService;

    public RestHumanTaskReferenceTable convertDefault(final FormulierVeldDefinitie veldDefinitie) {
        final ReferenceTable referenceTable = referenceTableService.readReferenceTable(veldDefinitie.getDefaultTabel().name());
        final RestHumanTaskReferenceTable restHumanTaskReferenceTable = new RestHumanTaskReferenceTable(veldDefinitie);
        restHumanTaskReferenceTable.tabel = ReferenceTableKt.toRestReferenceTable(referenceTable, false);
        return restHumanTaskReferenceTable;
    }

    public List<RestHumanTaskReferenceTable> convert(final Collection<HumanTaskReferentieTabel> humanTaskReferentieTabellen) {
        return humanTaskReferentieTabellen.stream()
                .map(RestHumanTaskReferenceTableConverter::convert)
                .collect(Collectors.toList());
    }

    public List<HumanTaskReferentieTabel> convert(final List<RestHumanTaskReferenceTable> restHumanTaskrestHumanTaskReferentieTabellen) {
        return restHumanTaskrestHumanTaskReferentieTabellen.stream()
                .map(this::convert)
                .toList();
    }

    private static RestHumanTaskReferenceTable convert(final HumanTaskReferentieTabel humanTaskReferentieTabel) {
        final RestHumanTaskReferenceTable restHumanTaskReferenceTable = new RestHumanTaskReferenceTable();
        restHumanTaskReferenceTable.id = humanTaskReferentieTabel.getId();
        restHumanTaskReferenceTable.veld = humanTaskReferentieTabel.getVeld();
        restHumanTaskReferenceTable.tabel = ReferenceTableKt.toRestReferenceTable(humanTaskReferentieTabel.getTabel(), false);
        return restHumanTaskReferenceTable;
    }

    private HumanTaskReferentieTabel convert(final RestHumanTaskReferenceTable restHumanTaskReferenceTable) {
        HumanTaskReferentieTabel humanTaskReferentieTabel = new HumanTaskReferentieTabel();
        humanTaskReferentieTabel.setId(restHumanTaskReferenceTable.id);
        humanTaskReferentieTabel.setVeld(restHumanTaskReferenceTable.veld);
        // The tabel is just a reference here, so don't update it from the REST but fetch it from the database
        humanTaskReferentieTabel.setTabel(referenceTableService.readReferenceTable(restHumanTaskReferenceTable.tabel.getId()));
        return humanTaskReferentieTabel;
    }
}
