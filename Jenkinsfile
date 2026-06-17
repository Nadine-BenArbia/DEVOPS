pipeline {
    agent any

    triggers {
        githubPush()
    }

    tools {
        jdk 'JDK21'
        maven 'maven3'
    }

    environment {
        IMAGE_FULL = "scarletmaster/student-management:${BUILD_NUMBER}"
        KUBECONFIG = "/var/lib/jenkins/.kube/config"
    }

    stages {

        stage('Clone') {
            steps {
                sh '''
                    echo "Cloning repo..."
                    rm -rf DEVOPS || true
                    git clone -b main https://github.com/Nadine-BenArbia/DEVOPS.git
                    cd DEVOPS
                '''
            }
        }

        stage('Build Maven') {
            steps {
                sh '''
                    cd DEVOPS
                    mvn clean package -DskipTests
                '''
            }
        }

        stage('Docker Build & Push') {
            steps {
                withCredentials([
                    usernamePassword(
                        credentialsId: 'dockerhub',
                        usernameVariable: 'USER',
                        passwordVariable: 'PASS'
                    )
                ]) {
                    sh '''
                        cd DEVOPS

                        echo "$PASS" | docker login -u "$USER" --password-stdin

                        docker build -t $IMAGE_FULL .
                        docker push $IMAGE_FULL
                    '''
                }
            }
        }

        stage('Deploy') {
            steps {
                sh '''
                    export KUBECONFIG=/var/lib/jenkins/.kube/config

                    kubectl set image deployment/student-management \
                        student-management=$IMAGE_FULL

                    kubectl rollout status deployment/student-management
                '''
            }
        }

        stage('Verify') {
            steps {
                sh '''
                    kubectl get pods
                    kubectl get svc
                '''
            }
        }
    }
}
