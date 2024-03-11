package net.atos.zac.aanvraag.model;

import jakarta.json.bind.adapter.JsonbAdapter;
import net.atos.zac.aanvraag.model.generated.Betrokkene;

public class IndicatieMachtigingJsonAdapter implements JsonbAdapter<Betrokkene.IndicatieMachtiging, String> {
    @Override
    public String adaptToJson(Betrokkene.IndicatieMachtiging value) {
        return value.name();
    }

    @Override
    public Betrokkene.IndicatieMachtiging adaptFromJson(String s) {
        return Betrokkene.IndicatieMachtiging.valueOf(s.toUpperCase());
    }
}
