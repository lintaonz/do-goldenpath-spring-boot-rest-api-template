# Observability

## Overview

To observe how this component performs in different environments, a directory named `observability-config` has been added to this repo with the following structure. This helps us configure our observability platform -[newRelic](https://newrelic.com/) - using `config as code` approach.

```shell
├── observability-config
│   ├── dev
│   │   ├── dashboards
│   │   │   └── dashboard-name-x.json
│   │   └── alerts
│   │       ├── descriptor.yaml
│   │       └── name-condition.gql
│   ├── prod
│   │   ├── dashboards
│   │   │   └── dashboard-name-x.json
│   │   └── alerts
│   │       ├── descriptor.yaml
│   │       └── name-condition.gql
│   └── test
│       ├── dashboards
│       │   └── dashboard-name-x.json
│       └── alerts
│           ├── descriptor.yaml
│           └── name-condition.gql
│
└── Jenkinsfile.observability
```

Each environment has its own set of configurations for dashboards and alerts which will be automatically applied when they are updated in the `master` branch - a pipeline named `{{cookiecutter.artifact_id}}-observability` has been setup to run and apply to all environments.

## What's provided

This repo comes with a default dashboard and alerts per environment to observe golden metrics for this component.

## How to add/update dashboards

You can either work with `json` files directory if you are comfortable doing that or you can export a working dashboard from newRelic, [see here](https://docs.newrelic.com/docs/query-your-data/explore-query-data/dashboards/dashboards-charts-import-export-data/), and add to the directory you want.

## How to view dashboards

The easiest way is to open this component in backstage, [click here](https://backstage.twg.co.nz/catalog/default/component/{{cookiecutter.artifact_id}}) and then click on the `New Relic Dashboards` link provided in the links section.

Or, you can login to newRelic and find the dashboards there, the dashboards will have this component's name `{{cookiecutter.artifact_id}}` in them.

## How to add/update alerts

### Add/update descriptor.yaml file

You can add/update policy and condition definitions by editing the `descriptor.yaml` file.

```yaml

apiVersion: twg.co.nz/v1alpha1
kind: newrelicAlert
metadata:
  policies:
    # The policy name
    - name: "{{cookiecutter.prefix|upper}} | {{cookiecutter.artifact_id}} | PROD"
      incidentPreference: PER_POLICY
      conditions:
        # The condition name
        - name: Error rate
          # The condition NerdGraph (GraphQL) code file
          definition: error-rate-condition.gql
          # The condition tags
          tags:
            - key: service
              value: {{cookiecutter.artifact_id}}
            - key: environment
              value: prod
            - key: criticality
              value: medium
      ...
```

### Add/update condition NerdGraph (GraphQL) code file

Each condition must have a NerdGraph (GraphQL) code file. You can either edit the condition code:

* Following the [NerdGraph tutorial](https://docs.newrelic.com/docs/apis/nerdgraph/examples/nerdgraph-api-nrql-condition-alerts).
* Copy/paste the `condition` code from [UI](https://thewarehouse.atlassian.net/wiki/spaces/DC/pages/2814149044/How-to+Add+default+Observability+Dashboards+and+Alerts+to+NewRelic+from+Backstage#How-to-update-the-{condition-name}.gql).

## How to view alerts

The easiest way is to open this component in backstage, [click here](https://backstage.twg.co.nz/catalog/default/component/{{cookiecutter.artifact_id}}) and then click on the `New Relic Alerts` link provided in the links section.
