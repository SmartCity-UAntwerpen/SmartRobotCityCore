package be.uantwerpen.sc.services.newMap;

import be.uantwerpen.sc.models.Bot;
import be.uantwerpen.sc.models.map.newMap.Link;
import be.uantwerpen.sc.repositories.newMap.LinkRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class LinkControlService {

    @Autowired
    private LinkRepository links;

    /**
     * Encapsulator for finding all available links
     * @return List of links
     */
    public List<Link> getAllLinks()
    {
        return links.findAll();
    }

    /**
     * Encapsulator for finding a link by id
     * @param id ID of link
     * @return Link
     */
    public Link getLink(Long id)
    {
        return links.findOne(id);
    }

    /**
     * Encapsulator for saving a link
     * @param link link to save
     */
    public void save(Link link)
    {
        links.save(link);
    }

    /**
     * Lock the tile of which a point belongs to
     * @param id, point id
     * @param status, lock status
     */
    public void setLock(Long id,Boolean status, Bot bot){
        this.getLink(id).lockLink(status,bot);
    }

    /**
     * Returns the lock status of a Tile of which a point belongs to
     * @param id, point id
     * @return lock status
     */
    public Boolean getLock(Long id){
        return this.getLink(id).getLockStatus();
    }

}