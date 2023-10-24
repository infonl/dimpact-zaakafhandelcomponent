Feature: Login

  Scenario: Bob wants to create a new zaak
    Given "Bob" navigates to "zac" with path "/"
    When "Bob" logs in with username "testuser1" and password "testuser1"
    When "Bob" wants to create a new zaak
    Then "Bob" sees the text: "Case |"
