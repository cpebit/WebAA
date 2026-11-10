package it.cpeb.teslaa.helpers.IP;

public class UDPHeader implements ITransportHeader {
    private int sourcePort;
    private int destinationPort;
    private int length;
    private int checksum;

    UDPHeader(int srcPort, int destPort, int length, int checksum) {
        this.sourcePort = srcPort;
        this.destinationPort = destPort;
        this.length = length;
        this.checksum = checksum;
    }

    public int getSourcePort() {
        return sourcePort;
    }

    public void setSourcePort(int sourcePort) {
        this.sourcePort = sourcePort;
    }

    public int getDestinationPort() {
        return destinationPort;
    }

    public void setDestinationPort(int destinationPort) {
        this.destinationPort = destinationPort;
    }

    public int getLength() {
        return length;
    }

    public void setLength(int length) {
        this.length = length;
    }

    public int getChecksum() {
        return checksum;
    }

    public void setChecksum(int checksum) {
        this.checksum = checksum;
    }

}
