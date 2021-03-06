package com.footballscience.parser

import com.footballscience.domain.Drive
import com.footballscience.domain.DriveType
import com.footballscience.domain.Game
import com.footballscience.domain.Kickoff
import com.footballscience.domain.Pass
import com.footballscience.domain.Play
import com.footballscience.domain.Punt
import com.footballscience.domain.Rush
import org.apache.commons.lang3.StringUtils

class ScoreTextParserLib {
    static char dash = '-'

    /*
    Need to deal with the multiple teamid BS, passed in ID will need to be global one to write rows
     */
//    static PlayType determinePlayType(String gameId, Integer teamId, Integer defensiveTeamId, Integer playNum, Integer ytg, String scoreText, Map rosters, Boolean onsideFlag) {
//        if(isPass(scoreText)) {
//            Pass pass = createPassRow(gameId, teamId, playNum, ytg, scoreText, rosters)
//            PlayType.PASS
//        } else if(scoreText.contains("punts")) {
//            Punt punt = createPuntRow(gameId, teamId, defensiveTeamId, playNum, scoreText, rosters)
//            PlayType.PUNT
//        } else if(scoreText.contains("kicks")) {
//            //kick-off row
//            Kickoff kickoff = createKickoffRow(gameId, teamId, playNum, scoreText, onsideFlag, rosters)
//            PlayType.KICKOFF
//        } else if((scoreText.contains("penalty") || scoreText.contains("Penalty") && !scoreText.contains("enforced")) ) {
//            //parse out penalty details, write to separate table?
//            //treat 'no play' rows discreetly
//            if(scoreText.contains('No Play')) {
//                //write the row?
//                PlayType.PENALTY//have to account for holding penalties that are accepted + enforced, not denoted w/No Play
//            }
//
//        } else if(scoreText.contains("Field Goal")) {
//            //write FG row
//            PlayType.FIELD_GOAL
//        } else if(scoreText.contains("extra point")) {
//            //write PAT row?
//            PlayType.ATTEMPT
//        } else if(scoreText.contains("Conversion") ||scoreText.contains("conversion") ) {
//            //parse this like a normal play for 2 points? Just record success/failure
//            if(isPass(scoreText)) {
//                Pass pass = createPassRow(gameId, teamId, playNum, ytg, scoreText, rosters)
//            } else {
//                Rush rush = createRushRow(gameId, teamId, playNum, ytg, scoreText, rosters)
//            }
//            PlayType.ATTEMPT
//            //create csv rush or pass row to attempts table on conversation attempt
//        } else if(scoreText.contains("Timeout") || scoreText.contains("timeout") || scoreText.contains("TIMEOUT")) {
//            //currently don't see this in the ncaa site data
//            PlayType.TIMEOUT
//        } else {
//            //must be a rush attempt
//            Rush rush = createRushRow(gameId, teamId, playNum, ytg, scoreText, rosters)
//            PlayType.RUSH
//        }
//    }

