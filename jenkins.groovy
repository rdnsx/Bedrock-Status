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
                    def buildNumber = env.BUILD_NUMBER
                    sh "sed -i 's/{{BUILD_NUMBER}}/${buildNumber}/g' templates/index.html"
                    
                    docker.withRegistry('', DOCKER_HUB_CREDENTIALS) {
                        def dockerImage = docker.build("${DOCKER_IMAGE_NAME}:${buildNumber}", ".")
                        dockerImage.push()
                        
                        dockerImage.tag("latest")
                        dockerImage.push("latest")
                    }
                }
            }
        }
        
        stage('Deploy to Swarm') {
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

        stage('Check Website Status and Notify') {
            steps {
                script {
                    def buildNumber = env.BUILD_NUMBER

                    echo "Waiting for ${WAIT_TIME} seconds before checking website status..."
                    sleep WAIT_TIME

                    def response = sh(script: "curl -s ${WEBSITE_URL}", returnStdout: true).trim()

                    def ntfyServer = 'ntfy.rdnsx.de'
                    def ntfyTopic = 'RDNSX_Jenkins'
                    def ntfyUrl = "https://${ntfyServer}/${ntfyTopic}"

                    if (response.contains(buildNumber)) {
                        echo "Website is up and contains ${buildNumber}."
                        def title = "Deployment Success"
                        def message = "👍 ${WEBSITE_URL} is successfully running on build ${buildNumber}!"
                        def jsonPayload = """{
                            "title": "${title}",
                            "message": "${message}",
                            "actions": [
                                {
                                    "action": "view",
                                    "url": "${WEBSITE_URL}",
                                    "label": "Check Website"
                                }
                            ]
                        }"""
                        sh "curl -X POST -H 'Content-Type: application/json' -d '''${jsonPayload}''' ${ntfyUrl}"
                    } else {
                        error "Website is not responding properly or does not contain ${buildNumber}."
                        def title = "Deployment Failure"
                        def message = "⛔️ ${WEBSITE_URL} is not responding properly or does not contain ${buildNumber}!"
                        def jsonPayload = """{
                            "title": "${title}",
                            "message": "${message}",
                            "actions": [
                                {
                                    "action": "view",
                                    "url": "${WEBSITE_URL}",
                                    "label": "Check Website"
                                }
                            ]
                        }"""
                        sh "curl -X POST -H 'Content-Type: application/json' -d '''${jsonPayload}''' ${ntfyUrl}"
                    }
                }
            }
        }
    }
}
