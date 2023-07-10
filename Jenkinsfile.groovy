pipeline {
    agent any
    environment {
        AWS_REGION = 'us-west-2'
        S3_BUCKET = 'my-s3-bucket'
        LAMBDA_FUNCTION = 'my-lambda-function'
        ECS_CLUSTER = 'my-ecs-cluster'
        ECS_SERVICE = 'my-ecs-service'
    }
    stages {
        stage('Source') {
            steps {
                // Checkout source code from GitHub
                git 'https://github.com/marize98/DemoACL.git'
            }
        }
        stage('Build') {
            steps {
                // Build the project using Maven
                sh 'mvn clean package'
            }
        }
        stage('Test') {
            steps {
                // Run tests using JUnit
                sh 'mvn test'
            }
        }
        stage('Deploy to S3') {
            steps {
                // Upload the JAR file to S3
                sh "aws s3 cp target/my-project.jar s3://${S3_BUCKET}/"
            }
        }
        stage('Deploy to Lambda') {
            steps {
                // Update the Lambda function with the new code
                sh "aws lambda update-function-code --function-name ${LAMBDA_FUNCTION} --s3-bucket ${S3_BUCKET} --s3-key my-project.jar"
            }
        }
        stage('Deploy to ECS') {
            steps {
                // Build the Docker image
                sh 'docker build -t my-image .'
                // Push the Docker image to ECR
                withCredentials([[$class: 'AmazonWebServicesCredentialsBinding', accessKeyVariable: 'AWS_ACCESS_KEY_ID', credentialsId: 'aws', secretKeyVariable: 'AWS_SECRET_ACCESS_KEY']]) {
                    sh "aws ecr get-login-password --region ${AWS_REGION} | docker login --username AWS --password-stdin ${ECR_REGISTRY}"
                    sh "docker tag my-image ${ECR_REGISTRY}/my-image"
                    sh "docker push ${ECR_REGISTRY}/my-image"
                }
                // Update the task definition with the new image
                sh "aws ecs register-task-definition --family my-task --container-definitions '[{\"name\":\"my-container\",\"image\":\"${ECR_REGISTRY}/my-image\"}]'"
                // Update the service with the new task definition
                sh "aws ecs update-service --cluster ${ECS_CLUSTER} --service ${ECS_SERVICE} --task-definition my-task"
            }
        }
    }
}
