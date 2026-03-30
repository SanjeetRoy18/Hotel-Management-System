import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.net.URL;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ResourceBundle;
import java.util.function.Predicate;

public class MainController implements Initializable {

    @FXML
    private TableView<Room> table;
    @FXML
    private TextField roomNo;
    @FXML
    private TextField price;
    @FXML
    private TextField bookingRoomNo;
    @FXML
    private TextField customerName;
    @FXML
    private TextField contact;
    @FXML
    private TextField noOfDays;
    @FXML
    private ComboBox<String> type;
    @FXML
    private Label message;
    @FXML
    private Button addRoom;
    @FXML
    private Button deleteRoom;
    @FXML
    private Button generateBill;
    @FXML
    private Button showAvailable;
    @FXML
    private Button showBooked;
    @FXML
    private Button showAll;
    @FXML
    private Button clearFilter;
    @FXML
    private Button book;
    @FXML
    private Button checkout;
    @FXML
    private TextField searchField;
    @FXML
    private Label totalRoomsLabel;
    @FXML
    private Label availableRoomsLabel;
    @FXML
    private Label bookedRoomsLabel;
    @FXML
    private Label totalRevenueLabel;
    @FXML
    private TextField totalRevenueField;

    private ObservableList<Room> rooms = FXCollections.observableArrayList();
    private ObservableList<Customer> customers = FXCollections.observableArrayList();
    private ObservableList<Booking> bookings = FXCollections.observableArrayList();
    private FilteredList<Room> filteredRooms = new FilteredList<>(rooms, room -> true);
    private Predicate<Room> currentFilter = room -> true;
    private String searchText = "";
    private static final String DATA_FILE = "hotel_data.txt";

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        initializeTable();
        type.getItems().addAll("Single", "Double", "Deluxe");
        loadData();

        searchField.textProperty().addListener((obs, oldText, newText) -> applySearch(newText));
        clearFilter.setOnAction(event -> {
            searchField.clear();
            applySearch("");
        });

        table.getSelectionModel().selectedItemProperty().addListener((obs, oldSelection, newSelection) -> {
            if (newSelection != null && bookingRoomNo != null) {
                bookingRoomNo.setText(String.valueOf(newSelection.getRoomNumber()));
            }
        });

        addRoom.setOnAction(event -> addRoom());
        deleteRoom.setOnAction(event -> deleteSelectedRoom());
        generateBill.setOnAction(event -> generateBill());
        showAvailable.setOnAction(event -> applyFilter(Room::isAvailable));
        showBooked.setOnAction(event -> applyFilter(room -> !room.isAvailable()));
        showAll.setOnAction(event -> applyFilter(room -> true));
        book.setOnAction(event -> bookRoom());
        checkout.setOnAction(event -> checkoutRoom());

