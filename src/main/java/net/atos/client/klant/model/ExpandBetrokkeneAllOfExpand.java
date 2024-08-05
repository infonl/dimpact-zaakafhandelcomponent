package net.atos.client.klant.model;

import jakarta.json.bind.annotation.JsonbProperty;

public class ExpandBetrokkeneAllOfExpand {

    @JsonbProperty("hadKlantcontact")
    private Klantcontact hadKlantcontact;

    public Klantcontact getHadKlantcontact() {
        return hadKlantcontact;
    }

    public void setHadKlantcontact(Klantcontact hadKlantcontact) {
        this.hadKlantcontact = hadKlantcontact;
    }
}
