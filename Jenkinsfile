pipeline {
    agent any

    stages {

        stage('Checkout') {
            steps {
                git branch: 'main', url: 'https://github.com/Nadine-BenArbia/DEVOPS.git'
            }
        }

        stage('Docker Info') {
            steps {
                sh 'docker --version'
            }
        }

        stage('Build Image') {
            steps {
                script {
                    docker.build("test-image:1.0", ".")
                }
            }
        }

        stage('Run Container') {
            steps {
                script {
                    docker.image("test-image:1.0").inside {
                        sh 'echo "Hello from container"'
                    }
                }
            }
        }
    }
}
