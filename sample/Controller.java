package sample;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.stage.Stage;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.URL;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.ResourceBundle;

public class Controller implements Initializable {



    public void initialize(URL location, ResourceBundle resources) {
//        String name = nameInput.getText();

        int targetPort = 8000;
//        BorderPane root = new BorderPane();

        userList = new ListView<>();
//        messageArea = new TextArea();
//        inputArea = new TextArea();

        try {
            KeyPair keyPair = generateKeys();
            PublicKey publicKey = keyPair.getPublic();
            PrivateKey privateKey = keyPair.getPrivate();
            System.out.println(String.valueOf(InetAddress.getLocalHost()));
//        onlineUsers.add(new User(String.valueOf(InetAddress.getLocalHost()), publicKey, name));

//        sendMessageButton = new Button("Send");
            sendMessageButton.setOnAction(e -> {
                try {
                    for (User user : onlineUsers) {
                        sendMessage(user, messageInputField.getText(), user.getPublicKey(), targetPort);
                    }
                } catch (IOException ioException) {
                    ioException.printStackTrace();
                }
            });

//        sendKeyButton = new Button("Keys");
//        sendKeyButton.setOnAction(e -> {
            try {
                sendPublicKey(publicKey, targetPort);
            } catch (IOException | NoSuchAlgorithmException ioException) {
                ioException.printStackTrace();
            }
//        });

//        listenButton = new Button("Listen");
            try {
                receivePublicKeys("name", targetPort, privateKey);
            } catch (IOException | ClassNotFoundException ioException) {
                ioException.printStackTrace();
            }

        } catch (NoSuchAlgorithmException | UnknownHostException e) {
            e.printStackTrace();
        }
    }

//    private final int port;
    private static final int BUFFER_SIZE = 1024;

    private TextField nameInput;
    private ListView<String> userList;
    //    @FXML
//    private TextArea messageArea;
//    @FXML
//    private TextArea inputArea;
//    @FXML
    public Button sendMessageButton;
    @FXML
    public ListView<String> messageList;
    //    private Button sendKeyButton;
    @FXML
    public TextArea messageInputField;
    @FXML
    public TextArea encryptionField;
    @FXML
    public Button showKeysButton;
    @FXML
    public Button showEncryptedButton;
    @FXML
    public Label onlineUsersField;

    List<User> onlineUsers = new ArrayList<User>();

//    public ChatApp() {
//        this.port = 1;
//        try {
//            init();
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//    }

    private KeyPair generateKeys() throws NoSuchAlgorithmException {
        KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
        keyGen.initialize(2048);
        return keyGen.generateKeyPair();
    }

