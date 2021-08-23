import com.cloudbees.plugins.credentials.CredentialsScope

def secrets = [
        [path: 'secret1/web01', engineVersion: 1, secretValues: [
                [envVar: 'ip', vaultKey: 'ip'],
                [envVar: 'password', vaultKey: 'password'],
                [envVar: 'username', vaultKey: 'username']]
        ],
        [path: 'secret1/innovation-lab', engineVersion: 1, secretValues: [
                [envVar: 'api_token', vaultKey: 'api_token']]
        ]
]


// optional configuration, if you do not provide this the next higher config
// (e.g. folder or global) will be used
def configuration = [vaultUrl: 'http://192.168.1.73:8200',
        vaultCredentialId: 'vault-token-root']


pipeline {
  agent any
  environment {
          TF_WORKSPACE = "terraform-simple-instance"
          TF_ORGNAME = "innovation-lab"
          VAULT_ADDR="http://192.168.1.73:8200"
          ROLE_ID="9641db0a-4b4d-576b-71ab-196106a82271"
          SECRET_ID=credentials("SECRET_ID")
          VAULT_TOKEN= sh(script: """
              curl -k -s --request POST \
                 --data '{ \"role_id\": "$ROLE_ID", \"secret_id\": \"$SECRET_ID\" }' \
                 "$VAULT_ADDR"/v1/auth/approle/login | jq -r .auth.client_token
              """, returnStdout: true, encoding: 'UTF-8').trim()
  }

  stages {
    stage('check') {
        steps {
            echo "BEARER_TOKEN"
        }
    }
    
    stage('declareTokenEnvVar') {
        steps {
            script {
                env.BEARER_TOKEN = "notatoken" // it can override env variable created imperatively
            }
        }
    }
    
    stage('assignToken') {
      steps {
            withVault([configuration: configuration, vaultSecrets: secrets]) {
               sh 'echo $api_token > /tmp/secret'
            }
        } //steps
    } //stage

    stage('getTokenfromFile') {
        steps {
            script {
                env.BEARER_TOKEN = sh(script:'cat /tmp/secret', returnStdout: true, encoding: 'UTF-8').trim()
            }
        }
    }
    stage('printToken') {
        steps {
            echo "BEARER_TOKEN=${env.BEARER_TOKEN}"
        }
    }

  } //stages
}//pipeline
