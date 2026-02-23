# Bruno API Client collections

This directory contains a [Bruno API client](https://www.usebruno.com/) collection for the ZAC backend API and scripts to generate/update the collection from the OpenAPI specification.

## Use the Bruno collection

To use the Bruno collection, you will need to have the Bruno application installed on your machine. 
See: [Bruno website](https://www.usebruno.com/).

Then:
1. Open the Bruno application.
2. Click on "Open collection" and select the (most recent) ZAC Bruno collection subfolder in the `/scripts/bruno/collections` folder.
3. Select the environment you wish to connect to. If you want to connect to the INFO TEST environment, you will need to set the `keycloakClientSecret` variable first.
4. Select the collection and open the `Auth` tab.
5. At the bottom click on `Get Access Token`.
6. You should now be directed to the ZAC login page. After logging in you should be redirected back to Bruno and see that the access token has been updated in the environment.
7. Select the API endpoint you wish to test, optionally add/change the contents and send the request to the ZAC API.

## Create a new Bruno collection for the ZAC backend API

Run:

```sh
./update-bruno-collection.sh
```

This will generate a new ZAC API Bruno collection, copying global configuration and environments from the most recent previous collection, 
and will automatically open this new collection in the Bruno application.

You can now add the new collection folder to Git and delete the previous collection folder.

## Ideas for future enhancements

In future the update collection script may be enhanced by updating the existing Bruno collection instead of creating a new one.
