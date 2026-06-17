pipeline {
    agent any
    
    triggers {
        githubPush()
    }
    
    environment {
        DOCKER_REGISTRY = 'docker.io'
        DOCKER_USERNAME = 'scarletmaster'  // Your Docker Hub username
        K8S_NAMESPACE = 'devops'
        MAVEN_OPTS = '-Dmaven.test.skip=true'
        // Use your existing credential ID
        DOCKER_CREDENTIALS_ID = 'dockerhub'
    }
    
    stages {
        stage('Checkout') {
            steps {
                echo 'Checking out source code...'
                checkout scm
                script {
                    env.GIT_COMMIT = sh(script: 'git rev-parse --short HEAD', returnStdout: true).trim()
                    env.BUILD_TIMESTAMP = sh(script: 'date +%Y%m%d-%H%M%S', returnStdout: true).trim()
                }
            }
        }
        
        stage('Build Backend') {
            steps {
                echo 'Building Backend (Spring Boot)...'
                sh 'mvn clean package -DskipTests'
            }
            post {
                success {
                    archiveArtifacts artifacts: 'target/*.jar', fingerprint: true
                }
            }
        }
        
        stage('Build Docker Images') {
            steps {
                script {
                    echo 'Building Docker images...'
                    
                    sh """
                        docker build -t ${DOCKER_USERNAME}/backend:latest .
                        docker tag ${DOCKER_USERNAME}/backend:latest ${DOCKER_USERNAME}/backend:${BUILD_NUMBER}
                        docker tag ${DOCKER_USERNAME}/backend:latest ${DOCKER_USERNAME}/backend:${GIT_COMMIT}
                        docker tag ${DOCKER_USERNAME}/backend:latest ${DOCKER_USERNAME}/backend:${BUILD_TIMESTAMP}
                    """
                }
            }
        }
        
        stage('Push Docker Images') {
            steps {
                script {
                    echo 'Pushing Docker images to Docker Hub...'
                    
                    // Use the existing 'dockerhub' credentials from Jenkins
                    withCredentials([usernamePassword(
                        credentialsId: DOCKER_CREDENTIALS_ID,
                        usernameVariable: 'DOCKER_USER',
                        passwordVariable: 'DOCKER_PASS'
                    )]) {
                        sh """
                            echo "Logging in to Docker Hub..."
                            echo "${DOCKER_PASS}" | docker login -u "${DOCKER_USER}" --password-stdin
                            
                            echo "Pushing images..."
                            docker push ${DOCKER_USERNAME}/backend:latest
                            docker push ${DOCKER_USERNAME}/backend:${BUILD_NUMBER}
                            docker push ${DOCKER_USERNAME}/backend:${GIT_COMMIT}
                            docker push ${DOCKER_USERNAME}/backend:${BUILD_TIMESTAMP}
                            
                            echo "Logging out..."
                            docker logout
                        """
                    }
                }
            }
        }
        
        stage('Deploy to Kubernetes') {
            steps {
                script {
                    echo 'Deploying to Kubernetes...'
                    
                    // Use the kubeconfig credential from Jenkins
                    withKubeConfig([credentialsId: 'kubeconfig-secret']) {
                        sh """
                            # Ensure namespace exists
                            kubectl create namespace ${K8S_NAMESPACE} --dry-run=client -o yaml | kubectl apply -f -
                            
                            # Update image tag in deployment files
                            sed -i 's|image:.*backend.*|image: ${DOCKER_USERNAME}/backend:${BUILD_NUMBER}|g' k8s/backend.yaml
                            
                            # Apply all resources
                            kubectl apply -f k8s/mysql.yaml
                            kubectl apply -f k8s/backend.yaml
                            kubectl apply -f k8s/frontend.yaml
                            
                            # Wait for deployments
                            echo "Waiting for MySQL..."
                            kubectl rollout status deployment/mysql -n ${K8S_NAMESPACE} --timeout=180s || echo "MySQL deployment timed out"
                            
                            echo "Waiting for Backend..."
                            kubectl rollout status deployment/backend -n ${K8S_NAMESPACE} --timeout=300s || echo "Backend deployment timed out"
                            
                            echo "✅ All deployments completed!"
                        """
                    }
                }
            }
        }
        
        stage('Verify Deployment') {
            steps {
                script {
                    echo 'Verifying deployment...'
                    
                    withKubeConfig([credentialsId: 'kubeconfig-secret']) {
                        sh """
                            echo "=== All Pods ==="
                            kubectl get pods -n ${K8S_NAMESPACE}
                            
                            echo "=== All Services ==="
                            kubectl get svc -n ${K8S_NAMESPACE}
                            
                            echo "=== Deployment Status ==="
                            kubectl get deployments -n ${K8S_NAMESPACE}
                        """
                    }
                    
                    // Get service URLs
                    def frontendUrl = sh(
                        script: "minikube service frontend-service -n ${K8S_NAMESPACE} --url 2>/dev/null || echo 'URL not available'",
                        returnStdout: true
                    ).trim()
                    
                    echo "Frontend Application URL: ${frontendUrl}"
                }
            }
        }
    }
    
    post {
        success {
            echo '🎉 Pipeline executed successfully!'
            // Optional: Send success notification
            // emailext (
            //     subject: "✅ Pipeline Success: ${env.JOB_NAME} - ${env.BUILD_NUMBER}",
            //     body: "The pipeline has been completed successfully.\n\nURL: ${env.BUILD_URL}",
            //     to: 'team@email.com'
            // )
        }
        failure {
            echo '❌ Pipeline execution failed!'
            // Optional: Send failure notification
            // emailext (
            //     subject: "❌ Pipeline Failed: ${env.JOB_NAME} - ${env.BUILD_NUMBER}",
            //     body: "The pipeline has failed.\n\nURL: ${env.BUILD_URL}",
            //     to: 'team@email.com'
            // )
        }
        always {
            cleanWs()
        }
    }
}
