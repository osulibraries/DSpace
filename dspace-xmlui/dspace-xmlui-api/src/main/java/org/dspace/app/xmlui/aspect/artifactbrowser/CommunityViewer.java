/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.xmlui.aspect.artifactbrowser;

import org.apache.cocoon.caching.CacheableProcessingComponent;
import org.apache.cocoon.util.HashUtil;
import org.apache.excalibur.source.SourceValidity;
import org.apache.log4j.Logger;
import org.dspace.app.xmlui.cocoon.AbstractDSpaceTransformer;
import org.dspace.app.xmlui.utils.DSpaceValidity;
import org.dspace.app.xmlui.utils.HandleUtil;
import org.dspace.app.xmlui.utils.UIException;
import org.dspace.app.xmlui.wing.Message;
import org.dspace.app.xmlui.wing.WingException;
import org.dspace.app.xmlui.wing.element.*;
import org.dspace.authorize.AuthorizeException;
import org.dspace.browse.BrowseException;
import org.dspace.browse.BrowseIndex;
import org.dspace.browse.ItemCountException;
import org.dspace.browse.ItemCounter;
import org.dspace.content.Community;
import org.dspace.content.DSpaceObject;
import org.dspace.content.LiteCollection;
import org.dspace.core.ConfigurationManager;
import org.springframework.util.StopWatch;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.io.Serializable;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * Display a single community. This includes a full text search, browse by list,
 * community display and a list of recent submissions.
 *     private static final Logger log = Logger.getLogger(DSpaceFeedGenerator.class);

 * @author Scott Phillips
 * @author Kevin Van de Velde (kevin at atmire dot com)
 * @author Mark Diggory (markd at atmire dot com)
 * @author Ben Bosman (ben at atmire dot com)
 */
public class CommunityViewer extends AbstractDSpaceTransformer implements CacheableProcessingComponent
{
    private static Logger log = Logger.getLogger(CommunityViewer.class);

    /** Language Strings */
    private static final Message T_dspace_home =
        message("xmlui.general.dspace_home");
    

    public static final Message T_untitled = 
    	message("xmlui.general.untitled");

    private static final Message T_head_browse =
        message("xmlui.ArtifactBrowser.CommunityViewer.head_browse");
    
    private static final Message T_browse_titles = 
        message("xmlui.ArtifactBrowser.CommunityViewer.browse_titles");
    
    private static final Message T_browse_authors =
        message("xmlui.ArtifactBrowser.CommunityViewer.browse_authors");
    
    private static final Message T_browse_dates =
        message("xmlui.ArtifactBrowser.CommunityViewer.browse_dates");
    

    private static final Message T_head_sub_communities = 
        message("xmlui.ArtifactBrowser.CommunityViewer.head_sub_communities");
    
    private static final Message T_head_sub_collections =
        message("xmlui.ArtifactBrowser.CommunityViewer.head_sub_collections");
    

    /** Cached validity object */
    private SourceValidity validity;

    /**
     * Generate the unique caching key.
     * This key must be unique inside the space of this component.
     */
    public Serializable getKey() {
        try {
            DSpaceObject dso = HandleUtil.obtainHandle(objectModel);
            
            if (dso == null)
            {
                return "0";  // no item, something is wrong
            }
            
            return HashUtil.hash(dso.getHandle());
        } 
        catch (SQLException sqle)
        {
            // Ignore all errors and just return that the component is not cachable.
            return "0";
        }
    }