    static Map<PlayType,Object> determinePlayTypeAndMapPlay(String gameId, Integer teamId, Integer defensiveTeamId, Integer playNum, Integer ytg, String scoreText, Map rosters, Boolean onsideFlag, Integer yfog, Map abrMap, String homeTeamId) {
        Map returnMap = [:]
        if((scoreText.contains("penalty") || scoreText.contains("Penalty")) && scoreText.contains('No Play')) {//Need to box these out first so they don't included in play counts
            //parse out penalty details, write to separate table?
            //treat 'no play' rows discreetly
            returnMap.put(PlayType.PENALTY, null)
        } else if(isPass(scoreText)) {
            Pass pass = createPassRow(gameId, teamId, playNum, ytg, scoreText, rosters, abrMap, yfog, homeTeamId)
            returnMap.put(PlayType.PASS, pass)
        } else if(scoreText.contains("punts")) {
            Punt punt = createPuntRow(gameId, teamId, defensiveTeamId, playNum, scoreText, rosters)
            returnMap.put(PlayType.PUNT, punt)
        } else if(scoreText.contains("kicks")) {
            //kick-off row
            Kickoff kickoff = createKickoffRow(gameId, teamId, playNum, scoreText, onsideFlag, rosters)
            returnMap.put(PlayType.KICKOFF, kickoff)
        } else if((scoreText.contains("penalty") || scoreText.contains("Penalty")) && scoreText.contains('No Play')) {
            //parse out penalty details, write to separate table?
            //treat 'no play' rows discreetly
            returnMap.put(PlayType.PENALTY, null)
        } else if(scoreText.contains("Field Goal")) {
            //write FG row
            returnMap.put(PlayType.FIELD_GOAL, null)
        } else if(scoreText.contains("extra point")) {
            //write PAT row?
            returnMap.put(PlayType.ATTEMPT, null)
        } else if(scoreText.contains("Conversion") ||scoreText.contains("conversion") ) {
            //parse this like a normal play for 2 points? Just record success/failure
            if(isPass(scoreText)) {
                Pass pass = createPassRow(gameId, teamId, playNum, ytg, scoreText, rosters, abrMap, yfog, homeTeamId)
                returnMap.put(PlayType.ATTEMPT, pass)
            } else {
                Rush rush = createRushRow(gameId, teamId, playNum, ytg, scoreText, rosters, yfog,abrMap, homeTeamId)
                returnMap.put(PlayType.ATTEMPT, rush)
            }
            //create csv rush or pass row to attempts table on conversation attempt
        } else if(scoreText.contains("Timeout") || scoreText.contains("timeout") || scoreText.contains("TIMEOUT")) {
            //currently don't see this in the ncaa site data
            returnMap.put(PlayType.TIMEOUT, null)
        } else {
            //must be a rush attempt
            Rush rush = createRushRow(gameId, teamId, playNum, ytg, scoreText, rosters, yfog, abrMap, homeTeamId)
            returnMap.put(PlayType.RUSH, rush)
        }
        return returnMap
    }

    static Kickoff createKickoffRow(String gameId, Integer kickingTeamId, Integer returningTeamId, String scoreText, Boolean onsideFlag, Map rosters) {
        Kickoff kickoff = new Kickoff(gameId: gameId)

        kickoff.kickingTeamId = kickingTeamId
        kickoff.returningTeamId = returningTeamId


        if (scoreText.charAt(1) == dash || scoreText.charAt(2) == dash) {
            kickoff.kickerId = lookupLeadingPlayerId(scoreText, rosters, kickingTeamId)
        } else {
            kickoff.kickerId = -1
        }

        kickoff.yards = scoreText.substring(scoreText.indexOf("kicks")+5, scoreText.indexOf("yards")).trim() as Integer

        if(scoreText.contains("touchback") || scoreText.contains("Touchback")) {
            kickoff.touchback = 1
            kickoff.returnerId = 0
            kickoff.returnYards = 0
        } else {
            String returnInfo = scoreText.substring(scoreText.indexOf(". ")+1).trim()
            if(hasLeadingPlayer(returnInfo)) {
                kickoff.returnerId = lookupLeadingPlayerId(returnInfo, rosters, returningTeamId)
            }
            if(kickoff.returnerId) {
                if(scoreText.contains("no gain")) {
                    kickoff.returnYards = 0
                } else {
                    kickoff.returnYards = returnInfo.substring(returnInfo.indexOf('for')+4, returnInfo.indexOf("yards")).trim() as Integer
                }
            } else {
                //this is either an onside attempt or an oob kick
                kickoff.returnerId = 0
                if(scoreText.contains("out of bounds")) {
                    kickoff.returnYards = 0
                    kickoff.oob = 1
                }
                if(scoreText.contains("no gain")) {
                    //0 yard return, likely an onside kick
                    kickoff.returnYards = 0
                    if(kickoff.yards < 20 ) {//likely be an onside kick
                        kickoff.onside = 1
                        //now figure out if it was recovered by the kicking team
                        //need a reliable way to do this
                        if(scoreText.contains("downed")) {
                            kickoff.onsideSuccess = 0
                        }
                    }
                }
            }
        }
        return kickoff
    }

