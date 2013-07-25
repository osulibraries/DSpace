package org.dspace.app.xmlui.aspect.dashboard;

import org.apache.cocoon.environment.ObjectModelHelper;
import org.apache.cocoon.environment.Request;
import org.apache.log4j.Logger;
import org.dspace.app.xmlui.aspect.statistics.StatisticsTransformer;
import org.dspace.app.xmlui.cocoon.AbstractDSpaceTransformer;
import org.dspace.app.xmlui.utils.HandleUtil;
import org.dspace.app.xmlui.wing.Message;
import org.dspace.app.xmlui.wing.WingException;
import org.dspace.app.xmlui.wing.element.*;
import org.dspace.content.*;
import org.dspace.content.Collection;
import org.dspace.content.Item;
import org.dspace.core.Constants;
import org.dspace.statistics.ElasticSearchLogger;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.search.SearchType;
import org.elasticsearch.client.Client;
import org.elasticsearch.common.joda.time.DateTime;
import org.elasticsearch.common.joda.time.Duration;
import org.elasticsearch.common.joda.time.Interval;
import org.elasticsearch.index.query.*;
import org.elasticsearch.search.facet.AbstractFacetBuilder;
import org.elasticsearch.search.facet.FacetBuilders;
import org.elasticsearch.search.facet.Facets;
import org.elasticsearch.search.facet.datehistogram.DateHistogramFacet;
import org.elasticsearch.search.facet.terms.TermsFacet;

import java.sql.SQLException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.List;

/**
 * Created by IntelliJ IDEA.
 * User: peterdietz
 * Date: 3/7/12
 * Time: 11:54 AM
 * To change this template use File | Settings | File Templates.
 */
public class ElasticSearchStatsViewer extends AbstractDSpaceTransformer {
    private static Logger log = Logger.getLogger(ElasticSearchStatsViewer.class);
    
    public static final String elasticStatisticsPath = "stats";

    private static SimpleDateFormat monthAndYearFormat = new SimpleDateFormat("MMMMM yyyy");
    private static SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");

    private static Client client;
    private static Division division;
    private static DSpaceObject dso;
    private static Date dateStart;
    private static Date dateEnd;

    protected static TermFilterBuilder justOriginals = FilterBuilders.termFilter("bundleName", "ORIGINAL");

    protected static AbstractFacetBuilder facetTopCountries = FacetBuilders.termsFacet("top_countries").field("country.untouched").size(150)
            .facetFilter(FilterBuilders.andFilter(
                justOriginals,
                FilterBuilders.notFilter(FilterBuilders.termFilter("country.untouched", "")))
            );

    protected static AbstractFacetBuilder facetMonthlyDownloads = FacetBuilders.dateHistogramFacet("monthly_downloads").field("time").interval("month")
            .facetFilter(FilterBuilders.andFilter(
                FilterBuilders.termFilter("type", "BITSTREAM"),
                justOriginals
            ));

    protected static AbstractFacetBuilder facetDailyDownloads = FacetBuilders.dateHistogramFacet("daily_downloads").field("time").interval("day")
            .facetFilter(FilterBuilders.andFilter(
                    FilterBuilders.termFilter("type", "BITSTREAM"),
                    justOriginals
            ));
    
    protected static AbstractFacetBuilder facetTopBitstreamsAllTime = FacetBuilders.termsFacet("top_bitstreams_alltime").field("id")
            .facetFilter(FilterBuilders.andFilter(
                    FilterBuilders.termFilter("type", "BITSTREAM"),
                    justOriginals
            ));
    
    protected static AbstractFacetBuilder facetTopUSCities = FacetBuilders.termsFacet("top_US_cities").field("city.untouched").size(50)
            .facetFilter(FilterBuilders.andFilter(
                FilterBuilders.termFilter("countryCode", "US"),
                justOriginals,
                FilterBuilders.notFilter(FilterBuilders.termFilter("city.untouched", ""))
            ));
    
