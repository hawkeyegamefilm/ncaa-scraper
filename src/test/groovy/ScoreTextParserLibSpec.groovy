import com.footballscience.database.ParserDAO
import com.footballscience.domain.DriveType
import com.footballscience.domain.Kickoff
import com.footballscience.domain.Pass
import com.footballscience.domain.Play
import com.footballscience.domain.Punt
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

//    def "correctly defines playtype"() {
//        when:
//        PlayType playType = ScoreTextParserLib.determinePlayType("someid", 1, 2, 1, 10, scoreText, [:], false)
//
//        then:
//        playType == expectedPlayType
//
//        where:
//        scoreText                                                                                                      | expectedPlayType
//        "17-S.Kollmorgen complete to 7-D.Johnson. 7-D.Johnson to IOW 15 for 60 yards (37-J.Lowdermilk)."               | PlayType.PASS
//        "Penalty on UNI 17-S.Kollmorgen, Delay of game, 5 yards, enforced at IOW 15. No Play."                         | PlayType.PENALTY
//        "7-D.Johnson to IOW 13 for 7 yards (37-J.Lowdermilk,27-J.Lomax)."                                              | PlayType.RUSH
//        "17-S.Kollmorgen sacked at IOW 20 for -6 yards (95-D.Ott)."                                                    | PlayType.RUSH
//        "40-M.Schmadeke 37 yards Field Goal is Good."                                                                  | PlayType.FIELD_GOAL
//        "40-M.Schmadeke kicks 65 yards from UNI 30. 10-J.Parker to IOW 27 for 22 yards (40-M.Schmadeke,59-B.Willson)." | PlayType.KICKOFF
//        "15-J.Rudock incomplete. Intended for 5-D.Bullock."                                                            | PlayType.PASS
//        "1-M.Koehn extra point is good."                                                                               | PlayType.ATTEMPT
//        "90-L.Bieghler punts 47 yards from UNI 33. 89-M.Vandeberg to IOW 43 for 23 yards (88-A.Reth,94-I.Ales)."       | PlayType.PUNT
//    }

    @Unroll
    def "playerFound functions correctly"() {
        setup:
        List roster = loadRoster("cfbstats_14", 71 as String)

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
        List roster = loadRoster("cfbstats_14", teamId.toString())
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

    @Unroll
    def "basics for createRushRow function correctly"() {
        setup:
        List roster = loadRoster("cfbstats_14", teamId.toString())
        Map rosters = [("${teamId}".toString()): roster]

        when:
        Rush rush = ScoreTextParserLib.createRushRow("somegameid", teamId, 1, ytg, scoreText, rosters, 0, [:], "1")

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

    @Unroll
    def "one off fumble scenarios for createRushRow function correctly"() {
        setup:
        List roster = loadRoster(schema, teamId.toString())
        Map rosters = [("${teamId}".toString()): roster, "${homeTeamId}": [:]]


        when:
        Rush rush = ScoreTextParserLib.createRushRow("somegameid", teamId, 1, ytg, scoreText, rosters, 63, [:], homeTeamId)

        then:
        rush.playerId == expectedPlayerId
        rush.yards == expectedYards
        rush.fumble == 1
        rush.fumbleLost == expectedFumbleLost

        where:
        scoreText                                                                                    | teamId | ytg | expectedPlayerId | expectedYards | expectedFumbleLost | schema   | homeTeamId
//        "10-J.Parker to IOW 35, FUMBLES (97-B.Dueitt). 97-B.Dueitt runs 35 yards for a touchdown."   | 71     | 10  | 41579            | 35            | 1                  | "cfbstats_14" | "71"
//        "14-B.Wallace scrambles to AUB 1, FUMBLES (24-D.Moncrief). 17-K.Frost to AUB 1 for no gain." | 1851   | 1   | 44690            | 0             | 1                  | "cfbstats_14" | "827"
        "21-I.Kelly-Martin to PSU 33, FUMBLES (6-C.Brown). 74-T.Wirfs to PSU 33 for no gain."        | 71     | 1   | 9181            | 4             | 0                  | "cfb_stats_18" | "2256"

    }

    def "kneel down rushes"() {
        setup:
        List roster = parserDAO.getRosterBySeasonTeamId(teamId.toString())
        Map rosters = [("${teamId}".toString()): roster]

        when:
        Rush rush = ScoreTextParserLib.createRushRow("somegameid", teamId, 1, ytg, scoreText, rosters, 68, [:], "1")

        then:
        rush.playerId == expectedPlayerId
        rush.yards == expectedYards
        rush.kneelDown == kneelDown

        where:
        scoreText                        | teamId | ytg | expectedPlayerId | expectedYards | kneelDown
        "kneels at IOW 29 for -3 yards." | 71     | 10  | null             | -3            | 1
    }

    @Unroll
    def "penalty calculations for rush play"() {
        when:
        Rush rush = ScoreTextParserLib.createRushRow("somegameid", teamId, 1, ytg, scoreText, ["71":[:], "1645": [:]], yfog, [71:"IOW", 1645: "NIL"], "71")

        then:
        rush.playerId == expectedPlayerId
        rush.yards == expectedYards

        where:
        scoreText                                                                                                                                         | teamId | ytg | expectedPlayerId | expectedYards | yfog
        "21-I.Kelly-Martin pushed ob at NIL 46 for 4 yards (8-M.Williams). Penalty on IOW 38-T.Hockenson, Holding, 10 yards, enforced at NIL 46."         | 71     | 10  | 0                | 4 | 50
        "21-I.Kelly-Martin pushed ob at NIL 13 for 45 yards (3-J.Embry). Penalty on IOW 59-R.Reynolds, Illegal low block, 15 yards, enforced at IOW 46." | 71     | 10  | 0                | 4 | 58

    }

    @Unroll
    def "sack scenarios"() {
        setup:
        List roster = parserDAO.getRosterBySeasonTeamId(teamId.toString())
        Map rosters = [("${teamId}".toString()): roster]
        when:
        Rush rush = ScoreTextParserLib.createRushRow("somegameid", teamId, 1, ytg, scoreText, rosters, yfog, [71:"IOW", 1645: "NIL"], homeTeamId)

        then:
        rush.playerId == expectedPlayerId
        rush.yards == expectedYards
        rush.sack == 1
        rush.fumble == fumble
        rush.fumbleLost == fumbleLost

        where:
        scoreText                                                                                                                | teamId | ytg | expectedPlayerId | expectedYards | yfog | fumble | fumbleLost | homeTeamId
        "4-N.Stanley sacked at IOW 49 for -4 yards, FUMBLES (15-S.Smith). 38-T.Hockenson to IOW 49 for no gain."                 | 71     | 10  | 9233             | -4            | 53   | 1      | 0          | "71"
        "15-M.Childers sacked at NIL 24 for -10 yards (40-P.Hesse)."                                                             | 1645   | 4   | 15050            | -10           | 34   | 0      | 0          | "71"
        "15-M.Childers sacked at NIL 9 for -15 yards, FUMBLES (34-K.Welch). 15-M.Childers to NIL 9 for no gain."                 | 1645   | 4   | 15050            | -15           | 24   | 1      | 0          | "71"
        "15-M.Childers sacked at NIL 26 for -12 yards, FUMBLES (94-A.Epenesa). 57-C.Golston to NIL 26 for no gain (65-N.Veloz)." | 1645   | 8   | 15050            | -12           | 24   | 1      | 1          | "71"
    }

    def "sack intentional grounding"() {
        when:
        Rush rush = ScoreTextParserLib.createRushRow("somegameid", teamId, 1, ytg, scoreText, [:], yfog, [71:"IOW", 498: "WIS"], homeTeamId)

        then:
        rush.playerId == expectedPlayerId
        rush.yards == expectedYards
        rush.sack == 1

        where:
        scoreText                                                                                                                                            | teamId | ytg | expectedPlayerId | expectedYards | yfog | homeTeamId
        "12-A.Hornibrook sacked at WIS 44 for -13 yards. Penalty on WIS 12-A.Hornibrook, Intentional grounding, 0 yards, enforced at WIS 44. (98-A.Nelson)." | 498    | 13  | 0                | -13            | 54  | "71"
    }

    def "fumbled rush in own EZ"() {
        when:
        Rush rush = ScoreTextParserLib.createRushRow("somegameid", teamId, 2, ytg, scoreText, [:], yfog, [:], "1")

        then:
        rush.attempt == 1
        rush.yards == 0
        rush.touchdown == 0
        rush.fumble == 1
        rush.fumbleLost == 1

        where:
        scoreText                                                                         | teamId | ytg | yfog
        "3-T.Pigrome to MAR End Zone, FUMBLES. 98-A.Nelson runs no gain for a touchdown." | 1      | 10  | 4
    }

    @Unroll
    def "enforce spot string"() {
        when:
        String actual = ScoreTextParserLib.getEnforcedSpot(scoreText)

        then:
        expected == actual

        where:
        scoreText                                                                                                                                        | expected
        "21-I.Kelly-Martin pushed ob at NIL 46 for 4 yards (8-M.Williams). Penalty on IOW 38-T.Hockenson, Holding, 10 yards, enforced at NIL 46."        | " NIL46"
        "21-I.Kelly-Martin pushed ob at NIL 13 for 45 yards (3-J.Embry). Penalty on IOW 59-R.Reynolds, Illegal low block, 15 yards, enforced at IOW 46." | " IOW46"
    }


    @Unroll
    def "basic completions and incompletions for createPassRow function correctly"() {
        setup:
        List roster = loadRoster("cfbstats_14", teamId.toString())
        Map rosters = [("${teamId}".toString()): roster]
        when:
        Pass pass = ScoreTextParserLib.createPassRow("someid", teamId, 1, ytg, scoreText, rosters, [:], 0, "")

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
        List roster = loadRoster("cfbstats_14", teamId.toString())
        List roster2 = loadRoster("cfbstats_14", teamId.toString())
        Map rosters = [("${teamId}".toString()): roster, (team2Id as String): roster2]
        when:
        Pass pass = ScoreTextParserLib.createPassRow("someid", teamId, 1, ytg, scoreText, rosters, [:], yfog, "71")

        then:
        pass.attempt
        pass.touchdown == touchdown
        pass.interception == intercepted
        pass.yards == yards
        pass.recieverId == receiverId
        pass.fumble == fumble
        pass.fumbleLost == fumbleLost

        where:
        scoreText                                                                                                                                                                                                      | teamId| team2Id | ytg | touchdown | intercepted | yards | receiverId | fumble | fumbleLost| yfog
        "17-S.Kollmorgen incomplete. Intended for 18-K.Vereen, INTERCEPTED by 41-B.Bower at IOW 47. 41-B.Bower runs ob at IOW 49 for 2 yards."                                                                         | 920   | 71 | 9   | 0         | 1           | 0     | 47261      | 0      | 0| 0
        "17-S.Kollmorgen incomplete. INTERCEPTED by 13-G.Mabin at IOW 28. 13-G.Mabin to IOW 46 for 18 yards (17-S.Kollmorgen). Penalty on IOW 6-R.Spearman, Unsportsmanlike conduct, 15 yards, enforced at IOW 46."    | 920   | 71 | 17  | 0         | 1           | 0     | 0          | 0      | 0| 0
        "15-J.Rudock complete to 22-D.Powell. 22-D.Powell runs 12 yards for a touchdown."                                                                                                                              | 71    | 920 | 12  | 1         | 0           | 12    | 41583      | 0      | 0| 0
        "17-S.Kollmorgen complete to 18-K.Vereen. 18-K.Vereen runs 23 yards for a touchdown."                                                                                                                          | 920   | 71 | 3   | 1         | 0           | 23    | 47261      | 0      | 0| 0
        "15-J.Rudock complete to 17-J.Hillyer. 17-J.Hillyer to IOW 47, FUMBLES (49-B.McMakin). 37-M.Busher to IOW 41 for 6 yards. Penalty on UNI 2-M.Dorleant, Unsportsmanlike conduct, 15 yards, enforced at IOW 41." | 71    | 920 | 9   | 0         | 0           | 6     | 41544      | 1      | 1| 41
        "15-J.Rudock complete to 4-T.Smith. 4-T.Smith to IOW 41, FUMBLES. to IOW 41 for no gain."                                                                                                                      | 71    | 920 | 10  | 0         | 0           | 0     | 41590      | 1      | 0| 59
    }

    def "one off pass scenario"() {
        setup:
        List roster = loadRoster("cfb_stats_18", teamId.toString())
        Map rosters = [("${teamId}".toString()): roster]
        when:
        Pass pass = ScoreTextParserLib.createPassRow("someid", teamId, 1, ytg, scoreText, rosters, [:], 0, "")

        then:
        pass.attempt
        pass.completion == 1
        pass.passerId == passerId
        pass.yards == 0

        where:
        scoreText                                                                                  | teamId | ytg | passerId
        "11-K.Hill complete to 24-T. Johnson. 24-T. Johnson to MAR 25 for no gain (49-N.Niemann)." | 2502   | 10  | 11122
    }

    def "fumble on reception, self recovery and advance"() {
        setup:
        List roster = loadRoster("cfb_stats_18", teamId.toString())
        Map rosters = [("${teamId}".toString()): roster, "2296": [:]]

        when:
        Pass pass = ScoreTextParserLib.createPassRow("someid", teamId, 1, ytg, scoreText, rosters, [71:"IOW", 2296: "PSU"], 39, "2296")

        then:
        pass.attempt
        pass.completion == 1
        pass.fumble == 1
        pass.fumbleLost == 0
        pass.yards == yards

        where:
        scoreText                                                                                                                       | teamId | ytg | yards
        "4-N.Stanley complete to 38-T.Hockenson. 38-T.Hockenson to PSU 35, FUMBLES (7-K.Farmer). 38-T.Hockenson to PSU 29 for no gain." | 71     | 15  | 32

    }

    @Unroll
    def "basics for createKickoffRow"() {
        setup:
        List roster = loadRoster("cfbstats_14", kickingTeamId as String)
        List roster2 = loadRoster("cfbstats_14", returningTeamId as String)
        Map rosters = [("${kickingTeamId}".toString()): roster, ("${returningTeamId}".toString()): roster2]

        when:
        Kickoff kickoff = ScoreTextParserLib.createKickoffRow('someid', kickingTeamId, returningTeamId, scoreText, onsideFlag, rosters)

        then:
        kickoff.kickingTeamId == kickingTeamId
        kickoff.kickerId == kickerId
        kickoff.yards == kickYards
        kickoff.returnerId == returnerId
        kickoff.returnYards == returnYards

        where:
        scoreText                                                                                 | kickingTeamId | returningTeamId | kickerId | kickYards | returnerId | returnYards | touchback | onsideFlag
        "40-M.Schmadeke kicks 56 yards from UNI 35. 45-M.Weisman to UNI 41 for 50 yards."         | 920           | 71              | 47249    | 56        | 41606      | 50          | 0         | false
        "1-M.Koehn kicks 57 yards from IOW 35. 5-D.Miller to UNI 28 for 20 yards (44-B.Niemann)." | 71            | 920             | 41560    | 57        | 47226      | 20          | 0         | false
        "40-M.Schmadeke kicks 65 yards from UNI 35 to IOW End Zone. touchback."                   | 920           | 71              | 47249    | 65        | 0          | 0           | 1         | false
        "1-M.Koehn kicks 65 yards from IOW 35 to UNI End Zone. touchback."                        | 71            | 920             | 41560    | 65        | 0          | 0           | 1         | false
    }

    /**
     * parsing onsides seems to be very touchy, very little to latch on to for positively IDing things
     * @return
     */
    @Unroll
    def "penaltys and onside kicks for createKickoffRow"() {
        setup:
        List roster = loadRoster("cfbstats_14", kickingTeamId as String)
        List roster2 = loadRoster("cfbstats_14", returningTeamId as String)
        Map rosters = [("${kickingTeamId}".toString()): roster, ("${returningTeamId}".toString()): roster2]

        when:
        Kickoff kickoff = ScoreTextParserLib.createKickoffRow('someid', kickingTeamId, returningTeamId, scoreText, onsideFlag, rosters)

        then:
        kickoff.kickingTeamId == kickingTeamId
        kickoff.kickerId == kickerId
        kickoff.yards == kickYards
        kickoff.returnerId == returnerId
        kickoff.returnYards == returnYards
        kickoff.onside == onside
        kickoff.onsideSuccess == onsideRecovery

        where:
        scoreText                                                                                                                                                          | kickingTeamId | returningTeamId | kickerId | kickYards | returnerId | returnYards | touchback | onside | onsideRecovery | onsideFlag
        "40-M.Schmadeke kicks 57 yards from UNI 35. 89-M.Vandeberg to IOW 31 for 23 yards (15-T.Omli). Penalty on IOW 36-C.Fisher, Holding, 10 yards, enforced at IOW 31." | 920           | 71              | 47249    | 57        | 41599      | 23          | 0         | 0      | 0              | false
        "1-M.Koehn kicks 7 yards from IOW 35. 88-P.Gallo to IOW 42 for no gain."                                                                                           | 71            | 2502            | 41560    | 7         | 43411      | 0           | 0         | 0      | 0              | false
//        "1-M.Koehn kicks 15 yards from IOW 35. to MAR 50 for no gain."                                                                                                     | 71            | 2502            | 41560    | 15        | 0          | 0           | 0         | 1      | 1              | true
    }

    @Unroll
    def "oob kicks"() {
        setup:
        List roster = loadRoster("cfbstats_14", kickingTeamId as String)
        List roster2 = loadRoster("cfbstats_14", returningTeamId as String)
        Map rosters = [("${kickingTeamId}".toString()): roster, ("${returningTeamId}".toString()): roster2]

        when:
        Kickoff kickoff = ScoreTextParserLib.createKickoffRow('someid', kickingTeamId, returningTeamId, scoreText, onsideFlag, rosters)

        then:
        kickoff.kickingTeamId == kickingTeamId
        kickoff.kickerId == kickerId
        kickoff.yards == kickYards
        kickoff.returnerId == returnerId
        kickoff.returnYards == returnYards
        kickoff.oob == oob

        where:
        scoreText                                                               | kickingTeamId | returningTeamId | kickerId | kickYards | returnerId | returnYards | oob | onsideFlag
        "15-B.Craddock kicks 63 yards from MAR 35, out of bounds at the IOW 2." | 2502          | 71              | 43395    | 63        | 0          | 0           | 1   | false
    }

    def "missing kicker on kickoff"() {
        when:
        Kickoff kickoff = ScoreTextParserLib.createKickoffRow('someid', kickingTeamId, returningTeamId, scoreText, true, [:])

        then:
        kickoff.kickingTeamId == kickingTeamId
        kickoff.kickerId == kickerId
        kickoff.yards == kickYards
        kickoff.returnerId == returnerId
        kickoff.returnYards == returnYards

        where:
        scoreText                                                                                | kickingTeamId | returningTeamId | kickerId | kickYards | returnerId | returnYards
        "kicks 10 yards from MIN 35 to the MIN 45, downed by 27-A.Hooker to MIN 45 for no gain." | 2515          | 71              | -1    | 10        | 0          | 0
    }

    @Unroll
    def "basic scenarios for createPuntRow"() {
        setup:
        List roster = loadRoster("cfbstats_14", puntingTeamId as String)
        List roster2 = loadRoster("cfbstats_14", returningTeamId as String)
        Map rosters = [("${puntingTeamId}".toString()): roster, ("${returningTeamId}".toString()): roster2]

        when:
        Punt punt = ScoreTextParserLib.createPuntRow('someid', puntingTeamId, returningTeamId, playNum, scoreText, rosters)

        then:
        punt.punterId == expectedPunterId
        punt.puntYards == expectedYards
        punt.returnerId == expectedReturnerId
        punt.returnYards == expectedReturnYards

        where:
        scoreText                                                                                         | puntingTeamId | returningTeamId | playNum | expectedPunterId | expectedYards | expectedReturnerId | expectedReturnYards
        "90-L.Bieghler punts 45 yards from UNI 37. 89-M.Vandeberg to IOW 17 for -1 yard (39-B.Williams)." | 920           | 71              | 4       | 47172            | 45            | 41599              | -1
        "16-D.Kidd punts 42 yards from IOW 23. 19-C.Owens to UNI 47 for 12 yards (27-J.Lomax)."           | 71            | 920             | 7       | 41555            | 42            | 47233              | 12
    }

    def "no gain scenario on punt"() {
        setup:
        List roster = loadRoster("cfb_stats_18", puntingTeamId as String)
        List roster2 = loadRoster("cfb_stats_18", returningTeamId as String)
        Map rosters = [("${puntingTeamId}".toString()): roster, ("${returningTeamId}".toString()): roster2]

        when:
        Punt punt = ScoreTextParserLib.createPuntRow('someid', puntingTeamId, returningTeamId, playNum, scoreText, rosters)

        then:
        punt.punterId == expectedPunterId
        punt.puntYards == expectedYards
        punt.returnerId == expectedReturnerId
        punt.returnYards == expectedReturnYards

        where:
        scoreText                                                                                     | puntingTeamId | returningTeamId | playNum | expectedPunterId | expectedYards | expectedReturnerId | expectedReturnYards
        "7-C.Rastetter punts 43 yards from IOW 26. 5-J.Harris to IU 31 for no gain (14-K.Groeneweg)." | 71            | 759             | 7       | 9212             | 43            | null               | 0
    }

    @Unroll
    def "touchbacks, downed & faircatches for createPuntRow"() {
        setup:
        List roster = loadRoster("cfbstats_14", puntingTeamId as String)
        List roster2 = loadRoster("cfbstats_14", returningTeamId as String)
        Map rosters = [("${puntingTeamId}".toString()): roster, ("${returningTeamId}".toString()): roster2]

        when:
        Punt punt = ScoreTextParserLib.createPuntRow('someid', puntingTeamId, returningTeamId, playNum, scoreText, rosters)

        then:
        punt.punterId == expectedPunterId
        punt.puntYards == expectedYards
        punt.returnerId == expectedReturnerId
        punt.returnYards == expectedReturnYards
        punt.fairCatch == fairCatch
        punt.touchBack == touchBack

        where:
        scoreText                                                                          | puntingTeamId | returningTeamId | playNum | expectedPunterId | expectedYards | expectedReturnerId | expectedReturnYards | fairCatch | touchBack | downed
        "90-L.Bieghler punts 35 yards from IOW 44 to IOW 9, fair catch by 89-M.Vandeberg." | 920           | 71              | 2       | 47172            | 35            | 41599              | 0                   | 1         | 0         | 0
        "16-D.Kidd punts 34 yards from IOW 14 to IOW 48, fair catch by 1-D.Hall."          | 71            | 920             | 4       | 41555            | 34            | 47200              | 0                   | 1         | 0         | 0
        "18-N.Renfro punts 47 yards from IOW 47 to IOW End Zone. touchback."               | 2502          | 71              | 3       | 43452            | 47            | 0                  | 0                   | 0         | 1         | 0
        "98-C.Kornbrath punts 28 yards from MAR 37 to the MAR 9, downed by 39-T.Perry."    | 71            | 2502            | 3       | 41561            | 28            | 0                  | 0                   | 0         | 0         | 1
    }

    def "out of bounds createPuntRow"() {
        setup:
        List roster = loadRoster("cfbstats_14", puntingTeamId as String)
        List roster2 = loadRoster("cfbstats_14", returningTeamId as String)
        Map rosters = [("${puntingTeamId}".toString()): roster, ("${returningTeamId}".toString()): roster2]

        when:
        Punt punt = ScoreTextParserLib.createPuntRow('someid', puntingTeamId, returningTeamId, 1, scoreText, rosters)

        then:
        punt.punterId == expectedPunterId
        punt.puntYards == expectedYards
        punt.returnerId == expectedReturnerId
        punt.returnYards == expectedReturnYards
        punt.oob == oob

        where:
        scoreText                                                                 | puntingTeamId | returningTeamId | expectedPunterId | expectedYards | expectedReturnerId | expectedReturnYards | oob
        "12-K.Schmidt punts 35 yards from BALL 11, out of bounds at the BALL 46." | 1558          | 71              | 34419            | 35            | 0                  | 0                   | 1
    }

    def "blocked punt"() {
        setup:
        List roster = parserDAO.getRosterBySeasonTeamId(puntingTeamId.toString())
        List roster2 = parserDAO.getRosterBySeasonTeamId(returningTeamId.toString())
        Map rosters = [("${puntingTeamId}".toString()): roster, ("${returningTeamId}".toString()): roster2]

        when:
        Punt punt = ScoreTextParserLib.createPuntRow('someid', puntingTeamId, returningTeamId, 1, scoreText, rosters)

        then:
        punt.punterId == expectedPunterId
        punt.puntYards == expectedYards
        punt.returnerId == expectedReturnerId
        punt.returnYards == expectedReturnYards
        punt.oob == oob
        punt.blocked == 1

        where:
        scoreText                                                                 | puntingTeamId | returningTeamId | expectedPunterId | expectedYards | expectedReturnerId | expectedReturnYards | oob
        "punts 0 yards from IOW 33 blocked by 9-J.Wesley. to IOW 23 for no gain." | 71            | 1645            | null             | 0          | 0               | 0                | 0
    }

    def "fumble during punt"() {
        setup:
        List roster = parserDAO.getRosterBySeasonTeamId(puntingTeamId.toString())
        List roster2 = parserDAO.getRosterBySeasonTeamId(returningTeamId.toString())
        Map rosters = [("${puntingTeamId}".toString()): roster, ("${returningTeamId}".toString()): roster2]

        when:
        Punt punt = ScoreTextParserLib.createPuntRow("someid", 1, 2, 1, scoreText, rosters)

        then:
        punt.puntYards == expectedYards
        punt.fumble == expectedFumble
        punt.fumbleLost == expectedFumbleLost


        where:
        scoreText                                                                                                                  | puntingTeamId | returningTeamId  | expectedYards | expectedFumble | expectedFumbleLost
        "15-A.Lotti punts 44 yards from WIS 24. 14-K.Groeneweg to WIS 45, FUMBLES (14-D.Dixon). 14-D.Dixon to WIS 45 for no gain." | 498           | 71               | 44            | 1              | 1
    }


    def "parse tackler block"() {
        setup:
        Integer defenseTeamId = 920
        Integer offenseTeamId = 71
        List roster = loadRoster("cfbstats_14", offenseTeamId as String)
        List roster2 = loadRoster("cfbstats_14", defenseTeamId as String)
        Map rosters = [("${offenseTeamId}".toString()): roster, ("${defenseTeamId}".toString()): roster2]

        when:
        List result = ScoreTextParserLib.parseTacklerBlock(scoreText, rosters, defenseTeamId)

        then:
        result == expected

        where:
        scoreText                                                       | expected
        "5-D.Bullock to UNI 26 for 2 yards (44-M.O'Brien,46-J.Farley)." | [47231, 47193]
        "5-D.Bullock to UNI 26 for 2 yards (44-M.O'Brien)."             | [47231]
    }

    @Unroll
    def "parse time to integer from mm:ss format"() {
        when:
        Integer result = ScoreTextParserLib.convertTimeStringToSeconds(timeString)

        then:
        result == expected

        where:
        timeString | expected
        null       | 0
        ""         | 0
        "15:00"    | 900
        "13:00"    | 780
        "13:59"    | 839
        "00:02"    | 2
        "00:59"    | 59
        ":51"      | 51
    }

    @Unroll
    def "determineDriveEndType from lastPlay"() {
        when:
        DriveType result = ScoreTextParserLib.determineEndType(lastPlay)

        then:
        result == expected

        where:
        lastPlay                                                                                                                                             | expected
        null                                                                                                                                                 | null
        new Play()                                                                                                                                           | DriveType.DOWNS
        new Play(playType: PlayType.PUNT, fullScoreText: "90-L.Bieghler punts 45 yards from UNI 37. 89-M.Vandeberg to IOW 17 for -1 yard (39-B.Williams).")  | DriveType.PUNT
        new Play(playType: PlayType.PUNT, fullScoreText: "95-N.Pritchard punts 13 yards from MAR 6 blocked by 31-A.Mends. 14-D.King to MAR 19 for no gain.") | DriveType.PUNT
        new Play(playType: PlayType.PASS, fullScoreText: "11-P.Hills incomplete. INTERCEPTED by 27-J.Lomax at IOW 3. 27-J.Lomax to IOW 3 for no gain.")      | DriveType.INTERCEPTION
        new Play(playType: PlayType.RUSH, fullScoreText: "45-B.Ross to IOW 35, FUMBLES (13-G.Mabin). 19-M.Taylor to IOW 38 for no gain.")                    | DriveType.FUMBLE
        new Play(playType: PlayType.PASS, fullScoreText: "15-J.Rudock incomplete. Intended for 5-D.Bullock.")                                                | DriveType.DOWNS
        new Play(playType: PlayType.FIELD_GOAL, fullScoreText: "40-M.Schmadeke 37 yards Field Goal is Good.")                                                | DriveType.FIELD_GOAL
        new Play(playType: PlayType.FIELD_GOAL, fullScoreText: "40-M.Schmadeke 37 yards Field Goal is no Good.")                                                | DriveType.MISSED_FG
    }

    def "test schema lookup"() {
        when:
        List roster = loadRoster("cfbstats_14", "71")

        then:
        roster != null
    }

    List loadRoster(String schema, String teamId) {
        parserDAO.getRosterBySeasonTeamIdAndSchema(schema,teamId)
    }

    @Unroll
    def "caclulate Spot"() {
        when:
        Integer spot = ScoreTextParserLib.calculateSpot([71:"IOW", 920: "UNI"], driveText, teamId)

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
}
