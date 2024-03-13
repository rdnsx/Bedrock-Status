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
                    def ntfyServer = 'ntfy.rdnsx.de'
                    def ntfyTopic = 'RDNSX_Jenkins'
                    def websiteUrl = env.WEBSITE_URL
                    
                    echo "Waiting for ${WAIT_TIME} seconds before checking website status..."
                    sleep time: WAIT_TIME.toInteger(), unit: 'SECONDS'
                    
                    def curlResponse = sh(script: "curl -s -o response.txt -w '%{http_code}' ${websiteUrl}", returnStdout: true).trim()
                    def response = readFile('response.txt').trim()

                    if (curlResponse == '200' && response.contains(buildNumber)) {
                        def message = "👍 ${websiteUrl} is successfully running on build ${buildNumber}!"
                        echo message
                        sh "curl -d '${message}' -H 'Actions: view, Check website, ${websiteUrl}' ${ntfyServer}/${ntfyTopic}"
                    } else {
                        def errorMessage = "⛔️ ${websiteUrl} is not responding properly or does not contain ${buildNumber}!"
                        echo errorMessage
                        sh "curl -d '${errorMessage}' -H 'Actions: view, Check website, ${websiteUrl}' ${ntfyServer}/${ntfyTopic}"
                        error errorMessage
                    }
                }
            }
        }
    }
}