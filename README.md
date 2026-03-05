# StartupFlow - Enhanced JavaFX Startup Funding Management System

## Overview
A comprehensive JavaFX application for managing startup funding applications with advanced features including AI document analysis, email notifications, dashboard analytics, and PDF export capabilities.

## New Features Added

### 1. Email API Integration
- **SendGrid/Gmail SMTP Integration**: Automatic email notifications for application status changes
- **EmailService Class**: Reusable service for sending emails
- **Configuration**: API keys stored securely in `config.properties`
- **Triggers**: Emails sent on approval, rejection, or status updates

### 2. AI Document Analysis
- **PDF Text Extraction**: Uses Apache PDFBox to extract text from attached PDF documents
- **OpenAI Integration**: Sends extracted text to OpenAI API for evaluation
- **Evaluation Criteria**:
  - Business model clarity
  - Financial feasibility
  - Market potential
  - Risk level
- **Results Stored**: AI score (0-100), decision (APPROVE/REVIEW/REJECT), and explanation
- **UI Integration**: "Evaluate with AI" button and results displayed in TableView

### 3. Dashboard & Statistics
- **New Dashboard View**: Comprehensive statistics dashboard
- **Charts Included**:
  - PieChart: Applications grouped by status
  - BarChart: Total funding amount per project
  - LineChart: Applications per month
- **Summary Cards**: Total applications and total funding amount
- **Dynamic Data**: All charts load data dynamically from database

### 4. PDF Export Feature
- **Single Application Export**: Export selected application details to PDF
- **Full List Export**: Export all applications to PDF
- **PDF Generation**: Uses iText library for professional PDF creation
- **Content Includes**: Application details, AI scores, decisions, dates, and status

## Technical Enhancements

### Database Schema Updates
- Added `aiScore` (INT), `aiDecision` (VARCHAR), `aiComment` (TEXT) columns
- Run `database_migration.sql` to update existing database

### New Dependencies Added
```xml
<!-- JavaMail for email -->
<dependency>
    <groupId>com.sun.mail</groupId>
    <artifactId>javax.mail</artifactId>
    <version>1.6.2</version>
</dependency>

<!-- PDFBox for text extraction -->
<dependency>
    <groupId>org.apache.pdfbox</groupId>
    <artifactId>pdfbox</artifactId>
    <version>2.0.29</version>
</dependency>

<!-- iText for PDF generation -->
<dependency>
    <groupId>com.itextpdf</groupId>
    <artifactId>itext7-core</artifactId>
    <version>7.2.5</version>
    <type>pom</type>
</dependency>

<!-- Apache HttpClient for API calls -->
<dependency>
    <groupId>org.apache.httpcomponents.client5</groupId>
    <artifactId>httpclient5</artifactId>
    <version>5.2.1</version>
</dependency>

<!-- Jackson for JSON -->
<dependency>
    <groupId>com.fasterxml.jackson.core</groupId>
    <artifactId>jackson-databind</artifactId>
    <version>2.15.2</version>
</dependency>

<!-- JavaFX Charts -->
<dependency>
    <groupId>org.openjfx</groupId>
    <artifactId>javafx-charts</artifactId>
    <version>17</version>
</dependency>
```

### New Services Created
- **EmailService**: Handles email sending via SMTP
- **AIService**: Manages OpenAI API integration
- **PDFService**: Handles PDF text extraction and generation
- **DashboardService**: Provides statistics and chart data

### UI Enhancements
- Added new TableView columns for AI results
- New buttons: "Evaluate with AI", "Export to PDF", "Approve", "Reject"
- New Dashboard view with charts and statistics
- Enhanced navigation between views

## Setup Instructions

### 1. Database Migration
Run the `database_migration.sql` script on your MySQL database to add the new columns.

### 2. Configuration
Update `src/main/resources/config.properties` with your actual credentials:

```properties
# Email Configuration (Gmail example)
email.smtp.host=smtp.gmail.com
email.smtp.port=587
email.smtp.auth=true
email.smtp.starttls.enable=true
email.username=your-email@gmail.com
email.password=your-app-password

# OpenAI API Configuration
openai.api.key=your-openai-api-key
openai.api.url=https://api.openai.com/v1/chat/completions
```

### 3. Build and Run
```bash
mvn clean compile
mvn javafx:run
```

## Usage Guide

### Managing Applications
1. **Add Applications**: Use the form to add new funding applications
2. **Attach PDFs**: Use the "Browse" button to attach PDF documents
3. **AI Evaluation**: Select an application and click "Evaluate with AI"
4. **Status Updates**: Use "Approve" or "Reject" buttons (sends email notifications)
5. **PDF Export**: Export individual applications or full lists to PDF

### Dashboard Access
- Click "Go to Stats" from the main application view
- View real-time statistics and charts
- Navigate back using "Back to Applications"

## Security Notes
- API keys are stored in `config.properties` (add to .gitignore)
- Email credentials are separate from application code
- PDF processing is done locally (no external uploads)

## Error Handling
- Comprehensive exception handling in all services
- User-friendly error messages via Alert dialogs
- Graceful degradation when APIs are unavailable

## Architecture
- **MVC Pattern**: Maintained across all new features
- **Service Layer**: Separated business logic from controllers
- **Modular Design**: Each feature is self-contained
- **Database Integration**: All new data properly persisted

## Future Enhancements
- Email templates customization
- Advanced AI evaluation criteria
- Real-time dashboard updates
- Multi-language support
- User authentication system