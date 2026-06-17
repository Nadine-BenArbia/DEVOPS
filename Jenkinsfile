pipeline {
    agent any
     triggers {
        githubPush()
    }
    environment {
        DOCKER_REGISTRY = 'docker.io'
        DOCKER_USERNAME = 'nadinebenarbia'  // Your Docker Hub username
        K8S_NAMESPACE = 'devops'
        // You can split into separate builds if needed
    }
    
    stages {
        stage('Checkout') {
            steps {
                echo 'Checking out source code...'
                checkout scm
                script {
                    env.GIT_COMMIT = sh(script: 'git rev-parse --short HEAD', returnStdout: true).trim()
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
                    
                    // Build Backend
                    sh """
                        docker build -t ${DOCKER_USERNAME}/backend:latest .
                        docker tag ${DOCKER_USERNAME}/backend:latest ${DOCKER_USERNAME}/backend:${BUILD_NUMBER}
                        docker tag ${DOCKER_USERNAME}/backend:latest ${DOCKER_USERNAME}/backend:${GIT_COMMIT}
                    """
                    
                    // Build Frontend (assuming you have a separate Dockerfile in /frontend)
                    // Uncomment if you have frontend code in the same repo
                    // sh """
                    //     cd frontend
                    //     docker build -t ${DOCKER_USERNAME}/frontend:latest .
                    //     docker tag ${DOCKER_USERNAME}/frontend:latest ${DOCKER_USERNAME}/frontend:${BUILD_NUMBER}
                    //     docker tag ${DOCKER_USERNAME}/frontend:latest ${DOCKER_USERNAME}/frontend:${GIT_COMMIT}
                    //     cd ..
                    // """
                }
            }
        }
        
        stage('Push Docker Images') {
            steps {
                script {
                    echo 'Pushing Docker images...'
                    
                    // Push Backend
                    sh """
                        docker push ${DOCKER_USERNAME}/backend:latest
                        docker push ${DOCKER_USERNAME}/backend:${BUILD_NUMBER}
                        docker push ${DOCKER_USERNAME}/backend:${GIT_COMMIT}
                    """
                    
                    // Push Frontend
                    // Uncomment if you have frontend Docker image
                    // sh """
                    //     docker push ${DOCKER_USERNAME}/frontend:latest
                    //     docker push ${DOCKER_USERNAME}/frontend:${BUILD_NUMBER}
                    //     docker push ${DOCKER_USERNAME}/frontend:${GIT_COMMIT}
                    // """
                }
            }
        }
        
        stage('Deploy to Kubernetes') {
            steps {
                script {
                    echo 'Deploying to Kubernetes...'
                    
                    // Update image tags in deployment files
                    sh """
                        sed -i 's|image:.*backend.*|image: ${DOCKER_USERNAME}/backend:${BUILD_NUMBER}|g' k8s/backend.yaml
                        sed -i 's|image:.*frontend.*|image: ${DOCKER_USERNAME}/frontend:${BUILD_NUMBER}|g' k8s/frontend.yaml
                    """
                    
                    // Deploy all resources
                    sh """
                        kubectl apply -f k8s/mysql.yaml
                        kubectl apply -f k8s/backend.yaml
                        kubectl apply -f k8s/frontend.yaml
                    """
                    
                    // Wait for deployments
                    sh """
                        kubectl rollout status deployment/mysql -n ${K8S_NAMESPACE} --timeout=180s
                        kubectl rollout status deployment/backend -n ${K8S_NAMESPACE} --timeout=300s
                        kubectl rollout status deployment/frontend -n ${K8S_NAMESPACE} --timeout=180s
                    """
                    
                    echo '✅ All deployments completed!'
                }
            }
        }
        
        stage('Verify Deployment') {
            steps {
                script {
                    echo 'Verifying deployment...'
                    
                    // Show all resources
                    sh """
                        echo "=== All Pods ==="
                        kubectl get pods -n ${K8S_NAMESPACE}
                        echo "=== All Services ==="
                        kubectl get svc -n ${K8S_NAMESPACE}
                    """
                    
                    // Get frontend URL
                    def frontendUrl = sh(
                        script: "minikube service frontend-service -n ${K8S_NAMESPACE} --url 2>/dev/null || echo 'URL not available'",
                        returnStdout: true
                    ).trim()
                    
                    echo "Frontend Application URL: ${frontendUrl}"
                    
                    // Test backend health
                    sh """
                        echo "Testing backend health..."
                        kubectl run test-pod --image=busybox -n ${K8S_NAMESPACE} --rm -it --restart=Never -- wget -O- backend-service:8080/actuator/health || echo "Health check not available"
                    """
                }
            }
        }
    }
    
    post {
        success {
            echo '🎉 Full pipeline executed successfully!'
            emailext (
                subject: "✅ Pipeline Success: ${env.JOB_NAME} - ${env.BUILD_NUMBER}",
                body: "The pipeline has been completed successfully.\n\nURL: ${env.BUILD_URL}",
                to: 'team@email.com'
            )
        }
        failure {
            echo '❌ Pipeline execution failed!'
            emailext (
                subject: "❌ Pipeline Failed: ${env.JOB_NAME} - ${env.BUILD_NUMBER}",
                body: "The pipeline has failed.\n\nURL: ${env.BUILD_URL}",
                to: 'team@email.com'
            )
        }
        always {
            cleanWs()
        }
    }
}
