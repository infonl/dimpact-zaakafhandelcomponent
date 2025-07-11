/*
 * SPDX-FileCopyrightText: 2022 Atos, 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.zac.app.admin.converter;

import java.util.List;
import java.util.Set;

import net.atos.zac.admin.model.MailtemplateKoppeling;
import net.atos.zac.app.admin.model.RESTMailtemplateKoppeling;

public final class RESTMailtemplateKoppelingConverter {

    public static RESTMailtemplateKoppeling convert(final MailtemplateKoppeling mailtemplateKoppeling) {
        final RESTMailtemplateKoppeling restMailtemplateKoppeling = new RESTMailtemplateKoppeling();
        restMailtemplateKoppeling.id = mailtemplateKoppeling.getId();
        restMailtemplateKoppeling.mailtemplate = RESTMailtemplateConverter.convert(mailtemplateKoppeling.getMailTemplate());

        return restMailtemplateKoppeling;
    }

    public static MailtemplateKoppeling convert(final RESTMailtemplateKoppeling restMailtemplateKoppeling) {
        final MailtemplateKoppeling mailtemplateKoppeling = new MailtemplateKoppeling();
        mailtemplateKoppeling.setMailTemplate(
                RESTMailtemplateConverter.convert(restMailtemplateKoppeling.mailtemplate)
        );
        return mailtemplateKoppeling;
    }

    public static List<RESTMailtemplateKoppeling> convert(final Set<MailtemplateKoppeling> mailtemplateKoppelingen) {
        return mailtemplateKoppelingen.stream().map(RESTMailtemplateKoppelingConverter::convert).toList();
    }

    public static List<MailtemplateKoppeling> convertRESTmailtemplateKoppelingen(
            final List<RESTMailtemplateKoppeling> restMailtemplateKoppelingen
    ) {
        return restMailtemplateKoppelingen.stream().map(RESTMailtemplateKoppelingConverter::convert).toList();
    }
}
