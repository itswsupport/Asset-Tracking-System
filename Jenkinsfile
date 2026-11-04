// AMS Backend (Asset-Tracking-System) — build, test, scan and deploy the API container.
//
// Mirrors the Sustainability backend pipeline: same parameters, the same OWASP and Trivy
// scans, the same port preflight before a deploy, the same health check and the same
// reports. What differs is this project's own values — the ams-backend container on the
// ams-net network the AMS UI proxies to, the db_asset schema, the ams-jwt-secret — and a
// final check of the AMS UI instead of mail links (this backend sends no mail).
//
// SECRETS are Jenkins credentials (Manage Jenkins > Credentials > Global > Secret text),
// never in this file:
//   DB_URL          jdbc:mysql://192.168.100.30:3309   (shared with CLMS / PRPO / Sustainability)
//   DB_USERNAME     (shared)
//   db-password     (shared)
//   ams-jwt-secret  AMS's own, 32+ characters — signs the sign-in tokens
//   nvd-api-key     the NVD API key the OWASP scan needs (shared with Sustainability)

pipeline {
  // Runs on the controller (the jenkins-test container). It already has Java 21 and git, and
  // mvnw fetches Maven; it needs the Docker CLI and the host's /var/run/docker.sock mounted
  // for the Docker stages.
  agent { label 'built-in' }

  triggers {
    // GitHub can't reach 192.168.100.30 (private LAN), so a push webhook never arrives. Poll
    // instead; switch to githubPush() if Jenkins is ever exposed.
    pollSCM('H/5 * * * *')
  }

  options {
    timestamps()
    disableConcurrentBuilds()
    buildDiscarder(logRotator(numToKeepStr: '15'))
    timeout(time: 45, unit: 'MINUTES')
  }

  parameters {
    string(
      name: 'BRANCH',
      defaultValue: 'main',
      description: 'Branch of Asset-Tracking-System to build. Work goes local-bk-ams -> develop -> main; main is the one deployed.'
    )
    string(
      name: 'HOST_PORT',
      defaultValue: '9095',
      description: 'Port on the Docker host to publish the API on (container port 8080). The AMS UI does not use it — it reaches ams-backend:8080 over the ams-net network — it is for the smoke test and for curl. 9096 is Sustainability, 9097 PRPO, 9098 clms-backend, 9099 attendance-sync-service.'
    )
    string(
      name: 'UI_URL',
      defaultValue: 'http://192.168.100.30:3023',
      description: 'Address users open the AMS UI at (the AMS_UI job publishes it on 3023). Checked after deploy: its login page /Login/ams/ and its /api proxy to this backend. No trailing slash.'
    )
    booleanParam(
      name: 'DEPLOY',
      defaultValue: true,
      description: 'Untick to build, test and scan only, without replacing the running container.'
    )
    booleanParam(
      name: 'FAIL_ON_VULNERABILITIES',
      defaultValue: false,
      description: 'Tick to fail the build when Trivy finds CRITICAL/HIGH issues instead of only reporting them.'
    )
  }

  environment {
    RECIPIENTS = 'ruchatest4@gmail.com'
    REPO_URL   = 'https://github.com/itswsupport/Asset-Tracking-System.git'

    // Image and container name. The CONTAINER name is what the AMS UI proxies to —
    // BACKEND_ORIGIN = 'http://ams-backend:8080' in the AMS_UI Jenkinsfile — so it must not
    // change without changing that too.
    IMAGE      = 'ams-backend'
    CONTAINER  = 'ams-backend'
    TAG        = "${env.BUILD_NUMBER}"

    // Docker network shared with the UI container (ui-ams). Container-name DNS only exists on
    // user-defined networks; on the default bridge the UI's proxy fails with
    // "getaddrinfo EAI_AGAIN ams-backend". Whichever job deploys first creates it.
    DOCKER_NETWORK = 'ams-net'

    // Database — the SHARED credentials. DB_URL is the base url with no schema; the schema is
    // added with ${DB_BASE_URL%/}/${DB_NAME}, so a stored value with or without a trailing
    // slash both work. useSSL=false and allowPublicKeyRetrieval=true: this MySQL offers only
    // TLS versions a modern JDK has disabled. The batching options repeat the ones in
    // application.properties (bulk upload relies on rewriteBatchedStatements), because
    // SPRING_DATASOURCE_URL replaces that whole url.
    DB_BASE_URL = credentials('DB_URL')
    DB_USERNAME = credentials('DB_USERNAME')
    DB_PASSWORD = credentials('db-password')
    DB_NAME     = 'db_asset'
    DB_OPTIONS  = 'useSSL=false&allowPublicKeyRetrieval=true&rewriteBatchedStatements=true&cachePrepStmts=true&prepStmtCacheSize=250&prepStmtCacheSqlLimit=2048'

    // Signs the sign-in tokens (JWT) — every API but /login and /logout needs one. AMS's OWN
    // secret, not shared with any other app: a token from one app must not open another.
    JWT_SECRET  = credentials('ams-jwt-secret')

    // >> CURRENTLY OFF for the test deployment. <<
    // 'false' skips the BCrypt check against emp_details.emp_pass: anyone who knows a
    // registered employee code can sign in as that employee. The employee must still be active
    // and hold an active ams_login_access row, and the role still comes from there.
    // LoginServiceImpl logs a WARN on every sign-in while this is off. Set back to 'true' once
    // real passwords are in use.
    LOGIN_PASSWORD_VERIFY = 'false'

    // Trivy configuration
    TRIVY_SEVERITY = 'CRITICAL,HIGH'

    // Jenkins itself runs in a container, so "localhost" in an sh step is the Jenkins
    // container, not the Docker host the API's port is published on. curl runs in a throwaway
    // container on the host's network instead, which tests the published port for real.
    CURL = 'docker run --rm --network host curlimages/curl:8.11.1'
  }

  stages {
    stage('Checkout Code') {
      steps {
        git(
          url: env.REPO_URL,
          branch: "${params.BRANCH ?: 'main'}",
          credentialsId: 'ui-payroll'
        )
        script {
          env.GIT_SHA = sh(script: 'git rev-parse --short HEAD', returnStdout: true).trim()
          env.TRIVY_EXIT_CODE = params.FAIL_ON_VULNERABILITIES ? '1' : '0'
          // On the first run after a parameter is added Jenkins has not registered it yet and
          // it reads as null — fall back to the default.
          env.HOST_PORT = params.HOST_PORT ?: '9095'

          def ui = (params.UI_URL ?: 'http://192.168.100.30:3023').trim().replaceAll('/+$', '')
          if (!(ui ==~ /https?:\/\/[^\/\s]+(\/\S*)?/)) {
            error "UI_URL must be a full http:// or https:// address — got '${ui}'"
          }
          env.UI_URL = ui
        }
        echo "Building ${env.IMAGE}:${env.TAG} from ${env.GIT_SHA} (branch ${params.BRANCH ?: 'main'})"
      }
    }

    stage('Build & Test') {
      steps {
        sh 'chmod +x mvnw'
        // AssetTrackingSystemApplicationTests boots the full Spring context against MySQL
        // (there is no in-memory database), so the tests get the same datasource and JWT
        // secret the container will. retry(2): one flaky database connection does not fail
        // the build; a real compile or test error fails both tries.
        retry(2) {
          sh '''
            export SPRING_DATASOURCE_URL="${DB_BASE_URL%/}/${DB_NAME}?${DB_OPTIONS}"
            export SPRING_DATASOURCE_USERNAME="${DB_USERNAME}"
            export SPRING_DATASOURCE_PASSWORD="${DB_PASSWORD}"
            ./mvnw -B clean package
          '''
        }
      }
      post {
        always { junit testResults: 'target/surefire-reports/*.xml', allowEmptyResults: true }
      }
    }

    stage('OWASP Dependency Check') {
      steps {
        // The CLI is run directly rather than through the dependencyCheck step so the NVD API
        // key reaches it as an environment variable and is never interpolated into the Groovy
        // string. Dependency-Check 12+ will not download the NVD data without a key. The
        // database is kept under the tool's install directory, so only the first run
        // downloads it in full.
        //
        // Scans the packaged jar: it holds every library that actually ships, transitive ones
        // included, and nothing that is test- or build-only. (The .jar.original
        // spring-boot-maven-plugin leaves beside it does not match *.jar.)
        withCredentials([string(credentialsId: 'nvd-api-key', variable: 'NVD_API_KEY')]) {
          sh """
            if [ -z "\$NVD_API_KEY" ]; then
              echo "the nvd-api-key credential is empty — paste the NVD API key into its Secret field"
              exit 1
            fi

            "${tool 'Dependency-Check'}/bin/dependency-check.sh" \
              --project '${IMAGE}' \
              --scan 'target/*.jar' \
              --format HTML \
              --format XML \
              --out . \
              --prettyPrint \
              --nvdApiKey "\$NVD_API_KEY"
          """
        }

        dependencyCheckPublisher pattern: 'dependency-check-report.xml'
      }
    }

    stage('Package') {
      steps {
        // The Dockerfile builds the jar again inside its own Maven stage (tests skipped there:
        // they already ran above), so the image never depends on the agent's target/.
        sh """
          docker build \
            -t ${IMAGE}:${TAG} \
            -t ${IMAGE}:latest \
            .
        """
      }
    }

    stage('Trivy FS Scan') {
      steps {
        sh '''
          # Every trivy call below ends in "|| true", so without this a missing trivy binary
          # passes every scan stage and the quality gate reports zero vulnerabilities.
          command -v trivy >/dev/null || {
            echo "trivy is not installed on the Jenkins agent — the security scans cannot run"
            exit 1
          }

          trivy fs --format table \
            --severity ${TRIVY_SEVERITY} \
            --exit-code ${TRIVY_EXIT_CODE} \
            --scanners vuln,secret,misconfig \
            . > trivy-fs-report.txt 2>&1 || true

          trivy fs --format json \
            --severity ${TRIVY_SEVERITY} \
            --output trivy-fs-report.json \
            --scanners vuln,secret,misconfig \
            . || true

          echo "=== Trivy FS Scan Results ==="
          cat trivy-fs-report.txt
        '''
      }
    }

    stage('Trivy Image Scan') {
      steps {
        sh '''
          trivy image --format table \
            --severity ${TRIVY_SEVERITY} \
            --exit-code ${TRIVY_EXIT_CODE} \
            --scanners vuln \
            ${IMAGE}:${TAG} > trivy-image-report.txt 2>&1 || true

          trivy image --format json \
            --severity ${TRIVY_SEVERITY} \
            --output trivy-image-report.json \
            --scanners vuln \
            ${IMAGE}:${TAG} || true

          trivy image --format template \
            --template "@/usr/share/trivy/templates/html.tpl" \
            --severity ${TRIVY_SEVERITY} \
            --output trivy-image-report.html \
            ${IMAGE}:${TAG} || true

          echo "=== Trivy Image Scan Results ==="
          cat trivy-image-report.txt
        '''
      }
    }

    stage('Trivy Scan Quality Gate') {
      steps {
        script {
          def criticalVulns = sh(
            script: '''
              COUNT=$(trivy image --format json \
                --severity CRITICAL \
                --scanners vuln \
                ${IMAGE}:${TAG} 2>/dev/null | \
                grep -c '"Severity": "CRITICAL"' || true)
              echo "${COUNT:-0}"
            ''',
            returnStdout: true
          ).trim()

          def vulnCount = criticalVulns.isInteger() ? criticalVulns.toInteger() : 0

          if (vulnCount > 0) {
            echo "WARNING: Found ${vulnCount} CRITICAL vulnerabilities!"
            if (params.FAIL_ON_VULNERABILITIES) {
              error "Failing build: ${vulnCount} CRITICAL vulnerabilities (FAIL_ON_VULNERABILITIES is ticked)"
            }
          } else {
            echo "No CRITICAL vulnerabilities found."
          }
        }
      }
    }

    stage('Deploy') {
      when { expression { return params.DEPLOY } }
      steps {
        // Single quotes: the shell expands the secrets, so they never appear in the
        // Groovy-interpolated command Jenkins logs. "-e NAME" with no value passes the
        // variable through without printing it.
        sh '''
          set -e

          # Preflight. Nothing is torn down until the port is known to be ours to take, so a
          # port held by something else fails the build instead of taking the running API —
          # and every UI request with it — down.
          OTHERS=$(docker ps --filter "publish=${HOST_PORT}" --format '{{.Names}}' | grep -vx "${CONTAINER}" || true)
          if [ -n "$OTHERS" ]; then
            echo "port ${HOST_PORT} is already published by container(s): $OTHERS"
            echo "the running ${CONTAINER} has been left untouched — free that port or pick another HOST_PORT"
            exit 1
          fi

          # With our own container not running, any listener on the port belongs to something
          # outside Docker.
          if [ -z "$(docker ps -q -f name="^${CONTAINER}$")" ]; then
            if ss -ltn 2>/dev/null | grep -q ":${HOST_PORT} "; then
              echo "port ${HOST_PORT} is held by a process on the host, not by Docker:"
              ss -ltnp 2>/dev/null | grep ":${HOST_PORT} " || true
              exit 1
            fi
          fi

          # Idempotent: `create` fails once the network exists, the normal case.
          docker network create ${DOCKER_NETWORK} 2>/dev/null || true

          docker rm -f ${CONTAINER} || true

          # docker-proxy can keep the binding for a moment after the container goes. Wait for
          # it to let go rather than racing it.
          for i in $(seq 1 15); do
            ss -ltn 2>/dev/null | grep -q ":${HOST_PORT} " || break
            sleep 1
          done

          export SPRING_DATASOURCE_URL="${DB_BASE_URL%/}/${DB_NAME}?${DB_OPTIONS}"
          export SPRING_DATASOURCE_USERNAME="${DB_USERNAME}"
          export SPRING_DATASOURCE_PASSWORD="${DB_PASSWORD}"

          # HOST_PORT:8080 — the right-hand side is Spring's port INSIDE the container, fixed
          # by SERVER_PORT in the image. Only the left-hand side is yours to choose.
          # -m gives the container a memory limit, which the image's MaxRAMPercentage=75.0
          # sizes the heap against — without it, 75% of the whole host's RAM.
          docker run -d \
            --name ${CONTAINER} \
            --network ${DOCKER_NETWORK} \
            --restart unless-stopped \
            -m 1536m \
            -p ${HOST_PORT}:8080 \
            -e TZ=Asia/Kolkata \
            -e SPRING_DATASOURCE_URL \
            -e SPRING_DATASOURCE_USERNAME \
            -e SPRING_DATASOURCE_PASSWORD \
            -e JWT_SECRET \
            -e LOGIN_PASSWORD_VERIFY \
            ${IMAGE}:${TAG}
        '''
      }
    }

    stage('Health Check') {
      when { expression { return params.DEPLOY } }
      steps {
        // The image's HEALTHCHECK signs in as employee 0 (a real database query) and has a 90s
        // start period, and Spring Boot with JPA can take a while against MySQL, so allow
        // 3 minutes. A container that crashes at startup (bad DB url, missing secret) is
        // restarted over and over — stop at the first restart instead of waiting it out.
        sh '''
          set +x
          for i in $(seq 1 36); do
            RESTARTS=$(docker inspect -f '{{.RestartCount}}' ${CONTAINER} 2>/dev/null || echo 0)
            if [ "$RESTARTS" -gt 0 ]; then
              echo "${CONTAINER} crashed during startup and has restarted $RESTARTS time(s)"
              break
            fi
            status=$(docker inspect -f '{{.State.Health.Status}}' ${CONTAINER} 2>/dev/null || echo missing)
            echo "health: $status"
            [ "$status" = "healthy" ] && exit 0
            [ "$status" = "missing" ] && break
            sleep 5
          done
          echo "container did not become healthy within 3 minutes"
          docker logs --tail 150 ${CONTAINER} || true
          exit 1
        '''
      }
    }

    stage('Smoke Test') {
      when { expression { return params.DEPLOY } }
      steps {
        // Healthy only means Spring answers inside the container. These call the PUBLISHED
        // port from the host. Two checks:
        //
        //  1. DATABASE — a sign-in as employee 0, which does not exist. /login is open and the
        //     lookup is a real query: status_code 500 "User Not Found" means the database
        //     answered. The BODY is checked: this backend answers HTTP 200 to everything and
        //     puts the outcome in "status_code".
        //  2. SECURITY — /device/list/ without a token must be refused with HTTP 401. A 200
        //     there means the API is open to anyone, and the deploy is failed.
        sh '''
          BODY=$(${CURL} -s -X POST "http://localhost:${HOST_PORT}/login/?emp_code=0&emp_pass=smoketest" || true)
          if echo "$BODY" | grep -Eq '"status_code" ?: ?500'; then
            echo "API answering on port ${HOST_PORT}, database reachable"
          else
            echo "API is NOT answering correctly on port ${HOST_PORT}"
            echo "response: $(echo "$BODY" | head -c 300)"
            echo "(status_code 501 = Spring is up but the database query failed — check DB_URL / DB_NAME)"
            docker logs --tail 50 ${CONTAINER}
            exit 1
          fi

          CODE=$(${CURL} -s -o /dev/null -w '%{http_code}' "http://localhost:${HOST_PORT}/device/list/" || true)
          if [ "$CODE" = "401" ]; then
            echo "security on: /device/list/ without a token -> 401"
          else
            echo "SECURITY CHECK FAILED: /device/list/ without a token returned HTTP ${CODE:-nothing}, expected 401"
            exit 1
          fi
        '''
      }
    }

    stage('Verify AMS UI') {
      when { expression { return params.DEPLOY } }
      steps {
        // Opens what a user opens: the UI's login page, and an API call through the UI's /api
        // proxy, which reaches this backend as ams-backend:8080 over ams-net (401 = it got
        // here and security answered). A warning, not a failure: the UI is deployed by its own
        // job and may simply not be up yet, which does not make this backend build bad.
        sh '''
          FAILED=0

          CODE=$(${CURL} -s -o /dev/null -w '%{http_code}' -L --max-time 15 "${UI_URL}/Login/ams/" || true)
          if [ "$CODE" = "200" ]; then
            echo "OK   ${UI_URL}/Login/ams/"
          else
            echo "WARN ${UI_URL}/Login/ams/ answered ${CODE:-nothing}, expected 200"
            FAILED=1
          fi

          CODE=$(${CURL} -s -o /dev/null -w '%{http_code}' --max-time 15 "${UI_URL}/api/device/list/" || true)
          if [ "$CODE" = "401" ]; then
            echo "OK   ${UI_URL}/api -> ${CONTAINER} (401 without a token, as expected)"
          else
            echo "WARN ${UI_URL}/api/device/list/ answered ${CODE:-nothing}, expected 401 — the UI may not reach ${CONTAINER} on ${DOCKER_NETWORK}"
            docker network inspect ${DOCKER_NETWORK} --format '{{range .Containers}}{{.Name}} {{end}}' || true
            FAILED=1
          fi

          if [ "$FAILED" = "1" ]; then
            echo "check that the AMS UI (ui-ams) is deployed and attached to ${DOCKER_NETWORK}, and that UI_URL is right"
          fi
        '''
      }
    }

    stage('Cleanup Old Images') {
      steps {
        sh '''
          # Keep only last 3 builds. An old image still used by a container is skipped rather
          # than failing the build — Docker refuses to remove it, and the deploy has already
          # succeeded by this point.
          docker images ${IMAGE} --format "{{.Tag}}" | grep -E '^[0-9]+$' | sort -rn | tail -n +4 | while read -r tag; do
            docker rmi "${IMAGE}:${tag}" || echo "kept ${IMAGE}:${tag} — still in use by a container"
          done

          # Remove dangling images — only those over a day old, so this never deletes layers
          # another job on this server is building with right now.
          docker image prune -f --filter "until=24h"
        '''
      }
    }
  }

  post {
    success {
      script {
        notify('✅', 'SUCCESS', """
The build was successful!

Job: ${env.JOB_NAME}
Build: ${env.BUILD_URL}
Branch: ${params.BRANCH}
Commit: ${env.GIT_SHA}
Image: ${env.IMAGE}:${env.TAG}
Status: ${params.DEPLOY ? "Deployed and running on port ${env.HOST_PORT} (network ${env.DOCKER_NETWORK})" : 'Built only (DEPLOY unticked)'}
Database: ${env.DB_NAME}

Application URL: ${env.UI_URL}/Login/ams/

Security Scans:
- OWASP Dependency Check: Completed
- Trivy Vulnerability Scan: Completed

View Trivy Report: ${env.BUILD_URL}Trivy_Vulnerability_Report/
View Test Results: ${env.BUILD_URL}testReport/
""")
      }
    }
    failure {
      script {
        notify('❌', 'FAILED', """
The build has failed.

Job: ${env.JOB_NAME}
Build: ${env.BUILD_URL}
Branch: ${params.BRANCH}
Commit: ${env.GIT_SHA}
Status: Failed

Security Scans may have identified issues. Check:
- Trivy Report: ${env.BUILD_URL}Trivy_Vulnerability_Report/
- OWASP Report: ${env.BUILD_URL}artifact/dependency-check-report.html
- Test Results: ${env.BUILD_URL}testReport/

Please check the console output: ${env.BUILD_URL}console
""")
      }
    }
    always {
      script {
        // A failure before any stage ran (e.g. a missing credential in environment) leaves no
        // workspace, and sh would then dump a MissingContextVariableException trace that
        // buries the real error.
        if (env.WORKSPACE) {
          archiveArtifacts artifacts: 'target/*.jar', allowEmptyArchive: true, onlyIfSuccessful: true

          // Archive container logs (2>&1: Spring writes its startup log to stderr)
          sh 'docker logs ${CONTAINER} > docker-logs.txt 2>&1 || true'
          archiveArtifacts artifacts: 'docker-logs.txt', allowEmptyArchive: true

          // Archive Trivy and OWASP reports
          archiveArtifacts artifacts: 'trivy-*.txt, trivy-*.json, trivy-*.html, dependency-check-report.*', allowEmptyArchive: true

          // Publish HTML report (requires HTML Publisher plugin)
          publishHTML(target: [
            allowMissing: true,
            alwaysLinkToLastBuild: true,
            keepAll: true,
            reportDir: '.',
            reportFiles: 'trivy-image-report.html',
            reportName: 'Trivy Vulnerability Report'
          ])

          sh 'docker image prune -f --filter "until=168h" || true'
        } else {
          echo 'Cleanup skipped: the build failed before a workspace existed. See the first ERROR above.'
        }
      }
    }
  }
}

// Mail is best-effort: missing SMTP config must not fail a good deploy.
def notify(String icon, String result, String body) {
  try {
    mail to: env.RECIPIENTS,
         subject: "${icon} Jenkins Build ${result}: ${env.JOB_NAME} [#${env.BUILD_NUMBER}]",
         body: body
  } catch (e) {
    echo "Mail not sent (${e.message}). Configure SMTP under Manage Jenkins > System."
  }
}
