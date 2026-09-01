# zaakspecifiek-geautoriseerde-zoekindex Specification

## Purpose

Keeps the search index that backs werklijsten and zoekresultaten aware of each zaak's zaakspecifiek
geautoriseerd status, and uses that to exclude zaakspecifiek geautoriseerde zaken (and their taken and
documenten) from a user's worklists and search results for any zaaktype where the user lacks the
`zaakspecifiek_geautoriseerd` application role.

## Requirements

### Requirement: The search index reflects each zaak's zaakspecifiek geautoriseerd status

The search index entry for a zaak, and for every taak and document associated with that zaak, SHALL
reflect whether the zaak is zaakspecifiek geautoriseerd (i.e. carries a `ZAAK_GEAUTORISEERD` zaakeigenschap
with value `true`), consistent with the same determination already used for single-resource rechten.

#### Scenario: A zaakspecifiek geautoriseerde zaak is indexed as such
- **WHEN** a zaak that carries a `ZAAK_GEAUTORISEERD` zaakeigenschap with value `true` is indexed
- **THEN** the zaak's search index entry, and the search index entries of its taken and documenten, are
  marked as zaakspecifiek geautoriseerd

#### Scenario: A regular zaak is not indexed as zaakspecifiek geautoriseerd
- **WHEN** a zaak that does not carry a `ZAAK_GEAUTORISEERD` zaakeigenschap with value `true` is indexed
- **THEN** the zaak's search index entry, and the search index entries of its taken and documenten, are not
  marked as zaakspecifiek geautoriseerd

### Requirement: Werklijsten and zoekresultaten exclude zaakspecifiek geautoriseerde zaken from zaaktypes the user isn't authorized for

A werklijst or zoekresultaat query SHALL exclude a zaakspecifiek geautoriseerde zaak, and its taken and
documenten, from its results for a zaaktype where the requesting user does not hold the
`zaakspecifiek_geautoriseerd` application role, even if the user holds another application role
(`raadpleger`, `behandelaar`, `coordinator`, `recordmanager`, or `beheerder`) for that zaaktype. This
applies regardless of how many application roles the user holds for the zaaktype.

#### Scenario: A user without the flag does not find a zaakspecifiek geautoriseerde zaak in a worklist
- **WHEN** a user who holds an application role for a zaaktype but not `zaakspecifiek_geautoriseerd`
  requests a werklijst that would otherwise include a zaakspecifiek geautoriseerde zaak of that zaaktype
- **THEN** that zaak is absent from the werklijst results

#### Scenario: A user without the flag does not find a task of a zaakspecifiek geautoriseerde zaak in a worklist
- **WHEN** a user who holds an application role for a zaaktype but not `zaakspecifiek_geautoriseerd`
  requests a werklijst that would otherwise include a taak belonging to a zaakspecifiek geautoriseerde zaak
  of that zaaktype
- **THEN** that taak is absent from the werklijst results

#### Scenario: A user without the flag does not find a zaakspecifiek geautoriseerde zaak in search results
- **WHEN** a user who holds an application role for a zaaktype but not `zaakspecifiek_geautoriseerd`
  performs a search that would otherwise match a zaakspecifiek geautoriseerde zaak, or a document linked
  to one, of that zaaktype
- **THEN** that zaak, respectively that document, is absent from the search results

#### Scenario: A user without the flag for one zaaktype still sees flagged zaken of another zaaktype they hold the flag for
- **WHEN** a user holds `zaakspecifiek_geautoriseerd` together with another application role for zaaktype
  A, and holds only another application role (without `zaakspecifiek_geautoriseerd`) for zaaktype B, and
  requests a werklijst or zoekresultaat that would otherwise include zaakspecifiek geautoriseerde zaken of
  both zaaktypes
- **THEN** zaaktype A's zaakspecifiek geautoriseerde zaken are present in the results and zaaktype B's are
  absent

### Requirement: Werklijsten and zoekresultaten include zaakspecifiek geautoriseerde zaken for a user who holds the flag

A werklijst or zoekresultaat query SHALL include a zaakspecifiek geautoriseerde zaak, and its taken and
documenten, for a zaaktype where the requesting user holds both the `zaakspecifiek_geautoriseerd`
application role and another application role, exactly as it would include a non-geautoriseerde zaak of
that zaaktype.

#### Scenario: A user with the flag finds a zaakspecifiek geautoriseerde zaak in a worklist
- **WHEN** a user who holds both `behandelaar` and `zaakspecifiek_geautoriseerd` for a zaaktype requests a
  werklijst that would include a zaakspecifiek geautoriseerde zaak of that zaaktype
- **THEN** that zaak is present in the werklijst results, indistinguishable in inclusion terms from a
  non-geautoriseerde zaak of the same zaaktype

#### Scenario: A user with the flag finds a zaakspecifiek geautoriseerde zaak in search results
- **WHEN** a user who holds both `behandelaar` and `zaakspecifiek_geautoriseerd` for a zaaktype performs a
  search that would match a zaakspecifiek geautoriseerde zaak of that zaaktype
- **THEN** that zaak is present in the search results
