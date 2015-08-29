package com.footballscience.scraper

import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import org.jsoup.select.Elements

class RosterScraper {

    String baseUrl = "http://stats.ncaa.org/team/roster/11980?org_id="
    String resourcePath


    void runLoad() {
        writeFile(harvestAllRosters())
    }

    String harvestAllRosters() {
        StringBuffer buffer = new StringBuffer()
        getAllOrgIds().each {
            buffer.append(createBatchFileByOrgId(it))
        }
        buffer.toString()

    }

    void writeFile(String fileContents) {
        File file = new File( resourcePath.substring(0,resourcePath.lastIndexOf("/")+1) + "results.csv")
        file.withWriter { writer ->
            writer.write(fileContents)
        }
    }

    List<String> getAllOrgIds() {
        List orgIds = new ArrayList()
        URL url = RosterScraper.getResource("teams.csv")
        resourcePath = url.getPath()
        url.text.eachLine {
            orgIds.add(it.tokenize(",")[0])
        }
        orgIds
    }

    String createBatchFileByOrgId(String orgId) {
        Document doc = Jsoup.connect(baseUrl+orgId).timeout(50000).get()
        return createCSVRowFromHtml(doc, orgId)
    }

    String createCSVRowFromHtml(Document doc, String orgId) {
        StringBuffer buffer = new StringBuffer()
        Elements table = doc.select("table#stat_grid")
        List<String> names

        table.select("tr:not(.heading)").each { tr ->
            buffer.append(orgId).append(",")
            tr.select("td").eachWithIndex { Element td, int index ->
                if(index == 1) {
                    names = td.text().tokenize(",").collect {it.trim()}
                    buffer.append(names[0]).append(",").append(names[1]).append(",")
                } else if(index == 5) {
                    buffer.append(td.text().trim())//no comma for last entry
                } else {
                    buffer.append(td.text().trim()).append(",")
                }
            }
            buffer.append(System.lineSeparator())
        }
        buffer.toString()
    }
}