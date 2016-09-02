package com.footballscience.domain

class Rush {
    String gameid
    Integer playNum
    Integer teamId
    Integer playerId
    Integer attempt
    Integer yards
    Integer touchdown
    Integer firstDown
    Integer sack
    Integer fumble
    Integer fumbleLost
    Integer safety

    String toCsvRow() {
        StringBuffer buffer = new StringBuffer()
        buffer.append(gameid).append(",")
        buffer.append(playNum).append(",")
        buffer.append(teamId).append(",")
        buffer.append(playerId).append(",")
        buffer.append(attempt).append(",")
        buffer.append(yards).append(",")
        buffer.append(touchdown).append(",")
        buffer.append(firstDown).append(",")
        buffer.append(sack).append(",")
        buffer.append(fumble).append(",")
        buffer.append(fumbleLost).append(",")
        buffer.append(safety).append(System.lineSeparator())
        buffer.toString()
    }
}
