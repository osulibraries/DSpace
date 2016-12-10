<?xml version="1.0" encoding="UTF-8"?>

<!--
    Document   : OSU-local.xsl
    Created on : July 6, 2010, 2:56 PM
    Author     : stamper.10
    Description:
        Contains templates that only exist in our local customization, as
        opposed to overrides that are found within the other xsl in this folder.
-->

<xsl:stylesheet xmlns:i18n="http://apache.org/cocoon/i18n/2.1"
                xmlns:dri="http://di.tamu.edu/DRI/1.0/"
                xmlns:mets="http://www.loc.gov/METS/"
                xmlns:xlink="http://www.w3.org/TR/xlink/"
                xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="1.0"
                xmlns:dim="http://www.dspace.org/xmlns/dspace/dim"
                xmlns:xhtml="http://www.w3.org/1999/xhtml"
                xmlns:mods="http://www.loc.gov/mods/v3"
                xmlns:dc="http://purl.org/dc/elements/1.1/"
                xmlns:rdf="http://www.w3.org/1999/02/22-rdf-syntax-ns#"
                xmlns:cc="http://creativecommons.org/ns#"
                xmlns="http://www.w3.org/1999/xhtml"
                exclude-result-prefixes="i18n dri mets xlink xsl dim xhtml mods dc rdf cc">

    <xsl:output indent="yes"/>


    <xsl:template name="buildHeadOSU">
        <!-- Grab Google CDN jQuery. fall back to local if necessary. Also use same http / https as site -->
        <script type="text/javascript">
            var JsHost = (("https:" == document.location.protocol) ? "https://" : "http://");
            document.write(unescape("%3Cscript src='" + JsHost + "ajax.googleapis.com/ajax/libs/jquery/1.7.2/jquery.min.js' type='text/javascript'%3E%3C/script%3E"));

            if(!window.jQuery) {
                document.write(unescape("%3Cscript src='<xsl:value-of select="/dri:document/dri:meta/dri:pageMeta/dri:metadata[@element='contextPath'][not(@qualifier)]"/>/static/js/jquery-1.7.2.min.js' type='text/javascript'%3E%3C/script%3E"));
            }
        </script>

        <!-- Twitter Bootstrap -->
        <link rel="stylesheet">
            <xsl:attribute name="href">
                <xsl:value-of select="/dri:document/dri:meta/dri:pageMeta/dri:metadata[@element='contextPath'][not(@qualifier)]"/>
                <xsl:text>/static/css/bootstrap.css</xsl:text>
            </xsl:attribute>
        </link>

        <script type="text/javascript">
            <xsl:attribute name="src">
                <xsl:value-of select="/dri:document/dri:meta/dri:pageMeta/dri:metadata[@element='contextPath'][not(@qualifier)]"/>
                <xsl:text>/static/js/bootstrap.js</xsl:text>
            </xsl:attribute>
            <xsl:text> </xsl:text>
        </script>


        <script type="text/javascript">
            <xsl:attribute name="src">
                <xsl:value-of select="/dri:document/dri:meta/dri:pageMeta/dri:metadata[@element='contextPath'][not(@qualifier)]"/>
                <xsl:text>/static/js/osul-customized.jquery.zrssfeed.js</xsl:text>
            </xsl:attribute>
            <xsl:text> </xsl:text>
        </script>

        <!-- KB - Custom Javascript for entire application. -->
        <script type="text/javascript">
            <xsl:attribute name="src">
                <xsl:value-of select="/dri:document/dri:meta/dri:pageMeta/dri:metadata[@element='contextPath'][not(@qualifier)]"/>
                <xsl:text>/static/js/application.js</xsl:text>
            </xsl:attribute>
            <xsl:text> </xsl:text>
        </script>
        <link rel="shortcut icon " type="image/x-icon">
            <xsl:attribute name="href">
                <xsl:value-of select="/dri:document/dri:meta/dri:pageMeta/dri:metadata[@element='contextPath'][not(@qualifier)]"/>
                <xsl:text>/static/osu-navbar-media/img/favicon.ico</xsl:text>
            </xsl:attribute>
        </link>
        <!-- bds: jQuery breadcrumb trail shrinker, uses easing plugin -->
        <!-- http://www.comparenetworks.com/developers/jqueryplugins/jbreadcrumb.html -->
        <script type="text/javascript">
            <xsl:attribute name="src">
                <xsl:value-of select="/dri:document/dri:meta/dri:pageMeta/dri:metadata[@element='contextPath'][not(@qualifier)]"/>
                <xsl:text>/static/js/jquery.easing.1.3.js</xsl:text>
            </xsl:attribute>
            <xsl:text> </xsl:text>
        </script>
        <script type="text/javascript">
            <xsl:attribute name="src">
                <xsl:value-of select="/dri:document/dri:meta/dri:pageMeta/dri:metadata[@element='contextPath'][not(@qualifier)]"/>
                <xsl:text>/static/js/jquery.jBreadCrumb.1.1.js</xsl:text>
            </xsl:attribute>
            <xsl:text> </xsl:text>
        </script>
        <script type="text/javascript">
            <xsl:attribute name="src">
                <xsl:value-of select="/dri:document/dri:meta/dri:pageMeta/dri:metadata[@element='contextPath'][not(@qualifier)]"/>
                <xsl:text>/static/js/ba-linkify.min.js</xsl:text>
            </xsl:attribute>
            <xsl:text> </xsl:text>
        </script>
        <script type="text/javascript">
            jQuery(document).ready(function() {
                jQuery("#breadCrumb0").jBreadCrumb();

                /* Linkify All Item Metadata content */
                jQuery('#aspect_artifactbrowser_ItemViewer_div_item-view table.ds-includeSet-table tr.ds-table-row td span').each(function(){
                    var that = jQuery(this),
                    text = that.html(),
                    options = {callback: function( text, href ) {return href ? '<a href="' + href + '" title="' + text + '">' + text + '</a>' : text;}};
                    that.html(linkify(text, options ));
                });
            });
        </script>
    </xsl:template>

    <!-- 2012-07-31 DE Redid the OSU Navbar -->
    <xsl:template name="buildBodyOSU">
        <div role="navigation" id="osu_navbar" aria-labelledby="osu_navbar_heading">
            
            <h2 id="osu_navbar_heading" class="osu-semantic">Ohio State nav bar</h2>
            <a href="#page-content" id="skip" class="osu-semantic">Skip to main content</a>
            
            <div class="container">
                <div class="univ_info">
                    <p class="univ_name"><a href="http://osu.edu" title="The Ohio State University">The Ohio State University</a></p>
                </div><!-- /univ_info -->
                <div class="univ_links">
                    <div class="links">
                        <ul>
                            <li><a href="http://www.osu.edu/help.php" class="help">Help</a></li>
                            <li><a href="http://buckeyelink.osu.edu/" class="buckeyelink" >BuckeyeLink</a></li>
                            <li><a href="http://www.osu.edu/map/" class="map">Map</a></li>
                            <li><a href="http://www.osu.edu/findpeople.php" class="findpeople">Find People</a></li>
                            <li><a href="https://email.osu.edu/" class="webmail">Webmail</a></li> 
                            <li><a href="http://www.osu.edu/search/" class="search">Search Ohio State</a></li>
                        </ul>
                    </div><!-- /links -->
                </div><!-- /univ_links -->
            </div><!-- /container -->

        </div><!-- /osu_navbar -->
        <!-- OLD!! -->
        <!--<div id="osu-nav-bar" class="clearfix">
            <h2 class="visuallyhidden">OSU Navigation Bar</h2>
            <a href="#main-content" id="skip" class="osu-semantic">Skip to main content</a>
            <p id="osu-site-title">
                <a href="http://www.osu.edu/" title="The Ohio State University homepage">The Ohio State University</a>
                <a href="http://library.osu.edu/" title="University Libraries at The Ohio State University">University Libraries</a>
                <a href="http://kb.osu.edu/" title="Knowledge Bank of University Libraries at The Ohio State University">Knowledge Bank</a>
            </p>
            <div id="osu-nav-primary">
                <h3 class="visuallyhidden">Links:</h3>
                <ul>
                    <li><a href="http://www.osu.edu/help.php" title="OSU Help">Help</a></li>
                    <li><a href="http://buckeyelink.osu.edu/" title="Buckeye Link">Buckeye Link</a></li>
                    <li><a href="http://www.osu.edu/map/" title="Campus map">Map</a></li>
                    <li><a href="http://www.osu.edu/findpeople.php" title="Find people at OSU">Find People</a></li>
                    <li><a href="https://webmail.osu.edu/" title="OSU Webmail">Webmail</a></li>
                    <li><a href="http://www.osu.edu/search.php" title="Search Ohio State">Search Ohio State</a></li>
                </ul>
            </div>
        </div>-->
    </xsl:template>
    <!-- This is a named template to be an easy way to override to add something to the buildHead -->
    <xsl:template name="extraHead-top"></xsl:template>
    <xsl:template name="extraHead-bottom"></xsl:template>
    <xsl:template name="extraBody-end"></xsl:template>


    <!-- Peter's RSS code for options box -->
    <xsl:template name="addRSSLinks">
        <xsl:for-each select="/dri:document/dri:meta/dri:pageMeta/dri:metadata[@element='feed']">
            <li><a>
                    <xsl:attribute name="href">
                        <xsl:value-of select="."/>
                    </xsl:attribute>

                    <xsl:choose>
                        <!-- Special Case: Media Type -->
                        <xsl:when test="contains(., 'audio')">Audio Feed</xsl:when>
                        <xsl:when test="contains(., 'video')">Video Feed</xsl:when>

                        <!-- Default: Feed Format -->
                        <xsl:when test="contains(., 'rss_1.0')">RSS 1.0</xsl:when>
                        <xsl:when test="contains(., 'rss_2.0')">RSS 2.0</xsl:when>
                        <xsl:when test="contains(., 'atom_1.0')">Atom</xsl:when>

                        <xsl:otherwise>
                            <xsl:value-of select="@qualifier"/>
                        </xsl:otherwise>
                    </xsl:choose>
            </a></li>
        </xsl:for-each>
    </xsl:template>


    <!-- bds: this adds "Please use this URL to cite.." to "Show full item" link section
    copied from structural.xsl, with a more specific match pattern added -->
    <xsl:template match="dri:p[@rend='item-view-toggle item-view-toggle-top']">
        <div class="notice">
            <p>
                Please use this identifier to cite or link to this item:
                <!-- bds: first get the METS URL where we can find the item metadata -->
                <xsl:variable name="metsURL">
                    <xsl:text>cocoon:/</xsl:text>
                    <xsl:value-of select="/dri:document/dri:body/dri:div/dri:referenceSet/dri:reference[@type='DSpace Item']/@url"/>
                    <xsl:text>?sections=dmdSec</xsl:text>
                </xsl:variable>
                <!-- bds: now grab the specific piece of metadata from that METS document -->
                <xsl:variable name="handleURI" select="document($metsURL)/mets:METS/mets:dmdSec/mets:mdWrap/mets:xmlData/dim:dim/dim:field[@element='identifier'][@qualifier='uri']"/>
                <a href="{$handleURI}">
                    <xsl:value-of select="$handleURI"/>
                </a>
            </p>
        </div>
        <p>
            <xsl:call-template name="standardAttributes">
                <xsl:with-param name="class">ds-paragraph</xsl:with-param>
            </xsl:call-template>
            <xsl:apply-templates />
        </p>
    </xsl:template>




