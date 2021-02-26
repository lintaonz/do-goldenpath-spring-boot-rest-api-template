#!groovy

def artifactId = "{{cookiecutter.artifact_id}}"
def deployToProd = false

pipeline {
    agent none

    options {
        timestamps()
        buildDiscarder(logRotator(numToKeepStr: "20"))
        skipStagesAfterUnstable()
        timeout time: 30, unit: "MINUTES"
    }

    stages {
        stage("Build & Publish Artifact") {
            agent {
                kubernetes {
                    yaml kubernetes_build_agent
                    inheritFrom "k8s-docker-image-build-template"
                    yamlMergeStrategy merge()
                }
            }

            stages {
                stage("Compile & Test") {
                    steps {
                        container("maven") {
                            sh "mvn clean verify"
                        }
                    }
                }

                stage("Push Artifact") {
                    when {
                        expression {
                            BRANCH_NAME == "master"
                        }
                    }

                    steps {
                        container("maven") {
                            sh "mvn deploy -DskipTests"
                        }
                    }
                }
            }
        }

        stage("Lock & Deploy to Dev") {
            agent {
                kubernetes {
                    yaml kubernetes_deploy_agent
                }
            }

            options {
                lock resource: "${JOB_NAME}-dev", quantity: 1, variable: "deployEnv"
            }

            when {
                beforeOptions true
                beforeAgent true

                expression {
                    BRANCH_NAME == "master"
                }
            }

            stages {
                stage("Deploy to dev") {
                    environment {
                        CHART_NAME = "do-container-deployment"
                        CHART_VERSION = "0.1.8"
                        ENVIRONMENT = "dev"
                        K8S_NAMESPACE = "do-dev"
                        K8S_DEV_CLUSTER_CREDS_ID = "EKS_dev_cluster_kubeconfig"
                        K8S_DEV_CLUSTER_AWS_CREDS_ID = "EKS_dev_cluster_AWS_credentials"
                        NEXUS_CREDS = credentials("do-nexus-credentials")
                        NEXUS_REPO_BASE_URL = "https://nexus-dev-aws.twg.co.nz/repository/do-helm-charts/"

                        expression {
                            BUILD_VERSION = "${GIT_COMMIT}".replaceAll("[^\\x00-\\x7F]", "")
                        }

                        expression {
                            CHART_DOWNLOAD_DIRECTORY = "./helm-chart-${CHART_NAME}-${CHART_VERSION}"
                        }

                        expression {
                            CHART_DIRECTORY = "${CHART_DOWNLOAD_DIRECTORY}/${CHART_NAME}"
                        }
                    }

                    steps {
                        container("helm") {
                            script {
                                withCredentials([file(credentialsId: K8S_DEV_CLUSTER_CREDS_ID, variable: "KUBECONFIG"),
                                                 file(credentialsId: K8S_DEV_CLUSTER_AWS_CREDS_ID, variable: "AWS_SHARED_CREDENTIALS_FILE")]) {
                                    sh "helm pull ${CHART_NAME} --version ${CHART_VERSION} --untar --untardir ${CHART_DOWNLOAD_DIRECTORY} --repo ${NEXUS_REPO_BASE_URL} --username ${NEXUS_CREDS_USR} --password ${NEXUS_CREDS_PSW}"
                                    sh "helm upgrade --wait --debug --install --K8S_NAMESPACE ${K8S_NAMESPACE} -f deploy/${ENVIRONMENT}/values.yaml --set-string appVersion=${BUILD_VERSION} ${artifactId} ${CHART_DIRECTORY}"
                                }
                            }
                        }
                    }
                }
            }
        }

        stage("Lock & Deploy to Test") {
            agent {
                kubernetes {
                    yaml kubernetes_deploy_agent
                }
            }

            options {
                lock resource: "${JOB_NAME}-test", quantity: 1, variable: "deployEnv"
            }

            when {
                beforeAgent true
                beforeOptions true

                expression {
                    BRANCH_NAME == "master"
                }
            }

            stages {
                stage("Deploy to Test") {
                    environment {
                        CHART_NAME = "do-container-deployment"
                        CHART_VERSION = "0.1.8"
                        ENVIRONMENT = "test"
                        K8S_NAMESPACE = "do-test"
                        K8S_DEV_CLUSTER_CREDS_ID = "EKS_dev_cluster_kubeconfig"
                        K8S_DEV_CLUSTER_AWS_CREDS_ID = "EKS_dev_cluster_AWS_credentials"
                        NEXUS_CREDS = credentials("do-nexus-credentials")
                        NEXUS_REPO_BASE_URL = "https://nexus-dev-aws.twg.co.nz/repository/do-helm-charts/"

                        expression {
                            BUILD_VERSION = "${GIT_COMMIT}".replaceAll("[^\\x00-\\x7F]", "")
                        }

                        expression {
                            CHART_DOWNLOAD_DIRECTORY = "./helm-chart-${CHART_NAME}-${CHART_VERSION}"
                        }

                        expression {
                            CHART_DIRECTORY = "${CHART_DOWNLOAD_DIRECTORY}/${CHART_NAME}"
                        }
                    }

                    steps {
                        container("helm") {
                            script {
                                withCredentials([file(credentialsId: K8S_DEV_CLUSTER_CREDS_ID, variable: "KUBECONFIG"),
                                                 file(credentialsId: K8S_DEV_CLUSTER_AWS_CREDS_ID, variable: "AWS_SHARED_CREDENTIALS_FILE")]) {
                                    sh "helm pull ${CHART_NAME} --version ${CHART_VERSION} --untar --untardir ${CHART_DOWNLOAD_DIRECTORY} --repo ${NEXUS_REPO_BASE_URL} --username ${NEXUS_CREDS_USR} --password ${NEXUS_CREDS_PSW}"
                                    sh "helm upgrade --wait --debug --install --K8S_NAMESPACE ${K8S_NAMESPACE} -f deploy/${ENVIRONMENT}/values.yaml --set-string appVersion=${BUILD_VERSION} ${artifactId} ${CHART_DIRECTORY}"
                                }
                            }
                        }
                    }
                }
            }
        }

        stage("Approve Prod deployment?") {
            agent none

            when {
                beforeAgent true
                beforeOptions true

                expression {
                    return BRANCH_NAME == "master"
                }
            }

            steps {
                script {
                    def proceed = true

                    try {
                        timeout(time: 10, unit: "SECONDS") {
                            input(message: "Deploy this build to PROD?")
                        }
                    } catch (err) {
                        proceed = false
                    }

                    if (proceed) {
                        deployToProd = true
                    }
                }
            }
        }

        stage("Lock & Deploy to Prod") {
            agent {
                kubernetes {
                    yaml kubernetes_deploy_agent
                }
            }

            options {
                lock resource: "${JOB_NAME}-prod", quantity: 1, variable: "deployEnv"
            }

            when {
                beforeAgent true
                beforeOptions true

                expression {
                    BRANCH_NAME == "master"
                }

                expression {
                    return deployToProd
                }
            }

            steps {
                container("helm") {
                    script {
                        sh "echo "deploy to prod""
                    }
                }
            }
        }
    }
}
