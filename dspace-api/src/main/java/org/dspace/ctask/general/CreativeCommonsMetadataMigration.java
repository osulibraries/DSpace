package org.dspace.ctask.general;

import org.apache.log4j.Logger;
import org.dspace.content.DSpaceObject;
import org.dspace.content.Item;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.curate.AbstractCurationTask;
import org.dspace.curate.Curator;
import org.dspace.curate.Distributive;
import org.dspace.license.CreativeCommons;
import org.dspace.storage.rdbms.DatabaseManager;
import org.dspace.storage.rdbms.TableRow;
import org.dspace.storage.rdbms.TableRowIterator;

import java.io.IOException;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A tool to migrate legacy items in DSpace with Creative Commons license to put their cc license information into the
 * Item-level metadata, as opposed to just hidden in the bitstream.
 * Author: Peter Dietz
 */
@Distributive
public class CreativeCommonsMetadataMigration extends AbstractCurationTask{
    private static Logger log = Logger.getLogger(CreativeCommonsMetadataMigration.class);

    private Map<String, Integer> licenseTable = new HashMap<String, Integer>();

    private static CreativeCommons.MdField uriField, nameField;

    private static final Map<String, String> licenseTextURIPairs = new HashMap<String, String>();
    static  {
        licenseTextURIPairs.put("http://creativecommons.org/licenses/by/2.0/", "Attribution 2.0 Generic");
        licenseTextURIPairs.put("http://creativecommons.org/licenses/by/2.5/", "Attribution 2.5 Generic");
        licenseTextURIPairs.put("http://creativecommons.org/licenses/by/3.0/", "Attribution 3.0 Unported");
        licenseTextURIPairs.put("http://creativecommons.org/licenses/by/3.0/us/", "Attribution 3.0 United States");
        licenseTextURIPairs.put("http://creativecommons.org/licenses/by-nc/2.0/", "Attribution-NonCommercial 2.0 Generic");
        licenseTextURIPairs.put("http://creativecommons.org/licenses/by-nc/2.0/", "Attribution-NonCommercial 2.0 Generic");
        licenseTextURIPairs.put("http://creativecommons.org/licenses/by-nc/2.5/", "Attribution-NonCommercial 2.5 Generic");
        licenseTextURIPairs.put("http://creativecommons.org/licenses/by-nc/3.0/", "Attribution-NonCommercial 3.0 Unported");
        licenseTextURIPairs.put("http://creativecommons.org/licenses/by-nc-nd/2.0/", "Attribution-NonCommercial-NoDerivs 2.0 Generic");
        licenseTextURIPairs.put("http://creativecommons.org/licenses/by-nc-nd/2.5/", "Attribution-NonCommercial-NoDerivs 2.5 Generic");
        licenseTextURIPairs.put("http://creativecommons.org/licenses/by-nc-nd/3.0/", "Attribution-NonCommercial-NoDerivs 3.0 Unported");
        licenseTextURIPairs.put("http://creativecommons.org/licenses/by-nc-nd/3.0/us/", "Attribution-NonCommercial-NoDerivs 3.0 United States");
        licenseTextURIPairs.put("http://creativecommons.org/licenses/by-nc-sa/2.0/", "Attribution-NonCommercial-ShareAlike 2.0 Generic");
        licenseTextURIPairs.put("http://creativecommons.org/licenses/by-nc-sa/2.5/", "Attribution-NonCommercial-ShareAlike 2.5 Generic");
        licenseTextURIPairs.put("http://creativecommons.org/licenses/by-nc-sa/3.0/", "Attribution-NonCommercial-ShareAlike 3.0 Unported");
        licenseTextURIPairs.put("http://creativecommons.org/licenses/by-nd/2.0/", "Attribution-NoDerivs 2.0 Generic");
        licenseTextURIPairs.put("http://creativecommons.org/licenses/by-nd/2.5/", "Attribution-NoDerivs 2.5 Generic");
        licenseTextURIPairs.put("http://creativecommons.org/licenses/by-nd/3.0/", "Attribution-NoDerivs 3.0 Unported");
        licenseTextURIPairs.put("http://creativecommons.org/licenses/by-nd/3.0/us/", "Attribution-NoDerivs 3.0 United States");
        licenseTextURIPairs.put("http://creativecommons.org/licenses/by-sa/2.0/", "Attribution-ShareAlike 2.0 Generic");
        licenseTextURIPairs.put("http://creativecommons.org/licenses/by-sa/2.5/", "Attribution-ShareAlike 2.5 Generic");
        licenseTextURIPairs.put("http://creativecommons.org/licenses/by-sa/3.0/", "Attribution-ShareAlike 3.0 Unported");
        licenseTextURIPairs.put("http://creativecommons.org/licenses/publicdomain/", "Public Domain Certification");
        licenseTextURIPairs.put("http://creativecommons.org/publicdomain/mark/1.0/", "Public Domain Mark 1.0");
        licenseTextURIPairs.put("http://creativecommons.org/publicdomain/zero/1.0/", "CC0 1.0 Universal");
    }

