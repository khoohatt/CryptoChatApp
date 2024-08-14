package sample;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;

import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;
import java.util.*;

public class ChatApp implements Initializable {
    private final int port;
    private final String name;
    private boolean encrypted = false;

    private static final int BUFFER_SIZE = 1024;

    private ArrayList<String> encryptedMessages = new ArrayList<>();
    private ArrayList<String> decryptedMessages = new ArrayList<>();

    @FXML
    public Button sendMessageButton;
    @FXML
    public Button sendKeysButton;
    @FXML
    public ListView<String> messageList;
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

    List<User> onlineUsers = new ArrayList<>();

    public void initialize(URL location, ResourceBundle resources) {

        int targetPort = this.port;
        String name = this.name;

        try {
            KeyPair keyPair = generateKeys();
            PublicKey publicKey = keyPair.getPublic();
            PrivateKey privateKey = keyPair.getPrivate();

            sendMessageButton.setOnAction(e -> {
                try {
                    for (User user : onlineUsers) {
                        sendMessage(name, messageInputField.getText(), user, targetPort);
                    }
                } catch (IOException ioException) {
                    ioException.printStackTrace();
                }
            });

            sendKeysButton.setOnAction(e -> {
                try {
                    sendPublicKey(publicKey, targetPort, name);
                } catch (IOException | NoSuchAlgorithmException ioException) {
                    ioException.printStackTrace();
                }
            });

            showKeysButton.setOnAction(e -> showKeys(keyPair));
            showEncryptedButton.setOnAction(e -> showEncrypted());

            try {
                receivePublicKeys(name, targetPort, privateKey);
            } catch (IOException | ClassNotFoundException ioException) {
                ioException.printStackTrace();
            }

        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
    }

    public ChatApp(int port, String name) {
        this.port = port;
        this.name = name;
    }

    private KeyPair generateKeys() throws NoSuchAlgorithmException {
        KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
        keyGen.initialize(2048);
        return keyGen.generateKeyPair();
    }

    private void sendPublicKey(PublicKey publicKey, int targetPort, String name) throws IOException, NoSuchAlgorithmException {
        DatagramSocket socket = new DatagramSocket();
        socket.setBroadcast(true);

        try {
            String encodedKey = Base64.getEncoder().encodeToString(publicKey.getEncoded());
            String string = "KEY:" + encodedKey + ":" + name;
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
        DatagramSocket socket = new DatagramSocket(targetPort);

        new Thread(() -> {
            while (true) {
                try {
                    byte[] buffer = new byte[BUFFER_SIZE];
                    DatagramPacket packet = new DatagramPacket(buffer, buffer.length);

                    socket.receive(packet);

                    String messageText = new String(packet.getData(), 0, packet.getLength(), StandardCharsets.UTF_8);
                    String[] parts = messageText.split(":");

                    switch (parts[0]) {
                        case "KEY":
                            PublicKey publicKey = parsePublicKey(parts[1]);

                            User newUser = new User(String.valueOf(getOutboundAddress(packet.getSocketAddress()).getHostAddress()), publicKey, parts[2]);
                            addUser(newUser);

                            break;

                        case "MES":
                            if (!parts[3].equals(name)) {
                                receiveMessage(privateKey, parts[2], parts[3]);
                            }

                            break;

                        case "END":
                            try {
                                for (User u : onlineUsers) {
                                    if (u.getName().equals(parts[1])) {
                                        onlineUsers.remove(u);
                                        showUsers();
                                        break;
                                    }
                                }
                            } catch (ConcurrentModificationException e) {
                                System.out.println("завершение общения...");
                            }
                            break;
                    }

                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    private void addUser(User user) {
        for (User u : onlineUsers) {
            if (u.getName().equals(user.getName())) {
                return;
            }
        }
        onlineUsers.add(user);
        showUsers();

    }

    private void showUsers() {
        StringBuilder users = new StringBuilder("онлайн:");
        for (User u : onlineUsers) {
            users.append(" ").append(u.getName());
        }
        if (users.length() < 8) {
            users.append(" никого нет...");
        }

        Platform.runLater(() -> onlineUsersField.setText(String.valueOf(users)));
    }

    private InetAddress getOutboundAddress(SocketAddress remoteAddress) throws SocketException {
        DatagramSocket sock = new DatagramSocket();
        sock.connect(remoteAddress);

        final InetAddress localAddress = sock.getLocalAddress();

        sock.disconnect();
        sock.close();

        return localAddress;
    }

    private PublicKey parsePublicKey(String keyInBase64) {
        try {
            byte[] keyBytes = Base64.getDecoder().decode(keyInBase64);
            X509EncodedKeySpec keySpec = new X509EncodedKeySpec(keyBytes);
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            return keyFactory.generatePublic(keySpec);
        } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
            throw new IllegalStateException(e);
        }
    }

    public void showKeys(KeyPair keyPair) {
        if (encryptionField.getText().length() <= 1) {
            encryptionField.setText("ваши ключи: " + keyPair.getPublic() + "\n" + keyPair.getPrivate());
        } else {
            encryptionField.clear();
        }
    }

    private void sendMessage(String name, String message, User user, int targetPort) throws IOException {
        DatagramSocket socket = new DatagramSocket();
        socket.setBroadcast(true);

        try {
            String encodedKey = Base64.getEncoder().encodeToString(user.getPublicKey().getEncoded());
            String string = "MES:" + encodedKey + ":" + getEncryptedBytes(message, user.getPublicKey()) + ":" + name;
            byte[] data = string.getBytes(StandardCharsets.UTF_8);

            DatagramPacket packet = new DatagramPacket(data, data.length, InetAddress.getByName(user.getIp()), targetPort == 8080 ? 8081 : 8080);
            socket.send(packet);

            System.out.println(getEncryptedBytes(message, user.getPublicKey()));
            System.out.println(string);

            decryptedMessages.add("вы: " + message + "\n");
            encryptedMessages.add(string);

            Platform.runLater(() -> messageList.getItems().add("вы: " + message + "\n"));
            messageInputField.clear();

            socket.disconnect();
            socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void showEncrypted() {
        ObservableList<String> observableArrayList;
        if (encrypted) {
            observableArrayList = FXCollections.observableArrayList(decryptedMessages);
        } else {
            observableArrayList = FXCollections.observableArrayList(encryptedMessages);
        }
        messageList.setItems(observableArrayList);
        encrypted = !encrypted;
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
        decryptedMessages.add(name + ": " + decryptedMessage + "\n");
        encryptedMessages.add(encryptedMessage);
        Platform.runLater(() -> messageList.getItems().add(name + ": " + decryptedMessage + "\n"));
        messageInputField.clear();
    }

    public void deleteFromOnlineUsers(int port) {
        try {
            for (User u : onlineUsers) {
                sendStopMessage(port, u.getIp());
            }
        } catch (SocketException e) {
            e.printStackTrace();
        }

    }

    public void sendStopMessage(int targetPort, String ip) throws SocketException {
        DatagramSocket socket = new DatagramSocket();
        socket.setBroadcast(true);

        try {
            String string = "END:" + name;
            byte[] data = string.getBytes(StandardCharsets.UTF_8);

            DatagramPacket packet = new DatagramPacket(data, data.length, InetAddress.getByName(ip), targetPort == 8080 ? 8081 : 8080);
            socket.send(packet);

            socket.disconnect();
            socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
