package com.planiarback.planiar.service;

import com.planiarback.planiar.model.Task;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.annotation.Autowired;

import com.planiarback.planiar.service.GeminiClient;

@Service
public class AIPlannerService {

    @Value("${ai.useGemini:false}")
    private boolean useGemini;

    @Autowired
    private GeminiClient geminiClient;

    private final ObjectMapper mapper = new ObjectMapper();

    /**
     * Plan tasks using a simple heuristic that respects availableHours and constraints.
     * This is a local planner used as a fallback; in the future this may call an external AI.
     */
    public List<Task> planTasks(List<Task> tasks, Map<String, List<String>> availableHours) {
        if (tasks == null) return Collections.emptyList();

        // Work on copies to avoid mutating caller objects unexpectedly
        List<Task> copy = new ArrayList<>();
        for (Task t : tasks) {
            Task c = new Task();
            c.setId(t.getId());
            c.setTitle(t.getTitle());
            c.setClassId(t.getClassId());
            c.setDueDate(t.getDueDate());
            c.setDueTime(t.getDueTime());
            c.setParentId(t.getParentId());
            c.setSegmentIndex(t.getSegmentIndex());
            c.setTotalSegments(t.getTotalSegments());
            c.setPriority(t.getPriority());
            c.setEstimatedTime(t.getEstimatedTime());
            c.setDescription(t.getDescription());
            c.setType(t.getType());
            c.setState(t.getState());
            c.setUser(t.getUser());
            copy.add(c);
        }

        // Sort by priority (High first), then by dueDate ascending, then by estimatedTime desc
        copy.sort((a,b) -> {
            int pa = priorityValue(a.getPriority());
            int pb = priorityValue(b.getPriority());
            if (pa != pb) return Integer.compare(pb, pa); // higher priority first
            LocalDate da = a.getDueDate(); LocalDate db = b.getDueDate();
            if (da != null && db != null) {
                int cmp = da.compareTo(db);
                if (cmp != 0) return cmp;
            } else if (da != null) return -1; else if (db != null) return 1;
            Integer ea = a.getEstimatedTime(); Integer eb = b.getEstimatedTime();
            return Integer.compare(eb != null ? eb : 0, ea != null ? ea : 0);
        });

        // Occupied slots map: key = date#HH:MM -> true
        Set<String> occupied = new HashSet<>();

        LocalDate today = LocalDate.now();

        for (Task t : copy) {
            // If task already has an assignment within allowed constraints, mark occupied
            if (t.getWorkingDate() != null && t.getStartTime() != null && t.getEndTime() != null) {
                LocalTime cur = t.getStartTime();
                while (cur.isBefore(t.getEndTime())) {
                    occupied.add(t.getWorkingDate().toString() + "#" + cur.toString());
                    cur = cur.plusMinutes(30);
                }
                continue;
            }

            // Try to find latest block before dueDate using availableHours
            LocalDate due = t.getDueDate();
            if (due == null || t.getEstimatedTime() == null || t.getEstimatedTime() <= 0) continue;

            int neededSlots = (int) Math.ceil(t.getEstimatedTime() / 30.0);
            boolean assigned = false;

            // Search backward from due date to tomorrow (no same-day scheduling if due today?)
            LocalDate startSearch = today.plusDays(1);
            for (LocalDate d = due; !d.isBefore(startSearch); d = d.minusDays(1)) {
                // Avoid weekends
                DayOfWeek dow = d.getDayOfWeek();
                if (dow == DayOfWeek.SATURDAY || dow == DayOfWeek.SUNDAY) continue;

                String dayKey = dayNameFor(d.getDayOfWeek().getValue() % 7);
                List<String> freeRanges = availableHours != null ? availableHours.get(dayKey) : null;
                if (freeRanges == null) continue;

                List<Slot> slots = new ArrayList<>();
                LocalTime deadlineTime = t.getDueTime() != null ? t.getDueTime() : LocalTime.of(23,59);

                for (String range : freeRanges) {
                    String[] parts = range.split("-");
                    if (parts.length != 2) continue;
                    try {
                        LocalTime s = LocalTime.parse(parts[0]);
                        LocalTime e = LocalTime.parse(parts[1]);
                        // Trim by deadline if same as due date
                        if (d.equals(due) && e.isAfter(deadlineTime)) e = deadlineTime;
                        // Skip unreasonable hours (1:00-4:00)
                        if (e.isBefore(LocalTime.of(1,0)) || s.isAfter(LocalTime.of(4,0))) {
                            // continue; but we still check per-slot below
                        }
                        LocalTime cur = s;
                        while (cur.plusMinutes(30).isBefore(e) || cur.plusMinutes(30).equals(e)) {
                            // Skip 1am-4am
                            if (cur.getHour() >=1 && cur.getHour() < 4) { cur = cur.plusMinutes(30); continue; }
                            // Skip Friday afternoon (FRI after 17:00)
                            if (d.getDayOfWeek() == DayOfWeek.FRIDAY && cur.isAfter(LocalTime.of(16,59))) { cur = cur.plusMinutes(30); continue; }
                            String key = d.toString() + "#" + cur.toString();
                            if (!occupied.contains(key)) slots.add(new Slot(d, cur, cur.plusMinutes(30)));
                            cur = cur.plusMinutes(30);
                        }
                    } catch (Exception ex) { /* ignore parse */ }
                }

                if (slots.isEmpty()) continue;

                // find contiguous blocks
                List<Block> blocks = findContiguousBlocks(slots);
                if (blocks.isEmpty()) continue;

                // choose block that ends latest and has enough slots
                blocks.sort(Comparator.comparing(b -> b.slots.get(b.slots.size()-1).end));
                for (int i = blocks.size()-1; i >=0; i--) {
                    Block b = blocks.get(i);
                    if (b.slots.size() >= neededSlots) {
                        int startIndex = Math.max(0, b.slots.size() - neededSlots);
                        List<Slot> chosen = b.slots.subList(startIndex, startIndex + neededSlots);
                        t.setWorkingDate(chosen.get(0).date);
                        t.setStartTime(chosen.get(0).start);
                        t.setEndTime(chosen.get(chosen.size()-1).end);
                        // mark occupied
                        LocalTime cur = t.getStartTime();
                        while (cur.isBefore(t.getEndTime())) {
                            occupied.add(t.getWorkingDate().toString() + "#" + cur.toString());
                            cur = cur.plusMinutes(30);
                        }
                        assigned = true;
                        break;
                    }
                }

                if (assigned) break;
            }

            // If not assigned, leave it without assignment — caller can handle further segmentation or external AI.
        }

        // If external AI is enabled or many tasks remain unassigned, try calling Gemini
        boolean needExternal = false;
        for (Task t : copy) if (t.getWorkingDate() == null || t.getStartTime() == null || t.getEndTime() == null) { needExternal = true; break; }

        if (useGemini && needExternal) {
            try {
                String prompt = buildGeminiPrompt(copy, availableHours);
                Optional<String> resp = geminiClient.generateText(prompt);
                if (resp.isPresent()) {
                    String text = resp.get();
                    // Expect JSON array of tasks with id/title/workingDate/startTime/endTime
                    try {
                        JsonNode root = mapper.readTree(text);
                        if (root.isArray()) {
                            List<Task> out = new ArrayList<>();
                            for (JsonNode n : root) {
                                Long id = n.has("id") && !n.get("id").isNull() ? n.get("id").asLong() : null;
                                String title = n.has("title") ? n.get("title").asText(null) : null;
                                Task t = findTaskByIdOrTitle(copy, id, title);
                                if (t == null) continue;
                                if (n.has("workingDate") && !n.get("workingDate").isNull()) t.setWorkingDate(LocalDate.parse(n.get("workingDate").asText()));
                                if (n.has("startTime") && !n.get("startTime").isNull()) t.setStartTime(LocalTime.parse(n.get("startTime").asText()));
                                if (n.has("endTime") && !n.get("endTime").isNull()) t.setEndTime(LocalTime.parse(n.get("endTime").asText()));
                                out.add(t);
                            }
                            return out;
                        }
                    } catch (Exception ex) {
                        // parse failed - log and fall through to return copy
                    }
                }
            } catch (Exception ex) {
                // external AI failed; ignore and return heuristic copy
            }
        }

        return copy;
    }