        refreshFilteredRooms();
        updateSummary();
    }

    private void initializeTable() {
        TableColumn<Room, Integer> col1 = new TableColumn<>("Room No");
        col1.setCellValueFactory(data -> new javafx.beans.property.SimpleIntegerProperty(data.getValue().getRoomNumber()).asObject());

        TableColumn<Room, String> col2 = new TableColumn<>("Type");
        col2.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(data.getValue().getType()));

        TableColumn<Room, Double> col3 = new TableColumn<>("Price");
        col3.setCellValueFactory(data -> new javafx.beans.property.SimpleDoubleProperty(data.getValue().getPrice()).asObject());

        TableColumn<Room, Boolean> col4 = new TableColumn<>("Available");
        col4.setCellValueFactory(data -> new javafx.beans.property.SimpleBooleanProperty(data.getValue().isAvailable()));

        TableColumn<Room, String> col5 = new TableColumn<>("Name");
        col5.setCellValueFactory(data -> {
            Room room = data.getValue();
            if (!room.isAvailable()) {
                for (Booking booking : bookings) {
                    if (booking.getRoom().getRoomNumber() == room.getRoomNumber()) {
                        return new javafx.beans.property.SimpleStringProperty(booking.getCustomer().getName());
                    }
                }
            }
            return new javafx.beans.property.SimpleStringProperty("");
        });

        TableColumn<Room, String> col6 = new TableColumn<>("Contact");
        col6.setCellValueFactory(data -> {
            Room room = data.getValue();
            if (!room.isAvailable()) {
                for (Booking booking : bookings) {
                    if (booking.getRoom().getRoomNumber() == room.getRoomNumber()) {
                        return new javafx.beans.property.SimpleStringProperty(booking.getCustomer().getContact());
                    }
                }
            }
            return new javafx.beans.property.SimpleStringProperty("");
        });

        TableColumn<Room, String> col7 = new TableColumn<>("Bill");
        col7.setCellValueFactory(data -> {
            Room room = data.getValue();
            if (!room.isAvailable()) {
                for (Booking booking : bookings) {
                    if (booking.getRoom().getRoomNumber() == room.getRoomNumber()) {
                        return new javafx.beans.property.SimpleStringProperty(String.format("Rs.%.2f", booking.getTotalCost()));
                    }
                }
            }
            return new javafx.beans.property.SimpleStringProperty("");
        });

        table.getColumns().addAll(col1, col2, col3, col4, col5, col6, col7);
        table.setItems(filteredRooms);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        table.setPlaceholder(new Label("No rooms available"));
    }

    private void addRoom() {
        try {
            int rNo = Integer.parseInt(roomNo.getText());
            double pr = Double.parseDouble(price.getText());
            String selectedType = type.getValue();

            if (selectedType == null || selectedType.isEmpty()) {
                message.setText("Select a room type!");
                return;
            }

            for (Room existing : rooms) {
                if (existing.getRoomNumber() == rNo) {
                    message.setText("Room number already exists!");
                    return;
                }
            }

            rooms.add(new Room(rNo, selectedType, pr, true));
            refreshFilteredRooms();
            message.setText("Room Added!");

            roomNo.clear();
            price.clear();
            type.setValue(null);
        } catch (NumberFormatException ex) {
            message.setText("Invalid Input!");
        }
    }

    private void bookRoom() {
        String name = customerName.getText();
        String phone = contact.getText();
        String daysText = noOfDays.getText();

        String roomText = bookingRoomNo.getText().trim();
        Room room = null;

        if (!roomText.isEmpty()) {
            try {
                int roomNumber = Integer.parseInt(roomText);
                room = findRoomByNumber(roomNumber);
                if (room == null) {
                    message.setText("Room number not found.");
                    return;
                }
            } catch (NumberFormatException ex) {
                message.setText("Invalid room number!");
                return;
            }
        } else {
            room = table.getSelectionModel().getSelectedItem();
        }

        if (room == null) {
            message.setText("Select or enter a room number!");
            return;
        }

        if (!room.isAvailable()) {
            message.setText("Room Already Occupied!");
            return;
        }

        if (name.isEmpty() || phone.isEmpty() || daysText.isEmpty()) {
            message.setText("Enter customer details and days!");
            return;
        }

        int daysToStay;
        try {
            daysToStay = Integer.parseInt(daysText);
            if (daysToStay <= 0) throw new NumberFormatException();
        } catch (NumberFormatException ex) {
            message.setText("Invalid number of days!");
            return;
        }

        Customer customer = new Customer(name, phone, room.getRoomNumber());
        Booking booking = new Booking(customer, room, java.time.LocalDate.now(), java.time.LocalDate.now().plusDays(daysToStay));

        bookings.add(booking);
        customers.add(customer);
        room.setAvailable(false);
        refreshFilteredRooms();

        message.setText("Room Booked! Total Cost: " + booking.getTotalCost());

        customerName.clear();
        contact.clear();
        noOfDays.clear();
        table.refresh();
    }

    private void checkoutRoom() {
        Room selectedRoom = table.getSelectionModel().getSelectedItem();
        if (selectedRoom == null) {
            message.setText("Select a room to checkout!");
            return;
        }

        Booking bookingToRemove = null;
        for (Booking booking : bookings) {
            if (booking.getRoom().getRoomNumber() == selectedRoom.getRoomNumber()) {
                bookingToRemove = booking;
                break;
            }
        }

        if (bookingToRemove == null) {
            message.setText("No booking found for this room!");
            return;
        }

        selectedRoom.setAvailable(true);
        bookings.remove(bookingToRemove);
        refreshFilteredRooms();

        message.setText("Checked Out! Bill: " + bookingToRemove.getTotalCost());
        table.refresh();
    }

    private void deleteSelectedRoom() {
        Room room = table.getSelectionModel().getSelectedItem();
        if (room == null) {
            message.setText("Select a room to delete!");
            return;
        }

        bookings.removeIf(booking -> booking.getRoom().getRoomNumber() == room.getRoomNumber());
        rooms.remove(room);
        refreshFilteredRooms();

        message.setText("Room deleted successfully.");
        table.refresh();
    }

    private void generateBill() {
        Room room = table.getSelectionModel().getSelectedItem();
        if (room == null) {
            message.setText("Select a booked room to generate a bill!");
            return;
        }

        if (room.isAvailable()) {
            message.setText("Select a booked room to generate a bill!");
            return;
        }

        Booking selectedBooking = null;
        for (Booking booking : bookings) {
            if (booking.getRoom().getRoomNumber() == room.getRoomNumber()) {
                selectedBooking = booking;
                break;
            }
        }

        if (selectedBooking == null) {
            message.setText("No booking found for selected room.");
            return;
        }

        String bill = buildBillText(selectedBooking);
        String fileName = String.format("bill_room_%d.txt", room.getRoomNumber());
        try {
            Files.writeString(Paths.get(fileName), bill);
            message.setText("Bill generated: " + fileName);
        } catch (IOException e) {
            message.setText("Failed to write bill file.");
            e.printStackTrace();
        }

        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Generated Bill");
        alert.setHeaderText("Bill for room " + room.getRoomNumber());
        TextArea area = new TextArea(bill);
        area.setEditable(false);
        area.setWrapText(true);
        area.setMaxWidth(Double.MAX_VALUE);
        area.setMaxHeight(Double.MAX_VALUE);
        alert.getDialogPane().setExpandableContent(area);
        alert.getDialogPane().setExpanded(true);
        alert.showAndWait();
    }

    private String buildBillText(Booking booking) {
        Room room = booking.getRoom();
        Customer customer = booking.getCustomer();

        StringBuilder sb = new StringBuilder();
        sb.append("HOTEL BILL\n");
        sb.append("==============================\n");
        sb.append(String.format("Room Number : %d\n", room.getRoomNumber()));
        sb.append(String.format("Room Type   : %s\n", room.getType()));
        sb.append(String.format("Price/Day   : Rs.%.2f\n", room.getPrice()));
        sb.append(String.format("Customer    : %s\n", customer.getName()));
        sb.append(String.format("Contact     : %s\n", customer.getContact()));
        sb.append(String.format("Check-in    : %s\n", booking.getCheckInDate()));
        sb.append(String.format("Check-out   : %s\n", booking.getCheckOutDate()));
        sb.append(String.format("Total Days  : %d\n", booking.getTotalDays()));
        sb.append(String.format("Total Bill  : Rs.%.2f\n", booking.getTotalCost()));
        sb.append("==============================\n");
        sb.append("Thank you for choosing our hotel!\n");
        return sb.toString();
    }

    private void applyFilter(Predicate<Room> predicate) {
        currentFilter = predicate;
        refreshFilteredRooms();
    }

    private void applySearch(String text) {
        searchText = text == null ? "" : text.trim().toLowerCase();
        refreshFilteredRooms();
    }

    private boolean matchesSearch(Room room) {
        if (searchText.isEmpty()) {
            return true;
        }
        String roomNumber = String.valueOf(room.getRoomNumber());
        String customerName = "";
        String customerContact = "";

        if (!room.isAvailable()) {
            for (Booking booking : bookings) {
                if (booking.getRoom().getRoomNumber() == room.getRoomNumber()) {
                    customerName = booking.getCustomer().getName().toLowerCase();
                    customerContact = booking.getCustomer().getContact().toLowerCase();
                    break;
                }
            }
        }

        return roomNumber.contains(searchText)
                || room.getType().toLowerCase().contains(searchText)
                || customerName.contains(searchText)
                || customerContact.contains(searchText);
    }

    private void refreshFilteredRooms() {
        filteredRooms.setPredicate(room -> currentFilter.test(room) && matchesSearch(room));
        updateSummary();
    }

    private void updateSummary() {
        int total = rooms.size();
        long available = rooms.stream().filter(Room::isAvailable).count();
        long booked = total - available;
        double revenue = bookings.stream().mapToDouble(Booking::getTotalCost).sum();

        totalRoomsLabel.setText(String.valueOf(total));
        availableRoomsLabel.setText(String.valueOf(available));
        bookedRoomsLabel.setText(String.valueOf(booked));
        String revenueText = String.format("Rs.%.2f", revenue);
        totalRevenueLabel.setText(revenueText);
        if (totalRevenueField != null) {
            totalRevenueField.setText(revenueText);
        }
    }

    private Room findRoomByNumber(int roomNumber) {
        for (Room room : rooms) {
            if (room.getRoomNumber() == roomNumber) {
                return room;
            }
        }
        return null;
    }

    private void loadData() {
        rooms.clear();
        customers.clear();
        bookings.clear();

        Path path = resolveDataFile();
        if (!Files.exists(path)) {
            message.setText("Data file not found: " + path.toAbsolutePath());
            return;
        }

        try (BufferedReader reader = Files.newBufferedReader(path)) {
            String line;
            boolean readingRooms = false;
            boolean readingBookings = false;

            while ((line = reader.readLine()) != null) {
                if (line.trim().isEmpty()) {
                    continue;
                }

                if (line.equals("ROOMS")) {
                    readingRooms = true;
                    readingBookings = false;
                    continue;
                }

                if (line.equals("BOOKINGS")) {
                    readingRooms = false;
                    readingBookings = true;
                    continue;
                }

                if (readingRooms) {
                    String[] parts = line.split(";", -1);
                    if (parts.length >= 4) {
                        int roomNumber = Integer.parseInt(parts[0]);
                        String typeValue = parts[1];
                        double priceValue = Double.parseDouble(parts[2]);
                        boolean available = Boolean.parseBoolean(parts[3]);
                        rooms.add(new Room(roomNumber, typeValue, priceValue, available));
                    }
                } else if (readingBookings) {
                    String[] parts = line.split(";", -1);
                    if (parts.length >= 5) {
                        int roomNumber = Integer.parseInt(parts[0]);
                        String name = parts[1];
                        String contactValue = parts[2];
                        java.time.LocalDate checkIn = java.time.LocalDate.parse(parts[3]);
                        java.time.LocalDate checkOut = java.time.LocalDate.parse(parts[4]);

                        Room room = findRoomByNumber(roomNumber);
                        if (room != null) {
                            Customer customer = new Customer(name, contactValue, roomNumber);
                            Booking booking = new Booking(customer, room, checkIn, checkOut);
                            bookings.add(booking);
                            customers.add(customer);
                            room.setAvailable(false);
                        }
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        refreshFilteredRooms();
    }

    public void saveData() {
        Path path = resolveDataFile();
        try (BufferedWriter writer = Files.newBufferedWriter(path)) {
            writer.write("ROOMS");
            writer.newLine();
            for (Room room : rooms) {
                writer.write(String.format("%d;%s;%.2f;%b", room.getRoomNumber(), room.getType(), room.getPrice(), room.isAvailable()));
                writer.newLine();
            }

            writer.write("BOOKINGS");
            writer.newLine();
            for (Booking booking : bookings) {
                writer.write(String.format(
                        "%d;%s;%s;%s;%s",
                        booking.getRoom().getRoomNumber(),
                        booking.getCustomer().getName(),
                        booking.getCustomer().getContact(),
                        booking.getCheckInDate(),
                        booking.getCheckOutDate()));
                writer.newLine();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private Path resolveDataFile() {
        Path currentDir = Paths.get("").toAbsolutePath().normalize();

        Path[] directCandidates = new Path[] {
                currentDir.resolve(DATA_FILE),
                currentDir.resolve("..").resolve(DATA_FILE).normalize(),
                currentDir.resolve("..").resolve("..").resolve(DATA_FILE).normalize()
        };

        for (Path candidate : directCandidates) {
            if (Files.exists(candidate)) {
                return candidate;
            }
        }

        Path searchRoot = currentDir;
        for (int depth = 0; depth < 4 && searchRoot != null; depth++) {
            Path found = findDataFileInDirectory(searchRoot);
            if (found != null) {
                return found;
            }
            searchRoot = searchRoot.getParent();
        }

        return currentDir.resolve(DATA_FILE);
    }

    private Path findDataFileInDirectory(Path directory) {
        if (!Files.isDirectory(directory)) {
            return null;
        }

        try (DirectoryStream<Path> stream = Files.newDirectoryStream(directory, DATA_FILE)) {
            for (Path path : stream) {
                return path;
            }
        } catch (IOException ignored) {
        }

        return null;
    }
}
