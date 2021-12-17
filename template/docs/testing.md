## Testing

### Unit Tests

#### How to run unit tests
```shell
mvn test
```

This will do the following steps
- Compile the code
- Run unit tests

### Component Tests

Component tests are part of the build. To run component tests, you need docker and kubernetes installed on your machine along with java.

The purpose of these tests are to make sure that our end docker image gets tested as a component, and we produce a fully tested component. This is part of our
`shift left on testing` strategy to build a fast feedback loop. With component tests, we are bringing functional tests in the build stage instead of doing them
after deployment.

The component tests should test the application using the mocked containerized backends. The application must not connect to live backend systems for
component testing, only local docker and local kubernetes environment should be used.

The component tests are under `src/componenttest/java` directory. This is to keep them separate from unit tests to maintain separation of concern. the name of test
files must end with `Test.java` for tests to be picked up for execution.

The [JKube plugin](https://www.eclipse.org/jkube/docs/kubernetes-maven-plugin) plugin spins up containers in local kubernetes once the application image has been
built. The kubernetes manifest files are under `src/componenttest/resources/jkube`. These files specify which containers should run during the component tests e.g.
if you want to run a wiremock container for mocking http calls or want to run a kafka server container to test against kafka, you need to add them to files in there.

#### How to run component tests

```shell
mvn verify -Pjkube
```

Make sure your local docker and kubernetes are running and have privileges to expose a port - run docker as administrator if possible.
This is needed for tests to call the application running in your local kubernetes.

`jkube` profile runs the component tests, specify it as `-Pjkube`.

```shell
# On windows command prompt
set KUBECONFIG=%HOME%/.kube/config && mvn verify -Pjkube

# On gitbash or linux command prompt
KUBECONFIG=$HOME/.kube/config mvn verify -Pjkube
```
where `%HOME%/.kube/config` points to local kubernetes environment

This is will do following steps
- Compile the code
- Run unit tests
- Build the application jar
- Build the application docker image using the application jar
- Deploy the application docker image and other required components to kubernetes e.g. postgres db and wiremock. See `src/componenttest/resources/jkube`
- Run component tests
- Clean up the kubernetes by undeploying the components

Notes:
- The docker application might not be able to use host's NodePort if it is not running as administrator. In that
  case, run your docker application as administrator.

#### TLDR;

Main problem with running `mvn verify` is that components tests seems to start before API itself is fully loaded resulting in some false postives.
Until that resolved you can use approach below.

```shell
# Only compile JAR, no Unit tests
mvn clean package -DskipTests -s ./settings.xml

# WARNING!!! Before continuing ensure your local k8s is running
#  and config is correct (not pointing to dev or god forbid - production environment)
# Below command should return something that you expect (in most cases - nothing)
# If it returns pods with names that are unfamiliar to you -
#  your config most likely pointing to different environment.
# If unsure at any point - consult with DevOps.
kubectl get pods

# Run kubernetes on your local without destroying it
# (you will need compiled JAR ready - see above)
mvn k8s:resource k8s:build k8s:apply -P jkube -s ./settings.xml

# If you feel like image is not being updated, remove it by running command below
#  and rerun previous command
docker image rm twgorg/{{cookiecutter.artifact_id}}:1.0.0-SNAPSHOT

# You can confirm that pods are running (when ready it should show 3/3 - it might take ~minute to complete):
kubectl get pods

# To tail logs for the app
kubectl logs --tail 0 -f $(kubectl get pods -l app={{cookiecutter.artifact_id}} -o name) -c app

# Only run IT, no UT (make sure your k8s pod is running - see above)
mvn test-compile failsafe:integration-test failsafe:verify -P jkube -s ./settings.xml

# When finished with testing you can remove running pod(s) like this
mvn k8s:undeploy -P jkube -s ./settings.xml
```

#### Running Service and Component Tests in the IDE
Sometimes it is ideal to run the services and component tests in your IDE, so they can be easily
controlled and debugged.
However, other components such as the database, or WireMock still need to be present for the component test
to function properly.

In the **How to run component tests** section, the `-P jkube` profile automatically
spins up your application, along with other support services (Postgres, Wiremock, etc) all in one package.

In this scenario, our service is already running in the IDE, and we just need the complementary services.

To run the support services in the kubernetes cluster, run:
```bash
mvn k8s:resource k8s:apply
```
Unlike `mvn verify -P jkube`, this will load a _local_ config with just the support services.

To clean up these pods after you're done with the testing, run:
```bash
mvn k8s:resource k8s:undeploy
```
