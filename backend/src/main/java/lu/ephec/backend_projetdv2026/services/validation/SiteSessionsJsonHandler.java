package lu.ephec.backend_projetdv2026.services.validation;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.stereotype.Component;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

/* JSON EXAMPLE

{
  "sessions": [
    {
      "match_set_id": 1,
      "start_time": "13:15:00",
      "end_time": "14:45:00",
      "duration_minutes": 90
    }
  ]
}

 */

@Component
public class SiteSessionsJsonHandler {

    private static final int MIN_PRE_SESSION_MINUTES = 15;
    private static final int MAX_PRE_SESSION_MINUTES = 30;
    private static final int SESSION_DURATION_MINUTES = 90;
    private static final int BREAK_MINUTES = 15;
    private static final int POST_SESSION_MINUTES = 15;  // Always mandatory

    private final ObjectMapper objectMapper = new ObjectMapper();

    // Automatically generates the JSON sessions based on the site's opening hours
    //
    // Logic:
    // - Pre-session: 15-30 min (optimized to fit sessions)
    // - Sessions: 1h30 each
    // - Breaks: 15 min between each session
    // - Post-session: 15 min after (MANDATORY - players must change and wash)

    public String generateSessionsJson(LocalTime openingTime, LocalTime closingTime) {
        ObjectNode rootNode = objectMapper.createObjectNode();
        ArrayNode sessionsArray = objectMapper.createArrayNode();

        // Try different pre-session durations (30 min first, then 15 min) to fit more sessions
        int preSessionMinutes = calculateOptimalPreSession(openingTime, closingTime);

        LocalTime sessTime = openingTime.plusMinutes(preSessionMinutes);
        int matchSetId = 1;

        // GENERATE SESSIONS AS LONG AS THERE'S TIME FOR A FULL SESSION + 15MIN POST-SESSION BEFORE CLOSING
        while (sessTime.plusMinutes(SESSION_DURATION_MINUTES + POST_SESSION_MINUTES).isBefore(closingTime) ||
                sessTime.plusMinutes(SESSION_DURATION_MINUTES + POST_SESSION_MINUTES).equals(closingTime)) {

            LocalTime sessionEnd = sessTime.plusMinutes(SESSION_DURATION_MINUTES);

            ObjectNode sessionNode = objectMapper.createObjectNode();
            sessionNode.put("match_set_id", matchSetId);
            sessionNode.put("start_time", formatTime(sessTime));
            sessionNode.put("end_time", formatTime(sessionEnd));
            sessionNode.put("duration_minutes", SESSION_DURATION_MINUTES);

            sessionsArray.add(sessionNode);

            // BREAK + NEXT SESSION START
            sessTime = sessionEnd.plusMinutes(BREAK_MINUTES);
            matchSetId++;
        }

        rootNode.set("sessions", sessionsArray);
        return rootNode.toString();
    }

    // Calculate optimal pre-session duration (try 30 min first, fallback to 15 min)
    private int calculateOptimalPreSession(LocalTime openingTime, LocalTime closingTime) {
        long minutesAvailable = java.time.temporal.ChronoUnit.MINUTES
                .between(openingTime, closingTime);

        // Try 30 min pre-session first
        if (minutesAvailable >= MAX_PRE_SESSION_MINUTES + SESSION_DURATION_MINUTES + POST_SESSION_MINUTES) {
            return MAX_PRE_SESSION_MINUTES;
        }

        // Fallback to 15 min
        return MIN_PRE_SESSION_MINUTES;
    }

    //FORMAT TIME TO ISO FOR JSON INPUT
    private String formatTime(LocalTime time) {
        return time.format(DateTimeFormatter.ISO_LOCAL_TIME);
    }
}
