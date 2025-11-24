# deploy-user-service.ps1
# PowerShell script to deploy/update User Profile Service to Kubernetes

param(
    [string]$Namespace = "openleaf",
    [string]$ImageTag = "latest",
    [string]$ConfigFile = "k8s-user-service-config.yaml",
    [string]$DeploymentFile = "k8s-user-service-deployment.yaml",
    [switch]$WatchRollout,
    [switch]$LoadTest
)

Write-Host "🚢 OpenLeaf User Service Deployment" -ForegroundColor Cyan
Write-Host "====================================" -ForegroundColor Cyan
Write-Host ""

# Check if kubectl is available
if (!(Get-Command kubectl -ErrorAction SilentlyContinue)) {
    Write-Host "❌ kubectl is not installed" -ForegroundColor Red
    exit 1
}

# Check if cluster is accessible
kubectl cluster-info 2>$null | Out-Null
if ($LASTEXITCODE -ne 0) {
    Write-Host "❌ Cannot connect to Kubernetes cluster" -ForegroundColor Red
    Write-Host "   Make sure your cluster is running (k3d or minikube)" -ForegroundColor Yellow
    exit 1
}

Write-Host "✅ Connected to cluster:" -ForegroundColor Green
kubectl config current-context
Write-Host ""

# Check if namespace exists
$namespaceExists = kubectl get namespace $Namespace 2>$null
if ($LASTEXITCODE -ne 0) {
    Write-Host "📦 Creating namespace: $Namespace" -ForegroundColor Yellow
    kubectl create namespace $Namespace
    Write-Host "✅ Namespace created" -ForegroundColor Green
} else {
    Write-Host "✅ Namespace exists: $Namespace" -ForegroundColor Green
}

Write-Host ""

# Apply configuration
if (Test-Path $ConfigFile) {
    Write-Host "⚙️ Applying configuration..." -ForegroundColor Yellow
    kubectl apply -f $ConfigFile

    if ($LASTEXITCODE -eq 0) {
        Write-Host "✅ Configuration applied" -ForegroundColor Green
    } else {
        Write-Host "❌ Failed to apply configuration" -ForegroundColor Red
        exit 1
    }
} else {
    Write-Host "⚠️ Configuration file not found: $ConfigFile" -ForegroundColor Yellow
}

Write-Host ""

# Update image tag if not latest
if ($ImageTag -ne "latest") {
    Write-Host "🏷️ Updating image tag to: $ImageTag" -ForegroundColor Yellow

    # Read, modify, and apply deployment
    $deploymentContent = Get-Content $DeploymentFile -Raw
    $deploymentContent = $deploymentContent -replace 'image: ghcr\.io/[^:]+:[^\s]+', "image: ghcr.io/YOUR_GITHUB_USERNAME/openleaf-user-profile-service:$ImageTag"

    $tempFile = "$DeploymentFile.tmp"
    $deploymentContent | Set-Content $tempFile

    kubectl apply -f $tempFile
    Remove-Item $tempFile
} else {
    # Apply deployment as-is
    Write-Host "🚀 Deploying User Profile Service..." -ForegroundColor Yellow

    if (Test-Path $DeploymentFile) {
        kubectl apply -f $DeploymentFile

        if ($LASTEXITCODE -eq 0) {
            Write-Host "✅ Deployment applied" -ForegroundColor Green
        } else {
            Write-Host "❌ Failed to apply deployment" -ForegroundColor Red
            exit 1
        }
    } else {
        Write-Host "❌ Deployment file not found: $DeploymentFile" -ForegroundColor Red
        exit 1
    }
}

Write-Host ""

# Wait for rollout
Write-Host "⏳ Waiting for rollout to complete..." -ForegroundColor Yellow
kubectl rollout status deployment/user-profile-service -n $Namespace --timeout=5m

if ($LASTEXITCODE -eq 0) {
    Write-Host "✅ Rollout completed successfully" -ForegroundColor Green
} else {
    Write-Host "❌ Rollout failed or timed out" -ForegroundColor Red
    Write-Host ""
    Write-Host "📋 Pod status:" -ForegroundColor Yellow
    kubectl get pods -n $Namespace -l app=user-profile-service
    Write-Host ""
    Write-Host "🔍 Recent events:" -ForegroundColor Yellow
    kubectl get events -n $Namespace --sort-by='.lastTimestamp' | Select-Object -Last 10
    exit 1
}

