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

pipeline {
  agent any
  parameters {
      string(name: 'ORGANIZATION', defaultValue: 'innovation-lab', description: '')
      string(name: 'WORKSPACE_NAME', defaultValue: 'terraform-simple-instance', description: '')
      string(name: 'RUN_ID', defaultValue: 'run-mipYPheDecehVYGM', description: '')
  }
  environment {
          AWS_ACCESS_KEY_ID = ""
          AWS_SECRET_ACCESS_KEY = ""
          AWS_REGION = "ap-southeast-1"

          VAULT_ADDR="http://192.168.1.73:8200"
          ROLE_ID="9641db0a-4b4d-576b-71ab-196106a82271"
          SECRET_ID=credentials("SECRET_ID")

          BEARER_TOKEN = ""
          TF_RUN_ID = "${params.RUN_ID}"
          TF_WORKSPACE_NAME = "${params.WORKSPACE_NAME}"
          TF_ORG_NAME =  "${params.ORGANIZATION}"

    }

    stages {
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
    } //stages
} //pipeline
