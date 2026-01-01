#!/bin/bash
set -e

echo "1. Creating/Checking Kind cluster..."
if ! kind get clusters | grep -q "scheduler-cluster"; then
  kind create cluster --name scheduler-cluster
else
  echo "Cluster 'scheduler-cluster' already exists."
fi

CONTAINER_RUNTIME="docker"
if command -v podman &> /dev/null; then
    CONTAINER_RUNTIME="podman"
    echo "Using Podman as container runtime."
fi

echo "2. Building Docker image..."
$CONTAINER_RUNTIME build -t scheduler:local .

echo "3. Loading image into Kind..."
if [ "$CONTAINER_RUNTIME" = "podman" ]; then
    echo "Saving image to archive for Kind (Podman)..."
    podman save -o scheduler.tar localhost/scheduler:local
    kind load image-archive scheduler.tar --name scheduler-cluster
    rm scheduler.tar
else
    kind load docker-image scheduler:local --name scheduler-cluster
fi

echo "4. Applying Kubernetes manifests..."
# Apply Postgres first and wait for it
kubectl apply -f k8s/postgres.yaml --context kind-scheduler-cluster
kubectl apply -f k8s/rbac.yaml --context kind-scheduler-cluster

echo "Waiting for Postgres to be ready..."
kubectl wait --for=condition=ready pod -l app=postgres --timeout=120s --context kind-scheduler-cluster

# Apply App
kubectl apply -f k8s/deployment.yaml --context kind-scheduler-cluster

echo "Done! Application deployed."
echo "Check status:"
echo "  kubectl get pods --context kind-scheduler-cluster"
echo "View logs:"
echo "  kubectl logs -l app=scheduler-app -f --context kind-scheduler-cluster"
