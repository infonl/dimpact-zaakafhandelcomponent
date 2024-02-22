/*
 * SPDX-FileCopyrightText: 2022 Atos, 2023-2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.client.zgw.zrc.util;

import static net.atos.client.zgw.shared.util.JsonbUtil.JSONB;
import static net.atos.client.zgw.zrc.model.Rol.BETROKKENE_TYPE_NAAM;

import java.lang.reflect.Type;

import jakarta.json.JsonObject;
import jakarta.json.bind.serializer.DeserializationContext;
import jakarta.json.bind.serializer.JsonbDeserializer;
import jakarta.json.stream.JsonParser;

import net.atos.client.zgw.zrc.model.BetrokkeneType;
import net.atos.client.zgw.zrc.model.Rol;
import net.atos.client.zgw.zrc.model.RolMedewerker;
import net.atos.client.zgw.zrc.model.RolNatuurlijkPersoon;
import net.atos.client.zgw.zrc.model.RolNietNatuurlijkPersoon;
import net.atos.client.zgw.zrc.model.RolOrganisatorischeEenheid;
import net.atos.client.zgw.zrc.model.RolVestiging;

public class RolJsonbDeserializer implements JsonbDeserializer<Rol<?>> {

    @Override
    public Rol deserialize(
            final JsonParser parser, final DeserializationContext ctx, final Type rtType) {
        final JsonObject jsonObject = parser.getObject();
        final BetrokkeneType betrokkenetype =
                BetrokkeneType.fromValue(
                        jsonObject.getJsonString(BETROKKENE_TYPE_NAAM).getString());

        switch (betrokkenetype) {
            case VESTIGING:
                return JSONB.fromJson(jsonObject.toString(), RolVestiging.class);
            case MEDEWERKER:
                return JSONB.fromJson(jsonObject.toString(), RolMedewerker.class);
            case NATUURLIJK_PERSOON:
                return JSONB.fromJson(jsonObject.toString(), RolNatuurlijkPersoon.class);
            case NIET_NATUURLIJK_PERSOON:
                return JSONB.fromJson(jsonObject.toString(), RolNietNatuurlijkPersoon.class);
            case ORGANISATORISCHE_EENHEID:
                return JSONB.fromJson(jsonObject.toString(), RolOrganisatorischeEenheid.class);
            default:
                throw new RuntimeException(
                        String.format(
                                "BetrokkeneType '%s' wordt niet ondersteund", betrokkenetype));
        }
    }
}