    private Task findTaskByIdOrTitle(List<Task> tasks, Long id, String title) {
        if (id != null) for (Task t : tasks) if (t.getId() != null && t.getId().equals(id)) return t;
        if (title != null) for (Task t : tasks) if (title.equals(t.getTitle())) return t;
        return null;
    }

    private String buildGeminiPrompt(List<Task> tasks, Map<String, List<String>> availableHours) {
        StringBuilder sb = new StringBuilder();
        sb.append("You are an assistant that must produce a JSON array describing scheduling assignments for tasks.\n");
        sb.append("Return only valid JSON (an array). Each element must include: id (if available), title, workingDate (YYYY-MM-DD) or null, startTime (HH:MM) or null, endTime (HH:MM) or null.\n");
        sb.append("Constraints:\n");
        sb.append("- Keep each task's dueDate and estimatedTime unchanged.\n");
        sb.append("- Do not assign tasks on weekends (Saturday or Sunday).\n");
        sb.append("- Avoid 01:00-04:00 and Friday after 17:00.\n");
        sb.append("- Assign each task inside availableHours. availableHours is a map DAY->list of ranges (HH:MM-HH:MM).\n");
        sb.append("- Try to schedule as late as possible but before dueDate/dueTime. Use 30-minute granularity.\n");
        sb.append("- Do not delete tasks; if no slot exists, set workingDate/startTime/endTime to null.\n");
        sb.append("Now input data:\n");
        sb.append("availableHours:\n");
        sb.append(safeSerialize(availableHours, "{}"));
        sb.append("\n");
        sb.append("tasks:\n");
        sb.append(safeSerialize(tasks, "[]"));
        sb.append("\n");
        sb.append("Return only JSON array. End.");
        return sb.toString();
    }

