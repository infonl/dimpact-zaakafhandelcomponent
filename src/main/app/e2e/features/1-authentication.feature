Feature: Login

  Background: 
    Given Zac is live

  Scenario: Bob wants to login to ZAC
    Given "Bob" navigates to "http://127.0.0.1:8080/"
    When "Bob" logs in with username "testuser1" and password "testuser1"
    Then "Bob" sees the text: "Dashboard"
