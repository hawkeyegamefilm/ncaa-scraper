package com.footballscience.scraper

import com.footballscience.database.ParserDAO
import com.footballscience.domain.Drive
import com.footballscience.domain.DriveType
import com.footballscience.domain.Play
import com.footballscience.parser.PlayType
import com.footballscience.parser.ScoreTextParserLib
import org.apache.commons.lang3.StringUtils
import org.codehaus.jackson.map.ObjectMapper

/*
Next steps
1) Refactor play extract to object/map
    Need to defer writing drive rows based upon switches from Plays
2)
 */

class PlayScraper {

    ObjectMapper objectMapper = new ObjectMapper()
    String year
    String month
    String day

    String homeTeamId
    String awayTeamId

    Map abrMap = [:]
    Map idMap = [:]

    Map rosters

    Map<String, Integer> downMap = ["1st": 1, "2nd": 2, "3rd": 3, "4th": 4]
    ParserDAO parserDAO

    void populateDataForRun(Map hometeam, Map awayteam) {
        populateRosters(hometeam, awayteam)
    }

    void populateRosters(Map hometeam, Map awayteam) {
        if(!parserDAO) {
            parserDAO = new ParserDAO()
        }
        List hometeamRoster = parserDAO.getRosterBySeasonTeamId(hometeam.id)
        homeTeamId = hometeamRoster[0].team_id
        List awayteamRoster = parserDAO.getRosterBySeasonTeamId(awayteam.id)
        awayTeamId = awayteamRoster[0].team_id
        rosters = [homeTeamId: hometeamRoster, awayTeamId: awayteamRoster]
        abrMap.put(hometeam.id as Integer, parserDAO.getAbreviationsByTeamId(hometeam.id))
        abrMap.put(awayteam.id as Integer, parserDAO.getAbreviationsByTeamId(awayteam.id))
        idMap.put(homeTeamId.toInteger(),hometeam.id)
        idMap.put(awayTeamId.toInteger(),awayteam.id)
    }

    Map getJsonFromUrl(String url) {
        populateDateVars(url)
        String json = url.toURL().text
        objectMapper.readValue(json, Map)
    }

    Map getJsonPFromUrl(String url) {
        populateDateVars(url)
        String jsonp = url.toURL().text
        objectMapper.readValue(cleanupTags(jsonp), Map)
    }

    void populateDateVars(String url) {
        String[] tokens = url.tokenize("/")
        year = tokens[6]
        month = tokens[7]
        day = tokens[8]
    }

    String cleanupTags(String jsonp) {
        jsonp.replace("callbackWrapper(", "").replace(");", "")
    }