    /**
     * Generate the cache validity object.
     * 
     * This validity object includes the community being viewed, all 
     * sub-communites (one level deep), all sub-collections, and 
     * recently submitted items.
     */
    public SourceValidity getValidity() 
    {
        StopWatch stopWatch = new StopWatch("CommunityViewer.getValidity");
        if (this.validity == null)
    	{
            Community community = null;
	        try {
                stopWatch.start("Misc");
	            DSpaceObject dso = HandleUtil.obtainHandle(objectModel);
	            
	            if (dso == null)
                {
                    return null;
                }
	            
	            if (!(dso instanceof Community))
                {
                    return null;
                }
	            
	            community = (Community) dso;
	            
	            DSpaceValidity validity = new DSpaceValidity();
	            validity.add(community);
                stopWatch.stop();

                stopWatch.start("getSubComms");
	            Community[] subCommunities = community.getSubcommunities();
                stopWatch.stop();

                stopWatch.start("validate subcomms");
	            // Sub communities
	            for (Community subCommunity : subCommunities)
	            {
	                validity.add(subCommunity);
	                
	                // Include the item count in the validity, only if the value is cached.
	                boolean useCache = ConfigurationManager.getBooleanProperty("webui.strengths.cache");
	                if (useCache)
	        		{
	                    try {	
	                    	int size = new ItemCounter(context).getCount(subCommunity);
	                    	validity.add("size:"+size);
	                    } catch(ItemCountException e) { /* ignore */ }
	        		}
	            }
                stopWatch.stop();

	            // Sub collections
                /*
                stopWatch.start("getColls");
                Collection[] collections = community.getCollections();
                stopWatch.stop();

                stopWatch.start("Validate colls");
                for (Collection collection : collections)
	            {
	                validity.add(collection);
	                
	                // Include the item count in the validity, only if the value is cached.
	                boolean useCache = ConfigurationManager.getBooleanProperty("webui.strengths.cache");
	                if (useCache)
	        		{
	                    try {
	                    	int size = new ItemCounter(context).getCount(collection);
	                    	validity.add("size:"+size);
	                    } catch(ItemCountException e) {
                        }
	        		}
	            }
                stopWatch.stop();*/


                stopWatch.start("getLITEcolls");
                ArrayList<LiteCollection> liteCollectionArrayList = community.getCollectionsLite();
                stopWatch.stop();

                stopWatch.start("Validate liteColls");
                for(LiteCollection liteCollection : liteCollectionArrayList) {
                    validity.add(liteCollection.getHandle() + liteCollection.getName());
                }
                stopWatch.stop();


	            this.validity = validity.complete();
	        } 
	        catch (Exception e)
	        {
	            // Ignore all errors and invalidate the cache.
	        }

    	}

        log.info(stopWatch.prettyPrint());

        return this.validity;
    }
    
    
    /**
     * Add the community's title and trail links to the page's metadata
     */
    public void addPageMeta(PageMeta pageMeta) throws SAXException,
            WingException, UIException, SQLException, IOException,
            AuthorizeException
    {
        DSpaceObject dso = HandleUtil.obtainHandle(objectModel);
        if (!(dso instanceof Community))
        {
            return;
        }

        // Set up the major variables
        Community community = (Community) dso;
        // Set the page title
        String name = community.getMetadata("name");
        if (name == null || name.length() == 0)
        {
            pageMeta.addMetadata("title").addContent(T_untitled);
        }
        else
        {
            pageMeta.addMetadata("title").addContent(name);
        }

        // Add the trail back to the repository root.
        pageMeta.addTrailLink(contextPath + "/",T_dspace_home);
        HandleUtil.buildHandleTrail(community, pageMeta,contextPath);
        
        // Add RSS links if available
        String formats = ConfigurationManager.getProperty("webui.feed.formats");
		if ( formats != null )
		{
            String audioCommunity = ConfigurationManager.getProperty("webui.feed.podcast.audio.communities");
            String videoCommunity = ConfigurationManager.getProperty("webui.feed.podcast.video.communities");

			for (String format : formats.split(","))
			{
				// Remove the protocol number, i.e. just list 'rss' or' atom'
				String[] parts = format.split("_");
				if (parts.length < 1)
                {
                    continue;
                }
				
				String feedFormat = parts[0].trim()+"+xml";
					
				String feedURL = contextPath+"/feed/"+format.trim()+"/"+community.getHandle();
				pageMeta.addMetadata("feed", feedFormat).addContent(feedURL);

                //if this community has audio/video specific feeds too. Add them
                if(audioCommunity != null && audioCommunity.contains(community.getHandle())) {
                    pageMeta.addMetadata("feed", feedFormat).addContent(feedURL + "/mediaType/audio");
                }

                if(videoCommunity != null && videoCommunity.contains(community.getHandle())) {
                    pageMeta.addMetadata("feed", feedFormat).addContent(feedURL + "/mediaType/video");
                }
			}
		}
    }

