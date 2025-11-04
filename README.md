# School Attendance and Room Booking Application

A comprehensive school management application for tracking student attendance and booking specialized rooms (science labs, computer labs). The system supports three distinct user roles: students, teachers, and school administrators.

## Features

### Authentication System
- Role-based login (Student, Teacher, Administrator)
- Secure password hashing with BCrypt
- Session management and timeout handling

### Attendance Management
- Daily attendance marking by teachers
- Present/absent checkboxes with notes field
- Bulk "Mark All Present" functionality
- Attendance history and statistics
- Monthly/yearly attendance reports

### Room Booking System
- Room inventory management
- Real-time availability visualization
- Teacher-only booking permissions
- Booking conflict prevention
- Malaysian school room types (Makmal Bio, Kimia, Fizik, etc.)

### User Roles
- **Students**: View attendance, class schedules, and room availability
- **Teachers**: Mark attendance, manage room bookings, view class statistics
- **Administrators**: Full system access, user management, and system configuration

## Technology Stack

- **Backend**: Java 17, Spring Boot 3.2, Spring Security
- **Frontend**: Thymeleaf templates, Bootstrap 5.3, jQuery
- **Database**: PostgreSQL 15 with Flyway migrations
- **Containerization**: Docker and Docker Compose
- **Web Server**: Nginx (reverse proxy)
- **Build Tool**: Maven

## Quick Start

### Prerequisites
- Docker and Docker Compose installed
- Git for cloning the repository

### Installation

1. **Clone the repository**
   ```bash
   git clone <repository-url>
   cd school-attendance-app
   ```

2. **Configure environment variables**
   ```bash
   cp .env.example .env
   # Edit .env file with your configuration
   ```

3. **Deploy the application**
   ```bash
   # For development
   ./scripts/deploy.sh dev

   # For production
   ./scripts/deploy.sh prod
   ```

4. **Access the application**
   - Open your browser and go to `http://localhost:8080`
   - Use demo accounts (see below)

### Demo Accounts

| Role | Email | Password |
|------|-------|----------|
| Administrator | admin@school.com | password |
| Teacher | teacher@school.com | password |
| Student | student@school.com | password |

## Development

### Project Structure
```
src/main/java/com/schoolapp/
├── config/           # Security, database, Thymeleaf configuration
├── controller/       # Web controllers for each role
├── dto/             # Data transfer objects
├── model/           # JPA entities
├── repository/      # Spring Data JPA repositories
├── service/         # Business logic
└── util/           # Utility classes

src/main/resources/
├── db/migration/    # Database migrations
├── templates/       # Thymeleaf templates
├── static/         # CSS, JS, images
└── application*.properties # Configuration files
```

### Running Locally

1. **Using Docker (Recommended)**
   ```bash
   docker-compose up -d
   ```

2. **Using Maven (Local PostgreSQL Required)**
   ```bash
   # Set up PostgreSQL database
   createdb schoolapp

   # Run the application
   mvn spring-boot:run
   ```

### Database

The application uses PostgreSQL with the following tables:
- `users` - User accounts and authentication
- `classes` - Course information
- `class_enrollments` - Student-class relationships
- `rooms` - Room inventory
- `attendance_records` - Attendance tracking
- `room_bookings` - Room reservations
- `user_sessions` - Authentication sessions

## Deployment

### Production Deployment

1. **Configure production environment**
   ```bash
   cp .env.example .env
   # Update with production values
   ```

2. **Deploy with production configuration**
   ```bash
   ./scripts/deploy.sh prod
   ```

3. **Set up SSL certificates**
   ```bash
   # Place certificates in nginx/ssl/
   # fullchain.pem and privkey.pem
   ```

### Environment Variables

Key environment variables to configure:

```bash
DB_PASSWORD=your-secure-password
SPRING_PROFILES_ACTIVE=production
ADMIN_EMAIL=admin@your-school.com
ADMIN_PASSWORD=secure-admin-password
```

### Backup and Recovery

- **Manual Backup**: `./scripts/backup.sh`
- **Automated Backups**: Configured via cron in production
- **Database Restoration**:
  ```bash
  docker-compose exec postgres psql -U schoolapp -d schoolapp < backup.sql
  ```

## Security

- Passwords are hashed using BCrypt
- Session-based authentication with secure cookies
- CSRF protection on all forms
- SQL injection prevention via JPA
- XSS prevention via Thymeleaf auto-escaping
- Rate limiting on login and API endpoints

## API Endpoints

### Authentication
- `POST /login` - User login
- `POST /logout` - User logout
- `GET /register` - Registration page

### Student Endpoints
- `GET /student/dashboard` - Student dashboard
- `GET /student/attendance` - Attendance view
- `GET /student/classes` - Enrolled classes

### Teacher Endpoints
- `GET /teacher/dashboard` - Teacher dashboard
- `POST /teacher/attendance/mark` - Mark attendance
- `GET /teacher/classes` - Teacher's classes

### Admin Endpoints
- `GET /admin/dashboard` - Admin dashboard
- `GET /admin/users` - User management
- `GET /admin/rooms` - Room management

## Monitoring and Health

- **Health Check**: `GET /actuator/health`
- **Application Info**: `GET /actuator/info`
- **Metrics**: `GET /actuator/metrics`

## Troubleshooting

### Common Issues

1. **Database Connection Error**
   - Check PostgreSQL is running
   - Verify database credentials in .env file
   - Ensure database exists

2. **Application Won't Start**
   - Check port 8080 is not in use
   - Review application logs: `docker-compose logs app`
   - Verify Java 17+ is installed

3. **Login Issues**
   - Verify user exists in database
   - Check password is correct
   - Ensure user account is active

### Viewing Logs

```bash
# Application logs
docker-compose logs -f app

# Database logs
docker-compose logs -f postgres

# Nginx logs
docker-compose logs -f nginx
```

## Contributing

1. Fork the repository
2. Create a feature branch
3. Make your changes
4. Add tests if applicable
5. Submit a pull request

## License

This project is licensed under the MIT License - see the LICENSE file for details.

## Support

For support and questions:
- Create an issue in the repository
- Contact the development team
- Check the documentation

## Version History

- **v1.0.0** - Initial release with attendance tracking and room booking
- Features: Role-based authentication, attendance management, room booking system