package com.footballscience.domain

class Punt {
    String gameId
    Integer playNum
    Integer teamId
    Integer punterId
    Integer attempt
    Integer puntYards
    Integer blocked
    Integer fairCatch
    Integer touchBack
    Integer downed
    Integer oob
    Integer returnerId
    Integer returnYards
    Integer fumble
    Integer fumbleLost
    Integer safety
    Integer touchdown
    Integer blockerId

    String toCsvRow() {
        StringBuffer buffer = new StringBuffer()
        buffer.append(gameId).append(",")
        buffer.append(playNum).append(",")
        buffer.append(teamId).append(",")
        buffer.append(punterId).append(",")
        buffer.append(attempt).append(",")
        buffer.append(puntYards).append(",")
        buffer.append(blocked).append(",")
        buffer.append(fairCatch).append(",")
        buffer.append(touchBack).append(",")
        buffer.append(downed).append(",")
        buffer.append(oob).append(",")
        buffer.append(returnerId).append(",")
        buffer.append(returnYards).append(",")
        buffer.append(fumble).append(",")
        buffer.append(fumbleLost).append(",")
        buffer.append(safety).append(System.lineSeparator())
        buffer.toString()
    }

}
