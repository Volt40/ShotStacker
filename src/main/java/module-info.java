module org.volt4.shotstacker {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.desktop;

    opens org.volt4.shotstacker to javafx.fxml;

    exports org.volt4.shotstacker;
}