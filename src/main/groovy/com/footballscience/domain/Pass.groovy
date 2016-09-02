package com.footballscience.domain

class Pass {
    String gameId
    Integer playNum
    Integer teamId
    Integer passerId
    Integer recieverId
    Integer attempt
    Integer completion
    Integer yards
    Integer touchdown
    Integer interception
    Integer firstDown
    Integer dropped
    Integer fumble
    Integer fumbleLost

    String toCsvRow() {
        StringBuffer buffer = new StringBuffer()
        buffer.append(gameId).append(",")
        buffer.append(playNum).append(",")
        buffer.append(teamId).append(",")
        buffer.append(passerId).append(",")
        buffer.append(recieverId).append(",")
        buffer.append(attempt).append(",")
        buffer.append(completion).append(",")
        buffer.append(yards).append(",")
        buffer.append(touchdown).append(",")
        buffer.append(interception).append(",")
        buffer.append(firstDown).append(",")
        buffer.append(dropped).append(",")
        buffer.append(fumble).append(",")
        buffer.append(fumbleLost).append(",").append(System.lineSeparator())
        buffer.toString()
    }
}
