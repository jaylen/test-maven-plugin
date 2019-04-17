test-maven-plugin
=================

#### About

A plugin for Maven to facilitate tests of other Maven plugins. The plugin creates
local repository and installs there there artefact to be tested along with its
dependencies. After that each test project is built with Maven by spawning separate
Maven process with appropriate command line arguments. The result of each build is
stored in a log file for further analysis.

This is work in progress, so bear with me, more things are coming.

#### Requirements

- Maven: 3.6 or above
- JDK: 8 or above

#### Licence

[Apache-2.0][1]

[1]: https://spdx.org/licenses/Apache-2.0.html
