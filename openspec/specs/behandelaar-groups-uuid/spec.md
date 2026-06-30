## REMOVED Requirements

### Requirement: Fetch behandelaar groups by zaaktype UUID
**Reason**: The UUID-based endpoint was a migration bridge from the old IAM architecture. The PABC-native endpoint using the zaaktype description (`listBehandelaarGroupsForZaaktype`) fully replaces it.
**Migration**: Use `GET /rest/identity/zaaktype/{zaaktypeDescription}/behandelaar-groups` — pass the zaaktype `omschrijving` (description string) instead of the zaaktype UUID.

#### Scenario: Client requests behandelaar groups for a zaaktype UUID
- **WHEN** a client calls `GET /rest/identity/groups/behandelaar/zaaktype/{zaaktypeUuid}`
- **THEN** the endpoint SHALL no longer exist (HTTP 404 or 405)
