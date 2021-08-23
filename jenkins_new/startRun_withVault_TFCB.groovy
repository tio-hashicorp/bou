#!groovy
import groovy.json.JsonOutput
import groovy.json.JsonSlurper

def getWorkspaceId(organization, workspace_name) {
    def response = httpRequest(
        customHeaders: [
                [ name: "Authorization", value: "Bearer " + env.BEARER_TOKEN ],
                [ name: "Content-Type", value: "application/vnd.api+json" ]
            ],
        url: "https://app.terraform.io/api/v2/organizations/" + organization + "/workspaces/" + workspace_name
    )

    def data = new JsonSlurper().parseText(response.content)
    println ("Workspace Id: " + data.data.id)
    return data.data.id
}

def doRun(workspaceid) {
    def payload = """
{
    "data": {
        "attributes": {
            "is-destroy":false,
            "message": "Triggered run from Jenkins (build #${env.BUILD_TAG})"
        },
        "type":"runs",
        "relationships": {
            "workspace": {
                "data": {
                    "type": "workspaces",
                    "id": "$workspaceid"
                }
            }
        }
    }
}
    """

    def response = httpRequest(
        customHeaders: [
                [ name: "Authorization", value: "Bearer " + env.BEARER_TOKEN ],
                [ name: "Content-Type", value: "application/vnd.api+json" ]
            ],
        httpMode: 'POST',
        requestBody: "${payload}",
        url: "https://app.terraform.io/api/v2/runs"
    )
    def data = new JsonSlurper().parseText(response.content)

    println ("Run id: " + data.data.id)
    println ("Run status" + data.data.attributes.status)

    return data.data.id
}


def listLiveRun(workspaceid) {
    def response = httpRequest(
        customHeaders: [
                [ name: "Authorization", value: "Bearer " + env.BEARER_TOKEN ],
                [ name: "Content-Type", value: "application/vnd.api+json" ]
            ],
        url: "https://app.terraform.io/api/v2/workspaces/${workspaceid}/runs"
    )
    def data = new JsonSlurper().parseText(response.content)

    println ("Number of runs: " + data.meta.pagination.'total-count')
    def result =  data.data
    def live_runs = []
    def i = 0
    result.each {
        def status = it.attributes.status
        if (status == "discarded" || status == "applied" || status == "errored" || status == "canceled" || status == "force_canceled") {
            i = i + 1
        } else {
            println "$it.id : $it.attributes.status"
            live_runs.push(it.id)
        }
    }

    i = live_runs.size()
    println ("number of live runs:" + i)
    return live_runs
}


def configuration = [vaultUrl: 'http://192.168.1.73:8200',
        vaultCredentialId: 'vault-token-root']

def secrets = [
        [path: 'secret1/innovation-lab', engineVersion: 1, secretValues: [
                [envVar: 'api_token', vaultKey: 'api_token']]
        ]
]

pipeline {
  agent any
  parameters {
      string(name: 'ORGANIZATION', defaultValue: 'innovation-lab', description: '')
      string(name: 'WORKSPACE_NAME', defaultValue: 'terraform-simple-instance', description: '')
  }
  environment {
          VAULT_ADDR="http://192.168.1.73:8200"
          ROLE_ID="9641db0a-4b4d-576b-71ab-196106a82271"
          SECRET_ID=credentials("SECRET_ID")

          //BEARER_TOKEN = "ECReMbuw02A2cw.atlasv1.JvPqUHbs6OnEkfzq1d4nyXNLVcvTw4O0IPzz2dg89uTz4aFenR1uv8d5E7sQmOktXNc"
          TF_RUN_ID = "${params.RUN_ID}"
          TF_WORKSPACE_NAME = "${params.WORKSPACE_NAME}"
          TF_ORG_NAME =  "${params.ORGANIZATION}"

    }

    stages {
        stage('declareTokenEnvVar') {
            steps {
                script {
                    // created imperatively, so we can modified & used at later stages
                    env.BEARER_TOKEN = "notatoken" 
                }
            }
        }

        stage('assignToken') {
            steps {
                script {
                    withVault([configuration: configuration, vaultSecrets: secrets]) {
                        env.BEARER_TOKEN = env.api_token
                    }
                }
            }
        }

        stage('printToken1') {
            steps {
                echo "BEARER_TOKEN=${env.BEARER_TOKEN}"
            }
        }

        stage('Get Workspace Id') { 
            steps{ 
                script {
                    env.TF_WORKSPACE_ID =  getWorkspaceId(env.TF_ORG_NAME, env.TF_WORKSPACE_NAME)
                }
                echo "TF_ORG_NAME is ${env.TF_ORG_NAME}"
                echo "TF_WORKSPACE_NAME is ${env.TF_WORKSPACE_NAME}"
                echo "TF_WORKSPACE_ID is ${env.TF_WORKSPACE_ID}"
            }
        }
        
        stage('ListRuns in a Workspace') {
            steps{
                script {
                    listLiveRun(env.TF_WORKSPACE_ID)
                }
            }
        }

        stage('Start Run in a Workspace') {
            steps{
                script {
                    doRun(env.TF_WORKSPACE_ID)
                }
            }
        }

    } //stages
} //pipeline
