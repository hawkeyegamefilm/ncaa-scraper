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

    def "scrapes html correctly and produces csv for 2018 roster"() {
        setup:
        Document doc = Jsoup.parse(RosterScraperTest.getResource("sampleRoster2018.html").text)

        when:
        String results = rosterScraper.createCSVRowFromHtml(doc, "306")

        then:
        results == RosterScraperTest.getResource("expectedRoster2018.csv").text
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

    def "find football url on org summary page"() {
        setup:
        Document doc = Jsoup.parse(RosterScraperTest.getResource("sampleOrgSummary.html").text)

        when:
        String result = rosterScraper.getFootballUrl(doc)

        then:
        result == "/teams/449824"
    }

    def "find roster url on team summary page"() {
        setup:
        Document doc = Jsoup.parse(RosterScraperTest.getResource("sampleTeamHomepage.html").text)

        when:
        String result = rosterScraper.findRosterPathOnTeamPage(doc)

        then:
        result == "/team/306/roster/14280"
    }

    def "url path test"() {
        when:
        String relativeRosterUrl = rosterScraper.findRosterUrlbyOrg("306")
        then:
        relativeRosterUrl == "/team/306/roster/14280"
    }

    @Ignore
    def "run load"() {//just for creating roster file
        when:
        rosterScraper.runLoad()

        then:
        1 == 1
    }

    @Ignore
    def "run load 2k18"() {//just for creating roster file
        when:
        rosterScraper.runLoad2018()

        then:
        1 == 1
    }

}
