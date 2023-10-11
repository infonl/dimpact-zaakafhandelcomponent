Feature: Login

  Scenario: Bob wants to login to ZAC
    Given "Bob" navigates to "http://localhost:8080/"
    When "Bob" logs in with username "testuser1" and password "testuser1"
    Then "Bob" sees the text: "Dashboard"