    List<Drive> createPlayRowsCSV(Map game) {
        StringBuffer playRows = new StringBuffer()
        StringBuffer driveRows = new StringBuffer()
        Map hometeam = game.meta.teams.find { it.homeTeam == 'true'}
        Map awayteam = game.meta.teams.find { it.homeTeam == 'false'}
        populateDataForRun(hometeam, awayteam)
        ArrayList teams = [hometeam.id, awayteam.id]
        String gameId = homeTeamId + awayTeamId + year + month + day
        Integer down
        Integer ytg
        Integer yfog
        Integer startYfog
        Integer defensiveTeamId
        String driveStartType
        String driveEndTime
        Boolean multiplePeriodDriveScenario
        List plays
        Integer globalPlayCount = 0
        List drives = []

        Drive currentDrive
        Drive drivePlusOne

        Boolean appendToExistingDrive

        game.periods.eachWithIndex { Map period, Integer periodIndex ->
            period.possessions.eachWithIndex { Map poss, Integer possIndex ->

                if((possIndex+1) == period.possessions.size() ) {
                    //last drive of period
                    //check to see if poss bridges
                    multiplePeriodDriveScenario = true
                    println "MULTITHINGY"

                    if(!appendToExistingDrive) {
                        plays = [] //reset array per drive
                    }
                } else {
                    //normal init
                    if(!appendToExistingDrive) {
                        plays = [] //reset array per drive
                    }
                    multiplePeriodDriveScenario = false
                }

                if(multiplePeriodDriveScenario) {
                    //use poss and poss + 1 and manually roll fwd? or just code to keep state for two poss groups
                } else {
                    //execute as it does now
                }
                poss.plays.eachWithIndex { Map play, Integer playIndex ->
                    globalPlayCount++

                    //write play objects based on cfbstats db
                    //calculate driveStart type here on index = 0
                    if(possIndex == 0 && ([0,2].contains(periodIndex))) { //opening kick of game or half
                        driveStartType = DriveType.KICKOFF
                    } else {
                        //peak back at previous type endType?
                        driveStartType = period.possessions[possIndex-1].endType
                    }
                    defensiveTeamId = (teams - poss.teamId)[0] as Integer

                    if(play.driveText) {
                        if(playIndex == 0) {
                            startYfog = calculateSpot(play.driveText,awayteam.id as Integer)
                        }
                        down = downMap.get(play.driveText.substring(0,3))
                        ytg = Integer.parseInt(play.driveText.substring(7,10).trim())
                        yfog = calculateSpot(play.driveText,awayteam.id as Integer)
                    } else {
                        down = null
                        ytg = null
                        yfog = null
                    }

                    Boolean onsideFlag = poss.plays.size > 1 //trying to flag onsides
                    PlayType playType = ScoreTextParserLib.determinePlayType(gameId, poss.teamId as Integer, defensiveTeamId, playIndex as Integer, ytg as Integer, play.scoreText, rosters, onsideFlag)
                    plays.add(new Play(gameId: gameId, playIndex: globalPlayCount, periodIndex: periodIndex, time: ScoreTextParserLib.convertTimeStringToSeconds(poss.time), teamId: cleanString(poss.teamId as String), defensiveTeamId: defensiveTeamId, visitingScore: cleanString(play.visitingScore as String), homeScore: cleanString(play.homeScore as String),down:down, ytg: ytg, yfog: yfog, playType: playType, driveNumber: possIndex, drivePlay: playIndex, fullScoreText:play.scoreText, driveText: play.driveText))
                }

                Integer timeValue = poss.time ? ScoreTextParserLib.convertTimeStringToSeconds(poss.time) : 0
                Integer endPeriodIndex
                if(!multiplePeriodDriveScenario) {
                    endPeriodIndex = periodIndex
                } else {
                    //add another iteration of plays and append to existing drive row data
                    endPeriodIndex= periodIndex + 1
                }

                if(!appendToExistingDrive) {
                    currentDrive = new Drive(gameId: gameId, driveNumber: possIndex,teamId: poss.teamId as Integer, startPeriod: poss.periodIndex, startClock: timeValue, startSpot: startYfog, startType: driveStartType, endPeriod: endPeriodIndex, plays: plays)
                } else {
                    println "this happened"//should just roll into previously created object
                }

                  //information in current drive insufficient, need to peak at drive N+1, also multi-period drive to consider
//                driveRows.append(endClock).append(",")//endClock
//                driveRows.append(endSpot).append(",")//endSpot
//                driveRows.append(endType).append(",")//endType
//                driveRows.append(poss.plays.size()).append(",")//startType
//                driveRows.append(poss.plays.size()).append(",")//drive yards
//                driveRows.append(poss.plays.size()).append(",")//TOP
                if(currentDrive && !appendToExistingDrive) {
                    drives.add(currentDrive)
                }
                if(multiplePeriodDriveScenario) {
                    appendToExistingDrive = true//next time around use existing drive
                } else {
                    appendToExistingDrive=false //normal swing around
                }
            }
        }
        drives
    }

    static Integer cleanString(String scoreField) {
        if(StringUtils.isEmpty(scoreField)) {
            return 0
        } else {
            return Integer.parseInt(scoreField)
        }
    }

    Integer calculateSpot(String driveText, Integer teamId) {
        String last = driveText.substring(driveText.lastIndexOf(" "))
        String teamString = last.replaceAll("[0-9]", "").trim()
        Integer yardline = last.replaceAll("[A-Za-z]", "").toInteger()

        if(abrMap.get(teamId) == teamString) {
            return yardline
        } else {
            return (50 - yardline) + 50
        }
    }
}
