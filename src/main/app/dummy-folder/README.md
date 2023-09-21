Dummy folder to serve as the 'output' folder for our custom Gradle 'npmRunTest' task.

This Gradle task does not have any output but Gradle incremental builds require at least one output folder so
we use this dummy folder for this purpose.