    /**
     * Designed to detect if a String begins with a player reference ex:  00-A.Anderson
     * @param text
     * @return
     */
    static boolean hasLeadingPlayer(String text) {
        return text.matches("^\\d{1,2}-[A-Z]\\.\\w{1,}\\s.*")
    }

    static Punt createPuntRow(String gameId, Integer kickTeamId, Integer returningTeamId, Integer playNum, String scoreText, Map rosters) {
        Punt punt = new Punt(gameId: gameId, teamId: kickTeamId, attempt: 1)
        punt.playNum = playNum


        if(scoreText.contains("blocked")) {
            punt.puntYards = 0
            punt.fairCatch = 0
            punt.oob = 0
            punt.returnerId = 0
            punt.returnYards = 0
            //block scenario, can be missing punter from front
            if(scoreText.startsWith("punts 0 yards")) {
                //found missing punter scenario
                punt.blocked = 1
                if(scoreText.contains("blocked by ")) {
                    String blockString = scoreText.substring(scoreText.indexOf("blocked by")+11)
                    String blockerString = blockString.substring(0, blockString.indexOf(" "))
                    String blockerJerseyNumer = blockerString.substring(0, blockerString.indexOf("-"))
                    String blockerFirstInitial = blockerString.substring(blockerString.indexOf("-") + 1, blockerString.indexOf("."))
                    String blockerLastName = blockerString.substring(blockerString.indexOf(".")+1, blockerString.length()-1)
                    punt.blockerId = getPlayerIdFromRosters(rosters,returningTeamId,blockerJerseyNumer, blockerFirstInitial, blockerLastName)
                }

            } else {
                //need to find other blocked punt scenarios
            }
            return punt
        }

        punt.punterId = lookupLeadingPlayerId(scoreText, rosters, kickTeamId)//looks like punter numbers are often wrong, maybe if this misses try selecting by pos + name only
        punt.puntYards = scoreText.substring(scoreText.indexOf("punts")+5, scoreText.indexOf("yards")).trim() as Integer

        if (scoreText.contains('fair catch')) {
            punt.returnYards = 0
            punt.fairCatch = 1
            punt.touchBack = 0
            punt.oob = 0
            punt.downed = 0
            //find returnerId at end of the string
            if(!scoreText.contains("fair catch by.")) {
                //this is a bad scenario and the scorer missed who made the fair catch or omitted it
                String returnerString = scoreText.substring(scoreText.indexOf('fair catch by')+13).trim()
                String returnerJerseyNumber = returnerString.substring(0, returnerString.indexOf("-"))
                String returnerFirstInitial = returnerString.substring(returnerString.indexOf("-") + 1, returnerString.indexOf("."))
                String returnerLastName = returnerString.substring(returnerString.indexOf(".")+1, returnerString.length()-1)
                punt.returnerId = getPlayerIdFromRosters(rosters,returningTeamId,returnerJerseyNumber, returnerFirstInitial, returnerLastName)
            }

        } else if(scoreText.contains('touchback')) {
            punt.returnerId = 0
            punt.returnYards = 0
            punt.fairCatch = 0
            punt.touchBack = 1
            punt.oob = 0
        } else if(scoreText.contains('downed by') || scoreText.contains("Downed") || scoreText.contains("downed")) {
            punt.returnerId = 0
            punt.returnYards = 0
            punt.fairCatch = 0
            punt.touchBack = 0
            punt.downed = 1
            punt.oob = 0
        } else if (scoreText.contains('out of bounds')) {
            punt.returnerId = 0
            punt.returnYards = 0
            punt.fairCatch = 0
            punt.touchBack = 0
            punt.downed = 0
            punt.oob = 1
        } else  {
            if(wasFumbled(scoreText)) {//fumbles are a 1 off, examine scoreText and add to test
                punt.fairCatch = 0
                punt.touchBack = 0
                punt.oob = 0
                punt.fumble = 1
                punt.fumbleLost = calculateFumbleLost(scoreText, returningTeamId, rosters)
            } else {
                String returnString = scoreText.substring(scoreText.indexOf(". " )+2)
                punt.returnerId = lookupLeadingPlayerId(returnString, rosters, returningTeamId)
                if (returnString.contains("no gain")) {
                    punt.returnYards = 0
                } else {
                    punt.returnYards = returnString.substring(returnString.indexOf("for")+4,returnString.indexOf("yard"))?.trim() as Integer
                }
            }

        }
        return punt
    }

