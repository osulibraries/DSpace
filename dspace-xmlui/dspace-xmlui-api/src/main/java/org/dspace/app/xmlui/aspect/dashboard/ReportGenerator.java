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

import org.apache.cocoon.environment.Request;
import org.apache.log4j.Logger;
import org.dspace.app.xmlui.wing.WingException;
import org.dspace.app.xmlui.wing.element.Body;
import org.dspace.app.xmlui.wing.element.Division;
import org.dspace.app.xmlui.wing.element.Para;
import org.dspace.app.xmlui.wing.element.Text;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * Use a form to dynamically generate a variety of reports.
 *
 * @author "Ryan McGowan" ("mcgowan.98@osu.edu")
 * @version
 */
public class ReportGenerator
{
    /**
     * A logger for this class.
     */
    private static Logger log = Logger.getLogger(ReportGenerator.class);
    /**
     * The minimum date for the from or to field to be. (e.g. The beginning of DSpace)
     */
    public static String MINIMUM_DATE_USAGE = "2008-01-01";
    public static String MINIMUM_DATE_GROWTH = "2003-07-11";
    private String MINIMUM_DATE = MINIMUM_DATE_USAGE;

    private static SimpleDateFormat dateFormat = new SimpleDateFormat("MM/dd/yyyy");

    // perfect input is 2008-01-22, an alternate format is 01/22/2008, or 1/22/08
    static String[] formatStrings = {"MM/dd/yy", "MM/dd/yyyy", "yyyy-MM-dd"};

    private Map<String, String> params;
    
    private Date dateStart;
    private Date dateEnd;

    private StringBuilder notice = new StringBuilder();

    public Date getDateStart() {
        return dateStart;
    }
    
    public String getDateStartFormatted() {
        try {
            return dateFormat.format(dateStart);
        } catch (Exception e) {
            return "";
        }
    }
    
    public void setDateStart() {
        if(! params.containsKey("from")) {
            dateStart = null;
        } else {
            dateStart = tryParse(params.get("from"));

            //Don't allow dates before min-date
            if(dateStart.before(tryParse(MINIMUM_DATE))) {
                notice.append("Start Date must be on/after the minimum date of: " + MINIMUM_DATE + ". ");
                dateStart = tryParse(MINIMUM_DATE);
            }
        }
    }
    
    public Date tryParse(String dateString) {
        if(dateString == null || dateString.length() == 0) {
            return null;
        }

        for(String formatString : formatStrings) {
            try {
                return new SimpleDateFormat(formatString).parse(dateString);
            } catch (ParseException e) {
                log.error("ReportGenerator couldn't parse date: " + dateString + ", with pattern of: "+formatString+" with error message:"+e.getMessage());
            }
        }
        return null;
    }

    public Date getDateEnd() {
        return dateEnd;
    }
    
    public String getDateEndFormatted() {
        try {
            return dateFormat.format(dateEnd);
        } catch (Exception e) {
            return "";
        }
    }
    
    public void setDateEnd() {
        if(! params.containsKey("to")) {
            dateEnd= null;
        } else {
            dateEnd = tryParse(params.get("to"));

            //DateEnd must be later than a DateStart.
            if(dateEnd != null && dateStart != null && dateEnd.before(dateStart)) {
                //Cheat, and make dateEnd after dateStart. Should provide some type of UI hint about this.
                notice.append("End Date must be after the start date. ");

                Calendar calendar = Calendar.getInstance();
                calendar.setTime(dateStart);
                calendar.add(Calendar.YEAR, +1);
                dateEnd = calendar.getTime();
            }
        }
    }

    public String getNotice() {
        return notice.toString();
    }

    public boolean hasNotice() {
        if(notice.length() > 0) {
            return true;
        } else {
            return false;
        }
    }

    public String getMINIMUM_DATE() {
        return MINIMUM_DATE;
    }

    /**
     * Setting the minimum date effects the lower-bound that dates can have. We have usage and growth stats which have
     * different start dates.
     * @param date ReportGenerator.MINIMUM_DATE_USAGE or ReportGenerator.MINIMUM_DATE_GROWTH
     */
    public void setMINIMUM_DATE(String date) {
        MINIMUM_DATE = date;
    }


    /**
     * This will parse the start/end dates out of the request, using "from" and to" params.
     * {@inheritDoc}
     * @see org.dspace.app.xmlui.cocoon.DSpaceTransformer#addBody(Body)
     */
    public void addReportGeneratorForm(Division parentDivision, Request request) {
        try {
            Division division = parentDivision.addDivision("report-generator", "primary");

            division.setHead("Report Generator");
            division.addPara("Used to generate reports with an arbitrary date range.");

            Division search = parentDivision.addInteractiveDivision("choose-report", request.getRequestURI(), Division.METHOD_GET, "primary");

            params = new HashMap<String, String>();
            for (Enumeration<String> paramNames = (Enumeration<String>) request.getParameterNames(); paramNames.hasMoreElements(); ) {
                String param = paramNames.nextElement();
                params.put(param, request.getParameter(param));
            }

            //Create Date Range part of form
            Para reportForm = search.addPara();

            setDateStart();
            Text from = reportForm.addText("from", "slick");
            from.setLabel("From");
            from.setHelp("The start date of the report, ex 01/31/2008");
            from.setValue(getDateStartFormatted());

            setDateEnd();
            Text to = reportForm.addText("to", "slick");
            to.setLabel("To");
            to.setHelp("The end date of the report, ex 12/31/2012");
            to.setValue(getDateEndFormatted());

            reportForm.addButton("submit_add").setValue("Generate Report");
        } catch (WingException e) {
            log.error(e.getMessage());
        }
    }
}