    protected static AbstractFacetBuilder facetTopUniqueIP = FacetBuilders.termsFacet("top_unique_ips").field("ip");

    protected static AbstractFacetBuilder facetTopUniqueDNS = FacetBuilders.termsFacet("top_unique_dns").field("dns.untouched").size(50);
    
    protected static AbstractFacetBuilder facetTopTypes = FacetBuilders.termsFacet("top_types").field("type");

    /** Language strings */
    private static final Message T_dspace_home = message("xmlui.general.dspace_home");

    private static final Message T_trail = message("xmlui.ArtifactBrowser.ItemViewer.trail");

    public void addPageMeta(PageMeta pageMeta) throws WingException, SQLException {
        dso = getDSO();

        pageMeta.addMetadata("title").addContent("Statistics Report for : " + dso.getName());

        pageMeta.addTrailLink(contextPath + "/",T_dspace_home);
        HandleUtil.buildHandleTrailIncludeTerminal(dso,pageMeta,contextPath);
        pageMeta.addTrail().addContent("View Statistics");
    }

    public ElasticSearchStatsViewer() {

    }

    public ElasticSearchStatsViewer(DSpaceObject dso, Date dateStart, Date dateEnd) {
        this.dso = dso;
        this.dateStart = dateStart;
        this.dateEnd = dateEnd;
        client = ElasticSearchLogger.getInstance().getClient();
    }
    
