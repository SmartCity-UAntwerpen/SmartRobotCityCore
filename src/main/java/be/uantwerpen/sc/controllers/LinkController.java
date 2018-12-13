package be.uantwerpen.sc.controllers;

import be.uantwerpen.rc.models.Bot;
import be.uantwerpen.rc.models.map.Link;
import be.uantwerpen.sc.services.BotControlService;
import be.uantwerpen.sc.services.newMap.LinkControlService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * @author  Niels on 30/03/2016.
 * @author Reinout
 * HTTP INTERFACE
 *
 * Link Controller
 *
 */
@RestController
@RequestMapping("/link/")
public class LinkController
{
    /**
     * Autowired Link Control Service
     */
    @Autowired
    private LinkControlService linkControlService;

    @Autowired
    private BotControlService botService;

    /**
     * Get <- TODO
     * Get list of all available
     * @return
     */
    @RequestMapping(method = RequestMethod.GET)
    public List<Link> allLinks()
    {
        return linkControlService.getAllLinks();
    }

    /**
     * Called by bot to lock a link
     * @param id ID of link to lock
     * @return Success
     */
    @RequestMapping(value = "requestlock/{botId}/{id}", method = RequestMethod.GET)
    public boolean requestLinkLock(@PathVariable("botId") Long botId,@PathVariable("id") Long id)
    {
        synchronized(this)
        {
            Link link = linkControlService.getLink(id);

            if(link == null) //Link not found
            {
                return false;
            }
            if(link.getLock().getStatus()) {
                return false;
            } else{
                //Point not locked -> attempt lock
                Bot bot = botService.getBot(botId);
                link.lockLink(true,bot);
                link.setWeight(link.getWeight()+10);
                linkControlService.save(link);
                return true;

            }
        }

    }

    /**
     * Called by bot to unlock a link
     * @param id ID of link to lock
     * @return Success
     */
    @RequestMapping(value = "unlock/{botId}/{id}", method = RequestMethod.GET)
    public boolean LinkUnLock(@PathVariable("botId") Long botId, @PathVariable("id") Long id)
    {
        synchronized(this)
        {
            Link link = linkControlService.getLink(id);

            if(link == null)//Link not found
            {
                return false;
            }

            //Check if bot asking the unlock is the one that locked the link
            if(link.getLock().getLockedBy().getIdCore().equals(botId) && link.getLock().getStatus()) {
                //Point already locked
                link.lockLink(false, null);
                link.setWeight(link.getWeight() - 10);
                linkControlService.save(link);
                return true;
            } else {
                return false;
            }
        }

    }
}

