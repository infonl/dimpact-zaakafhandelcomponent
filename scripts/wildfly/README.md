# Local WildFly development

These steps are part of the overall [ZAC installation instructions](../../docs/development/INSTALL.md).

## Install WildFly

1. Run the `install-wildfly.sh` script.
2. This should install a local and configured version of Wildfly in the root of this project.

## Update WildFly

1. Run the `update-wildfly.sh` script.
2. This will create a new directory with the new version of WildFly. You can now edit your runtime configuration and point it to the new wildfly directory. All settings can remain the same.
3. (optional) You can now remove the old WildFly directory.
