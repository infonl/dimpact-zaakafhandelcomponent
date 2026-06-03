from openforms.conf.docker import *  # noqa

# Disable 2FA for local development so the admin UI at http://localhost:8007/admin/
# and http://localhost:8009/admin/ can be accessed with username/password only.
MAYKIN_2FA_ALLOW_MFA_BYPASS_BACKENDS = ["django.contrib.auth.backends.ModelBackend"]

# Enable demo/simulatie plugins (digid-mock) for local development.
FLAGS = {
    **FLAGS,
    "ENABLE_DEMO_PLUGINS": [{"condition": "boolean", "value": True}],
}

# The ARM64 image (ghcr.io/infonl/open-formulieren) does not include the UMD (.js)
# SDK bundle in its staticfiles.json manifest — only .mjs (ESM) bundles are present.
# ManifestStaticFilesStorage raises ValueError for any missing manifest entry, crashing
# every page render. Non-manifest storage bypasses this check for local development.
STATICFILES_STORAGE = "django.contrib.staticfiles.storage.StaticFilesStorage"
