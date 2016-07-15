/*
 * DashboardViewer.java
 *
 * Version: $Revision$
 *
 * Date: $Date$
 *
 * Copyright (c) 2002, Hewlett-Packard Company and Massachusetts
 * Institute of Technology.  All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *
 * - Redistributions of source code must retain the above copyright
 * notice, this list of conditions and the following disclaimer.
 *
 * - Redistributions in binary form must reproduce the above copyright
 * notice, this list of conditions and the following disclaimer in the
 * documentation and/or other materials provided with the distribution.
 *
 * - Neither the name of the Hewlett-Packard Company nor the name of the
 * Massachusetts Institute of Technology nor the names of their
 * contributors may be used to endorse or promote products derived from
 * this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * ``AS IS'' AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * HOLDERS OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 * BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS
 * OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR
 * TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE
 * USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH
 * DAMAGE.
 */

package org.dspace.app.xmlui.aspect.dashboard;

import org.apache.cocoon.environment.ObjectModelHelper;
import org.apache.cocoon.environment.Request;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.apache.solr.client.solrj.SolrServerException;
import org.dspace.app.xmlui.cocoon.AbstractDSpaceTransformer;
import org.dspace.app.xmlui.utils.UIException;
import org.dspace.app.xmlui.wing.WingException;
import org.dspace.app.xmlui.wing.element.*;
import org.dspace.app.xmlui.wing.element.Item;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.*;
import org.dspace.core.Constants;
import org.dspace.eperson.EPerson;
import org.dspace.statistics.ObjectCount;
import org.dspace.statistics.SolrLogger;
import org.dspace.storage.rdbms.DatabaseManager;
import org.dspace.storage.rdbms.TableRow;
import org.dspace.storage.rdbms.TableRowIterator;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;


/**
 * Display a dashboard of information about the site.
 *
 *
 * @author Peter Dietz
 */
public class DashboardViewer extends AbstractDSpaceTransformer
{
    private static Logger log = Logger.getLogger(DashboardViewer.class);

    /**
     * Add a page title and trail links.
     */
    public void addPageMeta(PageMeta pageMeta) throws SAXException,
            WingException, UIException, SQLException, IOException,
            AuthorizeException
    {
        // Set the page title
        pageMeta.addMetadata("title").addContent("Dashboard");

        pageMeta.addTrailLink(contextPath + "/","KB Home");
        pageMeta.addTrailLink(contextPath + "/dashboard", "Dashboard");
    }

    /**
     * Add a community-browser division that includes refrences to community and
     * collection metadata.
     */
    public void addBody(Body body) throws SAXException, WingException,
            UIException, SQLException, IOException, AuthorizeException {
        Division division = body.addDivision("dashboard", "primary");
        division.setHead("Dashboard");
        division.addPara("A collection of statistical queries about the size and traffic of the KB.");

        Division search = body.addInteractiveDivision("choose-report", contextPath+"/dashboard", Division.METHOD_GET, "primary");
        search.setHead("Statistical Report Generation");
        org.dspace.app.xmlui.wing.element.List actionsList = search.addList("actions", "form");
        actionsList.addLabel("Label for action list");
        Item actionSelectItem = actionsList.addItem();
        Radio actionSelect = actionSelectItem.addRadio("report_name");
        actionSelect.setLabel("Choose a Report to View");
        actionSelect.addOption(false, "itemgrowth", "Number of Items in the Repository (Monthly) -- Google Chart");
        actionSelect.addOption(false, "commitems", "Items in Communities");
        actionSelect.addOption(false, "topDownloadsMonth", "Top Downloads for Month");
        actionSelect.addOption(false, "submissionsByUser", "Submissions by Cheryl to OSUL Research");
        actionSelect.addOption(false, "creativeCommonsError", "Has Creative Commons Bundle, but no metadata.");

        Para buttons = search.addPara();
        buttons.addButton("submit_add").setValue("Create Report");

        Request request = ObjectModelHelper.getRequest(objectModel);
        String reportName = request.getParameter("report_name");

        if (StringUtils.isEmpty(reportName)) {
            reportName = "";
        }

        if(reportName.equals("itemgrowth"))
        {
            queryItemGrowthPerMonth(division);
        } else if(reportName.equals("commitems"))
        {
            queryNumberOfItemsPerComm(division);
        } else if (reportName.equals("topDownloadsMonth"))
        {
            addMonthlyTopDownloads(division);
        } else if (reportName.equals("submissionsByUser"))
        {
            querySubmissionsByUser(division);
        } else if (reportName.equals("creativeCommonsError"))
        {
            addCCBundlesWithoutLicenseMetadata(division);
        }


        Division exportLinks = division.addDivision("export-links");
        exportLinks.setHead("Additional Reports / Exports");
        exportLinks.addPara("Additional reports or queries that have been frequently run against the KB. Typically these reports are a .csv export of some query. " +
                "Instead of manually requesting, and then waiting for someone to execute the query, this is a bit of a self-service shortcut.");

        org.dspace.app.xmlui.wing.element.List links = exportLinks.addList("links");
        links.addItemXref(contextPath + "/growth-statistics?type="+ Constants.ITEM, "Growth - Number of Items added to the Repository (Monthly)");
        links.addItemXref(contextPath + "/growth-statistics?type="+ Constants.BITSTREAM, "Growth - Number of Bitstreams added to the Repository (Monthly)");
        links.addItemXref(contextPath + "/content-statistics",  "Size Totals #(Comms, Coll, Items, Bits, GBs)");
        links.addItemXref(contextPath + "/collection-info", "Collection List - Name, ID, Handle, #Items");
        links.addItemXref(contextPath + "/community-info", "Community List - Name, ID, Handle, #Items");
        links.addItemXref(contextPath + "/hierarchy-info", "Site Hierarchy - Comm > ... > Coll, #Items, #Bits, #Views, #Downloads");
    }