    public void addBody(Body body) throws WingException, SQLException {
        try {
            //Try to find our DSpace object
            dso = getDSO();
            client = ElasticSearchLogger.getInstance().getClient();

            division = body.addDivision("elastic-stats");
            division.setHead("Statistical Report for " + dso.getName());
            division.addHidden("containerName").setValue(dso.getName());

            division.addHidden("baseURLStats").setValue(contextPath + "/handle/" + dso.getHandle() + "/" + elasticStatisticsPath);
            Request request = ObjectModelHelper.getRequest(objectModel);
            String[] requestURIElements = request.getRequestURI().split("/");

            // Statistics:Home = Summary, otherwise, a detailed report of aspect of statistics.
            if(requestURIElements[requestURIElements.length-1].trim().equalsIgnoreCase(elasticStatisticsPath)) {
                //Homepage will show the last 5 years worth of Data, and no form generator.
                Calendar cal = Calendar.getInstance();
                dateEnd = cal.getTime();

                //Roll back to Jan 1 0:00.000 five years ago.
                cal.roll(Calendar.YEAR, -5);
                cal.set(Calendar.MONTH, 0);
                cal.set(Calendar.DAY_OF_MONTH, 1);
                cal.set(Calendar.HOUR_OF_DAY,0);
                cal.set(Calendar.MINUTE, 0);
                cal.set(Calendar.SECOND, 0);
                cal.set(Calendar.MILLISECOND, 0);
                dateStart = cal.getTime();

                division.addHidden("reportDepth").setValue("summary");
                String dateRange = "Last Five Years [" + dateFormat.format(dateStart) + " to " + dateFormat.format(dateEnd) + "]";

                division.addHidden("timeRangeString").setValue("Date Range Shown: " + dateRange);
                if(dateStart != null) {
                    division.addHidden("dateStart").setValue(dateFormat.format(dateStart));
                }
                if(dateEnd != null) {
                    division.addHidden("dateEnd").setValue(dateFormat.format(dateEnd));
                }

                showAllReports();
                
            } else {
                //Other pages will show a form to choose which date range.
                ReportGenerator reportGenerator = new ReportGenerator();
                reportGenerator.addReportGeneratorForm(division, request);
                
                dateStart = reportGenerator.getDateStart();
                dateEnd = reportGenerator.getDateEnd();

                String requestedReport = requestURIElements[requestURIElements.length-1];
                log.info("Requested report is: "+ requestedReport);
                division.addHidden("reportDepth").setValue("detail");
                
                String dateRange = "";
                if(dateStart != null && dateEnd != null) {
                    dateRange = "from: "+dateFormat.format(dateStart) + " to: "+dateFormat.format(dateEnd);
                } else if (dateStart != null && dateEnd == null) {
                    dateRange = "starting from: "+dateFormat.format(dateStart);
                } else if(dateStart == null && dateEnd != null) {
                    dateRange = "ending with: "+dateFormat.format(dateEnd);
                } else if(dateStart == null && dateEnd == null) {
                    dateRange = "All Data Available";
                }
                division.addPara("Showing Data ( " + dateRange + " )");
                division.addHidden("timeRangeString").setValue(dateRange);
                if(dateStart != null) {
                    division.addHidden("dateStart").setValue(dateFormat.format(dateStart));
                }
                if(dateEnd != null) {
                    division.addHidden("dateEnd").setValue(dateFormat.format(dateEnd));
                }


                division.addHidden("reportName").setValue(requestedReport);

                if(requestedReport.equalsIgnoreCase("topCountries"))
                {
                    SearchRequestBuilder requestBuilder = facetedQueryBuilder(facetTopCountries, facetTopUSCities);
                    searchResponseToDRI(requestBuilder);
                }
                else if(requestedReport.equalsIgnoreCase("fileDownloads"))
                {
                    //When the requested range exceeds a months worth, give monthly results
                    // Otherwise gives results in days...
                    Boolean granularityResult = lessThanMonthApart(dateStart, dateEnd);
                    log.info("Do we give days, or months: " + granularityResult.toString() + " for date range:" + dateStart + " to " + dateEnd);

                    if(granularityResult) {
                        SearchRequestBuilder requestBuilder = facetedQueryBuilder(facetDailyDownloads);
                        searchResponseToDRI(requestBuilder);
                    } else {
                        SearchRequestBuilder requestBuilder = facetedQueryBuilder(facetMonthlyDownloads);
                        searchResponseToDRI(requestBuilder);
                    }
                }
                else if(requestedReport.equalsIgnoreCase("topDownloads"))
                {
                    SearchRequestBuilder requestBuilder = facetedQueryBuilder(facetTopBitstreamsAllTime, facetTopBitstreamsLastMonth());
                    SearchResponse resp = searchResponseToDRI(requestBuilder);

                    TermsFacet bitstreamsAllTimeFacet = resp.getFacets().facet(TermsFacet.class, "top_bitstreams_alltime");
                    addTermFacetToTable(bitstreamsAllTimeFacet, division, "Bitstream", "Top File Downloads (" + dateRange + ")");

                    TermsFacet bitstreamsFacet = resp.getFacets().facet(TermsFacet.class, "top_bitstreams_lastmonth");
                    addTermFacetToTable(bitstreamsFacet, division, "Bitstream", "Top File Downloads for " + getLastMonthString());
                }
                else if(requestedReport.equalsIgnoreCase("itemsAdded"))
                {
                    StatisticsTransformer statisticsTransformerInstance = new StatisticsTransformer(dateStart, dateEnd);

                    // 1 - Number of Items in The Container (Community/Collection) (monthly and cumulative for the year)
                    if(isContainer(dso)) {
                        statisticsTransformerInstance.addItemsInContainer(dso, division);
                        division.addDivision("chart_div");
                    }
                }
                else if(requestedReport.equalsIgnoreCase("filesAdded"))
                {
                    StatisticsTransformer statisticsTransformerInstance = new StatisticsTransformer(dateStart, dateEnd);
                    // 2 - Number of Files in The Container (monthly and cumulative)
                    if(isContainer(dso)) {
                        statisticsTransformerInstance.addFilesInContainer(dso, division);
                        division.addDivision("chart_div");
                    }
                }
            }

        } finally {
            //client.close();
        }
    }

    private DSpaceObject getDSO() throws SQLException {
        dso = HandleUtil.obtainHandle(objectModel);
        if(dso == null) {
            dso = Site.find(context, 0);
        }
        return dso;
    }

