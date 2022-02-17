# {{cookiecutter.artifact_id}}
Edit this README.md file to suit your component "{{cookiecutter.artifact_id}}".

You should try to give some detail about what this component does and some details about its implementation
so that other people can understand;
- What this project is about
- What purpose it serves as a component in the system
- Any project specific setup or quirks to get started and ready for local development.

Corresponding changes should also be made in `docs/index.md` because that file is the top level documentation
that is exposed in the Backstage system.

## Tech Docs

### Upstream Maintained Docs
These docs are part of what the template offers. Please refrain from changing these, as they will be updated
when the upstream template gets updated in the future.

However, if they do need to be modified to match the project's requirement, please do update them and
shift them into the [Project Specific Docs](#project-specific-docs) section.

- [Build Tools](docs/build-tools.md)
- [Development Environment Variables](docs/development-env-vars.md)
- [Local Development](docs/local-development.md)
- [Testing](docs/testing.md)
- [Deployment](docs/deployment.md)
- [Feature Toggles](docs/feature-toggles.md)

### Project Specific Docs
Feel free to add more project specific tech docs here.

### Showing Tech Docs in Backstage
Any Markdown documentations written can be reflected in Backstage
tech-docs by adding the file to the `docs` directory and registering them in the `mkdocs.yaml` file.
