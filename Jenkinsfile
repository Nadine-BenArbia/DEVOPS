pipeline {
    agent any

    triggers {
        githubPush()
    }

    environment {
        DOCKER_USERNAME       = 'scarletmaster'
        K8S_NAMESPACE         = 'devops'
        DOCKER_CREDENTIALS_ID = 'dockerhub'
    }

    stages {
        stage('Checkout') {
            steps {
                checkout scm
                script {
                    env.GIT_COMMIT = sh(script: 'git rev-parse --short HEAD', returnStdout: true).trim()
                    env.IMAGE_TAG  = "${BUILD_NUMBER}-${GIT_COMMIT}"
                }
            }
        }

        stage('Build Backend') {
            steps {
                sh 'mvn clean package -DskipTests'
            }
            post {
                success {
                    archiveArtifacts artifacts: 'target/*.jar', fingerprint: true
                }
            }
        }

        stage('Build Docker Image') {
            steps {
                sh """
                    docker build -t ${DOCKER_USERNAME}/backend:latest \
                                 -t ${DOCKER_USERNAME}/backend:${IMAGE_TAG} .
                """
            }
        }

        stage('Push Docker Image') {
            steps {
                withCredentials([usernamePassword(
                    credentialsId: DOCKER_CREDENTIALS_ID,
                    usernameVariable: 'DOCKER_USER',
                    passwordVariable: 'DOCKER_PASS'
                )]) {
                    sh """
                        echo "\${DOCKER_PASS}" | docker login -u "\${DOCKER_USER}" --password-stdin
                        docker push ${DOCKER_USERNAME}/backend:latest
                        docker push ${DOCKER_USERNAME}/backend:${IMAGE_TAG}
                        docker logout
                    """
                }
            }
        }

        stage('Deploy to Kubernetes') {
            steps {
                sh """
                    kubectl create namespace ${K8S_NAMESPACE} --dry-run=client -o yaml | kubectl apply -f -

                    # Deploy MySQL first
                    kubectl apply -f k8s/mysql-deployment.yaml
                    kubectl rollout status deployment/mysql -n ${K8S_NAMESPACE} --timeout=180s

                    # Deploy Backend with dynamic image tag
                    sed -i 's|image:.*backend.*|image: ${DOCKER_USERNAME}/backend:${IMAGE_TAG}|g' k8s/backend-deployment.yaml
                    kubectl apply -f k8s/backend-deployment.yaml
                    kubectl rollout status deployment/backend -n ${K8S_NAMESPACE} --timeout=300s || true
                """
            }
        }

        stage('Verify') {
            steps {
                sh """
                    echo "=== Pods ==="
                    kubectl get pods -n ${K8S_NAMESPACE}
                    echo "=== Services ==="
                    kubectl get svc -n ${K8S_NAMESPACE}
                    echo "=== Deployments ==="
                    kubectl get deployments -n ${K8S_NAMESPACE}
                    echo "=== Backend URL ==="
                    minikube service backend-service -n ${K8S_NAMESPACE} --url 2>/dev/null || true
                """
            }
        }
    }

    post {
        success { echo '✅ Pipeline completed successfully test!' }
        failure {
            script {
                sh """
                    kubectl get all -n ${K8S_NAMESPACE} 2>/dev/null || true
                    kubectl logs -l app=backend -n ${K8S_NAMESPACE} --tail=50 2>/dev/null || true
                    kubectl logs -l app=mysql   -n ${K8S_NAMESPACE} --tail=50 2>/dev/null || true
                """
            }
        }
        always { cleanWs() }
    }
}