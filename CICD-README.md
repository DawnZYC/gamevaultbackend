# GameVault Backend - CI/CD Documentation

## 📋 Overview

This document describes the complete CI/CD pipeline for the GameVault Backend project, based on industry best practices from NUS-ISS DevOps Engineering and Automation (S-DOEA) workshops.

## 🏗️ Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                   GitHub Actions CI/CD Pipeline              │
└─────────────────────────────────────────────────────────────┘

📝 Lint (Code Quality)
   ├── Checkstyle (Google Style)
   ├── SpotBugs (Bug Detection)
   └── PMD (Code Quality)

🔒 SAST (Security Analysis)
   ├── OWASP Dependency Check
   └── GitHub CodeQL

🏗️ CI (Build & Test)
   ├── Maven Build
   ├── Unit Tests (JUnit 5)
   ├── Code Coverage (JaCoCo)
   └── Docker Image Build

🐳 Container Scan
   ├── Docker Scout
   ├── Trivy
   └── Snyk (optional)

🚀 CD (Deploy)
   ├── Docker Hub Push
   ├── GitHub Release
   └── Environment Deployment
```

---

## 🔧 Prerequisites

### Required GitHub Secrets

Configure these secrets in your repository settings (`Settings > Secrets and variables > Actions`):

| Secret Name | Description | Required |
|------------|-------------|----------|
| `DOCKERHUB_USERNAME` | Docker Hub username | ✅ Yes |
| `DOCKERHUB_TOKEN` | Docker Hub access token | ✅ Yes |
| `NVD_API_KEY` | NIST NVD API key for dependency check | ⚠️ Recommended |
| `SNYK_TOKEN` | Snyk API token for container scanning | 🔵 Optional |

### Getting API Keys

#### 1. Docker Hub Token
1. Login to [Docker Hub](https://hub.docker.com/)
2. Go to `Account Settings > Security`
3. Click `New Access Token`
4. Copy the token and add to GitHub Secrets

#### 2. NVD API Key
1. Visit [NIST NVD](https://nvd.nist.gov/developers/request-an-api-key)
2. Request an API key (sent to your email)
3. Add to GitHub Secrets as `NVD_API_KEY`

#### 3. Snyk Token (Optional)
1. Sign up at [Snyk](https://snyk.io/)
2. Go to `Account Settings > General`
3. Copy the API token
4. Add to GitHub Secrets as `SNYK_TOKEN`

---

## 🔄 Workflows

### 1. Lint Workflow (`lint.yml`)

**Triggers:**
- Push to `dev/master`, `master`, `main`
- Pull requests to these branches

**What it does:**
- Runs Checkstyle for code style compliance
- Executes SpotBugs for bug detection
- Performs PMD code quality analysis
- Uploads reports as artifacts

**View Results:**
- Go to Actions tab
- Select the workflow run
- Download artifacts

### 2. SAST Scan Workflow (`sast-scan.yml`)

**Triggers:**
- Push to main branches
- Pull requests
- Weekly schedule (Monday 00:00 UTC)

**What it does:**
- Scans dependencies with OWASP Dependency Check
- Performs CodeQL static analysis
- Reports vulnerabilities by severity
- Uploads security reports

**Critical Action Required:**
- If Critical vulnerabilities found, update dependencies immediately
- Review `dependency-check-report.html` artifact

### 3. CI Workflow (`ci.yml`)

**Triggers:**
- Push to main branches
- Pull requests

**What it does:**
- Compiles Java code with Maven
- Runs unit tests with PostgreSQL + Redis test containers
- Generates code coverage report (JaCoCo)
- Builds Docker image (no push)
- Uploads JAR and test reports

**Success Criteria:**
- All tests pass
- Code coverage > 50%
- Docker image builds successfully

### 4. Container Scan Workflow (`container-scan.yml`)

**Triggers:**
- Push to main branches
- Pull requests
- Weekly schedule (Monday 02:00 UTC)

**What it does:**
- Scans Docker image with Docker Scout
- Performs Trivy vulnerability scan
- Optional: Snyk container scan
- Reports CVEs by severity
- Uploads SARIF to Security tab

**View Results:**
- Go to `Security > Code scanning`
- Check for container vulnerabilities

### 5. CD Workflow (`cd.yml`)

**Triggers:**
- Push to `master` branch
- Git tags (e.g., `v1.0.0`)

**What it does:**
- Creates GitHub Release with changelog
- Builds production Docker image
- Pushes to Docker Hub with tags
- Optional: Deploys to staging/production

**Docker Image Tags:**
- `latest` - Latest main branch
- `v1.0.0` - Semantic version tags
- `main-abc1234` - Branch + commit SHA

---

## 🚀 Usage Guide

### Running CI Locally

#### 1. Run Lint Checks
```bash
# Checkstyle
mvn checkstyle:checkstyle

# SpotBugs
mvn compile spotbugs:spotbugs

