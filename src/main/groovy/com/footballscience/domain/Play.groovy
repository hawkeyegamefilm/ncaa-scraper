package com.footballscience.domain

import com.footballscience.parser.PlayType

class Play {
    String gameId
    Integer playIndex
    Integer periodIndex
    Integer time
    Integer teamId
    Integer defensiveTeamId
    Integer visitingScore
    Integer homeScore
    Integer down
    Integer ytg
    Integer yfog
    PlayType playType
    Integer driveNumber
    Integer drivePlay

    String fullScoreText
    String driveText
    Rush rush
    Pass pass
    Kickoff kickoff
    Punt punt


    String toCsvRow() {
        StringBuffer stringBuffer = new StringBuffer()
        stringBuffer.append(gameId).append(",")
        stringBuffer.append(playIndex).append(",")
        stringBuffer.append(periodIndex).append(",")
        stringBuffer.append(time).append(",")
        stringBuffer.append(teamId).append(",")
        stringBuffer.append(defensiveTeamId).append(",")
        stringBuffer.append(visitingScore).append(",")
        stringBuffer.append(homeScore).append(",")
        if(driveText) {
            stringBuffer.append(down).append(",")
            stringBuffer.append(ytg).append(",")
            stringBuffer.append(yfog).append(",")
        } else {
            stringBuffer.append(",,,")//dummy up missing data exception, ie kickoffs, extra pts
        }

        stringBuffer.append(playType).append(",")
        stringBuffer.append(driveNumber).append(",")
        stringBuffer.append(drivePlay).append(",")
        stringBuffer.append(fullScoreText)
        stringBuffer.append(System.lineSeparator())
        stringBuffer.toString()
    }
}
