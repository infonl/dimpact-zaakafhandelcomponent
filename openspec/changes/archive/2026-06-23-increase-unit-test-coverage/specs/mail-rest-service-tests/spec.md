## ADDED Requirements

### Requirement: MailRestService sends a mail after asserting policy
`sendMail` SHALL read the zaak by UUID, assert the `versturenEmail` policy right, convert the REST mail data, and send it via `MailService`.

#### Scenario: Send mail with authorized user
- **WHEN** `sendMail` is called with a zaak UUID and `RESTMailGegevens`
- **THEN** `ZrcClientService.readZaak` is called with the UUID
- **THEN** `PolicyService.readZaakRechten` is called and `versturenEmail` is asserted
- **THEN** `MailService.sendMail` is called with the converted mail data and zaak bronnen

### Requirement: MailRestService sends an acknowledgement receipt mail with deduplication guard
`sendAcknowledgmentReceiptMail` SHALL check whether an ontvangstbevestiging was already sent and whether the policy permits sending. If permitted, it SHALL send the mail and mark the zaak accordingly.

#### Scenario: Acknowledgement not yet sent and policy permits
- **WHEN** `sendAcknowledgmentReceiptMail` is called and `ontvangstbevestigingVerstuurd` is false and policy permits
- **THEN** `MailService.sendMail` is called
- **THEN** `ZaakService.setOntvangstbevestigingVerstuurdIfNotHeropend` is called with the zaak

#### Scenario: Policy denies sending acknowledgement
- **WHEN** `sendAcknowledgmentReceiptMail` is called but policy denies `versturenOntvangstbevestiging`
- **THEN** the policy assertion throws and `MailService.sendMail` is NOT called
