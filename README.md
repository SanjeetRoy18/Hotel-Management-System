# Hotel Management System

A JavaFX-based Hotel Management application developed with Maven. This project demonstrates room management, booking, checkout, and billing using a user-friendly graphical interface and simple file persistence.

## Features

- Add and manage hotel rooms
- Book rooms for customers with contact details and stay duration
- Checkout booked rooms
- Generate and save customer bill details
- View available, booked, and all rooms
- Persistent storage using a text-based data file

## Technologies

- Java 17
- JavaFX 26
- Maven
- FXML layout and CSS styling

## Project Structure

- `pom.xml` - Maven build file and project dependencies
- `src/main/java/` - Java source files
  - `Main.java` - Application entry point
  - `MainController.java` - JavaFX controller and application logic
  - `Room.java`, `Customer.java`, `Booking.java` - domain model classes
- `src/main/resources/` - UI layout and styles
  - `Main.fxml` - FXML user interface
  - `styles.css` - CSS styling for the scene
- `hotel_data.txt` - persisted room and booking data

## Setup and Run

1. Install JDK 17.
2. Install JavaFX SDK 26 for your platform.
3. Open a terminal in the project folder.
4. Build and run with Maven:

```powershell
mvn clean compile javafx:run
```

If your environment requires manual JavaFX module settings, use:

```powershell
java --module-path "path\to\javafx-sdk-26\lib" --add-modules javafx.controls,javafx.fxml -cp target\classes Main
```

## Usage

- Use the Hotel Forms panel to enter room details, customer information, and stay duration.
- Click `Add Room` to save a new room.
- Use `Book Room` to reserve a selected room.
- Use `Checkout` to release a booked room.
- Use `Generate Bill` to create a bill file for the current room.
- Switch between room lists with `Show Available`, `Show Booked Rooms`, and `Show All`.

## Notes

- The application stores data in `hotel_data.txt` so bookings persist across sessions.
- Bill files are written to the project folder as text output.
- This repository is designed for academic demonstration and local desktop use.

