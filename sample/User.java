package sample;

import java.security.PublicKey;

public class User {
    private final String ip;
    private final PublicKey publicKey;
    private final String name;

    public User(String ip, PublicKey publicKey, String name) {
        this.ip = ip;
        this.name = name;
        this.publicKey = publicKey;
    }

    public String getIp() {
        return ip;
    }

    public PublicKey getPublicKey() {
        return publicKey;
    }

    public String getName() {
        return name;
    }
}
