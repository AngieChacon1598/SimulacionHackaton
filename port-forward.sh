#!/bin/bash

echo "Creando port-forward..."
kubectl port-forward service/angie-chacon-service 8080:8080 -n angie-chacon-namespace
