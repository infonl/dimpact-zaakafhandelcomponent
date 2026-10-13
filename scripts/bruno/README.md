# Bruno API Client collections

This directory contains the skeleton for a [Bruno API client](https://www.usebruno.com/) collection for the ZAC backend API and scripts to create the collection from the OpenAPI specification.

## Create the Bruno collection for the ZAC backend API

Run:

```sh
./create-bruno-collection.sh
```

This will (re)generate the Bruno endpoint stubs (`.bru` files) under the most recently generated collection in `scripts/bruno/collections` (typically `zaakafhandelcomponent_backend_api/rest`), and will automatically open the collection in the Bruno application.

Be careful never to commit any secret variables in the environment files, except for the 'ZAC localhost' environment.

## Use the ZAC Bruno collection

To use the Bruno collection, you will need to have the Bruno application installed on your machine. 
See: [Bruno website](https://www.usebruno.com/).

Then:
1. Open the Bruno application.
2. Click on "Open collection" and select `scripts/bruno/collections/zaakafhandelcomponent_backend_api` (or the most recently generated collection).
3. Select the environment you wish to connect to in the environment dropdown at the top right. See [Environments](#environments).
4. Select the collection and open the `Auth` tab.
5. At the bottom click on `Get Access Token`.
6. You should now be directed to the ZAC login page. After logging in you should be redirected back to Bruno and see that the access token has been updated in the environment.
7. Select the API endpoint you wish to test, optionally add/change the contents and send the request to the ZAC API.

### Environments

The collection contains its environments in `collections/zaakafhandelcomponent_backend_api/environments`, so you do not need to create one yourself.
`create-bruno-collection.sh` keeps these environments when it regenerates the collection.

| Environment | Use it for | Setup needed |
|---|---|---|
| `ZAC Localhost` | ZAC running locally, with Keycloak from the Docker Compose stack | None |
| `ZAC INFO TEST` | The INFO TEST environment | Set `keycloakClientSecret` |

Each environment defines these variables, which the OAuth2 configuration in `collection.bru` uses:

| Variable | Description |
|---|---|
| `baseUrl` | The ZAC base URL, also used as the OAuth2 callback URL |
| `keycloakBaseUrl` | The Keycloak base URL; the realm is always `zaakafhandelcomponent` |
| `keycloakClientSecret` | The client secret of the `zaakafhandelcomponent` Keycloak client |

To set `keycloakClientSecret` for `ZAC INFO TEST`:
1. Look up the client secret of the `zaakafhandelcomponent` client in Keycloak on the INFO TEST environment.
2. In Bruno, open the environment settings, select `ZAC INFO TEST` and enter the client secret as the value of `keycloakClientSecret`.

The environment file declares `keycloakClientSecret` as a secret variable, so Bruno stores its value encrypted on your machine and not in the environment file.
Keep the `Secret` checkbox of the variable ticked, so that the client secret never ends up in Git.

To connect to another environment, add an environment in Bruno with the same three variables.
