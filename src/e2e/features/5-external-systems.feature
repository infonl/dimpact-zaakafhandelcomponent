Feature: External Systems

  Scenario: Employee wants to create a new document for a zaak
    Given Employee "Oscar" is on the newly created zaak with status "Intake"
    And Employee "Oscar" logs out of zac
    And Employee "Bob" logs in to zac
    And Employee "Bob" clicks on Create Document for zaak
    And Employee "Bob" closes the SmartDocuments tab
    Then Employee "Bob" should not get an error
