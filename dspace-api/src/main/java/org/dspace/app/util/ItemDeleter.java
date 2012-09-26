package org.dspace.app.util;

import org.apache.log4j.Logger;
import org.dspace.content.Item;
import org.dspace.core.Context;

import java.sql.SQLException;

/**
 * Created by IntelliJ IDEA.
 * User: peterdietz
 * Date: 2/1/12
 * Time: 1:35 PM
 * To change this template use File | Settings | File Templates.
 */
public class ItemDeleter {
    private static Logger log = Logger.getLogger(ItemDeleter.class);

    public static void main(String[] argv) throws SQLException {
        log.info("Command Line Item Deletion");
        Context context = new Context();
        for(int i=0; i < argv.length; i++) {
            Item item = Item.find(context, Integer.parseInt(argv[i]));
            log.info("I would delete itemID:"+argv[i]+" with title:"+item.getName());
        }
                
        
        
        
    }
}
