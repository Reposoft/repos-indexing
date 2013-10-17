/**
 * Copyright (C) 2004-2012 Repos Mjukvara AB
 */
package se.repos.indexing.scheduling;

import se.repos.indexing.Marker;

/**
 * Indicates that this marker can be delayed out of sync with any indexing units, until schedule is idle.
 */
public interface MarkerWhenIdle extends Marker {

}
