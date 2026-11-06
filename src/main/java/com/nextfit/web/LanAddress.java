package com.nextfit.web;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Collections;
import java.util.Optional;

/** Finds the address of this computer in the local network, the one a phone can reach. */
public final class LanAddress {

    private LanAddress() {
    }

    /**
     * Looks for the IPv4 address of this computer in a private network
     * (192.168.x.x, 10.x.x.x, 172.16-31.x.x).
     *
     * @return the address, or "localhost" when the computer is not in any network
     */
    public static String find() {
        return findPrivate().orElse("localhost");
    }

    private static Optional<String> findPrivate() {
        try {
            for (NetworkInterface nic : Collections.list(NetworkInterface.getNetworkInterfaces())) {
                if (!nic.isUp() || nic.isLoopback() || nic.isVirtual()) {
                    continue;
                }
                for (InetAddress address : Collections.list(nic.getInetAddresses())) {
                    if (address instanceof Inet4Address && address.isSiteLocalAddress()) {
                        return Optional.of(address.getHostAddress());
                    }
                }
            }
        } catch (SocketException e) {
            return Optional.empty();
        }
        return Optional.empty();
    }
}
