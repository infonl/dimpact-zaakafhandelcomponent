# Open Zaak docker configuration

This directory contains supporting configuration and infrastructure to run Open Zaak
via `docker compose`.

You need Docker Engine v20.10 or newer for the documented setup to work.

## Spinning up the services

In the root of the project, spin up Open Zaak via `compose`:

```bash
docker compose up --detach openzaak-web  # or just `docker compose up` to bring everything up
```

Open Zaak binds to port 8001 on the host system.

## Accessing the admin environment

Open your browser and navigate to http://localhost:8001/admin/, where you can log in
with the credentials `admin` / `admin`.

## API client

One API client is configured, with all permissions enabled. The credentials are:

* Client ID: `woo-publications-dev`
* Secret: `insecure-yQL9Rzh4eHGVmYx5w3J2gu`

## API connection from Open Zaak to WOO Publications

Open Zaak will make certain API calls to API endpoints exposed by WOO Publications. This
requires some specific setup - most notably the Woo Publications must be listening on
port 8000 on the host system.

If you're bringing up the whole compose stack, this is taken care of already. If you run
the development server, it's easiest to bind to all ports:

```bash
src/manage.py runserver 0.0.0.0:8000
```

## Dumping the fixture

The service automatically loads the fixtures provided in the `fixtures` directory. When
making changes in the web interface to the configuration, you must update these
fixtures:

```bash
# from the root of the repository
docker compose run openzaak-web \
    python src/manage.py dumpdata \
        --indent=4 \
        --output /app/fixtures/configuration.json \
        authorizations.applicatie \
        vng_api_common.jwtsecret \
        config \
        zgw_consumers
```

Depending on your OS and local user ID, you may need to grant additional write permissions:

```bash
chmod -R o+rwx ./docker/open-zaak/fixtures
```
