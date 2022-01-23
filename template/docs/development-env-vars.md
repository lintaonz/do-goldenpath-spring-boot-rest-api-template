## Development Environment Variables

### Required Variables

Some environment variables are expected for performing
local development work;

|Variable| Abridged Example                      |
|---|---------------------------------------|
|JAVA_HOME| `C:\Progra...\jdk-11.0.13.8-hotspot\` |
|DOCKER_HOST| `tcp://localhost:2375`                |
|KUBECONFIG| `C:\Users\...\.kube\config`|

The `KUBECONFIG` should be a configuration that corresponds to
your **local** Kubernetes environment.

### How to Set a Variable

To set an environment variable in Windows PowerShell;

```
$env:KUBECONFIG = "$HOME\.kube\config"
```

To set an environment variable in bash;

```
export KUBECONFIG="$HOME/.kube/config"
```

Depending on your preference and requirements, it may be more convenient
to configure these in your Windows Preferences or in the `.bashrc` file.
