package be.uantwerpen.sc.controllers;


import be.uantwerpen.sc.models.Bot;
import be.uantwerpen.sc.models.BotState;
import be.uantwerpen.sc.models.Location;
import be.uantwerpen.sc.models.map.newMap.Link;
import be.uantwerpen.sc.services.BotControlService;
import be.uantwerpen.sc.services.newMap.LinkControlService;
import be.uantwerpen.sc.services.newMap.PointControlService;
import org.json.JSONArray;
import org.json.JSONException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;

import java.util.Date;
import java.util.List;
import org.json.JSONObject;

/**
 * Bot Controller
 */
@RestController
@RequestMapping("/bot/")
public class BotController
{
    /**
     * Autowired Botcontrol Service
     */
    @Autowired
    private BotControlService botControlService;

    /**
     * Autowired Link Control Service
     */
    @Autowired
    private LinkControlService linkControlService;

    /**
     * Autowired Link Control Service
     */
    @Autowired
    private PointControlService pointControlService;

    /**
     * Get All Bots
     * @return
     */
    @RequestMapping(method = RequestMethod.GET)
    public List<Bot> allBots()
    {
        return botControlService.getAllBots();
    }

    /**
     * BackBone IP
     */
    @Value("${backbone.ip:default}")
    String backboneIP;
    /**
     * BackBone Port
     */
    @Value("${backbone.port:default}")
    String backbonePort;

    @RequestMapping(value = "{id}",method = RequestMethod.GET)
    public Bot getBot(@PathVariable("id") Long id)
    {
        return botControlService.getBot(id);
    }

    @RequestMapping(value = "{id}",method = RequestMethod.POST)
    public void saveBot(@PathVariable("id") Long id, @RequestBody Bot bot)
    {
        botControlService.saveBot(bot);
    }

    @RequestMapping(value = "{id}",method = RequestMethod.PUT)
    public void updateBot(@PathVariable("id") Long id, @RequestBody Bot bot)
    {
        botControlService.saveBot(bot);
    }

    @RequestMapping(value = "updateBotTest/{id}",method = RequestMethod.GET)
    public void updateBotTest(@PathVariable("id") Long id)
    {
        Bot botEntity = new Bot(id);
        botEntity.setWorkingMode("Updated");
        botControlService.saveBot(botEntity);
    }


    @RequestMapping(value = "savetest",method = RequestMethod.GET)
    public void saveBotTest()
    {
        Bot bot = new Bot((long) getNewId());
        bot.setWorkingMode("test");
        botControlService.saveBot(bot);
    }


    /**
     * Robot calls this GET
     * @return
     */
    @RequestMapping(value = "newRobot", method = RequestMethod.GET)
    public Long newRobot()
    {
        Bot bot = new Bot((long) getNewId());
        System.out.println(bot);
        //Save bot in database and get bot new rid
        botControlService.saveBot(bot);

        Date date = new Date();
        System.out.println("New robot created!! - " + date.toString());

        return bot.getIdCore();
    }

    @RequestMapping(value = "{id}/lid/{lid}", method = RequestMethod.GET)
    public void locationLink(@PathVariable("id") Long id, @PathVariable("lid") Long lid)
    {
        Bot bot = botControlService.getBot(id);
        Link link;

        if(bot != null)
        {
            link = linkControlService.getLink(lid);

            if(link != null)
            {
                bot.setLinkId(link);
                botControlService.saveBot(bot);
                System.out.println(bot.getIdCore());
            }
            else
                System.out.println("Link with id: " + lid + " not found!");
        }
        else
            System.out.println("Bot with id:" + id + " not found!");
    }

    public void updateLocation(Long id, Long idvertex, int progress)
    {
        Bot bot = botControlService.getBot(id);

        if(bot != null)
        {
            //System.out.println(.getLink(idvertex));
            //bot.setLinkId(pointControlService.getPoint(idvertex));
            bot.setPercentageCompleted(progress);
            bot.updateStatus(BotState.Alive.ordinal());
            botControlService.saveBot(bot);
        }
    }

