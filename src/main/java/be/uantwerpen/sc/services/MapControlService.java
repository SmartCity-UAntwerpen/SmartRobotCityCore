package be.uantwerpen.sc.services;

import be.uantwerpen.sc.models.*;
import be.uantwerpen.sc.models.map.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Map Control Service
 */
@Service
public class MapControlService
{
    /**
     * Autowired Point Control Service
     */
    @Autowired
    private PointControlService pointControlService;

    /**
     * Autowired Link Control Service
     */
    @Autowired
    private LinkControlService linkControlService;

    /**
     * Autowired Bot Control Service
     */
    @Autowired
    private BotControlService botControlService;

    /**
     * Autowired TrafficLight Control Service
     */
    @Autowired
    private TrafficLightControlService trafficLightControlService;

    /**
     * Creates map from DB links
     * @return Created Map
     */
    public Map buildMap()
    {
        Map map = new Map();

        List<Link> linkEntityList = linkControlService.getAllLinks();

        for(Point point : pointControlService.getAllPoints())
        {
            Node node = new Node(point);
            List<Link> targetLinks = linkEntityList.stream().filter(item -> Objects.equals(item.getStartPoint().getId(), node.getNodeId())).collect(Collectors.toList());

            node.setNeighbours(targetLinks);
            map.addNode(node);
        }

        map.setBotEntities(botControlService.getAllBots());
        map.setTrafficlightEntity(trafficLightControlService.getAllTrafficLights());

        return map;
    }

    /**
     * Gets all links and points from the database
     * Connects all links to their specific nodes and sets them as neighbors
     * Adds all these nodes to the map
     * Map creates both real (Correct RFID) as simulated (letter of the alphabet) maps
     * @return Created JSON Map
     */
    public MapJson buildMapJson()
    {
        MapJson mapJson = new MapJson();
        List<Link> linkEntityList = linkControlService.getAllLinks();
        List<Point> points=pointControlService.getAllPoints();
        for(Point point : points)
        {
            NodeJson nodeJson = new NodeJson(point);
            List<Neighbour> neighbourList = new ArrayList<>();

            for(Link link: linkEntityList)
                if((link.getStartPoint().getId()) == (nodeJson.getPointEntity().getId()))
                    neighbourList.add(new Neighbour(link));

            nodeJson.setNeighbours(neighbourList);
            mapJson.addNodeJson(nodeJson);
        }

        return mapJson;
    }

    /**
     * Creates MapNew based on DB Links and points
     * TODO: MapNew renaming
     * @return MapNew
     */
    public MapNew buildNewMap(){
        MapNew mapNew = new MapNew();
        mapNew.setLinkList(linkControlService.getAllLinks());
        mapNew.setPointList(pointControlService.getAllPoints());

        return mapNew;
    }
}