Write-Host ""

# Show deployment status
Write-Host "📊 Deployment Status" -ForegroundColor Cyan
Write-Host "===================" -ForegroundColor Cyan
Write-Host ""

Write-Host "Pods:" -ForegroundColor Yellow
kubectl get pods -n $Namespace -l app=user-profile-service
Write-Host ""

Write-Host "Service:" -ForegroundColor Yellow
kubectl get svc -n $Namespace user-profile-service
Write-Host ""

Write-Host "HPA:" -ForegroundColor Yellow
kubectl get hpa -n $Namespace user-profile-service-hpa
Write-Host ""

# Get pod health
$healthyPods = kubectl get pods -n $Namespace -l app=user-profile-service --field-selector=status.phase=Running --no-headers 2>$null | Measure-Object -Line | Select-Object -ExpandProperty Lines
$totalPods = kubectl get pods -n $Namespace -l app=user-profile-service --no-headers 2>$null | Measure-Object -Line | Select-Object -ExpandProperty Lines

if ($healthyPods -gt 0) {
    Write-Host "✅ Health: $healthyPods/$totalPods pods running" -ForegroundColor Green
} else {
    Write-Host "⚠️ Health: $healthyPods/$totalPods pods running" -ForegroundColor Yellow
}

Write-Host ""

# Watch rollout if requested
if ($WatchRollout) {
    Write-Host "👀 Watching rollout (press Ctrl+C to stop)..." -ForegroundColor Cyan
    kubectl get pods -n $Namespace -l app=user-profile-service --watch
}

# Run load test if requested
if ($LoadTest) {
    Write-Host ""
    Write-Host "🏋️ Starting load test..." -ForegroundColor Cyan

    if (!(Get-Command k6 -ErrorAction SilentlyContinue)) {
        Write-Host "❌ k6 is not installed. Install with: choco install k6" -ForegroundColor Red
    } else {
        Write-Host "   Port forwarding to 8083..." -ForegroundColor Gray

        # Start port-forward in background
        $portForwardJob = Start-Job -ScriptBlock {
            kubectl port-forward -n openleaf svc/user-profile-service 8083:8083
        }

        Start-Sleep -Seconds 3

        if (Test-Path "k6-load-test.js") {
            Write-Host "   Running k6 test..." -ForegroundColor Gray
            k6 run k6-load-test.js
        } else {
            Write-Host "⚠️ k6-load-test.js not found" -ForegroundColor Yellow
        }

        # Stop port-forward
        Stop-Job $portForwardJob
        Remove-Job $portForwardJob
    }
}

Write-Host ""
Write-Host "✨ Deployment Complete!" -ForegroundColor Green
Write-Host "=======================" -ForegroundColor Green
Write-Host ""
Write-Host "📝 Quick Commands:" -ForegroundColor Cyan
Write-Host ""
Write-Host "View logs:" -ForegroundColor White
Write-Host "  kubectl logs -n $Namespace -l app=user-profile-service --tail=100 -f" -ForegroundColor Gray
Write-Host ""
Write-Host "Port forward:" -ForegroundColor White
Write-Host "  kubectl port-forward -n $Namespace svc/user-profile-service 8083:8083" -ForegroundColor Gray
Write-Host ""
Write-Host "Scale manually:" -ForegroundColor White
Write-Host "  kubectl scale deployment user-profile-service -n $Namespace --replicas=5" -ForegroundColor Gray
Write-Host ""
Write-Host "Watch HPA:" -ForegroundColor White
Write-Host "  kubectl get hpa -n $Namespace --watch" -ForegroundColor Gray
Write-Host ""
Write-Host "Restart deployment:" -ForegroundColor White
Write-Host "  kubectl rollout restart deployment/user-profile-service -n $Namespace" -ForegroundColor Gray
Write-Host ""
Write-Host "Delete deployment:" -ForegroundColor White
Write-Host "  kubectl delete -f $DeploymentFile" -ForegroundColor Gray
Write-Host ""