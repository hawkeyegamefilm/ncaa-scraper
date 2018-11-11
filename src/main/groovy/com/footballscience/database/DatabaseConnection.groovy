package com.footballscience.database

import org.skife.jdbi.v2.DBI
import org.skife.jdbi.v2.Handle

class DatabaseConnection {

    DBI dbi
    Handle handle

    DatabaseConnection() {
        dbi = new DBI("jdbc:mysql://localhost:3306/cfb_stats_18?user=root&password=password")
        handle = dbi.open()
    }

    void close() {
        handle.close()
    }

}
