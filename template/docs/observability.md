## Observability

### Overview
To observe how this component performs in different environments, a directory named `observability-config` has been added to this repo with the following structure. This helps us configure our observability platform -[newRelic](https://newrelic.com/) - using `config as code` approach.

```
├── observability-config
│   ├── dev
│   │   └── dashboards
│   │       └── dashboard-name-x.json
│   ├── prod
│   │   └── dashboards
│   │       └── dashboard-name-x.json
│   └── test
│       └── dashboards
│           └── dashboard-name-x.json
```

Each environment has its own set of configurations for dashboards which will be automatically applied when they are updated in the `master` branch - a pipeline named `{{cookiecutter.artifact_id}}-observability` has been setup to run and apply to all environments.


### What's provided
This repo comes with a default dashboard per environment to observe golden metrics for this component.

### How to add/update dashboards
You can either work with `json` files directory if you are comfortable doing that or you can export a working dashboard from newRelic, [see here](https://docs.newrelic.com/docs/query-your-data/explore-query-data/dashboards/dashboards-charts-import-export-data/), and add to the directory you want.

### How to view dashboards
The easiest way is to open this component in backstage, [click here](https://backstage.twg.co.nz/catalog/default/component/{{cookiecutter.artifact_id}}) and then click on the newRelic dashboard link provided in the links section.

Or, you can login to newRelic and find the dashboards there, the dashboards will have this component's name `{{cookiecutter.artifact_id}}` in them.
