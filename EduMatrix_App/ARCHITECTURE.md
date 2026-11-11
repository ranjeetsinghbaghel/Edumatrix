# AuraFace: System Architecture Documentation

## Overview
AuraFace is a role-based college management system engineered using a robust, multi-layered Android architecture that adheres to modern Clean Architecture principles. It is designed to ensure scalability, maintainability, and security for high-traffic college environments. The application is built with Kotlin and follows the Model-View-ViewModel (MVVM) design pattern to decouple UI logic from business operations, providing a seamless user experience across its three primary roles: Admin, Teacher, and Student.

## Core Architecture
The system utilizes the Model-View-ViewModel (MVVM) pattern as its foundational architectural block. This separation allows the UI layer (Activities/Fragments/Composables) to remain lean, focusing solely on data presentation, while the ViewModels handle the logic and state management. The architecture is supported by Hilt, a dependency injection framework that manages the lifecycle of essential components such as network clients, database instances, and repositories. This modular approach reduces boilerplate code and enhances testability.

## Data Management and Persistence
Data integrity is maintained through the Repository Pattern, which serves as a mediator between remote data sources and local storage. The system tracks synchronization using a "Remote-First with Local-Fallback" strategy.
- **Remote Networking**: Retrofit and OkHttp are used for API communication. OkHttp is configured with token interceptors that automatically inject JWT authentication headers into outgoing requests and handle token renewal.
- **Local Persistence**: Room (SQLite) is employed for offline data storage, ensuring that users can access critical information such as schedules and profiles even without an active internet connection.
- **Background Synchronization**: WorkManager is utilized to handle long-running and deferred tasks, such as uploading offline attendance records. This ensures that data is eventually consistent across the system regardless of connectivity issues.

## Security and Authentication
AuraFace implements a multi-tiered security model to protect sensitive academic data.
- **Authentication**: Secure login is facilitated through JWT-based authentication. Upon successful login, the system receives a payload containing the user's role and unique identifiers.
- **Secure Storage**: Sensitive tokens and session information are stored using EncryptedSharedPreferences (Jetpack Security), ensuring they are encrypted at rest using hardware-backed keys.
- **Authorization**: Role-based access control (RBAC) is enforced both at the UI and API levels. The application dynamically adjusts its navigation graph and feature availability based on the authenticated user's role. On the backend, every endpoint validates the user’s role embedded in the JWT to prevent unauthorized data access or privilege escalation.

## Role-Based Workflows
The application differentiates functionality based on user roles to streamline college operations:
- **Teacher Flow**: Teachers can initiate face-recognition-based attendance sessions, record marks, and manage leave requests for their classes.
- **Student Flow**: Students can view their attendance statistics, track their academic performance (marks), and apply for leaves.
- **Admin Flow**: Administrators have full visibility into the system, with tools to manage users, courses, and system-wide configurations.

## Face Recognition Attendance System
The marquee feature of AuraFace is its biometric attendance flow. Utilizing the CameraX API and on-device ML Kit (or cloud embeddings), the system captures and validates student faces in real-time. To prevent fraud, liveness detection is integrated into the flow. The system also supports geofencing, ensuring that attendance can only be marked within the physical boundaries of the classroom.

## Scalability and Reliability
To handle the demands of a real-world college environment, the architecture is designed for vertical and horizontal scaling. By delegating heavy computations (like image processing) to specialized layers and using asynchronous coroutines for all I/O, the app maintains a responsive 60fps UI. The modular DI-driven structure allows new features—such as fee management or library integration—to be added with minimal impact on the existing codebase, ensuring AuraFace remains a future-proof solution for educational institutions.
