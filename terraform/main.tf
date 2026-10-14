
terraform {
  required_version = ">= 1.5.0"

  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = "~> 6.0"
    }
  }
}

provider "aws" {
  region = "ap-south-1"
}

# Find the latest Amazon Linux 2023 x86_64 AMI.
data "aws_ami" "amazon_linux" {
  most_recent = true
  owners      = ["amazon"]

  filter {
    name   = "name"
    values = ["al2023-ami-2023.*-x86_64"]
  }

  filter {
    name   = "architecture"
    values = ["x86_64"]
  }

  filter {
    name   = "virtualization-type"
    values = ["hvm"]
  }
}

# Allow SSH only from your current public IP.
resource "aws_security_group" "product_crud" {
  name        = "product-crud-api-sg"
  description = "SSH access for Product CRUD API deployment"

  ingress {
    description = "SSH from my current public IP"
    from_port   = 22
    to_port     = 22
    protocol    = "tcp"
    cidr_blocks = ["103.210.133.103/32"]
  }

  egress {
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }

  tags = {
    Name      = "product-crud-api-sg"
    Project   = "ProductCRUD"
    ManagedBy = "Terraform"
  }
}

# EC2 instance for the Product CRUD API.
resource "aws_instance" "product_crud" {
  ami                         = data.aws_ami.amazon_linux.id
  instance_type               = "t3.micro"
  key_name                    = "ec2-keypair"
  vpc_security_group_ids      = [aws_security_group.product_crud.id]
  associate_public_ip_address = true

  root_block_device {
    volume_size = 8
    volume_type = "gp3"
    encrypted   = true
  }

  user_data = <<-EOF
    #!/bin/bash
    set -euxo pipefail
    dnf install -y docker
    systemctl enable --now docker
    usermod -aG docker ec2-user
  EOF

  user_data_replace_on_change = true

  tags = {
    Name      = "product-crud-api-server"
    Project   = "ProductCRUD"
    ManagedBy = "Terraform"
  }
}



