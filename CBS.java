import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import java.time.LocalTime;
import java.util.*;
import java.io.*;
import java.sql.*;

public class CBS extends Application {
    private List<Cab> availableCabs = new ArrayList<>();
    private TextArea statusArea = new TextArea();

    private static final String DB_URL = "jdbc:postgresql://localhost:5432/cabdb";
    private static final String DB_USER = "postgres";
    private static final String DB_PASS = "p0stgray11"; // <-- change to your real password

    @Override
    public void start(Stage stage) {
        setupCabs();
        Label header = new Label("Cab Booking System");
        header.setStyle("-fx-font-size: 22px; -fx-font-weight: bold; -fx-text-fill: #2c3e50;");

        Label nameLabel = new Label("Name:");
        TextField nameField = new TextField();
        Label pickLabel = new Label("Pickup Location:");
        TextField pickField = new TextField();
        pickField.setPromptText("Enter Pickup Place");
        Label dropLabel = new Label("Drop Location:");
        TextField dropField = new TextField();
        dropField.setPromptText("Enter Drop Place");
        Label carLabel = new Label("Cab Type:");
        ComboBox<String> carTypeBox = new ComboBox<>();
        carTypeBox.getItems().addAll("Standard", "Deluxe", "SUV");
        carTypeBox.setValue("Standard");
        Label paymentLabel = new Label("Payment Option:");
        ComboBox<String> paymentBox = new ComboBox<>();
        paymentBox.getItems().addAll("UPI", "Cash");
        paymentBox.setValue("UPI");

        Button bookButton = new Button("Book Cab");
        bookButton.setStyle("-fx-background-color: #27ae60; -fx-text-fill: white; -fx-font-weight: bold;");
        Button clearButton = new Button("Clear");
        clearButton.setStyle("-fx-background-color: #c0392b; -fx-text-fill: white; -fx-font-weight: bold;");

        statusArea.setEditable(false); statusArea.setPrefRowCount(7);
        statusArea.setStyle("-fx-control-inner-background: #ecf0f1; -fx-font-size: 15px; -fx-text-fill: #2c3e50;");

        GridPane form = new GridPane();
        form.setHgap(12); form.setVgap(14); form.setAlignment(Pos.CENTER);
        form.setPadding(new Insets(20, 24, 24, 24));
        form.add(nameLabel, 0, 0); form.add(nameField, 1, 0);
        form.add(pickLabel, 0, 1); form.add(pickField, 1, 1);
        form.add(dropLabel, 0, 2); form.add(dropField, 1, 2);
        form.add(carLabel, 0, 3); form.add(carTypeBox, 1, 3);
        form.add(paymentLabel, 0, 4); form.add(paymentBox, 1, 4);
        form.add(bookButton, 0, 5); form.add(clearButton, 1, 5);

        VBox root = new VBox(18, header, form, statusArea);
        root.setAlignment(Pos.TOP_CENTER);
        root.setStyle("-fx-background-color: linear-gradient(to bottom, #87cefa 0%, #e5f4ff 100%);");
        root.setPadding(new Insets(10, 20, 20, 20));

        bookButton.setOnAction(e -> {
            String name = nameField.getText().trim();
            String pickup = pickField.getText().trim();
            String drop = dropField.getText().trim();
            String carType = carTypeBox.getValue();
            String paymentOption = paymentBox.getValue();
            if (name.isEmpty() || pickup.isEmpty() || drop.isEmpty()) {
                statusArea.setText("Please fill in all fields.");
                return;
            }
            if (pickup.equalsIgnoreCase(drop)) {
                statusArea.setText("Pickup and drop locations must be different.");
                return;
            }
            try {
                Cab cab = bookCab(name, pickup, drop, carType);
                double fare = calculateFare(carType, pickup, drop);
                String fareInfo = String.format("₹%.2f", fare);
                String otp = generateOTP();
                double toPickupMin = estimateToPickupMinutes();
                double rideMin = estimateRideMinutes(pickup, drop);

                // Payment (logs and UI status)
                makePayment(fare, paymentOption);

                String bookingStatus = 
                    "Booked! " + cab.getFullDescription() +
                    "\nDriver: " + cab.driver.name +
                    "\nRoute: " + pickup + " ⟶ " + drop +
                    "\nFare: " + fareInfo + (isPeakHour() ? " (Peak hour rates applied)" : "") +
                    "\nPayment Option: " + paymentOption +
                    "\nYour OTP: " + otp +
                    String.format("\nCab arrival: %.1f min, ride duration: %.1f min", toPickupMin, rideMin);

                statusArea.setText(bookingStatus + "\nSaving booking, please wait...");

                BookingRecord record = new BookingRecord(name, pickup, drop, carType, cab, fare, otp, cab.driver.name, cab.getFullDescription(), paymentOption);

                javafx.concurrent.Task<Void> bookingTask = new javafx.concurrent.Task<>() {
                    @Override
                    protected Void call() {
                        try {
                            saveBooking(record);        // writes to booking.txt
                            saveBookingToDB(record);    // writes to PostgreSQL bookings table
                            Platform.runLater(() -> statusArea.setText(bookingStatus + "\nBooking saved successfully!"));
                        } catch (Exception ex) {
                            Platform.runLater(() -> statusArea.setText(bookingStatus + "\nError: " + ex.getMessage()));
                        }
                        return null;
                    }
                };
                new Thread(bookingTask).start();

            } catch (NoCabAvailableException ex) {
                statusArea.setText("No " + carType + " cabs available!");
            } catch (Exception ex) {
                statusArea.setText("Error: " + ex.getMessage());
            }
        });

        clearButton.setOnAction(e -> {
            nameField.clear(); pickField.clear(); dropField.clear(); statusArea.clear(); carTypeBox.setValue("Standard"); paymentBox.setValue("UPI");
        });
        stage.setTitle("Cab Booking System");
        stage.setScene(new Scene(root, 570, 470));
        stage.show();
    }

