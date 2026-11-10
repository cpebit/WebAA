package it.cpeb.teslaa.helpers.messageprocessors;

import static it.cpeb.teslaa.helpers.Constants.FLAG_ALL;
import static it.cpeb.teslaa.helpers.Constants.INPUTCHANNEL;

import android.os.SystemClock;

import org.greenrobot.eventbus.EventBus;

import it.cpeb.teslaa.helpers.AAMessage;
import it.cpeb.teslaa.helpers.Log;
import it.cpeb.teslaa.proto.InputMessageId;
import it.cpeb.teslaa.proto.InputReport;
import it.cpeb.teslaa.proto.KeyBindingResponse;
import it.cpeb.teslaa.proto.KeyEvent;
import it.cpeb.teslaa.proto.RelativeEvent;
import it.cpeb.teslaa.proto.TouchEvent;

public class InputMessage {


    public static AAMessage inputResponse(AAMessage received) throws Exception {

        if (received.getMessageID() == InputMessageId.INPUT_MESSAGE_KEY_BINDING_REQUEST_VALUE) {
            KeyBindingResponse.Builder binding = KeyBindingResponse.newBuilder();
            binding.setStatus(0);
            AAMessage resp = new AAMessage(received.getChannel(), FLAG_ALL, InputMessageId.INPUT_MESSAGE_KEY_BINDING_RESPONSE_VALUE);
            resp.setData(binding);
            return resp;
        }
        return null;
    }

    public static void sendInput(TouchEvent.Builder touch) {
        sendInput(touch, null, null, null, false);
    }

    public static void sendInput(KeyEvent.Builder button) {
        sendInput(null, button, null, null, false);
    }

    public static void sendInput(RelativeEvent.Builder relative, Long evenTime) {
        sendInput(null, null, relative, evenTime, false);
    }

    public static void sendInput(KeyEvent.Builder button, Long evenTime) {
        sendInput(null, button, null, evenTime, false);
    }

    public static void sendInput(
            TouchEvent.Builder touch,
            KeyEvent.Builder button,
            RelativeEvent.Builder relative,
            Long eventTime,
            Boolean touchPad
    ) {
        InputReport.Builder inputData = InputReport.newBuilder();

        if (eventTime == null)
            inputData.setTimestamp(SystemClock.uptimeMillis());
        else
            inputData.setTimestamp(eventTime);

        if (touch != null && !touchPad)
            inputData.setTouchEvent(touch.build());
        else if (touch != null && touchPad)
            inputData.setTouchpadEvent(touch.build());
        if (button != null)
            inputData.setKeyEvent(button.build());
        if (relative != null)
            inputData.setRelativeEvent(relative.build());

        AAMessage aamsg = new AAMessage(INPUTCHANNEL, FLAG_ALL, InputMessageId.INPUT_MESSAGE_INPUT_REPORT_VALUE);
        aamsg.setData(inputData);
        Log.d("Input", "Sending input: " + inputData);
        EventBus.getDefault().post(aamsg);
    }

    public static void sendkey(int keycode, boolean action) {
        sendKey(keycode, action, null);
    }

    public static void sendKey(int keycode, boolean action, Long eventTime) {
        Log.d("HU-KEY", "Code: " + keycode + "action: " + action + ", time: " + eventTime);
        sendInput(KeyEvent.newBuilder().addKeys(
                        KeyEvent.Key.newBuilder()
                                .setKeycode(keycode)
                                .setDown(action)
                                .setLongpress(false)
                                .setMetastate(0)
                ),
                eventTime
        );
    }

    public static void sendScrollKey(int delta, Long eventTime) {
        sendInput(RelativeEvent.newBuilder().addData(
                        RelativeEvent.Rel.newBuilder()
                                .setKeycode(65536)
                                .setDelta(delta)
                ),
                eventTime
        );
    }

}