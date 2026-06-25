#!/usr/bin/env bash
# Prerequisites — apply before kubectl apply -k
# These are external manifests managed outside Kustomize

set -euo pipefail

echo "Installing Gateway API CRDs..."
kubectl apply -f infrastructure/kubernetes/base/gateway/gateway-api-crds.yaml

echo "Installing Envoy Gateway..."
kubectl apply -f infrastructure/kubernetes/base/gateway/envoy-gateway-install.yaml

echo "Installing metrics-server..."
kubectl apply -f infrastructure/kubernetes/base/metrics-server/components.yaml

echo "Prerequisites installed."