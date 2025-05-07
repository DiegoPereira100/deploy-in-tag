pipeline {
    agent any

    stages {
        stage('Clone Repositório') {
            steps {
                checkout scm
            }
        }

        stage('Verificar Tag') {
            steps {
                script {
                    bat 'git fetch --tags --force'
                    
                    def tags = bat(
                        script: '@git tag --points-at HEAD 2>nul || echo ""',
                        returnStdout: true
                    ).trim().split('\r\n')
                    
                    if (tags.size() > 0 && tags[0] != '') {
                        echo "✅ Build disparado pela TAG: ${tags[0]}"
                        env.IS_TAG = "true"
                        env.TAG_NAME = tags[0]
                    } else {
                        echo "ℹ️ Build normal (não é uma TAG)"
                        env.IS_TAG = "false"
                    }
                }
            }
          }  

        stage('Build') {
            steps {
                echo "🛠️ Executando build padrão..."
                bat 'npm install'
            }
        }

        stage('Build de Release') {
            when { 
                expression { 
                    return env.IS_TAG == "true" && env.TAG_NAME ==~ /v\d+\.\d+\.\d+/ 
                } 
            }
            steps {
                echo "🚀 Build de Release para TAG: ${env.TAG_NAME}"
                bat 'echo "Simulando deploy da versão ${env.TAG_NAME}"'
            }
        }

        stage('Deploy em Produção') {
            when { 
                expression { 
                    return env.IS_TAG == "true" 
                } 
            }
            steps {
                echo "🚀 Deploy da versão ${env.TAG_NAME} em produção..."
            }
        }
    }

    post {
        success {
            script {
                if (env.IS_TAG == "true") {
                    echo "✅ Release ${env.TAG_NAME} publicada com sucesso!"
                } else {
                    echo "✅ Build padrão concluído!"
                }
            }
        }
        failure {
            echo "❌ Falha no pipeline!"
        }
    }
}