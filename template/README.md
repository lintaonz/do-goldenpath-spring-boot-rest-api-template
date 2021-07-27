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

## How to test

### Unit Tests
```shell
mvn verify
```

This will do the following steps
- Compile the code
- Run unit tests

### Integration Tests
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
- Deploy the application docker image and other required components to kubernetes e.g. postgres db and wiremock. See `src/java/main/jkube`
- Run integration tests (These could include component, function, integration and any other tests)
- Clean up the kubernetes by undeploying the components

Notes:
- The docker application might not be able to use host's NodePort if it is not running as administrator. In that
  case, run your docker application as administrator.

### Sonar scan

You will need to include settings-security.xml file in your `HOME/.m2` directory in order to decrypt token. If you don't have that file you can obtain it from maven-settings-security secret in jenkins namespace.

```shell
mvn sonar:sonar
```

To get settings-security.xml:
```shell
kubectl -n jenkins get secret maven-settings-security --template='{{ "{{" }} index .data "settings-security.xml" | base64decode {{ "}}" }}' > ~/.m2/settings-security.xml
```

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