# Bruno API Client collections

This directory contains a [Bruno API client](https://www.usebruno.com/) collection for the ZAC backend API and scripts to generate/update the collection from the OpenAPI specification.

## Create a new Bruno collection for the ZAC backend API

Run:

```sh
./update-bruno-collection.sh
```

This will generate a new ZAC API Bruno collection, copying global configuration and environments from the most recent previous collection, 
and will automatically open this new collection in the Bruno application.

In future this script may be enhanced by updating the existing Bruno collection instead of creating a new one.
