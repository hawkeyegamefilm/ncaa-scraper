import com.footballscience.database.ParserDAO
import com.footballscience.domain.Kickoff
import com.footballscience.domain.Pass
import com.footballscience.domain.Rush
import com.footballscience.parser.PlayType
import com.footballscience.parser.ScoreTextParserLib
import spock.lang.Ignore
import spock.lang.Specification
import spock.lang.Unroll

class ScoreTextParserLibSpec extends Specification {
    ParserDAO parserDAO

    def setup() {
        parserDAO = new ParserDAO()
    }

    def "correctly defines playtype"() {
        when:
        PlayType playType = ScoreTextParserLib.determinePlayType("someid", 1, 1, 10, scoreText, [:])

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
        Integer actual = ScoreTextParserLib.calculateFumbleLost(scoreText, teamId.toInteger(), rosters)

        then:
        actual == expected

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

    def "basics for createRushRow function correctly"() {
        setup:
        List roster = parserDAO.getRosterBySeasonTeamId(teamId.toString())
        Map rosters = [("${teamId}".toString()): roster]

        when:
        Rush rush = ScoreTextParserLib.createRushRow("somegameid", teamId, 1, ytg, scoreText, rosters)

        then:
        rush.playerId == expectedPlayerId
        rush.firstDown == expectedFirstDown
        rush.touchdown == expectedTD
        rush.yards == expectedYards

        where:
        scoreText                                                           | teamId | ytg | expectedPlayerId | expectedYards | expectedFirstDown | expectedTD
        "5-D.Miller to UNI 45 for 1 yard (90-L.Trinca-Pasat)."              | 920    | 10  | 47226            | 1             | 0                 | 0
        "33-J.Canzeri to UNI 28 for 2 yards (44-M.O'Brien)."                | 71     | 10  | 41521            | 2             | 0                 | 0
        "5-D.Bullock to UNI 26 for 2 yards (44-M.O'Brien,46-J.Farley)."     | 71     | 8   | 41520            | 2             | 0                 | 0
        "29-L.Daniels Jr. runs 13 yards for a touchdown."                   | 71     | 10  | 41524            | 13            | 1                 | 1
        "7-D.Johnson to UNI 34 for 6 yards (27-J.Lomax,90-L.Trinca-Pasat)." | 920    | 10  | 47209            | 6             | 0                 | 0
        "44-C.Artis-Payne to AUB 11 for no gain."                           | 827    | 10  | 34128            | 0             | 0                 | 0
    }

    def "one off fumble scenarios for createRushRow function correctly"() {
        setup:
        List roster = parserDAO.getRosterBySeasonTeamId(teamId.toString())
        Map rosters = [("${teamId}".toString()): roster]

        when:
        Rush rush = ScoreTextParserLib.createRushRow("somegameid", teamId, 1, ytg, scoreText, rosters)

        then:
        rush.playerId == expectedPlayerId
        rush.yards == expectedYards
        rush.fumble == 1
        rush.fumbleLost == expectedFumbleLost

        where:
        scoreText                                                                                    | teamId | ytg | expectedPlayerId | expectedYards | expectedFumbleLost
        "10-J.Parker to IOW 35, FUMBLES (97-B.Dueitt). 97-B.Dueitt runs 35 yards for a touchdown."   | 71     | 10  | 41579            | 35            | 1
        "14-B.Wallace scrambles to AUB 1, FUMBLES (24-D.Moncrief). 17-K.Frost to AUB 1 for no gain." | 1851   | 1   | 44690            | 0             | 1

    }

    @Unroll
    def "basic completions and incompletions for createPassRow function correctly"() {
        setup:
        List roster = parserDAO.getRosterBySeasonTeamId(teamId.toString())
        Map rosters = [("${teamId}".toString()): roster]
        when:
        Pass pass = ScoreTextParserLib.createPassRow("someid", teamId, 1, ytg, scoreText, rosters)

        then:
        pass.attempt
        pass.yards == yards
        pass.firstDown == firstDown
        pass.passerId == passerId
        pass.recieverId == receiverId

        where:
        scoreText                                                                               | teamId | ytg | yards | firstDown | passerId | receiverId
        "15-J.Rudock complete to 11-K.Martin-Manley. 11-K.Martin-Manley to UNI 45 for 6 yards." | 71     | 10  | 6     | 0         | 41586    | 41569
        "15-J.Rudock complete to 4-T.Smith. 4-T.Smith to UNI 18 for 8 yards (37-M.Busher)."     | 71     | 6   | 8     | 1         | 41586    | 41590
        "15-J.Rudock incomplete. Intended for 82-R.Hamilton."                                   | 71     | 10  | 0     | 0         | 41586    | 41541
    }

    @Unroll
    def "special cases for createPassRow function correctly"() {
        setup:
        List roster = parserDAO.getRosterBySeasonTeamId(teamId.toString())
        Map rosters = [("${teamId}".toString()): roster]
        when:
        Pass pass = ScoreTextParserLib.createPassRow("someid", teamId, 1, ytg, scoreText, rosters)

        then:
        pass.attempt
        pass.touchdown == touchdown
        pass.interception == intercepted
        pass.yards == yards
        pass.recieverId == receiverId
        pass.fumble == fumble
        pass.fumbleLost == fumbleLost

        where:
        scoreText                                                                                                                                                                                                      | teamId | ytg | touchdown | intercepted | yards | receiverId | fumble | fumbleLost
        "17-S.Kollmorgen incomplete. Intended for 18-K.Vereen, INTERCEPTED by 41-B.Bower at IOW 47. 41-B.Bower runs ob at IOW 49 for 2 yards."                                                                         | 920    | 9   | 0         | 1           | 0     | 47261      | 0      | 0
        "15-J.Rudock complete to 22-D.Powell. 22-D.Powell runs 12 yards for a touchdown."                                                                                                                              | 71     | 12  | 1         | 0           | 12    | 41583      | 0      | 0
        "17-S.Kollmorgen complete to 18-K.Vereen. 18-K.Vereen runs 23 yards for a touchdown."                                                                                                                          | 920    | 3   | 1         | 0           | 23    | 47261      | 0      | 0
        "15-J.Rudock complete to 17-J.Hillyer. 17-J.Hillyer to IOW 47, FUMBLES (49-B.McMakin). 37-M.Busher to IOW 41 for 6 yards. Penalty on UNI 2-M.Dorleant, Unsportsmanlike conduct, 15 yards, enforced at IOW 41." | 71     | 9   | 0         | 0           | 6     | 41544      | 1      | 1
        "15-J.Rudock complete to 4-T.Smith. 4-T.Smith to IOW 41, FUMBLES. to IOW 41 for no gain."                                                                                                                      | 71     | 10  | 0         | 0           | 0     | 41590      | 1      | 0
    }

    @Unroll
    def "basic scenarios for createKickoffRow"() {
        setup:
        List roster = parserDAO.getRosterBySeasonTeamId(kickingTeamId.toString())
        List roster2 = parserDAO.getRosterBySeasonTeamId(returningTeamId.toString())
        Map rosters = [("${kickingTeamId}".toString()): roster, ("${returningTeamId}".toString()): roster2]

        when:
        Kickoff kickoff = ScoreTextParserLib.createKickoffRow('someid', kickingTeamId, returningTeamId, scoreText, rosters)

        then:
        kickoff.kickingTeamId == kickingTeamId
        kickoff.kickerId == kickerId
        kickoff.yards == kickYards
        kickoff.returnerId == returnerId
        kickoff.returnYards == returnYards

        where:
        scoreText                                                                                 | kickingTeamId | returningTeamId | kickerId | kickYards | returnerId | returnYards | touchback
        "40-M.Schmadeke kicks 56 yards from UNI 35. 45-M.Weisman to UNI 41 for 50 yards."         | 920           | 71              | 47249    | 56        | 41606      | 50          | 0
        "1-M.Koehn kicks 57 yards from IOW 35. 5-D.Miller to UNI 28 for 20 yards (44-B.Niemann)." | 71            | 920             | 41560    | 57        | 47226      | 20          | 0
        "40-M.Schmadeke kicks 65 yards from UNI 35 to IOW End Zone. touchback."                   | 920           | 71              | 47249    | 65        | 0          | 0           | 1
        "1-M.Koehn kicks 65 yards from IOW 35 to UNI End Zone. touchback."                        | 71            | 920             | 41560    | 65        | 0          | 0           | 1
    }

    @Unroll
    def "one off scenarios for createKickoffRow"() {
        setup:
        List roster = parserDAO.getRosterBySeasonTeamId(kickingTeamId.toString())
        List roster2 = parserDAO.getRosterBySeasonTeamId(returningTeamId.toString())
        Map rosters = [("${kickingTeamId}".toString()): roster, ("${returningTeamId}".toString()): roster2]

        when:
        Kickoff kickoff = ScoreTextParserLib.createKickoffRow('someid', kickingTeamId, returningTeamId, scoreText, rosters)

        then:
        kickoff.kickingTeamId == kickingTeamId
        kickoff.kickerId == kickerId
        kickoff.yards == kickYards
        kickoff.returnerId == returnerId
        kickoff.returnYards == returnYards

        where:
        scoreText                                                                                                                                                          | kickingTeamId | returningTeamId | kickerId | kickYards | returnerId | returnYards | touchback
        "40-M.Schmadeke kicks 57 yards from UNI 35. 89-M.Vandeberg to IOW 31 for 23 yards (15-T.Omli). Penalty on IOW 36-C.Fisher, Holding, 10 yards, enforced at IOW 31." | 920           | 71              | 47249    | 57        | 41599      | 23          | 0
    }


}
