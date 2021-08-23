//import com.cloudbees.plugins.credentials.CredentialsScope

def configuration = [vaultUrl: 'http://192.168.1.73:8200',
        vaultCredentialId: 'vault-token-root']

def secrets = [
        [path: 'secret1/innovation-lab', engineVersion: 1, secretValues: [
                [envVar: 'api_token', vaultKey: 'api_token']]
        ]
]

pipeline {
  agent any
  environment {
          TF_WORKSPACE = "terraform-simple-instance"
          TF_ORGNAME = "innovation-lab"
  }

  stages {
    stage('declareTokenEnvVar') {
        steps {
            script {
                env.BEARER_TOKEN = "notatoken" // created imperatively so can be modified & used at later stages
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

  } //stages
}
