pipeline {
    agent any
    
    environment {
        DOCKER_HUB_CREDENTIALS = 'DockerHub'
        SOURCE_REPO_URL = 'https://github.com/rdnsx/Bedrock-Status.git'
        DOCKER_IMAGE_NAME = 'rdnsx/bedrockstatus'
        TAG_NAME = 'latest'
        SSH_USER = 'root'
        SSH_HOST = '91.107.199.72'
        SSH_PORT = '22'
    stages {
        stage('Checkout') {
            steps {
                git branch: 'main', url: SOURCE_REPO_URL
            }
        }
        
        stage('Build Docker Image') {
            steps {
                script {
                    docker.withRegistry('', DOCKER_HUB_CREDENTIALS) {
                        def dockerImage = docker.build("${DOCKER_IMAGE_NAME}:${TAG_NAME}", ".")
                        dockerImage.push()
                    }
                }
            }
        }
        
        stage('Deploy to Swarm Server') {
            steps {
                script {
                    sshagent(['Swarm00']) {
                        sh "ssh -o StrictHostKeyChecking=no -p ${SSH_PORT} ${SSH_USER}@${SSH_HOST} '"
                            + "mount -a"
                            + "cd /mnt/SSS/DockerData/"
                            + "docker image rm -f ${DOCKER_IMAGE_NAME}:${TAG_NAME};"
                            + "git clone ${SOURCE_REPO_URL};"
                            + "cd Bedrock-Status;"
                            + "docker stack deploy -c docker-compose-swarm.yml Bedrock-Status;'"
                    }
                }
            }
        }
    }
}
