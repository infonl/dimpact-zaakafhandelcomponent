# Django settings module that disables enforced two-factor authentication for
# local docker-compose development.
#
# Upstream only reads DISABLE_2FA in openarchiefbeheer.conf.dev, which is not part
# of the openarchiefbeheer.conf.docker -> production -> base import chain used by
# this container, so setting the DISABLE_2FA env var has no effect here.
# Switching DJANGO_SETTINGS_MODULE to openarchiefbeheer.conf.dev instead is not an
# option either: it unconditionally adds django-debug-toolbar to INSTALLED_APPS,
# which is not installed in the production image this container runs from.
#
# This mirrors what conf.dev does for its DISABLE_2FA handling, without pulling in
# those dev-only dependencies.
from openarchiefbeheer.conf.docker import *  # noqa: F401,F403

MAYKIN_2FA_ALLOW_MFA_BYPASS_BACKENDS = AUTHENTICATION_BACKENDS  # noqa: F405
