package nz.co.twg.service.{{cookiecutter.java_package_name}};

public enum FeatureFlag {
    {{cookiecutter.artifact_id|upper|replace("-", "_")}}_UPPERCASE_NAME
}