    @RequestMapping(value = "delete/{rid}",method = RequestMethod.GET)
    public void deleteBot(@PathVariable("rid") Long rid)
    {
        botControlService.deleteBot(rid);
    }

    @RequestMapping(value = "/deleteBots}",method = RequestMethod.GET)
    public void resetBots()
    {
        botControlService.deleteBots();
    }

    /**
     * Get all bot positions
     * @return
     */
    @RequestMapping(value = "posAll", method = RequestMethod.GET)
    public String posAll(){
        List<Bot> bots = botControlService.getAllBots();
        JSONArray array = new JSONArray();
        for(Bot b : bots){
            Location loc = new Location();
            loc.setVehicleID(b.getIdCore());

            if (b.getBusy()==1){
                loc.setStartID(b.getIdStart());
                loc.setStopID(b.getIdStop());
                loc.setPercentage((long) b.getPercentageCompleted());
            }else if(b.getBusy()==0){
                loc.setStartID(b.getLinkId().getStartPoint().getId());
                loc.setStopID(b.getLinkId().getStartPoint().getId());
                loc.setPercentage( (long)100);
            }

            JSONObject obj = new JSONObject();
            try{
                obj.put("idVehicle", loc.getVehicleID());
                obj.put("idStart", loc.getStartID());
                obj.put("idEnd", loc.getStopID());
                obj.put("percentage", loc.getPercentage());
            }
            catch (JSONException e) {e.printStackTrace(); }
            array.put(obj);
        }
        return array.toString();
    }

    @RequestMapping(value = "checkTimer", method = RequestMethod.GET)
    public void checkTimer(){
        System.out.println("Checking Alive Bots");
        List<Bot> bots = botControlService.getAllBots();
        long currentDate=new Date().getTime();
        for(Bot b : bots){
            if(b.getStatus()!=BotState.Unknown.ordinal()){
                if(currentDate-b.getLastUpdated().getTime()>1000*60*5) {
                    b.updateStatus(BotState.Unknown.ordinal());
                    botControlService.saveBot(b);
                }
            }
            else{
                if(new Date().getTime()-b.getLastUpdated().getTime()>1000*60*5) {
                    botControlService.deleteBot(b.getIdCore());
                }
            }
        }
    }

    /**
     * 1st Function at Bot Boot
     * Creates Bot, initiates entry for database, and returns its ID
     * @param modus Type: Independent, partial or full server
     * @return
     */
    @RequestMapping(value = "initiate/{modus}", method = RequestMethod.GET)
    public long initiate(@PathVariable("modus") String modus){
        Bot bot = new Bot((long) getNewId());
        bot.setWorkingMode(modus);
        bot.setJobId((long) 0);
        bot.setLinkId(linkControlService.getLink((long) 1));
        bot.setPercentageCompleted(100);
        bot.setIdStart((long) 1);
        bot.setIdStop((long) 1);
        bot.setBusy(0);
        bot.updateStatus(BotState.Alive.ordinal());
        //id ook doorgeven naar robot zelf
        botControlService.saveBot(bot);
        return bot.getIdCore();
    }

    /**
     * Gets New Bot ID for running a job? Something something calcweight
     * TODO: Timeout HTTP
     * @return Bot ID
     */
    public int getNewId(){
        /*StringBuilder data = new StringBuilder();
        try {
            URL url = new URL("http://"+backboneIP+":"+backbonePort+"//bot/newBot/robot");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("Accept", "application/json");

            if (conn.getResponseCode() != 200) {
                throw new RuntimeException("Failed : HTTP error code : "
                        + conn.getResponseCode());
            }

            BufferedReader br = new BufferedReader(new InputStreamReader(
                    (conn.getInputStream())));

            String output;
            System.out.println("Output from Server .... \n");
            while ((output = br.readLine()) != null) {
                data.append(output);
                System.out.println(output);
            }

            conn.disconnect();

        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {

            e.printStackTrace();
        }
        System.out.println(data);
        return Integer.parseInt(data.toString());*/
        return 2;
    }

}
