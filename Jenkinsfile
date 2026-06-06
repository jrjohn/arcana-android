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
        // Render ANSI colour codes (gradle/fastlane) in the Jenkins console.
        // Needs the AnsiColor plugin. NOTE: raw consoleText still contains the
        // escape codes — log consumers (daily-ci-agent) strip them when parsing.
        ansiColor('xterm')
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
            // Build under a UNIQUE :build-N tag, then derive :1.0.0 from it via
            // `docker tag`. Exporting the static :1.0.0 directly fails with
            // `image "...android-app:1.0.0": already exists` (#78) — buildkit's
            // containerd image store refuses to re-export an existing tag, and
            // Cleanup Old Images only rotates build-N images so the prior :1.0.0
            // lingers. A unique build-N export never collides; `docker tag` is a
            // metadata reassign that overwrites freely. Mirrors arcana-cloud-rust.
            steps {
                sh "VERSION=build-${BUILD_NUMBER} docker compose -f docker-compose.ci.yml build"
                sh "docker tag localhost:5000/arcana/android-app:build-${BUILD_NUMBER} localhost:5000/arcana/android-app:1.0.0"
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
            // Blocking: a failing unit test (non-zero from the test lane) fails the
            // stage. The coverage report is pulled out with `docker cp` (DinD: no
            // bind mount works, see docker-compose android-test note) so SonarQube
            // can import it in the next stage.
            steps {
                sh '''
                    docker rm -f arcana-test-build-${BUILD_NUMBER} 2>/dev/null || true
                    docker compose -f docker-compose.ci.yml run --name arcana-test-build-${BUILD_NUMBER} android-test
                    TEST_RC=$?
                    mkdir -p app/build/reports app/build/test-results app/build/outputs
                    docker cp arcana-test-build-${BUILD_NUMBER}:/project/app/build/reports/. app/build/reports/ 2>/dev/null || true
                    docker cp arcana-test-build-${BUILD_NUMBER}:/project/app/build/test-results/. app/build/test-results/ 2>/dev/null || true
                    docker cp arcana-test-build-${BUILD_NUMBER}:/project/app/build/outputs/. app/build/outputs/ 2>/dev/null || true
                    docker rm -f arcana-test-build-${BUILD_NUMBER} 2>/dev/null || true
                    exit $TEST_RC
                '''
            }
            post {
                always {
                    junit allowEmptyResults: true, testResults: 'app/build/test-results/**/*.xml'
                }
            }
        }

        stage("SonarQube Analysis") {
            // Blocking quality gate. The scanner imports the JaCoCo coverage report
            // pulled out by "Unit Tests with Coverage"; with the coverage.exclusions
            // below (DI/Compose UI/navigation/theme/workers) SonarQube computes ~83.5%
            // overall coverage, above the gate's 80% threshold. The build FAILS if the
            // gate is not OK (coverage drop or new-code rating regression).
            steps {
                withSonarQubeEnv('SonarQube') {
                    // NO sonar.pullrequest.* params: this is SonarQube Community Build,
                    // which rejects them ("Developer Edition or above is required") and
                    // fails the scan — which is why PR analysis was silently broken until
                    // the gate became blocking. Analyze with the plain project key on
                    // every branch and PR; the gate poll below keys off THIS run's
                    // analysisId, so the OK/ERROR verdict is correct per-analysis even
                    // though PR and main analyses share one project.
                    sh """sonar-scanner \
                      -Dsonar.projectKey=android-app \
                      -Dsonar.projectName="Android App" \
                      -Dsonar.sources=. \
                      -Dsonar.exclusions=**/build/**,**/.gradle/**,**/docs/**,**/test/** \
                      -Dsonar.java.binaries=. \
                      -Dsonar.scm.disabled=true \
                      -Dsonar.coverage.jacoco.xmlReportPaths=app/build/reports/coverage/test/debug/report.xml,app/build/reports/coverage/unit/debug/report.xml \
                      -Dsonar.coverage.exclusions=**/di/**,**/navigation/**,**/theme/**,**/ui/components/**,buildSrc/**,**/ui/screens/**,**/nav/**,**/NavGraph*,**/worker/**,**/MainActivity*,**/MyApplication*,**/ConnectivityManagerNetworkMonitor*,**/NavigationAnalyticsObserver*,**/AnalyticsManager*,**/SyncManager*"""
                    // waitForQualityGate() needs a server→Jenkins webhook (not
                    // configured here); instead poll the compute-engine task named in
                    // .scannerwork/report-task.txt, then read the gate status. The
                    // jenkins agent has only curl (no jq), so parse JSON with grep.
                    sh '''
                        set -e
                        # plugin exposes the token as SONAR_AUTH_TOKEN (2.x) or SONAR_TOKEN
                        TOKEN="${SONAR_AUTH_TOKEN:-$SONAR_TOKEN}"
                        RT=.scannerwork/report-task.txt
                        [ -f "$RT" ] || { echo "report-task.txt not found — scanner did not run"; exit 1; }
                        CE_TASK_ID=$(grep '^ceTaskId=' "$RT" | cut -d= -f2-)
                        echo "CE task id: $CE_TASK_ID"
                        ANALYSIS_ID=""
                        for i in $(seq 1 60); do
                            RESP=$(curl -s -u "$TOKEN:" "$SONAR_HOST_URL/api/ce/task?id=$CE_TASK_ID")
                            ST=$(echo "$RESP" | grep -o '"status":"[A-Z_]*"' | head -1 | cut -d'"' -f4)
                            echo "  CE status: ${ST:-?} (try $i)"
                            if [ "$ST" = "SUCCESS" ]; then
                                ANALYSIS_ID=$(echo "$RESP" | grep -o '"analysisId":"[^"]*"' | head -1 | cut -d'"' -f4)
                                break
                            elif [ "$ST" = "FAILED" ] || [ "$ST" = "CANCELED" ]; then
                                echo "CE task ended $ST"; exit 1
                            fi
                            sleep 5
                        done
                        [ -n "$ANALYSIS_ID" ] || { echo "CE task did not finish in time"; exit 1; }
                        GATE=$(curl -s -u "$TOKEN:" "$SONAR_HOST_URL/api/qualitygates/project_status?analysisId=$ANALYSIS_ID")
                        GST=$(echo "$GATE" | grep -o '"status":"[A-Z]*"' | head -1 | cut -d'"' -f4)
                        echo "Quality gate: ${GST:-UNKNOWN}"
                        if [ "$GST" != "OK" ]; then
                            echo "--- gate response ---"; echo "$GATE"
                            exit 1
                        fi
                    '''
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
                            docker rm -f arcana-aab-build-${BUILD_NUMBER} 2>/dev/null || true
                            KEYSTORE_B64="$(base64 -w0 "$KEYSTORE_FILE")" \
                            KEY_ALIAS="$KEY_ALIAS" KEY_PASSWORD="$KEY_PASSWORD" STORE_PASSWORD="$STORE_PASSWORD" \
                                docker compose -f docker-compose.ci.yml run --name arcana-aab-build-${BUILD_NUMBER} android-aab
                            mkdir -p app/build/outputs/bundle/release app/build/outputs/mapping/release
                            docker cp arcana-aab-build-${BUILD_NUMBER}:/project/app/build/outputs/bundle/release/. app/build/outputs/bundle/release/ || true
                            docker cp arcana-aab-build-${BUILD_NUMBER}:/project/app/build/outputs/mapping/release/mapping.txt app/build/outputs/mapping/release/ || true
                            docker rm -f arcana-aab-build-${BUILD_NUMBER} 2>/dev/null || true
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
                            docker rm -f arcana-play-deploy-${BUILD_NUMBER} 2>/dev/null || true
                            docker create --name arcana-play-deploy-${BUILD_NUMBER} \
                                localhost:5000/arcana/android-app:${VERSION:-1.0.0} \
                                bash -c "mkdir -p app/build/outputs/bundle/release && cp /tmp/app-release.aab app/build/outputs/bundle/release/app-release.aab && bundle exec fastlane upload_aab_internal"
                            docker cp app/build/outputs/bundle/release/app-release.aab arcana-play-deploy-${BUILD_NUMBER}:/tmp/app-release.aab
                            docker cp "$GP_KEY" arcana-play-deploy-${BUILD_NUMBER}:/project/fastlane/google-play-key.json
                            docker start -a arcana-play-deploy-${BUILD_NUMBER}
                            docker rm -f arcana-play-deploy-${BUILD_NUMBER} 2>/dev/null || true
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
            // Blocking: arch-qube exits non-zero if the architecture score is below
            // --threshold 90, which fails the stage. DinD-safe like the other stages:
            // bind mounts resolve on the host daemon, so source is copied IN via a tar
            // stream (excluding build/.git noise) and the report is copied OUT, both
            // through anonymous volumes (/src, /output) that exist for the container.
            steps {
                sh '''
                    docker rm -f arcana-arch-qube-android-${BUILD_NUMBER} 2>/dev/null || true
                    docker create --name arcana-arch-qube-android-${BUILD_NUMBER} --network devops_default \
                        -v /src -v /output \
                        arcana.boo/arcana/arch-qube:latest \
                        scan /src --framework android --no-ai --ci \
                        --format json,markdown -o /output --threshold 90 || exit 1
                    tar --exclude=./.git --exclude=./app/build --exclude=./build \
                        --exclude=./.gradle --exclude=./.scannerwork \
                        --exclude=./arch-qube-reports -C . -cf - . \
                        | docker cp - arcana-arch-qube-android-${BUILD_NUMBER}:/src || exit 1
                    docker start -a arcana-arch-qube-android-${BUILD_NUMBER}
                    AQ_RC=$?
                    mkdir -p arch-qube-reports
                    docker cp arcana-arch-qube-android-${BUILD_NUMBER}:/output/. arch-qube-reports/ 2>/dev/null || true
                    docker rm -f arcana-arch-qube-android-${BUILD_NUMBER} 2>/dev/null || true
                    exit $AQ_RC
                '''
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
