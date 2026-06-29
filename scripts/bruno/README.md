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
2. Click on "Open collection" and select the ZAC Bruno collection subfolder in the `/scripts/bruno/collections` folder.
3. Select the environment you wish to connect to. If you want to connect to the INFO TEST environment, you will need to set the `keycloakClientSecret` variable first.
You can find the value for this variable in Keycloak on the INFO TEST environment.
4. Select the collection and open the `Auth` tab.
5. At the bottom click on `Get Access Token`.
6. You should now be directed to the ZAC login page. After logging in you should be redirected back to Bruno and see that the access token has been updated in the environment.
7. Select the API endpoint you wish to test, optionally add/change the contents and send the request to the ZAC API.
