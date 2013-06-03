/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content;

import org.apache.log4j.Logger;
import org.dspace.authorize.AuthorizeException;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.handle.HandleManager;
import org.dspace.storage.rdbms.DatabaseManager;
import org.dspace.storage.rdbms.TableRow;

import java.io.IOException;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Represents the root of the DSpace Archive.
 * By default, the handle suffix "0" represents the Site, e.g. "1721.1/0"
 */
public class Site extends DSpaceObject
{
    /** log4j */
    private static Logger log = Logger.getLogger(Site.class);

    /** Our context */
    private Context ourContext;

    /** "database" identifier of the site */
    public static final int SITE_ID = 0;

    // cache for Handle that is persistent ID for entire site.
    private static String handle = null;

    private static Site theSite = null;

    Site(Context context) {
        this.ourContext = context;
    }

    /**
     * Get the type of this object, found in Constants
     *
     * @return type of the object
     */
    public int getType()
    {
        return Constants.SITE;
    }

    /**
     * Get the internal ID (database primary key) of this object
     *
     * @return internal ID of object
     */
    public int getID()
    {
        return SITE_ID;
    }

    /**
     * Get the Handle of the object. This may return <code>null</code>
     *
     * @return Handle of the object, or <code>null</code> if it doesn't have
     *         one
     */
    public String getHandle()
    {
        return getSiteHandle();
    }

    /**
     * Static method to return site Handle without creating a Site.
     * @return handle of the Site.
     */
    public static String getSiteHandle()
    {
        if (handle == null)
        {
            handle = HandleManager.getPrefix() + "/" + String.valueOf(SITE_ID);
        }
        return handle;
    }

    /**
     * Get Site object corresponding to db id (which is ignored).
     * @param context the context.
     * @param id integer database id, ignored.
     * @return Site object.
     */
    public static DSpaceObject find(Context context, int id)
        throws SQLException
    {
        if (theSite == null)
        {
            theSite = new Site(context);
        }
        return theSite;
    }

    void delete()
        throws SQLException, AuthorizeException, IOException
    {
    }

    public void update()
        throws SQLException, AuthorizeException, IOException
    {
    }

    public String getName()
    {
        return ConfigurationManager.getProperty("dspace.name");
    }

    public String getURL()
    {
        return ConfigurationManager.getProperty("dspace.url");
    }

    /**
     * How many Items are in the Site
     * Only counts Items that are in_archive, and not withdrawn
     *
     * @return int total items
     */
    public int countItems()
            throws SQLException
    {
        int itemcount = 0;
        PreparedStatement statement = null;
        ResultSet rs = null;

        try
        {
            String query = "SELECT count(*) FROM item WHERE item.in_archive = true AND item.withdrawn = false;";

            statement = ourContext.getDBConnection().prepareStatement(query);

            rs = statement.executeQuery();
            if (rs != null)
            {
                rs.next();
                itemcount = rs.getInt(1);
            }
        }
        finally
        {
            if (rs != null)
            {
                try { rs.close(); } catch (SQLException sqle) { }
            }

            if (statement != null)
            {
                try { statement.close(); } catch (SQLException sqle) { }
            }
        }

        return itemcount;
    }

    /**
     * counts items in this collection
     *
     * @return  total items
     */
    public Integer countItemsBeforeDate(String date)
    {
        String query = "SELECT count(*)::integer as count FROM item, metadatavalue WHERE "
                + " in_archive = true AND item.withdrawn=false "
                + "AND metadatavalue.item_id = item.item_id "
                + "AND metadatavalue.metadata_field_id = 12 "
                + "AND metadatavalue.text_value < '"+date+"' ";

        try {
            log.info(query + this.getID() + date);
            TableRow row = DatabaseManager.querySingle(ourContext, query);
            log.info("Query happened.");
            return row.getIntColumn("count");

        } catch (Exception e) {
            log.error("Error getting countItemsBeforeDate: " + e.getMessage());
            return null;
        }
    }

