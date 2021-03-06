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
        List<Map<String, Object>> rs = handle.select("select p.player_id, p.team_id, lower(p.lastname) as lastname, p.firstname, p.uniform_number, p.class, p.position from player p, team t where p.team_id = t.team_id and t.pbp_team_id = ?",seasonTeamId)

        return rs
    }

    List getRosterBySeasonTeamIdAndSchema(String schema, String seasonTeamId) {
        List<Map<String, Object>> rs = handle.select("select p.player_id, p.team_id, lower(p.lastname) as lastname, p.firstname, p.uniform_number, p.class, p.position from ${schema}.player p, team t where p.team_id = t.team_id and t.pbp_team_id = ?",seasonTeamId)

        return rs
    }

    String getAbreviationsByTeamId(String seasonTeamId) {
        List<Map<String, Object>> rs = handle.select("select six_char_abr from team where pbp_team_id = ?",seasonTeamId)

        return rs[0].get('six_char_abr') as String
    }


}
