package sample;

import javafx.application.Application;
import javafx.stage.Stage;

public class Main extends Application {

    @Override
    public void start(Stage primaryStage) throws Exception {
        Thread thread1 = new Thread(() -> {
            new UserNaming(8080);
        });

        Thread thread2 = new Thread(() -> {
            new UserNaming(8081);
        });

        thread1.start();
        thread2.start();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