    /**
     * Determine how many bitstreams are contained within this collection.
     * @param bundleName (Optional) Specify a bundle name to restrict the search to just those within this bundle
     * @return Number of bitstreams in this collection
     */
    public int countBitstreams(String bundleName) {
        String query = "SELECT count(*) FROM public.item2bundle,public.bundle2bitstream, public.bitstream,public.bundle, public.item" +
                " WHERE item2bundle.bundle_id = bundle2bitstream.bundle_id" +
                " AND item2bundle.item_id = item.item_id" +
                " AND item.in_archive = true" +
                " AND item.withdrawn = false" +
                " AND bundle2bitstream.bitstream_id = bitstream.bitstream_id" +
                " AND bundle.bundle_id = item2bundle.bundle_id";


        // If bundle is specified, then we need to limit our search to just those within the bundle.
        if (bundleName.length() > 0) {
            query = query.concat(" AND bundle.\"name\" = ?");
        }

        int bitstreamCount = 0;
        PreparedStatement statement = null;
        ResultSet rs = null;

        try {
            statement = ourContext.getDBConnection().prepareStatement(query);
            log.info("Query: "+query);
            if(bundleName.length() > 0) {
                statement.setString(1, bundleName);
            }
            log.info("Query2: "+query);

            rs = statement.executeQuery();
            if (rs != null) {
                rs.next();
                bitstreamCount = rs.getInt(1);
            }
        } catch (SQLException sqlE) {
            log.error(sqlE.getMessage());
        } catch (Exception e) {
            log.error(e.getMessage());

        } finally
        {
            if (rs != null) {
                try {
                    rs.close();
                } catch (SQLException sqle) {
                }
            }

            if (statement != null) {
                try {
                    statement.close();
                } catch (SQLException sqle) {
                }
            }
        }
        return bitstreamCount;
    }

    /**
     * Determine how many bitstreams are contained within this collection.
     * @param bundleName (Optional) Specify a bundle name to restrict the search to just those within this bundle
     * @return Number of bitstreams in this collection
     */
    public int countBitstreamsBeforeDate(String bundleName, String date) {
        String query = "SELECT count(*) FROM public.item2bundle,public.bundle2bitstream, public.bitstream,public.bundle, public.item, public.metadatavalue" +
                " WHERE item2bundle.bundle_id = bundle2bitstream.bundle_id" +
                " AND item2bundle.item_id = item.item_id" +
                " AND item.in_archive = true" +
                " AND item.withdrawn = false" +
                " AND bundle2bitstream.bitstream_id = bitstream.bitstream_id" +
                " AND bundle.bundle_id = item2bundle.bundle_id" +
                " AND metadatavalue.item_id = item.item_id " +
                " AND metadatavalue.metadata_field_id = 12 " +
                " AND metadatavalue.text_value < '"+date+"' ";




        // If bundle is specified, then we need to limit our search to just those within the bundle.
        if (bundleName.length() > 0) {
            query = query.concat(" AND bundle.\"name\" = ?");
        }

        int bitstreamCount = 0;
        PreparedStatement statement = null;
        ResultSet rs = null;

        try {
            statement = ourContext.getDBConnection().prepareStatement(query);
            log.info("Query: "+query);
            if(bundleName.length() > 0) {
                statement.setString(1, bundleName);
            }
            log.info("Query2: "+query);

            rs = statement.executeQuery();
            if (rs != null) {
                rs.next();
                bitstreamCount = rs.getInt(1);
            }
        } catch (SQLException sqlE) {
            log.error(sqlE.getMessage());
        } catch (Exception e) {
            log.error(e.getMessage());

        } finally
        {
            if (rs != null) {
                try {
                    rs.close();
                } catch (SQLException sqle) {
                }
            }

            if (statement != null) {
                try {
                    statement.close();
                } catch (SQLException sqle) {
                }
            }
        }
        return bitstreamCount;
    }
}
