// AMS backend (Asset-Tracking-System) — build, package and deploy the Spring Boot container.
// Same pipeline as the TMS backend, with this project's values.
//
// The agent needs Docker only: Maven and the JDK (21) run inside the builder image.
//
// NOTE ON SECRETS: the datasource url, username and password come from the shared Jenkins
// credentials the rest of the fleet already uses — DB_URL, DB_USERNAME and db-password
// (Global > Secret text), the same three the TMS backend reads — plus AMS's own JWT secret,
// 'ams-jwt-secret', which signs the sign-in tokens. They are deliberately NOT in this file. The container overrides only the datasource properties (and the login check
// below) through environment variables; application.properties is left as it is for local
// work, and a database change needs no image rebuild.
//
// If the build fails at the start or at Deploy, check those four entries exist on this
// controller.

pipeline {
  agent any

  triggers {
    githubPush()
  }

  options {
    timestamps()
    disableConcurrentBuilds()
    buildDiscarder(logRotator(numToKeepStr: '15'))
    timeout(time: 45, unit: 'MINUTES')
  }

  environment {
    RECIPIENTS = 'ruchatest4@gmail.com'

    // Image and container name. The CONTAINER name is what the AMS UI proxies to —
    // BACKEND_ORIGIN = 'http://ams-backend:8080' in the AMS_UI Jenkinsfile — so it must not
    // change without changing that too.
    IMAGE      = 'ams-backend'
    CONTAINER  = 'ams-backend'
    TAG        = "${env.BUILD_NUMBER}"

    // Unlike TMS (whose Maven project sits in repl/), pom.xml and this Dockerfile are at the
    // repository root, so everything below builds from the workspace root.

    // The port INSIDE the container: Spring's default, since application.properties sets no
    // server.port. The UI reaches it as ams-backend:8080 over the Docker network.
    CONTAINER_PORT = '8080'

    // Port on the Docker HOST, used only by this pipeline's smoke test and by a human with
    // curl — the UI does not use it. NOT 8080: Jenkins itself normally listens on 8080 on the
    // host. 9098 is the port the AMS UI's .env.example already names for this backend. The
    // Deploy preflight fails the build if anything else holds it.
    HOST_PORT  = '9098'

    // No server.servlet.context-path: the backend serves from the root, which is why the
    // UI's rewrite strips /api (/api/device/list/ -> /device/list/).

    // Docker network shared with the UI container (ui-ams). Container-name DNS only exists
    // on user-defined networks; on the default bridge the UI's proxy fails with
    // "getaddrinfo EAI_AGAIN ams-backend". The UI pipeline creates the same network, so
    // whichever job deploys first brings it into being.
    DOCKER_NETWORK = 'ams-net'

    // Database — SHARED credentials, the same three the TMS backend uses. DB_URL is the base
    // url WITH a trailing slash, so the jdbc url is composed as ${DB_URL}${DB_NAME}. Its host
    // must be one THIS Docker host can route to (see the TMS Jenkinsfile for the full story:
    // not the 192.168.100.30 office address on a public host). The database is on 3309.
    DB_URL      = credentials('DB_URL')
    DB_USERNAME = credentials('DB_USERNAME')
    DB_PASSWORD = credentials('db-password')

    // AMS's own schema (application.properties), NOT db_ems — that one is TMS's.
    DB_NAME     = 'db_asset'

    // Signs the sign-in tokens (JWT) — every API but /login and /logout needs one. AMS's OWN
    // secret, not shared with TMS: a token from one app must not open the other. Create it once
    // as Global > Secret text, id 'ams-jwt-secret', 32+ random characters. If it is missing the
    // build stops here; application.properties' development default is never used.
    JWT_SECRET  = credentials('ams-jwt-secret')

    // Trivy configuration. '1' fails the build on findings; '0' reports and carries on,
    // which is what the rest of the fleet does.
    TRIVY_SEVERITY  = 'CRITICAL,HIGH'
    TRIVY_EXIT_CODE = '0'
  }

  stages {
    // Repo and branch stated here, the same way the AMS UI Jenkinsfile does it, so the
    // pipeline always deploys main whatever branch the job happens to be pointed at.
    // Work goes local-bk-ams -> develop -> main; only main is deployed.
    stage('Checkout') {
      steps {
        git(
          url: 'https://github.com/itswsupport/Asset-Tracking-System.git',
          branch: 'main',
          credentialsId: 'ui-payroll'
        )
        script {
          env.GIT_SHA = sh(script: 'git rev-parse --short HEAD', returnStdout: true).trim()
        }
        echo "Building ${env.IMAGE}:${env.TAG} from ${env.GIT_SHA}"
      }
    }

    stage('Build') {
      steps {
        // --target builder stops after `mvn package`, so a compile failure is reported
        // before any runtime image is assembled.
        sh "docker build --target builder -t ${IMAGE}:builder-${TAG} ."
      }
    }

    stage('OWASP Dependency Check') {
      steps {
        // Same flags as the TMS backend: the build happens inside Docker, so the scan reads
        // pom.xml; target/ and bin/ are stale local build output; and the network analyzers
        // are disabled because they are what make this stage hang on rate-limited lookups.
        // Its own timeout so a stall fails this stage instead of the whole pipeline.
        timeout(time: 10, unit: 'MINUTES') {
          dependencyCheck additionalArguments: """
            --scan .
            --exclude "**/target/**"
            --exclude "**/bin/**"
            --disableCentral
            --disableOssIndex
            --disableNodeAudit
            --disableRetireJS
            --disableAssembly
            --format HTML
            --format XML
            --out .
            --prettyPrint
          """, odcInstallation: 'Dependency-Check'
        }

        dependencyCheckPublisher pattern: 'dependency-check-report.xml'
      }
    }

    stage('Package') {
      steps {
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
          } else {
            echo "No CRITICAL vulnerabilities found."
          }
        }
      }
    }

    stage('Deploy') {
      steps {
        // The settings go into a root-only env-file rather than -e arguments, so the password
        // does not appear in the host process list or the build log; `set +x` for the same
        // reason. Every secret is written \${...} — expanded by the SHELL, not by Groovy —
        // so it never appears in the rendered script.
        //
        // SPRING_DATASOURCE_URL replaces the whole url from application.properties, so the
        // batching options it carries are repeated here (bulk upload relies on
        // rewriteBatchedStatements). useSSL=false and allowPublicKeyRetrieval=true for the
        // same reason as TMS: this MySQL offers only TLS versions a modern JDK has disabled,
        // so the handshake would fail and read like the server is unreachable.
        //
        // LOGIN_PASSWORD_VERIFY=true — NOT optional. application.properties has
        // login.password.verify=false, a local-development switch that lets ANYONE sign in
        // with any password (only the emp_code is checked). This env var overrides it, so
        // the deployed backend always checks passwords whatever the file in git says.
        sh """
          set +x
          umask 077
          cat > .env.deploy <<EOF
SPRING_DATASOURCE_URL=\${DB_URL}\${DB_NAME}?useSSL=false&allowPublicKeyRetrieval=true&rewriteBatchedStatements=true&cachePrepStmts=true&prepStmtCacheSize=250&prepStmtCacheSqlLimit=2048
SPRING_DATASOURCE_USERNAME=\${DB_USERNAME}
SPRING_DATASOURCE_PASSWORD=\${DB_PASSWORD}
LOGIN_PASSWORD_VERIFY=true
JWT_SECRET=\${JWT_SECRET}
EOF

          # Preflight: fail the build, rather than take the backend down, if the host port
          # belongs to some other container or to a process outside Docker (e.g. Jenkins).
          OTHERS=\$(docker ps --filter "publish=${HOST_PORT}" --format '{{.Names}}' | grep -vx "${CONTAINER}" || true)
          if [ -n "\$OTHERS" ]; then
            echo "port ${HOST_PORT} is already published by container(s): \$OTHERS"
            echo "the running ${CONTAINER} has been left untouched — free that port or pick another HOST_PORT"
            rm -f .env.deploy
            exit 1
          fi
          if [ -z "\$(docker ps -q -f name=^${CONTAINER}\$)" ]; then
            if ss -ltn 2>/dev/null | grep -q ":${HOST_PORT} "; then
              echo "port ${HOST_PORT} is held by a process on the host, not by Docker:"
              ss -ltnp 2>/dev/null | grep ":${HOST_PORT} " || true
              rm -f .env.deploy
              exit 1
            fi
          fi

          # Idempotent: `create` fails once the network exists, the normal case after the
          # first build.
          docker network create ${DOCKER_NETWORK} 2>/dev/null || true

          docker rm -f ${CONTAINER} || true

          # -m gives the container a memory limit, which the image's MaxRAMPercentage=75.0
          # sizes the heap against — without it, 75% of the whole host's RAM.
          docker run -d \
            --name ${CONTAINER} \
            --network ${DOCKER_NETWORK} \
            --restart unless-stopped \
            --env-file .env.deploy \
            -m 1536m \
            -p ${HOST_PORT}:${CONTAINER_PORT} \
            ${IMAGE}:${TAG}

          rm -f .env.deploy
        """
      }
    }

    stage('Smoke test') {
      steps {
        // Spring Boot plus JPA takes a while to come up, hence the long window.
        //
        // Two checks, because every API but /login and /logout now needs a sign-in token:
        //
        //  1. DATABASE — a sign-in as employee 0, which does not exist. /login is open and the
        //     lookup is a real query: status_code 500 "User Not Found" means the database
        //     answered; 501 means it did not. The BODY is checked, because this backend answers
        //     HTTP 200 to everything — curl -f alone would pass a backend whose database is
        //     unreachable, the exact trap the TMS smoke test once fell into.
        //  2. SECURITY — /device/list/ without a token must be refused with HTTP 401. A 200
        //     there means the API is open to anyone, and the deploy is failed.
        sh """
          for i in \$(seq 1 120); do
            BODY=\$(curl -fsS -X POST "http://localhost:${HOST_PORT}/login/?emp_code=0&emp_pass=smoketest" 2>/dev/null || true)
            if echo "\$BODY" | grep -Eq '"status_code" ?: ?500'; then
              echo "backend answering, database reachable, after \${i}s"
              CODE=\$(curl -s -o /dev/null -w '%{http_code}' "http://localhost:${HOST_PORT}/device/list/")
              if [ "\$CODE" = "401" ]; then
                echo "security on: /device/list/ without a token -> 401"
                exit 0
              fi
              echo "SECURITY CHECK FAILED: /device/list/ without a token returned HTTP \$CODE, expected 401"
              exit 1
            fi
            sleep 1
          done
          echo "backend did not answer the database check within 120s"
          echo "--- last response ---"
          echo "\$BODY" | head -c 500; echo
          echo "(status_code 501 = Spring is up but the database query failed — check DB_URL / DB_NAME)"
          docker ps -a --filter "name=${CONTAINER}" --format '{{.Names}}\\t{{.Status}}\\t{{.Ports}}'
          docker logs --tail 80 ${CONTAINER}
          exit 1
        """
      }
    }

    stage('Cleanup Old Images') {
      steps {
        sh '''
          # Keep only the last 3 numbered builds. builder-<n> does not match ^[0-9]+$,
          # so it is untouched here — post.always removes it.
          docker images ${IMAGE} --format "{{.Tag}}" | grep -E '^[0-9]+$' | sort -rn | tail -n +4 | xargs -r -I {} docker rmi ${IMAGE}:{}

          # Remove dangling images
          docker image prune -f
        '''
      }
    }
  }

  post {
    success {
      echo "Deployed ${IMAGE}:${TAG} (${env.GIT_SHA}) on port ${HOST_PORT}"
      mail to: "${RECIPIENTS}",
           subject: "Jenkins Build SUCCESS: ${env.JOB_NAME} [#${env.BUILD_NUMBER}]",
           body: """
The build was successful.

Job: ${env.JOB_NAME}
Build: ${env.BUILD_URL}
Commit: ${env.GIT_SHA}
Image: ${IMAGE}:${TAG}
Port: ${HOST_PORT} (container ${CONTAINER_PORT})
Network: ${DOCKER_NETWORK}
Database: ${env.DB_NAME}

Security Scans:
- OWASP Dependency Check: Completed
- Trivy Vulnerability Scan: Completed

View Trivy Report: ${env.BUILD_URL}Trivy_Vulnerability_Report/
"""
    }
    failure {
      mail to: "${RECIPIENTS}",
           subject: "Jenkins Build FAILED: ${env.JOB_NAME} [#${env.BUILD_NUMBER}]",
           body: """
The build has failed.

Job: ${env.JOB_NAME}
Build: ${env.BUILD_URL}
Commit: ${env.GIT_SHA}

Security Scans may have identified issues. Check:
- Trivy Report: ${env.BUILD_URL}Trivy_Vulnerability_Report/
- OWASP Report: ${env.BUILD_URL}artifact/dependency-check-report.html

Console output: ${env.BUILD_URL}console
"""
    }
    always {
      // The env-file holds the database password. Deploy removes it, but a build that fails
      // or is aborted in between would leave it in the workspace.
      sh "rm -f .env.deploy || true"

      // 2>&1 because Spring writes its startup log to stderr.
      sh 'docker logs ${CONTAINER} > docker-logs.txt 2>&1 || true'
      archiveArtifacts artifacts: 'docker-logs.txt', allowEmptyArchive: true

      archiveArtifacts artifacts: 'trivy-*.txt, trivy-*.json, trivy-*.html', allowEmptyArchive: true

      // Requires the HTML Publisher plugin.
      publishHTML(target: [
        allowMissing: true,
        alwaysLinkToLastBuild: true,
        keepAll: true,
        reportDir: '.',
        reportFiles: 'trivy-image-report.html',
        reportName: 'Trivy Vulnerability Report'
      ])

      // The builder image carries the whole Maven dependency tree — don't leave it behind.
      sh "docker image rm -f ${IMAGE}:builder-${TAG} || true"
      sh 'docker image prune -f --filter "until=168h" || true'
    }
  }
}
