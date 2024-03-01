Feature: External Systems

  Scenario: Bob wants to create a new document for a zaak
    Given Employee "Oscar" is on the newly created zaak with status "Intake"
    And "Oscar" clicks on Create Document for zaak
    And "Oscar" closes the SmartDocuments tab
    Then "Oscar" should not get an error
