# Welcome
You have just created a backstaged managed dev environment.
We use backstage to manage the Service Creation, cataloging, life cycle management and documentation for services based on golden path standards.

You now have a basic environment that you can build upon

## Things to note:
1. The backstage portal is your home for all things related to your service. Its a great place to live
2. Docs travel with your service. Docs relating to your service should be annotated in the `/doc` directory
3. CI/CD has been setup, you can connect to this directly form the backstage portal

## Lets Start
Now all the hard work is out of the way, you're free to do your thing. Code away!

## Error Prone

The [Error Prone](https://errorprone.info/) tool is employed in order to provide for static analysis of the code.  It will check for a number of common mistakes that developers might make.

Some of the mistakes are classified as errors and in this case the build will fail.  Other mistakes are classified as warnings and in this case, the warnings will be output to the build console.  An abridged example warning might be;

```
[WARNING] /home/.../src/main/java/nz/.../Application.java:[22,17] [MissingCasesInEnumSwitch] Non-exhaustive switch; either add a default or handle the remaining cases: HAMMER, DRILL
    (see https://errorprone.info/bugpattern/MissingCasesInEnumSwitch)
```

Sometimes Error Prone may raise an error or warning incorrectly.  In this case, you can supress it checking a section of code as follows;

```
@SuppressWarnings("MissingCasesInEnumSwitch")
public static void main(String[] args) {
  ...
}
```

It is possible to test the analysis using;

```
mvn clean compile
```

## Dependency Convergence

Different [Maven](https://maven.apache.org/) dependencies are employed in a project and these in turn have their own dependencies transitively.  A problem can arise where the same artifact is included via different paths of dependencies more than once.  In this case the versions of those dependencies can be different and so it becomes difficult to tell which version is actually going to be included in the build product.  In addition, small changes in the dependencies of the project at the top level can swap the version of transitive dependencies employed and thus unexpectedly destabilise a build.  We call this a "convergence" problem.

To avoid a convergence problem, the [Maven Enforcer](https://maven.apache.org/enforcer/maven-enforcer-plugin/) plugin runs on each build to detect when there is a convergence problem.  If the plugin finds there is a problem, it will fail the build requiring you to then go and resolve the issue by excluding dependencies in order to stipulate explicitly which combination of dependencies should be used.

A simple typical convergence error would appear like this in the Maven output;

```
...
[INFO] --- maven-enforcer-plugin:1.4.1:enforce (enforce-versions) @ {{cookiecutter.artifact_id}} ---
[WARNING]
Dependency convergence error for commons-io:commons-io:2.2 paths to dependency are:
+-nz.co.twg.do:{{cookiecutter.artifact_id}}:1.0.0-SNAPSHOT
  +-org.apache.axis2:axis2-kernel:1.7.9
    +-commons-fileupload:commons-fileupload:1.3.3
      +-commons-io:commons-io:2.2
and
+-nz.co.twg.do:{{cookiecutter.artifact_id}}:1.0.0-SNAPSHOT
  +-org.apache.axis2:axis2-kernel:1.7.9
    +-commons-io:commons-io:2.1

[WARNING] Rule 0: org.apache.maven.plugins.enforcer.DependencyConvergence failed with message:
Failed while enforcing releasability the error(s) are [
Dependency convergence error for commons-io:commons-io:2.2 paths to dependency are:
+-nz.co.twg.do:{{cookiecutter.artifact_id}}:1.0.0-SNAPSHOT
  +-org.apache.axis2:axis2-kernel:1.7.9
    +-commons-fileupload:commons-fileupload:1.3.3
      +-commons-io:commons-io:2.2
and
+-nz.co.twg.do:{{cookiecutter.artifact_id}}:1.0.0-SNAPSHOT
  +-org.apache.axis2:axis2-kernel:1.7.9
    +-commons-io:commons-io:2.1
```

## How to test

### Unit Tests

#### How to run unit tests
```shell
mvn verify
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

### TLDR;

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

## Backstage managed info
Do not modify this part, it is managed by backstage

```
Template name: {{cookiecutter.artifact_id}}-template
Version: 2021-10-07
```
