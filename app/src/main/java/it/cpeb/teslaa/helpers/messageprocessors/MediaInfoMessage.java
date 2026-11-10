package it.cpeb.teslaa.helpers.messageprocessors;

import static it.cpeb.teslaa.helpers.Constants.FLAG_ALL;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.util.Base64;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import it.cpeb.teslaa.helpers.AAMessage;
import it.cpeb.teslaa.proto.MediaPlaybackMetadata;


public class MediaInfoMessage {

    static ByteArrayOutputStream stream = new ByteArrayOutputStream();

    private static byte[] fanart;
    private static boolean isMultiPart = false;

    public static void MediaInfoMessage(AAMessage message, Context context) throws IOException, JSONException {


        Intent intent = new Intent();


        if (message.getMessageID() == 32769 && (!isMultiPart || (isMultiPart && message.getFlag() == FLAG_ALL))) {
            if (stream.size() > 0) {
                //   Log.d("MEDIA", Util.bytesToHex(stream.toByteArray()));
                MediaPlaybackMetadata metadata = MediaPlaybackMetadata.parseFrom(stream.toByteArray());
                stream.reset();
                if (metadata.toString().length() > 2) {


                    int duration = metadata.getDurationSeconds();


                    JSONObject obj = new JSONObject();

                    obj.put("type", "mediastatus");
                    obj.put("song", metadata.getSong());
                    obj.put("artist", metadata.getArtist());
                    obj.put("album", metadata.getAlbum());
                    obj.put("remaining", String.format("%02d:%02d", duration / 60, duration % 60));
                    fanart = metadata.getAlbumArt().toByteArray();
                    if (fanart != null) {
                        obj.put("fanart", Base64.encodeToString(fanart, Base64.DEFAULT));
                    }

                    //  mediaStatusSocket.PushToBrowser(obj.toString());
                }
            }
            isMultiPart = false;
        } else if (message.getMessageID() == 32771) {
            // Log.d("HU-MEDIA",Util.bytesToHex(decoded));
            message.trim();
            stream.write(message.getData());
            isMultiPart = true;
        } else {
            stream.write(message.getData());

        }

    }
}