    private void sendPublicKey(PublicKey publicKey, int targetPort) throws IOException, NoSuchAlgorithmException {
        DatagramSocket socket = new DatagramSocket();
        socket.setBroadcast(true);

        try {
            String encodedKey = Base64.getEncoder().encodeToString(publicKey.getEncoded());
            String string = "KEY:" + encodedKey;
            byte[] data = string.getBytes(StandardCharsets.UTF_8);
            DatagramPacket packet = new DatagramPacket(data, data.length, InetAddress.getByName("255.255.255.255"), targetPort == 8080 ? 8081 : 8080);
            socket.send(packet);

            socket.disconnect();
            socket.close();
            socket.disconnect();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void receivePublicKeys(String name, int targetPort, PrivateKey privateKey) throws IOException, ClassNotFoundException {
//        int[] ports = {8080, 8081};
//        int usedPort = targetPort; //== 8080 ? 8081 : 8080;
        DatagramSocket socket = new DatagramSocket(targetPort);
        new Thread(() -> {
            while (true) {
//                for (int port : ports) {
                try {


                    byte[] buffer = new byte[BUFFER_SIZE];
                    DatagramPacket packet = new DatagramPacket(buffer, buffer.length);

                    socket.receive(packet);

                    String messageText = new String(packet.getData(), 0, packet.getLength(), StandardCharsets.UTF_8);
                    String[] parts = messageText.split(":");
                    System.out.println(messageText);

                    System.out.println(parts[0]);

                    if (parts[0].equals("KEY")) {
                        System.out.println(parts[1]);
                        PublicKey publicKey = parsePublicKey(parts[1]);
                        System.out.println(publicKey);
                        User newUser = new User(String.valueOf(packet.getAddress()), publicKey, "aaa");
                        onlineUsers.add(newUser);
                        System.out.println(String.valueOf(packet.getAddress()));
                    } else if (parts[0].equals("MES")) {
                        System.out.println(parts[2]);
                        if (!parts[3].equals(name)) {
                            receiveMessage(privateKey, parts[2], parts[3]);
                        }
                    }

                } catch (IOException e) {
                    e.printStackTrace();
                }

                    /*try {
                        ServerSocket serverSocket = new ServerSocket(port);
                        Socket clientSocket = serverSocket.accept();
                        BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));

                        String inputLine;
                        while ((inputLine = in.readLine()) != null) {
                            System.out.println("Received message: " + inputLine);
                        }

                        in.close();
                        clientSocket.close();
                        serverSocket.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }*/
//                }
            }
        }).start();
//        socket.disconnect();
//        socket.close();
//        socket.disconnect();

//        return null;
    }

    private PublicKey parsePublicKey(String keyInBase64) {
        try {
            byte[] keyBytes = Base64.getDecoder().decode(keyInBase64);
            X509EncodedKeySpec keySpec = new X509EncodedKeySpec(keyBytes);
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            return keyFactory.generatePublic(keySpec);
        } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
            throw new IllegalStateException("Error while parsing public key", e);
        }
    }

    private void receiveMessages(String ip, int usedPort) throws IOException {

        DatagramSocket socket = new DatagramSocket(usedPort, InetAddress.getByName(ip));
        new Thread(() -> {
            while (true) {
                try {
                    byte[] buffer = new byte[BUFFER_SIZE];
                    DatagramPacket packet = new DatagramPacket(buffer, buffer.length);

                    socket.receive(packet);

                    ByteArrayInputStream inputStream = new ByteArrayInputStream(packet.getData());
                    ObjectInputStream objectInputStream = new ObjectInputStream(inputStream);
                    Object chars = objectInputStream.readChar();

                    System.out.println(chars);
                    objectInputStream.close();

                } catch (IOException e) {
                    e.printStackTrace();

                }
            }
        }).start();
    }

    //    @Override
    public void start(Stage primaryStage) {
        primaryStage.setTitle("Chat Application");

        // Окно для ввода имени
        GridPane nameGrid = createNameInputGrid(primaryStage);

        Scene scene = new Scene(nameGrid, 300, 150);
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    private GridPane createNameInputGrid(Stage primaryStage) {
        GridPane grid = new GridPane();
        grid.setAlignment(Pos.CENTER);
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(25, 25, 25, 25));

        Label nameLabel = new Label("Введите ваше имя:");
        nameInput = new TextField();

        Button confirmButton = new Button("Подтвердить");
        confirmButton.setOnAction(e -> {
            try {
                handleConfirmation(primaryStage);
            } catch (IOException ioException) {
                ioException.printStackTrace();
            }
        });

        grid.add(nameLabel, 0, 0);
        grid.add(nameInput, 1, 0);
        grid.add(confirmButton, 1, 1);

        return grid;
    }


    public void handleConfirmation(Stage primaryStage) throws IOException {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("../fxml/sample.fxml"));
        Parent root = loader.load();
        Scene scene = new Scene(root, 605, 3440);
        primaryStage.setTitle("Encrypted Chat " );
        primaryStage.setScene(scene);
        primaryStage.show();
//        Parent root = FXMLLoader.load(getClass().getResource("int.fxml"));

        String name = nameInput.getText();

        int targetPort = 8000;
//        BorderPane root = new BorderPane();

        userList = new ListView<>();
//        messageArea = new TextArea();
//        inputArea = new TextArea();

        try {
            KeyPair keyPair = generateKeys();
            PublicKey publicKey = keyPair.getPublic();
            PrivateKey privateKey = keyPair.getPrivate();
            System.out.println(String.valueOf(InetAddress.getLocalHost()));
//        onlineUsers.add(new User(String.valueOf(InetAddress.getLocalHost()), publicKey, name));

//        sendMessageButton = new Button("Send");
            sendMessageButton.setOnAction(e -> {
                try {
                    for (User user : onlineUsers) {
                        sendMessage(user, messageInputField.getText(), user.getPublicKey(), targetPort);
                    }
                } catch (IOException ioException) {
                    ioException.printStackTrace();
                }
            });

//        sendKeyButton = new Button("Keys");
//        sendKeyButton.setOnAction(e -> {
            try {
                sendPublicKey(publicKey, targetPort);
            } catch (IOException | NoSuchAlgorithmException ioException) {
                ioException.printStackTrace();
            }
//        });

//        listenButton = new Button("Listen");
            try {
                receivePublicKeys("name", targetPort, privateKey);
            } catch (IOException | ClassNotFoundException ioException) {
                ioException.printStackTrace();
            }

        } catch (NoSuchAlgorithmException | UnknownHostException e) {
            e.printStackTrace();
        }

//        HBox bottomBox = new HBox();
//        bottomBox.getChildren().addAll(inputArea, sendMessageButton, sendKeyButton);
//        bottomBox.setSpacing(10);
//        bottomBox.setPadding(new Insets(10));

//        root.setLeft(userList);
//        root.setCenter(messageArea);
//        root.setBottom(bottomBox);

//        assert root != null;

    }

    private void sendMessage(User user, String message, PublicKey publicKey, int targetPort) throws IOException {
        DatagramSocket socket = new DatagramSocket();
        socket.setBroadcast(true);

        try {
            String encodedKey = Base64.getEncoder().encodeToString(publicKey.getEncoded());
            String string = "MES:" + encodedKey + ":" + getEncryptedBytes(message, publicKey) + ":" + user.getName();
            byte[] data = string.getBytes(StandardCharsets.UTF_8);

            System.out.println(user.getIp());
            DatagramPacket packet = new DatagramPacket(data, data.length, InetAddress.getByName("255.255.255.255"), targetPort == 8080 ? 8081 : 8080);
            socket.send(packet);

//            messageList.appendText("You: " + message + "\n");
            Platform.runLater(() -> messageList.getItems().add("You: " + message + "\n"));
            messageInputField.clear();

            socket.disconnect();
            socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private String getEncryptedBytes(String decryptedMessage, PublicKey publicKey) {
        Crypto crypto = new Crypto();
        return Base64.getEncoder().encodeToString(crypto.encrypt(decryptedMessage, publicKey));
    }

    private String getDecryptedString(String encryptedMessage, PrivateKey privateKey) {
        Crypto crypto = new Crypto();
        return crypto.decrypt(encryptedMessage, privateKey);
    }

    public void receiveMessage(PrivateKey privateKey, String encryptedMessage, String name) {
        String decryptedMessage = getDecryptedString(encryptedMessage, privateKey);
        Platform.runLater(() -> messageList.getItems().add("You: " + decryptedMessage + "\n"));
//        messageArea.appendText(name + ": " + decryptedMessage + "\n");
        messageInputField.clear();
    }
}