<!-- bds: needed this replace-string to do some character escaping -->
<!-- from http://www.dpawson.co.uk/xsl/sect2/replace.html#d8763e61 -->
  <xsl:template name="replace-string">
    <xsl:param name="text"/>
    <xsl:param name="replace"/>
    <xsl:param name="with"/>
    <xsl:choose>
      <xsl:when test="contains($text,$replace)">
        <xsl:value-of select="substring-before($text,$replace)"/>
        <xsl:value-of select="$with"/>
        <xsl:call-template name="replace-string">
          <xsl:with-param name="text" select="substring-after($text,$replace)"/>
          <xsl:with-param name="replace" select="$replace"/>
          <xsl:with-param name="with" select="$with"/>
        </xsl:call-template>
      </xsl:when>
      <xsl:otherwise>
        <xsl:value-of select="$text"/>
      </xsl:otherwise>
    </xsl:choose>
  </xsl:template>


<!-- bds: remove search box from community/collection pages -->
<xsl:template match="dri:div[@id='aspect.artifactbrowser.CollectionSearch.div.collection-search'] | dri:div[@id='aspect.artifactbrowser.CommunitySearch.div.community-search']">
</xsl:template>

<!-- bds: recent submissions box -->
<!-- adding the 'View all' link -->
<xsl:template match="dri:div[@n='collection-recent-submission'] | dri:div[@n='community-recent-submission']">
    <xsl:apply-templates select="./dri:head"/>
    <ul class="ds-artifact-list">
        <xsl:apply-templates select="./dri:referenceSet" mode="summaryList"/>
    </ul>
    <div id="more-link">
        <a>
            <xsl:attribute name="href">
                <xsl:value-of select="/dri:document/dri:body/dri:div/dri:div/dri:div/dri:list/dri:item[3]/dri:xref/@target"/>
            </xsl:attribute>
            <xsl:text>View all submissions ></xsl:text>
        </a>
    </div>
