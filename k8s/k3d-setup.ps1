# k3d-setup-complete.ps1
# PowerShell script to automate k3d cluster setup for OpenLeaf - ALL SERVICES

param(
    [string]$ClusterName = "openleaf-local",
    [int]$Agents = 3,
    [string]$GithubUsername = "",
    [string]$GithubToken = ""
)

Write-Host "OpenLeaf Complete k3d Cluster Setup" -ForegroundColor Cyan
Write-Host "=======================================" -ForegroundColor Cyan
Write-Host ""

# Check prerequisites
Write-Host "[*] Checking prerequisites..." -ForegroundColor Yellow

# Check Docker
if (!(Get-Command docker -ErrorAction SilentlyContinue)) {
    Write-Host "[X] Docker is not installed. Please install Docker Desktop." -ForegroundColor Red
    exit 1
}

# Check if Docker is running
docker ps 2>$null | Out-Null
if ($LASTEXITCODE -ne 0) {
    Write-Host "[X] Docker is not running. Please start Docker Desktop." -ForegroundColor Red
    exit 1
}
Write-Host "[OK] Docker is running" -ForegroundColor Green

# Check k3d
if (!(Get-Command k3d -ErrorAction SilentlyContinue)) {
    Write-Host "[!] k3d is not installed. Installing via Chocolatey..." -ForegroundColor Yellow
    if (Get-Command choco -ErrorAction SilentlyContinue) {
        choco install k3d -y
    } else {
        Write-Host "[X] Chocolatey not found. Please install k3d manually: https://k3d.io/" -ForegroundColor Red
        exit 1
    }
}
Write-Host "[OK] k3d is installed" -ForegroundColor Green

# Check kubectl
if (!(Get-Command kubectl -ErrorAction SilentlyContinue)) {
    Write-Host "[!] kubectl is not installed. Installing via Chocolatey..." -ForegroundColor Yellow
    if (Get-Command choco -ErrorAction SilentlyContinue) {
        choco install kubernetes-cli -y
    } else {
        Write-Host "[X] Please install kubectl manually: https://kubernetes.io/docs/tasks/tools/" -ForegroundColor Red
        exit 1
    }
}
Write-Host "[OK] kubectl is installed" -ForegroundColor Green

Write-Host ""

# Delete existing cluster if it exists
$existingCluster = k3d cluster list | Select-String $ClusterName
if ($existingCluster) {
    Write-Host "[!] Cluster '$ClusterName' already exists. Deleting..." -ForegroundColor Yellow
    k3d cluster delete $ClusterName
    Start-Sleep -Seconds 5
}

# Create cluster
Write-Host "[*] Creating k3d cluster: $ClusterName" -ForegroundColor Yellow
Write-Host "   - Servers: 1" -ForegroundColor Gray
Write-Host "   - Agents: $Agents" -ForegroundColor Gray

k3d cluster create $ClusterName `
    --servers 1 `
    --agents $Agents `
    --api-port 127.0.0.1:6550 `
    --port "8090:80@loadbalancer" `
    --port "8443:443@loadbalancer" `
    --port "8444:8443@loadbalancer" `
    --port "9080:8080@loadbalancer" `
    --wait

if ($LASTEXITCODE -eq 0) {
    Write-Host "[OK] Cluster created successfully" -ForegroundColor Green
} else {
    Write-Host "[X] Failed to create cluster" -ForegroundColor Red
    exit 1
}

Write-Host ""

# Verify cluster
Write-Host "[*] Verifying cluster..." -ForegroundColor Yellow
kubectl cluster-info
Write-Host ""
kubectl get nodes
Write-Host ""

# Create namespace
Write-Host "[*] Creating OpenLeaf namespace..." -ForegroundColor Yellow
kubectl create namespace openleaf 2>$null
if ($LASTEXITCODE -eq 0) {
    Write-Host "[OK] Namespace created" -ForegroundColor Green
} else {
    Write-Host "[!] Namespace already exists" -ForegroundColor Yellow
}

Write-Host ""

# Create image pull secret if credentials provided
if ($GithubUsername -and $GithubToken) {
    Write-Host "[*] Creating GitHub Container Registry pull secret..." -ForegroundColor Yellow

    kubectl create secret docker-registry ghcr-secret `
        --docker-server=ghcr.io `
        --docker-username=$GithubUsername `
        --docker-password=$GithubToken `
        --namespace=openleaf 2>$null

    if ($LASTEXITCODE -eq 0) {
        Write-Host "[OK] Pull secret created" -ForegroundColor Green
    } else {
        Write-Host "[!] Pull secret already exists or failed to create" -ForegroundColor Yellow
    }
} else {
    Write-Host "[!] GitHub credentials not provided. Skipping pull secret creation." -ForegroundColor Yellow
    Write-Host "   Run this manually later:" -ForegroundColor Gray
    Write-Host "   kubectl create secret docker-registry ghcr-secret --docker-server=ghcr.io --docker-username=<USER> --docker-password=<PAT> --namespace=openleaf" -ForegroundColor Gray
}

Write-Host ""

# Deploy infrastructure first
Write-Host "Deploying Infrastructure Components..." -ForegroundColor Cyan
Write-Host "=========================================" -ForegroundColor Cyan
Write-Host ""

