[Architecture Logical](../attachments/images/209072433-90115f29-8ac7-4461-bde5-c3d60f786c91.jpg)

Before filling out the starting form, the citizen logs in via DigiD. After completing the form in "Open formulieren", the following happens:

1. The content of the form is saved in a structured format (JSON) as _Product Request_ in Miscellaneous Registrations.
2. The BSN obtained via DigiD from the citizen is stored in the Product Request.
3. The completed form is saved as a PDF document in Open Case.

Creating the Product Request in Miscellaneous Registrations ensures that a notification is sent to Open Notifications. The Case Handling Component has a subscription to notifications, which means that Open Notifications forwards the notification to the Case Handling Component. After receiving the notification of creating a new Product Request in Miscellaneous Registrations, the following happens in the Case Handling Component:

1. The Product Request is retrieved from Miscellaneous Registrations.
2. Based on the type of Product Request, the Case Type is determined, and a Case is created.
3. The Product Request is linked to the Case.
4. The existing PDF document of the completed form is linked to the Case.
5. The BSN or Chamber of Commerce number from the Product Request is used to link a Role of the type Applicant to the Case. The BSN or establishment number is stored with the Role.
6. A CMMN Case is started for the case. The started CMMN Case is derived from the case type and can be configured in the Case Handling Component using case handling parameters.

# Identity and Policy

[Identity and Policy](../attachments/images/181199480-049a5fcc-5507-4299-9269-136d6659477f.jpg)

# "Open formulieren" connected to Case Handling Component

["Open formulieren" linked to case handling component](../attachments/images/189580091-59b45596-8025-416e-b785-632c564671a2.png)

# "Open formulieren" connected to e-Suite

["Open formulieren" linked to e-Suite](../attachments/images/189580171-1a1b3ce1-b5f9-49ad-8725-4566d6088712.png)
