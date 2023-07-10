pipeline {
    agent any

    stages {
        stage('Checkout') {
            steps {
                checkout scm
            }
        }
        stage('Build') {
            steps {
                sh './gradlew build'
            }
        }
        stage('Test') {
            steps {
                sh './gradlew test'
            }
        }
        stage('Deploy') {
            when {
                branch 'master'
            }
            steps {
                sh './deploy.sh'
            }
        }
    }

    post {
        always {
            junit 'build/test-results/**/*.xml'
        }
        success {
            slackSend channel: '#builds', color: 'good', message: 'Build successful!'
        }
        failure {
            slackSend channel: '#builds', color: 'danger', message: 'Build failed!'
        }
    }
}
