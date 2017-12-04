package be.uantwerpen.sc.services;

import be.uantwerpen.sc.controllers.BotController;
import be.uantwerpen.sc.controllers.CostController;
import be.uantwerpen.sc.controllers.JobController;
import be.uantwerpen.sc.models.Bot;
import be.uantwerpen.sc.models.Link;
import be.uantwerpen.sc.tools.Terminal;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * Service for running the Terminal
 */
@Service
public class TerminalService
{
    @Autowired
    private JobService jobService;
    
    @Autowired
    private BotControlService botControlService;

    @Autowired
    private PointControlService pointControlService;

    @Autowired
    private CostController costController;

    @Autowired
    private BotController botController;

    @Autowired
    private JobController jobController;
    @Autowired
    private TimerService timerService;

    private Terminal terminal;

    /**
     * Default constructor that binds the Terminal's ExecuteCommand to ParseCommand
     */
    public TerminalService()
    {
        terminal = new Terminal()
        {
            @Override
            public void executeCommand(String commandString)
            {
                parseCommand(commandString);
            }
        };
    }

    /**
     * Starts TimerService and activates Terminal
     */
    public void systemReady()
    {
        try {
            new Thread(timerService).start();
            System.out.println("Timer started");
        }catch(Exception e){
            System.out.println("Timer not started");
        }

        terminal.printTerminal("\nSmartCity Backend [Version " + getClass().getPackage().getImplementationVersion() + "]\n(c) 2015-2017 University of Antwerp. All rights reserved.");
        terminal.printTerminal("Type 'help' to display the possible commands.");
        terminal.activateTerminal();
    }

    /**
     * Parses Terminal commands
     * Commands:
     *      job [botID] [command] //TODO Kanker
     *      show [bots]         TODO: Shit command, only Bots? Add Traffic Lights or something
     *      reset
     *      delete [botID]
     *      exit
     *      help
     *      ?
     *      calcweight [start] [stop]
     * @param commandString
     */
    private void parseCommand(String commandString)
    {
        String command = commandString.split(" ", 2)[0].toLowerCase();

        switch(command)
        {
            case "job":
                if(commandString.split(" ", 3).length <= 2)
                {
                    terminal.printTerminalInfo("Missing arguments! 'job {botId} {command}");
                }
                else
                {
                    int parsedInt;

                    try
                    {
                        parsedInt = this.parseInteger(commandString.split(" ", 3)[1]);

                        this.sendJob((long)parsedInt, commandString.split(" ", 3)[2]);

                    }
                    catch(Exception e)
                    {
                        terminal.printTerminalError(e.getMessage());
                    }
                }
                break;
            case "show":
                if(commandString.split(" ", 2).length <= 1)
                {
                    terminal.printTerminalInfo("Missing arguments! 'show {bots}'");
                }
                else
                {
                    if(commandString.split(" ", 2)[1].equals("bots"))
                    {
                        this.printAllBots();
                    }
                    else
                    {
                        terminal.printTerminalInfo("Unknown arguments! 'show {bots}'");
                    }
                }
                break;
            case "reset":
                this.deleteBots();
                this.clearPointLocks();
                break;
            case "delete":
                if(commandString.split(" ", 2).length <= 1)
                {
                    terminal.printTerminalInfo("Missing arguments! 'delete {botId}'");
                }
                else
                {
                    try
                    {
                        int parsedInt = this.parseInteger(commandString.split(" ", 2)[1]);

                        this.deleteBot(parsedInt);
                    }
                    catch(Exception e)
                    {
                        terminal.printTerminalError(e.getMessage());
                    }
                }
                break;
            case "exit":
                exitSystem();
                break;
            case "help":
            case "?":
                printHelp("");
                break;
            case "calcweight":
                if(commandString.split(" ", 3).length <= 2)
                {
                    terminal.printTerminalInfo("Missing arguments! 'calcWeight {start} {stop}");
                }
                else
                {
                    int parsedInt;

                    try//TODO
                    {
                        parsedInt = this.parseInteger(commandString.split(" ", 3)[1]);
                        //JSONArray a = costController.calcWeight(parsedInt, this.parseInteger(commandString.split(" ", 3)[2]));
                        //JSONArray b = botController.posAll();
                        String data = "{\"pointList\" :[{\"id\" : 1, \"rfid\" :\"4e\", \"pointLock\" : 0}, {\"id\" : 2, \"rfid\" :\"3r\", \"pointLock\" : 0}, {\"id\" : 3, \"rfid\" :\"9d\", \"pointLock\" : 0}, {\"id\" : 4, \"rfid\" :\"1y\", \"pointLock\" : 0}], \"linkList\" :[{\"id\" : 1, \"length\" : 1, \"startPoint\" : 1, \"stopPoint\" : 3, \"startDirection\" : \"N\", \"stopDirection\" : \"Z\", \"weight\" :1}, {\"id\" : 2, \"length\" : 1, \"startPoint\" : 3, \"stopPoint\" : 1, \"startDirection\" : \"Z\", \"stopDirection\" : \"N\", \"weight\" :1}, {\"id\" : 3, \"length\" : 1, \"startPoint\" : 3, \"stopPoint\" : 4, \"startDirection\" : \"N\", \"stopDirection\" : \"Z\", \"weight\" :1}, {\"id\" : 4, \"length\" : 1, \"startPoint\" : 4, \"stopPoint\" : 3, \"startDirection\" : \"Z\", \"stopDirection\" : \"N\", \"weight\" :1}, {\"id\" : 5, \"length\" : 1, \"startPoint\" : 4, \"stopPoint\" : 2, \"startDirection\" : \"W\", \"stopDirection\" : \"O\", \"weight\" :1}, {\"id\" : 6, \"length\" : 1, \"startPoint\" : 2, \"stopPoint\" : 4, \"startDirection\" : \"O\", \"stopDirection\" : \"W\", \"weight\" :1}]}";
                        //costController.getMap();
                        botController.getNewId();
                        //System.out.println(b.toString());
                    }
                    catch(Exception e)
                    {
                        terminal.printTerminalError(e.getMessage());
                    }
                }
                break;
            default:
                terminal.printTerminalInfo("Command: '" + command + "' is not recognized.");
                break;
        }
    }

