## Local Development Instructions

### Expected Environment Variables

Please note that for development purposes some environment variables
will need to be set as a prerequisite.  See [here](development-env-vars.md)
for details.
Assuming the presence of a local Kubernetes environment, this project is configured to spin up PostgresDB and Wiremock in a local kube cluster with the
following command:

```
mvn k8s:resource k8s:apply
```

And to spin down

```
mvn k8s:resource k8s:undeploy
```

Build...

```
mvn clean package
```

The Spring application has been configured to run with the `local` profile by default, and
can be tweaked further in `application-local.yml`.

The `local` profile has
- A data source db connection in place.
- LaunchDarkly configured to read from a local file instead of connecting to the SaaS.
- `features` actuator endpoint enabled for ability to tweak feature flags via HTTP.
- The logging profile tuned to output in the regular format instead of ECS format.



