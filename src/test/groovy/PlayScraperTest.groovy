import com.footballscience.scraper.PlayScraper
import spock.lang.Specification
import spock.lang.Unroll

class PlayScraperTest extends Specification {

    PlayScraper scrapper
    String testUrl = 'http://data.ncaa.com/jsonp/game/football/fbs/2014/08/30/uni-iowa/pbp.json'

    def setup() {
        scrapper = new PlayScraper()
    }

    def 'test convert to map'() {
        when:
        Map result = scrapper.getJsonFromUrl(testUrl)

        then:
        result.meta
        result.meta.teams
        result.periods

    }

    def 'strips tags correctly'() {
        when:
        String result = scrapper.cleanupTags(readResourceText("sampleGame.json"))

        then:
        !result.contains("callbackWrapper(")
        !result.contains(");")

    }

    def "create csv test"() {
        when:
        String result = scrapper.createCSVFromMap(scrapper.getJsonFromUrl(testUrl))

        then:
        result
        println result
    }

    def "populate global vars"() {
        when:
        scrapper.populateDateVars(testUrl)

        then:
        scrapper.year == '2014'
        scrapper.month == '08'
        scrapper.day == '30'
    }

    @Unroll
    def "caclulate Spot"() {
        setup:
        scrapper.createIdMap([id: 71], [id:920])

        when:
        Integer spot = scrapper.calculateSpot(driveText, teamId)

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
