package com.footballscience.database

import org.skife.jdbi.v2.Handle

class ParserDAO {
    DatabaseConnection connection
    Handle handle

    ParserDAO() {
        connection = new DatabaseConnection()
        handle = connection.handle
    }

    List getRosterBySeasonTeamId(String seasonTeamId) {
        List<Map<String, Object>> rs = handle.select("select p.* from player p, team t where p.team_id = t.team_id and t.14_team_id = ?",seasonTeamId)

        return rs
    }

    String getAbreviationsByTeamId(String seasonTeamId) {
        List<Map<String, Object>> rs = handle.select("select four_char_abr from team where 14_team_id = ?",seasonTeamId)

        return rs[0].get('four_char_abr') as String
    }


}
