package net.flex.dci.otn.controller.idc.manager.component;

import net.flex.dci.otn.db.jpa.entity.SiteInfo;

/**
 * @version 1.0
 * @date 2022/3/28 11:04
 */
public interface SiteInfoDecorator {

    SiteInfo enrichSiteInfo(SiteInfo siteInfo);
}
