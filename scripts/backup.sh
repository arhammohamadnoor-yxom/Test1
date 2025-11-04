#!/bin/bash

# Database Backup Script for School Attendance App

set -e

# Configuration
BACKUP_DIR="/app/backups"
DB_NAME="schoolapp"
DB_USER="schoolapp"
DATE=$(date +%Y%m%d_%H%M%S)
BACKUP_FILE="schoolapp_backup_${DATE}.sql"
COMPRESSED_FILE="${BACKUP_FILE}.gz"

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

print_status() {
    echo -e "${BLUE}[INFO]${NC} $1"
}

print_success() {
    echo -e "${GREEN}[SUCCESS]${NC} $1"
}

print_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

# Create backup directory
mkdir -p $BACKUP_DIR

print_status "Starting database backup..."

# Create database backup
docker-compose exec -T postgres pg_dump -U $DB_USER -d $DB_NAME > "${BACKUP_DIR}/${BACKUP_FILE}"

if [ $? -eq 0 ]; then
    print_success "Database backup created: ${BACKUP_DIR}/${BACKUP_FILE}"
else
    print_error "Failed to create database backup"
    exit 1
fi

# Compress backup
gzip "${BACKUP_DIR}/${BACKUP_FILE}"

if [ $? -eq 0 ]; then
    print_success "Backup compressed: ${BACKUP_DIR}/${COMPRESSED_FILE}"
else
    print_error "Failed to compress backup"
    exit 1
fi

# Remove old backups (keep last 30 days)
find $BACKUP_DIR -name "schoolapp_backup_*.sql.gz" -mtime +30 -delete

print_success "Old backups cleaned up"
print_success "Backup process completed successfully!"