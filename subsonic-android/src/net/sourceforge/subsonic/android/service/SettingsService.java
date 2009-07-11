/*
 * (c) Copyright WesternGeco. Unpublished work, created 2009. All rights
 * reserved under copyright laws. This information is confidential and is
 * the trade property of WesternGeco. Do not use, disclose, or reproduce
 * without the prior written permission of the owner.
 */
package net.sourceforge.subsonic.android.service;

/**
 * @author Sindre Mehus
 * @version $Id$
 */
public interface SettingsService {
    String getUsername();

    String getPassword();

    int getPlayer();

    String getBaseUrl();
}