package net.atos.zac.aanvraag.util;

import jakarta.json.bind.adapter.JsonbAdapter;

import net.atos.zac.aanvraag.model.generated.Geometry;

/**
 * JSON adapter for the {@link net.atos.zac.aanvraag.model.generated.Geometry.Type} enum that matches on the enum's value instead
 * of the enum's name.
 */
public class GeometryTypeEnumJsonAdapter implements JsonbAdapter<Geometry.Type, String> {
    @Override
    public String adaptToJson(Geometry.Type value) {
        return value.name();
    }

    @Override
    public Geometry.Type adaptFromJson(String s) {
        return Geometry.Type.fromValue(s);
    }
}