    /**
     * Adds Google charts visualizer of items in repository. It has a hidden table with the data too.
     * @param division
     * @throws SQLException
     * @throws WingException
     */
    private void queryItemGrowthPerMonth(Division division) throws SQLException, WingException
    {
        String query = "SELECT to_char(date_trunc('month', t1.ts), 'YYYY-MM') AS yearmo, count(*) as countitem " +
            "FROM ( SELECT to_timestamp(text_value, 'YYYY-MM-DD') AS ts FROM metadatavalue, item " +
            "WHERE metadata_field_id = 12 AND metadatavalue.resource_id = item.item_id AND metadatavalue.resource_type_id = 2 and item.in_archive=true	) t1 " +
            "GROUP BY date_trunc('month', t1.ts) order by yearmo asc;";
        TableRowIterator tri = DatabaseManager.query(context, query);
        List itemStatRows = tri.toList();

        division.addDivision("chart_div");

        Division descriptionDivision = division.addDivision("description");
        descriptionDivision.addPara().addXref(contextPath + "/growth-statistics", "Download This Dataset as CSV");

        Table itemTable = division.addTable("items_added_monthly", itemStatRows.size(), 3);
        Row headerRow = itemTable.addRow(Row.ROLE_HEADER);
        headerRow.addCell().addContent("Date");
        headerRow.addCell().addContent("#Items Added");
        headerRow.addCell().addContent("Total #Items");
        Integer totalItems = 0;

        for(int i=0; i<itemStatRows.size();i++)
        {
            TableRow row = (TableRow) itemStatRows.get(i);
            //log.debug(row.toString());
            String date = row.getStringColumn("yearmo");
            Long numItems = row.getLongColumn("countitem");
            totalItems += numItems.intValue();
            Row dataRow = itemTable.addRow();
            dataRow.addCell("date", Cell.ROLE_DATA,null).addContent(date);
            dataRow.addCell("items_added", Cell.ROLE_DATA,null).addContent(numItems.intValue());
            dataRow.addCell("items_total", Cell.ROLE_DATA, null).addContent(totalItems);
        }

    }

    private void querySubmissionsByUser(Division division) throws SQLException, WingException, AuthorizeException
    {
        Collection osulResearchCollection = Collection.find(context, 23);
        context.ignoreAuthorization();
        EPerson cheryl = EPerson.findByEmail(context, "obong.1@osu.edu");
        ItemIterator itemIterator = osulResearchCollection.getAllItems();

        List<org.dspace.content.Item> cherylsItems = new ArrayList<org.dspace.content.Item>();
        
        while(itemIterator.hasNext()) {
            org.dspace.content.Item item = itemIterator.next();
            if(item.getSubmitter().equals(cheryl)) {
                cherylsItems.add(item);
            }
        }
        
        Table table = division.addTable("submissionsByUser", cherylsItems.size(), 4);
        table.setHead("Items Submitted to "+osulResearchCollection.getName() + " by " + cheryl.getName());
        Row headerRow = table.addRow(Row.ROLE_HEADER);
        headerRow.addCellContent("Title");
        headerRow.addCellContent("Author");
        headerRow.addCellContent("Date Issued");
        headerRow.addCellContent("Date Accessioned");
        
        for(org.dspace.content.Item item : cherylsItems) {
            Row bodyRow = table.addRow(Row.ROLE_DATA);
            bodyRow.addCell().addXref(contextPath + "/handle/" + item.getHandle(), item.getName());
            bodyRow.addCellContent(item.getMetadata("dc.creator"));
            bodyRow.addCellContent(item.getMetadata("dc.date.issued"));
            bodyRow.addCellContent(item.getMetadata("dc.date.accessioned"));
        }
        
        
        
        
    }