    private void makePayment(double fare, String paymentMethod) {
        Payment payment;
        if ("UPI".equalsIgnoreCase(paymentMethod)) {
            payment = new UPIPayment();
        } else {
            payment = new CashPayment();
        }
        payment.processPayment(fare);
        Platform.runLater(() -> {
            String prev = statusArea.getText();
            String msg = "\n[Payment] Processed ₹" + String.format("%.2f", fare) + " via " + payment.getPaymentType() + ".";
            if (prev == null || prev.isEmpty()) statusArea.setText(msg.trim());
            else statusArea.setText(prev + msg);
        });
    }

    // Writes booking as text entry in booking.txt file (not binary!)
    private void saveBooking(BookingRecord record) throws IOException {
        try (FileWriter fw = new FileWriter("booking.txt", true);
             BufferedWriter bw = new BufferedWriter(fw);
             PrintWriter out = new PrintWriter(bw)) {
            out.println(formatBookingRecord(record));
        }
    }

    private String formatBookingRecord(BookingRecord record) {
        return String.join(" | ",
            "Name: " + record.name,
            "Pickup: " + record.pickup,
            "Drop: " + record.drop,
            "Type: " + record.type,
            "Cab: " + record.carDescription,
            "Fare: ₹" + String.format("%.2f", record.fare),
            "OTP: " + record.otp,
            "Driver: " + record.driverName,
            "Payment: " + record.paymentOption
        );
    }

