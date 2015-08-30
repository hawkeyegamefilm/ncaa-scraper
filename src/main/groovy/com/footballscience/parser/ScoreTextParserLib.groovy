package com.footballscience.parser

import com.footballscience.domain.Pass
import com.footballscience.domain.Rush

class ScoreTextParserLib {

    /*
    Need to deal with the multiple teamid BS, passed in ID will need to be global one to write rows
     */
    static PlayType determinePlayType(String gameId, Integer teamId, Integer playNum, Integer ytg, String scoreText, Map rosters) {
        if(isPass(scoreText)) {
            Pass pass = createPassRow(gameId, teamId, playNum, ytg, scoreText, rosters)
            PlayType.PASS
        }
        else if(scoreText.contains("punts")) {
            PlayType.PUNT
        }
        else if(scoreText.contains("kicks")) {
            PlayType.KICKOFF
        }
        else if(scoreText.contains("penalty") || scoreText.contains("Penalty") ) {
            PlayType.PENALTY
        }
        else if(scoreText.contains("Field Goal")) {
            PlayType.FIELD_GOAL
        }
        else if(scoreText.contains("extra point")) {
            PlayType.ATTEMPT
        }
        else if(scoreText.contains("Conversion") ||scoreText.contains("conversion") ) {
            //parse this like a normal play for 2 points
            if(isPass(scoreText)) {
                Pass pass = createPassRow(gameId, teamId, playNum, ytg, scoreText, rosters)
            } else {
                Rush rush = createRushRow(gameId, teamId, playNum, ytg, scoreText, rosters)
            }
            PlayType.ATTEMPT
            //persist rush or pass row on conversation attempt
        }
        else if(scoreText.contains("Timeout") || scoreText.contains("timeout") || scoreText.contains("TIMEOUT")) {
            //currently don't see this in the ncaa site data
            PlayType.TIMEOUT
        } else {
            //must be a rush attempt
            createRushRow(gameId, teamId, playNum, ytg, scoreText, rosters)
            PlayType.RUSH
        }
    }

    static boolean isPass(String scoreText) {
        return scoreText.contains("incomplete") || scoreText.contains("complete")
    }

    static Pass createPassRow(String gameId, Integer teamId, Integer playNum, Integer ytg, String scoreText, Map rosters) {
        return new Pass()
    }

    static Rush createRushRow(String gameId, Integer teamId, Integer playNum, Integer ytg, String scoreText, Map rosters) {
        String jerseyNumber = scoreText.substring(0, scoreText.indexOf("-"))
        String firstInitial = scoreText.substring(scoreText.indexOf("-")+1,scoreText.indexOf("."))
        String lastName = scoreText.substring(scoreText.indexOf(".")+1, scoreText.indexOf(" "))
        //look up player id
        String playerId = rosters.get(teamId.toString()).find {
            it.uniform_number.contains(jerseyNumber) && it.lastname == lastName && it.firstname.substring(0,1) == firstInitial
        }?.player_id

        Integer touchdown = 0
        if(scoreText.contains("Touchdown") || scoreText.contains("touchdown")) {
            touchdown = 1
        }

        Integer fumble = 0
        Integer fumbleLost = 0
        if(scoreText.contains("FUMBLES")) {
            fumble = 1
            fumbleLost = calculateFumbleLost(scoreText, teamId, rosters)
        }

        String yards
        if(touchdown) {
            yards = scoreText.substring(scoreText.indexOf("runs")+4,scoreText.indexOf("yard")).trim()
        } else {
            //handle no gain cases
            if (scoreText.contains("no gain")) {
                yards = 0
            } else {
                yards = scoreText.substring(scoreText.indexOf("for")+4,scoreText.indexOf("yard"))?.trim()
            }
        }

        Integer firstDown = 0
        if(Integer.parseInt(yards) >= ytg) {
            firstDown = 1
        }

        Integer sack = 0
        if(scoreText.contains("sack") || scoreText.contains("Sack")) {
            sack = 1
        }

        Integer safety = 0
        if(scoreText.contains("SAFETY") || scoreText.contains("safety")) {
            safety = 1
        }

        return new Rush(gameid: gameId, playNum: playNum, teamId: teamId, playerId: playerId ? playerId as Integer : 0, attempt: 1, yards: yards ? yards as Integer : 0, touchdown: touchdown, firstDown: firstDown, sack: sack, fumble: fumble, fumbleLost: fumbleLost, safety: safety)
    }

    static int calculateFumbleLost(String scoreText,Integer teamId, Map rosters) {
        String subString = scoreText.substring(scoreText.indexOf("FUMBLES")+7)//skip FUMBLES
        if (subString[0] == ".") {
            subString = subString.substring(2)
            //know the recovering player is next or it's a space
            Integer number = handleNumber(subString)
            if(!number) {
                return 0
            }
            String firstInitial = subString.substring(subString.indexOf("-")+1,subString.indexOf("."))
            String lastName = subString.substring(subString.indexOf(".")+1, subString.indexOf(" "))
            if(isRecoveringPlayerOnOwnRoster(teamId, number, firstInitial, lastName, rosters)) {
                return 0
            } else {
                return 1
            }
        } else if (subString[0] == " ") {
            //this case has a forced by () block, deal w/it & find recovering player
            String rest = subString.substring(subString.indexOf(").")+2).trim()
            Integer number = handleNumber(rest)
            if(!number) {
                return 0
            }

            String firstInitial = rest.substring(rest.indexOf("-")+1,rest.indexOf("."))
            String lastName = rest.substring(rest.indexOf(".")+1, rest.indexOf(" "))
            if(isRecoveringPlayerOnOwnRoster(teamId, number, firstInitial, lastName, rosters)) {
                return 0
            } else {
                return 1
            }
        } else {
            return 0
        }
    }

    static boolean isRecoveringPlayerOnOwnRoster(Integer teamId,Integer number, String firstInitial, String lastName, Map rosters ) {
        //implement me
        List offensiveTeam = rosters.get(teamId.toString())
        //build a player look up
        if(playerFound(offensiveTeam, number, firstInitial, lastName)) {
            return 1
        }
        return 0
    }

    static boolean playerFound(List team, Integer number, String firstInitial, String lastName) {
        return team.find {
            it.uniform_number.contains(number.toString()) &&
            it.lastname == lastName &&
            it.firstname.substring(0,1) == firstInitial
        }
    }

    private static Integer handleNumber(String subString) {
        Integer number
        try {
            number = subString.substring(0, subString.indexOf("-")).trim()?.toInteger()
        } catch(StringIndexOutOfBoundsException e) {
            return null//no recovery
        }
        number
    }

}
