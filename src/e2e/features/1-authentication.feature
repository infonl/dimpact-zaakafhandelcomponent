Feature: Login

  Scenario: Bob wants to login to ZAC
    Given "Bob" navigates to "zac" with path "/"
    When "Bob" logs in with username "testuser1" and password "testuser1"
    Then "Bob" sees the text: "Dashboard1"
