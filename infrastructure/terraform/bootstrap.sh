#!/usr/bin/env bash
# Bootstrap — run ONCE before terraform init
# Creates the S3 bucket and DynamoDB table used as Terraform remote backend.
# These resources are intentionally managed outside Terraform (chicken-and-egg problem:
# Terraform needs a backend to store state, but it can't create the backend itself).
#
# Prerequisites: AWS CLI configured with terraform-sandbox credentials
# Usage: ./infrastructure/terraform/bootstrap.sh

set -euo pipefail

BUCKET="zatadev-terraform-state"
TABLE="zatadev-terraform-locks"
REGION="eu-north-1"

echo "Creating S3 bucket for Terraform state..."
aws s3api create-bucket \
  --bucket "$BUCKET" \
  --region "$REGION" \
  --create-bucket-configuration LocationConstraint="$REGION"

echo "Enabling versioning on S3 bucket..."
aws s3api put-bucket-versioning \
  --bucket "$BUCKET" \
  --versioning-configuration Status=Enabled

echo "Blocking public access on S3 bucket..."
aws s3api put-public-access-block \
  --bucket "$BUCKET" \
  --public-access-block-configuration \
    BlockPublicAcls=true,IgnorePublicAcls=true,BlockPublicPolicy=true,RestrictPublicBuckets=true

echo "Bootstrap complete."
echo "S3 bucket : $BUCKET"
echo "Region    : $REGION"
echo ""
echo "You can now run: cd infrastructure/terraform/environments/dev && terraform init"