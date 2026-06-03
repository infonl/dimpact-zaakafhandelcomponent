# BRP protocollering configuration

ZAC supports BRP (Basisregistratie Personen) protocollering by injecting provider-specific HTTP headers on every BRP API request. Each header name is individually configurable via environment variables, which allows the same ZAC deployment to work with any proxy provider without code changes.

This guide covers configuration for the three supported providers:

- [iConnect (PinkRoccade)](#iconnect-pinkroccade)
- [2Secure](#2secure)
- [eServices](#eservices)

---

## How it works

ZAC builds a set of outgoing BRP request headers from the environment variables listed below. Before each BRP API call:

1. The current user's ID is read from the session and placed in the configured gebruiker header.
2. The doelbinding and verwerkingregister values are resolved — either from the zaaktype-level configuration in the admin UI (when `BRP_DOELBINDING_PER_ZAAKTYPE=true`) or from the fallback environment variable values.
3. All configured headers are forwarded to the BRP proxy by `BrpClientHeadersFactory`.

Setting a header name environment variable to an empty string disables that header entirely.

---

## Environment variables

| Variable | Required | Description |
|---|---|---|
| `BRP_PROTOCOLLERING_ENABLED` | Yes | Set to `true` to enable header injection. Default: `false` |
| `BRP_DOELBINDING_PER_ZAAKTYPE` | Yes | Set to `true` to require doelbinding values per zaaktype in the admin UI. Default: `false` |
| `BRP_ORIGIN_OIN` | When origin OIN header is enabled | OIN of the requesting organisation |
| `BRP_ORIGIN_OIN_HEADER` | No | Header name for the origin OIN. Empty string disables the header |
| `BRP_DOELBINDING_HEADER` | No | Header name for the doelbinding value. Empty string disables the header |
| `BRP_DOELBINDING_ZOEKMET` | When doelbinding header is enabled | Default doelbinding for search requests |
| `BRP_DOELBINDING_RAADPLEEGMET` | When doelbinding header is enabled | Default doelbinding for retrieval requests |
| `BRP_VERWERKING_HEADER` | No | Header name for the verwerkingregister value. Empty string disables the header |
| `BRP_VERWERKINGSREGISTER` | When verwerking header is enabled | Default verwerkingregister value |
| `BRP_GEBRUIKER_HEADER` | No | Header name for the gebruiker (user) value. Empty string disables the header |
| `BRP_TOEPASSING_HEADER` | No | Header name for the toepassing value. Empty string disables the header |
| `BRP_TOEPASSING` | When toepassing header is enabled | Value to send in the toepassing header, e.g. `ZAC` |
| `BRP_SYSTEM_USER` | No | Fallback user identifier when no logged-in user is available. Default: `SystemUser` |
| `BRP_API_KEY_HEADER` | No | Header name for the API key. Defaults to `X-API-KEY`. Empty string disables the header |
| `BRP_API_KEY` | When API key header is enabled | API key value for BRP authentication. Only injected as a secret when set |
| `BRP_LOG_LEVEL` | No | Java log level for BRP request/response logging (`OFF`, `INFO`, `FINE`, etc.). Default: `OFF` |

---

## iConnect (PinkRoccade)

The iConnect proxy requires per-zaaktype doelbinding values. These are configured per zaaktype in the ZAC admin UI under the zaaktype parameters, using reference table values `BRP_DOELBINDING_RAADPLEEG_WAARDE`, `BRP_DOELBINDING_ZOEK_WAARDE`, and `BRP_VERWERKINGSREGISTER_WAARDE`.

### Environment variables

```bash
BRP_PROTOCOLLERING_ENABLED=true
BRP_DOELBINDING_PER_ZAAKTYPE=true

# Origin OIN of your organisation
BRP_ORIGIN_OIN=<your-oin>
BRP_ORIGIN_OIN_HEADER=x-origin-oin

# Doelbinding — fallback values used when no zaaktype-level value is configured
BRP_DOELBINDING_HEADER=x-doelbinding
BRP_DOELBINDING_ZOEKMET=BRPACT-ZoekenAlgemeen
BRP_DOELBINDING_RAADPLEEGMET=BRPACT-Totaal

# Verwerkingregister — fallback value used when no zaaktype-level value is configured
BRP_VERWERKING_HEADER=x-verwerking
BRP_VERWERKINGSREGISTER=<verwerkingsregister>

# Logged-in user header
BRP_GEBRUIKER_HEADER=x-gebruiker
BRP_SYSTEM_USER=SystemUser

# Application name header
BRP_TOEPASSING_HEADER=x-toepassing
BRP_TOEPASSING=ZAC

# API key
BRP_API_KEY_HEADER=x-api-key
BRP_API_KEY=<your-api-key>

BRP_LOG_LEVEL=OFF
```

### Helm values

```yaml
brpApi:
  url: "https://<brp-proxy-url>"
  apiKey:
    header: "x-api-key"
    value: "<your-api-key>"
  logLevel: "OFF"
  protocollering:
    enabled: true
    systemUser: "SystemUser"
    originOin:
      oin: "<your-oin>"
      header: "x-origin-oin"
    doelbinding:
      perZaaktype: true
      header: "x-doelbinding"
      zoekmet: "BRPACT-ZoekenAlgemeen"
      raadpleegmet: "BRPACT-Totaal"
    verwerking:
      header: "x-verwerking"
      register: "<verwerkingsregister>"
    gebruiker:
      header: "x-gebruiker"
    toepassing:
      header: "x-toepassing"
      value: "ZAC"
```

### Admin UI

With `BRP_DOELBINDING_PER_ZAAKTYPE=true`, the zaaktype configuration screen in the ZAC admin UI shows dropdowns for **Zoekwaarde**, **Raadpleegwaarde**, and **Verwerkingregister**. Populate these reference tables first:

- `BRP_DOELBINDING_RAADPLEEG_WAARDE`
- `BRP_DOELBINDING_ZOEK_WAARDE`
- `BRP_VERWERKINGSREGISTER_WAARDE`

---

## 2Secure

The 2Secure proxy uses a single doelbinding value for all requests — per-zaaktype configuration is not supported. The zaaktype dropdowns in the ZAC admin UI are disabled when `BRP_DOELBINDING_PER_ZAAKTYPE=false`.

### Environment variables

```bash
BRP_PROTOCOLLERING_ENABLED=true
BRP_DOELBINDING_PER_ZAAKTYPE=false

# Origin OIN of your organisation
BRP_ORIGIN_OIN=<your-oin>
BRP_ORIGIN_OIN_HEADER=x-origin-oin

# Single doelbinding value used for all requests
BRP_DOELBINDING_HEADER=x-doelbinding
BRP_DOELBINDING_ZOEKMET=BRPACT-ZoekenAlgemeen
BRP_DOELBINDING_RAADPLEEGMET=BRPACT-Totaal

# Verwerkingregister
BRP_VERWERKING_HEADER=x-verwerking
BRP_VERWERKINGSREGISTER=<verwerkingsregister>

# Logged-in user header
BRP_GEBRUIKER_HEADER=x-gebruiker
BRP_SYSTEM_USER=SystemUser

# Application name header
BRP_TOEPASSING_HEADER=x-toepassing
BRP_TOEPASSING=ZAC

# API key
BRP_API_KEY_HEADER=x-api-key
BRP_API_KEY=<your-api-key>

BRP_LOG_LEVEL=OFF
```

### Helm values

```yaml
brpApi:
  url: "https://<brp-proxy-url>"
  apiKey:
    header: "x-api-key"
    value: "<your-api-key>"
  logLevel: "OFF"
  protocollering:
    enabled: true
    systemUser: "SystemUser"
    originOin:
      oin: "<your-oin>"
      header: "x-origin-oin"
    doelbinding:
      perZaaktype: false
      header: "x-doelbinding"
      zoekmet: "BRPACT-ZoekenAlgemeen"
      raadpleegmet: "BRPACT-Totaal"
    verwerking:
      header: "x-verwerking"
      register: "<verwerkingsregister>"
    gebruiker:
      header: "x-gebruiker"
    toepassing:
      header: "x-toepassing"
      value: "ZAC"
```

---

## eServices

The eServices proxy does not use a separate doelbinding header. The application name (`x-request-application`) is used for both the verwerking and toepassing headers; the toepassing value (`ZAC`) is what ultimately reaches the proxy on that header.

### Environment variables

```bash
BRP_PROTOCOLLERING_ENABLED=true
BRP_DOELBINDING_PER_ZAAKTYPE=false

# Origin OIN of your organisation
BRP_ORIGIN_OIN=<your-oin>
BRP_ORIGIN_OIN_HEADER=x-request-organization

# Doelbinding disabled for eServices
BRP_DOELBINDING_HEADER=

# Verwerkingregister — sent on x-request-application
BRP_VERWERKING_HEADER=x-request-application
BRP_VERWERKINGSREGISTER=<verwerkingsregister>

# Logged-in user header
BRP_GEBRUIKER_HEADER=x-request-user

# Application name header (same header as verwerking — toepassing value takes effect)
BRP_TOEPASSING_HEADER=x-request-application
BRP_TOEPASSING=ZAC

# API key
BRP_API_KEY_HEADER=x-api-key
BRP_API_KEY=<your-api-key>

BRP_LOG_LEVEL=OFF
```

### Helm values

```yaml
brpApi:
  url: "https://<brp-proxy-url>"
  apiKey:
    header: "x-api-key"
    value: "<your-api-key>"
  logLevel: "OFF"
  protocollering:
    enabled: true
    originOin:
      oin: "<your-oin>"
      header: "x-request-organization"
    doelbinding:
      perZaaktype: false
      header: ""
    verwerking:
      header: "x-request-application"
      register: "<verwerkingsregister>"
    gebruiker:
      header: "x-request-user"
    toepassing:
      header: "x-request-application"
      value: "ZAC"
```

---

## Disabling protocollering

To disable BRP protocollering completely:

```bash
BRP_PROTOCOLLERING_ENABLED=false
```

All header injection is bypassed. BRP API calls are still made but without any protocollering headers.

---

## Enabling request/response logging

Set `BRP_LOG_LEVEL` to a Java log level to log BRP request and response payloads. This is useful for debugging but is not recommended for production environments.

```bash
BRP_LOG_LEVEL=INFO   # logs to the INFO level
BRP_LOG_LEVEL=FINE   # logs to the FINE (debug) level
BRP_LOG_LEVEL=OFF    # disables logging (default)
```
