import com.footballscience.scraper.RosterScraper
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import spock.lang.Ignore
import spock.lang.Specification

class RosterScraperTest extends Specification {

    RosterScraper rosterScraper

    def setup() {
        rosterScraper = new RosterScraper()
    }

    def "scrapes html correctly and produces csv"() {
        setup:
        Document doc = Jsoup.parse(RosterScraperTest.getResource("sampleRoster.html").text)

        when:
        String results = rosterScraper.createCSVRowFromHtml(doc, "312")

        then:
        results == RosterScraperTest.getResource("expectedRoster.csv").text
    }

    def "loads orgs from resource"() {
        when:
        List orgs = rosterScraper.getAllOrgIds()

        then:
        orgs.size() == 247
    }

    def "writes file"() {
        setup:
        rosterScraper.getAllOrgIds()

        when:
        rosterScraper.writeFile("blah,who,what" + System.lineSeparator() + "line2,blah,things")

        then:
        println RosterScraper.getResource("results.csv").text
    }

    @Ignore
    def "run load"() {
        when:
        rosterScraper.runLoad()

        then:
        1 == 1
    }

}
