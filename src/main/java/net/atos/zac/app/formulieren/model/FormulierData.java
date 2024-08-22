package net.atos.zac.app.formulieren.model;

import net.atos.zac.util.LocalDateAdapter;
import org.apache.commons.lang3.BooleanUtils;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

import static org.apache.commons.lang3.StringUtils.isNotBlank;

public class FormulierData {

    private static final String ZAAK_OPSCHORTEN = "zaak-opschorten";

    private static final String ZAAK_HERVATTEN = "zaak-hervatten";

    private static final String TAAK_FATALE_DATUM = "taak-fatale-datum";

    private static final String TAAK_TOEKENNEN_GROEP = "taak-toekennen-groep";

    private static final String TAAK_TOEKENNEN_MEDEWERKER = "taak-toekennen-behandelaar";

    private static final String DOCUMENTEN_VERZENDEN = "documenten-verzenden";

    private static final String DOCUMENTEN_VERZENDEN_DATUM = "documenten-verzenden-datum";

    private static final String DOCUMENTEN_ONDERTEKENEN = "documenten-onderteken";

    private static final String MAIL_BIJLAGEN = "mail-bijlagen";

    private static final String TOELICHTING = "toelichting";

    public boolean zaakOpschorten;

    public boolean zaakHervatten;

    public LocalDate taakFataleDatum;

    public String taakToekennenGroep;

    public String taakToekennenMedewerker;

    public String toelichting;

    public String mailBijlagen;

    public LocalDate documentenVerzendenDatum;

    public String documentenVerzenden;

    public String documentenOndertekenen;

    public final Map<String, Object> zaakVariabelen;

    public FormulierData(final Map<String, Object> taakData) {
        this.zaakVariabelen = new HashMap<>(taakData);
        if (taakData.containsKey(ZAAK_OPSCHORTEN)) {
            if (BooleanUtils.TRUE.equals(taakData.get(ZAAK_OPSCHORTEN))) {
                this.zaakOpschorten = true;
            }
            zaakVariabelen.remove(ZAAK_OPSCHORTEN);
        }
        if (taakData.containsKey(ZAAK_HERVATTEN)) {
            if (BooleanUtils.TRUE.equals(taakData.get(ZAAK_HERVATTEN))) {
                this.zaakHervatten = true;
            }
            zaakVariabelen.remove(ZAAK_HERVATTEN);
        }
        if (taakData.containsKey(TAAK_FATALE_DATUM)) {
            this.taakFataleDatum = new LocalDateAdapter().adaptFromJson(taakData.get(TAAK_FATALE_DATUM).toString());
            zaakVariabelen.remove(TAAK_FATALE_DATUM);
        }
        if (taakData.containsKey(TOELICHTING)) {
            this.toelichting = taakData.get(TOELICHTING).toString();
            zaakVariabelen.remove(TOELICHTING);
        }
        if (taakData.containsKey(TAAK_TOEKENNEN_GROEP)) {
            this.taakToekennenGroep = taakData.get(TAAK_TOEKENNEN_GROEP).toString();
            zaakVariabelen.remove(TAAK_TOEKENNEN_GROEP);
        }
        if (taakData.containsKey(TAAK_TOEKENNEN_MEDEWERKER)) {
            this.taakToekennenMedewerker = taakData.get(TAAK_TOEKENNEN_MEDEWERKER).toString();
            zaakVariabelen.remove(TAAK_TOEKENNEN_MEDEWERKER);
        }
        if (taakData.containsKey(MAIL_BIJLAGEN)) {
            if (isNotBlank(taakData.get(MAIL_BIJLAGEN).toString())) {
                this.mailBijlagen = taakData.get(MAIL_BIJLAGEN).toString();
            }
            zaakVariabelen.remove(MAIL_BIJLAGEN);
        }
        if (taakData.containsKey(DOCUMENTEN_VERZENDEN)) {
            if (isNotBlank(taakData.get(DOCUMENTEN_VERZENDEN).toString())) {
                this.documentenVerzenden = taakData.get(DOCUMENTEN_VERZENDEN).toString();
            }
            zaakVariabelen.remove(DOCUMENTEN_VERZENDEN);
        }
        if (taakData.containsKey(DOCUMENTEN_VERZENDEN_DATUM)) {
            this.documentenVerzendenDatum = new LocalDateAdapter().adaptFromJson(taakData.get(DOCUMENTEN_VERZENDEN_DATUM).toString());
            zaakVariabelen.remove(DOCUMENTEN_VERZENDEN_DATUM);
        } else {
            this.documentenVerzendenDatum = LocalDate.now();
        }
        if (taakData.containsKey(DOCUMENTEN_ONDERTEKENEN)) {
            if (isNotBlank(taakData.get(DOCUMENTEN_ONDERTEKENEN).toString())) {
                this.documentenOndertekenen = taakData.get(DOCUMENTEN_ONDERTEKENEN).toString();
            }
            zaakVariabelen.remove(DOCUMENTEN_ONDERTEKENEN);
        }
    }
}
