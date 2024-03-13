package net.atos.zac.aanvraag.util;

import jakarta.json.bind.adapter.JsonbAdapter;

import net.atos.zac.aanvraag.model.generated.Betrokkene;

/**
 * JSON adapter for the {@link Betrokkene.RolOmschrijvingGeneriek} enum that matches on the enum's value instead
 * of the enum's name.
 */
public class RolOmschrijvingGeneriekEnumJsonAdapter implements JsonbAdapter<Betrokkene.RolOmschrijvingGeneriek, String> {
    @Override
    public String adaptToJson(Betrokkene.RolOmschrijvingGeneriek value) {
        return value.name();
    }

    @Override
    public Betrokkene.RolOmschrijvingGeneriek adaptFromJson(String s) {
        return Betrokkene.RolOmschrijvingGeneriek.fromValue(s);
    }
}