    static boolean isPass(String scoreText) {
        return scoreText.contains("incomplete") || scoreText.contains("complete")
    }

    static Pass createPassRow(String gameId, Integer teamId, Integer playNum, Integer ytg, String scoreText, Map rosters, Map abrMap, Integer yfog, String homeTeamId) {
        String awayTeamId
        if(rosters.keySet().size() == 2) {
            awayTeamId = (rosters.keySet() - homeTeamId)[0]
        }
        Pass pass = new Pass(gameId: gameId, teamId: teamId, playNum: playNum)
        pass.passerId = lookupLeadingPlayerId(scoreText, rosters, teamId)

        pass.attempt = 1//just hard code for now, circle back & deal w/exceptions
        String subScoreText = scoreText.substring(scoreText.indexOf(" "))//text after passer
        if(scoreText.contains('complete to')) {
            pass.completion = 1
            pass.interception = 0
            //find the target
            String receiverJerseyNumber = subScoreText.substring(subScoreText.indexOf('to')+2,subScoreText.indexOf("-")).trim()
            String receiverFirstInitial = subScoreText.substring(subScoreText.indexOf("-")+1,subScoreText.indexOf("."))

            String receiverLastName
            try {
                receiverLastName = subScoreText.substring(subScoreText.indexOf(".")+1,subScoreText.indexOf(". "))
            } catch(IndexOutOfBoundsException e) {//this handles bizarre extra spaces in players last name
                String remainder = subScoreText.substring(subScoreText.indexOf(".")+1).trim()
                receiverLastName = remainder.substring(0, remainder.indexOf(". ")).trim()
            }

            pass.recieverId = getPlayerIdFromRosters(rosters, teamId, receiverJerseyNumber, receiverFirstInitial, receiverLastName)

            if(scoreText.contains("touchdown")) {
                pass.touchdown = 1
                pass.yards = subScoreText.substring(subScoreText.indexOf("runs")+4,subScoreText.indexOf("yard")).trim() as Integer
                //need to do yards correctly for TD scenario
            } else if (scoreText.contains("no gain")) {
                pass.touchdown = 0
                pass.yards = 0
            } else {
                pass.touchdown = 0
                pass.yards = subScoreText.substring(subScoreText.indexOf("for")+4,subScoreText.indexOf("yard")).trim() as Integer
            }

            if(pass.yards > ytg) {
                pass.firstDown = 1
            } else pass.firstDown = 0
        } else {
            pass.completion = 0
            pass.yards = 0
            pass.firstDown = 0
            pass.touchdown = 0
            //find the target - different pattern
            pass.passerId = lookupLeadingPlayerId(scoreText, rosters, teamId)
            String intendedForText = subScoreText.substring(subScoreText.indexOf(".")+1)

            if(wasIntercepted(scoreText)) {
                pass.interception = 1
                //do some funky shit for INT row
                if(subScoreText.indexOf("Intended for") == -1) {//no target on the INT line
                    pass.recieverId = 0
                } else {//try to find the intended WR
                    String playerChunk = subScoreText.substring(subScoreText.indexOf("Intended for")+12,subScoreText.indexOf(","))

                    String intendedReceiverJerseyNumber = playerChunk.substring(0, playerChunk.indexOf("-")).trim()
                    String intendedReceiverFirstInitial = playerChunk.substring(playerChunk.indexOf("-")+1,playerChunk.indexOf("."))
                    String intendedReceiverLastName = playerChunk.substring(playerChunk.indexOf(".")+1)
                    pass.recieverId = getPlayerIdFromRosters(rosters, teamId, intendedReceiverJerseyNumber, intendedReceiverFirstInitial, intendedReceiverLastName)
                }
            } else {
                pass.interception = 0
                if(intendedForText.isEmpty()) {
                    pass.recieverId = 0//un-targeted pass
                } else {
                    String receiverJerseyNumber = intendedForText.substring(intendedForText.indexOf('for')+4,intendedForText.indexOf("-"))
                    String receiverFirstInitial = intendedForText.substring(intendedForText.indexOf("-")+1,intendedForText.indexOf("."))
                    String receiverLastName = intendedForText.substring(intendedForText.indexOf(".")+1).replace(".", "")
                    pass.recieverId = getPlayerIdFromRosters(rosters, teamId, receiverJerseyNumber, receiverFirstInitial, receiverLastName)
                }

            }
        }

        if(wasFumbled(scoreText)) {
            pass.fumble = 1
            pass.fumbleLost = calculateFumbleLost(scoreText, teamId, rosters)
            //do fumble yards here
            if(!(pass.fumbleLost == 1)) {
                String spotString = scoreText.substring(scoreText.lastIndexOf("to")+2, scoreText.indexOf("for")).replace(" ", "")
                int endingYfog = calculateSpot(abrMap, spotString, Integer.parseInt(awayTeamId))
                pass.yards = Math.abs(yfog - endingYfog)
            }

        } else {
            pass.fumble = 0
            pass.fumbleLost = 0
        }
        return pass
    }

