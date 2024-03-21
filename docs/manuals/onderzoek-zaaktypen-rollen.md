In eerste instantie deze zaaktypen:
![alt text](image.png)

Stappen per zaaktype:
1. Nieuwe versie toevoegen (dan moet je een datum einde geldigheid bij de huidge versie toevoegen)
2. Roltype toevoegen


Bevindingen:
- omschrijving (generiek) van roltype wordt in de db overgenomen als attributen op de rol bij een zaak
- voor de solr index pakken we het attribuut van de rol bij een zaak, dus niet van het roltype
- je mag niet meer dan 1 Roltype met omschrijving generiek `initiator` en `zaakcoordinator`

Zie [code maykin](https://github.com/open-zaak/open-zaak/blob/c52837c37912698b7f674d979c3c0617bc62e8e0/src/openzaak/components/zaken/api/serializers/zaken.py#L932)
```py
class Meta: 
    validators = [
        RolOccurenceValidator(RolOmschrijving.initiator, max_amount=1),
        RolOccurenceValidator(RolOmschrijving.zaakcoordinator, max_amount=1),
    ]
```