    private String safeSerialize(Object obj, String fallback) {
        try {
            return mapper.writeValueAsString(obj);
        } catch (Exception e) {
            return fallback;
        }
    }

    private int priorityValue(String p) {
        if (p == null) return 1;
        switch (p) {
            case "High": return 3;
            case "Medium": return 2;
            default: return 1;
        }
    }

    private String dayNameFor(int idx) {
        String[] dayNames = {"SUN", "MON", "TUE", "WED", "THU", "FRI", "SAT"};
        if (idx < 0 || idx >= dayNames.length) return "MON";
        return dayNames[idx];
    }

    // Helper types
    private static class Slot { LocalDate date; LocalTime start; LocalTime end; Slot(LocalDate d, LocalTime s, LocalTime e){date=d;start=s;end=e;} }
    private static class Block { List<Slot> slots = new ArrayList<>(); }

    private List<Block> findContiguousBlocks(List<Slot> slots) {
        List<Block> result = new ArrayList<>();
        if (slots.isEmpty()) return result;
        slots.sort(Comparator.comparing((Slot s) -> s.date).thenComparing(s -> s.start));
        Block current = new Block();
        for (Slot s : slots) {
            if (current.slots.isEmpty()) { current.slots.add(s); continue; }
            Slot last = current.slots.get(current.slots.size()-1);
            if (s.date.equals(last.date) && s.start.equals(last.end)) current.slots.add(s);
            else { result.add(current); current = new Block(); current.slots.add(s); }
        }
        if (!current.slots.isEmpty()) result.add(current);
        return result;
    }
}
