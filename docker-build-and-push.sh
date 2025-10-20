#!/bin/bash

echo "Construyendo imagen..."
docker build -t 02-angie-chacon:1.0 .

echo "Tag para Docker Hub..."
docker tag 02-angie-chacon:1.0 tu-usuario/02-angie-chacon:1.0

echo "Login a Docker Hub..."
docker login

echo "Subiendo imagen..."
docker push tu-usuario/02-angie-chacon:1.0
