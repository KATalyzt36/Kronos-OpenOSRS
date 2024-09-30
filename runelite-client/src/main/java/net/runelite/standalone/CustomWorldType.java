package net.runelite.standalone;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.net.InetAddress;
import java.net.UnknownHostException;

@RequiredArgsConstructor @Getter
public enum CustomWorldType {

    PVP("KronosPK", getIp(), getIp(), getIp()),
    ECO("Kronos", getIp(), getIp(), getIp()),
    BETA("BETA", getIp(), getIp(), getIp()),
    DEV("Development", getIp(), getIp(), getIp());

    private final String name;
    private final String url;
    private final String gameServerAddress;
    private final String fileServerAddress;

    private static String getIp()
    {
        String hostname = System.getenv("DDNS");
        try {
            InetAddress address = InetAddress.getByName(hostname);
            return address.getHostAddress();
        } catch (UnknownHostException e) {
            e.printStackTrace();
            return null;
        }
    }
}
