#!/usr/bin/env bash

# ------------------------------------------------------------
# teardown.sh – Fully clean up the local development environment
# ------------------------------------------------------------
# This script stops any running port‑forwards, deletes the Kind
# cluster, stops the container runtime (Podman or Docker), removes
# the locally built scheduler image, prunes dangling containers
# and optionally cleans Gradle build artefacts.
# ------------------------------------------------------------

set -euo pipefail

# Helper to print sections nicely
section() {
  echo -e "\n=== $1 ==="
}

# 1️⃣ Kill any active kubectl port‑forward processes
section "Stopping port‑forwards"
# Find PIDs of running port‑forwards for this project
pids=$(ps aux | grep '[k]ubectl port-forward' | awk '{print $2}')
if [[ -n "$pids" ]]; then
  echo "Found port‑forward processes: $pids"
  kill $pids || true
  echo "Port‑forwards stopped."
else
  echo "No port‑forward processes found."
fi

# 2️⃣ Delete the Kind cluster (named scheduler-cluster)
section "Deleting Kind cluster"
if kind get clusters | grep -q '^scheduler-cluster$'; then
  kind delete cluster --name scheduler-cluster
  echo "Kind cluster 'scheduler-cluster' deleted."
else
  echo "Kind cluster 'scheduler-cluster' does not exist."
fi

# 3️⃣ Stop the container runtime (Podman preferred, fallback to Docker)
section "Stopping container runtime"
if command -v podman >/dev/null 2>&1; then
  echo "Stopping Podman service..."
  # Homebrew service stop (common on macOS)
  if brew services list | grep -q '^podman'; then
    brew services stop podman || true
  fi
  # If started manually
  podman system service --stop || true
  echo "Podman stopped."
elif command -v docker >/dev/null 2>&1; then
  echo "Stopping Docker Desktop..."
  osascript -e 'quit app "Docker"' || true
  echo "Docker stopped."
else
  echo "No Podman or Docker binary found – nothing to stop."
fi

# 4️⃣ Remove the local scheduler image
section "Removing local scheduler image"
IMAGE_NAME="localhost/scheduler:local"
if command -v podman >/dev/null 2>&1; then
  if podman images --format '{{.Repository}}:{{.Tag}}' | grep -q "^$IMAGE_NAME$"; then
    podman rmi "$IMAGE_NAME" || true
    echo "Removed image $IMAGE_NAME via Podman."
  else
    echo "Image $IMAGE_NAME not found in Podman."
  fi
elif command -v docker >/dev/null 2>&1; then
  if docker images --format '{{.Repository}}:{{.Tag}}' | grep -q "^$IMAGE_NAME$"; then
    docker rmi "$IMAGE_NAME" || true
    echo "Removed image $IMAGE_NAME via Docker."
  else
    echo "Image $IMAGE_NAME not found in Docker."
  fi
fi

# 5️⃣ Prune any dangling containers, networks, volumes
section "Pruning container system"
if command -v podman >/dev/null 2>&1; then
  podman system prune -a -f || true
  echo "Podman system pruned."
elif command -v docker >/dev/null 2>&1; then
  docker system prune -a -f || true
  echo "Docker system pruned."
fi

# 6️⃣ (Optional) Clean Gradle build artefacts
section "Cleaning Gradle build artefacts"
if [[ -f "./gradlew" ]]; then
  ./gradlew clean
  echo "Gradle clean completed."
else
  echo "gradlew not found – skipping Gradle clean."
fi

echo -e "\nAll cleanup steps completed. Your environment is now clean."
