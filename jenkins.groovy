pipeline {
    agent any
    
    environment {
        DOCKER_HUB_CREDENTIALS = 'DockerHub'
        SOURCE_REPO_URL = 'https://github.com/rdnsx/Bedrock-Status.git'
        DOCKER_IMAGE_NAME = 'rdnsx/bedrockstatus'
        LATEST_TAG = 'latest'
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
        
        stage('Get Latest Tag') {
            steps {
                script {
                    def dockerHubUrl = "https://hub.docker.com/v2/repositories/${DOCKER_IMAGE_NAME}/tags/?page_size=100"
                    def response = sh(script: "curl -s ${dockerHubUrl}", returnStdout: true)
                    def latestTag = findLatestTag(response)
                    
                    def buildNumber = env.BUILD_NUMBER
                    
                    echo "Latest Docker tag found: ${latestTag}"
                    env.TAG_NAME = "${latestTag}-build_${buildNumber}"
                }
            }
        }
        
        stage('Build Docker Image') {
            steps {
                script {
                    docker.withRegistry('', DOCKER_HUB_CREDENTIALS) {
                        def dockerImage = docker.build("${DOCKER_IMAGE_NAME}:${TAG_NAME}", ".")
                        dockerImage.push()

                        dockerImage.tag("${LATEST_TAG}")
                        dockerImage.push("${LATEST_TAG}")
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

        stage('Check Website Status') {
            steps {
                script {

                    echo "Waiting for ${WAIT_TIME} seconds before checking website status..."
                    sleep WAIT_TIME

                    def response = sh(script: "curl -s ${WEBSITE_URL}", returnStdout: true).trim()

                    if (response.contains('Gamemode')) {
                        echo "Website is up and contains 'Gamemode'."
                    } else {
                        error "Website is not responding properly or does not contain 'Gamemode'."
                    }

                }
            }
        }
    }
}

def findLatestTag(response) {
    def jsonSlurper = new groovy.json.JsonSlurper()
    def tags = jsonSlurper.parseText(response).results.name
    def numericTags = tags.findAll { tag -> tag.matches("\\d+(\\.\\d+)*") }
    def sortedTags = numericTags.sort { tag1, tag2 ->
        compareTagVersions(tag1, tag2)
    }
    return sortedTags.last()
}

def compareTagVersions(tag1, tag2) {
    def versionParts1 = tag1.tokenize('.').collect { it as Integer }
    def versionParts2 = tag2.tokenize('.').collect { it as Integer }

    for (int i = 0; i < Math.min(versionParts1.size(), versionParts2.size()); i++) {
        if (versionParts1[i] < versionParts2[i]) {
            return -1
        } else if (versionParts1[i] > versionParts2[i]) {
            return 1
        }
    }
    
    if (versionParts1.size() < versionParts2.size()) {
        return -1
    } else if (versionParts1.size() > versionParts2.size()) {
        return 1
    }
    
    return 0
}

