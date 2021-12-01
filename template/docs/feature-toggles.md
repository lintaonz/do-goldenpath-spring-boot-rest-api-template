# Feature Toggles
Feature toggling is one of the key components to enable a successful trunk based development.
Having the ability to keep a code path off even when the code is merged into the main branch avoids
big long living branches, ugly merge conflicts, and frustrating code stabilizing/freezing period.
Eliminating these problems speeds up the team's performance and increases autonomy.

### **Use cases**

Below are some common use cases for using feature toggles (non-exhaustive).

#### Release Date Case
A user interface element is to be shown to users only at the "correct time" but the development needs
to be prepared "ready to go". In this case, the user interface element can be hidden behind a feature
flag and released when the time comes to make this user interface visible to users.
By doing this the changes are able to go into trunk and roll out prior to being switch on.
This makes the timing of the release easy and also allows any unintended consequences of the change
able to be seen ahead of time. This covers situations where backend logic is also "moment in time"
dependent such as meeting a regulatory deadline.

#### Inter-project Coordination Case
Two system components may need to coordinate changes, but their deployment cycles are not in lock-step.
In this case the feature-flag can be used to disable the change in each of the systems so that they
can be turned on in the running system in unison. By doing this the changes are able to go into trunk
ahead of time and the feature flag can be used to switch this on when both systems are ready without
having to hold back changes where there may be delays causing merge difficulties.
This would otherwise require a difficult coordinated deployment.

#### Long Running Case
A piece of work is large and is extrapolated over a number of months with a number of changes in a
number of systems. If the changes are held in a long living branch, then the risk and difficulty of
the rebase/merge is large.  By using feature-flagging, the behaviours of the large project can be
turned off through feature-flagging until ready but the work-stream and any merge risk as well as any
unintended consequences are discovered early.

#### Don't Have Time Case
A piece of work has to be released, and it requires to be observed or monitored by somebody during the
go live. Everyone is busy, but will have time in the near future.  The work can proceed and go out with
other changes, but can be disabled until somebody has the time to verify and check by using a feature-flag.

#### Step-by-Step Enabling Case
Sometimes to complete a feature, step-by-step procedures may be required, and these steps may require
manual intervention or sanity checks before enabling the next step. Feature flags can be used to
facilitating the staged enablement of a feature.

An example would be a feature flag for enabling the ability for triggering data migration/synchronization,
followed by another feature flag for switching to the new data source.

### **Important Notes**
#### Housekeeping is important
Without proper housekeeping, the number of feature toggles will keep growing, increasing the difficulty
for testing different combinations of code paths. When a feature is enabled in production, always try to
tidy-up/removing the enabled flag.


## LaunchDarkly
LaunchDarkly is an online platform that offers feature toggle functionality.
The platform provides SDK for accessing feature toggle values, and REST APIs to mutate the feature
toggle state.

Read their development documentations [here](https://docs.launchdarkly.com/home).

LaunchDarkly will be the backing implementation for handling the management and the "source of truth"
of all feature flags.

## Code
To maintain framework agnostic, a thin wrapper class `nz.co.twg.common.features.Features` has been introduced
for allowing simple querying of feature toggles. This class is created as a bean and injected to
components that require access to feature toggles.

```java
if (features.isActive("feature-key")) {
    // do this
} else {
    // do that
}
```

If a reactionary/push type of approach is desired, one can register change listeners against a key.
This is especially helpful if a functionality is to be toggled on and off during runtime without needing
to rely on a separate thread polling for a state change.

e.g. turning on and off
a connection to a third party system, such as Kafka, database, or a SFTP connection.

```java
features.registerChangeListener("feature-key", (oldState, newState) -> {
    if (newState) {
        // do this
    } else {
        // do that
    }
});
```

## Local Development
For ease of local development, by default, the local Spring profile will be enabled. This will also configure the LaunchDarkly SDK to source its value from a local file.

This file by default will exist in the temporary directory (varies by OS). Feel free to configure to wherever is desired.
* For Linux / macOS, this will by default be `/tmp`
* For Windows, this will be by default be `C:\Users\<user>\AppData\Local\Temp`)

The content of the file follows the following format:

```json
{
    "flagValues": {
        "featureKey1": true,
        "featureKey2": false,
        "featureKey3": true,
        ...
    }
}
```

One can modify this file manually to change the feature key value, but that can be a bit manual and tedious.
An Actuator endpoint `https://localhost:{actuatorPort}/features` is available for accessing and mutating the feature
values via HTTP requests.

## Actuator
The following actuator endpoint has been created to allow simple management operations on feature toggles.
They can be found under `https://{host}:{actuatorPort}/features`.

> GET - `https://localhost:{actuatorPort}/features`- Returns all features

> GET - `https://localhost:{actuatorPort}/features/{key}` - Returns the value for the given feature key

> POST - `https://localhost:{actuatorPort}/features/{key}` - Modify the value for a given feature key
> ```json
> {
>     "value": true | false
> }
> ```

> DELETE - `https://localhost:{actuatorPort}/features/{key}` - Remove the given feature key

> DELETE - `https://localhost:{actuatorPort}/features` - Remove all features

The mutating endpoints (`POST`, `DELETE`) are only available if the LD data-source is a file, and the `auto-reload` config must be true.

## Testing
Since the underlying implementation is abstracted away via the `nz.co.twg.common.features.Features` wrapper
class, mock objects can be easily plugged in during unit tests.

```java

@Mock private Features features;

// service under test
private MyService service;

@BeforeEach
public void setup() {
    MockitoAnnotations.openMocks(this);
    this.service = new MyService(features);
}

@Test
public void test() {
    // given
    when(features.isActive("feature-key")).thenReturn(true);

    // when
    this.service.doSomething();

    // then
    // assert something
}
```

## Component Testing
During component testing, feature toggle status  can be controlled via the
`nz.co.twg.common.features.FeaturesSupport` class.

The `nz.co.twg.service.{{cookiecutter.java_package_name}}.componenttest.util.ActuatorFeaturesSupport` implementation
will use the actuator endpoint provided by the running service to mutate the feature states.

```java
private final FeaturesSupport featuresSupport =
        new ActuatorFeaturesSupport(getHostname(), getActuatorPort());
@Test
public void test() {
    // given
    this.featuresSupport.configure("feature-key", true);

    // when
    // do something

    // then
    // assert something
}
```
