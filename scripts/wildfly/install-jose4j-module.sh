#!/bin/bash

WILDFLY_HOME=$1
JOSE_VERSION="0.9.6"
MODULE_DIR="$WILDFLY_HOME/modules/org/jose4j/main"

echo ">>> Installing jose4j WildFly module..."

mkdir -p "$MODULE_DIR"

# Download jose4j jar
curl -s -L -o "$MODULE_DIR/jose4j-$JOSE_VERSION.jar" \
  "https://repo1.maven.org/maven2/org/bitbucket/b_c/jose4j/$JOSE_VERSION/jose4j-$JOSE_VERSION.jar"

# Create module.xml
cat <<EOF > "$MODULE_DIR/module.xml"
<?xml version="1.0" encoding="UTF-8"?>
<module xmlns="urn:jboss:module:1.9" name="org.bitbucket.jose4j">
  <resources>
    <resource-root path="jose4j-$JOSE_VERSION.jar"/>
  </resources>
  <dependencies>
    <module name="jboss.api"/>
  </dependencies>
</module>
EOF

echo "jose4j module installed at $MODULE_DIR ✅"
