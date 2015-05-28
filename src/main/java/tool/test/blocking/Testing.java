package tool.test.blocking;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.joran.JoranConfigurator;
import ch.qos.logback.core.joran.spi.JoranException;
import command.line.ArgumentsParser;
import date.Dates;
import org.openqa.selenium.WebDriver;
import org.slf4j.LoggerFactory;
import selenium.WebDriverHub;

import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * User: YamStranger
 * Date: 5/28/15
 * Time: 5:15 PM
 */
public class Testing {
    static {
        //configure logging
        try {
            LoggerContext context = (LoggerContext) LoggerFactory.getILoggerFactory();
            JoranConfigurator jc = new JoranConfigurator();
            jc.setContext(context);
            context.reset(); // override default configuration
            // inject the name of the current application as "application-name"
            // property of the LoggerContext
            jc.doConfigure("logback.xml");
        } catch (JoranException e) {
            System.out.println("could not configure logging " + e);
        }
    }

    public static void main(String... args) {
        ArgumentsParser parser = new ArgumentsParser(args);
        Map<String, String> arguments = parser.arguments();
        String url = arguments.get("url");
        if (url == null || url.isEmpty()) {
            throw new RuntimeException("please add argument url of testing site");
        }
        String hub = arguments.get("hub");
        if (hub == null || hub.isEmpty()) {
            throw new RuntimeException("please add argument hub");
        }

        List<String> hubs = new LinkedList<>();
        if (hub.contains(";")) {
            hubs.addAll(Arrays.asList(hub.split(";")));
        } else {
            hubs.add(hub);
        }

        String threads = arguments.get("threads");
        if (threads == null || threads.isEmpty()) {
            throw new RuntimeException("please add argument threads");
        }

        System.out.println("started " + new Dates());
        ExecutorService tasks = Executors.newFixedThreadPool(Integer.valueOf(threads));
        WebDriverHub manager = new WebDriverHub(true, true, hubs, 120, Integer.valueOf(threads));

        System.out.println("test site " + "http://" + url);
        WebDriver driver = null;
        try {
            driver = manager.driver();
            driver.get("http://" + url);
        } catch (Exception e) {
            System.out.println("some error with url");
        } finally {
            driver.quit();
        }

        List<Future<Integer>> results = new LinkedList<>();
        int count = 0;
        int requests = 0;
        Dates start = new Dates();
        int speed = 0;
        Dates last = new Dates();
        while (count <= 1000) {
            results.add(tasks.submit(new Loader(url, manager.driver())));
            try {
                Iterator<Future<Integer>> iter = results.iterator();
                while (iter.hasNext()) {
                    Future<Integer> res = iter.next();
                    if (res.isDone()) {
                        iter.remove();
                        requests += res.get();
                        if (new Dates().difference(last, Calendar.MINUTE) > 1) {
                            last = new Dates();
                            long min = new Dates().difference(start, Calendar.MINUTE);
                            System.out.print(" " + min + ":" + requests + ":" + (requests/min) + "inmin");
                        }
                        count--;
                    }
                }

            } catch (Exception e) {
                count++;
            }
        }
        System.out.println("May be blocked in " + requests + " requests during " + new Dates().difference(start, Calendar.MINUTE) + " minutes");
        manager.quit();
    }
}
