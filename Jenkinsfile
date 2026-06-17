pipeline {
    agent any

    // Optional fallback (you can remove this if you use GitHub webhook)
    triggers {
        pollSCM('H/5 * * * *')
    }

    tools {
        jdk 'JDK21'
        maven 'maven3'
    }

    environment {
        DOCKER_USER = 'scarletmaster'
        IMAGE_NAME  = 'student-management'
        IMAGE_TAG   = "${BUILD_NUMBER}"
        IMAGE_FULL  = "scarletmaster/student-management:${BUILD_NUMBER}"
        KUBECONFIG  = '/var/lib/jenkins/.kube/config'
    }

    stages {

        stage('Clone') {
            steps {
                git branch: 'main',
                    url: 'https://github.com/Nadine-BenArbia/DEVOPS.git'
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
                withCredentials([
                    usernamePassword(
                        credentialsId: 'dockerhub',
                        usernameVariable: 'DOCKER_HUB_USER',
                        passwordVariable: 'DOCKER_HUB_PASSWORD'
                    )
                ]) {
                    sh '''
                        echo "$DOCKER_HUB_PASSWORD" | docker login -u "$DOCKER_HUB_USER" --password-stdin

                        docker build -t $IMAGE_FULL .

                        docker push $IMAGE_FULL
                    '''
                }
            }
        }

        stage('Deploy to Kubernetes') {
            steps {
                sh '''
                    kubectl get nodes

                    kubectl set image deployment/student-management \
                    student-management=$IMAGE_FULL

                    kubectl rollout status deployment/student-management
                '''
            }
        }

        stage('Verify Deployment') {
            steps {
                sh '''
                    kubectl get deployments
                    kubectl get pods
                    kubectl get svc
                '''
            }
        }
    }

    post {
        success {
            echo "Deployment successful: ${IMAGE_FULL}"
        }

        failure {
            echo "Deployment failed"
        }
    }
}
