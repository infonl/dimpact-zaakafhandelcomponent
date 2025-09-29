/*
 * SPDX-FileCopyrightText: 2022 Atos, 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.zac.app.admin.converter;

import java.util.List;
import java.util.Set;

import net.atos.zac.admin.model.ZaaktypeCmmnMailtemplateParameters;
import net.atos.zac.app.admin.model.RESTMailtemplateKoppeling;

public final class RESTMailtemplateKoppelingConverter {

    public static RESTMailtemplateKoppeling convert(final ZaaktypeCmmnMailtemplateParameters zaaktypeCmmnMailtemplateParameters) {
        final RESTMailtemplateKoppeling restMailtemplateKoppeling = new RESTMailtemplateKoppeling();
        restMailtemplateKoppeling.id = zaaktypeCmmnMailtemplateParameters.getId();
        restMailtemplateKoppeling.mailtemplate = RESTMailtemplateConverter.convert(zaaktypeCmmnMailtemplateParameters.getMailTemplate());

        return restMailtemplateKoppeling;
    }

    public static ZaaktypeCmmnMailtemplateParameters convert(final RESTMailtemplateKoppeling restMailtemplateKoppeling) {
        final ZaaktypeCmmnMailtemplateParameters zaaktypeCmmnMailtemplateParameters = new ZaaktypeCmmnMailtemplateParameters();
        zaaktypeCmmnMailtemplateParameters.setMailTemplate(
                RESTMailtemplateConverter.convert(restMailtemplateKoppeling.mailtemplate)
        );
        return zaaktypeCmmnMailtemplateParameters;
    }

    public static List<RESTMailtemplateKoppeling> convert(
            final Set<ZaaktypeCmmnMailtemplateParameters> zaaktypeCmmnMailtemplateKoppelingen
    ) {
        return zaaktypeCmmnMailtemplateKoppelingen.stream().map(RESTMailtemplateKoppelingConverter::convert).toList();
    }

    public static List<ZaaktypeCmmnMailtemplateParameters> convertRESTmailtemplateKoppelingen(
            final List<RESTMailtemplateKoppeling> restMailtemplateKoppelingen
    ) {
        return restMailtemplateKoppelingen.stream().map(RESTMailtemplateKoppelingConverter::convert).toList();
    }
}