    private boolean isContainer(DSpaceObject dso) {
        return (dso instanceof org.dspace.content.Collection || dso instanceof Community || dso instanceof Site);
    }
    
    public void showAllReports() throws WingException, SQLException{
        // Show some non-usage-stats.
        // @TODO Refactor the non-usage stats out of the StatsTransformer
        StatisticsTransformer statisticsTransformerInstance = new StatisticsTransformer(dateStart, dateEnd);

        // 1 - Number of Items in The Container (Community/Collection) (monthly and cumulative for the year)
        if(isContainer(dso)) {
            statisticsTransformerInstance.addItemsInContainer(dso, division);
        }

        // 2 - Number of Files in The Container (monthly and cumulative)
        if(isContainer(dso)) {
            statisticsTransformerInstance.addFilesInContainer(dso, division);
        }

        List<AbstractFacetBuilder> summaryFacets = new ArrayList<AbstractFacetBuilder>();
        summaryFacets.add(facetMonthlyDownloads);
        summaryFacets.add(facetTopCountries);
        summaryFacets.add(facetTopBitstreamsLastMonth());

        if(dso instanceof Site) {
            //Goodies for site-as-a-whole view.

            //Hits to BITSTREAM vs ITEM vs COLLECTION vs COMMUNITY
            summaryFacets.add(facetTopTypes);

            //Top User IP's, probably Spiders
            summaryFacets.add(facetTopUniqueIP);

            summaryFacets.add(facetTopUniqueDNS);

            //Not needed on homepage
            summaryFacets.add(facetTopUSCities);


            summaryFacets.add(facetTopBitstreamsAllTime);
        }


        SearchRequestBuilder requestBuilder = facetedQueryBuilder(summaryFacets);
        SearchResponse resp = searchResponseToDRI(requestBuilder);

                // Top Downloads to Owning Object
        if(resp != null) {
            Facets responseFacets = resp.getFacets();
            if(responseFacets != null) {
                TermsFacet bitstreamsFacet = responseFacets.facet(TermsFacet.class, "top_bitstreams_lastmonth");
                addTermFacetToTable(bitstreamsFacet, division, "Bitstream", "Top File Downloads for " + getLastMonthString());
            }

        }
    }
    
    public AbstractFacetBuilder facetTopBitstreamsLastMonth() {
        Calendar calendar = Calendar.getInstance();

        // Show Previous Whole Month
        calendar.add(Calendar.MONTH, -1);

        calendar.set(Calendar.DAY_OF_MONTH, calendar.getActualMinimum(Calendar.DAY_OF_MONTH));
        String lowerBound = dateFormat.format(calendar.getTime());

        calendar.set(Calendar.DAY_OF_MONTH, calendar.getActualMaximum(Calendar.DAY_OF_MONTH));
        String upperBound = dateFormat.format(calendar.getTime());

        log.info("Lower:"+lowerBound+" -- Upper:"+upperBound);
        
        return FacetBuilders.termsFacet("top_bitstreams_lastmonth").field("id")
                .facetFilter(FilterBuilders.andFilter(
                        FilterBuilders.termFilter("type", "BITSTREAM"),
                        justOriginals,
                        FilterBuilders.rangeFilter("time").from(lowerBound).to(upperBound)
                ));
    }
    
    public String getLastMonthString() {
        Calendar calendar = Calendar.getInstance();
        // Show Previous Whole Month
        calendar.add(Calendar.MONTH, -1);

        calendar.set(Calendar.DAY_OF_MONTH, calendar.getActualMinimum(Calendar.DAY_OF_MONTH));
        return monthAndYearFormat.format(calendar.getTime());
    }
    
    public SearchRequestBuilder facetedQueryBuilder(AbstractFacetBuilder facet) throws WingException{
        List<AbstractFacetBuilder> facetList = new ArrayList<AbstractFacetBuilder>();
        facetList.add(facet);
        return facetedQueryBuilder(facetList);
    }