    /**
     * In monthly intervals, find out the number that were in the KB.
     */
    private void queryNumberOfItemsPerComm(Division division) throws SQLException, WingException
    {
        String query = "SELECT to_char(date_trunc('month', t1.ts), 'YYYY-MM') AS yearmo, community_id," +
            "count(*) as numitems FROM 	(	SELECT to_timestamp(text_value, 'YYYY-MM-DD') AS ts, community2item.community_id " +
            "FROM metadatavalue, community2item, item	WHERE metadata_field_id = 12 AND community2item.item_id = metadatavalue.resource_id and metadatavalue.resource_type_id = 2 " +
            "AND metadatavalue.resource_id = item.item_id AND item.in_archive=true 	) t1 GROUP BY date_trunc('month', t1.ts), " +
            "community_id order by community_id asc, yearmo desc;";
        TableRowIterator tri = DatabaseManager.query(context, query);
        List itemStatRows = tri.toList();
        
        Table itemTable = division.addTable("items_added_per_comm", itemStatRows.size(), 3);
        Row headerRow = itemTable.addRow(Row.ROLE_HEADER);
        headerRow.addCell().addContent("YearMonth");
        headerRow.addCell().addContent("community_id");
        headerRow.addCell().addContent("num items");

        for(int i=0; i<itemStatRows.size();i++)
        {
            TableRow row = (TableRow) itemStatRows.get(i);
            log.debug(row.toString());
            String date = row.getStringColumn("yearmo");
            Integer community_id = row.getIntColumn("community_id");
            Long numItems = row.getLongColumn("numitems");
            Row dataRow = itemTable.addRow();
            dataRow.addCell().addContent(date);
            dataRow.addCell().addContent(community_id);

            dataRow.addCell().addContent(numItems.intValue());
        }
    }

    public void addMonthlyTopDownloads(Division division) throws WingException {
        Request request = ObjectModelHelper.getRequest(objectModel);
        String yearMonth = request.getParameter("yearMonth");
        Calendar calendar;
        if (StringUtils.isNotEmpty(yearMonth)) {
            // User Specified A Month   2011-08
            // Human years are something like 2005, ... same as computer
            // Human months are 1-12, computer months are 0-11. So we need to decrement input by 1.
            String[] dateChunk = yearMonth.split("-");
            Integer yearInput = Integer.valueOf(dateChunk[0]);
            Integer monthInput = Integer.valueOf(dateChunk[1])-1;
            calendar = new GregorianCalendar(yearInput, monthInput, 1);
        } else {
            // Show Previous Whole Month
            calendar = Calendar.getInstance();
            calendar.add(Calendar.MONTH, -1);
        }

        Integer humanMonthNumber = calendar.get(Calendar.MONTH)+1;

        // 2011-08-01T00:00:00.000Z TO 2011-08-31T23:59:59.999Z
        String monthRange = calendar.get(Calendar.YEAR) + "-" + humanMonthNumber + "-" + "01"                                               + "T00:00:00.000Z"
                 + " TO " + calendar.get(Calendar.YEAR) + "-" + humanMonthNumber + "-" + calendar.getActualMaximum(Calendar.DAY_OF_MONTH)   + "T23:59:59.999Z";

        String query = "type:0 AND owningComm:[0 TO 9999999] AND -dns:msnbot-* AND -isBot:true AND time:["+monthRange+"]";
        log.info("Top Downloads Query: "+query);
        ObjectCount[] objectCounts = new ObjectCount[0];
        try {
            objectCounts = SolrLogger.queryFacetField(query, "", "id", 50, true, null);

        } catch (SolrServerException e) {
            log.error("Top Downloads query failed.");
            log.error(e.getMessage());  //To change body of catch statement use File | Settings | File Templates.
        }

        Division downloadsDivision = division.addDivision("top-downloads", "primary");
        downloadsDivision.setHead("Top Bitstream Downloads for Month");
        downloadsDivision.addPara("The Top 50 Bitstream Downloads during the month of "+calendar.getDisplayName(Calendar.MONTH, Calendar.LONG, context.getCurrentLocale())+" "+calendar.get(Calendar.YEAR)+".");


        // Bitstream  | Bundle | Item Title | Collection Name | Number of Hits |

        Table table = downloadsDivision.addTable("topDownloads",objectCounts.length +1, 2);
        Row header = table.addRow(Row.ROLE_HEADER);
        header.addCell().addContent("Bitstream");
        header.addCell().addContent("Bundle");
        header.addCell().addContent("Item");
        header.addCell().addContent("Collection");
        header.addCell().addContent("Number of Hits");

        for(int i=0; i< objectCounts.length; i++)
        {
            Row row = table.addRow(Row.ROLE_DATA);
            Cell bitstreamCell = row.addCell();
            Cell bundleCell = row.addCell();
            Cell itemCell = row.addCell();
            Cell collectionCell = row.addCell();
            Cell hitsCell = row.addCell();

            String objectValue = objectCounts[i].getValue();
            if(objectValue.equals("total")) {
                bitstreamCell.addContent(objectValue);
            } else {
                Integer bitstreamID = Integer.parseInt(objectCounts[i].getValue());
                try {
                    Bitstream bitstream = Bitstream.find(context, bitstreamID);
                    bitstream.getName().length();
                    bitstreamCell.addXref(contextPath + "/bitstream/id/" + bitstreamID + "/" + bitstream.getName(), StringUtils.abbreviate(bitstream.getName(), 50));

                    Bundle[] bundles = bitstream.getBundles();
                    if(bundles != null && bundles.length > 0) {
                        Bundle bundle = bundles[0];
                        bundleCell.addContent(bundle.getName());

                        org.dspace.content.Item item = bundle.getItems()[0];
                        itemCell.addXref(contextPath + "/handle/" + item.getHandle(), StringUtils.abbreviate(item.getName(), 47));
                        Collection collection = item.getOwningCollection();
                        collectionCell.addXref(contextPath + "/handle/" + collection.getHandle(), StringUtils.abbreviate(collection.getName(), 47));
                    }
                } catch (SQLException e) {
                    log.error(e.getMessage());  //To change body of catch statement use File | Settings | File Templates.
                    bitstreamCell.addContent(bitstreamID);
                }
            }
            hitsCell.addContent((int) objectCounts[i].getCount());
        }
    }

