## Local Development Instructions

### Expected Environment Variables

Please note that for development purposes some environment variables
will need to be set as a prerequisite.  See [here](development-env-vars.md)
for details.

### Building the Project

To build the project you will need to issue the following command;

```bash
# First build
mvn clean package -s settings.xml

# Subsequent builds
mvn clean package
```

This is an **important** step. The template relies on generated code (from `openapi-generator-maven-plugin` and
`jsonschema2pojo-maven-plugin`), so you must build the project first, then import the project into the IDE for it to
recognize the generated classes.

This can be done AFTER the import, but will require the maven project be reloaded (the IDE can be smart enough
to reload itself, but sometimes it requires a manual reload).

The `-s settings.xml` option is required for the first build to allow some TWG specific common libraries in the TWG Nexus
to be downloaded into the local `.m2` cache. Once they're in the cache, this option can be skipped.

If you have no access to the TWG internal network, you may need to download the missing libraries from Bitbucket and run
`mvn install` on each of them manually. The projects to download and install are:
- do-lib-twg-annotation
- do-lib-twg-common
- do-lib-twg-jsonschema2pojo
- do-lib-twg-openapigenerator

For more information about these libraries, please read the [SDEM Encryption & Decryption](sdem-encryption-decryption.md)
documentation.

### Generated sources

Generated code produced by `openapi-generator-maven-plugin` and `jsonschema2pojo-maven-plugin` can be found under
`target/generated-sources`. These will not be committed to the source control.

### Running the Project

#### Starting Support Services

Assuming the presence of a local Kubernetes environment, the project conveniently has
the ability to start up PostgresDB and Wiremock to work with. It is possible to launch a Kubernetes
pod with containers for these support services by issuing the following command:

Assuming the presence of a local Kubernetes environment, this project is configured to spin up PostgresDB and Wiremock in a local kube cluster with the
following command:

```
mvn k8s:resource k8s:apply
```

When you have finished, the following command will shutdown the support services;

```
mvn k8s:resource k8s:undeploy
```

#### Starting the Application

The Spring application has been configured to run with the `local` profile by default, and
can be tweaked further in `application-local.yml`.

The `local` profile has
- A data source db connection in place.
- LaunchDarkly configured to read from a local file instead of connecting to the SaaS.
- `features` actuator endpoint enabled for ability to tweak feature flags via HTTP.
- The logging profile tuned to output in the regular format instead of ECS format.
