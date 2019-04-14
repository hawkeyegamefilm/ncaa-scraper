import com.footballscience.domain.Drive
import com.footballscience.domain.Game
import com.footballscience.domain.Play
import com.footballscience.parser.PlayType
import com.footballscience.parser.ScoreTextParserLib
import com.footballscience.scraper.PlayScraper
import org.codehaus.jackson.map.ObjectMapper
import spock.lang.Specification
import spock.lang.Unroll

class PlayScraperTest extends Specification {

    PlayScraper scraper
    String testUrl = 'http://data.ncaa.com/game/football/fbs/2014/08/30/uni-iowa/pbp.json'
    String testUrl2018 = 'https://data.ncaa.com/game/football/fbs/2018/09/01/northern-ill-iowa/pbp.json'
    ObjectMapper objectMapper

    def setup() {
        scraper = new PlayScraper()
        objectMapper = new ObjectMapper()
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

    def "parseGameByDrives - println test "() {
        when:
        List<Drive> result = scraper.parseGameByDrives(scraper.getJsonFromUrl(testUrl2018))

        then:
        result
        result.eachWithIndex { Drive drive, Integer dIndex ->
            println "--------------${dIndex}-----------------"
            drive.plays.each { Play play -> println play.toCsvRow()}
            println "--------------END-----------------"
//            drive.plays.each { Play play ->
//                if(play.playType == PlayType.RUSH && play.teamId == 71) {
//                    println(play.driveNumber)
//                    println(play.down)
//                    println(play.ytg)
//
//                }
//            }
        }
    }

    def "create game from drives"() {
        when:
        List<Drive> result = scraper.parseGameByDrives(scraper.getJsonFromUrl(testUrl2018))
        Game gameResult = ScoreTextParserLib.createGameFromDrives(result)

        then:
        gameResult.homeScore == 33
        gameResult.visitorScore == 7
        gameResult.drives.size() == 27

        //find rushing total for Iowa
        List<Drive> iowaDrives = gameResult.drives.collect { Drive drive ->
            if(drive.teamId == 71) { return drive}
        }.findAll()
        iowaDrives.size() == 14

        List<Play> iowaRushes = []
        List<Play> iowaPasses = []

        for(Drive drive: iowaDrives) {
            for(Play play: drive.plays) {
                if(play.playType == PlayType.RUSH) {
                    iowaRushes.add(play)
                } else if(play.playType == PlayType.PASS) {
                    iowaPasses.add(play)
                }
            }
        }

//        List<Play> iowaRushes = iowaDrives.each { Drive drive ->
//            return drive.plays.collect { Play play ->
//                if(play.playType == PlayType.RUSH){return play}
//            }
//        }
        List<Play> ikmRushes = iowaRushes.findAll{ it.fullScoreText.contains("21-I.Kelly-Martin")}
//        iowaPasses.each {println(it.fullScoreText)}
        int ikmYards = 0
        for(Play play: ikmRushes) {
            ikmYards += play.rush.yards
        }

        ikmRushes.size() == 16
        ikmYards == 62//this fails until yards parse correctly
        int iowaRushYards = 0
        iowaRushes.size() == 48
        for(Play play: iowaRushes) {
            iowaRushYards += play.rush.yards
        }
        iowaRushYards == 209

        //passes
        iowaPasses.size() == 25
        int passYards = 0
        for(Play play: iowaPasses) {
            passYards += play.pass.yards
        }
        passYards == 143
    }


    def "parseGameByDrives - validate drives "() {
        when:
        List<Drive> result = scraper.parseGameByDrives(scraper.getJsonFromUrl(testUrl2018))

        then:
        result
        result.eachWithIndex { Drive drive, Integer dIndex ->
            println drive.toCsvRow()
        }
    }
    def "create csv test - local file"() {
        when:
        PlayScraper scraper = new PlayScraper(year: "2016", month: "8", day: "30")
        List<Drive> result = scraper.parseGameByDrives(objectMapper.readValue(readResourceText("sampleGame.json"), Map))

        then:
        result
        result.size() == 26
    }

    def "populate global vars"() {
        when:
        scraper.populateDateVars(testUrl)

        then:
        scraper.year == '2014'
        scraper.month == '08'
        scraper.day == '30'
    }

    static String readResourceText(String resourcePath) {
        URL url = PlayScraperTest.getResource(resourcePath)
        assert url, "resource not found: ${resourcePath}"
        return url.getText("utf-8")
    }
}
