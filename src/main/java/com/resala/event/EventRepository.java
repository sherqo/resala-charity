package com.resala.event;

import com.resala.core.JdbcRepository;
import com.resala.model.Event;
import com.resala.model.EventRegistration;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public class EventRepository extends JdbcRepository<Event> {

    private static final String SELECT_EVENT =
            "SELECT id, title, event_type_id, date, capacity, status, description FROM events";

    private static final String SELECT_REGISTRATION =
            "SELECT id, event_id, volunteer_id, ticket_code, registered_at, attended_at "
                    + "FROM event_registrations";

    public EventRepository(){

    }
    public EventRepository(Connection conn){
        super(conn);
    }

    @Override
    public long insert(Event e) {
        String sql = """
                INSERT INTO events (title, event_type_id, date, capacity, status, description)
                VALUES (?, ?, ?, ?, ?, ?)
                """;
        long id = insertReturnKey(sql,
                e.getTitle(),
                e.getEventTypeId(),
                dateOf(e),
                e.getCapacity(),
                statusOf(e),
                e.getDescription());
        e.setId(id);
        return id;
    }

    @Override
    public boolean update(Event e) {
        String sql = """
                UPDATE events
                   SET title = ?, event_type_id = ?, date = ?, capacity = ?, status = ?, description = ?
                 WHERE id = ?
                """;
        return execute(sql,
                e.getTitle(),
                e.getEventTypeId(),
                dateOf(e),
                e.getCapacity(),
                statusOf(e),
                e.getDescription(),
                e.getId()) > 0;
    }

    @Override
    public boolean deleteById(long id) {
        return execute("DELETE FROM events WHERE id = ?", id) > 0;
    }

    @Override
    public Optional<Event> findById(long id) {
        return queryOne(SELECT_EVENT + " WHERE id = ?", EventRepository::mapEvent, id);
    }

    @Override
    public List<Event> findAll() {
        return query(SELECT_EVENT + " ORDER BY date", EventRepository::mapEvent);
    }

    public long getRegisteredCount(long eventId) {
        return countOf("SELECT COUNT(*) AS total FROM event_registrations WHERE event_id = ?", eventId);
    }

    public long register(long eventId, long volunteerId) {
        return register(eventId, volunteerId, "TC-" + eventId + "-" + volunteerId);
    }

    public long register(long eventId, long volunteerId, String ticketCode) {
        return insertReturnKey("""
                INSERT INTO event_registrations (event_id, volunteer_id, ticket_code)
                VALUES (?, ?, ?)
                """, eventId, volunteerId, ticketCode);
    }

    public boolean cancelRegistration(long eventId, long volunteerId) {
        return execute("DELETE FROM event_registrations WHERE event_id = ? AND volunteer_id = ?",
                eventId, volunteerId) > 0;
    }

    public boolean markAttended(long eventId, long volunteerId) {
        return execute("""
                UPDATE event_registrations
                   SET attended_at = strftime('%Y-%m-%dT%H:%M:%S','now')
                 WHERE event_id = ? AND volunteer_id = ?
                """, eventId, volunteerId) > 0;
    }

    public List<EventRegistration> registrationsFor(long eventId) {
        return query(SELECT_REGISTRATION + " WHERE event_id = ? ORDER BY id",
                EventRepository::mapRegistration, eventId);
    }

    public Optional<EventRegistration> findRegistration(long eventId, long volunteerId) {
        return queryOne(SELECT_REGISTRATION + " WHERE event_id = ? AND volunteer_id = ?",
                EventRepository::mapRegistration, eventId, volunteerId);
    }

    public Optional<EventRegistration> findByTicketCode(String ticketCode) {
        return queryOne(SELECT_REGISTRATION + " WHERE ticket_code = ?",
                EventRepository::mapRegistration, ticketCode);
    }

    public long getAttendedCount(long eventId) {
        return countOf("SELECT COUNT(*) AS total FROM event_registrations "
                + "WHERE event_id = ? AND attended_at IS NOT NULL", eventId);
    }

    public List<Event> findByType(long eventTypeId){
        return query(SELECT_EVENT + " WHERE event_type_id = ? ORDER BY date", EventRepository::mapEvent, eventTypeId);
    }

    public List<Event> findByStatus(Event.Status status){
        return query(
                SELECT_EVENT + " WHERE status = ? ORDER BY date", EventRepository::mapEvent, statusOf(status)
        );
    }


    public List<Event> findUpcoming(LocalDate from){
        return query(
                SELECT_EVENT + " WHERE date >= ? ORDER BY date", EventRepository::mapEvent, from.toString()
        );
    }

    public long count(){
        return  countOf("SELECT COUNT(*) AS total FROM events");
    }

    private long countOf(String sql, Object... params){
        Optional<Long> total = queryOne(sql, rs -> rs.getLong("total"), params);
        return total.orElse(0L);
    }

    private static String statusOf(Event e){
        return statusOf(e.getStatus());
    }

    private static String statusOf(Event.Status status){
        return status == null ? Event.Status.DRAFT.name() : status.name();
    }

    private static String dateOf(Event e) {
        return e.getDate() == null ? null : e.getDate().toString();
    }

    private static Event mapEvent(ResultSet rs) throws SQLException {
        return new Event(
                num(rs, "id"),
                str(rs, "title"),
                num(rs, "event_type_id"),
                LocalDate.parse(str(rs, "date")),
                num(rs, "capacity"),
                Event.Status.valueOf(str(rs, "status")),
                str(rs, "description"));
    }

    private static EventRegistration mapRegistration(ResultSet rs) throws SQLException {
        return new EventRegistration(
                num(rs, "id"),
                num(rs, "event_id"),
                num(rs, "volunteer_id"),
                str(rs, "ticket_code"),
                LocalDateTime.parse(str(rs, "registered_at")),
                attendedAt(rs));
    }

    private static LocalDateTime attendedAt(ResultSet rs) throws SQLException {
        String raw = str(rs, "attended_at");
        return raw == null ? null : LocalDateTime.parse(raw);
    }

}
