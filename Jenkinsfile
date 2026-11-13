pipeline {
    agent any

    tools {
        jdk 'JDK21'
    }

    environment {
        SONAR_PROJECT_KEY = 'ambientar-proyecto-prueba'
        SONAR_PROJECT_NAME = 'Ambientar-Proyecto-prueba'
    }

    options {
        timestamps()
        disableConcurrentBuilds()
    }

    stages {

        stage('Clonar repositorio') {
            steps {
                git branch: 'develop',
                    url: 'https://github.com/Aplicaciones-Web-Progresivas-GIDS6102/Ambientar-Proyecto-prueba.git'
            }
        }

        stage('Verificar entorno') {
            steps {
                sh '''
                    java -version
                    chmod +x gradlew
                    ./gradlew --version
                '''
            }
        }

        stage('Compilar y ejecutar pruebas') {
            steps {
                sh './gradlew clean test jacocoTestReport jacocoTestCoverageVerification'
            }
            post {
                always {
                    junit allowEmptyResults: true,
                        testResults: '**/build/test-results/test/*.xml'

                    archiveArtifacts allowEmptyArchive: true,
                        artifacts: '**/build/reports/**'
                }
            }
        }

        stage('Analisis SonarQube') {
            steps {
                withSonarQubeEnv('SonarQube') {
                    sh '''
                        ./gradlew sonar \
                          -Dsonar.projectKey=ambientar-proyecto-prueba \
                          -Dsonar.projectName=Ambientar-Proyecto-prueba \
                          -Dsonar.coverage.jacoco.xmlReportPaths=build/reports/jacoco/test/jacocoTestReport.xml \
                          -Dsonar.exclusions="**/App.java,**/config/**,**/model/**,**/entity/**,**/enums/**,**/dto/**,**/mapper/**,**/client/**,**/exception/**,**/controller/**" \
                          -Dsonar.coverage.exclusions="**/App.java,**/config/**,**/controller/**,**/dto/**,**/entity/**,**/enums/**,**/exception/**,**/mapper/**,**/model/**,**/client/**,**/repositorys/**,**/security/JwtAuthenticationFilter.java,**/scheduler/**"
                    '''
                }
            }
        }

        stage('Quality Gate') {
            steps {
                timeout(time: 5, unit: 'MINUTES') {
                    waitForQualityGate abortPipeline: true
                }
            }
        }
    }

    post {
        success {
            echo 'Proyecto compilado y analizado correctamente en SonarQube.'
        }

        failure {
            echo 'Error en el pipeline. Revisar Console Output.'
        }

        always {
            echo 'Finalizo la ejecucion del pipeline.'
        }
    }
}
