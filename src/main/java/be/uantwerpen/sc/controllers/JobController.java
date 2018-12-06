package be.uantwerpen.sc.controllers;

import be.uantwerpen.sc.models.Bot;
import be.uantwerpen.sc.models.BotState;
import be.uantwerpen.sc.services.*;
import be.uantwerpen.sc.services.newMap.PointControlService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

/**
 * @author  Niels on 4/05/2016.
 * @author Reinout
 *
 * Job Controller
 * TODO Use, comments?
 * HTTP Interface
 */
@RestController
@RequestMapping("/job/")
public class JobController
{
    /**
     * Autowired Bot Control Service
     */
    @Autowired
    private BotControlService botControlService;

    /**
     * Autowired Job Service
     */
    @Autowired
    private JobService jobService;

    /**
     * Autowired Point Control Service
     */
    @Autowired
    private PointControlService pointControlService;

    /**
     * Maas IP
     */
    @Value("${maas.ip:default}")
    private String maasIp;

    /**
     * Maas Port
     */
    @Value("${maas.port:default}")
    private String maasPort;

    /**
     * HTTP RECEIVE from MAAS
     * Uses COREID
     * @param idJob
     * @param idVehicle
     * @param idstart
     * @param idstop
     * @return
     */
    @RequestMapping(value = "executeJob/{idJob}/{idVehicle}/{idStart}/{idStop}",method = RequestMethod.GET)
    public String executeJob(@PathVariable("idJob") long idJob, @PathVariable("idVehicle") long idVehicle, @PathVariable("idStart") long idstart, @PathVariable("idStop") long idstop)
    {
        Bot b;
        try {
            b = botControlService.getBot( idVehicle);
        }catch(Exception e){
            return "HTTP status : 404";
        }
        if (b.getBusy()==1){
            return "HTTP status : 403";
        }
        try {
            pointControlService.getPoint( idstart);
        }catch (Exception e){
            return "HTTP status : 404";
        }
        try {
            pointControlService.getPoint( idstop);
        }catch (Exception e){
            return "HTTP status : 404";
        }

        b.setJobId( idJob);
        b.setBusy(1);
        b.setIdStart(idstart);
        b.setIdStop(idstop);
        b.updateStatus(BotState.Busy.ordinal());
        botControlService.saveBot(b);

        jobService.sendJob(idJob, idVehicle, idstart, idstop);

        return "HTTP status : 200";
    }

    /**
     *
     * @param robotId
     */
    @RequestMapping(value = "finished/{robotId}",method = RequestMethod.GET)
    public void finished(@PathVariable("robotId") long robotId)
    {
        Bot bot = botControlService.getBot( robotId);
        bot.setBusy(0);
        bot.setStatus(BotState.Alive.ordinal());
        botControlService.saveBot(bot);
        completeJob(bot.getJobId());
    }

    /**
     * HTTP GET -> MAAS
     * Notifies that job is completed
     * @param id
     */
    public void completeJob(long id){
        try {
            String u = "http://"+maasIp+":"+maasPort+"/completeJob/" + id;
            URL url = new URL(u);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("Accept", "application/json");

            if (conn.getResponseCode() != 200) {
                throw new RuntimeException("Failed : HTTP error code : "
                        + conn.getResponseCode());
            }
            conn.disconnect();

        } catch (MalformedURLException e) {

            e.printStackTrace();

        } catch (IOException e) {

            e.printStackTrace();

        }
    }

}
