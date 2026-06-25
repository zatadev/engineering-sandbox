variable "aws_region" {
  description = "AWS region"
  type        = string
  default     = "eu-north-1"
}

variable "project" {
  description = "Project name — used for resource naming and tagging"
  type        = string
  default     = "engineering-sandbox"
}

variable "environment" {
  description = "Environment name"
  type        = string
  default     = "dev"
}