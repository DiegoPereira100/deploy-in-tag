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
                    def tag = bat(
                        script: '@git describe --exact-match --tags HEAD || echo ""',
                        returnStdout: true
                    ).trim()

                    if (tag && tag != "") {
                        echo "✅ Build disparado pela TAG: ${tag}"
                        env.IS_TAG = "true"
                        env.TAG_NAME = tag
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
                    return env.IS_TAG == "true" 
                } 
            }
            steps {
                echo "🚀 Build de Release para TAG: ${env.TAG_NAME}"
                bat 'mvn clean deploy -DskipTests'
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