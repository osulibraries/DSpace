package org.dspace.ctask.general;

import org.apache.log4j.Logger;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.DSpaceObject;
import org.dspace.content.Item;
import org.dspace.curate.AbstractCurationTask;
import org.dspace.curate.Curator;
import org.dspace.license.CreativeCommons;

import java.io.IOException;
import java.sql.SQLException;

/**
 * A tool to migrate legacy items in DSpace with Creative Commons license to put their cc license information into the
 * Item-level metadata, as opposed to just hidden in the bitstream.
 * Author: Peter Dietz
 */
public class CreativeCommonsMetadataMigration extends AbstractCurationTask{
    private static Logger log = Logger.getLogger(CreativeCommonsMetadataMigration.class);

    @Override
    public int perform(DSpaceObject dso) throws IOException {
        if( !(dso instanceof Item) ) {
            return Curator.CURATE_UNSET;
        }

        Item item = (Item) dso;
        try {


            String licenseText = CreativeCommons.getLicenseURL(item);

            log.info(item.getHandle() + " -- " + item.getName() + " -- CC LicenseText of: " + licenseText);





        } catch (SQLException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        } catch (AuthorizeException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }
        return 0;  //To change body of implemented methods use File | Settings | File Templates.
    }
}
