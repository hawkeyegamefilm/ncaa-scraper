package com.footballscience.scraper

import com.footballscience.database.ParserDAO
import com.footballscience.domain.DriveStart
import com.footballscience.parser.ScoreTextParserLib
import org.codehaus.jackson.map.ObjectMapper

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

    Map downMap = ["1st": 1, "2nd": 2, "3rd": 3, "4th": 4]
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

    String createPlayRowsCSV(Map game) {
        StringBuffer playRows = new StringBuffer()
        StringBuffer driveRows = new StringBuffer()
        Map hometeam = game.meta.teams.find { it.homeTeam == 'true'}
        Map awayteam = game.meta.teams.find { it.homeTeam == 'false'}
        populateDataForRun(hometeam, awayteam)
        ArrayList teams = [hometeam.id, awayteam.id]
        String gameId = homeTeamId + awayTeamId + year + month + day
        String down
        String ytg
        String yfog
        String startYfog
        Integer defensiveTeamId
        String driveStartType
        String driveEndTime
        Boolean multiplePeriodDriveScenario

        game.periods.eachWithIndex { period, periodIndex ->
            period.possessions.eachWithIndex { poss,possIndex ->
                //gonna need to produce a drive row here
                //add up play types from drive? probably best way to do it
                if(possIndex == period.possessions.length  ) {
                    //last drive of period
                    //check to see if poss bridges
                    multiplePeriodDriveScenario = true
                }

                poss.plays.eachWithIndex { play, playIndex ->
                    //write play rows based on cfbstats db
                    //calculate driveStart type here on index = 0
                    if(possIndex == 0) { //opening kick
                        driveStartType = DriveStart.KICKOFF
                    } else {
                        //peak back at previous type endType?
                        driveStartType = period.possessions[possIndex-1].endType
                    }
                    defensiveTeamId = (teams - poss.teamId)[0] as Integer

                    playRows.append(gameId).append(",")
                    playRows.append(playIndex).append(",")
                    playRows.append(periodIndex).append(",")
                    playRows.append(poss.time).append(",")
                    playRows.append(poss.teamId).append(",")
                    playRows.append(defensiveTeamId).append(",")
                    playRows.append(play.visitingScore).append(",")//not sure this is correct
                    playRows.append(play.homeScore).append(",")//may need to invert these
                    if(play.driveText) {
                        if(playIndex == 0) {
                            startYfog = calculateSpot(play.driveText,awayteam.id as Integer)
                        }
                        down = downMap.get(play.driveText.substring(0,3))
                        ytg = play.driveText.substring(7,10).trim()
                        yfog = calculateSpot(play.driveText,awayteam.id as Integer)
                        playRows.append(down).append(",")
                        playRows.append(ytg).append(",")
                        playRows.append(yfog).append(",")
                    } else {
                        playRows.append(",,,")//dummy up missing data exception, ie kickoffs, extra pts
                    }

                    Boolean onsideFlag = poss.plays.size > 1 //trying to flag onsides
                    playRows.append(ScoreTextParserLib.determinePlayType(gameId, poss.teamId as Integer, defensiveTeamId, playIndex as Integer, ytg as Integer, play.scoreText, rosters, onsideFlag))

                    //rows.append(play.scoreText)
                    playRows.append(System.lineSeparator())
                }

                //"Game Code","Drive Number","Team Code","Start Period","Start Clock","Start Spot","Start Reason","End Period","End Clock","End Spot","End Reason","Plays","Yards","Time Of Possession","Red Zone Attempt"

                driveRows.append(gameId).append(",")
                driveRows.append(possIndex).append(",")
                driveRows.append(poss.teamId).append(",")
                driveRows.append(poss.periodIndex).append(",")
                driveRows.append(poss.time).append(",")
                driveRows.append(startYfog).append(",")
                driveRows.append(driveStartType).append(",")//startType

                if(!multiplePeriodDriveScenario) {
                    driveRows.append(periodIndex).append(",")//endPeriod

                } else {
                    driveRows.append(periodIndex+1).append(",")//endPeriod
                    //toggle another iteration of plays and append to existing drive row data
                }

//                driveRows.append(endClock).append(",")//endClock
//                driveRows.append(endSpot).append(",")//endSpot
//                driveRows.append(endType).append(",")//endType
//                driveRows.append(poss.plays.size()).append(",")//startType
//                driveRows.append(poss.plays.size()).append(",")//drive yards
//                driveRows.append(poss.plays.size()).append(",")//TOP


            }
        }
        playRows.toString()
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
