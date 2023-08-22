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
        WEBSITE_URL = 'https://status.pietscraft.net'
        WAIT_TIME = 30
    }
    
    stages {
        stage('Checkout') {
            steps {
                git branch: 'main', url: env.SOURCE_REPO_URL
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
                        sh """
                            ssh -o StrictHostKeyChecking=no -p ${SSH_PORT} ${SSH_USER}@${SSH_HOST} '
                            mount -a &&
                            cd /mnt/SSS/DockerCompose/ &&
                            rm -rf Bedrock-Status/ &&
                            mkdir Bedrock-Status/ &&
                            cd Bedrock-Status/ &&
                            wget https://raw.githubusercontent.com/rdnsx/Bedrock-Status/main/docker-compose-swarm.yml &&
                            docker stack deploy -c docker-compose-swarm.yml Bedrock-Status;'
                            """
                    }
                }
            }
        }

        stage('Check Website Status') {
            steps {
                script {
                    def startTime = System.currentTimeMillis()

                    echo "Waiting for ${WAIT_TIME} seconds before checking website status..."
                    sleep WAIT_TIME

                    def response = sh(script: "curl -s ${WEBSITE_URL}", returnStdout: true).trim()

                    if (response.contains('Gamemode')) {
                        echo "Website is up and contains 'Gamemode'."
                    } else {
                        error "Website is not responding properly or does not contain 'Gamemode'."
                    }

                    def endTime = System.currentTimeMillis()
                    def elapsedTime = (endTime - startTime) / 1000.0

                    echo "Total time elapsed: ${elapsedTime} seconds"
                }
            }
        }
    }
}
