import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import java.util.*;
import java.io.*;

public class Chk extends Application {
    private List<Cab> availableCabs = new ArrayList<>();
    private TextArea statusArea = new TextArea();

    @Override
    public void start(Stage stage) {
        setupCabs();

        // --- UI Controls ---
        Label header = new Label("Cab Booking System");
        header.setStyle("-fx-font-size: 22px; -fx-font-weight: bold; -fx-text-fill: #2c3e50;");

        Label nameLabel = new Label("Name:");
        TextField nameField = new TextField();
        Label pickLabel = new Label("Pickup Location:");
        TextField pickField = new TextField();
        Label dropLabel = new Label("Drop Location:");
        TextField dropField = new TextField();

        Label carLabel = new Label("Car Type:");
        ComboBox<String> carTypeBox = new ComboBox<>();
        carTypeBox.getItems().addAll("Standard", "Deluxe", "SUV");
        carTypeBox.setValue("Standard");

        Button bookButton = new Button("Book Cab");
        bookButton.setStyle("-fx-background-color: #27ae60; -fx-text-fill: white; -fx-font-weight: bold;");
        Button clearButton = new Button("Clear");
        clearButton.setStyle("-fx-background-color: #c0392b; -fx-text-fill: white; -fx-font-weight: bold;");

        statusArea.setEditable(false); statusArea.setPrefRowCount(4);
        statusArea.setStyle("-fx-control-inner-background: #ecf0f1; -fx-font-size: 15px; -fx-text-fill: #2c3e50;");

        // --- Layout ---
        GridPane form = new GridPane();
        form.setHgap(12); form.setVgap(14); form.setAlignment(Pos.CENTER);
        form.setPadding(new Insets(20, 24, 24, 24));
        form.add(nameLabel, 0, 0); form.add(nameField, 1, 0);
        form.add(pickLabel, 0, 1); form.add(pickField, 1, 1);
        form.add(dropLabel, 0, 2); form.add(dropField, 1, 2);
        form.add(carLabel, 0, 3); form.add(carTypeBox, 1, 3);
        form.add(bookButton, 0, 4); form.add(clearButton, 1, 4);

        VBox root = new VBox(18, header, form, statusArea);
        root.setAlignment(Pos.TOP_CENTER);
        root.setStyle("-fx-background-color: linear-gradient(to bottom, #87cefa, #f0fff0);");
        root.setPadding(new Insets(10, 20, 20, 20));

        // --- Event Handling ---
        bookButton.setOnAction(e -> {
            String name = nameField.getText().trim();
            String pickup = pickField.getText().trim();
            String drop = dropField.getText().trim();
            String carType = carTypeBox.getValue();

            if (name.isEmpty() || pickup.isEmpty() || drop.isEmpty()) {
                statusArea.setText("Please enter all required details.");
                return;
            }
            try {
                Cab cab = bookCab(name, pickup, drop, carType);
                statusArea.setText("✅ Booked! " + cab + "\nRoute: " + pickup + " ⟶ " + drop);
                saveBooking(name, pickup, drop, carType, cab);
            } catch (NoCabAvailableException ex) {
                statusArea.setText("❌ No " + carType + " cabs available!");
            } catch (Exception ex) {
                statusArea.setText("Error: " + ex.getMessage());
            }
        });
        clearButton.setOnAction(e -> {
            nameField.clear(); pickField.clear(); dropField.clear(); statusArea.clear(); carTypeBox.setValue("Standard");
        });

        // Show window
        stage.setTitle("Cab Booking System");
        stage.setScene(new Scene(root, 430, 420));
        stage.show();
    }

    private void setupCabs() {
        availableCabs.add(new StandardCab("MH12AB1234", new Driver("Amit")));
        availableCabs.add(new DeluxeCab("MH02CD5678", new Driver("Sunita")));
        availableCabs.add(new SUVCab("MH03EF1122", new Driver("Raj")));
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

    private void saveBooking(String name, String pickup, String drop, String type, Cab cab) throws IOException {
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream("booking.ser"))) {
            oos.writeObject(new BookingRecord(name, pickup, drop, type, cab));
        }
    }

    // --- Supporting Classes ---
    public static void main(String[] args) { launch(args); }
}

abstract class Cab implements Serializable {
    String licensePlate, bookedBy, pickupLocation, dropLocation;
    Driver driver;
    public Cab(String lp, Driver dr) { licensePlate = lp; driver = dr; }
    public abstract String getType();
    public String toString() {
        return getType() + " Cab [" + licensePlate + "] (Driver: " + driver.name + ")";
    }
}

class StandardCab extends Cab {
    public StandardCab(String lp, Driver dr) { super(lp, dr); }
    public String getType() { return "Standard"; }
}

class DeluxeCab extends Cab {
    public DeluxeCab(String lp, Driver dr) { super(lp, dr); }
    public String getType() { return "Deluxe"; }
}

class SUVCab extends Cab {
    public SUVCab(String lp, Driver dr) { super(lp, dr); }
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
    String name, pickup, drop, type;
    Cab cab;
    BookingRecord(String name, String pickup, String drop, String type, Cab cab) {
        this.name = name; this.pickup = pickup; this.drop = drop; this.type = type; this.cab = cab;
    }
}
