// Jenkins declarative pipeline for building, testing, and publishing the platform.

pipeline {
    agent any

    environment {
        REGISTRY = "your-registry.example.com/gametrade"
        IMAGE_TAG = "${env.GIT_COMMIT ?: 'latest'}"
        JAVA_SERVICES = "gateway user-service item-service inventory-service payment-service order-service notification-service"
    }

    options {
        timestamps()
        disableConcurrentBuilds()
    }

    stages {
        stage('Build & Test (Java)') {
            steps {
                sh './mvnw -B verify'
            }
            post {
                always {
                    junit '**/target/surefire-reports/*.xml'
                }
            }
        }

        stage('Test (Python AI assistant)') {
            steps {
                dir('ai-assistant-service') {
                    sh '''
                        python3 -m venv .venv
                        . .venv/bin/activate
                        pip install --quiet pydantic pydantic-settings httpx pytest
                        pytest -q
                    '''
                }
            }
        }

        stage('Docker Build & Push') {
            steps {
                script {
                    def services = env.JAVA_SERVICES.split(' ')
                    for (svc in services) {
                        sh "docker build -f ${svc}/Dockerfile -t ${REGISTRY}/${svc}:${IMAGE_TAG} ."
                        sh "docker push ${REGISTRY}/${svc}:${IMAGE_TAG}"
                    }
                    sh "docker build -t ${REGISTRY}/ai-assistant-service:${IMAGE_TAG} ./ai-assistant-service"
                    sh "docker push ${REGISTRY}/ai-assistant-service:${IMAGE_TAG}"
                    sh "docker build -t ${REGISTRY}/frontend:${IMAGE_TAG} ./frontend"
                    sh "docker push ${REGISTRY}/frontend:${IMAGE_TAG}"
                }
            }
        }

        stage('Deploy to ECS / Kubernetes') {
            steps {
                // Rolling update; on Kubernetes this is `kubectl set image` per deployment.
                sh 'echo "kubectl apply -k k8s/ && kubectl rollout status deploy/gateway"'
            }
        }
    }

    post {
        success {
            echo "Release ${IMAGE_TAG} completed."
        }
        failure {
            echo "Build failed for ${IMAGE_TAG}."
        }
    }
}
