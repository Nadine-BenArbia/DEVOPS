pipeline {
    agent any
    
    triggers {
        githubPush()
    }
    
    environment {
        DOCKER_USERNAME = 'scarletmaster'
        K8S_NAMESPACE = 'devops'
        MAVEN_OPTS = '-Dmaven.test.skip=true'
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
                    
                    sh """
                        # Ensure namespace exists
                        kubectl create namespace ${K8S_NAMESPACE} --dry-run=client -o yaml | kubectl apply -f -
                        
                        # Update image tag in backend deployment file
                        echo "Updating backend image tag..."
                        sed -i 's|image:.*backend.*|image: ${DOCKER_USERNAME}/backend:${BUILD_NUMBER}|g' k8s/backend-deployment.yaml
                        
                        # Show the updated image line for verification
                        echo "Updated image line:"
                        grep "image:" k8s/backend-deployment.yaml
                        
                        # Deploy MySQL
                        echo "Deploying MySQL..."
                        kubectl apply -f k8s/mysql-deployment.yaml
                        
                        # Deploy Backend
                        echo "Deploying Backend..."
                        kubectl apply -f k8s/backend-deployment.yaml
                        
                        # Deploy Frontend
                        echo "Deploying Frontend..."
                        kubectl apply -f k8s/frontend-deployment.yaml
                        
                        # Wait for deployments
                        echo "Waiting for MySQL..."
                        kubectl rollout status deployment/mysql -n ${K8S_NAMESPACE} --timeout=180s || echo "MySQL deployment timed out"
                        
                        echo "Waiting for Backend..."
                        kubectl rollout status deployment/backend -n ${K8S_NAMESPACE} --timeout=300s || echo "Backend deployment timed out"
                        
                        echo "Waiting for Frontend..."
                        kubectl rollout status deployment/frontend -n ${K8S_NAMESPACE} --timeout=180s || echo "Frontend deployment timed out"
                        
                        echo "✅ All deployments completed!"
                    """
                }
            }
        }
        
        stage('Verify Deployment') {
            steps {
                script {
                    echo 'Verifying deployment...'
                    
                    sh """
                        echo "=== All Pods in ${K8S_NAMESPACE} ==="
                        kubectl get pods -n ${K8S_NAMESPACE}
                        
                        echo "=== All Services in ${K8S_NAMESPACE} ==="
                        kubectl get svc -n ${K8S_NAMESPACE}
                        
                        echo "=== All Deployments in ${K8S_NAMESPACE} ==="
                        kubectl get deployments -n ${K8S_NAMESPACE}
                        
                        echo "=== Persistent Volumes ==="
                        kubectl get pv
                        kubectl get pvc -n ${K8S_NAMESPACE}
                    """
                    
                    // Try to get service URLs
                    script {
                        def services = ['frontend-service', 'backend-service', 'mysql-service']
                        for (svc in services) {
                            def url = sh(
                                script: "minikube service ${svc} -n ${K8S_NAMESPACE} --url 2>/dev/null || echo ''",
                                returnStdout: true
                            ).trim()
                            if (url) {
                                echo "${svc} URL: ${url}"
                            }
                        }
                    }
                }
            }
        }
    }
    
    post {
        success {
            echo '🎉 Pipeline executed successfully!'
        }
        failure {
            echo '❌ Pipeline execution failed!'
            // Show debug info on failure
            sh """
                echo "=== Debug Info ==="
                echo "Kubernetes resources:"
                kubectl get all -n ${K8S_NAMESPACE} 2>/dev/null || echo "No resources found"
                echo "Pod logs (if any):"
                kubectl logs -l app=backend -n ${K8S_NAMESPACE} --tail=50 2>/dev/null || echo "No backend logs"
                kubectl logs -l app=mysql -n ${K8S_NAMESPACE} --tail=50 2>/dev/null || echo "No MySQL logs"
            """
        }
        always {
            cleanWs()
        }
    }
}
