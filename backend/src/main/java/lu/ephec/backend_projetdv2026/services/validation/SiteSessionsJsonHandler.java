package lu.ephec.backend_projetdv2026.services.validation;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.stereotype.Component;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;

@Component
public class SiteSessionsJsonHandler {

    private static final int MIN_PRE_SESSION_MINUTES = 15;
    private static final int MAX_PRE_SESSION_MINUTES = 30;
    private static final int SESSION_DURATION_MINUTES = 90;
    private static final int BREAK_MINUTES = 15;
    private static final int POST_SESSION_MINUTES = 15;  // Always mandatory

    private final ObjectMapper objectMapper = new ObjectMapper();

    //WORKING WITH ChronoUnit as LocalTime was wrapping on 24h !!

    // Automatically generates the JSON sessions based on the site's opening hours
    //
    // Logic:
    // - Pre-session: 15-30 min (optimized to fit sessions)
    // - Sessions: 1h30 each
    // - Breaks: 15 min between each session
    // - Post-session: minimum 15 min after (MANDATORY - players must change and wash)
    public String generateSessionsJson(LocalTime openingTime, LocalTime closingTime) {
        ObjectNode rootNode = objectMapper.createObjectNode();
        ArrayNode sessionsArray = objectMapper.createArrayNode();

        long totalMinutes = ChronoUnit.MINUTES.between(openingTime, closingTime);
        int preSessionMinutes = calculateOptimalPreSession(totalMinutes);

        long offset = preSessionMinutes; // minutes since opening
        int matchSetId = 1;

        // GENERATE SESSIONS - check BEFORE adding to array
        while (offset + SESSION_DURATION_MINUTES + POST_SESSION_MINUTES <= totalMinutes) {

            LocalTime start = openingTime.plusMinutes(offset);
            LocalTime end = start.plusMinutes(SESSION_DURATION_MINUTES);

            ObjectNode sessionNode = objectMapper.createObjectNode();
            sessionNode.put("match_set_id", matchSetId);
            sessionNode.put("start_time", formatTime(start));
            sessionNode.put("end_time", formatTime(end));
            sessionNode.put("duration_minutes", SESSION_DURATION_MINUTES);

            sessionsArray.add(sessionNode);

            // NEXT SESSION START (with break)
            offset += SESSION_DURATION_MINUTES + BREAK_MINUTES;
            matchSetId++;
        }

        rootNode.set("sessions", sessionsArray);
        return rootNode.toString();
    }

    // Calculate optimal pre-session duration (count real sessions with 15 vs 30)
    private int calculateOptimalPreSession(long totalMinutes) {
        int sessionsWith15 = countSessions(totalMinutes, MIN_PRE_SESSION_MINUTES);
        int sessionsWith30 = countSessions(totalMinutes, MAX_PRE_SESSION_MINUTES);

        // Use option that yields more sessions
        if (sessionsWith30 > sessionsWith15) return MAX_PRE_SESSION_MINUTES;
        if (sessionsWith15 > sessionsWith30) return MIN_PRE_SESSION_MINUTES;

        // Same number of sessions: prefer 30 if it keeps leftover <=30, else 15
        long post15 = remainingAfterLast(totalMinutes, MIN_PRE_SESSION_MINUTES);
        long post30 = remainingAfterLast(totalMinutes, MAX_PRE_SESSION_MINUTES);
        if (post30 <= 30 && post15 > 30) return MAX_PRE_SESSION_MINUTES;

        return MIN_PRE_SESSION_MINUTES;
    }

    // Count how many sessions fit with a given pre-session
    private int countSessions(long totalMinutes, int preSession) {
        long offset = preSession;
        int count = 0;
        while (offset + SESSION_DURATION_MINUTES + POST_SESSION_MINUTES <= totalMinutes) {
            count++;
            offset += SESSION_DURATION_MINUTES + BREAK_MINUTES;
        }
        return count;
    }

    // Minutes remaining after the last session for a given pre-session
    private long remainingAfterLast(long totalMinutes, int preSession) {
        long offset = preSession;
        long lastEndOffset = preSession;
        while (offset + SESSION_DURATION_MINUTES + POST_SESSION_MINUTES <= totalMinutes) {
            lastEndOffset = offset + SESSION_DURATION_MINUTES;
            offset += SESSION_DURATION_MINUTES + BREAK_MINUTES;
        }
        return totalMinutes - lastEndOffset;
    }

    //FORMAT TIME TO ISO FOR JSON INPUT
    private String formatTime(LocalTime time) {
        return time.format(DateTimeFormatter.ISO_LOCAL_TIME);
    }
}
