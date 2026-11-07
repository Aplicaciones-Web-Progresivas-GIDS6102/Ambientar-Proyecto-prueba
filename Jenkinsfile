
pipeline {
    agent any

    tools {
        jdk 'JDK21'
    }

    options {
        timestamps()
        disableConcurrentBuilds()
    }

    stages {

        stage('Descargar codigo') {
            steps {
                echo 'Descargando proyecto desde GitHub'
                checkout scm
            }
        }

        stage('Verificar Java y Gradle') {
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
                sh './gradlew clean test jacocoTestReport'
            }
            post {
                always {
                    junit allowEmptyResults: true,
                          testResults: '**/build/test-results/test/*.xml'

                    archiveArtifacts allowEmptyArchive: true,
                        artifacts: 'build/reports/tests/test/**,build/reports/jacoco/test/**'
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
                          -Dsonar.coverage.jacoco.xmlReportPaths=build/reports/jacoco/test/jacocoTestReport.xml
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
            echo 'Pipeline terminado correctamente.'
        }
        failure {
            echo 'El pipeline fallo. Revisar la consola de Jenkins.'
        }
    }
}
