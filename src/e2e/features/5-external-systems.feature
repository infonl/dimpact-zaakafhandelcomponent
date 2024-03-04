Feature: External Systems

  Scenario: Employee wants to create a new document for a zaak
    Given Employee "Oscar" sees the task assigned to Employee "Bob" in my task list
    And Employee "Oscar" is on the newly created zaak with status "Intake"
    And Employee "Oscar" clicks on Create Document for zaak
    And Employee "Oscar" closes the SmartDocuments tab
    Then Employee "Oscar" should not get an error
