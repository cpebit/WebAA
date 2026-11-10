package it.cpeb.teslaa.helpers;

import static it.cpeb.teslaa.helpers.Constants.FLAG_ALL;
import static it.cpeb.teslaa.helpers.Constants.FLAG_CONTROL;
import static it.cpeb.teslaa.helpers.Constants.FLAG_ENCRYPTED;
import static it.cpeb.teslaa.helpers.Constants.FLAG_FIRST;
import static it.cpeb.teslaa.helpers.Constants.FLAG_LAST;
import static it.cpeb.teslaa.helpers.Constants.FLAG_MIDDLE;
import static it.cpeb.teslaa.helpers.Constants.FLAG_SPECIFIC;

import androidx.annotation.NonNull;

import com.google.protobuf.GeneratedMessage;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Arrays;

import it.cpeb.teslaa.sslhelpers.NioSSL;

public class AAMessage {
    byte channel;
    byte flag;
    short enc_len;
    int payload;
    int messageID;
    byte[] data;
    byte encrypted = FLAG_ENCRYPTED;
    byte type = FLAG_SPECIFIC;


    public AAMessage(byte[] read, NioSSL nioSSL) throws IOException {

        this.channel = read[0];
        byte flag = read[1];
        this.encrypted = (byte) (flag & FLAG_ENCRYPTED);
        this.type = (byte) (flag & FLAG_CONTROL);
        this.flag = (byte) (flag & FLAG_ALL);

        this.enc_len = (short) ((read[2] & 0xFF) << 8 | (read[3] & 0xFF));
        // Log.d("AAMessage", "Channel: " + channel+", Flag: " + flag+", encryptyed = " + encrypted + "len: "+ enc_len + "raw: " + Util.bytesToHex(read));


        if (this.flag == FLAG_FIRST)
            payload = (short) ((read[4] & 0xFF) << 8 | (read[5] & 0xFF));

        if (this.flag == FLAG_FIRST && this.encrypted == FLAG_ENCRYPTED)
            data = nioSSL.decrypt(read, 8, enc_len);
        else if (this.encrypted == FLAG_ENCRYPTED)
            data = nioSSL.decrypt(read, 4, enc_len);
        else
            data = Arrays.copyOfRange(read, 4, 4 + enc_len);

        if (data.length > 0 && this.flag != FLAG_MIDDLE && this.flag != FLAG_LAST)
            this.messageID = ((data[0] & 0xFF) << 8 | (data[1] & 0xFF));


    }

    public AAMessage(byte channel, byte flag, int messageID) {
        this.channel = channel;
        this.flag = flag;
        this.messageID = messageID;
    }

    public AAMessage(byte channel, byte flag, short enc_len, short payload, int messageID, byte[] data) {
        this.channel = channel;
        this.flag = flag;
        this.enc_len = enc_len;
        this.payload = payload;
        this.messageID = messageID;
        this.data = data;
    }

    public AAMessage(byte channel, byte flag, int messageID, byte[] toByteArray) {
        this.channel = channel;
        this.flag = flag;
        this.messageID = messageID;
        this.data = toByteArray;
    }

    public AAMessage(byte channel, byte flagAll, int messageID, GeneratedMessage.Builder<?> data) {
        this(channel, flagAll, messageID);
        setData(data);
    }


    public void setData(GeneratedMessage.Builder<?> msg) {
        this.data = msg.build().toByteArray();
    }

    public byte getEncrypted() {
        return encrypted;
    }

    public void setEncrypted(byte encrypted) {
        this.encrypted = encrypted;
    }

    public byte getType() {
        return type;
    }

    public void setType(byte type) {
        this.type = type;
    }

    public byte getChannel() {
        return channel;
    }

    public void setChannel(byte channel) {
        this.channel = channel;
    }

    public byte getFlag() {
        return flag;
    }

    public void setFlag(byte flag) {
        this.flag = flag;
    }

    public short getEnc_len() {
        return enc_len;
    }

    public void setEnc_len(short enc_len) {
        this.enc_len = enc_len;
    }

    public int getPayload() {
        return payload;
    }

    public void setPayload(short payload) {
        this.payload = payload;
    }

    public int getMessageID() {
        return messageID;
    }

    public void setMessageID(short messageID) {
        this.messageID = messageID;
    }

    public byte[] getData() {
        return data;
    }

    public void setData(byte[] data) {
        this.data = data;
    }

    public void trim() {
        data = Arrays.copyOfRange(data, 2, data.length);
    }

    public byte[] getFormated(NioSSL ssl) {
        ByteBuffer formated;
        if (this.encrypted == FLAG_ENCRYPTED)
            try {
                formated = ByteBuffer.allocate(16384);
                //      Log.d("AAMSG", "Data to be encrypted: " + data.length + " messageid: " + messageID + ",Channel: " + channel + ", hex: " + Util.bytesToHex(data));
                formated.put(channel);
                formated.put((byte) (flag + encrypted + type));
                ssl.encrypt(data, messageID, formated);
                formated.flip();
                byte[] ba = new byte[formated.remaining()];
                formated.get(ba);
                return ba;
            } catch (Exception e) {
                return null;
            }
        else {
            //if (messageID>0)
            formated = ByteBuffer.allocate(data.length + 8);
            //else
            //  formated=ByteBuffer.allocate(data.length+4);
            formated.put(channel);
            formated.put((byte) (flag + type));
            formated.put((byte) (data.length + 2 >> 8 & 0xFF));
            formated.put((byte) (data.length + 2 & 0xFF));
            //if (messageID>0)
            //{
            formated.put((byte) (messageID >> 8 & 0xFF));
            formated.put((byte) (messageID & 0xFF));
            //}
            formated.put(data);
            return formated.array();
        }
    }

    @NonNull
    @Override
    public String toString() {
        return "Channel: " + channel + ", flag: " + flag + ", messageid: " + messageID + ", data: " + Util.bytesToHex(data);
    }
}