    public SearchRequestBuilder facetedQueryBuilder(AbstractFacetBuilder... facets) throws WingException {
        List<AbstractFacetBuilder> facetList = new ArrayList<AbstractFacetBuilder>();

        for(AbstractFacetBuilder facet : facets) {
            facetList.add(facet);
        }

        return facetedQueryBuilder(facetList);
    }
    
    public SearchRequestBuilder facetedQueryBuilder(List<AbstractFacetBuilder> facetList) {
        FilterBuilder dateRangeFilter = FilterBuilders.rangeFilter("time").from(dateStart).to(dateEnd);

        QueryBuilder containerScopeQuery;
        if(dso instanceof Collection || dso instanceof Community || dso instanceof Item) {
            containerScopeQuery = QueryBuilders.termQuery(getOwningText(dso), dso.getID());
        } else {
            //Need a no-op query to join.. All, as opposed to specify a owning-something
            containerScopeQuery = QueryBuilders.matchAllQuery();
        }

        FilteredQueryBuilder filteredQueryBuilder = QueryBuilders.filteredQuery(containerScopeQuery, dateRangeFilter);

        QueryBuilder scopedDatedDebottedQuery = QueryBuilders.boolQuery()
                .must(filteredQueryBuilder)
                .mustNot(QueryBuilders.termQuery("isBot", true))
                .mustNot(QueryBuilders.termQuery("isBotUA", true));

        SearchRequestBuilder searchRequestBuilder = client.prepareSearch(ElasticSearchLogger.getInstance().indexName)
                .setSearchType(SearchType.DFS_QUERY_THEN_FETCH)
                .setQuery(scopedDatedDebottedQuery)
                .setSize(0);

        for(AbstractFacetBuilder facet : facetList) {
            searchRequestBuilder.addFacet(facet);
        }

        return searchRequestBuilder;
    }

    public SearchResponse searchResponseToDRI(SearchRequestBuilder searchRequestBuilder) throws WingException{
        division.addHidden("request").setValue(searchRequestBuilder.toString());

        SearchResponse resp = searchRequestBuilder.execute().actionGet();

        if(resp == null) {
            log.info("Elastic Search is down for searching.");
            division.addPara("Elastic Search seems to be down :(");
            return null;
        }

        division.addHidden("response").setValue(resp.toString());
        division.addDivision("chart_div");

        return resp;
    }

    private void addTermFacetToTable(TermsFacet termsFacet, Division division, String termName, String tableHeader) throws WingException, SQLException {
        List<? extends TermsFacet.Entry> termsFacetEntries = termsFacet.getEntries();

        if(termName.equalsIgnoreCase("country")) {
            division.addDivision("chart_div_map");
        }

        Table facetTable = division.addTable("facet-"+termName, termsFacetEntries.size()+1, 10);
        facetTable.setHead(tableHeader);

        Row facetTableHeaderRow = facetTable.addRow(Row.ROLE_HEADER);
        if(termName.equalsIgnoreCase("bitstream")) {
            facetTableHeaderRow.addCellContent("Title");
            facetTableHeaderRow.addCellContent("Creator");
            facetTableHeaderRow.addCellContent("Publisher");
            facetTableHeaderRow.addCellContent("Date");
        } else {
            facetTableHeaderRow.addCell().addContent(termName);
        }

        facetTableHeaderRow.addCell().addContent("Count");

        if(termsFacetEntries.size() == 0) {
            facetTable.addRow().addCell().addContent("No Data Available");
            return;
        }

        for(TermsFacet.Entry facetEntry : termsFacetEntries) {
            Row row = facetTable.addRow();

            if(termName.equalsIgnoreCase("bitstream")) {
                Bitstream bitstream = Bitstream.find(context, Integer.parseInt(facetEntry.getTerm()));
                Item item = (Item) bitstream.getParentObject();
                row.addCell().addXref(contextPath + "/handle/" + item.getHandle(), item.getName());
                row.addCellContent(getFirstMetadataValue(item, "dc.creator"));
                row.addCellContent(getFirstMetadataValue(item, "dc.publisher"));
                row.addCellContent(getFirstMetadataValue(item, "dc.date.issued"));
            } else if(termName.equalsIgnoreCase("country")) {
                row.addCell("country", Cell.ROLE_DATA,"country").addContent(new Locale("en", facetEntry.getTerm()).getDisplayCountry());
            } else {
                row.addCell().addContent(facetEntry.getTerm());
            }
            row.addCell("count", Cell.ROLE_DATA, "count").addContent(facetEntry.getCount());
        }
    }

