# Implementation Plan - MedGuia Functional Evolution

Evolve the current MedGuia prototype into a fully functional app with local Room persistence, input validations, dynamic alarm configuration, and AlarmManager/NotificationManager alerts.

## User Review Required

> [!IMPORTANT]
> - **Room Database & KSP**: We will add Room dependencies and KSP plugin for schema compilation.
> - **Permissions**: Notifications and exact alarms require runtime permission handling on Android 12+ / 13+.

## Open Questions

- None at this stage.

## Proposed Changes

### Build Configuration
#### [MODIFY] [app/build.gradle.kts](file:///C:/Users/enzo3/AndroidStudioProjects/MedGuiaBase/app/build.gradle.kts)
- Add Room library dependencies (`room-runtime`, `room-ktx`, `room-compiler`) and KSP plugin.

### Data Layer (Room)
#### [NEW] [UsuarioEntity.kt](file:///C:/Users/enzo3/AndroidStudioProjects/MedGuiaBase/app/src/main/java/com/example/medguiabase/data/UsuarioEntity.kt)
- Room entity for users (Doctor and Patient).
#### [NEW] [AlarmeEntity.kt](file:///C:/Users/enzo3/AndroidStudioProjects/MedGuiaBase/app/src/main/java/com/example/medguiabase/data/AlarmeEntity.kt)
- Room entity for medication alarms.
#### [NEW] [UsuarioDao.kt](file:///C:/Users/enzo3/AndroidStudioProjects/MedGuiaBase/app/src/main/java/com/example/medguiabase/data/UsuarioDao.kt)
- DAO for user CRUD operations and authentication.
#### [NEW] [AlarmeDao.kt](file:///C:/Users/enzo3/AndroidStudioProjects/MedGuiaBase/app/src/main/java/com/example/medguiabase/data/AlarmeDao.kt)
- DAO for alarm CRUD operations.
#### [NEW] [AppDatabase.kt](file:///C:/Users/enzo3/AndroidStudioProjects/MedGuiaBase/app/src/main/java/com/example/medguiabase/data/AppDatabase.kt)
- Room database instance singleton.
#### [NEW] [MedGuiaRepository.kt](file:///C:/Users/enzo3/AndroidStudioProjects/MedGuiaBase/app/src/main/java/com/example/medguiabase/data/MedGuiaRepository.kt)
- Repository encapsulating database operations.

### Notifications & Alarm Management
#### [NEW] [AlarmReceiver.kt](file:///C:/Users/enzo3/AndroidStudioProjects/MedGuiaBase/app/src/main/java/com/example/medguiabase/alarm/AlarmReceiver.kt)
- BroadcastReceiver for alarm triggers, handling notifications and retry logic.
#### [NEW] [NotificationHelper.kt](file:///C:/Users/enzo3/AndroidStudioProjects/MedGuiaBase/app/src/main/java/com/example/medguiabase/alarm/NotificationHelper.kt)
- Notification channels and builder.

### UI & ViewModel Updates
#### [MODIFY] [MainActivity.kt](file:///C:/Users/enzo3/AndroidStudioProjects/MedGuiaBase/app/src/main/java/com/example/medguiabase/MainActivity.kt)
- Integrate Room repository and ViewModel / State.
- Implement field validation (`isError`, supportingText).
- Update Patient Login 1 screen to check for existing saved patient in Room.
- Implement functional Alarm configuration screen with TimePickerDialog and emoji grid.
- Implement dynamic Remédios screen calculated from Room alarms and current time.

## Verification Plan

### Automated Tests
- Build project successfully using `gradle_build(app:assembleDebug)`.

### Manual Verification
- Deploy to emulator and test signup, login validation, alarm creation, notification scheduling, and dose confirmation.
