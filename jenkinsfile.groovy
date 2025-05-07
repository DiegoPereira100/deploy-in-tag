pipeline {
    agent any

    stages {
        stage('Preparação') {
            steps {
                checkout scm
                bat 'git config --global --add safe.directory %WORKSPACE%'  // Corrige possível erro de permissão
            }
        }

        stage('Verificar Tag') {
            steps {
                script {
                    // Método mais confiável para verificar tags
                    bat 'git fetch --tags --force'
                    def commitId = bat(script: '@git rev-parse HEAD', returnStdout: true).trim()
                    def tags = bat(
                        script: "@git tag --points-at %commitId% 2>nul || echo \"\"",
                        returnStdout: true
                    ).trim().split('\r\n')

                    if (tags && tags[0]) {
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
                bat 'echo "Build simulado - build com sucesso"'
                
            }
        }

        stage('Release') {
            when { 
                expression { 
                    return env.IS_TAG == "true" 
                } 
            }
            steps {
                echo "🚀 Preparando release ${env.TAG_NAME}"
                bat """
                    echo Versão: %TAG_NAME% > version.txt
                    7z a build-${env.TAG_NAME}.zip build\\libs\\* version.txt
                """
            }
        }
    }

    post {
        always {
            echo "📌 Status final: ${currentBuild.currentResult}"
        }
        success {
            script {
                if (env.IS_TAG == "true") {
                    echo "✅ Release ${env.TAG_NAME} concluída!"
                    archiveArtifacts artifacts: "build-${env.TAG_NAME}.zip"
                } else {
                    echo "✅ Build padrão concluído!"
                }
            }
        }
        failure {
            echo "❌ Pipeline falhou no estágio: ${currentBuild.result}"
        }
    }
}