package com.footballscience.scraper

import com.footballscience.database.ParserDAO
import com.footballscience.parser.ScoreTextParserLib
import org.codehaus.jackson.map.ObjectMapper

class PlayScraper {

    ObjectMapper objectMapper = new ObjectMapper()
    String year
    String month
    String day

    String homeTeamAbr = "IOW"
    String awayTeamAbr = "UNI"

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
        abrMap.put(hometeam.id as Integer, homeTeamAbr)
        abrMap.put(awayteam.id as Integer, awayTeamAbr)
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

    String createPlayRowCSV(Map game) {
        StringBuffer rows = new StringBuffer()
        Map hometeam = game.meta.teams.find { it.homeTeam = 'true'}
        Map awayteam = game.meta.teams.find { it.homeTeam = 'false'}
        populateDataForRun(hometeam, awayteam)
        ArrayList teams = [hometeam.id, awayteam.id]
        String gameId = homeTeamId + awayTeamId + year + month + day

        game.periods.eachWithIndex { period, periodIndex ->
            period.possessions.eachWithIndex { poss,possIndex ->
                //gonna need to produce a drive row here
                poss.plays.eachWithIndex { play, playIndex ->
                    //write play rows based on cfbstats db
                    rows.append(gameId).append(",")
                    rows.append(playIndex).append(",")
                    rows.append(periodIndex).append(",")
                    rows.append(poss.time).append(",")
                    rows.append(poss.teamId).append(",")
                    rows.append((teams - poss.teamId)[0]).append(",")
                    rows.append(play.visitingScore).append(",")
                    rows.append(play.homeScore).append(",")
                    if(play.driveText) {
                        rows.append(downMap.get(play.driveText.substring(0,2))).append(",")
                        rows.append(play.driveText.substring(7,10).trim()).append(",")
                    }
                    rows.append(calculateSpot(play.driveText,awayteam.id)).append(",")

                    rows.append(play.scoreText)
                    rows.append(System.lineSeparator())
                }
            }
        }
        rows.toString()
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

    String calculatePlayType(String scoreText) {

    }
}
