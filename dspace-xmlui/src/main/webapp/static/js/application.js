jQuery(document).ready(function(){

    // "Enter search keywords..." label to appear on top of search box. This JS handles this gracefully.
    // Input-Prompt-Text: A Better way - http://kyleschaeffer.com/best-practices/input-prompt-text/
    jQuery('input[title][id="ds-global-search-box"][type="text"]').each(function(i){
        jQuery(this).addClass('input-prompt-' + i);
        var promptSpan = jQuery('<span class="input-prompt"/>');
        jQuery(promptSpan).attr('id', 'input-prompt-' + i);
        jQuery(promptSpan).append(jQuery(this).attr('title'));
        jQuery(promptSpan).click(function(){
            jQuery(this).hide();
            jQuery('.' + jQuery(this).attr('id')).focus();
        });
        if(jQuery(this).val() != ''){
            jQuery(promptSpan).hide();
        }
        jQuery(this).before(promptSpan);
        jQuery(this).focus(function(){
            jQuery('#input-prompt-' + i).hide();
        });
        jQuery(this).blur(function(){
            if(jQuery(this).val() == ''){
                jQuery('#input-prompt-' + i).show();
            }
        });
    });

    // On advanced-search result pages, scroll-down to the results. Doing this in HTML with #anchorID, was breaking pagination links.
    if (jQuery(location).attr('pathname').indexOf('advanced-search') >= 0) {
        if (((jQuery(location).attr('search').length > 0) || jQuery(location).attr('hash').length > 0)) {
            jQuery(document).scrollTop( jQuery("#result-query").offset().top );
        }
    }

});