    private static boolean wasIntercepted(String scoreText) {
        scoreText.contains('INTERCEPTED')
    }

    private static boolean wasFumbled(String scoreText) {
        scoreText.contains('FUMBLES')
    }

    protected static Integer lookupLeadingPlayerId(String scoreText, Map rosters, int teamId) {
        String playerJerseyNumber = scoreText.substring(0, scoreText.indexOf("-"))
        String playerFirstInitial = scoreText.substring(scoreText.indexOf("-") + 1, scoreText.indexOf("."))
        String playerLastName
        if(scoreText.contains(" ")) {//conditions where player followed by whole pbp string
            playerLastName = scoreText.substring(scoreText.indexOf(".") + 1, scoreText.indexOf(" "))
        } else {// no spaces, just read to EOL
            playerLastName = scoreText.substring(scoreText.indexOf(".") + 1)
        }
        return getPlayerIdFromRosters(rosters, teamId, playerJerseyNumber, playerFirstInitial, playerLastName)
    }

    /*
        Do comparison using lower case on last name, expect DB data to have mixed case data vs play by play
        example Matt VandeBerg in Roster data vs Matt Vandeberg in play by play data
     */
    protected static Integer getPlayerIdFromRosters(Map rosters, int teamId, String jerseyNumber, String firstInitial, String lastName) {
        rosters.get(teamId.toString()).find {
            it.uniform_number.contains(jerseyNumber) && it.lastname == lastName.toLowerCase() && it.firstname.substring(0, 1) == firstInitial
        }?.player_id
    }

    static String getEnforcedSpot(String scoreText) {
        String tailString = scoreText.substring(scoreText.indexOf("enforced at")+11, scoreText.lastIndexOf(".")).replace(" ", "")
        return " "+tailString
    }

