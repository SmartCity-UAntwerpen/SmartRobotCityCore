package be.uantwerpen.sc.services;

import be.uantwerpen.rc.models.Bot;
import be.uantwerpen.rc.models.map.Map;
import be.uantwerpen.rc.models.map.Point;
import be.uantwerpen.sc.controllers.mqtt.MqttJobPublisher;
import be.uantwerpen.rc.models.Job;
import be.uantwerpen.sc.repositories.JobRepository;
import be.uantwerpen.sc.tools.NavigationParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.util.List;
import java.util.TreeMap;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 * @author Dieter 2018-2019
 * <p>
 * Job Service
 */
@Service
public class JobControlService implements Runnable {
    /**
     * Autowired Job repository
     */
    @Autowired
    private JobRepository jobs;

    /**
     * Autowired MQTT Publisher
     */
    @Autowired
    private MqttJobPublisher mqttJobPublisher;

    /**
     * Autowired Botcontrol Service
     */
    @Autowired
    private BotControlService botControlService;

    /**
     * Autowired MapControl Service
     */
    @Autowired
    private MapControlService mapControlService;

    /**
     * Autowired Pathplannig service
     */
    @Autowired
    private PathPlanningService pathPlanningService;

    /**
     * Autowired PointControlService service
     */
    @Autowired
    private PointControlService pointControlService;


    //TODO: remove blocking queue ==> use database
    private BlockingQueue<Job> jobQueue = null;

    private Logger logger = LoggerFactory.getLogger(JobControlService.class);

    @PostConstruct
    public void init() {
        logger.info("Initialising job queue...");
        //Initialize job queue and add all jobs from database
        jobQueue = new ArrayBlockingQueue<Job>(100);
        jobQueue.addAll(jobs.findAllByBotNull());
        logger.info(jobs.findAllByBotNull().size() + " jobs added to the job queue! Init done!");
    }

    /**
     * Delete all jobs from database
     */
    public void deleteAllJobs()
    {
        List<Job> jobs = this.jobs.findAll();
        jobQueue = new ArrayBlockingQueue<Job>(100); //Reinitialize the jobQueue
        this.jobs.delete(jobs);
    }


    /**
     * Send job over MQTT
     *
     * @param jobId   ID of job for bot
     * @param bot     The bot that executes the job
     * @param idStart ID start Point
     * @param idStop  ID stop Point
     * @return Success
     */
    private boolean sendJob(Long jobId, Bot bot, long idStart, long idStop) {
        Job job = new Job(jobId);
        job.setIdStart(idStart);
        job.setIdEnd(idStop);
        job.setBot(bot);
        return mqttJobPublisher.publishJob(job, bot.getIdCore());
    }

    /**
     * Update a job in database
     *
     * @param job, the job
     */
    public void saveJob(Job job) {
        jobs.save(job);
    }

    /**
     * Get the jobs the bot is executing
     *
     * @param bot, the bot
     * @return list of all jobs the bot is executing (this list should only contain one item)
     */
    public List<Job> getExecutingJob(Bot bot) {
        return jobs.findAllByBot(bot);
    }

    /**
     * Adds a job to the blocking queue
     *
     * @param jobId   ID of the job
     * @param idStart ID of starting point
     * @param idEnd  ID of end point
     * @return
     */
    public boolean queueJob(Long jobId, long idStart, long idEnd) {
        //Check if points exist
        try {
            pointControlService.getPoint(idStart);
        } catch (Exception e) {
            return false;
        }
        try {
            pointControlService.getPoint(idEnd);
        } catch (Exception e) {
            return false;
        }

        //Create new job and add to queue
        if (jobId == null) {
            jobId = 9999L;
        }
        Job job = new Job(jobId);
        job.setIdStart(idStart);
        job.setIdEnd(idEnd);
        try {
            boolean tmp = jobQueue.add(job);
            jobs.save(job);
            logger.info("New job queued!\tId: " + job.getJobId() + "\tStart: " + job.getIdStart() + "\tEnd: " + job.getIdEnd());
            return true;
        } catch (IllegalStateException e) {
            logger.error("Error adding job to job queue!");
            return false;
        }
    }

    @Override //TODO:: this run method can be migrated to another class (not clean to make a Thread from a Service-class)
    public void run() {
        logger.info("Starting Job Service...");
        if (jobQueue != null && !jobQueue.isEmpty())
        {
            while (true) {
                //Process that checks the queue and seeks a bot that can execute the job
                try {
                    if (!botControlService.getAllAvailableBots().isEmpty())
                    {
                        Job job = jobQueue.take();
                        //Find closest bot
                        List<Bot> bots = botControlService.getAllAvailableBots();
                        TreeMap<Integer, Bot> sortedBots = new TreeMap<>();
                        for (Bot b : bots) {
                            int targetId = -1;
                            //Depending on the type of job, calculate how far the bot is
                            if (job.getIdStart() == -1L) {
                                //Go to point job ==> which bot is closest to end
                                targetId = job.getIdEnd().intValue();
                            } else {
                                //Normal job ==> which bot is closest to start point
                                targetId = job.getIdStart().intValue();
                            }
                            sortedBots.put((int) pathPlanningService.CalculatePathWeight(b.getPoint().intValue(), targetId), b);
                        }

                        //Get closest bot == bot with least cost == first entry (key who has the lowest value) and assign job
                        Bot bot = sortedBots.firstEntry().getValue();
                        //If the start point is -1L than this is a goToPoint job ==> set start point to current location of the bot
                        if (job.getIdStart() == -1L) {
                            job.setIdStart(bot.getPoint());
                        }
                        bot.setBusy(true);
                        bot.setIdStart(job.getIdStart());
                        bot.setIdStop(job.getIdEnd());
                        bot.setJobId(job.getJobId());
                        job.setBot(bot);
                        botControlService.saveBot(bot);
                        jobs.save(job);
                        //Send MQTT message to bot
                        if(bot.getWorkingMode().equals("INDEPENDENT"))
                            this.sendJob(job.getJobId(), bot, job.getIdStart(), job.getIdEnd());
                        else if(bot.getWorkingMode().equals("FULLSERVER"))
                        {
                            Map map = this.mapControlService.getMap();
                            if(map != null)
                            {
                                List<Point> path = this.pathPlanningService.Calculatepath(map, job.getIdStart(), job.getIdEnd());
                                NavigationParser navigationParser;
                                if(job.getIdStart() == -1L)
                                    navigationParser = new NavigationParser(path, map, false);
                                else
                                    navigationParser = new NavigationParser(path, map, true);
                                navigationParser.parseMap();
                                job.setDriveDirections(navigationParser.getCommands());
                                this.sendJob(job.getJobId(), bot, job.getIdStart(), job.getIdEnd());
                            }
                        }
                    } else {
                        //Sleep seconds
                        TimeUnit.SECONDS.sleep(2);
                    }
                } catch (Exception e) {
                    logger.error("Error taking job from queue: " + e.getMessage());
                }
            }
        }
        else
        {
            logger.info("JobQueue is null or is empty!");
        }


    }
}