</xsl:template>

    <!-- Overrides GeneralHandler
        bds: this template completely replaces original to display CC-license info, logo, with links, and to NOT display other licenses -->
    <xsl:template match="mets:fileGrp[@USE='CC-LICENSE' or @USE='LICENSE']">
        <xsl:if test="@USE='CC-LICENSE'">
            <xsl:variable name="ccLicenseName"
                              select="/mets:METS/mets:dmdSec/mets:mdWrap/mets:xmlData/dim:dim/dim:field[@element='rights'][@qualifier='cc']" />
            <xsl:variable name="ccLicenseUri"
                              select="/mets:METS/mets:dmdSec/mets:mdWrap/mets:xmlData/dim:dim/dim:field[@element='rights'][@qualifier='ccuri']" />
            <xsl:variable name="handleUri">
                <xsl:for-each select="/mets:METS/mets:dmdSec/mets:mdWrap/mets:xmlData/dim:dim/dim:field[@element='identifier' and @qualifier='uri']">
                    <a>
                        <xsl:attribute name="href">
                            <xsl:copy-of select="./node()"/>
                        </xsl:attribute>
                        <xsl:copy-of select="./node()"/>
                    </a>
                    <xsl:if test="count(following-sibling::dim:field[@element='identifier' and @qualifier='uri']) != 0">
                        <xsl:text>, </xsl:text>
                    </xsl:if>
                </xsl:for-each>
            </xsl:variable>

            <div about="{$handleUri}" class="row">
                <div class="col-sm-3 col-xs-12">
                    <a rel="license" href="{$ccLicenseUri}" alt="{$ccLicenseName}" title="{$ccLicenseName}">
                        <img class="img-responsive">
                            <xsl:attribute name="src">
                                <xsl:value-of select="concat($context-path,'/static/images/cc-ship.gif')"/>
                            </xsl:attribute>
                            <xsl:attribute name="alt">
                                <xsl:value-of select="$ccLicenseName"/>
                            </xsl:attribute>
                        </img>
                    </a>
                </div>
                <div class="col-sm-8">
                    <span>
                        <i18n:text>xmlui.dri2xhtml.METS-1.0.cc-license-text</i18n:text><br />
                        <a rel="license" href="{$ccLicenseUri}" alt="{$ccLicenseName}" title="{$ccLicenseName}">
                            <xsl:value-of select="$ccLicenseName"/>
                            &#160;
                        </a>
                    </span>
                </div>
            </div>
        </xsl:if>
    </xsl:template>

    <xsl:template match="mets:fileSec" mode="artifact-preview">
        <xsl:param name="href"/>


        <xsl:if test="mets:fileGrp[@USE='THUMBNAIL'] and not(contains(mets:fileGrp[@USE='THUMBNAIL']/mets:file/mets:FLocat[@LOCTYPE='URL']/@xlink:href,'isAllowed=n'))">
            <div class="thumbnail artifact-preview osulocal-318">
                <a class="image-link osu-319" href="{$href}">
                    <img alt="Thumbnail">
                        <xsl:attribute name="src">
                            <xsl:value-of
                                    select="mets:fileGrp[@USE='THUMBNAIL']/mets:file/mets:FLocat[@LOCTYPE='URL']/@xlink:href"/>
                        </xsl:attribute>
                        <xsl:attribute name="class">
                            <xsl:text>img-responsive</xsl:text>
                        </xsl:attribute>
                    </img>
                </a>
            </div>
        </xsl:if>

    </xsl:template>


</xsl:stylesheet>
