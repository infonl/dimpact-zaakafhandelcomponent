/*
 * SPDX-FileCopyrightText: 2023 Atos
 * SPDX-License-Identifier: EUPL-1.2+
 */

package net.atos.zac.app.admin.converter;

import java.util.List;
import java.util.stream.Collectors;

import net.atos.zac.app.admin.model.RESTReplyTo;
import nl.info.zac.admin.model.ReferenceTableValue;
import nl.info.zac.admin.model.ZaaktypeCmmnZaakafzenderParameters;

public final class RESTReplyToConverter {
    // Private constructor to prevent instantiation
    private RESTReplyToConverter() {
    }

    public static List<RESTReplyTo> convertReplyTos(final List<ReferenceTableValue> waarden) {
        final List<RESTReplyTo> restReplyTos = waarden.stream()
                .map(RESTReplyToConverter::convertReplyTo)
                .collect(Collectors.toList());
        for (final ZaaktypeCmmnZaakafzenderParameters.SpecialMail specialMail : ZaaktypeCmmnZaakafzenderParameters.SpecialMail.values()) {
            restReplyTos.add(new RESTReplyTo(specialMail));
        }
        restReplyTos.sort((a, b) -> a.speciaal != b.speciaal ? a.speciaal ? -1 : 1 : a.mail.compareTo(b.mail));
        return restReplyTos;
    }

    public static RESTReplyTo convertReplyTo(final ReferenceTableValue waarde) {
        final RESTReplyTo restReplyTo = new RESTReplyTo();
        restReplyTo.mail = waarde.name;
        restReplyTo.speciaal = false;
        return restReplyTo;
    }
}
