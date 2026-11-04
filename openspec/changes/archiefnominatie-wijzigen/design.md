## Context

Open Zaak sets `archiefnominatie`, `archiefactiedatum` and `startdatumBewaartermijn` of a zaak when the zaak is
closed, from its resultaattype. ZAC only reads them. ZAC regards a zaak as afgehandeld when it has an
archiefnominatie (`Zaak.isOpen()`).

## Goals / Non-Goals

**Goals:** every requirement in `specs/zaak-archiefnominatie-wijzigen/spec.md`.

**Non-Goals:** recalculating or editing the archiefactiedatum; choosing a selectielijst(item); the relation with
vernietigingslijsten in Open Archiefbeheer; the action menu and pop-up (waiting for the design).

## Spike findings: what Open Zaak does

Checked against the Open Zaak source of version 1.31.1, the version ZAC runs, and the VNG Zaken API standard.

1. **A change of archiefnominatie triggers nothing.** `archiefnominatie` is a plain writable field of the zaak
   ([`ZaakSerializer`](https://github.com/open-zaak/open-zaak/blob/1.31.1/src/openzaak/components/zaken/api/serializers/zaken.py#L511)).
   The zaak `save()` only recalculates on a changed `einddatum`
   ([`models/zaken.py`](https://github.com/open-zaak/open-zaak/blob/1.31.1/src/openzaak/components/zaken/models/zaken.py#L570)).
   The `archiefactiedatum` stays what it was, so a zaak switched from *Blijvend bewaren* to *Vernietigen* can be
   without a vernietigingsdatum.
2. **Open Zaak recalculates the archiefgegevens on other triggers**
   ([`try_calculate_archiving`](https://github.com/open-zaak/open-zaak/blob/1.31.1/src/openzaak/components/zaken/archiving.py#L46-L54)):
   setting the eindstatus, a changed einddatum, adding or deleting a resultaat, and changing the source of the
   brondatum (zaakeigenschap, zaakobject, besluit), for the matching afleidingswijze. ZAC's own *brondatum zetten*
   is such a trigger.
3. **A recalculation resets the archiefnominatie to that of the resultaattype**
   ([`archiving.py` L36-42](https://github.com/open-zaak/open-zaak/blob/1.31.1/src/openzaak/components/zaken/archiving.py#L36-L42)).
   A changed archiefnominatie can therefore silently be undone. The standard only asks to copy it when the zaak
   has none
   ([ZRC 1.8.0, *Archiveren*](https://github.com/VNG-Realisatie/gemma-zaken/blob/2639b45e4a62c2eaab0ec059cd38478827c7729a/docs/standaard/zaken/zrc/1.8.x/1.8.0/specification.md#L188-L216)).
4. **Changing an afgehandelde zaak needs the `zaken.geforceerd-bijwerken` scope**
   ([`ZaakViewSet.perform_update`](https://github.com/open-zaak/open-zaak/blob/1.31.1/src/openzaak/components/zaken/api/viewsets.py#L455-L478)).
5. **Once the archiefstatus is no longer `nog_te_archiveren`**, Open Zaak requires an archiefnominatie and an
   archiefactiedatum
   ([`validators.py`](https://github.com/open-zaak/open-zaak/blob/1.31.1/src/openzaak/components/zaken/api/validators.py#L233-L244)).

## Decisions

### ZAC changes the archiefnominatie only

As agreed with the PO for this spike, ZAC sends only `archiefnominatie` and leaves the archiefactiedatum to Open
Zaak. The findings above go to the story as recommendations for a follow-up.

### A separate right, not `wijzigen`

The story asks for a right `wijzigen_archief` for recordmanager and beheerder. It follows `heropenen`: the zaak is
afgehandeld, the zaaktype is allowed and the zaak is accessible (`zaak_allowed`).

### A separate endpoint, like *brondatum zetten*

`PUT /rest/zaken/zaak/{uuid}/archiefnominatie` in a new `ZaakArchiefRestService`, next to
`ZaakBrondatumRestService`, so the pop-up can grow with more archive actions. The policy is checked on the zaak as
it was; the response is the zaak as Open Zaak returns it after the patch, with its rechten evaluated on that zaak,
as every zaak mutation does.

### The reden is the audit toelichting

The reden goes into Open Zaak's audit trail via `ZrcClientService.patchZaak(..., explanation)`, as for the other
zaak updates, so the zaakhistorie shows it.

## Risks / Trade-offs

- A changed archiefnominatie can be reset by Open Zaak (finding 3) → documented on the story; a follow-up decides
  whether ZAC or Open Zaak should handle it.
- A *Vernietigen* zaak without archiefactiedatum (finding 1) → same.
