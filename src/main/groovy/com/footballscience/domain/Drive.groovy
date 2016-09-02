package com.footballscience.domain

class Drive {
    String gameId
    Integer driveNumber
    Integer teamId
    Integer startPeriod
    Integer startClock
    Integer startSpot
    DriveType startType
    Integer endPeriod
    Integer endClock
    Integer endSpot
    DriveType endType
    Integer yards
    Integer top
    Integer rzDrive

    List plays

    String toCsvRow() {
        StringBuffer buffer = new StringBuffer()
        buffer.append(gameId).append(",")
        buffer.append(driveNumber).append(",")
        buffer.append(teamId).append(",")
        buffer.append(startPeriod).append(",")
        buffer.append(startClock).append(",")
        buffer.append(startSpot).append(",")
        buffer.append(startType).append(",")
        buffer.append(endPeriod).append(",")
        buffer.append(endClock).append(",")
        buffer.append(endSpot).append(",")
        buffer.append(endType).append(",")
        buffer.append(plays.size()).append(",")
        buffer.append(yards).append(",")
        buffer.append(top).append(",")
        buffer.append(rzDrive).append(System.lineSeparator())
        buffer.toString()
    }
}
