Feature: External Systems

  Scenario: Employee wants to create a new document for a zaak
    Given "Bob" is logged in to zac
    And Employee "Bob" is on the newly created zaak with status "Intake"
    When Employee "Bob" clicks on Create Document for zaak
    And Employee "Bob" closes the SmartDocuments tab
    Then Employee "Bob" should not get an error
