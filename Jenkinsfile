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
        // 9h. The amd64 release builds run under qemu (no native arm64 aapt2/d8),
        // so Build Release AAB (bundleRelease ~136m) + Build Release APK
        // (assembleRelease ~120m) plus ~123m of earlier stages legitimately need
        // ~6.5h. The old 360m timeout was sized when those release stages failed
        // instantly (0m, broken keystore mount) and aborted #51 mid-APK at 384m.
        timeout(time: 540, unit: 'MINUTES')
        disableConcurrentBuilds()
        buildDiscarder(logRotator(numToKeepStr: '10'))
        timestamps()
    }

    parameters {
        // Off by default: Play upload is on-demand, not every main build, because
        // Play rejects a duplicate versionCode — bump app/build.gradle.kts versionCode
        // before running a deploy. Tick this in "Build with Parameters" to upload the
        // freshly-built release AAB to the Play internal track (draft).
        booleanParam(name: 'DEPLOY_PLAY', defaultValue: false,
                     description: 'Upload the release AAB to Google Play internal track (draft). main only.')
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

        stage("Cleanup Stale Gradle Locks") {
            // Build #21 was killed mid-gradle by a Jenkins agent flap and left
            // /root/.gradle/caches/journal-1/journal-1.lock owned by a dead PID,
            // blocking the next gradle invocation with a "Timeout waiting to lock
            // journal cache" error. disableConcurrentBuilds() guarantees no other
            // build holds the volume right now, so it's safe to wipe stale locks.
            // Wipe-list rationale (kept here, not inside the sh-c, so embedded
            //   '#' / em-dash / inner quotes don't confuse the shell):
            //   *.lock                  — stale advisory locks from dead PIDs
            //   caches/journal-1        — FileAccessTimeJournal half-write
            //                              (#31 fail: "Could not create service
            //                              of type FileAccessTimeJournal")
            //   caches/transforms-*     — occasional ASM/D8 corruption
            //   caches/*/transforms-*   — same, nested under per-version dirs
            //   daemon/                 — stale daemon registry PIDs
            steps {
                sh '''
                    docker run --rm \
                        -v arcana-android_gradle-cache:/cache \
                        alpine:3 \
                        sh -c "find /cache -name '*.lock' -type f -delete 2>/dev/null ; rm -rf /cache/caches/journal-1 /cache/caches/transforms-* /cache/caches/*/transforms-* /cache/daemon 2>/dev/null ; true"
                    # Also reclaim docker network subnet pool — bridge driver only has
                    # ~30 /16 slots in the default pool. Each multibranch PR build
                    # claims one (NAME_default), and #34 hit "all predefined address
                    # pools have been fully subnetted" on a fresh main build because
                    # closed-PR networks were never reclaimed.
                    docker network prune -f >/dev/null 2>&1 || true
                '''
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
                        // This compose talks to the HOST docker daemon, so bind-mount
                        // source paths resolve on the host, not in this jenkins container
                        // — `- .:/project` shadowed the baked Gemfile and
                        // `- ./keystore.jks:...` couldn't be seen by the host daemon
                        // (#42/#43/#49/#50). So mount NO workspace path: the image bakes
                        // /project (source + Gemfile + bundle), the keystore is passed as
                        // base64 the container decodes in-place, and the signed AAB is
                        // pulled back with `docker cp` (CLI-side copy, no path mount).
                        sh '''
                            set -e
                            docker rm -f arcana-aab-build 2>/dev/null || true
                            KEYSTORE_B64="$(base64 -w0 "$KEYSTORE_FILE")" \
                            KEY_ALIAS="$KEY_ALIAS" KEY_PASSWORD="$KEY_PASSWORD" STORE_PASSWORD="$STORE_PASSWORD" \
                                docker compose -f docker-compose.ci.yml run --name arcana-aab-build android-aab
                            mkdir -p app/build/outputs/bundle/release app/build/outputs/mapping/release
                            docker cp arcana-aab-build:/project/app/build/outputs/bundle/release/. app/build/outputs/bundle/release/ || true
                            docker cp arcana-aab-build:/project/app/build/outputs/mapping/release/mapping.txt app/build/outputs/mapping/release/ || true
                            docker rm -f arcana-aab-build 2>/dev/null || true
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

        // NOTE: "Build Release APK" (assembleRelease) removed 2026-05-28 — Play only
        // needs the AAB, and assembleRelease was a second ~2h qemu cold build doing the
        // same compile/R8/dex as bundleRelease. Dropping it cuts the pipeline ~2h. The
        // build_release fastlane lane + android-release compose service still exist for
        // manual/local APK builds if ever needed.

        stage("Deploy to Play (internal)") {
            // On-demand (DEPLOY_PLAY param) + main only. Uploads the release AAB built
            // by "Build Release AAB" to the Play internal track as a draft — no rebuild,
            // reuses the workspace AAB. The AAB is already signed with the registered
            // Play upload key (arcana keystore via android-keystore credential); this
            // stage only needs the google-play-key service account to talk to Play.
            // docker create + docker cp (not bind mounts) to dodge the DinD host-path
            // problem (see Build Release AAB note).
            when {
                allOf {
                    branch 'main'
                    expression { params.DEPLOY_PLAY }
                }
            }
            steps {
                catchError(buildResult: 'SUCCESS', stageResult: 'UNSTABLE') {
                    withCredentials([file(credentialsId: 'google-play-key', variable: 'GP_KEY')]) {
                        sh '''
                            set -e
                            docker rm -f arcana-play-deploy 2>/dev/null || true
                            docker create --name arcana-play-deploy --platform linux/amd64 \
                                localhost:5000/arcana/android-app:${VERSION:-1.0.0} \
                                bash -c "mkdir -p app/build/outputs/bundle/release && cp /tmp/app-release.aab app/build/outputs/bundle/release/app-release.aab && bundle exec fastlane upload_aab_internal"
                            docker cp app/build/outputs/bundle/release/app-release.aab arcana-play-deploy:/tmp/app-release.aab
                            docker cp "$GP_KEY" arcana-play-deploy:/project/fastlane/google-play-key.json
                            docker start -a arcana-play-deploy
                            docker rm -f arcana-play-deploy 2>/dev/null || true
                        '''
                    }
                }
            }
        }

        stage("Verify Artifacts") {
            when { branch 'main' }
            steps {
                sh """
                    echo '=== Debug APK ==='
                    ls -la app/build/outputs/apk/debug/ || echo 'Debug APK not found'
                    echo '=== Release AAB ==='
                    ls -la app/build/outputs/bundle/release/ || echo 'Release AAB not found'
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