if (Test-Path "k8s-infrastructure-deployment.yaml") {
    Write-Host "[*] Deploying PostgreSQL databases, RabbitMQ, and Keycloak..." -ForegroundColor Yellow
    kubectl apply -f k8s-infrastructure-deployment.yaml

    if ($LASTEXITCODE -eq 0) {
        Write-Host "[OK] Infrastructure deployed" -ForegroundColor Green
        Write-Host "[*] Waiting for infrastructure to be ready - 60 seconds..." -ForegroundColor Yellow
        Start-Sleep -Seconds 60
    } else {
        Write-Host "[X] Failed to deploy infrastructure" -ForegroundColor Red
    }
} else {
    Write-Host "[!] k8s-infrastructure-deployment.yaml not found" -ForegroundColor Yellow
}

Write-Host ""

# Deploy backend services
Write-Host "Deploying Backend Services..." -ForegroundColor Cyan
Write-Host "================================" -ForegroundColor Cyan
Write-Host ""

$services = @(
    @{Name="User Profile Service"; ConfigFile="k8s-user-service-config.yaml"; DeployFile="k8s-user-service-deployment.yaml"},
    @{Name="Journal Service"; ConfigFile=""; DeployFile="k8s-journal-service-deployment.yaml"},
    @{Name="Assignment Service"; ConfigFile=""; DeployFile="k8s-assignment-service-deployment.yaml"},
    @{Name="Scheduling Service"; ConfigFile=""; DeployFile="k8s-scheduling-service-deployment.yaml"},
    @{Name="AI Summarization Service"; ConfigFile=""; DeployFile="k8s-ai-summarization-deployment.yaml"}
)

foreach ($service in $services) {
    Write-Host "[*] Deploying $($service.Name)..." -ForegroundColor Yellow

    # Apply config if exists
    if ($service.ConfigFile -and (Test-Path $service.ConfigFile)) {
        Write-Host "   Applying configuration..." -ForegroundColor Gray
        kubectl apply -f $service.ConfigFile
    }

    # Apply deployment
    if (Test-Path $service.DeployFile) {
        Write-Host "   Applying deployment..." -ForegroundColor Gray
        kubectl apply -f $service.DeployFile

        if ($LASTEXITCODE -eq 0) {
            Write-Host "[OK] $($service.Name) deployed" -ForegroundColor Green
        } else {
            Write-Host "[X] Failed to deploy $($service.Name)" -ForegroundColor Red
        }
    } else {
        Write-Host "[!] $($service.DeployFile) not found" -ForegroundColor Yellow
    }

    Write-Host ""
}

# Deploy API Gateway
Write-Host "Deploying API Gateway..." -ForegroundColor Cyan
Write-Host "===========================" -ForegroundColor Cyan
Write-Host ""

if (Test-Path "k8s-api-gateway-config.yaml") {
    Write-Host "[*] Applying API Gateway configuration..." -ForegroundColor Yellow
    kubectl apply -f k8s-api-gateway-config.yaml
}

if (Test-Path "k8s-api-gateway-deployment.yaml") {
    Write-Host "[*] Applying API Gateway deployment..." -ForegroundColor Yellow
    kubectl apply -f k8s-api-gateway-deployment.yaml

    if ($LASTEXITCODE -eq 0) {
        Write-Host "[OK] API Gateway deployed" -ForegroundColor Green
    } else {
        Write-Host "[X] Failed to deploy API Gateway" -ForegroundColor Red
    }
}

Write-Host ""

# Wait for pods to be ready
Write-Host "[*] Waiting for all pods to be ready - this may take 2-3 minutes..." -ForegroundColor Yellow
Start-Sleep -Seconds 30

Write-Host ""
Write-Host "Current Cluster Status:" -ForegroundColor Cyan
Write-Host "=========================" -ForegroundColor Cyan
Write-Host ""

Write-Host "Pods:" -ForegroundColor Yellow
kubectl get pods -n openleaf
Write-Host ""

Write-Host "Services:" -ForegroundColor Yellow
kubectl get svc -n openleaf
Write-Host ""

Write-Host "HPA:" -ForegroundColor Yellow
kubectl get hpa -n openleaf
Write-Host ""

Write-Host "Setup Complete!" -ForegroundColor Green
Write-Host "==================" -ForegroundColor Green
Write-Host ""
Write-Host "Next Steps:" -ForegroundColor Cyan
Write-Host ""
Write-Host "1. Check all pods are running:" -ForegroundColor White
Write-Host "   kubectl get pods -n openleaf -w" -ForegroundColor Gray
Write-Host ""
Write-Host "2. View logs for any service:" -ForegroundColor White
Write-Host "   kubectl logs -n openleaf -l app=SERVICE_NAME --tail=50 -f" -ForegroundColor Gray
Write-Host ""
Write-Host "3. Access API Gateway:" -ForegroundColor White
Write-Host "   kubectl port-forward -n openleaf svc/api-gateway 8443:8443" -ForegroundColor Gray
Write-Host ""
Write-Host "4. Access Keycloak:" -ForegroundColor White
Write-Host "   kubectl port-forward -n openleaf svc/keycloak 8080:8080" -ForegroundColor Gray
Write-Host ""
Write-Host "5. Access RabbitMQ Management:" -ForegroundColor White
Write-Host "   kubectl port-forward -n openleaf svc/rabbitmq 15672:15672" -ForegroundColor Gray
Write-Host ""
Write-Host "6. Watch autoscaling:" -ForegroundColor White
Write-Host "   kubectl get hpa -n openleaf --watch" -ForegroundColor Gray
Write-Host ""
Write-Host "7. View all resources:" -ForegroundColor White
Write-Host "   kubectl get all -n openleaf" -ForegroundColor Gray
Write-Host ""
Write-Host "8. When done, delete cluster:" -ForegroundColor White
Write-Host "   k3d cluster delete $ClusterName" -ForegroundColor Gray
Write-Host ""
Write-Host "Happy Kubernetes deployment!" -ForegroundColor Cyan