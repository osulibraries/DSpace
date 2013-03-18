/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.xmlui.utils;

import org.apache.cocoon.environment.ObjectModelHelper;
import org.apache.cocoon.environment.Request;
import org.dspace.app.xmlui.wing.Message;
import org.dspace.app.xmlui.wing.WingException;
import org.dspace.app.xmlui.wing.element.PageMeta;
import org.dspace.content.*;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.handle.HandleManager;

import java.sql.SQLException;
import java.util.Map;
import java.util.Stack;

/**
 * Simple utility class for extracting handles.
 * 
 * @author Scott Phillips
 */

public class HandleUtil
{

    /** The URL prefix of all handles */
    protected static final String HANDLE_PREFIX = "handle/";

    protected static final String DSPACE_OBJECT = "dspace.object";

    /**
     * Obtain the current DSpace handle for the specified request.
     * 
     * @param objectModel
     *            The cocoon model.
     * @return A DSpace handle, or null if none found.
     */
    public static DSpaceObject obtainHandle(Map objectModel)
            throws SQLException
    {
        Request request = ObjectModelHelper.getRequest(objectModel);

        DSpaceObject dso = (DSpaceObject) request.getAttribute(DSPACE_OBJECT);

        if (dso == null)
        {
            String uri = request.getSitemapURI();

            if (!uri.startsWith(HANDLE_PREFIX))
            {
                // Dosn't start with the prefix then no match
                return null;
            }

            String handle = uri.substring(HANDLE_PREFIX.length());

            int firstSlash = handle.indexOf('/');
            if (firstSlash < 0)
            {
                // If there is no first slash then no match
                return null;
            }

            int secondSlash = handle.indexOf('/', firstSlash + 1);
            if (secondSlash < 0)
            {
                // A trailing slash is not nesssary if there is nothing after
                // the handle.
                secondSlash = handle.length();
            }

            handle = handle.substring(0, secondSlash);

            Context context = ContextUtil.obtainContext(objectModel);
            dso = HandleManager.resolveToObject(context, handle);

            request.setAttribute(DSPACE_OBJECT, dso);
        }

        return dso;
    }

    /**
     * Determine if the given DSO is an ancestor of the the parent handle.
     * 
     * @param dso
     *            The child DSO object.
     * @param parent
     *            The Handle to test against.
     * @return The matched DSO object or null if none found.
     */
    public static boolean inheritsFrom(DSpaceObject dso, String parent)
            throws SQLException
    {

        DSpaceObject current = dso;

        while (current != null)
        {

            // Check if the current object has the handle we are looking for.
            if (current.getHandle().equals(parent))
            {
                return true;
            }

            if (current.getType() == Constants.ITEM)
            {
                current = ((Item) current).getOwningCollection();
            }
            else if (current.getType() == Constants.COLLECTION)
            {
                current = ((Collection) current).getCommunities()[0];
            }
            else if (current.getType() == Constants.COMMUNITY)
            {
                current = ((Community) current).getParentCommunity();
            }
        }

        // If the loop finished then we searched the entire parant-child chain
        // and did not find this handle, so the object was not found.

        return false;
    }

    /**
     * Build a list of trail metadata starting with the owning collection and
     * ending with the root level parent. If the Object is an item, a bundle,
     * or a bitstream, then the object is not included, but its collection and
     * community parents are. However if the item is a community or collection
     * then it is included along with all parents.
     * 
     * <p>
     * If the terminal object in the trail is the passed object, do not link to
     * it, because that is (presumably) the page at which the user has arrived.
     * 
     * @param dso
     * @param pageMeta
     */
    public static void buildHandleTrail(DSpaceObject dso, PageMeta pageMeta,
            String contextPath) throws SQLException, WingException
    {
        buildHandleTrail(dso, pageMeta, contextPath, false);
    }

    /**
     * A trail of DSpace Objects to the root, that includes a link to the terminal level.
     * @param dso
     * @param pageMeta
     * @param contextPath
     * @throws SQLException
     * @throws WingException
     */
    public static void buildHandleTrailIncludeTerminal(DSpaceObject dso, PageMeta pageMeta,
                                        String contextPath) throws SQLException, WingException
    {
        buildHandleTrail(dso, pageMeta, contextPath, true);
    }

    public static void buildHandleTrail(DSpaceObject dso, PageMeta pageMeta, String contextPath, Boolean includeTerminalLink) throws SQLException, WingException {
        Stack<DSpaceObject> stack = buildDSOStack(dso);

        while (!stack.empty())
        {
            DSpaceObject pop = stack.pop();

            // Do not link "back" to the terminal object, unless we are browsing within it...
            String target;
            if (pop == dso && includeTerminalLink==false) {
                target = null;
            } else {
                target = contextPath + "/handle/" + pop.getHandle();
            }

            if (pop instanceof Collection)
            {
                addTrailLinkForCollection((Collection) pop, pageMeta, target);
            }
            else if (pop instanceof Community)
            {
                addTrailLinkForCommunity((Community) pop, pageMeta, target);
            }

        }
    }

    /**
     * Add the trail back to the repository root.
     * @param dso
     * @return
     * @throws SQLException
     */
    public static Stack<DSpaceObject> buildDSOStack(DSpaceObject dso) throws SQLException {
        Stack<DSpaceObject> stack = new Stack<DSpaceObject>();

        if (dso instanceof Bitstream)
        {
            Bitstream bitstream = (Bitstream) dso;
            Bundle[] bundles = bitstream.getBundles();

            dso = bundles[0];
        }

        if (dso instanceof Bundle)
        {
            Bundle bundle = (Bundle) dso;
            Item[] items = bundle.getItems();

            dso = items[0];
        }

        if (dso instanceof Item)
        {
            Item item = (Item) dso;
            Collection collection = item.getOwningCollection();

            dso = collection;
        }

        if (dso instanceof Collection)
        {
            Collection collection = (Collection) dso;
            stack.push(collection);
            Community[] communities = collection.getCommunities();

            dso = communities[0];
        }

        if (dso instanceof Community)
        {
            Community community = (Community) dso;
            stack.push(community);

            for (Community parent : community.getAllParents())
            {
                stack.push(parent);
            }
        }

        return stack;
    }

    public static void addTrailLinkForCommunity(Community community, PageMeta pageMeta, String targetURL) throws WingException {
        String name = community.getMetadata("name");
        if (name == null || name.length() == 0)
        {
            pageMeta.addTrailLink(targetURL, new Message("default", "xmlui.general.untitled"));
        }
        else
        {
            pageMeta.addTrailLink(targetURL, name);
        }
    }

    public static void addTrailLinkForCollection(Collection collection, PageMeta pageMeta, String targetURL) throws WingException {
        String name = collection.getMetadata("name");
        if (name == null || name.length() == 0)
        {
            pageMeta.addTrailLink(targetURL, new Message("default", "xmlui.general.untitled"));
        }
        else
        {
            pageMeta.addTrailLink(targetURL, name);
        }
    }

}
