package com.footballscience.domain

class Kickoff {
    String gameId
    Integer playNum
    Integer kickingTeamId
    Integer kickerId
    Integer attempt
    Integer yards
    Integer fairCatch
    Integer touchback
    Integer oob
    Integer onside
    Integer onsideSuccess
    Integer returningTeamId
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
        buffer.append(kickingTeamId).append(",")
        buffer.append(kickerId).append(",")
        buffer.append(attempt).append(",")
        buffer.append(yards).append(",")
        buffer.append(fairCatch).append(",")
        buffer.append(touchback).append(",")
        buffer.append(oob).append(",")
        buffer.append(onside).append(",")
        buffer.append(onsideSuccess).append(",")
        buffer.append(returningTeamId).append(",")
        buffer.append(returnerId).append(",")
        buffer.append(returnYards).append(",")
        buffer.append(fumble).append(",")
        buffer.append(fumbleLost).append(",")
        buffer.append(safety).append(System.lineSeparator())
        buffer
    }
}
