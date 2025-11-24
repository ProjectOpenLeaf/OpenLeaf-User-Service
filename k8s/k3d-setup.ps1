# k3d-setup.ps1
# PowerShell script to automate k3d cluster setup for OpenLeaf

param(
    [string]$ClusterName = "openleaf-local",
    [int]$Agents = 2,
    [string]$GithubUsername = "",
    [string]$GithubToken = ""
)

Write-Host " OpenLeaf k3d Cluster Setup Script" -ForegroundColor Cyan
Write-Host "=====================================" -ForegroundColor Cyan
Write-Host ""

# Check prerequisites
Write-Host " Checking prerequisites..." -ForegroundColor Yellow

# Check Docker
if (!(Get-Command docker -ErrorAction SilentlyContinue)) {
    Write-Host " Docker is not installed. Please install Docker Desktop." -ForegroundColor Red
    exit 1
}

# Check if Docker is running
docker ps 2>$null | Out-Null
if ($LASTEXITCODE -ne 0) {
    Write-Host " Docker is not running. Please start Docker Desktop." -ForegroundColor Red
    exit 1
}
Write-Host " Docker is running" -ForegroundColor Green

# Check k3d
if (!(Get-Command k3d -ErrorAction SilentlyContinue)) {
    Write-Host "️ k3d is not installed. Installing via Chocolatey..." -ForegroundColor Yellow
    if (Get-Command choco -ErrorAction SilentlyContinue) {
        choco install k3d -y
    } else {
        Write-Host " Chocolatey not found. Please install k3d manually: https://k3d.io/" -ForegroundColor Red
        exit 1
    }
}
Write-Host " k3d is installed" -ForegroundColor Green

# Check kubectl
if (!(Get-Command kubectl -ErrorAction SilentlyContinue)) {
    Write-Host " kubectl is not installed. Installing via Chocolatey..." -ForegroundColor Yellow
    if (Get-Command choco -ErrorAction SilentlyContinue) {
        choco install kubernetes-cli -y
    } else {
        Write-Host " Please install kubectl manually: https://kubernetes.io/docs/tasks/tools/" -ForegroundColor Red
        exit 1
    }
}
Write-Host "✅ kubectl is installed" -ForegroundColor Green

Write-Host ""

# Create cluster
Write-Host " Creating k3d cluster: $ClusterName" -ForegroundColor Yellow
Write-Host "   - Servers: 1" -ForegroundColor Gray
Write-Host "   - Agents: $Agents" -ForegroundColor Gray

k3d cluster create $ClusterName `
    --servers 1 `
    --agents $Agents `
    --api-port 127.0.0.1:6550 `
    --port "8090:80@loadbalancer" `
    --port "8443:443@loadbalancer" `
    --wait

if ($LASTEXITCODE -eq 0) {
    Write-Host " Cluster created successfully" -ForegroundColor Green
} else {
    Write-Host " Failed to create cluster" -ForegroundColor Red
    exit 1
}

Write-Host ""

# Verify cluster
Write-Host " Verifying cluster..." -ForegroundColor Yellow
kubectl cluster-info
Write-Host ""
kubectl get nodes
Write-Host ""

# Create namespace
Write-Host " Creating OpenLeaf namespace..." -ForegroundColor Yellow
kubectl create namespace openleaf 2>$null
if ($LASTEXITCODE -eq 0) {
    Write-Host " Namespace created" -ForegroundColor Green
} else {
    Write-Host "️ Namespace already exists" -ForegroundColor Yellow
}

Write-Host ""

# Create image pull secret if credentials provided
if ($GithubUsername -and $GithubToken) {
    Write-Host " Creating GitHub Container Registry pull secret..." -ForegroundColor Yellow

    kubectl create secret docker-registry ghcr-secret `
        --docker-server=ghcr.io `
        --docker-username=$GithubUsername `
        --docker-password=$GithubToken `
        --namespace=openleaf 2>$null

    if ($LASTEXITCODE -eq 0) {
        Write-Host " Pull secret created" -ForegroundColor Green
    } else {
        Write-Host "⚠ Pull secret already exists or failed to create" -ForegroundColor Yellow
    }
} else
{
    Write-Host " GitHub credentials not provided. Skipping pull secret creation." -ForegroundColor Yellow
    Write-Host "   Run this manually later:" -ForegroundColor Gray
    Write-Host "   kubectl create secret docker-registry ghcr-secret --docker-server=ghcr.io --docker-username=<USER> --docker-password=<PAT> --namespace=openleaf" -ForegroundColor Gray
}

Write-Host ""

# Deploy User Service (if manifests exist)
if (Test-Path "k8s-user-service-config.yaml" -and Test-Path "k8s-user-service-deployment.yaml") {
    Write-Host " Deploying User Profile Service..." -ForegroundColor Yellow

    Write-Host "   Applying configuration..." -ForegroundColor Gray
    kubectl apply -f k8s-user-service-config.yaml

    Write-Host "   Applying deployment..." -ForegroundColor Gray
    kubectl apply -f k8s-user-service-deployment.yaml

    Write-Host " Deployment manifests applied" -ForegroundColor Green
    Write-Host ""

    Write-Host " Waiting for pods to be ready (this may take a minute)..." -ForegroundColor Yellow
    Start-Sleep -Seconds 10
    kubectl get pods -n openleaf
} else {
    Write-Host " Kubernetes manifests not found in current directory" -ForegroundColor Yellow
}

Write-Host ""
Write-Host " Setup Complete!" -ForegroundColor Green
Write-Host "==================" -ForegroundColor Green
Write-Host ""
Write-Host " Next Steps:" -ForegroundColor Cyan
Write-Host "1. Check cluster status:" -ForegroundColor White
Write-Host "   kubectl get all -n openleaf" -ForegroundColor Gray
Write-Host ""
Write-Host "2. View pod logs:" -ForegroundColor White
Write-Host "   kubectl logs -n openleaf -l app=user-profile-service --tail=50 -f" -ForegroundColor Gray
Write-Host ""
Write-Host "3. Port forward to access service:" -ForegroundColor White
Write-Host "   kubectl port-forward -n openleaf svc/user-profile-service 8083:8083" -ForegroundColor Gray
Write-Host ""
Write-Host "4. Watch autoscaling:" -ForegroundColor White
Write-Host "   kubectl get hpa -n openleaf --watch" -ForegroundColor Gray
Write-Host ""
Write-Host "5. When done, delete cluster:" -ForegroundColor White
Write-Host "   k3d cluster delete $ClusterName" -ForegroundColor Gray
Write-Host ""
Write-Host " Happy Kubernetes-ing!" -ForegroundColor Cyan