# BRP Protocollering Configuration

ZAC supports BRP (_Basisregistratie Personen_) access via the Haal Centraal BRP Personen API. When a BRP API provider requires _protocollering_ (audit logging of personal data access), ZAC injects a set of HTTP headers on every BRP request. This guide describes how to configure ZAC for the two supported providers: **iConnect** and **2Secure**.

BRP protocollering is enabled by setting `brpApi.protocollering.enabled: true` in your Helm values. When this is `false` (the default), no protocollering headers are sent. All header names and values default to empty and must be supplied explicitly.

## iConnect

iConnect requires doelbinding values to be configured per zaaktype in the ZAC admin UI. Set `doelbindingPerZaaktype: true` to enable this in the admin UI.

```yaml
brpApi:
  url: "https://<iconnect-brp-api-url>"
  protocollering:
    enabled: true
    originOin:
      oin: "<your-municipality-OIN>"
      header: "x-origin-oin"
    doelbinding:
      perZaaktype: true   # enables per-zaaktype doelbinding configuration in admin UI
      header: "x-doelbinding"
      zoekmet: "BRPACT-ZoekenAlgemeen"   # used as fallback when no per-zaaktype value is configured
      raadpleegmet: "BRPACT-Totaal"       # used as fallback when no per-zaaktype value is configured
    verwerking:
      header: "x-verwerking"
      register: "Algemeen"
    toepassing:
      header: "x-toepassing"
      value: "ZAC"
    gebruiker:
      header: "x-gebruiker"
```

After deploying with this configuration, open the ZAC admin UI and configure the doelbinding values for each zaaktype that uses BRP under _Zaakafhandelparameters > BRP doelbinding_.

## 2Secure

2Secure does not require per-zaaktype doelbinding values. A single global doelbinding configuration is sufficient.

```yaml
brpApi:
  url: "https://<2secure-brp-api-url>"
  protocollering:
    enabled: true
    originOin:
      oin: "<your-municipality-OIN>"
      header: "x-origin-oin"
    doelbinding:
      perZaaktype: false
      header: "x-doelbinding"
      zoekmet: "BRPACT-ZoekenAlgemeen"
      raadpleegmet: "BRPACT-Totaal"
    verwerking:
      header: "x-verwerking"
      register: "Algemeen"
    toepassing:
      header: "x-toepassing"
      value: "ZAC"
    gebruiker:
      header: "x-gebruiker"
```

## eServices

eServices works similarly to 2Secure (no per-zaaktype doelbinding) but uses different header names. The `x-request-afnemerscode` is a municipality-supplied code that maps to the verwerking register field. Doelbinding headers are not used.

```yaml
brpApi:
  url: "https://<eservices-brp-api-url>"
  protocollering:
    enabled: true
    doelbindingPerZaaktype: false
    originOin:
      oin: "<your-municipality-OIN>"
      header: "x-request-organization"
    doelbinding:
      header: ""   # not used by eServices
    verwerking:
      header: "x-request-afnemerscode"
      register: "<your-afnemerscode>"   # municipality-supplied afnemerscode
    toepassing:
      header: "x-request-application"
      value: "ZAC"
    gebruiker:
      header: "x-request-user"
```

## Disabling individual headers

Each protocollering header can be disabled by setting its `header` value to an empty string. When a header is disabled, the corresponding value is not required. For example, to disable the `x-toepassing` header:

```yaml
brpApi:
  protocollering:
    toepassing:
      header: ""   # disables the x-toepassing header entirely
```

## All configurable header names

| Helm value | Header name used by iConnect/2Secure | Purpose |
|---|---|---|
| `protocollering.originOin.header` | `x-origin-oin` | OIN of the municipality |
| `protocollering.doelbinding.header` | `x-doelbinding` | Purpose of data access |
| `protocollering.verwerking.header` | `x-verwerking` | Processing register reference |
| `protocollering.gebruiker.header` | `x-gebruiker` | Logged-in user identity |
| `protocollering.toepassing.header` | `x-toepassing` | Application name (fixed: `ZAC`) |

If your provider uses different header names, override the `header` value in your Helm values.
