Feature: Login

  Scenario: Bob wants to login to ZAC
    Given "Bob" navigates to "http://zaakafhandelcomponent-zac-dev.westeurope.cloudapp.azure.com"
    When "Bob" logs in with username "testuser1" and password "testuser1"
    Then "Bob" sees the text: "Dashboard"
