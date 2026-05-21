from openforms.conf.docker import *  # noqa

# Disable 2FA for local development so the admin UI at http://localhost:8007/admin/
# and http://localhost:8009/admin/ can be accessed with username/password only.
MAYKIN_2FA_ALLOW_MFA_BYPASS_BACKENDS = ["django.contrib.auth.backends.ModelBackend"]