    static Rush createRushRow(String gameId, Integer teamId, Integer playNum, Integer ytg, String scoreText, Map rosters, Integer yfog, Map abrMap, String homeTeamId) {
        String awayTeamId
        if(rosters.keySet().size() == 2) {
            awayTeamId = (rosters.keySet() - homeTeamId)[0]
        }
        String yards
        int attempt = 1
        //filter kneel downs
        if(scoreText.contains("kneels")) {
            //find the yards still
            yards = scoreText.substring(scoreText.indexOf("for")+3,scoreText.indexOf("yard")).trim()
            return new Rush(gameid: gameId, playNum: playNum, teamId: teamId, attempt: attempt,  kneelDown: 1, yards: Integer.parseInt(yards))

        }

        Integer sack = 0
        if(scoreText.contains("sack") || scoreText.contains("Sack") || scoreText.contains("sacked")) {
            sack = 1
//            if(scoreText.contains("FUMBLES")) {
//                attempt = 0
//                //come up with a way to parse out own fumbles and log yards/attempt correctly if this matters?
//            }
        }

        String playerId
        if(scoreText.charAt(1) == dash || scoreText.charAt(2) == dash) {
            String jerseyNumber = scoreText.substring(0, scoreText.indexOf("-"))
            String firstInitial = scoreText.substring(scoreText.indexOf("-")+1,scoreText.indexOf("."))
            String lastName = scoreText.substring(scoreText.indexOf(".")+1, scoreText.indexOf(" "))
            //look up player id
            playerId = getPlayerIdFromRosters(rosters, teamId, jerseyNumber, firstInitial, lastName)
        }


        Integer touchdown = 0
        if(scoreText.contains("Touchdown") || scoreText.contains("touchdown")) {
            touchdown = 1
        }


        Integer safety = 0
        if(scoreText.contains("SAFETY") || scoreText.contains("safety")) {
            safety = 1
        }

        if(touchdown) {
            //check for 1 off defensive recovery score
            if(scoreText.contains("End Zone") && scoreText.contains("FUMBLES")) {
                yards = 0
                touchdown = 0
            } else {
                yards = scoreText.substring(scoreText.indexOf("runs")+4,scoreText.indexOf("yard")).trim()
            }
        } else {
            //handle no gain cases
            if (scoreText.contains("no gain") && sack == 0) {
                yards = 0
            } else {
                //default yards calculation
                yards = scoreText.substring(scoreText.indexOf("for")+4,scoreText.indexOf("yard"))?.trim()
            }
        }

        Integer fumble = 0
        Integer fumbleLost = 0
        if(scoreText.contains("FUMBLES")) {
            fumble = 1
            fumbleLost = calculateFumbleLost(scoreText, teamId, rosters)
            //do custom yards calc here
            //get spot
            if(!scoreText.contains("sacked") && fumbleLost == 0 && safety == 0) {//only do yard calc for non sacks
                String spotString = scoreText.substring(scoreText.indexOf("to")+2, scoreText.indexOf(",")).replace(" ", "")
                int endingYfog = calculateSpot(abrMap, spotString, Integer.parseInt(awayTeamId))
                yards = Math.abs(yfog - endingYfog)
            }
        }

        Integer firstDown = 0
        if(Integer.parseInt(yards) >= ytg) {
            firstDown = 1
        }

        //penalty mods
        if(scoreText.contains("penalty") || scoreText.contains("Penalty")) {
            if(!scoreText.contains("Intentional grounding")) {
                //this is an insane one off; calculate from enforced spot
                //use start yardline = yfog, then calculate yards from 'enforced at' spot
                String enforcedSpot = getEnforcedSpot(scoreText)
                int endingYfog = calculateSpot(abrMap,enforcedSpot,Integer.parseInt(awayTeamId))
                yards = Math.abs(yfog - endingYfog)
            }
        }

        return new Rush(gameid: gameId, playNum: playNum, teamId: teamId, playerId: playerId ? playerId as Integer : 0, attempt: attempt, yards: yards ? yards as Integer : 0, touchdown: touchdown, firstDown: firstDown, sack: sack, fumble: fumble, fumbleLost: fumbleLost, safety: safety)
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
        List offensiveTeam = rosters.get(teamId.toString())
        if(playerFound(offensiveTeam, number, firstInitial, lastName)) {
            return 1
        }
        return 0
    }

    static boolean playerFound(List team, Integer number, String firstInitial, String lastName) {
        return team.find {
            it.uniform_number.contains(number.toString()) &&
            it.lastname == lastName.toLowerCase() &&
            it.firstname.substring(0,1) == firstInitial
        }
    }

