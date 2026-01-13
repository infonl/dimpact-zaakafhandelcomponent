# ZAC deployment model

ZAC consists of [a number of components](systemContext.md) that are packaged as Docker images and together make up the full ZAC application.
These components are deployed as Kubernetes pods using the [ZAC Helm Chart](../../charts/zac).

Besides these pods ZAC also uses the following Kubernetes cron jobs, which are also deployed using the ZAC Helm Chart:

| Cron job name | Description                                                                                                                                                                                                 | Frequency |
|---------------|-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|-----------|
| Send signaleringen | Calls the ZAC 'send signaleringen' endpoint, so that ZAC will send out 'signaleringen' for zaken that have almost reached their due date (e.g. “Graag actie, op DD-MM-YYY moet zaak ZAAK-XXX afgehandeld zijn”). | Daily |
| Delete old signaleringen | Calls the ZAC 'delete old signaleringen' endpoint, so that ZAC will delete any 'signaleringen' that are older than a certain configured amount of days.                                                     | Daily |

## Dependencies

ZAC requires an existing PostgreSQL database to store its data.
This database is not provisioned by the ZAC Helm Chart and must be created separately before deploying ZAC.

Also, ZAC has a number of external dependencies that are not part of the ZAC Helm Chart and must be provided separately.
The connection configuration for these dependencies must be provided to the ZAC Helm chart during deployment.
Please see the [ZAC system context](systemContext.md) for an overview of these dependencies.

## Cloud-agnostic deployment

ZAC is cloud-agnostic and can be deployed on any Kubernetes cluster, regardless of the cloud provider or on-premises setup.

## Scalability

ZAC currently cannot scale horizontally. This means that there can only be one instance of ZAC running on any given environment.

Should the need arise for horizontal scalability for ZAC in the future, some things that will need to be tackled in order to achieve this are:
- Integration with the `Open Notificaties` component. 
Care must be taken that a specific notification event for ZAC is only handled once within a ZAC cluster.
- Session management. ZAC uses HTTP sessions to keep track of logged-in users. 
Either sticky sessions will need to be used at the load balancer level, or session data will need to be stored in a shared session store (e.g. Redis).