    /**
     * Display a single community (and refrence any sub communites or
     * collections)
     */
    public void addBody(Body body) throws SAXException, WingException,
            UIException, SQLException, IOException, AuthorizeException
    {

        DSpaceObject dso = HandleUtil.obtainHandle(objectModel);
        if (!(dso instanceof Community))
        {
            return;
        }

        // Set up the major variables
        Community community = (Community) dso;

        StopWatch stopWatch = new StopWatch("CommunityViewer Fetching subComms / colls (commID: " + community.getID());

        // Build the community viewer division.
        Division home = body.addDivision("community-home", "primary repository community");
        String name = community.getMetadata("name");
        if (name == null || name.length() == 0)
        {
            home.setHead(T_untitled);
        }
        else
        {
            home.setHead(name);
        }

        // The search / browse box.
        {
            Division search = home.addDivision("community-search-browse",
                    "secondary search-browse");


//            TODO: move browse stuff out of here
            // Browse by list
            Division browseDiv = search.addDivision("community-browse","secondary browse");
            List browse = browseDiv.addList("community-browse", List.TYPE_SIMPLE,
                    "community-browse");
            browse.setHead(T_head_browse);
            String url = contextPath + "/handle/" + community.getHandle();

            try
            {
                // Get a Map of all the browse tables
                BrowseIndex[] bis = BrowseIndex.getBrowseIndices();
                for (BrowseIndex bix : bis)
                {
                    // Create a Map of the query parameters for this link
                    Map<String, String> queryParams = new HashMap<String, String>();

                    queryParams.put("type", bix.getName());

                    // Add a link to this browse
                    browse.addItemXref(super.generateURL(url + "/browse", queryParams),
                            message("xmlui.ArtifactBrowser.Navigation.browse_" + bix.getName()));
                }
            }
            catch (BrowseException bex)
            {
                browse.addItemXref(url + "/browse?type=title",T_browse_titles);
                browse.addItemXref(url + "/browse?type=author",T_browse_authors);
                browse.addItemXref(url + "/browse?type=dateissued",T_browse_dates);
            }
        }

        // Add main reference:
        {
        	Division viewer = home.addDivision("community-view","secondary");
        	
            ReferenceSet referenceSet = viewer.addReferenceSet("community-view",
                    ReferenceSet.TYPE_DETAIL_VIEW);
            Reference communityInclude = referenceSet.addReference(community);

            stopWatch.start("Get SubComms");
            Community[] subCommunities = community.getSubcommunities();
            stopWatch.stop();

            stopWatch.start("Add Comms to ref");
            // If the community has any children communities also refrence them.
            if (subCommunities != null && subCommunities.length > 0)
            {
                ReferenceSet communityReferenceSet = communityInclude
                        .addReferenceSet(ReferenceSet.TYPE_SUMMARY_LIST,null,"hierarchy");

                communityReferenceSet.setHead(T_head_sub_communities);

                // Sub communities
                for (Community subCommunity : subCommunities)
                {
                    communityReferenceSet.addReference(subCommunity);
                }
            }
            stopWatch.stop();



            /*stopWatch.start("getCollections");
            Collection[] collections = community.getCollections();
            stopWatch.stop();

            stopWatch.start("Add Colls to ref");
            if (collections != null && collections.length > 0)
            {
                ReferenceSet communityReferenceSet = communityInclude
                        .addReferenceSet(ReferenceSet.TYPE_SUMMARY_LIST,null,"hierarchy");

                communityReferenceSet.setHead(T_head_sub_collections);
                       

                // Sub collections
                for (Collection collection : collections)
                {
                    communityReferenceSet.addReference(collection);
                }

            }
            stopWatch.stop();*/

            stopWatch.start("getCollectionsLite");
            ArrayList<LiteCollection> liteCollectionList = community.getCollectionsLite();
            stopWatch.stop();

            stopWatch.start("Add lite-colls to dri");
            if(liteCollectionList != null && liteCollectionList.size() > 0) {
                List liteCollList = viewer.addList("LiteCollectionList");
                liteCollList.setHead("Collections in this Community (LITE)");
                for(LiteCollection liteCollection : liteCollectionList) {
                    liteCollList.addItemXref(contextPath + "/handle/" + liteCollection.getHandle(), liteCollection.getName());
                }

            }
            stopWatch.stop();
        }// main refrence

        log.info(stopWatch.prettyPrint());
    }
    


    /**
     * Recycle
     */
    public void recycle()
    {
        // Clear out our item's cache.
        this.validity = null;
        super.recycle();
    }


}
