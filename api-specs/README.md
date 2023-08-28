# API specs for manually generated client stubs

The Open API specs in this directory have been used to manually generate the corresponding Java client stubs.
These client stubs have subsequently been added to the [src/main/java](../src/main/java) code base.

In contrast, other Java client stubs are automatically generated from their Open API specs dynamically at build time.
The Open API specs for these client stubs are located in the [src/main/resources/api-specs](../src/main/resources/api-specs) directory.

The reason why this was not done for these manually generated stubs is that they required some manual
tweaking to make them work in this project.
