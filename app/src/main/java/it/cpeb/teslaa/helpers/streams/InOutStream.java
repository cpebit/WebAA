package it.cpeb.teslaa.helpers.streams;

import android.content.Context;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;


public class InOutStream {

    DataInputStream inputStream;
    OutputStream outputStream;


    public Context getContext() {
        return context;
    }

    Context context;


    public InOutStream(InputStream inputStream, OutputStream outputStream) {
        this.inputStream = new DataInputStream(inputStream);
        this.outputStream = outputStream;
    }

    public byte[] read() throws IOException, InterruptedException {
        return read(0);
    }

    public synchronized void write(final byte[] data) {
        write(data, 0);
    }

    public void write(byte[] data, int i) {
        if (data == null)
            return;

        try {

            outputStream.write(data);
            outputStream.flush();

        } catch (IOException e) {
            //  Intent intent = new Intent(context, TransporterService.class);
            //   context.stopService(intent);
        }
    }

    public byte[] read(int i) throws IOException, InterruptedException {

        byte[] buf = new byte[16384];

        inputStream.readFully(buf, 0, 4);
        short enc_len = (short) ((buf[2] & 0xFF) << 8 | (buf[3] & 0xFF));
        if (buf[1] == 9 || buf[1] == 1)
            enc_len += 4;

        inputStream.readFully(buf, 4, enc_len);
        return Arrays.copyOf(buf, enc_len + 4);

    }


}