    private void saveBookingToDB(BookingRecord record) {
        try (Connection con = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS)) {
            String sql = "INSERT INTO bookings (name, pickup, drop_loc, cab_type, car_description, fare, otp, driver, payment) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
            try (PreparedStatement stmt = con.prepareStatement(sql)) {
                stmt.setString(1, record.name);
                stmt.setString(2, record.pickup);
                stmt.setString(3, record.drop);
                stmt.setString(4, record.type);
                stmt.setString(5, record.carDescription);
                stmt.setDouble(6, record.fare);
                stmt.setString(7, record.otp);
                stmt.setString(8, record.driverName);
                stmt.setString(9, record.paymentOption);
                stmt.executeUpdate();
            }
            System.out.println("Saved booking in database for customer: " + record.name);
        } catch (Exception e) {
            e.printStackTrace();
            Platform.runLater(() -> statusArea.setText(statusArea.getText() + "\n[DB Error: " + e.getMessage() + "]"));
        }
    }

    private void setupCabs() {
        availableCabs.add(new StandardCab("MH12AB1234", new Driver("Amit"), "White", "Maruti Suzuki"));
        availableCabs.add(new DeluxeCab("MH02CD5678", new Driver("Sunita"), "Grey", "WagonR"));
        availableCabs.add(new SUVCab("MH03EF1122", new Driver("Raj"), "Black", "Hyundai Creta"));
    }

    private Cab bookCab(String user, String pickup, String drop, String carType) throws NoCabAvailableException {
        for (Iterator<Cab> it = availableCabs.iterator(); it.hasNext();) {
            Cab cab = it.next();
            if (cab.getType().equalsIgnoreCase(carType)) {
                it.remove();
                cab.bookedBy = user; cab.pickupLocation = pickup; cab.dropLocation = drop;
                return cab;
            }
        }
        throw new NoCabAvailableException();
    }

    private double calculateFare(String cabType, String pickup, String drop) {
        int baseDistance = Math.abs(pickup.length() - drop.length()) + 5;
        double rate;
        switch (cabType) {
            case "Standard": rate = 10.0; break;
            case "Deluxe":   rate = 15.0; break;
            case "SUV":      rate = 20.0; break;
            default:          rate = 10.0;
        }
        double multiplier = isPeakHour() ? 1.5 : 1.0;
        return baseDistance * rate * multiplier;
    }
    private boolean isPeakHour() {
        int hour = LocalTime.now().getHour();
        return (hour >= 18 && hour < 21);
    }
    private String generateOTP() {
        Random r = new Random();
        int otp = 100000 + r.nextInt(900000);
        return String.valueOf(otp);
    }
    private double estimateToPickupMinutes() {
        Random r = new Random();
        int dist = 1 + r.nextInt(5); // 1–5 km
        double speed = 20.0; // km/h
        return (dist / speed) * 60;
    }
    private double estimateRideMinutes(String pickup, String drop) {
        int dist = Math.abs(pickup.length() - drop.length()) + 5; // as above
        double speed = 30.0; // km/h
        return (dist / speed) * 60;
    }
    public static void main(String[] args) { launch(args); }
}

/* Cab, Drivers & Bookings data classes */

abstract class Cab implements Serializable {
    String licensePlate, bookedBy, pickupLocation, dropLocation, color, model;
    Driver driver;
    public Cab(String lp, Driver dr, String color, String model) {
        licensePlate = lp; driver = dr; this.color = color; this.model = model;
    }
    public abstract String getType();
    public String getFullDescription() {
        return color + " " + model + " (" + getType() + ", Plate: " + licensePlate + ")";
    }
    public String toString() {
        return getFullDescription() + " - Driver: " + driver.name;
    }
}
class StandardCab extends Cab {
    public StandardCab(String lp, Driver dr, String color, String model) {
        super(lp, dr, color, model); }
    public String getType() { return "Standard"; }
}
class DeluxeCab extends Cab {
    public DeluxeCab(String lp, Driver dr, String color, String model) {
        super(lp, dr, color, model); }
    public String getType() { return "Deluxe"; }
}
class SUVCab extends Cab {
    public SUVCab(String lp, Driver dr, String color, String model) {
        super(lp, dr, color, model); }
    public String getType() { return "SUV"; }
}
class Driver implements Serializable {
    String name;
    public Driver(String n) { name = n; }
}
class NoCabAvailableException extends Exception {
    public NoCabAvailableException() { super("No cab available of selected type."); }
}
class BookingRecord implements Serializable {
    String name, pickup, drop, type, otp, driverName, carDescription, paymentOption;
    Cab cab;
    double fare;
    public BookingRecord(String name, String pickup, String drop, String type, Cab cab, double fare, String otp, String driverName, String carDescription, String paymentOption) {
        this.name = name; this.pickup = pickup; this.drop = drop; this.type = type; this.cab = cab;
        this.fare = fare; this.otp = otp; this.driverName = driverName; this.carDescription = carDescription; this.paymentOption = paymentOption;
    }
}

/* Payment interface and implementations */

interface Payment {
    void processPayment(double amount);
    String getPaymentType();
}

class UPIPayment implements Payment {
    @Override
    public void processPayment(double amount) {
        System.out.println("Processing ₹" + String.format("%.2f", amount) + " through UPI...");
    }
    @Override
    public String getPaymentType() { return "UPI"; }
}

class CashPayment implements Payment {
    @Override
    public void processPayment(double amount) {
        System.out.println("Payment of ₹" + String.format("%.2f", amount) + " will be made in cash.");
    }
    @Override
    public String getPaymentType() { return "Cash"; }
}
