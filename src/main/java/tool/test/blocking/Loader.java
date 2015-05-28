package tool.test.blocking;

import date.Dates;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import selenium.elements.Condition;
import selenium.elements.Search;

import java.util.Calendar;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.Callable;

/**
 * User: YamStranger
 * Date: 5/28/15
 * Time: 7:04 PM
 */
public class Loader implements Callable<Integer> {
    private final Search refs;
    private final String url;
    private final WebDriver driver;
    private List<String> urls = new LinkedList<>();

    public Loader(String url, WebDriver webDriver) {
        this.driver = webDriver;
        this.refs = new Search("All refs on page", new Condition(By.xpath("//*/a[contains(@href,\"" + url + "\")]")));
        this.url = "http://" + url;
    }

    @Override
    public Integer call() throws Exception {
        Integer request = 0;
        try {
            this.driver.get(url);
            request++;
            Dates start = new Dates();
            int max = 0;
            int count = 0;
            List<WebElement> refs = new LinkedList<>();
            while (new Dates().difference(start, Calendar.SECOND) < 30 && count < 3) {
                refs = this.refs.all(this.driver);
                if (max < refs.size()) {
                    max = refs.size();
                } else {
                    count++;
                    sleep();
                }
            }
            if (refs.isEmpty()) {
                throw new RuntimeException("site without refs");
            }

            for (final WebElement ref : refs) {
                this.urls.add(ref.getAttribute("href"));
            }

            for (final String reference : this.urls) {
                this.driver.get(reference);
                request++;
            }

            this.driver.get(url);
            request++;
            int current = 0;
            count = 0;
            start = new Dates();
            while (new Dates().difference(start, Calendar.SECOND) < 30 && count < 3) {
                refs = this.refs.all(this.driver);
                if (current < refs.size()) {
                    current = refs.size();
                } else {
                    count++;
                    sleep();
                }
            }
            if (max*0.5 > current) {
                throw new RuntimeException("refs count changed");
            }
        } finally {
            this.driver.quit();
        }
        return request;
    }

    private final void sleep() {
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
