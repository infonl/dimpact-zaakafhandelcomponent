from openforms.conf.docker import *  # noqa

# Disable 2FA for local development so the admin UI at http://localhost:8007/admin/
# and http://localhost:8009/admin/ can be accessed with username/password only.
MAYKIN_2FA_ALLOW_MFA_BYPASS_BACKENDS = ["django.contrib.auth.backends.ModelBackend"]

# The arm64 INFO.nl repackaged image (ghcr.io/infonl/open-formulieren) is missing the UMD
# bundle (open-forms-sdk.js) from its static files. ManifestStaticFilesStorage raises
# ValueError for any missing manifest entry, breaking the admin login page.
# Use StaticFilesStorage to skip manifest validation for local development.
STATICFILES_STORAGE = "django.contrib.staticfiles.storage.StaticFilesStorage"