    /**
     * Program exits, used as command
     */
    private void exitSystem()
    {
        System.exit(0);
    }

    /**
     * Prints help, used as command
     * TODO: Bullshit function, doesn't use parameter
     * @param command Command to get help about
     */
    private void printHelp(String command)
    {
        switch(command)
        {
            default:
                terminal.printTerminal("Available commands:");
                terminal.printTerminal("-------------------");
                terminal.printTerminal("calcWeight {start} {stop}");
                terminal.printTerminal("'job {botId} {command}' : send a job to the bot with the given id.");
                terminal.printTerminal("'show {bots}' : show all bots in the database.");
                terminal.printTerminal("'reset' : remove all bots from the database and release all point locks.");
                terminal.printTerminal("'delete {botId}' : remove the bot with the given id from the database.");
                terminal.printTerminal("'exit' : shutdown the server.");
                terminal.printTerminal("'help' / '?' : show all available commands.\n");
                break;
        }
    }

    /**
     * Prints information about all available Bots
     */
    private void printAllBots()
    {
        List<Bot> bots = botControlService.getAllBots();

        if(bots.isEmpty())
        {
            terminal.printTerminalInfo("There are no bots available to list.");
        }
        else
        {
            terminal.printTerminal("Bot-id\t\tLink-id\t\tStatus");
            terminal.printTerminal("------------------------------");

            for(Bot bot : bots)
            {
                Long linkId = -1L;
                Link link = bot.getLinkId();

                if(link != null)
                {
                    linkId = link.getId();
                }

                terminal.printTerminal("\t" + bot.getId() + "\t\t" + linkId + "\t\t\t" + bot.getState());
            }
        }
    }

    /**
     * Deletes Bot, used as command
     * @param botID ID of the Bot to delete
     */
    private void deleteBot(int botID)
    {
        if(botControlService.deleteBot(botID))
        {
            terminal.printTerminalInfo("Bot deleted with id: " + botID + ".");
        }
        else
        {
            terminal.printTerminalError("Could not delete bot with id: " + botID + "!");
        }
    }

    /**
     * Resets available bots, used as command
     */
    private void deleteBots()
    {
        botControlService.deleteBots();
        terminal.printTerminalInfo("All bot entries cleared from database.");
    }

    /**
     * Clears all point locks, used as command
     */
    private void clearPointLocks()
    {
        pointControlService.clearAllLocks();
        terminal.printTerminalInfo("All points are released.");
    }

    /**
     * Sends Job to defined bot
     * TODO: Check comments
     * @param botId ID of bot
     * @param command Command to send
     */
    private void sendJob(Long botId, String command)
    {
        if(botControlService.getBot((long)botId) == null)
        {
            //Could not find bot in database
            terminal.printTerminalError("Could not find bot with id: " + botId + "!");

            return;
        }

        /*if(jobService.sendJob(botId, command))
        {
            terminal.printTerminalInfo("Job send to bot with id: " + botId + ".");
        }
        else
        {
            terminal.printTerminalError("Could not send job to bot with id: " + botId + "!");
        }*/
    }

    /**
     * Parses Int from String with exception
     * TODO: Bullshit function
     * @param value String to parse for int
     * @return Int found
     * @throws Exception If string is not an int
     */
    private int parseInteger(String value) throws Exception
    {
        int parsedInt;

        try
        {
            parsedInt = Integer.parseInt(value);
        }
        catch(NumberFormatException e)
        {
            throw new Exception("'" + value + "' is not an integer value!");
        }

        return parsedInt;
    }

    /**
     * Parses long from String with exception
     * TODO: Bullshit function
     * @param value String to parse for long
     * @return Long found
     * @throws Exception If string is not a long
     */
    private long parseLong(String value) throws Exception
    {
        Long parsedLong;

        try
        {
            parsedLong = Long.parseLong(value);
        }
        catch(NumberFormatException e)
        {
            throw new Exception("'" + value + "' is not a numeric value!");
        }

        return parsedLong;
    }
}
