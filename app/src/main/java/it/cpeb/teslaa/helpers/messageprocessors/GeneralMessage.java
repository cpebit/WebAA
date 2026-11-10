package it.cpeb.teslaa.helpers.messageprocessors;

import it.cpeb.teslaa.helpers.AAMessage;
import it.cpeb.teslaa.helpers.streams.InOutStream;

public class GeneralMessage {
    AAMessage received;
    InOutStream inOutStream;
    AAMessage resp = null;

    public GeneralMessage(AAMessage message, InOutStream inOutStream) {
        received = message;
        this.inOutStream = inOutStream;
    }

}
