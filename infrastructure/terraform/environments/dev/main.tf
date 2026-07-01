# Modules will be wired here as they are created
module "vpc" {
  source = "../../modules/vpc"

  project     = var.project
  environment = var.environment
}
# Phase 7 — ZAT-135: RDS
# Phase 7 — ZAT-136: EKS
# Phase 7 — ZAT-137: IAM