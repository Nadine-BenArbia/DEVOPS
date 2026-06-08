pipeline {

    agent any

    tools {
        jdk 'JDK21'
        maven 'maven3'
    }

    environment {
        DOCKER_USER     = 'scarletmaster'
        IMAGE_NAME      = 'student-management'
        IMAGE_TAG       = '1.0.0'
        DOCKER_BUILDKIT = '0'
    }

    stages {

        stage('Clone') {
            steps {
                git branch: 'main', url: 'https://github.com/islembenchaabene/jenkins-project.git'
            }
        }

        stage('Clean') {
            steps {
                sh 'mvn clean'
            }
        }

        stage('Build') {
            steps {
                sh 'mvn package -DskipTests'
            }
        }

        stage('MVN SONARQUBE') {
            steps {
                // Run SonarQube analysis using the configured server and scanner
                withSonarQubeEnv('SonarQube') {
                    sh 'mvn sonar:sonar'
                }
            }
        }

        stage('Docker Build & Push') {
            steps {
                withCredentials([usernamePassword(credentialsId: 'dockerhub',
                                                 usernameVariable: 'DOCKER_HUB_USER',
                                                 passwordVariable: 'DOCKER_HUB_PASSWORD')]) {
                    // 1. Login to Docker Hub
                    sh "echo \$DOCKER_HUB_PASSWORD | docker login -u \$DOCKER_HUB_USER --password-stdin"

                    // 2. Build Docker image
                    sh "docker build -t ${DOCKER_USER}/${IMAGE_NAME}:${IMAGE_TAG} ."

                    // 3. Push Docker image
                    sh "docker push ${DOCKER_USER}/${IMAGE_NAME}:${IMAGE_TAG}"
                }
            }
        }
    }
}
