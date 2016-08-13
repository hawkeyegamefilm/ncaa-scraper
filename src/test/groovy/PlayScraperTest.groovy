import com.footballscience.scraper.PlayScraper
import spock.lang.Specification
import spock.lang.Unroll

class PlayScraperTest extends Specification {

    PlayScraper scraper
    String testUrl = 'http://data.ncaa.com/jsonp/game/football/fbs/2014/08/30/uni-iowa/pbp.json'

    def setup() {
        scraper = new PlayScraper()
    }

    def 'test convert to map'() {
        when:
        Map result = scraper.getJsonFromUrl(testUrl)

        then:
        result.meta
        result.meta.teams
        result.periods

    }

    def 'strips tags correctly'() {
        when:
        String result = scraper.cleanupTags(readResourceText("sampleGame.json"))

        then:
        !result.contains("callbackWrapper(")
        !result.contains(");")

    }

    def "create csv test"() {
        when:
        String result = scraper.createPlayRowsCSV(scraper.getJsonFromUrl(testUrl))

        then:
        result
        println result
    }

    def "populate global vars"() {
        when:
        scraper.populateDateVars(testUrl)

        then:
        scraper.year == '2014'
        scraper.month == '08'
        scraper.day == '30'
    }

    @Unroll
    def "caclulate Spot"() {
        setup:
        scraper.populateRosters([id:"71"],[id: "920"])

        when:
        Integer spot = scraper.calculateSpot(driveText, teamId)

        then:
        spot == expectedResult

        where:
        driveText             | teamId | expectedResult
        "1st and 10 at UNI35" | 71     | 65
        "1st and 10 at UNI41" | 71     | 59
        "1st and 10 at UNI30" | 71     | 70
        "1st and 10 at UNI35" | 920    | 35
        "1st and 10 at UNI41" | 920    | 41
        "1st and 10 at UNI30" | 920    | 30
    }

    static String readResourceText(String resourcePath) {
        URL url = PlayScraperTest.getResource(resourcePath)
        assert url, "resource not found: ${resourcePath}"
        return url.getText("utf-8")
    }
}
