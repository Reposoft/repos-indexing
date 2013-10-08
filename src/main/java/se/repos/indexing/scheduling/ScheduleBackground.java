package se.repos.indexing.scheduling;

/**
 * Send the rest of indexing to background.
 * Handlers before this action can throw exceptions to the caller.
 */
public final class ScheduleBackground extends MarkerOnly {
}