    @Override
    public int perform(DSpaceObject dso) throws IOException {
        uriField = CreativeCommons.getCCField("uri") ;
        nameField = CreativeCommons.getCCField("name");
        log.info("uriField toString is:[" + uriField + "]");
        log.info("nameField toString is:[" + nameField + "]");

        licenseTable.clear();

        if(dso.getType() == Constants.SITE) {
            //Use an optimized algorithm for hitting all remaining items when done at site level.
            distributeToUnperformedItems();
        } else {
            //When selectively performing this task, recurse just through this subset.
            distribute(dso);
        }



        //context.complete();
        formatResults();
        return Curator.CURATE_SUCCESS;

    }

    @Override
    protected void performItem(Item item)
    {
        try
        {
            if(uriField.ccItemValue(item) != null && nameField.ccItemValue(item) != null) {
                //Fail really fast if we already have metadata values set.
                log.info(item.getHandle() + " -- We didn't bother to look into the Item since it already had CC rights values set. (failfast)");
                return;
            }

            if(!CreativeCommons.hasLicense(curator.curationContext(), item))
            {
                Integer count = licenseTable.get("None");
                count = (count == null) ? 1 : count+1;
                licenseTable.put("None", count);

                return;
            }

            String licenseURL = CreativeCommons.getLicenseURL(item);
            String licenseName = licenseTextURIPairs.get(licenseURL);

            Integer count = licenseTable.get(licenseURL);
            count = (count == null) ? 1 : count+1;
            licenseTable.put(licenseURL, count);

            boolean haveWeChangedAnything = false;

            if(uriField.ccItemValue(item) == null) {
                uriField.addItemValue(item, licenseURL);
                haveWeChangedAnything = true;
            }

            if(nameField.ccItemValue(item) == null) {
                nameField.addItemValue(item, licenseName);
                haveWeChangedAnything = true;
            }

            if(haveWeChangedAnything) {
                item.update();
                log.info(item.getHandle() + " -- Alter CC Metadata -- " + uriField + ":"  + licenseURL + " -- " + nameField + ":" + licenseName);
            }

            //Probably very expensive to do this each time.
            curator.curationContext().commit();

        } catch (Exception e) {
            log.error("ERROR: " + item.getHandle() + " -- " + e.getMessage());
            Integer count = licenseTable.get("Error");
            count = (count == null) ? 1 : count+1;
            licenseTable.put("Error", count);
        }
    }

    private void formatResults() {
        log.info("RESULTS OF CreativeCommonsMetadataMigration on set.");
        log.info("===================================================");
        log.info(licenseTable.toString());
        log.info("===================================================");
    }

    /*
    An alternate method to have the Database query the remaining DSpace Items, as opposed to recursive scanning EVERYTHING
     */
    private void distributeToUnperformedItems() throws IOException {
        try {
            Context context = curator.curationContext();

            String unmigratedItemsQuery = "SELECT \n" +
                    "  distinct(item.item_id) \n" +
                    "FROM \n" +
                    "  public.item, \n" +
                    "  public.item2bundle, \n" +
                    "  public.bundle\n" +
                    "WHERE \n" +
                    "  item.item_id = item2bundle.item_id AND\n" +
                    "  item2bundle.bundle_id = bundle.bundle_id AND\n" +
                    "  bundle.name = 'CC-LICENSE' AND\n" +
                    "  item.item_id NOT IN ( \n" +
                    "    select metadatavalue.item_id \n" +
                    "    from \n" +
                    "      public.metadatafieldregistry, \n" +
                    "      public.metadatavalue\n" +
                    "    where \n" +
                    "      metadatavalue.metadata_field_id = metadatafieldregistry.metadata_field_id AND\n" +
                    "      metadatafieldregistry.element = 'rights' AND \n" +
                    "      metadatafieldregistry.qualifier = 'ccuri'\n" +
                    "    );";

            TableRowIterator itemsTri = DatabaseManager.query(context, unmigratedItemsQuery);
            List<TableRow> itemRowList = itemsTri.toList();
            log.info("distributeToUnperformedItems found: " + itemRowList.size() + " items needing to be performed.");

            for(TableRow itemRow : itemRowList) {
                Integer itemID = itemRow.getIntColumn("item_id");
                Item item = Item.find(context, itemID);
                performItem(item);
            }
        } catch (SQLException e) {
            log.error(e.getMessage());
        }
    }


}
