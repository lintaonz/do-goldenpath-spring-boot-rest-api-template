# do-goldenpath-spring-boot-rest-api

This is the Golden Path template for building Java Spring Boot REST APIs.

## What is included

- A Spring Boot REST API
- ECS Logging
- Jenkins pipeline
- Kubernetes deployment
- OpenAPI spec
- Open Telemetry (coming soon)
- Prometheus stats


## Cookiecutter

### How to manually generate the project out of template.


Following is a quick dirty way to generate project from template. **TODO: Make it nice and easy.**
```
# only works if the template folder is named as {{cookiecutter.component_id}} or similar
# because of this https://github.com/cookiecutter/cookiecutter/blob/b0c5e3f94df601b43eb2328f9a6d6bfe2d9bccd6/cookiecutter/find.py#L22

# The values are read from cookiecutter.json file.

rm -rf '{{cookiecutter.component_id}}' && \
rm -rf ./tmp/output && \
cp -r template '{{cookiecutter.component_id}}' && \
cookiecutter --no-input  -o $(pwd)/tmp/output . --verbose
```
