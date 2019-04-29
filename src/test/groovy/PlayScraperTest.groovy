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

    def "validate '18 Iowa game results from stats"() {
        when:
        List<Drive> result = scraper.parseGameByDrives(objectMapper.readValue(readResourceText(gameUrl), Map))
        Game gameResult = ScoreTextParserLib.createGameFromDrives(result)

        then:
        gameResult.homeScore == expectedHomeScore
        gameResult.visitorScore == expectedVisitorScore
        gameResult.drives.size() == expectedDrives

        where:
        gameUrl                     | expectedDrives | expectedIowaDrives | expectedHomeScore | expectedVisitorScore
        'iowa-18/iowa-niu-18.json'  | 27             | 14                 | 33                | 7
        'iowa-18/iowa-isu-18.json'  | 23             | 12                 | 13                | 3
        'iowa-18/iowa-uni-18.json'  | 23             | 12                 | 38                | 14
        'iowa-18/iowa-wisc-18.json' | 19             | 9                  | 17                | 28

    }

    //scraper.parseGameByDrives(objectMapper.readValue(readResourceText("sampleGame.json"), Map))
    /**
     * Iowa vs NIU : No errors found
     * Iowa vs ISU : Two bad plays found in json
     *      "4-N.Stanley sacked at ISU 39 for -2 yards, FUMBLES (42-M.Spears). 3-J.Bailey to ISU 35 for no gain." - play never happened
     *      "4-N.Stanley complete to 38-T.Hockenson. 38-T.Hockenson to IOW 26 for 8 yards (6-D.Ruth)." - play is replaced by correct yardage one play below, 9y instead of 8
     * Iowa vs UNI
     * @return
     */
    @Unroll
    def "validate '18 Iowa rushes from stats"() {
        when:
        List<Drive> result = scraper.parseGameByDrives(objectMapper.readValue(readResourceText(gameUrl), Map))
        Game gameResult = ScoreTextParserLib.createGameFromDrives(result)

        then:
        //find rushing total for Iowa
        List<Drive> iowaDrives = gameResult.drives.collect { Drive drive ->
            if(drive.teamId == 71) { return drive}
        }.findAll()

        List<Play> iowaRushes = []
        List<Play> iowaPasses = []

        for(Drive drive: iowaDrives) {
            for(Play play: drive.plays) {
                if(play.playType == PlayType.RUSH && play.rush.attempt == 1) {
                    iowaRushes.add(play)
                } else if(play.playType == PlayType.PASS) {
                    iowaPasses.add(play)
                }
            }
        }

        List<Play> ikmRushes = iowaRushes.findAll{ it.fullScoreText.contains("21-I.Kelly-Martin")}
        List<Play> tyRushes = iowaRushes.findAll{ it.fullScoreText.contains("28-T.Young")}
        List<Play> msRushes = iowaRushes.findAll{ it.fullScoreText.contains("10-M.Sargent")}
        List<Play> stanleyRushes = iowaRushes.findAll{ it.fullScoreText.contains("4-N.Stanley")}

        int ikmYards = 0
        if(ikmRushes.size() > 0) {
            ikmRushes.each {ikmYards += it.rush.yards}
        }
        ikmRushes.size() == expectedIkmRushes
        ikmYards == expectedIkmYards

        int tyYards = 0

        tyRushes.each { tyYards += it.rush.yards}
        tyRushes.size() == expectedTYRushes
        tyYards == expectedTYYards


        int msYards = 0
        msRushes.each {msYards += it.rush.yards}
        msRushes.size() == expectedMSRushes
        msYards == expectedMSYards

        int stanleyYards = 0
        stanleyRushes.each {stanleyYards += it.rush.yards}
        stanleyRushes.size() == expectedStanleyRushes
        stanleyYards == expectedStanleyYards

        int iowaRushYards = 0
        iowaRushes.size() == expectedIowaRushes
        for(Play play: iowaRushes) {
            iowaRushYards += play.rush.yards
        }
        iowaRushYards == expectedIowaRushYards

        where:
        gameUrl                     | expectedIowaRushes | expectedIowaRushYards | expectedIkmRushes | expectedIkmYards | expectedTYRushes | expectedTYYards | expectedMSRushes | expectedMSYards | expectedStanleyRushes | expectedStanleyYards
        'iowa-18/iowa-niu-18.json'  | 48                 | 209                   | 16                | 62               | 8                | 84              | 12               | 40              | 3                     | 3
        'iowa-18/iowa-isu-18.json'  | 36                 | 105                   | 0                 | 0                | 21               | 68              | 11               | 25              | 2                     | 7
        'iowa-18/iowa-uni-18.json'  | 50                 | 207                   | 0                 | 0                | 14               | 82              | 15               | 72              | 6                     | 0
        'iowa-18/iowa-wisc-18.json' | 31                 | 148                   | 14                | 72               | 6                | 34              | 6                | 14              | 2                     | 2
    }

    @Unroll
    def "validate '18 Iowa passes from stats"() {
        when:
        List<Drive> result = scraper.parseGameByDrives(objectMapper.readValue(readResourceText(gameUrl), Map))
        Game gameResult = ScoreTextParserLib.createGameFromDrives(result)

        then:
        //find rushing total for Iowa
        List<Drive> iowaDrives = gameResult.drives.collect { Drive drive ->
            if(drive.teamId == 71) { return drive}
        }.findAll()

        List<Play> iowaPasses = []

        for(Drive drive: iowaDrives) {
            for(Play play: drive.plays) {
                if(play.playType == PlayType.PASS) {
                    iowaPasses.add(play)
                }
            }
        }
        //passes
        iowaPasses.size() == expectedIowaPasses
        int passYards = 0
        for(Play play: iowaPasses) {
            passYards += play.pass.yards
        }
        passYards == expectedIowaPassYards

        where:
        gameUrl                     | expectedIowaPasses | expectedIowaPassYards
        'iowa-18/iowa-niu-18.json'  | 25                 | 143
        'iowa-18/iowa-isu-18.json'  | 28                 | 166
        'iowa-18/iowa-uni-18.json'  | 31                 | 338
        'iowa-18/iowa-wisc-18.json' | 23                 | 256
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
