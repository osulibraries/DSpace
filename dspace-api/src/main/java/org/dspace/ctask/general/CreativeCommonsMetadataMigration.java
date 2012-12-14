package org.dspace.ctask.general;

import org.apache.log4j.Logger;
import org.dspace.content.DSpaceObject;
import org.dspace.content.Item;
import org.dspace.core.Context;
import org.dspace.curate.AbstractCurationTask;
import org.dspace.curate.Curator;
import org.dspace.curate.Distributive;
import org.dspace.license.CreativeCommons;

import java.io.IOException;
import java.sql.SQLException;
import java.util.HashMap;
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
    private Context context;

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
        licenseTable.clear();
        try {
            context = new Context();


            distribute(dso);
            formatResults();
            return Curator.CURATE_SUCCESS;

        } catch (SQLException e) {
            log.error("SQL error");
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
            return Curator.CURATE_ERROR;
        }
    }

    @Override
    protected void performItem(Item item)
    {
        try
        {
            if(!CreativeCommons.hasLicense(context, item))
            {
                Integer count = licenseTable.get("None");
                count = (count == null) ? 1 : count+1;
                licenseTable.put("None", count);

                return;
            }


            String licenseURL = CreativeCommons.getLicenseURL(item);
            Integer count = licenseTable.get(licenseURL);
            count = (count == null) ? 1 : count+1;
            licenseTable.put(licenseURL, count);

            log.info(item.getHandle() + " -- CC LicenseText of: " + licenseURL);





        } catch (Exception e) {
            log.error(e.getMessage());
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


}
