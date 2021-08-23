import hudson.util.Secret
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
  stages {
    stage('loginUserpass') {

      steps {
            sh """
                #!/bin/bash -x
                echo "Retreive sensitive info from Vault ..."
                """
            withVault([configuration: configuration, vaultSecrets: secrets]) {
                sh 'echo $ip > /tmp/secret'
                sh 'echo $username >> /tmp/secret'
                sh 'echo $password >> /tmp/secret'
                sh 'echo $api_token >> /tmp/secret'
            }
            
        } //steps
    } //stage
  } //stages
}//pipeline
