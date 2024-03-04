Feature: Login

  Scenario: Bob wants to login to ZAC
    Given "Bob" navigates to "zac" with path "/"
    When "Bob" logs in
    Then "Bob" sees the text: "Dashboard"
