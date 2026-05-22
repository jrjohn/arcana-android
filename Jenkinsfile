// Jenkinsfile — multibranch pipeline for arcana-android
// Adapted from legacy android-app-pipeline (single-branch, pollSCM main),
// re-wired for multibranch + PR builds.
//
// Key differences from the legacy XML-embedded script:
//   * `checkout scm` (no hardcoded branch=main)        — supports every branch + every PR
//   * `pollSCM` trigger removed                        — Jenkins multibranch + GitHub webhook drive triggers
//   * `dir("${env.PROJECTS_DIR}/arcana-android")` blocks REMOVED — multibranch uses workspace root
//   * Release AAB/APK + Arch Qube Metrics gated `when { branch 'main' }` — PRs skip release signing/push
//   * SonarQube gets pullrequest.* params on PRs       — PR-decoration in Sonar UI

pipeline {
    agent any

    options {
        timeout(time: 150, unit: 'MINUTES')
        disableConcurrentBuilds()
        buildDiscarder(logRotator(numToKeepStr: '10'))
        timestamps()
    }

    environment {
        PROJECT_NAME = "android-app"
        APP_NAME     = "android-app"
    }

    stages {
        stage("Checkout") {
            steps {
                checkout scm
                sh 'git log -1 --oneline'
                script {
                    echo "Branch: ${env.BRANCH_NAME ?: 'unknown'}"
                    echo "PR: ${env.CHANGE_ID ?: 'no'} (target: ${env.CHANGE_TARGET ?: 'n/a'})"
                }
            }
        }

        stage("Cleanup Old Images") {
            steps {
                sh '''
                    # Remove dangling/unused images to free disk space
                    docker image prune -f || true
                    # Keep only last 3 build-tagged images for this app
                    docker images --format '{{.Repository}}:{{.Tag}}' \
                        | grep "${APP_NAME}.*build-" \
                        | sort -t- -k2 -rn \
                        | tail -n +4 \
                        | xargs -r docker rmi 2>/dev/null || true
                    # Stop leftover test containers
                    docker compose -f docker-compose.test.yml down \
                        --remove-orphans 2>/dev/null || true
                '''
            }
        }

        stage("Docker Compose Build") {
            steps {
                sh "docker compose -f docker-compose.ci.yml build"
            }
        }

        stage("Gradle Build") {
            steps {
                sh "docker compose -f docker-compose.ci.yml run --rm android-build"
            }
        }

        stage("Unit Tests with Coverage") {
            steps {
                catchError(buildResult: 'SUCCESS', stageResult: 'UNSTABLE') {
                    sh "docker compose -f docker-compose.ci.yml run --rm android-test"
                }
            }
        }

        stage("SonarQube Analysis") {
            steps {
                catchError(buildResult: 'SUCCESS', stageResult: 'UNSTABLE') {
                    withSonarQubeEnv('SonarQube') {
                        script {
                            def prArgs = env.CHANGE_ID ? """ \
                                -Dsonar.pullrequest.key=${env.CHANGE_ID} \
                                -Dsonar.pullrequest.branch=${env.BRANCH_NAME} \
                                -Dsonar.pullrequest.base=${env.CHANGE_TARGET}""" : ''
                            sh """sonar-scanner \
                              -Dsonar.projectKey=android-app \
                              -Dsonar.projectName="Android App" \
                              -Dsonar.sources=. \
                              -Dsonar.exclusions=**/build/**,**/.gradle/**,**/docs/**,**/test/** \
                              -Dsonar.java.binaries=. \
                              -Dsonar.scm.disabled=true \
                              -Dsonar.coverage.jacoco.xmlReportPaths=app/build/reports/coverage/test/debug/report.xml,app/build/reports/coverage/unit/debug/report.xml \
                              -Dsonar.coverage.exclusions=**/di/**,**/navigation/**,**/theme/**,**/ui/components/**,buildSrc/**,**/ui/screens/**,**/nav/**,**/NavGraph*,**/worker/**,**/MainActivity*,**/MyApplication*,**/ConnectivityManagerNetworkMonitor*,**/NavigationAnalyticsObserver*,**/AnalyticsManager*,**/SyncManager*${prArgs}"""
                        }
                    }
                }
            }
        }

        stage("Fastlane CI") {
            steps {
                catchError(buildResult: 'SUCCESS', stageResult: 'UNSTABLE') {
                    sh "docker compose -f docker-compose.ci.yml run --rm android-fastlane"
                }
            }
        }

        stage("Build Release AAB") {
            // Release signing pulls keystore from credentials — only run on main builds.
            when { branch 'main' }
            steps {
                catchError(buildResult: 'SUCCESS', stageResult: 'UNSTABLE') {
                    withCredentials([
                        file(credentialsId: 'android-keystore', variable: 'KEYSTORE_FILE'),
                        string(credentialsId: 'android-key-alias', variable: 'KEY_ALIAS'),
                        string(credentialsId: 'android-key-password', variable: 'KEY_PASSWORD'),
                        string(credentialsId: 'android-store-password', variable: 'STORE_PASSWORD')
                    ]) {
                        sh '''
                            KEYSTORE_B64=$(base64 -w 0 $KEYSTORE_FILE)
                            docker run --rm \
                                -v $(pwd):/project \
                                -v arcana-android_gradle-cache:/root/.gradle \
                                -e KEYSTORE_B64="$KEYSTORE_B64" \
                                -e KEY_ALIAS=$KEY_ALIAS \
                                -e KEY_PASSWORD=$KEY_PASSWORD \
                                -e STORE_PASSWORD=$STORE_PASSWORD \
                                localhost:5000/arcana/android-app:1.0.0 \
                                bash -c 'echo "$KEYSTORE_B64" | base64 -d > /tmp/arcana.keystore && KEYSTORE_FILE=/tmp/arcana.keystore bundle exec fastlane bundle_release'
                        '''
                    }
                }
            }
            post {
                success {
                    archiveArtifacts artifacts: 'app/build/outputs/bundle/release/*.aab, app/build/outputs/mapping/release/mapping.txt', allowEmptyArchive: true
                }
            }
        }

        stage("Build Release APK") {
            when { branch 'main' }
            steps {
                catchError(buildResult: 'SUCCESS', stageResult: 'UNSTABLE') {
                    withCredentials([
                        file(credentialsId: 'android-keystore', variable: 'KEYSTORE_FILE'),
                        string(credentialsId: 'android-key-alias', variable: 'KEY_ALIAS'),
                        string(credentialsId: 'android-key-password', variable: 'KEY_PASSWORD'),
                        string(credentialsId: 'android-store-password', variable: 'STORE_PASSWORD')
                    ]) {
                        sh '''
                            KEYSTORE_B64=$(base64 -w 0 $KEYSTORE_FILE)
                            docker run --rm \
                                -v $(pwd):/project \
                                -v arcana-android_gradle-cache:/root/.gradle \
                                -e KEYSTORE_B64="$KEYSTORE_B64" \
                                -e KEY_ALIAS=$KEY_ALIAS \
                                -e KEY_PASSWORD=$KEY_PASSWORD \
                                -e STORE_PASSWORD=$STORE_PASSWORD \
                                localhost:5000/arcana/android-app:1.0.0 \
                                bash -c 'echo "$KEYSTORE_B64" | base64 -d > /tmp/arcana.keystore && KEYSTORE_FILE=/tmp/arcana.keystore bundle exec fastlane build_release'
                        '''
                    }
                }
            }
        }

        stage("Verify APK") {
            when { branch 'main' }
            steps {
                sh """
                    echo '=== Debug APK ==='
                    ls -la app/build/outputs/apk/debug/ || echo 'Debug APK not found'
                    echo '=== Release APK ==='
                    ls -la app/build/outputs/apk/release/ || echo 'Release APK not found'
                """
            }
        }

        stage("Architecture Qube") {
            steps {
                catchError(buildResult: 'SUCCESS', stageResult: 'UNSTABLE') {
                    sh """
                        mkdir -p arch-qube-reports
                        docker run --rm --network devops_default \\
                            -v \$(pwd):/project \\
                            -v \$(pwd)/arch-qube-reports:/output \\
                            arcana.boo/arcana/arch-qube:latest scan /project \\
                            --framework android --no-ai \\
                            --ci --format json,markdown \\
                            -o /output --threshold 90 || true
                    """
                }
            }
        }

        stage("Arch Qube Metrics") {
            when { branch 'main' }
            steps {
                catchError(buildResult: 'SUCCESS', stageResult: 'SUCCESS') {
                    sh "bash /data/projects/_scripts/arch-qube-metrics.sh \$(pwd) arcana-android || true"
                }
            }
        }
    }

    post {
        success { echo "Android build SUCCESS: ${PROJECT_NAME} branch=${env.BRANCH_NAME ?: '?'} pr=${env.CHANGE_ID ?: 'no'}" }
        failure { echo "Android build FAILED: ${PROJECT_NAME} branch=${env.BRANCH_NAME ?: '?'} pr=${env.CHANGE_ID ?: 'no'}" }
        always {
            sh "docker compose -f docker-compose.ci.yml down --remove-orphans || true"
        }
    }
}
