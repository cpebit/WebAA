package it.cpeb.teslaa.helpers.messageprocessors;

import static it.cpeb.teslaa.helpers.Constants.FLAG_ALL;
import static it.cpeb.teslaa.helpers.Constants.SENORCHANNEL;
import static it.cpeb.teslaa.helpers.Constants.startGPS;
import static it.cpeb.teslaa.helpers.Constants.startNight;
import static it.cpeb.teslaa.helpers.Constants.startSpeed;
import static it.cpeb.teslaa.proto.SensorMessageId.SENSOR_MESSAGE_BATCH_VALUE;
import static it.cpeb.teslaa.proto.SensorMessageId.SENSOR_MESSAGE_REQUEST_VALUE;
import static it.cpeb.teslaa.proto.SensorMessageId.SENSOR_MESSAGE_RESPONSE_VALUE;

import org.greenrobot.eventbus.EventBus;

import it.cpeb.teslaa.helpers.AAMessage;
import it.cpeb.teslaa.proto.DrivingStatus;
import it.cpeb.teslaa.proto.DrivingStatusData;
import it.cpeb.teslaa.proto.MessageStatus;
import it.cpeb.teslaa.proto.ParkingBrakeData;
import it.cpeb.teslaa.proto.SensorBatch;
import it.cpeb.teslaa.proto.SensorRequest;
import it.cpeb.teslaa.proto.SensorResponse;
import it.cpeb.teslaa.proto.SensorType;


public class SensorMessage {


    public static AAMessage sensorResponse(AAMessage received) throws Exception {
        AAMessage resp = null;


        if (received.getMessageID() == SENSOR_MESSAGE_REQUEST_VALUE) // Sensor Start Request
        {
            received.trim();
            resp = new AAMessage(SENORCHANNEL, FLAG_ALL, SENSOR_MESSAGE_RESPONSE_VALUE);
            resp.setData(SensorResponse.newBuilder().setStatus(MessageStatus.STATUS_SUCCESS));
            EventBus.getDefault().post(resp);


            SensorRequest sensorrequest = SensorRequest.parseFrom(received.getData());

            if (sensorrequest.getType() == SensorType.SENSOR_DRIVING_STATUS_DATA) {
                resp = new AAMessage(SENORCHANNEL, FLAG_ALL, SENSOR_MESSAGE_BATCH_VALUE);
                SensorBatch.Builder sensor = SensorBatch.newBuilder();
                sensor.addDrivingStatusData(DrivingStatusData.newBuilder().setStatus(DrivingStatus.DRIVE_STATUS_UNRESTRICTED_VALUE).build());
                resp.setData(sensor);
                EventBus.getDefault().post(resp);
            } else if (sensorrequest.getType() == SensorType.SENSOR_PARKING_BRAKE) {
                resp = new AAMessage(SENORCHANNEL, FLAG_ALL, SENSOR_MESSAGE_BATCH_VALUE);
                SensorBatch.Builder sensor = SensorBatch.newBuilder();
                sensor.addParkingBrakeData(ParkingBrakeData.newBuilder().setParkingBrake(true).build());
                resp.setData(sensor);
                EventBus.getDefault().post(resp);
            } else if (sensorrequest.getType() == SensorType.SENSOR_LOCATION)
                EventBus.getDefault().post(startGPS);
            else if (sensorrequest.getType() == SensorType.SENSOR_SPEED)
                EventBus.getDefault().post(startSpeed);
            else if (sensorrequest.getType() == SensorType.SENSOR_NIGHT_MODE)
                EventBus.getDefault().post(startNight);
        }
        return null;
    }
}
