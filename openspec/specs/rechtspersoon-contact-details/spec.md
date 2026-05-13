### Requirement: Contact details enrichment for rechtspersoon by KVK number
When a rechtspersoon is retrieved by KVK number, the system SHALL concurrently fetch digital address data from Open Klant and include the email address and phone number in the response.

#### Scenario: Rechtspersoon found with contact details available
- **WHEN** `GET /klanten/rechtspersoon/kvknummer/{kvkNummer}` is called and the KVK API returns a rechtspersoon and Open Klant returns digital addresses with email and phone
- **THEN** the response SHALL include `emailadres` and `telefoonnummer` populated from the Open Klant data

#### Scenario: Rechtspersoon found but no contact details in Open Klant
- **WHEN** `GET /klanten/rechtspersoon/kvknummer/{kvkNummer}` is called and the KVK API returns a rechtspersoon but Open Klant returns an empty list of digital addresses
- **THEN** the response SHALL be returned with `emailadres` and `telefoonnummer` set to null

#### Scenario: Rechtspersoon not found
- **WHEN** `GET /klanten/rechtspersoon/kvknummer/{kvkNummer}` is called and the KVK API returns no rechtspersoon
- **THEN** the system SHALL throw `RechtspersoonNotFoundException` and return HTTP 404

### Requirement: Concurrent retrieval of KVK and Open Klant data
The system SHALL fetch rechtspersoon data from the KVK API and digital addresses from Open Klant concurrently, not sequentially, to minimise latency.

#### Scenario: Both calls execute in parallel
- **WHEN** `GET /klanten/rechtspersoon/kvknummer/{kvkNummer}` is called
- **THEN** the KVK lookup and the Open Klant digital-address lookup SHALL be initiated simultaneously within the same `Dispatchers.IO` coroutine scope
