Feature: External Systems

  Scenario: Bob wants to create a new document for a zaak
    Given Employee "Bob" is on the newly created zaak with status "Intake"
    When "Bob" clicks on Create Document for zaak with a delay
    And "Bob" closes the SmartDocuments tab
    Then "Bob" should not get an error
