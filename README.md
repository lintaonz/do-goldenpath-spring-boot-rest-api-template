# do-goldenpath-spring-boot-rest-api
---
This is the Golden Path template for building Java Spring Boot REST APIs.

## What is included

- A Spring Boot REST API
- ECS Logging
- Jenkins pipeline
- Kubernetes deployment
- OpenAPI spec
- Open Telemetry (coming soon)
- Prometheus stats

## How do I use it?
---
If you want to create a project using this template then go to [Backstage](https://backstage.twg.co.nz) and select the template named `do-goldenpath-spring-boot-rest-api-template` when creating your new component. [This repo](https://bitbucket.org/twgnz/do-goldenpath-spring-boot-rest-api/src/master) is a completed example that is kept in sync with the template.

## How do I work on it?
---
It is best NOT to make changes to this repository directly, because all of the templating variables make it hard to read and impossible to open with an IDE.

You should follow these steps instead:

1. make the changes in the [do-goldenpath-spring-boot-api](https://bitbucket.org/twgnz/do-goldenpath-spring-boot-rest-api/src/master) project
1. once your changes are merged follow [the instructions](https://bitbucket.org/twgnz/do-goldenpath-spring-boot-rest-api/src/master/template-generator/README.md) for generating the template from that repo
1. open a separate pull request against this template repository

### Cookiecutter

#### How to manually generate the project out of template.

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
