pipeline {
    agent any
    stages {
        stage('Checkout') {
            steps {
                git branch: 'main', url: 'https://github.com/Nadine-BenArbia/DEVOPS.git'
            }
        }
        stage('Build Docker Image') {
            steps {
                script {
                    docker.build("scarletmaster/alpine:1.0.0")
                }
            }
        }
        stage('Run Container') {
            steps {
                script {
                    docker.image("scarletmaster/alpine:1.0.0").run('--rm java -version')
                }
            }
        }
    }
}

