package be.uantwerpen.sc.services;

import be.uantwerpen.sc.controllers.mqtt.MqttJobPublisher;
import be.uantwerpen.sc.models.Job;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Job Service
 */
@Service
public class JobService
{
    /**
     * Autowired MQTT Publisher
     */
    @Autowired
    MqttJobPublisher mqttJobPublisher;

    /**
     * Send job over MQTT
     * @param botId ID of bot for job
     * @param jobId ID of job for bot
     * @param idStart ID start TODO ?
     * @param idStop ID stop TODO ?
     * @return Success
     */
    public boolean sendJob(Long botId, Long jobId, long idStart, long idStop)
    {
        Job job = new Job();
        job.setIdEnd(idStop);
        job.setIdStart(idStart);
        job.setIdVehicle(botId);
        job.setJobId(jobId);

        return mqttJobPublisher.publishJob(job, botId);
    }

}
