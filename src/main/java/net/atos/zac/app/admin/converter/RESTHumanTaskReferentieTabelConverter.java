/*
 * SPDX-FileCopyrightText: 2022 Atos
 * SPDX-License-Identifier: EUPL-1.2+
 */

package net.atos.zac.app.admin.converter;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import jakarta.inject.Inject;

import net.atos.zac.app.admin.model.RESTHumanTaskReferentieTabel;
import net.atos.zac.zaaksturing.ReferentieTabelService;
import net.atos.zac.zaaksturing.model.FormulierVeldDefinitie;
import net.atos.zac.zaaksturing.model.HumanTaskReferentieTabel;

public class RESTHumanTaskReferentieTabelConverter {

    @Inject
    private ReferentieTabelService referentieTabelService;

    public RESTHumanTaskReferentieTabel convertDefault(final FormulierVeldDefinitie veldDefinitie) {
        final RESTHumanTaskReferentieTabel referentieTabel = new RESTHumanTaskReferentieTabel(veldDefinitie);
        referentieTabel.tabel = RESTReferentieTabelConverter.convert(
                referentieTabelService.readReferentieTabel(veldDefinitie.getDefaultTabel().name()),
                false
        );
        return referentieTabel;
    }

    public List<RESTHumanTaskReferentieTabel> convert(final Collection<HumanTaskReferentieTabel> humanTaskReferentieTabellen) {
        return humanTaskReferentieTabellen.stream()
                .map(RESTHumanTaskReferentieTabelConverter::convert)
                .collect(Collectors.toList());
    }

    public List<HumanTaskReferentieTabel> convert(final List<RESTHumanTaskReferentieTabel> restHumanTaskrestHumanTaskReferentieTabellen) {
        return restHumanTaskrestHumanTaskReferentieTabellen.stream()
                .map(this::convert)
                .toList();
    }

    private static RESTHumanTaskReferentieTabel convert(final HumanTaskReferentieTabel humanTaskReferentieTabel) {
        final RESTHumanTaskReferentieTabel restHumanTaskReferentieTabel = new RESTHumanTaskReferentieTabel();
        restHumanTaskReferentieTabel.id = humanTaskReferentieTabel.getId();
        restHumanTaskReferentieTabel.veld = humanTaskReferentieTabel.getVeld();
        restHumanTaskReferentieTabel.tabel = RESTReferentieTabelConverter.convert(humanTaskReferentieTabel.getTabel(), false);
        return restHumanTaskReferentieTabel;
    }

    private HumanTaskReferentieTabel convert(final RESTHumanTaskReferentieTabel restHumanTaskReferentieTabel) {
        HumanTaskReferentieTabel humanTaskReferentieTabel = new HumanTaskReferentieTabel();
        humanTaskReferentieTabel.setId(restHumanTaskReferentieTabel.id);
        humanTaskReferentieTabel.setVeld(restHumanTaskReferentieTabel.veld);
        // The tabel is just a reference here, so don't update it from the REST but fetch it from the database
        humanTaskReferentieTabel.setTabel(referentieTabelService.readReferentieTabel(restHumanTaskReferentieTabel.tabel.id));
        return humanTaskReferentieTabel;
    }
}
