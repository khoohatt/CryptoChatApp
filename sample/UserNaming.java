package sample;

import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.stage.Stage;

import java.io.IOException;

public class UserNaming {
    private final int port;

    public UserNaming(int port) {
        this.port = port;
        Platform.runLater(() -> start(new Stage()));
    }

    public void start(Stage primaryStage) {
        primaryStage.setTitle("вход");

        GridPane nameGrid = createNameInputGrid(primaryStage, this.port);

        Scene scene = new Scene(nameGrid, 320, 150);
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    private GridPane createNameInputGrid(Stage primaryStage, int port) {
        GridPane grid = new GridPane();
        grid.setAlignment(Pos.CENTER);
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(25, 25, 25, 25));

        Label nameLabel = new Label("введите ваше имя:");
        TextField nameInput = new TextField();

        Button confirmButton = new Button("подтвердить!");
        confirmButton.setOnAction(e -> {
            try {
                handleConfirmation(primaryStage, port, nameInput.getText());
            } catch (IOException ioException) {
                ioException.printStackTrace();
            }
        });

        grid.add(nameLabel, 0, 0);
        grid.add(nameInput, 1, 0);
        grid.add(confirmButton, 1, 1);

        return grid;
    }

    public void handleConfirmation(Stage primaryStage, int port, String name) throws IOException {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("../fxml/int.fxml"));
        ChatApp chatApp = new ChatApp(port, name);
        loader.setController(chatApp);

        Parent root = loader.load();
        Scene scene = new Scene(root, 601, 371);
        primaryStage.setTitle("чат (" + name + ")");
        primaryStage.setScene(scene);
        primaryStage.show();

        primaryStage.setOnCloseRequest(event -> chatApp.deleteFromOnlineUsers(port));
    }
}
