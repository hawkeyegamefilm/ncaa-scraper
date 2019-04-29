package com.footballscience.scraper

import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import org.jsoup.select.Elements

class RosterScraper {

    String baseUrl = "http://stats.ncaa.org/team/roster/11980?org_id="
    String resourcePath

    String orgBaseUrl2018 = "http://stats.ncaa.org/team/"
    String baseStatsUrl = "http://stats.ncaa.org"

    String findRosterUrlbyOrg(String orgId) {
        Document orgSummaryPage = Jsoup.connect(orgBaseUrl2018+orgId).timeout(5000).get()
        String teamRelativePath = getFootballUrl(orgSummaryPage)

        Document teamSummaryPage = Jsoup.connect(baseStatsUrl+teamRelativePath).timeout(5000).get()
        String rosterPath = findRosterPathOnTeamPage(teamSummaryPage)

        return rosterPath
    }

    String findRosterPathOnTeamPage(Document teamSummaryPage) {
        Element rosterAnchor = teamSummaryPage.select("a").find { Element element ->
            if(element.getElementsContainingText("Roster").size() > 0 ) {
                return element
            }
        }
        return rosterAnchor.attr("href")
    }


    static String getFootballUrl(Document orgSummaryPage) {
        Element footballAnchor = orgSummaryPage.select("a").find { Element element ->
            if(element.getElementsContainingText("Football").size() > 0) {
                return element
            }
        }
        return footballAnchor.attr("href")
    }

    void runLoad() {
        writeFile(harvestAllRosters())
    }

    void runLoad2018() {
        writeFile(harvestAllRosters2018())
    }

    String harvestAllRosters2018() {
        StringBuffer buffer = new StringBuffer()
        List<String> orgIds = getAllOrgIds()
        orgIds.each { String orgId ->
            println("hitting url for: $orgId")
            String footballUrl = findRosterUrlbyOrg(orgId)
            Document doc = Jsoup.connect(baseStatsUrl+footballUrl).timeout(5000).get()
            buffer.append(createCSVRowFromHtml(doc, orgId))
            buffer.append(System.lineSeparator())
        }
        buffer.toString()
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
        Document doc = Jsoup.connect(baseUrl+orgId).timeout(30000).get()
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
        buffer.toString().trim()
    }
}
