# Spring Boot Distributed Scheduler (ShedLock + Postgres)

This project demonstrates a production-ready implementation of a **Distributed Scheduled Job** using Spring Boot, PostgreSQL, and ShedLock. It is designed to run in a clustered Kubernetes environment where multiple replicas of the application exist, but the scheduled task must execute on **only one instance** at a time.

## Key Features

- **Leader Election**: Uses `ShedLock` with a shared PostgreSQL database to ensure that the `@Scheduled` task runs on only **one pod** at any given time.
- **Auto-Failover**: If the leader pod dies or becomes unresponsive, the lock is automatically released after a configured timeout, allowing another instance to take over.
- **Kubernetes Ready**: Includes full manifests and helper scripts for deployment to Kubernetes (OpenShift/Kind).
- **Local Testing**: Built-in support for testing locally using `Kind` (Kubernetes in Docker) with `Podman` or `Docker`.

## Project Structure

```text
├── src/main/java/.../scheduler/task  # The scheduled task implementation
├── src/main/java/.../config          # Configuration for ShedLock and Scheduling
├── src/main/resources                # Application config & DB schema
├── k8s/                              # Kubernetes manifests (App, DB, RBAC)
├── deploy-local.sh                   # Script to automate local deployment
└── Dockerfile                        # Multi-stage Docker build
```

## Prerequisites

- Java 21+
- Docker or Podman
- Kubernetes CLI (`kubectl`)
- Kind (`kind`) - for local testing

## Quick Start (Local Kubernetes)

We have provided a helper script to automate the entire lifecycle of building, configuring, and deploying the application to a local Kind cluster.

### 1. Run the Deployment Script
This script will create a Kind cluster (if missing), build the Docker image, setup a Postgres database, and deploy the application.

```bash
./deploy-local.sh
```

### 2. Verify Deployment
Once the script completes, you can check the status of the pods:

```bash
kubectl get pods --context kind-scheduler-cluster
```

You should see:
- 1 `postgres` pod (Running)
- 2 `scheduler-deployment` pods (Running)

### 3. Observe Leader Election
Tail the logs of all scheduler pods to verify that only one instance is executing the task:

```bash
kubectl logs -l app=scheduler-app -f --context kind-scheduler-cluster
```

**Expected Output:**
You will see logs from *only one* pod executing the task:
```text
[scheduling-1] ...ClusterAwareScheduledTask : Executing scheduled task on this member...
[scheduling-1] ...ClusterAwareScheduledTask : Scheduled task finished.
```
The other pod will remain silent for the scheduled task key.

## Monitoring & Observability

This application exposes REST APIs to monitor the state of the locks and the history of executed jobs.

### 1. View Active Locks
Check which pods currently hold the lock for any scheduled task.

```bash
GET /api/scheduler/locks
```

**Response Example:**
```json
[
  {
    "name": "ClusterAwareScheduledTask_run",
    "lockUntil": "2026-01-01T10:00:30",
    "lockedAt": "2026-01-01T10:00:00",
    "lockedBy": "scheduler-deployment-xyz-123"
  }
]
```

### 2. View Job History
Audit the execution history of all scheduled tasks (Start Time, End Time, Status, Node).

```bash
GET /api/scheduler/history
```

**Response Example:**
```json
[
  {
    "id": 1,
    "jobName": "OneMinuteTask",
    "startTime": "2026-01-01T10:00:00",
    "endTime": "2026-01-01T10:00:01",
    "status": "SUCCESS",
    "executedBy": "scheduler-deployment-xyz-123",
    "errorMessage": null
  }
]
```

### How to Access Locally
Since the pods are in a private cluster network, use `kubectl port-forward` to access them:

```bash
# Forward local port 8080 to the application
kubectl port-forward deployment/scheduler-deployment 8080:8080 --context kind-scheduler-cluster

# Open in browser
http://localhost:8080/api/scheduler/locks
http://localhost:8080/api/scheduler/history
```

## Manual Configuration Details

### Environment Variables
The application is configured via environment variables (defined in `k8s/deployment.yaml`):

| Variable | Description |
|----------|-------------|
| `JDBC_URL` | Connection string for the Postgres DB |
| `JDBC_USERNAME` | Database username |
| `JDBC_PASSWORD` | Database password |
| `SPRING_PROFILES_ACTIVE` | Active Spring profile (e.g., `k8s`) |

### Database Schema
The database requires a `shedlock` table. This is automatically created in the local setup via `src/main/resources/schema.sql`:

```sql
CREATE TABLE IF NOT EXISTS shedlock (
    name VARCHAR(64) NOT NULL,
    lock_until TIMESTAMP NOT NULL,
    locked_at TIMESTAMP NOT NULL,
    locked_by VARCHAR(255) NOT NULL,
    PRIMARY KEY (name)
);

CREATE TABLE IF NOT EXISTS job_history (
    id SERIAL PRIMARY KEY,
    job_name VARCHAR(255) NOT NULL,
    start_time TIMESTAMP NOT NULL,
    end_time TIMESTAMP,
    status VARCHAR(50) NOT NULL,
    executed_by VARCHAR(255) NOT NULL,
    error_message TEXT
);
```

### Task Configuration
The scheduled task can be found in `ClusterAwareScheduledTask.java`.

- `lockAtLeastFor`: Ensures the lock is held for a minimum duration to prevent rapid flicking between nodes (default 15s).
- `lockAtMostFor`: Safety valve. If the node dies, the lock is released after this time (default 50s).

## Production Deployment

To deploy this to a real OpenShift or AWS EKS cluster:

1.  **Database**: Point `JDBC_URL` to your managed database instance (RDS/CloudSQL).
2.  **Secrets**: **Do not** hardcode passwords in `deployment.yaml`. Use Kubernetes Secrets.
3.  **Image**: Push the image to your container registry (ECR, Docker Hub, Artifactory) and update the `image:` field in `k8s/deployment.yaml`.
