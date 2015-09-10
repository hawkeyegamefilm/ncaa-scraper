package com.footballscience.domain

class Kickoff {
    String gameId
    Integer playNum
    Integer teamId
    Integer kickerId
    Integer attempt
    Integer yards
    Integer fairCatch
    Integer touchBack
    Integer oob
    Integer onside
    Integer onsideSuccess
    Integer returnerId
    Integer returnYards
    Integer fumble
    Integer fumbleLost
    Integer safety
    Integer touchdown

    String toCsvRow() {
        StringBuffer buffer = new StringBuffer()
        buffer.append(gameId).append(",")
        buffer.append(playNum).append(",")
        buffer.append(teamId).append(",")
        buffer.append(kickerId).append(",")
        buffer.append(attempt).append(",")
        buffer.append(yards).append(",")
        buffer.append(fairCatch).append(",")
        buffer.append(touchBack).append(",")
        buffer.append(oob).append(",")
        buffer.append(onside).append(",")
        buffer.append(onsideSuccess)
        buffer.append(returnerId).append(",")
        buffer.append(returnYards).append(",")
        buffer.append(fumble).append(",")
        buffer.append(fumbleLost).append(",")
        buffer.append(safety).append(System.lineSeparator())
        buffer
    }
}
