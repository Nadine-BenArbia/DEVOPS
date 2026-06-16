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
                git branch: 'main', url: 'https://github.com/Nadine-BenArbia/DEVOPS.git'
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

        stage('Docker Build & Push') {
            steps {
                withCredentials([usernamePassword(credentialsId: 'dockerhub',
                                                 usernameVariable: 'DOCKER_HUB_USER',
                                                 passwordVariable: 'DOCKER_HUB_PASSWORD')]) {
                    sh "echo \$DOCKER_HUB_PASSWORD | docker login -u \$DOCKER_HUB_USER --password-stdin"
                    sh "docker build -t ${DOCKER_USER}/${IMAGE_NAME}:${IMAGE_TAG} ."
                    sh "docker push ${DOCKER_USER}/${IMAGE_NAME}:${IMAGE_TAG}"
                }
            }
        }

        stage('Deploy to Kubernetes') {
            steps {
                sh 'kubectl apply -f k8s/'
            }
        }

        stage('Verify Deployment') {
            steps {
                sh 'kubectl get pods -n default'
                sh 'kubectl get svc -n default'
            }
        }
    }
}
