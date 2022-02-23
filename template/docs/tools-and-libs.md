# Tools and Libs

Below are the tools that has been set up to ensure consistency, quality of life, and healthiness
of the project.

## Error Prone

The [Error Prone](https://errorprone.info/) tool is employed in order to provide for static analysis of the code.  It will check for a number of common mistakes that developers might make.

Some mistakes are classified as errors and in this case the build will fail.  Other mistakes are classified as warnings and in this case, the warnings will be output to the build console.  An abridged example warning might be;

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

## OWASP Dependency Vulnerability Check

The OWASP Dependency Vulnerability Check is built into the project's build lifecycle.
One can execute the check by enabling the `dependencyCheck` profile.

```bash
mvn clean verify -DskipTests -P dependencyCheck
```

The first execution will take a while, but subsequent runs should be much faster.

The generated report can be found under `target/dependency-check-report.html`.
