package be.uantwerpen.sc.controllers.mqtt;

import be.uantwerpen.sc.controllers.BotController;
import be.uantwerpen.sc.models.Bot;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttMessage;

/**
 * Created by Arthur on 9/05/2016.
 */
public class MqttLocationSubscriberCallback implements MqttCallback
{
    BotController botController;

    public MqttLocationSubscriberCallback(BotController botController)
    {
        this.botController = botController;
    }

    @Override
    public void connectionLost(Throwable cause)
    {
        //This is called when the connection is lost. We could reconnect here.
    }

    @Override
    public void messageArrived(String topic, MqttMessage mqttMessage) throws Exception
    {
        String botIDString = topic.split("/")[1];
        Long botID = Long.parseLong(botIDString);

        String payloadString = new String(mqttMessage.getPayload());
        System.out.println("LOC :"+payloadString);
        System.out.println("ID :"+botID);

        if(!topic.endsWith("Location")){
            System.out.println("no location");
            return;
        }

        try
        {
            System.out.println("MQTT LOCATION ARRIVED");
            String temp = payloadString.split("id:")[1];
            String id = temp.split("/")[0];
            temp = temp.split("vertexid:")[1];
            String vertexid = temp.split("/")[0];
            temp = temp.split("progress:")[1];
            String progress = temp.split("}")[0];

            int Id = Integer.parseInt(id);
            int VertexId = Integer.parseInt(vertexid);
            int Progress = Integer.parseInt(progress);
            botController.updateLocation((long) Id, (long) VertexId, Progress);
            Bot b = botController.getBot((long) Id);
            b.setAlive(true);
        }
        catch(Exception e)
        {
            System.err.println("Could not parse integer from payloadString: " + payloadString);
        }
    }

    @Override
    public void deliveryComplete(IMqttDeliveryToken iMqttDeliveryToken)
    {

    }
}