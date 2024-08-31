pipeline {
    agent any

    options {
        ansiColor('xterm')
        timestamps()
        buildDiscarder(logRotator(numToKeepStr:'10'))
        disableConcurrentBuilds()
    }

    environment{
        GITHUB = credentials('mca-bot-gh')
	    //GIT_REPO_NAME = "${env.JOB_NAME.split('/')[-2]}"
	    GIT_REPO_NAME = "lambda-smart-create-workflow"
	    LOGGING_FORMAT = 'flat'

        REDIS_PASSWORD = 'easypassword'
        REDIS_PASSWORD_KEY = 'dev/redis/password'
        REDIS_HOST = 'service.local.smart.mcga.uk'
        REDIS_TLS = 'true'
        REDIS_PORT = '6379'
        //SMART_API_URI = 'http://172.17.0.1:8080/'

//         OKTA_CLIENT_ID_KEY = 'dev/smart/okta_client_id'
//         OKTA_CLIENT_SECRET_KEY = 'dev/smart/okta_client_secret'
//
//         OKTA_CLIENT_SECRET = credentials('dev/smart/okta_client_secret')
//         OKTA_CLIENT_ID = credentials('dev/smart/okta_client_id')
//         OKTA_ISSUER = credentials('dev/smart/okta_issuer')
//         OKTA_TOKEN_URL = "${env.OKTA_ISSUER}/v1/token"

        GRADLE_OPTS = '-Dorg.gradle.daemon=false'

//         SONAR_ORG = 'mcga-gov-uk'
//         SONAR_PROJECT = "${env.JOB_NAME.toLowerCase().split('/')[1]}"
//         SONAR_TOKEN = credentials('devtools/sonar-token')
        AWS_REGION = 'eu-west-2'

        AWS_CREDENTIALS_ID = 'aws-jenkins-service-account-credentials' // ID for AWS credentials in Jenkins
    }

    stages {
        stage('setup') {
            agent {
                docker {
                    image '009543623063.dkr.ecr.eu-west-2.amazonaws.com/jenkins-gradle-ci:corretto-17'
                    alwaysPull true
                    args '-v /var/run/docker.sock:/var/run/docker.sock -v /tmp:/tmp'
                }
            }
            steps{
                script {
                    scmSkip(deleteBuild: true, skipPattern:'.*\\[skip ci\\].*')

                    // get the build user
                    wrap([$class: 'BuildUser']) {
                        env.BUILDER = sh (script:'[[ -z "${BUILD_USER}" ]] && echo -n "$(git show -s --pretty=%ae)" || echo -n "${BUILD_USER}"',returnStdout: true).trim()
                    }
                    //env.SLACK_ID = getSlackid.forEmail "${env.BUILDER}"

                    env.BUILD_VERSION = sh (script:'''git tag -l 'v[0-9]*' | cut -d 'v' -f 2 | sort --version-sort --reverse | head -n 1 | awk -F. -v OFS=. '{$NF++;print}' ''', returnStdout: true).trim()
                    sh 'echo "BUILD_VERSION = ${BUILD_VERSION}"'
                    if (!env.BUILD_VERSION?.trim()) {
                        env.BUILD_VERSION = "1.0.0"
                    }
                    if ( BRANCH_NAME != 'master'){
                        if ( BRANCH_NAME == 'develop'){
                            env.BUILD_VERSION = BUILD_VERSION + '-SNAPSHOT'
                        }else{
                            env.BUILD_VERSION = BUILD_VERSION + '-' + BRANCH_NAME.replaceAll("feature/","").replaceAll("\\s","").replaceAll("/","_")
                        }
                    }

                    sh 'echo "Setting BUILD_VERSION to ${BUILD_VERSION}"'
                    buildName "${BUILD_VERSION}"

                    withAWS(roleAccount:'009543623063', role:'CrossAccount-Deployer', region: "${AWS_REGION}") {
                        sh "./update-local-certs.sh"
                        env.CODEARTIFACT_AUTH_TOKEN = sh(script:'''aws codeartifact get-authorization-token --domain mcga --domain-owner 009543623063 --query authorizationToken --output text''', returnStdout: true).trim()
                    }
                }
            }
        }

//         stage('test') {
//             steps {
//                 script {
//                     sh 'docker compose pull'
//                     sh 'docker compose up -d'
//                     sh "./gradlew clean test -i"
//                 }
//             }
//             post {
//                 always {
//                     sh 'docker compose logs redis --no-color > redis-logs.txt'
//                     sh 'docker compose logs localstack --no-color > localstack-logs.txt'
//                     sh 'docker compose down || true'
//                     withChecks('Unit tests') {
//                         junit 'build/test-results/test/*.xml'
//                     }
//                     recordCoverage(tools: [[parser: 'JACOCO', pattern: 'build/reports/jacoco/test/jacocoTestReport.xml']],
//                         id: 'jacoco', name: 'Code Coverage',
//                         sourceCodeRetention: 'EVERY_BUILD',
//                         qualityGates: [
//                                 [threshold: 60.0, metric: 'LINE', baseline: 'PROJECT', criticality: 'UNSTABLE'],
//                                 [threshold: 60.0, metric: 'BRANCH', baseline: 'PROJECT', criticality: 'UNSTABLE']])
//                 }
//             }
//         }

        // stage('sonarcloud') {
        //     when { branch 'master' }
        //     steps {
        //             sh '''
        //                 sonar-scanner  \
        //                     -Dsonar.host.url=https://sonarcloud.io \
        //                     -Dsonar.organization=${SONAR_ORG} \
        //                     -Dsonar.projectKey=${SONAR_ORG}_${SONAR_PROJECT} \
        //                     -Dsonar.sources=src/main/java \
        //                     -Dsonar.tests=src/test/java \
        //                     -Dsonar.java.binaries=build/classes \
        //                     -Dsonar.junit.reportsPath=build/test-results/test/
        //             '''
        //     }
        // }

        stage('publish') {
            when { branch 'master' }
            steps {
                script {
                    env.LAMBDA_FOLDER  = env.GIT_REPO_NAME
                    env.LAMBDA_PACKAGE = sh (script:'echo -n "${LAMBDA_FOLDER}-${BUILD_VERSION}-aws.jar"',returnStdout: true).trim()

                    sh "./gradlew -x test -PprojVersion=${BUILD_VERSION} clean build"

                    sh 'aws s3 cp build/libs/${LAMBDA_PACKAGE} s3://mcauk-smart-live-terraform-tfstate/artifacts/lambdas/${LAMBDA_FOLDER}/${LAMBDA_PACKAGE}'
                }
            }
        }
        stage('Tag Release') {
            when {
                branch 'master'
            }
            steps {
                script {
                    withCredentials([usernamePassword(credentialsId: 'mca-bot-gh', passwordVariable: 'GIT_PASSWORD', usernameVariable: 'GIT_USERNAME')]) {
                        sh '''
                            git tag -a v${BUILD_VERSION} -m "release ${BUILD_VERSION} || true"
                            git push https://${GIT_USERNAME}:${GIT_PASSWORD}@github.com/mcga-gov-uk/${GIT_REPO_NAME}.git v${BUILD_VERSION}
                        '''
                    }
                }
            }
        }

    }

//     post {
//         always {
//             jiraSendBuildInfo site: 'mcauk.atlassian.net'
//         }
//         failure {
//             slackSend (color: '#FF0000', message: '', attachments: [
//                 [
//                     text:   '<@' + env.BUILDER?.split('@')[0] + '>\n' +
//                             ' A build you started has failed\n' +
//                             '<' + env.BUILD_URL + '|' +
//                             env.JOB_NAME.replaceAll('/', ' » ') +
//                             ' #' + env.BUILD_NUMBER + '>\n' ,
//                     color: '#FF0000'
//                 ]
//                     ])
//             emailext (
//                 subject: "[JENKINS MCAUK] FAILED: Job '${env.JOB_NAME} [${env.BUILD_NUMBER}]'",
//                 body: """<p>FAILED: Job '${env.JOB_NAME} [${env.BUILD_NUMBER}]':</p>
//                         <p>Check console output at &QUOT;<a href='${env.BUILD_URL}'>${env.JOB_NAME} [${env.BUILD_NUMBER}]</a>&QUOT;</p>""",
//                 to: 'mcauk@catapult.cx',
//                 recipientProviders: [[$class: 'DevelopersRecipientProvider'], [$class: 'RequesterRecipientProvider'], [$class: 'CulpritsRecipientProvider']]
//             )
//         }
//     }
}