    private void addDateHistogramToTable(DateHistogramFacet monthlyDownloadsFacet, Division division, String termName, String termDescription) throws WingException {
        List<? extends DateHistogramFacet.Entry> monthlyFacetEntries = monthlyDownloadsFacet.getEntries();

        if(monthlyFacetEntries.size() == 0) {
            division.addPara("Empty result set for: "+termName);
            return;
        }

        Table monthlyTable = division.addTable(termName, monthlyFacetEntries.size(), 10);
        monthlyTable.setHead(termDescription);
        Row tableHeaderRow = monthlyTable.addRow(Row.ROLE_HEADER);
        tableHeaderRow.addCell("date", Cell.ROLE_HEADER,null).addContent("Month/Date");
        tableHeaderRow.addCell("count", Cell.ROLE_HEADER,null).addContent("Count");

        for(DateHistogramFacet.Entry histogramEntry : monthlyFacetEntries) {
            Row dataRow = monthlyTable.addRow();
            Date facetDate = new Date(histogramEntry.getTime());
            dataRow.addCell("date", Cell.ROLE_DATA,"date").addContent(dateFormat.format(facetDate));
            dataRow.addCell("count", Cell.ROLE_DATA,"count").addContent("" + histogramEntry.getCount());
        }
    }
    
    private String getOwningText(DSpaceObject dso) {
        switch (dso.getType()) {
            case Constants.ITEM:
                return "owningItem";
            case Constants.COLLECTION:
                return "owningColl";
            case Constants.COMMUNITY:
                return "owningComm";
            default:
                return "";
        }
    }
    
    private String getFirstMetadataValue(Item item, String metadataKey) {
        DCValue[] dcValue = item.getMetadata(metadataKey);
        if(dcValue.length > 0) {
            return dcValue[0].value;
        } else {
            return "";
        }
    }

    /**
     * Determine if this date-range is less than a months worth.
     * - days/true      - if dates are within same month
     * - days/true      - different months but dates are less than 28 days apart.
     * - months/false   - different months and 28 or more days apart.
     * @param dateBegin defaults to Minimum Date if null
     * @param dateEnd defaults to current date if null
     * @return
     */
    protected boolean lessThanMonthApart(Date dateBegin, Date dateEnd) {
        boolean monthOrMore = false;
        boolean days = true;

        if(dateBegin == null) {
            SimpleDateFormat dateFormatYMD = new SimpleDateFormat("yyyy-MM-dd");
            try {
                dateBegin = dateFormatYMD.parse(ReportGenerator.MINIMUM_DATE);

            } catch (ParseException e) {
                log.error(e.getMessage());
                return false;
            }
        }

        if(dateEnd == null) {
            dateEnd = new Date();
        }

        DateTime date1 = new DateTime(dateBegin);
        DateTime date2 = new DateTime(dateEnd);
        Interval interval = new Interval(date1, date2);
        Duration duration = new Duration(interval);

        log.info("JODA has " + date1 + " for " + dateBegin + " and " + date2 + " for " + dateEnd);

        if (dateBegin.getYear() == dateEnd.getYear() && dateBegin.getMonth() == dateEnd.getMonth()) {
            return days;
        } else if (duration.getStandardDays() < 28) {
            return days;
        } else {
            return monthOrMore;
        }
    }
}