# PMD
mvn pmd:pmd
```

#### 2. Run Tests
```bash
# Start dependencies
docker-compose up -d

# Run tests
mvn test

# Generate coverage report
mvn jacoco:report
```

#### 3. Build Docker Image
```bash
# Build image
docker build -t gamevault-backend:local .

# Run container
docker run -d -p 8080:8080 \
  -e SPRING_DATASOURCE_URL=jdbc:postgresql://host.docker.internal:12000/gamevault \
  gamevault-backend:local
```

### Deploying to Production

#### Method 1: Using Git Tags (Recommended)
```bash
# Create and push tag
git tag -a v1.0.0 -m "Release version 1.0.0"
git push origin v1.0.0
```

This triggers:
- GitHub Release creation
- Docker image build with version tags
- Production deployment (if configured)

#### Method 2: Manual Docker Deployment
```bash
# Pull latest image
docker pull <your-dockerhub-username>/gamevault-backend:latest

# Stop old container
docker stop gamevault-backend
docker rm gamevault-backend

# Run new container
docker run -d \
  --name gamevault-backend \
  --network gamevault \
  -p 8080:8080 \
  -v $(pwd)/secrets:/app/secrets \
  -e SPRING_DATASOURCE_URL=jdbc:postgresql://postgres:5432/gamevault \
  -e SPRING_DATASOURCE_USERNAME=gamevault_user \
  -e SPRING_DATASOURCE_PASSWORD=gamevault_pass \
  -e SPRING_DATA_REDIS_HOST=redis \
  -e SPRING_DATA_REDIS_PORT=6379 \
  <your-dockerhub-username>/gamevault-backend:latest
```

---

## 📊 Monitoring & Reports

### Code Coverage
- **Tool:** JaCoCo
- **Threshold:** 50% line coverage
- **Location:** `target/site/jacoco/index.html`
- **CI Upload:** Codecov

### Security Reports
- **SAST:** OWASP Dependency Check HTML report
- **Container:** Trivy SARIF + GitHub Security tab
- **Location:** GitHub Actions artifacts

### Code Quality
- **Checkstyle:** Google Java Style Guide
- **SpotBugs:** Max effort, Low threshold
- **PMD:** Default ruleset

---

## 🔧 Troubleshooting

### Common Issues

#### 1. Maven Build Fails
**Problem:** Dependencies not downloading
**Solution:**
```bash
# Clear Maven cache
rm -rf ~/.m2/repository
mvn clean install -U
```

#### 2. Tests Fail in CI
**Problem:** Database connection refused
**Solution:**
- Check GitHub Actions services are healthy
- Verify environment variables in `ci.yml`

#### 3. Docker Build Fails
**Problem:** Out of memory
**Solution:**
```bash
# Increase Docker memory limit
docker build --memory=4g -t gamevault-backend:local .
```

#### 4. Container Scan Errors
**Problem:** Rate limit exceeded
**Solution:**
- Add Docker Hub credentials for higher rate limits
- Use GitHub token for authenticated pulls

---

## 🎯 Best Practices

### 1. Branch Strategy
```
main/master    → Production-ready code
dev/master     → Development integration
feature/*      → Feature branches
hotfix/*       → Critical fixes
```

### 2. Commit Messages
Follow [Conventional Commits](https://www.conventionalcommits.org/):
```
feat: add user authentication
fix: resolve memory leak in file upload
docs: update API documentation
chore: upgrade dependencies
```

### 3. Pull Request Checklist
- [ ] All CI checks pass
- [ ] Code coverage maintained/improved
- [ ] No new security vulnerabilities
- [ ] Documentation updated
- [ ] Tests added for new features

### 4. Security
- Never commit secrets to repository
- Use GitHub Secrets for sensitive data
- Regularly update dependencies
- Review security scan reports weekly

---

## 📚 Workshop References

This CI/CD pipeline is based on:

- **Workshop 4:** Containers and Container Management
- **Workshop 5:** Terraform and Ansible (IaC)
- **Workshop 6:** DevOps in the Cloud (GitHub Actions)
- **Workshop 7:** End-to-end DevOps (Docker Scout)
- **Workshop 8:** Lint, SAST, DAST

---

## 🆘 Support

For issues or questions:
1. Check GitHub Actions logs
2. Review workshop materials
3. Create issue in repository
4. Contact DevOps team

---

## 📝 Changelog

### v1.0.0 (2025-01-XX)
- ✅ Initial CI/CD pipeline setup
- ✅ Lint workflows (Checkstyle, SpotBugs, PMD)
- ✅ SAST scanning (OWASP Dependency Check, CodeQL)
- ✅ Container security scanning (Docker Scout, Trivy)
- ✅ Automated Docker image builds
- ✅ GitHub Release automation

---

**Last Updated:** 2025-01-21
**Maintained by:** GameVault DevOps Team
