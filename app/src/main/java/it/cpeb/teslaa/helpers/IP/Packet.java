package it.cpeb.teslaa.helpers.IP;

import androidx.annotation.NonNull;

public class Packet {
    @NonNull
    private final IPv4Header ipHeader;
    @NonNull
    private final ITransportHeader transportHeader;
    @NonNull
    private final byte[] buffer;

    public Packet(@NonNull IPv4Header ipHeader, @NonNull ITransportHeader transportHeader, @NonNull byte[] data) {
        this.ipHeader = ipHeader;
        this.transportHeader = transportHeader;
        int transportLength;
        if (transportHeader instanceof TCPHeader) {
            transportLength = ((TCPHeader) transportHeader).getDataOffset();
        } else if (transportHeader instanceof UDPHeader) {
            transportLength = 8;
        }
        buffer = data;
    }

    public byte getProtocol() {
        return ipHeader.getProtocol();
    }

    @NonNull
    public ITransportHeader getTransportHeader() {
        return transportHeader;
    }

    public int getSourcePort() {
        return transportHeader.getSourcePort();
    }

    public int getDestinationPort() {
        return transportHeader.getDestinationPort();
    }

    @NonNull
    public IPv4Header getIpHeader() {
        return ipHeader;
    }

    /**
     * the whole packet data as an array of byte
     *
     * @return byte[]
     */
    @NonNull
    public byte[] getBuffer() {
        return buffer;
    }

}