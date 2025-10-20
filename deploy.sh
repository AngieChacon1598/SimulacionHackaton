#!/bin/bash

echo "Desplegando microservicio..."

kubectl apply -f 02-angie-chacon-namespace.yml
kubectl apply -f 02-angie-chacon-secret.yml
kubectl apply -f 02-angie-chacon-service.yml
kubectl apply -f 02-angie-chacon-deployment.yml

echo "Esperando pods..."
kubectl wait --for=condition=ready pod -l app=webflux-ai -n angie-chacon-namespace --timeout=300s

echo "Verificando estado..."
kubectl get pods -n angie-chacon-namespace

echo "Para port-forward ejecuta:"
echo "kubectl port-forward service/angie-chacon-service 8080:8080 -n angie-chacon-namespace"
