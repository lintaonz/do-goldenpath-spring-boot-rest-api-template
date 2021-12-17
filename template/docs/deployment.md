## Deployment to Dev / Test / Prod Environment

Once the changes are merged into the `master` branch, the deployment process (handled by Harness) will push the application
into a dev, test, and prod environment. The helm chart for each of these environments can be found under
the `<project.root>/deploy` directory.

Spring Boolt allows overriding application properties via environment variable, and therefore the helm charts
are also the place to export the environment dependent properties used by your application
to the appropriate values as environment variables in the `extraEnvs` section.

#### Secrets
Secrets should never be exposed in plain text. In this case, the secrets will need to be placed in a secret manager
(e.g. Kubernetes Secrets or Vault) and referenced using the code snippet below:

```yaml
extraEnvs:
   - name: <secret.environment.variable>
     valueFrom:
       secretKeyRef:
         name: <project.name>
         key: <secret.key>
```

For example, to set up LaunchDarkly SDK key as a secret:
1. Create the role, project, and project environments in LaunchDarkly.
2. Add the environment's SDK key to the secret manager for each environments.
3. Add the following snippet to each helm chart variables `values.yaml`.
   ```yaml
   - name: LAUNCHDARKLY_SDKKEY
     valueFrom:
       secretKeyRef:
         name: <project.name>
         key: LAUNCHDARKLY_SDKKEY
   ```
