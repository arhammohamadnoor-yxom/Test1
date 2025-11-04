#!/bin/bash

# School Attendance App Deployment Script
# This script automates the deployment process

set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Function to print colored output
print_status() {
    echo -e "${BLUE}[INFO]${NC} $1"
}

print_success() {
    echo -e "${GREEN}[SUCCESS]${NC} $1"
}

print_warning() {
    echo -e "${YELLOW}[WARNING]${NC} $1"
}

print_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

# Check if Docker is installed
check_docker() {
    if ! command -v docker &> /dev/null; then
        print_error "Docker is not installed. Please install Docker first."
        exit 1
    fi

    if ! command -v docker-compose &> /dev/null; then
        print_error "Docker Compose is not installed. Please install Docker Compose first."
        exit 1
    fi

    print_success "Docker and Docker Compose are installed"
}

# Check environment variables
check_env() {
    if [ ! -f .env ]; then
        print_warning ".env file not found. Creating from .env.example..."
        cp .env.example .env
        print_warning "Please edit .env file with your configuration before running deployment again."
        exit 1
    fi

    # Load environment variables
    source .env

    # Check required variables
    if [ -z "$DB_PASSWORD" ]; then
        print_error "DB_PASSWORD is not set in .env file"
        exit 1
    fi

    print_success "Environment variables are configured"
}

# Build the application
build_app() {
    print_status "Building the application..."
    docker-compose build --no-cache
    print_success "Application built successfully"
}

# Deploy the application
deploy_app() {
    print_status "Deploying the application..."

    # Stop any existing containers
    docker-compose down

    # Start the services
    if [ "$1" = "prod" ]; then
        docker-compose -f docker-compose.yml -f docker-compose.prod.yml up -d
    else
        docker-compose up -d
    fi

    print_success "Application deployed successfully"
}

# Wait for database to be ready
wait_for_db() {
    print_status "Waiting for database to be ready..."

    for i in {1..30}; do
        if docker-compose exec -T postgres pg_isready -U schoolapp -d schoolapp &> /dev/null; then
            print_success "Database is ready"
            return
        fi
        echo -n "."
        sleep 2
    done

    print_error "Database failed to start"
    exit 1
}

# Wait for application to be ready
wait_for_app() {
    print_status "Waiting for application to be ready..."

    for i in {1..60}; do
        if curl -f http://localhost:8080/actuator/health &> /dev/null; then
            print_success "Application is ready"
            return
        fi
        echo -n "."
        sleep 2
    done

    print_error "Application failed to start"
    print_status "Check logs with: docker-compose logs app"
    exit 1
}

# Run health checks
health_check() {
    print_status "Running health checks..."

    # Check database
    if docker-compose exec -T postgres pg_isready -U schoolapp -d schoolapp; then
        print_success "Database health check passed"
    else
        print_error "Database health check failed"
    fi

    # Check application
    if curl -f http://localhost:8080/actuator/health &> /dev/null; then
        print_success "Application health check passed"
    else
        print_error "Application health check failed"
    fi
}

# Show deployment info
show_info() {
    print_status "Deployment Information:"
    echo "----------------------------------------"
    echo "Application URL: http://localhost:8080"
    echo "Database: PostgreSQL 15"
    echo "Demo Accounts:"
    echo "  - Admin: admin@school.com / password"
    echo "  - Teacher: teacher@school.com / password"
    echo "  - Student: student@school.com / password"
    echo "----------------------------------------"
    echo "Useful Commands:"
    echo "  - View logs: docker-compose logs -f"
    echo "  - Stop services: docker-compose down"
    echo "  - Restart: docker-compose restart"
    echo "  - Database shell: docker-compose exec postgres psql -U schoolapp -d schoolapp"
    echo "----------------------------------------"
}

# Cleanup function
cleanup() {
    print_status "Cleaning up..."
    docker-compose down
    docker system prune -f
    print_success "Cleanup completed"
}

# Main deployment function
main() {
    echo "========================================"
    echo "School Attendance App Deployment"
    echo "========================================"

    # Parse command line arguments
    ENVIRONMENT=${1:-dev}

    case $ENVIRONMENT in
        "dev"|"development")
            print_status "Starting development deployment..."
            ;;
        "prod"|"production")
            print_status "Starting production deployment..."
            ;;
        "clean")
            cleanup
            exit 0
            ;;
        *)
            print_error "Invalid environment. Use 'dev' or 'prod'"
            echo "Usage: $0 [dev|prod|clean]"
            exit 1
            ;;
    esac

    # Run deployment steps
    check_docker
    check_env
    build_app
    deploy_app $ENVIRONMENT
    wait_for_db
    wait_for_app
    health_check
    show_info

    print_success "Deployment completed successfully!"
}

# Trap to handle interrupts
trap 'print_error "Deployment interrupted"; exit 1' INT TERM

# Run main function
main "$@"