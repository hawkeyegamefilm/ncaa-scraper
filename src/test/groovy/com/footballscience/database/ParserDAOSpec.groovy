package com.footballscience.database

import spock.lang.Specification

class ParserDAOSpec extends Specification {
    ParserDAO parserDAO

    def setup() {
        parserDAO = new ParserDAO()
    }

    def "test get rosters"() {
        when:
        List roster = parserDAO.getRosterBySeasonTeamId("71")

        then:
        roster.size() == 98
        roster[0].team_id == 312

        cleanup:
        parserDAO.connection.close()
    }

    def "test get abr"() {
        when:
        String abr = parserDAO.getAbreviationsByTeamId("71")

        then:
        abr == 'IOW'

        cleanup:
        parserDAO.connection.close()
    }
}