    static Integer handleNumber(String subString) {
        Integer number
        try {
            number = subString.substring(0, subString.indexOf("-")).trim()?.toInteger()
        } catch(StringIndexOutOfBoundsException e) {
            return null//no recovery
        }
        number
    }

    static List parseTacklerBlock(String scoreText, Map rosters, Integer defenseTeamId) {
        String tacklerText = scoreText.substring(scoreText.indexOf("(")+1, scoreText.indexOf(")"))
        List tacklerIds = tacklerText.split(",").collect {
            lookupLeadingPlayerId(it.trim(), rosters, defenseTeamId)
        }
        tacklerIds
    }

    static Integer convertTimeStringToSeconds(String timeString) {
        if(!StringUtils.isEmpty(timeString)) {
            String minsString = timeString.substring(0, timeString.indexOf(":"))
            Integer mins = minsString ? minsString.toInteger() : 0
            Integer seconds = timeString.substring(timeString.indexOf(":")+1).toInteger()
            return (mins * 60) + seconds
        }
        return 0
    }

    static DriveType determineEndType(Play play) {
        DriveType value
        if (play) {
            if(play.playType == PlayType.PUNT) {//punts
                value =  DriveType.PUNT
            } else if(play.playType == PlayType.ATTEMPT) {//touchdown
                value = DriveType.TOUCHDOWN
            } else if (play.playType == PlayType.FIELD_GOAL) {
                //determine if missed or made by exmining score/text - MISSED_FG, FIELD_GOAL
                if(play.fullScoreText.toLowerCase().contains("no good")) {
                    value = DriveType.MISSED_FG
                } else  {
                    value = DriveType.FIELD_GOAL
                }
            } else if (play.playType == PlayType.PASS && wasIntercepted(play.fullScoreText)) {
                value = DriveType.INTERCEPTION
            }  else if (play.fullScoreText && wasFumbled(play.fullScoreText)) {
                value = DriveType.FUMBLE
            } else {
                //must be downs
                value = DriveType.DOWNS
            }
        }

        value
    }

    static void addMappedPlayToDomainObject(Map<PlayType, Object> mappedPlay, Play play) {
        switch (mappedPlay.keySet()[0]) {
            case PlayType.RUSH:
                play.rush = mappedPlay.get(PlayType.RUSH) as Rush
                break
            case PlayType.PASS:
                play.pass = mappedPlay.get(PlayType.PASS) as Pass
                break
            case PlayType.KICKOFF:
                play.kickoff = mappedPlay.get(PlayType.KICKOFF) as Kickoff
                break
            case PlayType.PUNT:
                play.punt = mappedPlay.get(PlayType.PUNT) as Punt
                break
            default:
                break
        }

    }

    static Game createGameFromDrives(List<Drive> drives) {
        Game game = new Game()
        game.drives = drives
        //find scores by walking back to last scoring play?
        boolean scoreFound = false

        Drive lastScoringDrive = drives.reverse().find { Drive drive ->
            return drive.plays.find { Play play ->
                if(play.homeScore != 0 || play.visitingScore != 0) {
                    return play
                }

            }
        }
        Play lastScoringPlay = lastScoringDrive.plays.last()
        game.homeScore = lastScoringPlay.homeScore
        game.visitorScore = lastScoringPlay.visitingScore

        return game
    }
/*


 */
    static Integer calculateSpot(Map abrMap, String driveText, Integer awayTeamId) {
        String last
        if(driveText.contains(" ")) {
            last = driveText.substring(driveText.lastIndexOf(" "))
        } else {
            last = driveText
        }
        String teamString = last.replaceAll("[0-9]", "").trim()
        Integer yardline = last.replaceAll("[A-Za-z]", "").toInteger()

        if(abrMap.get(awayTeamId) == teamString) {
            return yardline
        } else {
            return (50 - yardline) + 50
        }
    }

    static boolean isChangeOfPossesionPlay(String fullScoreText) {
        if(fullScoreText.contains("punt")) {
            return true
        }
        if(wasIntercepted(fullScoreText)) {
            return true
        }
        false
    }
}
