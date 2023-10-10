Feature: Login

  Scenario: Bob wants to create a new zaak
    Given "Bob" navigates to "http://zaakafhandelcomponent-zac-dev.westeurope.cloudapp.azure.com"
    When "Bob" logs in with username "testuser1" and password "testuser1"
    When "Bob" wants to create a new zaak
    Then "Bob" sees the text: "Case |"
