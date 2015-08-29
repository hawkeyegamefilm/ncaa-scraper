import com.footballscience.database.ParserDAO
import com.footballscience.domain.Rush
import com.footballscience.parser.PlayType
import com.footballscience.parser.ScoreTextParserLib
import spock.lang.Specification
import spock.lang.Unroll

class ScoreTextParserLibSpec extends Specification {
    ParserDAO parserDAO

    def setup() {
        parserDAO = new ParserDAO()
    }

    def "correctly defines playtype"() {
        when:
        PlayType playType = ScoreTextParserLib.determinePlayType(scoreText)

        then:
        playType == expectedPlayType

        where:
        scoreText                                                                                                      | expectedPlayType
        "17-S.Kollmorgen complete to 7-D.Johnson. 7-D.Johnson to IOW 15 for 60 yards (37-J.Lowdermilk)."               | PlayType.PASS
        "Penalty on UNI 17-S.Kollmorgen, Delay of game, 5 yards, enforced at IOW 15. No Play."                         | PlayType.PENALTY
        "7-D.Johnson to IOW 13 for 7 yards (37-J.Lowdermilk,27-J.Lomax)."                                              | PlayType.RUSH
        "17-S.Kollmorgen sacked at IOW 20 for -6 yards (95-D.Ott)."                                                    | PlayType.RUSH
        "40-M.Schmadeke 37 yards Field Goal is Good."                                                                  | PlayType.FIELD_GOAL
        "40-M.Schmadeke kicks 65 yards from UNI 30. 10-J.Parker to IOW 27 for 22 yards (40-M.Schmadeke,59-B.Willson)." | PlayType.KICKOFF
        "15-J.Rudock incomplete. Intended for 5-D.Bullock."                                                            | PlayType.PASS
        "1-M.Koehn extra point is good."                                                                               | PlayType.ATTEMPT
        "90-L.Bieghler punts 47 yards from UNI 33. 89-M.Vandeberg to IOW 43 for 23 yards (88-A.Reth,94-I.Ales)."       | PlayType.PUNT
    }

    @Unroll
    def "playerFound functions correctly"() {
        setup:
        List roster = parserDAO.getRosterBySeasonTeamId("71")
        when:
        Boolean result = ScoreTextParserLib.playerFound(roster, number, firstInit, lastName)

        then:
        result == expected

        where:
        number | firstInit | lastName  | expected
        10     | "J"       | "Parker"  | true
        10     | "J"       | "Someone" | false
        15     | "J"       | "Rudock"  | true
        12     | "J"       | "Rudock"  | false
    }

    @Unroll
    def "fumbleLost functions correctly"() {
        when:
        List roster = parserDAO.getRosterBySeasonTeamId(teamId)
        Map rosters = [("${teamId}".toString()): roster]
        Integer integer = ScoreTextParserLib.calculateFumbleLost(scoreText, teamId.toInteger(), rosters)

        then:
        integer == expected

        cleanup:
        roster.clear()

        where:
        teamId | scoreText                                                                                                                                                                                                                   | expected
        "71"   | "10-J.Parker to IOW 35, FUMBLES (97-B.Dueitt). 97-B.Dueitt runs 35 yards for a touchdown."                                                                                                                                  | 1
        "71"   | "1-S.Secor kicks 59 yards from BALL 35. 10-J.Parker to IOW 23, FUMBLES. 37-A.Taylor to IOW 23 for no gain."                                                                                                                 | 1
        "2476" | "20-D.Nealy to IOW 47, FUMBLES (27-J.Lomax). to IOW 50 for no gain."                                                                                                                                                        | 0
        "71"   | "15-J.Rudock complete to 4-T.Smith. 4-T.Smith to IOW 41, FUMBLES. to IOW 41 for no gain."                                                                                                                                   | 0
        "71"   | "1-C.Netten kicks 43 yards from ISU 35. 80-H.Krieger Coble 10-J.Parker 46-G.Kittle 80-H.Krieger Coble 45-M.Weisman 89-M.Vandeberg to IOW 18, FUMBLES. 1-C.Netten recovers at the IOW 18. 1-C.Netten to IOW 18 for no gain." | 1
        "759"  | "2-C.Covington to IU 25, FUMBLES. 2-C.Covington to IU 25 for no gain (41-B.Bower,34-N.Meier)."                                                                                                                              | 0
    }

    def "createRushRow functions correctly"() {
        setup:
        List roster = parserDAO.getRosterBySeasonTeamId(teamId.toString())
        Map rosters = [("${teamId}".toString()): roster]

        when:
        Rush rush = ScoreTextParserLib.createRushRow("somegameid", teamId, 1, ytg, scoreText, rosters)

        then:
        rush.playerId == expectedPlayerId
        rush.firstDown == expectedFirstDown

        where:
        scoreText                                                       | teamId | ytg | expectedPlayerId | expectedFirstDown
        "5-D.Miller to UNI 45 for 1 yard (90-L.Trinca-Pasat)."          | 920    | 10  | 47226            | 0
        "33-J.Canzeri to UNI 28 for 2 yards (44-M.O'Brien)."            | 71     | 10  | 41521            | 0
        "5-D.Bullock to UNI 26 for 2 yards (44-M.O'Brien,46-J.Farley)." | 71     | 8   | 41520            | 0


    }
}
