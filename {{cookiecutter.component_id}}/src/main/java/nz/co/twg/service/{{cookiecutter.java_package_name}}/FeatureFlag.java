package nz.co.twg.service.{{cookiecutter.java_package_name}};

/** Feature flags. */
public enum FeatureFlag {
    {{cookiecutter.artifact_id|upper|replace("-", "_")}}_UPPERCASE_NAME,
    {{cookiecutter.artifact_id|upper|replace("-", "_")}}_INCLUDE_DOGS_FROM_THIRD_PARTY
}
