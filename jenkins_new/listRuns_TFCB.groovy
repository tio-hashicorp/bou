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

def listRun(workspaceid) {
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
    result.each {
        println "$it.id : $it.attributes.status"
    }
    //println ("Results: " + result)
    def count = data.meta.pagination.'total-count'
    return count

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
          AWS_ACCESS_KEY_ID = ""
          AWS_SECRET_ACCESS_KEY = ""
          AWS_REGION = "ap-southeast-1"

          VAULT_ADDR="http://192.168.1.73:8200"
          ROLE_ID="9641db0a-4b4d-576b-71ab-196106a82271"
          SECRET_ID=credentials("SECRET_ID")

          //BEARER_TOKEN = ""
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
                echo "AWS_REGION is $AWS_REGION"
            }
        }
        
        stage('ListRuns in a Workspace') {
            steps{
                script {
                    listRun(env.TF_WORKSPACE_ID)
                }
            }
        }
    } //stages
} //pipeline
