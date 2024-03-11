package net.atos.zac.aanvraag.model;

import jakarta.json.bind.adapter.JsonbAdapter;
import net.atos.zac.aanvraag.model.generated.Betrokkene;

public class RolOmschrijvingGeneriekJsonAdapter implements JsonbAdapter<Betrokkene.RolOmschrijvingGeneriek, String> {
    @Override
    public String adaptToJson(Betrokkene.RolOmschrijvingGeneriek value) {
        return value.name();
    }

    @Override
    public Betrokkene.RolOmschrijvingGeneriek adaptFromJson(String s) {
        return Betrokkene.RolOmschrijvingGeneriek.valueOf(s.toUpperCase());
    }
}
