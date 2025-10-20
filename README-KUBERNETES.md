# Microservicio WebFlux AI - Angie Chacon

## Archivos de Kubernetes

### 1. Namespace
```bash
kubectl apply -f 02-angie-chacon-namespace.yml
```

### 2. Secret
```bash
kubectl apply -f 02-angie-chacon-secret.yml
```

### 3. Service
```bash
kubectl apply -f 02-angie-chacon-service.yml
```

### 4. Deployment
```bash
kubectl apply -f 02-angie-chacon-deployment.yml
```

## Desplegar

```bash
chmod +x deploy.sh
./deploy.sh
```

## Port-Forward

```bash
chmod +x port-forward.sh
./port-forward.sh
```

## Docker

```bash
chmod +x docker-build-and-push.sh
./docker-build-and-push.sh
```

## Acceso

- API: http://localhost:8080
- Swagger: http://localhost:8080/swagger-ui.html
