import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import java.io.*;
import java.util.*;

public class CabBookingSystem extends Application {
    private List<Cab> availableCabs = new ArrayList<>();
    private TextArea logArea = new TextArea();

    @Override
    public void start(Stage stage) {
        setupCabs();
        VBox root = new VBox(10);
        root.setStyle("-fx-padding: 15;");
        Label nameLabel = new Label("Enter your name:");
        TextField nameField = new TextField();
        Button bookButton = new Button("Book Cab");
        Button loadButton = new Button("Load Last Booking");
        logArea.setPrefRowCount(8);
        logArea.setEditable(false);

        bookButton.setOnAction(e -> {
            String name = nameField.getText();
            try {
                Cab cab = bookCab(name);
                logArea.appendText("Booked: " + cab + "\n");
                saveBooking(name, cab);
            } catch (NoCabAvailableException ex) {
                logArea.appendText("No cabs available!\n");
            } catch (Exception ex) {
                logArea.appendText("Booking error: " + ex.getMessage() + "\n");
            }
        });
        loadButton.setOnAction(e -> {
            try {
                BookingRecord r = loadBooking();
                logArea.appendText("Last Booking: " + r.name + " used " + r.cab + "\n");
            } catch (Exception ex) {
                logArea.appendText("Load failed: " + ex.getMessage() + "\n");
            }
        });
        root.getChildren().addAll(nameLabel, nameField, bookButton, loadButton, logArea);
        stage.setTitle("Cab Booking System");
        stage.setScene(new Scene(root, 400, 300));
        stage.show();
    }

    private void setupCabs() {
        Driver d1 = new Driver("Amit");
        Driver d2 = new Driver("Sunita");
        availableCabs.add(new StandardCab("MH12AB1234", d1));
        availableCabs.add(new DeluxeCab("MH02CD5678", d2));
    }

    private Cab bookCab(String user) throws NoCabAvailableException {
        if (availableCabs.isEmpty()) throw new NoCabAvailableException();
        Cab cab = availableCabs.remove(0);
        cab.bookedBy = user;
        return cab;
    }

    private void saveBooking(String name, Cab cab) throws IOException {
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream("booking.ser"))) {
            oos.writeObject(new BookingRecord(name, cab));
        }
    }
    private BookingRecord loadBooking() throws Exception {
        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream("booking.ser"))) {
            return (BookingRecord) ois.readObject();
        }
    }

    public static void main(String[] args) { launch(args); }
}

// Abstract class and inheritance
abstract class Cab implements Serializable {
    String licensePlate;
    Driver driver;
    String bookedBy;
    public Cab(String lp, Driver dr) {
        licensePlate = lp; driver = dr;
    }
    public abstract String getType();
    public String toString() {
        return getType() + " [" + licensePlate + "] (Driver: " + driver.name + ")";
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
class Driver implements Serializable {
    String name;
    public Driver(String n) { name = n; }
}
class NoCabAvailableException extends Exception {
    public NoCabAvailableException() { super("No cab available at the moment."); }
}
class BookingRecord implements Serializable {
    String name;
    Cab cab;
    BookingRecord(String name, Cab cab) { this.name = name; this.cab = cab; }
}