    /*
    Look for Items in a weird state where they have a CC-LICENSE bundle, but no CC metadata.
     */
    public void addCCBundlesWithoutLicenseMetadata(Division division) throws SQLException, WingException {
        String query = "SELECT " +
                "  distinct(item.item_id) " +
                "FROM \n" +
                "  public.item, " +
                "  public.item2bundle, " +
                "  public.bundle, metadatavalue " +
                "WHERE \n" +
                "  item.item_id = item2bundle.item_id AND\n" +
                "  item2bundle.bundle_id = bundle.bundle_id AND " +
                "  metadatavalue.resource_type_id = 1 and metadatavalue.resource_id = bundle.bundle_id" +
                " and metadatavalue.metadata_field_id = 64 and metadatavalue.text_value = 'CC-LICENSE';";

        TableRowIterator tri = DatabaseManager.query(context, query);

        List<org.dspace.content.Item> itemsInWeirdState = new ArrayList<org.dspace.content.Item>();

        List<TableRow> ccItems = tri.toList();
        for(TableRow itemRow : ccItems) {
            Integer itemID = itemRow.getIntColumn("item_id");
            org.dspace.content.Item item = org.dspace.content.Item.find(context, itemID);
            Metadatum[] ccMetadatas = item.getMetadataByMetadataString("dc.rights.ccuri");

            //Look for items with CC bundle, but no metadata
            if(ccMetadatas.length == 0) {
                itemsInWeirdState.add(item);
            }
        }

        Table table = division.addTable("weirdCCItems", itemsInWeirdState.size() + 1, 3);
        table.setHead("Weird Items with Creative Commons bundle, but no CC Metadata.");

        Row header = table.addRow(Row.ROLE_HEADER);
        header.addCellContent("Title");
        header.addCellContent("ItemID");
        header.addCellContent("Collection");
        header.addCellContent("Submitter");


        for(org.dspace.content.Item weirdItem : itemsInWeirdState) {
            Row body = table.addRow();

            //Title
            body.addCell().addXref(contextPath + "/handle/" + weirdItem.getHandle(), weirdItem.getName());

            //ItemID
            body.addCellContent(weirdItem.getID() + "");

            //Collection
            Collection[] collections = weirdItem.getCollections();
            if(collections != null && collections.length > 0) {
                body.addCell().addXref(contextPath + "/handle/" + collections[0].getHandle(), collections[0].getName());
            } else {
                body.addCellContent("No Collection");
            }

            //Submitter
            EPerson submitter = weirdItem.getSubmitter();
            body.addCellContent(submitter.getFullName());
        }

        tri.close();
    }

}
